package com.example.data.engine

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.model.VisemeType

object MouthShapeRenderer {

    // Colors for cartoon mouth elements
    val CavityColor = Color(0xFF1E1B4B)
    val LipColor = Color(0xFFB91C1C)
    val TeethColor = Color(0xFFFFFFFF)
    val TongueColor = Color(0xFFFB7185)

    /**
     * Draws the viseme mouth shape on Compose DrawScope.
     * @param center center coordinate of the mouth
     * @param baseWidth standard width of mouth
     * @param baseHeight standard height of mouth
     */
    fun drawMouthCompose(
        drawScope: DrawScope,
        viseme: VisemeType,
        center: Offset,
        baseWidth: Float,
        baseHeight: Float,
        openFactor: Float = 1.0f
    ) {
        with(drawScope) {
            val w = baseWidth
            val h = (baseHeight * (0.4f + 0.6f * openFactor)).coerceAtLeast(6f)

            when (viseme) {
                VisemeType.CLOSED -> {
                    // Slight gentle smile curve
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(center.x - w * 0.45f, center.y)
                        quadraticTo(center.x, center.y + h * 0.25f, center.x + w * 0.45f, center.y)
                    }
                    drawPath(path, LipColor, style = Stroke(width = 4f))
                }

                VisemeType.SMILE -> {
                    // Crescent grin with teeth
                    val cavityPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(center.x - w * 0.5f, center.y - h * 0.1f)
                        quadraticTo(center.x, center.y + h * 0.8f, center.x + w * 0.5f, center.y - h * 0.1f)
                        quadraticTo(center.x, center.y + h * 0.1f, center.x - w * 0.5f, center.y - h * 0.1f)
                        close()
                    }
                    drawPath(cavityPath, CavityColor, style = Fill)

                    // Teeth top row
                    drawArc(
                        color = TeethColor,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(center.x - w * 0.35f, center.y - h * 0.15f),
                        size = Size(w * 0.7f, h * 0.4f)
                    )

                    // Lip stroke
                    drawPath(cavityPath, LipColor, style = Stroke(width = 3.5f))
                }

                VisemeType.O -> {
                    // Circular 'O' mouth
                    val radius = (w * 0.32f).coerceAtLeast(8f)
                    drawCircle(CavityColor, radius = radius, center = center)
                    // Tongue at bottom of O
                    drawCircle(TongueColor, radius = radius * 0.5f, center = Offset(center.x, center.y + radius * 0.4f))
                    drawCircle(LipColor, radius = radius, center = center, style = Stroke(width = 3.5f))
                }

                VisemeType.E -> {
                    // Stretched wide grin with visible upper and lower teeth
                    val rect = androidx.compose.ui.geometry.Rect(
                        center.x - w * 0.55f,
                        center.y - h * 0.35f,
                        center.x + w * 0.55f,
                        center.y + h * 0.35f
                    )
                    drawRoundRect(CavityColor, topLeft = rect.topLeft, size = rect.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f))
                    // Teeth row
                    drawRect(
                        TeethColor,
                        topLeft = Offset(rect.left + 4f, rect.top + 2f),
                        size = Size(rect.width - 8f, rect.height * 0.35f)
                    )
                    drawRect(
                        TeethColor,
                        topLeft = Offset(rect.left + 4f, rect.bottom - rect.height * 0.35f - 2f),
                        size = Size(rect.width - 8f, rect.height * 0.35f)
                    )
                    drawRoundRect(LipColor, topLeft = rect.topLeft, size = rect.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f), style = Stroke(width = 3.5f))
                }

                VisemeType.FV -> {
                    // Top teeth biting down on bottom lip
                    val cavityPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(center.x - w * 0.45f, center.y - h * 0.2f)
                        quadraticTo(center.x, center.y + h * 0.4f, center.x + w * 0.45f, center.y - h * 0.2f)
                        close()
                    }
                    drawPath(cavityPath, CavityColor, style = Fill)
                    // Two prominent front teeth
                    drawRect(
                        TeethColor,
                        topLeft = Offset(center.x - w * 0.2f, center.y - h * 0.2f),
                        size = Size(w * 0.4f, h * 0.35f)
                    )
                    drawPath(cavityPath, LipColor, style = Stroke(width = 3.5f))
                }

                VisemeType.MBP -> {
                    // Compressed straight lip line
                    drawLine(
                        LipColor,
                        start = Offset(center.x - w * 0.42f, center.y),
                        end = Offset(center.x + w * 0.42f, center.y),
                        strokeWidth = 5f
                    )
                }

                VisemeType.WIDE_OPEN -> {
                    // Huge expressive cartoon exclamation mouth
                    val cavityPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(center.x - w * 0.52f, center.y - h * 0.5f)
                        quadraticTo(center.x, center.y - h * 0.6f, center.x + w * 0.52f, center.y - h * 0.5f)
                        quadraticTo(center.x + w * 0.45f, center.y + h * 0.7f, center.x, center.y + h * 0.75f)
                        quadraticTo(center.x - w * 0.45f, center.y + h * 0.7f, center.x - w * 0.52f, center.y - h * 0.5f)
                        close()
                    }
                    drawPath(cavityPath, CavityColor, style = Fill)

                    // Top teeth
                    drawArc(
                        color = TeethColor,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(center.x - w * 0.4f, center.y - h * 0.55f),
                        size = Size(w * 0.8f, h * 0.35f)
                    )
                    // Big round tongue
                    drawCircle(
                        TongueColor,
                        radius = w * 0.28f,
                        center = Offset(center.x, center.y + h * 0.42f)
                    )
                    drawPath(cavityPath, LipColor, style = Stroke(width = 4f))
                }

                VisemeType.A, VisemeType.OPEN -> {
                    // Natural speech opening with rounded bottom & upper teeth
                    val cavityPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(center.x - w * 0.48f, center.y - h * 0.35f)
                        quadraticTo(center.x, center.y - h * 0.4f, center.x + w * 0.48f, center.y - h * 0.35f)
                        quadraticTo(center.x + w * 0.42f, center.y + h * 0.55f, center.x, center.y + h * 0.6f)
                        quadraticTo(center.x - w * 0.42f, center.y + h * 0.55f, center.x - w * 0.48f, center.y - h * 0.35f)
                        close()
                    }
                    drawPath(cavityPath, CavityColor, style = Fill)

                    // Top teeth
                    drawArc(
                        color = TeethColor,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(center.x - w * 0.35f, center.y - h * 0.4f),
                        size = Size(w * 0.7f, h * 0.3f)
                    )
                    // Tongue
                    drawCircle(
                        TongueColor,
                        radius = w * 0.22f,
                        center = Offset(center.x, center.y + h * 0.35f)
                    )
                    drawPath(cavityPath, LipColor, style = Stroke(width = 3.5f))
                }
            }
        }
    }

    /**
     * Draws the viseme mouth shape on Android Canvas (for hardware video export).
     */
    fun drawMouthAndroidCanvas(
        canvas: Canvas,
        viseme: VisemeType,
        cx: Float,
        cy: Float,
        baseWidth: Float,
        baseHeight: Float,
        openFactor: Float = 1.0f
    ) {
        val w = baseWidth
        val h = (baseHeight * (0.4f + 0.6f * openFactor)).coerceAtLeast(6f)

        val cavityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#1E1B4B")
            style = Paint.Style.FILL
        }
        val lipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#B91C1C")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val teethPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        val tonguePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#FB7185")
            style = Paint.Style.FILL
        }

        when (viseme) {
            VisemeType.CLOSED -> {
                val path = Path().apply {
                    moveTo(cx - w * 0.45f, cy)
                    quadTo(cx, cy + h * 0.25f, cx + w * 0.45f, cy)
                }
                canvas.drawPath(path, lipPaint)
            }
            VisemeType.O -> {
                val radius = (w * 0.32f).coerceAtLeast(8f)
                canvas.drawCircle(cx, cy, radius, cavityPaint)
                canvas.drawCircle(cx, cy + radius * 0.4f, radius * 0.5f, tonguePaint)
                canvas.drawCircle(cx, cy, radius, lipPaint)
            }
            VisemeType.MBP -> {
                lipPaint.strokeWidth = 5.5f
                canvas.drawLine(cx - w * 0.42f, cy, cx + w * 0.42f, cy, lipPaint)
            }
            else -> {
                // Open / Wide Open / Smile / A / E / FV
                val path = Path().apply {
                    moveTo(cx - w * 0.5f, cy - h * 0.35f)
                    quadTo(cx, cy - h * 0.45f, cx + w * 0.5f, cy - h * 0.35f)
                    quadTo(cx + w * 0.45f, cy + h * 0.6f, cx, cy + h * 0.65f)
                    quadTo(cx - w * 0.45f, cy + h * 0.6f, cx - w * 0.5f, cy - h * 0.35f)
                    close()
                }
                canvas.drawPath(path, cavityPaint)

                // Teeth
                val teethRect = RectF(cx - w * 0.35f, cy - h * 0.45f, cx + w * 0.35f, cy - h * 0.1f)
                canvas.drawRoundRect(teethRect, 6f, 6f, teethPaint)

                // Tongue
                canvas.drawCircle(cx, cy + h * 0.35f, w * 0.22f, tonguePaint)
                canvas.drawPath(path, lipPaint)
            }
        }
    }
}
