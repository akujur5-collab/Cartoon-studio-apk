package com.example.ui.panels

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun CharacterPanel(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val project by viewModel.currentProject.collectAsState()
    val selectedId by viewModel.selectedCharacterId.collectAsState()

    val currentScene = project.scenes.firstOrNull() ?: return
    val selectedChar = currentScene.characters.firstOrNull { it.id == selectedId }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.addCharacter(CharacterType.CUSTOM_IMAGE, imageUri = uri.toString(), name = "Gallery Toon")
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. Add Characters Row ---
        Text("Add Characters 🎭", style = MaterialTheme.typography.titleSmall, color = Color.White)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                Button(
                    onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add from Gallery", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Image", fontSize = 12.sp)
                }
            }
            item {
                AssistChip(
                    onClick = { viewModel.addCharacter(CharacterType.PRESET_DINO) },
                    label = { Text("🦖 Rex Dino") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            item {
                AssistChip(
                    onClick = { viewModel.addCharacter(CharacterType.PRESET_ROBOT) },
                    label = { Text("🤖 Pip Robot") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            item {
                AssistChip(
                    onClick = { viewModel.addCharacter(CharacterType.PRESET_CAT) },
                    label = { Text("🐱 Luna Cat") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            item {
                AssistChip(
                    onClick = { viewModel.addCharacter(CharacterType.PRESET_BOB) },
                    label = { Text("👦 Hero Bob") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // --- 2. Character Layers List ---
        Text("Scene Layers (${currentScene.characters.size})", style = MaterialTheme.typography.titleSmall, color = Color.White)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(currentScene.characters) { char ->
                val isSelected = char.id == selectedId
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0xFF3730A3) else Color(0xFF1E293B))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF818CF8) else Color(0xFF334155),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.selectCharacter(char.id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = char.name,
                            fontSize = 12.sp,
                            color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                        )
                        if (char.isLocked) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // --- 3. Selected Character Controls ---
        if (selectedChar != null) {
            Surface(
                color = Color(0xFF181C2A),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF272F45)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Actions: Flip, Lock, Layer Up/Down, Duplicate, Delete
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedChar.name, style = MaterialTheme.typography.titleMedium, color = Color(0xFF818CF8))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { viewModel.flipCharacter(selectedChar.id) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = "Flip", tint = if (selectedChar.isFlippedH) Color(0xFF38BDF8) else Color.White)
                            }
                            IconButton(onClick = { viewModel.toggleLockCharacter(selectedChar.id) }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = if (selectedChar.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Lock",
                                    tint = if (selectedChar.isLocked) Color(0xFFFBBF24) else Color.White
                                )
                            }
                            IconButton(onClick = { viewModel.reorderCharacter(selectedChar.id, bringForward = true) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Bring Forward", tint = Color.White)
                            }
                            IconButton(onClick = { viewModel.duplicateCharacter(selectedChar.id) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color.White)
                            }
                            IconButton(onClick = { viewModel.deleteCharacter(selectedChar.id) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFF43F5E))
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF272F45))

                    // Movement Controls
                    Text("Movement Control 🏃", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    var expandedMovement by remember { mutableStateOf(false) }

                    Box {
                        OutlinedButton(
                            onClick = { expandedMovement = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Motion: ${selectedChar.movement.preset.displayName}", color = Color.White)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = expandedMovement,
                            onDismissRequest = { expandedMovement = false }
                        ) {
                            MovementPreset.entries.forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(preset.displayName) },
                                    onClick = {
                                        viewModel.updateCharacterMovement(selectedChar.id, selectedChar.movement.copy(preset = preset))
                                        expandedMovement = false
                                    }
                                )
                            }
                        }
                    }

                    // Sliders for Position, Size, Rotation
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Position X: ${(selectedChar.movement.startX * 100).toInt()}%", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Slider(
                                value = selectedChar.movement.startX,
                                onValueChange = { viewModel.updateCharacterPosition(selectedChar.id, it, selectedChar.movement.startY) },
                                valueRange = 0.05f..0.95f
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Position Y: ${(selectedChar.movement.startY * 100).toInt()}%", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Slider(
                                value = selectedChar.movement.startY,
                                onValueChange = { viewModel.updateCharacterPosition(selectedChar.id, selectedChar.movement.startX, it) },
                                valueRange = 0.05f..0.95f
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Scale: ${(selectedChar.movement.startScale * 100).toInt()}%", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Slider(
                                value = selectedChar.movement.startScale,
                                onValueChange = { viewModel.updateCharacterScale(selectedChar.id, it) },
                                valueRange = 0.4f..2.2f
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Rotation: ${selectedChar.movement.startRotation.toInt()}°", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Slider(
                                value = selectedChar.movement.startRotation,
                                onValueChange = { viewModel.updateCharacterRotation(selectedChar.id, it) },
                                valueRange = -180f..180f
                            )
                        }
                    }

                    // Movement duration slider
                    Text("Movement Duration: ${selectedChar.movement.durationMs / 1000f}s", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Slider(
                        value = selectedChar.movement.durationMs.toFloat(),
                        onValueChange = {
                            viewModel.updateCharacterMovement(selectedChar.id, selectedChar.movement.copy(durationMs = it.toLong()))
                        },
                        valueRange = 500f..project.durationMs.toFloat()
                    )
                }
            }
        }
    }
}
