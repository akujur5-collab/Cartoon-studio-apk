package com.example.data.engine

import com.example.data.model.AudioTrackData
import com.example.data.model.CharacterLayerData
import com.example.data.model.VisemeType
import kotlin.math.sin

object LipSyncEngine {

    /**
     * Determines the active viseme for a character at a given timestamp.
     */
    fun evaluateViseme(
        character: CharacterLayerData,
        audioTracks: List<AudioTrackData>,
        currentTimeMs: Long
    ): Pair<VisemeType, Float> {
        // If user locked a manual viseme in the UI, prioritize it
        if (character.manualViseme != null) {
            return Pair(character.manualViseme, 1.0f)
        }

        if (!character.lipSyncEnabled) {
            return Pair(VisemeType.CLOSED, 0f)
        }

        // Find assigned audio track or any active voice-over track
        val track = audioTracks.firstOrNull { it.id == character.assignedAudioTrackId }
            ?: audioTracks.firstOrNull { it.isVoiceOver && currentTimeMs in it.startTimeMs..(it.startTimeMs + it.durationMs) }

        if (track == null || currentTimeMs < track.startTimeMs || currentTimeMs > track.startTimeMs + track.durationMs) {
            return Pair(VisemeType.CLOSED, 0f)
        }

        val relativeTimeMs = currentTimeMs - track.startTimeMs
        val amplitude = getAmplitudeAtTime(track, relativeTimeMs) * character.lipSyncSensitivity

        if (amplitude < 0.08f) {
            return Pair(VisemeType.CLOSED, 0f)
        }

        // Modulation to create realistic phonetic viseme transitions based on speech cadence
        val cycle = (relativeTimeMs / 120.0).toInt() % 7
        val modPhase = sin(relativeTimeMs / 80.0).toFloat()

        val viseme = when {
            amplitude > 0.75f -> {
                if (modPhase > 0.2f) VisemeType.WIDE_OPEN else VisemeType.A
            }
            amplitude > 0.45f -> {
                when (cycle) {
                    0, 3 -> VisemeType.OPEN
                    1 -> VisemeType.O
                    2 -> VisemeType.E
                    4 -> VisemeType.A
                    5 -> VisemeType.SMILE
                    else -> VisemeType.FV
                }
            }
            amplitude > 0.20f -> {
                when (cycle % 4) {
                    0 -> VisemeType.OPEN
                    1 -> VisemeType.E
                    2 -> VisemeType.O
                    else -> VisemeType.MBP
                }
            }
            else -> {
                if (modPhase > 0f) VisemeType.MBP else VisemeType.CLOSED
            }
        }

        val openFactor = (amplitude.coerceIn(0f, 1f))
        return Pair(viseme, openFactor)
    }

    private fun getAmplitudeAtTime(track: AudioTrackData, relativeTimeMs: Long): Float {
        if (track.amplitudes.isEmpty() || track.durationMs <= 0) return 0.25f // Fallback speaking vibe
        val progress = (relativeTimeMs.toFloat() / track.durationMs.toFloat()).coerceIn(0f, 1f)
        val index = (progress * (track.amplitudes.size - 1)).toInt()
        return track.amplitudes.getOrElse(index) { 0f }
    }
}
