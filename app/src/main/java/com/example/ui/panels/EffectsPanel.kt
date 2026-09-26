package com.example.ui.panels

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EffectType
import com.example.ui.viewmodel.StudioViewModel

data class EffectCardItem(
    val type: EffectType,
    val emoji: String,
    val description: String
)

@Composable
fun EffectsPanel(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val project by viewModel.currentProject.collectAsState()
    val selectedCharId by viewModel.selectedCharacterId.collectAsState()
    val currentScene = project.scenes.firstOrNull() ?: return

    val effectList = remember {
        listOf(
            EffectCardItem(EffectType.SPARKLE, "✨", "Sparkles"),
            EffectCardItem(EffectType.BOUNCE, "🦘", "Bounce"),
            EffectCardItem(EffectType.SHAKE, "🫨", "Screen Shake"),
            EffectCardItem(EffectType.GLOW, "🌟", "Magic Glow"),
            EffectCardItem(EffectType.ZOOM_PULSE, "🔍", "Pulse Zoom"),
            EffectCardItem(EffectType.FADE_IN, "🌅", "Fade In"),
            EffectCardItem(EffectType.FADE_OUT, "🌃", "Fade Out")
        )
    }

    var applyToChar by remember { mutableStateOf(false) }
    var effectDuration by remember { mutableFloatStateOf(2000f) }
    var effectIntensity by remember { mutableFloatStateOf(0.8f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Visual & Motion Effects ✨", style = MaterialTheme.typography.titleMedium, color = Color.White)

        // Options: Target & Duration
        Surface(
            color = Color(0xFF181C2A),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF272F45)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (applyToChar) "Target: Selected Character" else "Target: Whole Scene",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Switch(
                        checked = applyToChar,
                        onCheckedChange = { applyToChar = it }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Duration: ${effectDuration / 1000f}s", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Slider(
                            value = effectDuration,
                            onValueChange = { effectDuration = it },
                            valueRange = 500f..project.durationMs.toFloat()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Intensity: ${(effectIntensity * 100).toInt()}%", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Slider(
                            value = effectIntensity,
                            onValueChange = { effectIntensity = it },
                            valueRange = 0.2f..1.0f
                        )
                    }
                }
            }
        }

        // Effects Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            items(effectList) { item ->
                Box(
                    modifier = Modifier
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.addEffect(
                                type = item.type,
                                targetCharId = if (applyToChar) selectedCharId else null,
                                durationMs = effectDuration.toLong(),
                                intensity = effectIntensity
                            )
                        }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(item.emoji, fontSize = 22.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(item.description, fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        // Active Effects List
        if (currentScene.effects.isNotEmpty()) {
            Text("Active Scene Effects (${currentScene.effects.size})", style = MaterialTheme.typography.titleSmall, color = Color.White)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                currentScene.effects.forEach { eff ->
                    Surface(
                        color = Color(0xFF181C2A),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${eff.type.displayName} (${eff.durationMs / 1000f}s)", fontSize = 12.sp, color = Color.White)
                            IconButton(onClick = { viewModel.deleteEffect(eff.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFF43F5E), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
