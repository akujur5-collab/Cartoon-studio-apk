package com.example

import com.example.data.engine.LipSyncEngine
import com.example.data.engine.MovementEvaluator
import com.example.data.model.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun lipSync_evaluatesCorrectVisemes() {
        val audioTrack = AudioTrackData(
            id = "audio_1",
            startTimeMs = 0L,
            durationMs = 2000L,
            amplitudes = listOf(0.0f, 0.1f, 0.5f, 0.9f, 0.0f)
        )
        val char = CharacterLayerData(
            assignedAudioTrackId = "audio_1",
            lipSyncEnabled = true
        )

        // Silent region (amp 0.0)
        val (silentViseme, _) = LipSyncEngine.evaluateViseme(char, listOf(audioTrack), 0L)
        assertEquals(VisemeType.CLOSED, silentViseme)

        // Loud region (amp 0.9)
        val (loudViseme, factor) = LipSyncEngine.evaluateViseme(char, listOf(audioTrack), 1500L)
        assertTrue(loudViseme == VisemeType.WIDE_OPEN || loudViseme == VisemeType.A)
        assertTrue(factor > 0.5f)
    }

    @Test
    fun movementEvaluator_handlesBounceAndShake() {
        val char = CharacterLayerData(
            movement = MovementData(
                preset = MovementPreset.BOUNCE,
                startX = 0.5f,
                startY = 0.5f,
                startTimeMs = 0L,
                durationMs = 2000L
            )
        )
        val eval = MovementEvaluator.evaluate(char, emptyList(), 1000L)
        assertEquals(0.5f, eval.x, 0.01f)
        assertTrue(eval.alpha == 1.0f)
    }
}

