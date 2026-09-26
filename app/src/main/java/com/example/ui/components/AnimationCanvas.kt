package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.engine.CartoonGraphicHelper
import com.example.data.engine.LipSyncEngine
import com.example.data.engine.MovementEvaluator
import com.example.data.model.*
import com.example.ui.viewmodel.StudioTab
import kotlin.math.hypot

@Composable
fun AnimationCanvas(
    project: Project,
    currentTimeMs: Long,
    selectedCharacterId: String?,
    activeTab: StudioTab,
    onSelectCharacter: (String?) -> Unit,
    onUpdateCharacterPosition: (String, Float, Float) -> Unit,
    onUpdateCharacterScale: (String, Float) -> Unit,
    onUpdateCharacterRotation: (String, Float) -> Unit,
    onUpdateMouthAnchor: (String, Float, Float) -> Unit,
    onLongPressCharacter: (CharacterLayerData) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val currentScene = remember(project, currentTimeMs) {
        project.scenes.firstOrNull {
            currentTimeMs in it.startTimeMs until (it.startTimeMs + it.durationMs)
        } ?: project.scenes.firstOrNull() ?: SceneData()
    }

    // Cache custom bitmaps for characters
    val charBitmaps = remember(currentScene) {
        val map = mutableMapOf<String, Bitmap?>()
        for (char in currentScene.characters) {
            if (char.imageUri != null) {
                map[char.id] = CartoonGraphicHelper.loadBitmap(context, char.imageUri)
            }
        }
        map
    }

    // Custom background bitmap
    val bgBitmap = remember(currentScene.backgroundCustomUri) {
        currentScene.backgroundCustomUri?.let { CartoonGraphicHelper.loadBitmap(context, it) }
    }

    var canvasWidth by remember { mutableFloatStateOf(1f) }
    var canvasHeight by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, Color(0xFF4F46E5).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A))
                .pointerInput(currentScene, selectedCharacterId, activeTab) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        val selectedChar = currentScene.characters.firstOrNull { it.id == selectedCharacterId }
                        if (selectedChar != null && !selectedChar.isLocked) {
                            if (activeTab == StudioTab.LIP_SYNC) {
                                // In lip sync tab, dragging moves the mouth anchor!
                                val newAnchorX = selectedChar.mouthAnchorX + (pan.x / (canvasWidth * 0.45f))
                                val newAnchorY = selectedChar.mouthAnchorY + (pan.y / (canvasWidth * 0.45f))
                                onUpdateMouthAnchor(selectedChar.id, newAnchorX, newAnchorY)
                            } else {
                                // Move character
                                val newX = (selectedChar.movement.startX + (pan.x / canvasWidth)).coerceIn(0.05f, 0.95f)
                                val newY = (selectedChar.movement.startY + (pan.y / canvasHeight)).coerceIn(0.05f, 0.95f)
                                onUpdateCharacterPosition(selectedChar.id, newX, newY)

                                if (zoom != 1f) {
                                    val newScale = (selectedChar.movement.startScale * zoom).coerceIn(0.3f, 2.5f)
                                    onUpdateCharacterScale(selectedChar.id, newScale)
                                }
                                if (rotation != 0f) {
                                    val newRot = (selectedChar.movement.startRotation + rotation) % 360f
                                    onUpdateCharacterRotation(selectedChar.id, newRot)
                                }
                            }
                        }
                    }
                }
                .pointerInput(currentScene) {
                    detectTapGestures(
                        onTap = { offset ->
                            // Hit test characters in reverse zIndex order
                            val hit = currentScene.characters.reversed().firstOrNull { char ->
                                val eval = MovementEvaluator.evaluate(char, currentScene.effects, currentTimeMs)
                                val cx = eval.x * canvasWidth
                                val cy = eval.y * canvasHeight
                                val charSize = canvasWidth * 0.55f * eval.scaleY
                                val dist = hypot(offset.x - cx, offset.y - cy)
                                dist <= charSize / 2f
                            }
                            onSelectCharacter(hit?.id)
                        },
                        onLongPress = { offset ->
                            val hit = currentScene.characters.reversed().firstOrNull { char ->
                                val eval = MovementEvaluator.evaluate(char, currentScene.effects, currentTimeMs)
                                val cx = eval.x * canvasWidth
                                val cy = eval.y * canvasHeight
                                val charSize = canvasWidth * 0.55f * eval.scaleY
                                hypot(offset.x - cx, offset.y - cy) <= charSize / 2f
                            }
                            if (hit != null) {
                                onSelectCharacter(hit.id)
                                onLongPressCharacter(hit)
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                canvasWidth = size.width
                canvasHeight = size.height

                // 1. Draw Background
                CartoonGraphicHelper.drawBackgroundCompose(
                    drawScope = this,
                    type = currentScene.backgroundType,
                    colorHex = currentScene.backgroundColorHex,
                    width = size.width,
                    height = size.height,
                    customBitmap = bgBitmap
                )

                // 2. Draw Characters
                for (char in currentScene.characters) {
                    val eval = MovementEvaluator.evaluate(char, currentScene.effects, currentTimeMs)
                    val charCenter = Offset(eval.x * size.width, eval.y * size.height)
                    val charSize = size.width * 0.55f * eval.scaleY
                    val (viseme, factor) = LipSyncEngine.evaluateViseme(char, project.audioTracks, currentTimeMs)

                    // Draw Character & Lip-Sync Mouth
                    CartoonGraphicHelper.drawCharacterCompose(
                        drawScope = this,
                        type = char.type,
                        customBitmap = charBitmaps[char.id],
                        center = charCenter,
                        size = charSize,
                        viseme = viseme,
                        mouthFactor = factor,
                        mouthAnchorX = char.mouthAnchorX,
                        mouthAnchorY = char.mouthAnchorY,
                        mouthScale = char.mouthScale,
                        alpha = eval.alpha,
                        glowIntensity = eval.glowIntensity
                    )

                    // Draw selection indicator if selected
                    if (char.id == selectedCharacterId) {
                        val selHalf = charSize * 0.52f
                        drawRoundRect(
                            color = Color(0xFF6366F1),
                            topLeft = Offset(charCenter.x - selHalf, charCenter.y - selHalf),
                            size = androidx.compose.ui.geometry.Size(selHalf * 2, selHalf * 2),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                            style = Stroke(width = 3f)
                        )

                        // If Lip Sync tab is active, draw mouth anchor calibration target
                        if (activeTab == StudioTab.LIP_SYNC) {
                            val mouthX = (charCenter.x - charSize / 2f) + charSize * char.mouthAnchorX
                            val mouthY = (charCenter.y - charSize / 2f) + charSize * char.mouthAnchorY
                            drawCircle(Color(0xFFF43F5E), radius = 10f, center = Offset(mouthX, mouthY), style = Stroke(width = 3f))
                            drawCircle(Color.White, radius = 4f, center = Offset(mouthX, mouthY))
                        }
                    }
                }

                // 3. Subtle 9:16 safe-area frame border
                drawRect(
                    color = Color.White.copy(alpha = 0.08f),
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}
