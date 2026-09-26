package com.example.ui.panels

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.engine.RecordingState
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun VoicePanel(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val project by viewModel.currentProject.collectAsState()
    val recordingState by viewModel.recorderManager.recordingState.collectAsState()
    val liveAmp by viewModel.recorderManager.liveAmplitude.collectAsState()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            viewModel.startVoiceRecording()
        }
    }

    val audioFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importAudio(uri)
        }
    }

    var autoAssignToChar by remember { mutableStateOf(true) }

    // Pulsing animation for recording button
    val pulseInfinite = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseInfinite.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. Recorder Console ---
        Surface(
            color = Color(0xFF181C2A),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF272F45)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = when (recordingState) {
                        RecordingState.IDLE -> "Voice-Over Studio 🎙️"
                        RecordingState.RECORDING -> "Recording Voice... Speak Now!"
                        RecordingState.PAUSED -> "Recording Paused"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (recordingState == RecordingState.RECORDING) Color(0xFFF43F5E) else Color.White
                )

                // Live Amplitude Visualizer Bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(Color(0xFF0F121D), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0..24) {
                        val barHeight = if (recordingState == RecordingState.RECORDING) {
                            val wave = kotlin.math.sin(i * 0.4f + System.currentTimeMillis() * 0.01f)
                            (liveAmp * (0.4f + 0.6f * kotlin.math.abs(wave)) * 34f).coerceIn(4f, 34f)
                        } else 4f

                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(barHeight.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (recordingState == RecordingState.RECORDING) Color(0xFFF43F5E)
                                    else Color(0xFF334155)
                                )
                        )
                    }
                }

                // Controls Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (recordingState == RecordingState.IDLE) {
                        // Start Record Button
                        FloatingActionButton(
                            onClick = {
                                if (hasMicPermission) {
                                    viewModel.startVoiceRecording()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            containerColor = Color(0xFFF43F5E),
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Record", modifier = Modifier.size(28.dp))
                        }

                        OutlinedButton(
                            onClick = { audioFilePicker.launch("audio/*") },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Audio")
                        }
                    } else {
                        // Pause / Resume Button
                        IconButton(
                            onClick = {
                                if (recordingState == RecordingState.RECORDING) viewModel.pauseVoiceRecording()
                                else viewModel.resumeVoiceRecording()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF334155), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (recordingState == RecordingState.RECORDING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Pause/Resume",
                                tint = Color.White
                            )
                        }

                        // Stop & Save Button
                        Box(
                            modifier = Modifier
                                .scale(if (recordingState == RecordingState.RECORDING) pulseScale else 1f)
                        ) {
                            FloatingActionButton(
                                onClick = { viewModel.stopVoiceRecording(assignToSelectedCharacter = autoAssignToChar) },
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White,
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.Done, contentDescription = "Done Recording", modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Checkbox(
                        checked = autoAssignToChar,
                        onCheckedChange = { autoAssignToChar = it }
                    )
                    Text("Auto-assign to selected character for Lip-Sync", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }
        }

        // --- 2. Audio Tracks List ---
        Text("Project Audio Clips (${project.audioTracks.size})", style = MaterialTheme.typography.titleSmall, color = Color.White)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(project.audioTracks) { track ->
                Surface(
                    color = Color(0xFF181C2A),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF272F45)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(track.name, fontSize = 12.sp, color = Color.White)
                            Text("Start: ${track.startTimeMs / 1000f}s | Duration: ${track.durationMs / 1000f}s", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }
                        Row {
                            IconButton(onClick = { viewModel.recorderManager.playAudio(track.filePath) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color(0xFF38BDF8))
                            }
                            IconButton(onClick = { viewModel.deleteAudioTrack(track.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFF43F5E))
                            }
                        }
                    }
                }
            }
        }
    }
}
