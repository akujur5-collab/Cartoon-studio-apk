package com.example.data.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer

object VideoExporter {

    data class ExportOptions(
        val width: Int = 720,
        val height: Int = 1280,
        val fps: Int = 30,
        val bitrate: Int = 4_000_000
    )

    suspend fun exportProject(
        context: Context,
        project: Project,
        options: ExportOptions = ExportOptions(),
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val totalDurationMs = project.durationMs.coerceAtLeast(1000L)
        val fps = options.fps
        val totalFrames = ((totalDurationMs * fps) / 1000L).toInt()

        val outputDir = File(context.cacheDir, "exported_videos").apply { mkdirs() }
        val rawVideoFile = File(outputDir, "video_raw_${System.currentTimeMillis()}.mp4")
        val finalOutputFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir,
            "ToonCraft_${System.currentTimeMillis()}.mp4"
        )

        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null

        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, options.width, options.height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, options.bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, options.fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            muxer = MediaMuxer(rawVideoFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()

            // Pre-load bitmaps for custom characters
            val charBitmaps = mutableMapOf<String, Bitmap?>()
            for (scene in project.scenes) {
                for (char in scene.characters) {
                    if (char.imageUri != null && !charBitmaps.containsKey(char.id)) {
                        charBitmaps[char.id] = CartoonGraphicHelper.loadBitmap(context, char.imageUri)
                    }
                }
            }

            for (frame in 0 until totalFrames) {
                val currentTimeMs = (frame * 1000L) / fps
                val currentScene = project.scenes.firstOrNull {
                    currentTimeMs in it.startTimeMs until (it.startTimeMs + it.durationMs)
                } ?: project.scenes.firstOrNull() ?: continue

                // Render frame onto inputSurface
                val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    inputSurface.lockHardwareCanvas()
                } else {
                    inputSurface.lockCanvas(null)
                }

                if (canvas != null) {
                    val evaluatedChars = currentScene.characters.map { char ->
                        val transform = MovementEvaluator.evaluate(char, currentScene.effects, currentTimeMs)
                        val bmp = charBitmaps[char.id]
                        Triple(char.type, bmp, transform)
                    }

                    val visemes = currentScene.characters.map { char ->
                        val (viseme, factor) = LipSyncEngine.evaluateViseme(char, project.audioTracks, currentTimeMs)
                        Triple(viseme, factor, Pair(char.mouthAnchorX, char.mouthAnchorY))
                    }

                    CartoonGraphicHelper.drawFrameToAndroidCanvas(
                        canvas = canvas,
                        width = options.width,
                        height = options.height,
                        bgType = currentScene.backgroundType,
                        customBgBitmap = null,
                        characters = evaluatedChars,
                        visemes = visemes
                    )
                    inputSurface.unlockCanvasAndPost(canvas)
                }

                // Drain encoder
                while (true) {
                    val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
                    if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (muxerStarted) {
                            throw RuntimeException("Format changed twice")
                        }
                        videoTrackIndex = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    } else if (outputBufferIndex >= 0) {
                        val encodedData = encoder.getOutputBuffer(outputBufferIndex)
                        if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            bufferInfo.presentationTimeUs = (frame * 1_000_000L) / fps
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }
                        encoder.releaseOutputBuffer(outputBufferIndex, false)
                    }
                }

                val progress = ((frame.toFloat() / totalFrames.toFloat()) * 90f).toInt()
                withContext(Dispatchers.Main) {
                    onProgress(progress)
                }
            }

            // Signal end of stream
            encoder.signalEndOfInputStream()
            var eos = false
            while (!eos) {
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputBufferIndex >= 0) {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        eos = true
                    }
                    val encodedData = encoder.getOutputBuffer(outputBufferIndex)
                    if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outputBufferIndex, false)
                } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                }
            }

            encoder.stop()
            encoder.release()
            encoder = null

            if (muxerStarted) {
                muxer.stop()
            }
            muxer.release()
            muxer = null

            // Merge audio track if available
            val voiceTrack = project.audioTracks.firstOrNull { File(it.filePath).exists() }
            if (voiceTrack != null) {
                mergeVideoAndAudio(rawVideoFile, File(voiceTrack.filePath), finalOutputFile)
                rawVideoFile.delete()
            } else {
                rawVideoFile.copyTo(finalOutputFile, overwrite = true)
                rawVideoFile.delete()
            }

            withContext(Dispatchers.Main) {
                onProgress(100)
            }
            Result.success(finalOutputFile)
        } catch (e: Exception) {
            Log.e("VideoExporter", "Failed export", e)
            try { encoder?.release() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
            Result.failure(e)
        }
    }

    private fun mergeVideoAndAudio(videoFile: File, audioFile: File, outputFile: File) {
        try {
            val videoExtractor = MediaExtractor().apply { setDataSource(videoFile.absolutePath) }
            val audioExtractor = MediaExtractor().apply { setDataSource(audioFile.absolutePath) }
            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // Select video track
            var videoTrack = -1
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoExtractor.selectTrack(i)
                    videoTrack = muxer.addTrack(format)
                    break
                }
            }

            // Select audio track
            var audioTrack = -1
            for (i in 0 until audioExtractor.trackCount) {
                val format = audioExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioExtractor.selectTrack(i)
                    audioTrack = muxer.addTrack(format)
                    break
                }
            }

            muxer.start()

            val buffer = ByteBuffer.allocate(1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()

            // Copy video
            if (videoTrack >= 0) {
                while (true) {
                    val sampleSize = videoExtractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = videoExtractor.sampleTime
                    bufferInfo.flags = videoExtractor.sampleFlags
                    muxer.writeSampleData(videoTrack, buffer, bufferInfo)
                    videoExtractor.advance()
                }
            }

            // Copy audio
            if (audioTrack >= 0) {
                while (true) {
                    val sampleSize = audioExtractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = audioExtractor.sampleTime
                    bufferInfo.flags = audioExtractor.sampleFlags
                    muxer.writeSampleData(audioTrack, buffer, bufferInfo)
                    audioExtractor.advance()
                }
            }

            muxer.stop()
            muxer.release()
            videoExtractor.release()
            audioExtractor.release()
        } catch (e: Exception) {
            Log.e("VideoExporter", "Failed to merge audio", e)
            videoFile.copyTo(outputFile, overwrite = true)
        }
    }

    /**
     * Saves exported video into user's public gallery (DCIM / Movies)
     */
    fun saveToGallery(context: Context, videoFile: File): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/ToonCraft")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                FileInputStream(videoFile).use { input ->
                    input.copyTo(out)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }
            uri
        } catch (e: Exception) {
            null
        }
    }
}
