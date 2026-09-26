package com.example.ui.panels

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BackgroundType
import com.example.data.model.SceneTransition
import com.example.ui.viewmodel.StudioViewModel

data class BackgroundPresetItem(
    val type: BackgroundType,
    val title: String,
    val emoji: String,
    val previewColor: Color
)

@Composable
fun BackgroundPanel(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val project by viewModel.currentProject.collectAsState()
    val currentScene = project.scenes.firstOrNull() ?: return

    val presets = remember {
        listOf(
            BackgroundPresetItem(BackgroundType.PRESET_CITY, "Sunset City", "🏙️", Color(0xFFF97316)),
            BackgroundPresetItem(BackgroundType.PRESET_SPACE, "Cosmic Space", "🚀", Color(0xFF3B0764)),
            BackgroundPresetItem(BackgroundType.PRESET_FOREST, "Enchanted Forest", "🌲", Color(0xFF047857)),
            BackgroundPresetItem(BackgroundType.PRESET_STUDIO, "Studio Stage", "🎬", Color(0xFF1E293B)),
            BackgroundPresetItem(BackgroundType.PRESET_SUNSET, "Tropical Beach", "🌅", Color(0xFF9333EA))
        )
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setBackgroundType(BackgroundType.CUSTOM_IMAGE, customUri = uri.toString())
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Background Presets 🖼️", style = MaterialTheme.typography.titleSmall, color = Color.White)
            OutlinedButton(
                onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Custom 9:16", fontSize = 12.sp)
            }
        }

        // Preset Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            items(presets) { preset ->
                val isSelected = currentScene.backgroundType == preset.type
                Box(
                    modifier = Modifier
                        .height(80.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(preset.previewColor.copy(alpha = 0.35f))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF818CF8) else Color(0xFF334155),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { viewModel.setBackgroundType(preset.type) }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(preset.emoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(preset.title, fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        // Scene Duration & Transition
        Surface(
            color = Color(0xFF181C2A),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Scene Settings", style = MaterialTheme.typography.titleSmall, color = Color.White)

                Text("Total Project Duration: ${project.durationMs / 1000f}s", fontSize = 12.sp, color = Color(0xFF94A3B8))
                Slider(
                    value = project.durationMs.toFloat(),
                    onValueChange = { viewModel.updateProjectDuration(it.toLong()) },
                    valueRange = 3000f..30000f,
                    steps = 26
                )

                Text("Scene Transition: ${currentScene.transition.displayName}", fontSize = 12.sp, color = Color(0xFF94A3B8))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SceneTransition.entries.take(4).forEach { trans ->
                        val isSel = currentScene.transition == trans
                        FilterChip(
                            selected = isSel,
                            onClick = { viewModel.setSceneTransition(trans) },
                            label = { Text(trans.displayName, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }
    }
}
