package com.example.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun TimelinePanel(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val project by viewModel.currentProject.collectAsState()
    val currentTimeMs by viewModel.playbackTimeMs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val totalDurationMs = project.durationMs.coerceAtLeast(1000L)
    val currentScene = project.scenes.firstOrNull() ?: return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. Playhead & Transport Controls ---
        Surface(
            color = Color(0xFF181C2A),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF272F45)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val curSec = currentTimeMs / 1000f
                    val totSec = totalDurationMs / 1000f
                    Text(
                        text = String.format("⏱️ %02d:%04.1f / %02d:%04.1f", (curSec / 60).toInt(), curSec % 60, (totSec / 60).toInt(), totSec % 60),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.restartPlayback() }) {
                            Icon(Icons.Default.FastRewind, contentDescription = "Restart", tint = Color.White)
                        }
                        FilledIconButton(
                            onClick = { viewModel.togglePlayPause() },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF4F46E5))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Scrubbing Slider
                Slider(
                    value = currentTimeMs.toFloat(),
                    onValueChange = { viewModel.seekTo(it.toLong()) },
                    valueRange = 0f..totalDurationMs.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF818CF8),
                        activeTrackColor = Color(0xFF4F46E5),
                        inactiveTrackColor = Color(0xFF334155)
                    )
                )
            }
        }

        // --- 2. Mobile Visual Timeline Tracks ---
        Text("Timeline Tracks 🎞️", style = MaterialTheme.typography.titleSmall, color = Color.White)

        Surface(
            color = Color(0xFF131622),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Track 1: Scene / Background
                TimelineTrackRow(
                    label = "🖼️ Scene",
                    trackColor = Color(0xFF3B82F6),
                    startRatio = 0f,
                    widthRatio = 1f,
                    clipName = "${currentScene.name} (${currentScene.backgroundType.name.removePrefix("PRESET_")})"
                )

                // Track 2: Characters
                currentScene.characters.forEach { char ->
                    val m = char.movement
                    val charStartRatio = (m.startTimeMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
                    val charDurRatio = (m.durationMs.toFloat() / totalDurationMs).coerceIn(0.1f, 1f)

                    TimelineTrackRow(
                        label = "🎭 ${char.name.take(7)}",
                        trackColor = Color(0xFF10B981),
                        startRatio = charStartRatio,
                        widthRatio = charDurRatio,
                        clipName = "${char.name} [${m.preset.displayName}]"
                    )
                }

                // Track 3: Voice / Audio Tracks
                if (project.audioTracks.isEmpty()) {
                    TimelineTrackRow(
                        label = "🎙️ Voice",
                        trackColor = Color(0xFF475569),
                        startRatio = 0f,
                        widthRatio = 1f,
                        clipName = "No audio recorded yet"
                    )
                } else {
                    project.audioTracks.forEach { audio ->
                        val aStartRatio = (audio.startTimeMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
                        val aDurRatio = (audio.durationMs.toFloat() / totalDurationMs).coerceIn(0.1f, 1f)

                        TimelineTrackRow(
                            label = "🎙️ Voice",
                            trackColor = Color(0xFFF43F5E),
                            startRatio = aStartRatio,
                            widthRatio = aDurRatio,
                            clipName = audio.name
                        )
                    }
                }

                // Track 4: Effects
                if (currentScene.effects.isNotEmpty()) {
                    currentScene.effects.forEach { eff ->
                        val effStartRatio = (eff.startTimeMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
                        val effDurRatio = (eff.durationMs.toFloat() / totalDurationMs).coerceIn(0.1f, 1f)

                        TimelineTrackRow(
                            label = "✨ FX",
                            trackColor = Color(0xFFFBBF24),
                            startRatio = effStartRatio,
                            widthRatio = effDurRatio,
                            clipName = eff.type.displayName
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineTrackRow(
    label: String,
    trackColor: Color,
    startRatio: Float,
    widthRatio: Float,
    clipName: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.width(76.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(28.dp)
                .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                .padding(2.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val leftOffset = maxWidth * startRatio
                val blockWidth = (maxWidth * widthRatio).coerceAtMost(maxWidth - leftOffset)

                Box(
                    modifier = Modifier
                        .offset(x = leftOffset)
                        .width(blockWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(trackColor.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = clipName,
                        fontSize = 10.sp,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
