package com.example.data.engine

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.example.data.model.BackgroundType
import com.example.data.model.CharacterType
import com.example.data.model.VisemeType
import java.io.InputStream
import kotlin.math.sin

object CartoonGraphicHelper {

    // Cache loaded bitmaps
    private val bitmapCache = mutableMapOf<String, Bitmap>()

    fun loadBitmap(context: Context, uriStr: String): Bitmap? {
        if (bitmapCache.containsKey(uriStr)) return bitmapCache[uriStr]
        return try {
            val uri = Uri.parse(uriStr)
            val input: InputStream? = context.contentResolver.openInputStream(uri)
            val bmp = BitmapFactory.decodeStream(input)
            input?.close()
            if (bmp != null) {
                bitmapCache[uriStr] = bmp
            }
            bmp
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Draws background in Compose DrawScope
     */
    fun drawBackgroundCompose(
        drawScope: DrawScope,
        type: BackgroundType,
        colorHex: Long,
        width: Float,
        height: Float,
        customBitmap: Bitmap? = null
    ) {
        with(drawScope) {
            when (type) {
                BackgroundType.CUSTOM_IMAGE -> {
                    if (customBitmap != null) {
                        drawContext.canvas.nativeCanvas.drawBitmap(
                            customBitmap,
                            null,
                            RectF(0f, 0f, width, height),
                            null
                        )
                    } else {
                        drawRect(Color(0xFF1E293B), size = Size(width, height))
                    }
                }
                BackgroundType.SOLID_COLOR -> {
                    drawRect(Color(colorHex), size = Size(width, height))
                }
                BackgroundType.PRESET_CITY -> {
                    // Cartoon City Sunset
                    // Sky gradient: coral pink to deep purple
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color(0xFF0F172A), Color(0xFF4338CA), Color(0xFFBE185D), Color(0xFFF97316)),
                            startY = 0f,
                            endY = height * 0.75f
                        ),
                        size = Size(width, height)
                    )
                    // Big glowing warm sun
                    drawCircle(
                        color = Color(0xFFFDE047),
                        radius = width * 0.18f,
                        center = Offset(width * 0.75f, height * 0.48f)
                    )
                    // Distant skyline
                    drawSkyline(this, width, height, 0.58f, Color(0xFF312E81), 7)
                    // Midground skyline with windows
                    drawSkylineWithWindows(this, width, height, 0.68f, Color(0xFF1E1B4B), 5)
                    // Foreground street
                    drawRect(Color(0xFF0F172A), topLeft = Offset(0f, height * 0.82f), size = Size(width, height * 0.18f))
                    // Street lights / curb line
                    drawLine(Color(0xFFFACC15), start = Offset(0f, height * 0.90f), end = Offset(width, height * 0.90f), strokeWidth = 8f)
                }
                BackgroundType.PRESET_SPACE -> {
                    // Cosmic Space
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color(0xFF030712), Color(0xFF111827), Color(0xFF3B0764)),
                            startY = 0f,
                            endY = height
                        ),
                        size = Size(width, height)
                    )
                    // Stars
                    for (i in 0..35) {
                        val sx = (sin(i * 13.7f) * 0.5f + 0.5f) * width
                        val sy = (sin(i * 37.1f) * 0.5f + 0.5f) * height
                        val r = (i % 3 + 2).toFloat()
                        drawCircle(Color.White.copy(alpha = 0.85f), radius = r, center = Offset(sx, sy))
                    }
                    // Big Saturn ring planet
                    val planetCenter = Offset(width * 0.28f, height * 0.28f)
                    drawCircle(Color(0xFFE11D48), radius = width * 0.15f, center = planetCenter)
                    drawArc(
                        Color(0xFFFBBF24),
                        startAngle = -20f,
                        sweepAngle = 220f,
                        useCenter = false,
                        topLeft = Offset(planetCenter.x - width * 0.25f, planetCenter.y - width * 0.08f),
                        size = Size(width * 0.5f, width * 0.16f),
                        style = Stroke(width = 12f)
                    )
                    // Moon surface ground
                    drawArc(
                        Color(0xFF1F2937),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(-width * 0.2f, height * 0.80f),
                        size = Size(width * 1.4f, height * 0.4f)
                    )
                }
                BackgroundType.PRESET_FOREST -> {
                    // Enchanted Forest
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF34D399)),
                            startY = 0f,
                            endY = height * 0.7f
                        ),
                        size = Size(width, height)
                    )
                    // Cartoon hills
                    drawCircle(Color(0xFF065F46), radius = width * 0.7f, center = Offset(width * 0.1f, height * 0.85f))
                    drawCircle(Color(0xFF047857), radius = width * 0.8f, center = Offset(width * 0.85f, height * 0.88f))
                    // Ground
                    drawRect(Color(0xFF064E3B), topLeft = Offset(0f, height * 0.82f), size = Size(width, height * 0.18f))
                }
                BackgroundType.PRESET_STUDIO -> {
                    // Studio Room
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
                            startY = 0f,
                            endY = height * 0.75f
                        ),
                        size = Size(width, height)
                    )
                    // Studio spotlight beam
                    val spotPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(width * 0.5f, 0f)
                        lineTo(width * 0.15f, height * 0.82f)
                        lineTo(width * 0.85f, height * 0.82f)
                        close()
                    }
                    drawPath(spotPath, Color.White.copy(alpha = 0.08f), style = Fill)
                    // Wooden stage floor
                    drawRect(Color(0xFF78350F), topLeft = Offset(0f, height * 0.78f), size = Size(width, height * 0.22f))
                    drawLine(Color(0xFFB45309), start = Offset(0f, height * 0.78f), end = Offset(width, height * 0.78f), strokeWidth = 6f)
                }
                BackgroundType.PRESET_SUNSET -> {
                    // Tropical Sunset
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color(0xFF4C1D95), Color(0xFF9333EA), Color(0xFFF43F5E), Color(0xFFFBBF24)),
                            startY = 0f,
                            endY = height * 0.75f
                        ),
                        size = Size(width, height)
                    )
                    drawCircle(Color(0xFFFEF08A), radius = width * 0.22f, center = Offset(width * 0.5f, height * 0.65f))
                    drawRect(Color(0xFF1E1B4B), topLeft = Offset(0f, height * 0.75f), size = Size(width, height * 0.25f))
                }
            }
        }
    }

    private fun drawSkyline(drawScope: DrawScope, w: Float, h: Float, topRatio: Float, color: Color, count: Int) {
        val bW = w / count
        for (i in 0 until count) {
            val bH = h * (0.15f + (sin(i * 1.8f) * 0.5f + 0.5f) * 0.15f)
            drawScope.drawRect(
                color = color,
                topLeft = Offset(i * bW, h * topRatio - bH + (h * 0.24f)),
                size = Size(bW - 2f, bH + (h * 0.3f))
            )
        }
    }

    private fun drawSkylineWithWindows(drawScope: DrawScope, w: Float, h: Float, topRatio: Float, color: Color, count: Int) {
        val bW = w / count
        for (i in 0 until count) {
            val bH = h * (0.20f + (sin(i * 2.3f) * 0.5f + 0.5f) * 0.18f)
            val topY = h * topRatio - bH + (h * 0.20f)
            drawScope.drawRect(
                color = color,
                topLeft = Offset(i * bW, topY),
                size = Size(bW - 4f, bH + (h * 0.3f))
            )
            // Windows
            for (row in 0..4) {
                val winY = topY + 20f + row * 24f
                if (winY < h * 0.82f) {
                    drawScope.drawRect(
                        color = if ((i + row) % 2 == 0) Color(0xFFFDE047).copy(alpha = 0.75f) else Color(0xFF38BDF8).copy(alpha = 0.5f),
                        topLeft = Offset(i * bW + 12f, winY),
                        size = Size(10f, 14f)
                    )
                }
            }
        }
    }

    /**
     * Draws a cartoon character (Preset or Custom Bitmap) in Compose DrawScope
     */
    fun drawCharacterCompose(
        drawScope: DrawScope,
        type: CharacterType,
        customBitmap: Bitmap?,
        center: Offset,
        size: Float,
        viseme: VisemeType,
        mouthFactor: Float,
        mouthAnchorX: Float,
        mouthAnchorY: Float,
        mouthScale: Float,
        alpha: Float = 1.0f,
        glowIntensity: Float = 0f
    ) {
        with(drawScope) {
            val left = center.x - size / 2f
            val top = center.y - size / 2f

            if (glowIntensity > 0.05f) {
                drawCircle(
                    color = Color(0xFFFBBF24).copy(alpha = (glowIntensity * 0.35f * alpha).coerceIn(0f, 0.8f)),
                    radius = size * 0.65f,
                    center = center
                )
            }

            when (type) {
                CharacterType.CUSTOM_IMAGE -> {
                    if (customBitmap != null) {
                        val paint = Paint().apply { this.alpha = (alpha * 255).toInt().coerceIn(0, 255) }
                        drawContext.canvas.nativeCanvas.drawBitmap(
                            customBitmap,
                            null,
                            RectF(left, top, left + size, top + size),
                            paint
                        )
                    } else {
                        // Placeholder friendly shape
                        drawCircle(Color(0xFF6366F1).copy(alpha = alpha), radius = size * 0.4f, center = center)
                    }
                }
                CharacterType.PRESET_DINO -> {
                    drawCartoonDino(this, left, top, size, alpha)
                }
                CharacterType.PRESET_ROBOT -> {
                    drawCartoonRobot(this, left, top, size, alpha)
                }
                CharacterType.PRESET_CAT -> {
                    drawCartoonCat(this, left, top, size, alpha)
                }
                CharacterType.PRESET_BOB -> {
                    drawCartoonBob(this, left, top, size, alpha)
                }
            }

            // Draw Viseme Mouth
            val mouthCenterX = left + size * mouthAnchorX
            val mouthCenterY = top + size * mouthAnchorY
            val baseMW = size * 0.22f * mouthScale
            val baseMH = size * 0.16f * mouthScale

            MouthShapeRenderer.drawMouthCompose(
                this,
                viseme = viseme,
                center = Offset(mouthCenterX, mouthCenterY),
                baseWidth = baseMW,
                baseHeight = baseMH,
                openFactor = mouthFactor
            )
        }
    }

    private fun drawCartoonDino(drawScope: DrawScope, l: Float, t: Float, s: Float, a: Float) {
        with(drawScope) {
            val bodyColor = Color(0xFF10B981).copy(alpha = a)
            val bellyColor = Color(0xFF6EE7B7).copy(alpha = a)
            val spikeColor = Color(0xFFF59E0B).copy(alpha = a)

            // Back spikes
            for (i in 0..3) {
                val spikePath = androidx.compose.ui.graphics.Path().apply {
                    val sx = l + s * 0.28f - i * s * 0.05f
                    val sy = t + s * (0.32f + i * 0.10f)
                    moveTo(sx, sy)
                    lineTo(sx - s * 0.08f, sy - s * 0.04f)
                    lineTo(sx, sy + s * 0.07f)
                    close()
                }
                drawPath(spikePath, spikeColor, style = Fill)
            }

            // Tail
            val tailPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(l + s * 0.35f, t + s * 0.65f)
                quadraticTo(l + s * 0.12f, t + s * 0.72f, l + s * 0.08f, t + s * 0.60f)
                quadraticTo(l + s * 0.18f, t + s * 0.78f, l + s * 0.40f, t + s * 0.78f)
                close()
            }
            drawPath(tailPath, bodyColor, style = Fill)

            // Big Chubby Head & Body
            drawRoundRect(
                bodyColor,
                topLeft = Offset(l + s * 0.30f, t + s * 0.22f),
                size = Size(s * 0.44f, s * 0.56f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.20f, s * 0.20f)
            )

            // Cute yellow/green belly
            drawRoundRect(
                bellyColor,
                topLeft = Offset(l + s * 0.40f, t + s * 0.46f),
                size = Size(s * 0.26f, s * 0.28f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.12f, s * 0.12f)
            )

            // Dinosaur Feet
            drawRoundRect(
                bodyColor,
                topLeft = Offset(l + s * 0.34f, t + s * 0.74f),
                size = Size(s * 0.14f, s * 0.10f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.05f, s * 0.05f)
            )
            drawRoundRect(
                bodyColor,
                topLeft = Offset(l + s * 0.52f, t + s * 0.74f),
                size = Size(s * 0.14f, s * 0.10f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.05f, s * 0.05f)
            )

            // Big expressive eyes
            val eyeWhite = Color.White.copy(alpha = a)
            val eyePupil = Color(0xFF0F172A).copy(alpha = a)
            val eyeY = t + s * 0.36f

            // Left eye
            drawCircle(eyeWhite, radius = s * 0.075f, center = Offset(l + s * 0.42f, eyeY))
            drawCircle(eyePupil, radius = s * 0.045f, center = Offset(l + s * 0.44f, eyeY))
            drawCircle(eyeWhite, radius = s * 0.018f, center = Offset(l + s * 0.455f, eyeY - s * 0.015f))

            // Right eye
            drawCircle(eyeWhite, radius = s * 0.075f, center = Offset(l + s * 0.60f, eyeY))
            drawCircle(eyePupil, radius = s * 0.045f, center = Offset(l + s * 0.62f, eyeY))
            drawCircle(eyeWhite, radius = s * 0.018f, center = Offset(l + s * 0.635f, eyeY - s * 0.015f))

            // Pink cheeks
            val blush = Color(0xFFF472B6).copy(alpha = 0.7f * a)
            drawCircle(blush, radius = s * 0.04f, center = Offset(l + s * 0.35f, t + s * 0.46f))
            drawCircle(blush, radius = s * 0.04f, center = Offset(l + s * 0.68f, t + s * 0.46f))
        }
    }

    private fun drawCartoonRobot(drawScope: DrawScope, l: Float, t: Float, s: Float, a: Float) {
        with(drawScope) {
            val metal = Color(0xFF64748B).copy(alpha = a)
            val darkMetal = Color(0xFF334155).copy(alpha = a)
            val cyanGlow = Color(0xFF38BDF8).copy(alpha = a)
            val yellowAcc = Color(0xFFFBBF24).copy(alpha = a)

            // Antenna
            drawLine(metal, start = Offset(l + s * 0.5f, t + s * 0.16f), end = Offset(l + s * 0.5f, t + s * 0.25f), strokeWidth = 6f)
            drawCircle(yellowAcc, radius = s * 0.045f, center = Offset(l + s * 0.5f, t + s * 0.15f))

            // Ears/bolts
            drawRect(darkMetal, topLeft = Offset(l + s * 0.22f, t + s * 0.32f), size = Size(s * 0.06f, s * 0.10f))
            drawRect(darkMetal, topLeft = Offset(l + s * 0.72f, t + s * 0.32f), size = Size(s * 0.06f, s * 0.10f))

            // Head (rounded box)
            drawRoundRect(
                metal,
                topLeft = Offset(l + s * 0.26f, t + s * 0.25f),
                size = Size(s * 0.48f, s * 0.36f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.08f, s * 0.08f)
            )

            // Body
            drawRoundRect(
                darkMetal,
                topLeft = Offset(l + s * 0.30f, t + s * 0.63f),
                size = Size(s * 0.40f, s * 0.22f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.05f, s * 0.05f)
            )

            // Visor screen / Eye panel
            drawRoundRect(
                Color(0xFF0F172A).copy(alpha = a),
                topLeft = Offset(l + s * 0.32f, t + s * 0.32f),
                size = Size(s * 0.36f, s * 0.14f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.04f, s * 0.04f)
            )

            // Glowing cyan robot eyes
            drawCircle(cyanGlow, radius = s * 0.04f, center = Offset(l + s * 0.40f, t + s * 0.39f))
            drawCircle(cyanGlow, radius = s * 0.04f, center = Offset(l + s * 0.60f, t + s * 0.39f))
        }
    }

    private fun drawCartoonCat(drawScope: DrawScope, l: Float, t: Float, s: Float, a: Float) {
        with(drawScope) {
            val fur = Color(0xFFF97316).copy(alpha = a)
            val innerEar = Color(0xFFFCA5A5).copy(alpha = a)

            // Left ear
            val earL = androidx.compose.ui.graphics.Path().apply {
                moveTo(l + s * 0.32f, t + s * 0.32f)
                lineTo(l + s * 0.32f, t + s * 0.18f)
                lineTo(l + s * 0.46f, t + s * 0.28f)
                close()
            }
            drawPath(earL, fur, style = Fill)
            val innerEarL = androidx.compose.ui.graphics.Path().apply {
                moveTo(l + s * 0.34f, t + s * 0.30f)
                lineTo(l + s * 0.34f, t + s * 0.22f)
                lineTo(l + s * 0.43f, t + s * 0.28f)
                close()
            }
            drawPath(innerEarL, innerEar, style = Fill)

            // Right ear
            val earR = androidx.compose.ui.graphics.Path().apply {
                moveTo(l + s * 0.68f, t + s * 0.32f)
                lineTo(l + s * 0.68f, t + s * 0.18f)
                lineTo(l + s * 0.54f, t + s * 0.28f)
                close()
            }
            drawPath(earR, fur, style = Fill)
            val innerEarR = androidx.compose.ui.graphics.Path().apply {
                moveTo(l + s * 0.66f, t + s * 0.30f)
                lineTo(l + s * 0.66f, t + s * 0.22f)
                lineTo(l + s * 0.57f, t + s * 0.28f)
                close()
            }
            drawPath(innerEarR, innerEar, style = Fill)

            // Round Head & Body
            drawCircle(fur, radius = s * 0.24f, center = Offset(l + s * 0.5f, t + s * 0.44f))
            drawRoundRect(
                fur,
                topLeft = Offset(l + s * 0.34f, t + s * 0.60f),
                size = Size(s * 0.32f, s * 0.24f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.12f, s * 0.12f)
            )

            // Eyes
            val eyeWhite = Color.White.copy(alpha = a)
            val eyeIris = Color(0xFF10B981).copy(alpha = a)
            drawCircle(eyeWhite, radius = s * 0.065f, center = Offset(l + s * 0.41f, t + s * 0.40f))
            drawCircle(eyeIris, radius = s * 0.045f, center = Offset(l + s * 0.42f, t + s * 0.40f))
            drawCircle(eyeWhite, radius = s * 0.065f, center = Offset(l + s * 0.59f, t + s * 0.40f))
            drawCircle(eyeIris, radius = s * 0.045f, center = Offset(l + s * 0.58f, t + s * 0.40f))

            // Little nose
            drawCircle(Color(0xFFBE185D).copy(alpha = a), radius = s * 0.02f, center = Offset(l + s * 0.5f, t + s * 0.47f))

            // Whiskers
            val whisker = Color(0xFF78350F).copy(alpha = a)
            drawLine(whisker, start = Offset(l + s * 0.35f, t + s * 0.48f), end = Offset(l + s * 0.20f, t + s * 0.47f), strokeWidth = 3f)
            drawLine(whisker, start = Offset(l + s * 0.35f, t + s * 0.51f), end = Offset(l + s * 0.22f, t + s * 0.54f), strokeWidth = 3f)
            drawLine(whisker, start = Offset(l + s * 0.65f, t + s * 0.48f), end = Offset(l + s * 0.80f, t + s * 0.47f), strokeWidth = 3f)
            drawLine(whisker, start = Offset(l + s * 0.65f, t + s * 0.51f), end = Offset(l + s * 0.78f, t + s * 0.54f), strokeWidth = 3f)
        }
    }

    private fun drawCartoonBob(drawScope: DrawScope, l: Float, t: Float, s: Float, a: Float) {
        with(drawScope) {
            val skin = Color(0xFFFBBF24).copy(alpha = a)
            val hair = Color(0xFF451A03).copy(alpha = a)
            val shirt = Color(0xFF3B82F6).copy(alpha = a)

            // Cool cartoon boy hair
            drawRoundRect(
                hair,
                topLeft = Offset(l + s * 0.30f, t + s * 0.22f),
                size = Size(s * 0.40f, s * 0.20f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.08f, s * 0.08f)
            )

            // Head
            drawRoundRect(
                skin,
                topLeft = Offset(l + s * 0.32f, t + s * 0.28f),
                size = Size(s * 0.36f, s * 0.34f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.12f, s * 0.12f)
            )

            // T-shirt Body
            drawRoundRect(
                shirt,
                topLeft = Offset(l + s * 0.28f, t + s * 0.60f),
                size = Size(s * 0.44f, s * 0.25f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.06f, s * 0.06f)
            )

            // Eyes & Glasses
            val glasses = Color(0xFF1E293B).copy(alpha = a)
            drawCircle(glasses, radius = s * 0.07f, center = Offset(l + s * 0.41f, t + s * 0.42f), style = Stroke(width = 4f))
            drawCircle(glasses, radius = s * 0.07f, center = Offset(l + s * 0.59f, t + s * 0.42f), style = Stroke(width = 4f))
            drawLine(glasses, start = Offset(l + s * 0.48f, t + s * 0.42f), end = Offset(l + s * 0.52f, t + s * 0.42f), strokeWidth = 4f)

            drawCircle(Color(0xFF0F172A).copy(alpha = a), radius = s * 0.035f, center = Offset(l + s * 0.42f, t + s * 0.42f))
            drawCircle(Color(0xFF0F172A).copy(alpha = a), radius = s * 0.035f, center = Offset(l + s * 0.60f, t + s * 0.42f))
        }
    }

    /**
     * Draws background and characters onto Android Canvas (used during MP4 encoding)
     */
    fun drawFrameToAndroidCanvas(
        canvas: Canvas,
        width: Int,
        height: Int,
        bgType: BackgroundType,
        customBgBitmap: Bitmap?,
        characters: List<Triple<CharacterType, Bitmap?, EvaluatedTransform>>,
        visemes: List<Triple<VisemeType, Float, Pair<Float, Float>>> // viseme, factor, (mouthX, mouthY)
    ) {
        val w = width.toFloat()
        val h = height.toFloat()

        // Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        when (bgType) {
            BackgroundType.CUSTOM_IMAGE -> {
                if (customBgBitmap != null) {
                    canvas.drawBitmap(customBgBitmap, null, RectF(0f, 0f, w, h), bgPaint)
                } else {
                    bgPaint.color = android.graphics.Color.parseColor("#1E293B")
                    canvas.drawRect(0f, 0f, w, h, bgPaint)
                }
            }
            BackgroundType.PRESET_CITY -> {
                val shader = LinearGradient(0f, 0f, 0f, h * 0.75f,
                    intArrayOf(
                        android.graphics.Color.parseColor("#0F172A"),
                        android.graphics.Color.parseColor("#4338CA"),
                        android.graphics.Color.parseColor("#BE185D"),
                        android.graphics.Color.parseColor("#F97316")
                    ), null, Shader.TileMode.CLAMP)
                bgPaint.shader = shader
                canvas.drawRect(0f, 0f, w, h, bgPaint)
                bgPaint.shader = null

                // Sun
                bgPaint.color = android.graphics.Color.parseColor("#FDE047")
                canvas.drawCircle(w * 0.75f, h * 0.48f, w * 0.18f, bgPaint)

                // Foreground street
                bgPaint.color = android.graphics.Color.parseColor("#0F172A")
                canvas.drawRect(0f, h * 0.82f, w, h, bgPaint)
            }
            BackgroundType.PRESET_SPACE -> {
                bgPaint.color = android.graphics.Color.parseColor("#090D16")
                canvas.drawRect(0f, 0f, w, h, bgPaint)
                bgPaint.color = android.graphics.Color.parseColor("#E11D48")
                canvas.drawCircle(w * 0.28f, h * 0.28f, w * 0.15f, bgPaint)
            }
            else -> {
                bgPaint.color = android.graphics.Color.parseColor("#1E1B4B")
                canvas.drawRect(0f, 0f, w, h, bgPaint)
            }
        }

        // Draw Characters
        for (i in characters.indices) {
            val (cType, bmp, transform) = characters[i]
            val cx = transform.x * w
            val cy = transform.y * h
            val baseSize = w * 0.55f * transform.scaleY

            canvas.save()
            canvas.translate(cx, cy)
            canvas.rotate(transform.rotation)
            if (transform.scaleX < 0) {
                canvas.scale(-1f, 1f)
            }

            val cPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.alpha = (transform.alpha * 255).toInt().coerceIn(0, 255)
            }

            if (cType == CharacterType.CUSTOM_IMAGE && bmp != null) {
                canvas.drawBitmap(bmp, null, RectF(-baseSize / 2f, -baseSize / 2f, baseSize / 2f, baseSize / 2f), cPaint)
            } else {
                // Draw simplified fallback character on Android Canvas
                cPaint.color = android.graphics.Color.parseColor("#10B981")
                canvas.drawRoundRect(RectF(-baseSize * 0.22f, -baseSize * 0.28f, baseSize * 0.22f, baseSize * 0.28f), baseSize * 0.1f, baseSize * 0.1f, cPaint)
                // Eyes
                val eyeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
                canvas.drawCircle(-baseSize * 0.08f, -baseSize * 0.12f, baseSize * 0.04f, eyeP)
                canvas.drawCircle(baseSize * 0.08f, -baseSize * 0.12f, baseSize * 0.04f, eyeP)
            }

            // Draw Viseme Mouth
            val (viseme, factor, anchor) = visemes[i]
            val mx = (anchor.first - 0.5f) * baseSize
            val my = (anchor.second - 0.5f) * baseSize
            MouthShapeRenderer.drawMouthAndroidCanvas(
                canvas,
                viseme = viseme,
                cx = mx,
                cy = my,
                baseWidth = baseSize * 0.22f,
                baseHeight = baseSize * 0.15f,
                openFactor = factor
            )

            canvas.restore()
        }
    }
}
