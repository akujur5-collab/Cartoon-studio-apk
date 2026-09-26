package com.example.ui.panels

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.MouthShapeRenderer
import com.example.data.model.VisemeType
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun LipSyncPanel(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val project by viewModel.currentProject.collectAsState()
    val selectedId by viewModel.selectedCharacterId.collectAsState()
    val currentScene = project.scenes.firstOrNull() ?: return
    val selectedChar = currentScene.characters.firstOrNull { it.id == selectedId }

    if (selectedChar == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Select a character on the canvas to configure lip-sync 👄", color = Color(0xFF94A3B8))
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. Header & Character Switcher ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Auto Lip-Sync System 👄", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(selectedChar.name, fontSize = 12.sp, color = Color(0xFF818CF8))
        }

        // --- 2. Assign Voice Audio Track Dropdown ---
        var expandedTrack by remember { mutableStateOf(false) }
        val assignedTrack = project.audioTracks.firstOrNull { it.id == selectedChar.assignedAudioTrackId }

        Surface(
            color = Color(0xFF181C2A),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF272F45)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Assigned Voice Track", fontSize = 11.sp, color = Color(0xFF94A3B8))

                Box {
                    OutlinedButton(
                        onClick = { expandedTrack = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(assignedTrack?.name ?: "Auto (Any Active Voice)", color = Color.White)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = expandedTrack,
                        onDismissRequest = { expandedTrack = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Auto (Any Active Voice)") },
                            onClick = {
                                viewModel.updateCharacterLipSync(
                                    selectedChar.id,
                                    selectedChar.lipSyncEnabled,
                                    selectedChar.lipSyncSensitivity,
                                    null,
                                    selectedChar.manualViseme
                                )
                                expandedTrack = false
                            }
                        )
                        project.audioTracks.forEach { track ->
                            DropdownMenuItem(
                                text = { Text(track.name) },
                                onClick = {
                                    viewModel.updateCharacterLipSync(
                                        selectedChar.id,
                                        selectedChar.lipSyncEnabled,
                                        selectedChar.lipSyncSensitivity,
                                        track.id,
                                        selectedChar.manualViseme
                                    )
                                    expandedTrack = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto Lip-Sync Enabled", fontSize = 12.sp, color = Color.White)
                    Switch(
                        checked = selectedChar.lipSyncEnabled,
                        onCheckedChange = {
                            viewModel.updateCharacterLipSync(
                                selectedChar.id,
                                it,
                                selectedChar.lipSyncSensitivity,
                                selectedChar.assignedAudioTrackId,
                                selectedChar.manualViseme
                            )
                        }
                    )
                }

                // Sensitivity slider
                Text("Speech Sensitivity: ${String.format("%.1fx", selectedChar.lipSyncSensitivity)}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Slider(
                    value = selectedChar.lipSyncSensitivity,
                    onValueChange = {
                        viewModel.updateCharacterLipSync(
                            selectedChar.id,
                            selectedChar.lipSyncEnabled,
                            it,
                            selectedChar.assignedAudioTrackId,
                            selectedChar.manualViseme
                        )
                    },
                    valueRange = 0.4f..2.5f
                )
            }
        }

        // --- 3. Mouth Position & Scale Calibration ---
        Surface(
            color = Color(0xFF181C2A),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF272F45)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mouth Position & Scale (Target Pin)", style = MaterialTheme.typography.titleSmall, color = Color.White)
                Text("Tip: Drag the red mouth target on the Canvas to align perfectly!", fontSize = 11.sp, color = Color(0xFF38BDF8))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mouth X: ${(selectedChar.mouthAnchorX * 100).toInt()}%", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        Slider(
                            value = selectedChar.mouthAnchorX,
                            onValueChange = { viewModel.updateMouthAnchor(selectedChar.id, it, selectedChar.mouthAnchorY, selectedChar.mouthScale) },
                            valueRange = 0.1f..0.9f
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mouth Y: ${(selectedChar.mouthAnchorY * 100).toInt()}%", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        Slider(
                            value = selectedChar.mouthAnchorY,
                            onValueChange = { viewModel.updateMouthAnchor(selectedChar.id, selectedChar.mouthAnchorX, it, selectedChar.mouthScale) },
                            valueRange = 0.1f..0.9f
                        )
                    }
                }

                Text("Mouth Scale: ${(selectedChar.mouthScale * 100).toInt()}%", fontSize = 10.sp, color = Color(0xFF94A3B8))
                Slider(
                    value = selectedChar.mouthScale,
                    onValueChange = { viewModel.updateMouthAnchor(selectedChar.id, selectedChar.mouthAnchorX, selectedChar.mouthAnchorY, it) },
                    valueRange = 0.5f..2.0f
                )
            }
        }

        // --- 4. Visemes & Phonetic Test Grid ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Viseme Shapes Showcase", style = MaterialTheme.typography.titleSmall, color = Color.White)
            if (selectedChar.manualViseme != null) {
                TextButton(
                    onClick = {
                        viewModel.updateCharacterLipSync(
                            selectedChar.id,
                            selectedChar.lipSyncEnabled,
                            selectedChar.lipSyncSensitivity,
                            selectedChar.assignedAudioTrackId,
                            null
                        )
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto Mode", fontSize = 11.sp, color = Color(0xFF38BDF8))
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) {
            items(VisemeType.entries) { viseme ->
                val isSelected = selectedChar.manualViseme == viseme
                Box(
                    modifier = Modifier
                        .height(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0xFF3730A3) else Color(0xFF1E293B))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF818CF8) else Color(0xFF334155),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            viewModel.updateCharacterLipSync(
                                selectedChar.id,
                                selectedChar.lipSyncEnabled,
                                selectedChar.lipSyncSensitivity,
                                selectedChar.assignedAudioTrackId,
                                viseme
                            )
                        }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Canvas(modifier = Modifier.size(42.dp, 24.dp)) {
                            MouthShapeRenderer.drawMouthCompose(
                                drawScope = this,
                                viseme = viseme,
                                center = Offset(size.width / 2f, size.height / 2f),
                                baseWidth = 32f,
                                baseHeight = 18f,
                                openFactor = 1.0f
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(viseme.displayName, fontSize = 10.sp, color = Color.White)
                        Text(viseme.phoneticExample, fontSize = 8.sp, color = Color(0xFF94A3B8), maxLines = 1)
                    }
                }
            }
        }
    }
}
