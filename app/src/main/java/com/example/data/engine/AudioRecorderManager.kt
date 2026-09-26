package com.example.data.engine

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.sqrt

enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED
}

class AudioRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentOutputFile: File? = null
    private var recordingStartTime = 0L
    private var pausedDuration = 0L
    private var pauseStartTime = 0L

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _liveAmplitude = MutableStateFlow(0f)
    val liveAmplitude: StateFlow<Float> = _liveAmplitude.asStateFlow()

    private val recordedAmplitudes = mutableListOf<Float>()
    private var pollingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun startRecording(): File? {
        stopPlayback()
        val dir = File(context.cacheDir, "voice_recordings").apply { mkdirs() }
        val file = File(dir, "voice_${System.currentTimeMillis()}.m4a")
        currentOutputFile = file
        recordedAmplitudes.clear()

        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            recordingStartTime = System.currentTimeMillis()
            pausedDuration = 0L
            _recordingState.value = RecordingState.RECORDING

            // Poll amplitude every 60ms
            pollingJob = scope.launch(Dispatchers.Default) {
                while (isActive && _recordingState.value == RecordingState.RECORDING) {
                    val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                    // Normalize 0..32767 to 0..1 with logarithmic/sqrt smoothing
                    val normalized = (maxAmp.toFloat() / 32767f).coerceIn(0f, 1f)
                    val curved = sqrt(normalized)
                    _liveAmplitude.value = curved
                    recordedAmplitudes.add(curved)
                    delay(60)
                }
            }
            return file
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start recording", e)
            _recordingState.value = RecordingState.IDLE
            return null
        }
    }

    fun pauseRecording() {
        if (_recordingState.value == RecordingState.RECORDING) {
            try {
                mediaRecorder?.pause()
                pauseStartTime = System.currentTimeMillis()
                _recordingState.value = RecordingState.PAUSED
            } catch (e: Exception) {
                Log.e("AudioRecorder", "Failed to pause recording", e)
            }
        }
    }

    fun resumeRecording() {
        if (_recordingState.value == RecordingState.PAUSED) {
            try {
                mediaRecorder?.resume()
                pausedDuration += System.currentTimeMillis() - pauseStartTime
                _recordingState.value = RecordingState.RECORDING
            } catch (e: Exception) {
                Log.e("AudioRecorder", "Failed to resume recording", e)
            }
        }
    }

    data class RecordResult(
        val file: File,
        val durationMs: Long,
        val amplitudes: List<Float>
    )

    fun stopRecording(): RecordResult? {
        pollingJob?.cancel()
        _liveAmplitude.value = 0f

        val file = currentOutputFile
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error stopping recorder", e)
        }
        mediaRecorder = null
        _recordingState.value = RecordingState.IDLE

        if (file != null && file.exists() && file.length() > 0) {
            val totalTime = (System.currentTimeMillis() - recordingStartTime - pausedDuration).coerceAtLeast(300L)
            // Ensure we have representative amplitude curve
            val finalAmps = if (recordedAmplitudes.isEmpty()) {
                List(20) { 0.2f }
            } else {
                recordedAmplitudes.toList()
            }
            return RecordResult(file, totalTime, finalAmps)
        }
        return null
    }

    fun playAudio(filePath: String, onCompletion: () -> Unit = {}) {
        stopPlayback()
        if (filePath.isBlank()) return
        try {
            val player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    onCompletion()
                }
                start()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to play audio: $filePath", e)
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            // ignore
        }
        mediaPlayer = null
    }

    fun importAudioUri(uri: Uri): RecordResult? {
        return try {
            val dir = File(context.cacheDir, "imported_audio").apply { mkdirs() }
            val destFile = File(dir, "imported_${System.currentTimeMillis()}.m4a")

            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(destFile)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            // Extract duration with MediaPlayer
            val mp = MediaPlayer()
            mp.setDataSource(destFile.absolutePath)
            mp.prepare()
            val durationMs = mp.duration.toLong()
            mp.release()

            // Generate representative synthetic speech rhythm amplitudes for imported audio
            val sampleCount = (durationMs / 60).toInt().coerceIn(10, 200)
            val generatedAmps = (0 until sampleCount).map { i ->
                val wave1 = sqrt(kotlin.math.abs(kotlin.math.sin(i * 0.3f)))
                val wave2 = kotlin.math.abs(kotlin.math.cos(i * 0.15f))
                (wave1 * wave2 * 0.85f).coerceIn(0.05f, 0.95f)
            }

            RecordResult(destFile, durationMs.coerceAtLeast(500L), generatedAmps)
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to import audio uri", e)
            null
        }
    }

    fun release() {
        stopRecording()
        stopPlayback()
        scope.cancel()
    }
}
