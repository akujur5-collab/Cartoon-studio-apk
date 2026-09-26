package com.example.data.engine

import com.example.data.model.*
import kotlin.math.*

data class EvaluatedTransform(
    val x: Float, // Normalized 0..1 (center anchor)
    val y: Float, // Normalized 0..1
    val scaleX: Float,
    val scaleY: Float,
    val rotation: Float,
    val alpha: Float,
    val glowIntensity: Float = 0f
)

object MovementEvaluator {

    fun evaluate(
        character: CharacterLayerData,
        effects: List<EffectData>,
        currentTimeMs: Long
    ): EvaluatedTransform {
        val movement = character.movement
        val startTime = movement.startTimeMs
        val duration = movement.durationMs.coerceAtLeast(100L)

        // Raw progress 0..1
        val rawProgress = if (currentTimeMs < startTime) {
            0f
        } else if (currentTimeMs >= startTime + duration) {
            1f
        } else {
            (currentTimeMs - startTime).toFloat() / duration.toFloat()
        }

        val easedProgress = applyEasing(rawProgress, movement.easing)

        var posX: Float
        var posY: Float
        var scale: Float = movement.startScale + (movement.endScale - movement.startScale) * easedProgress
        var rot: Float = movement.startRotation + (movement.endRotation - movement.startRotation) * easedProgress

        when (movement.preset) {
            MovementPreset.STATIC -> {
                posX = movement.startX
                posY = movement.startY
            }
            MovementPreset.LEFT_TO_RIGHT,
            MovementPreset.RIGHT_TO_LEFT,
            MovementPreset.TOP_TO_BOTTOM,
            MovementPreset.BOTTOM_TO_TOP -> {
                posX = movement.startX + (movement.endX - movement.startX) * easedProgress
                posY = movement.startY + (movement.endY - movement.startY) * easedProgress
            }
            MovementPreset.ZOOM_IN -> {
                posX = movement.startX
                posY = movement.startY
                scale = movement.startScale + 0.6f * easedProgress
            }
            MovementPreset.ZOOM_OUT -> {
                posX = movement.startX
                posY = movement.startY
                scale = (movement.startScale - 0.4f * easedProgress).coerceAtLeast(0.2f)
            }
            MovementPreset.BOUNCE -> {
                posX = movement.startX
                val bounceOffset = abs(sin(rawProgress * Math.PI.toFloat() * 3f)) * 0.08f
                posY = movement.startY - bounceOffset
                scale = movement.startScale * (1f + 0.05f * sin(rawProgress * Math.PI.toFloat() * 6f))
            }
            MovementPreset.SHAKE -> {
                val shakeX = sin(rawProgress * 45f) * 0.02f
                val shakeY = cos(rawProgress * 35f) * 0.015f
                posX = movement.startX + shakeX
                posY = movement.startY + shakeY
                rot = movement.startRotation + sin(rawProgress * 40f) * 4f
            }
            MovementPreset.CUSTOM_WAYPOINTS -> {
                if (movement.waypoints.isEmpty()) {
                    posX = movement.startX
                    posY = movement.startY
                } else {
                    val allPoints = listOf(PointFData(movement.startX, movement.startY)) + movement.waypoints
                    val totalSegments = allPoints.size - 1
                    val segmentProgress = easedProgress * totalSegments
                    val segIndex = segmentProgress.toInt().coerceIn(0, totalSegments - 1)
                    val t = segmentProgress - segIndex
                    val p1 = allPoints[segIndex]
                    val p2 = allPoints[segIndex + 1]
                    posX = p1.x + (p2.x - p1.x) * t
                    posY = p1.y + (p2.y - p1.y) * t
                }
            }
        }

        // Apply active effects for this character or scene-wide
        var alpha = 1.0f
        var glow = 0.0f

        val activeEffects = effects.filter {
            (it.targetCharacterId == null || it.targetCharacterId == character.id) &&
                    currentTimeMs in it.startTimeMs..(it.startTimeMs + it.durationMs)
        }

        for (effect in activeEffects) {
            val effProg = ((currentTimeMs - effect.startTimeMs).toFloat() / effect.durationMs.toFloat()).coerceIn(0f, 1f)
            when (effect.type) {
                EffectType.FADE_IN -> {
                    alpha *= effProg
                }
                EffectType.FADE_OUT -> {
                    alpha *= (1f - effProg)
                }
                EffectType.ZOOM_PULSE -> {
                    val pulse = sin(effProg * Math.PI.toFloat() * 4f) * 0.15f * effect.intensity
                    scale *= (1f + pulse)
                }
                EffectType.SHAKE -> {
                    posX += sin(effProg * 50f) * 0.03f * effect.intensity
                    posY += cos(effProg * 40f) * 0.02f * effect.intensity
                    rot += sin(effProg * 45f) * 5f * effect.intensity
                }
                EffectType.BOUNCE -> {
                    posY -= abs(sin(effProg * Math.PI.toFloat() * 4f)) * 0.10f * effect.intensity
                }
                EffectType.GLOW -> {
                    glow = (0.5f + 0.5f * sin(effProg * Math.PI.toFloat() * 3f)) * effect.intensity
                }
                EffectType.SPARKLE -> {
                    glow = (0.3f + 0.7f * abs(sin(effProg * 12f))) * effect.intensity
                }
                else -> {}
            }
        }

        val flipMult = if (character.isFlippedH) -1f else 1f
        return EvaluatedTransform(
            x = posX.coerceIn(-0.5f, 1.5f),
            y = posY.coerceIn(-0.5f, 1.5f),
            scaleX = scale * flipMult,
            scaleY = scale,
            rotation = rot,
            alpha = alpha.coerceIn(0f, 1f),
            glowIntensity = glow
        )
    }

    private fun applyEasing(t: Float, type: EasingType): Float {
        return when (type) {
            EasingType.LINEAR -> t
            EasingType.EASE_IN_OUT -> {
                if (t < 0.5f) 2f * t * t else 1f - (-2f * t + 2f).pow(2) / 2f
            }
            EasingType.BOUNCE -> {
                val n1 = 7.5625f
                val d1 = 2.75f
                var x = t
                if (x < 1 / d1) {
                    n1 * x * x
                } else if (x < 2 / d1) {
                    x -= 1.5f / d1
                    n1 * x * x + 0.75f
                } else if (x < 2.5 / d1) {
                    x -= 2.25f / d1
                    n1 * x * x + 0.9375f
                } else {
                    x -= 2.625f / d1
                    n1 * x * x + 0.984375f
                }
            }
            EasingType.ELASTIC -> {
                val c4 = (2 * Math.PI) / 3
                when {
                    t == 0f -> 0f
                    t == 1f -> 1f
                    else -> -(2.0.pow((10 * t - 10).toDouble()).toFloat() * sin((t * 10f - 10.75f) * c4).toFloat())
                }
            }
        }
    }
}
