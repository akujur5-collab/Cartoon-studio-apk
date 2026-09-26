package com.example.ui.panels

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun PreviewPanel(
    viewModel: StudioViewModel,
    onOpenFullScreenPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val project by viewModel.currentProject.collectAsState()
    val currentTimeMs by viewModel.playbackTimeMs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val totalDurationMs = project.durationMs.coerceAtLeast(1000L)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            color = Color(0xFF181C2A),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF272F45)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val curSec = currentTimeMs / 1000f
                val totSec = totalDurationMs / 1000f
                Text(
                    text = String.format("%02d:%04.1f / %02d:%04.1f", (curSec / 60).toInt(), curSec % 60, (totSec / 60).toInt(), totSec % 60),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                Slider(
                    value = currentTimeMs.toFloat(),
                    onValueChange = { viewModel.seekTo(it.toLong()) },
                    valueRange = 0f..totalDurationMs.toFloat()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.seekTo((currentTimeMs - 1000L).coerceAtLeast(0L)) }) {
                        Icon(Icons.Default.Replay10, contentDescription = "-1s", tint = Color.White)
                    }

                    FloatingActionButton(
                        onClick = { viewModel.togglePlayPause() },
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(onClick = { viewModel.seekTo((currentTimeMs + 1000L).coerceAtMost(totalDurationMs)) }) {
                        Icon(Icons.Default.Forward10, contentDescription = "+1s", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onOpenFullScreenPreview,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Full-Screen Cinema Preview 🎬", fontSize = 12.sp)
                }
            }
        }
    }
}
