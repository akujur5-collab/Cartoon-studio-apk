package com.example.ui.panels

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun ExportPanel(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val project by viewModel.currentProject.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val progress by viewModel.exportProgress.collectAsState()
    val exportedFile by viewModel.exportedVideoFile.collectAsState()

    var is1080p by remember { mutableStateOf(false) }
    var selectedFps by remember { mutableIntStateOf(30) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Export 9:16 Cartoon Video 📤", style = MaterialTheme.typography.titleMedium, color = Color.White)

        // Settings Surface
        Surface(
            color = Color(0xFF181C2A),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF272F45)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Resolution Selector
                Text("Video Resolution (9:16)", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !is1080p,
                        onClick = { is1080p = false },
                        label = { Text("720 × 1280 (HD)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = is1080p,
                        onClick = { is1080p = true },
                        label = { Text("1080 × 1920 (FHD)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // FPS Selector
                Text("Frame Rate (FPS)", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(24, 30, 60).forEach { fps ->
                        FilterChip(
                            selected = selectedFps == fps,
                            onClick = { selectedFps = fps },
                            label = { Text("$fps FPS") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF272F45))

                // Export Button / Progress
                if (isExporting) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = Color(0xFF4F46E5),
                            trackColor = Color(0xFF334155),
                        )
                        Text("Rendering 9:16 MP4 Frames... $progress%", fontSize = 12.sp, color = Color(0xFF818CF8))
                    }
                } else {
                    Button(
                        onClick = { viewModel.exportVideo(fps = selectedFps, is1080p = is1080p) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                    ) {
                        Icon(Icons.Default.MovieCreation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export ${if (is1080p) "1080p" else "720p"} MP4 Video", fontSize = 13.sp)
                    }
                }
            }
        }

        // --- Export Success Actions ---
        if (exportedFile != null) {
            Surface(
                color = Color(0xFF064E3B),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = Color(0xFF34D399))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Video Rendered Successfully!", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    }
                    Text(
                        "File: ${exportedFile?.name} (${(exportedFile?.length() ?: 0) / 1024} KB)",
                        fontSize = 11.sp,
                        color = Color(0xFFA7F3D0)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.saveExportedToGallery() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save to Gallery", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val file = exportedFile ?: return@Button
                                val uri = try {
                                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                } catch (e: Exception) {
                                    android.net.Uri.fromFile(file)
                                }
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "video/mp4"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Cartoon Video"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share Video", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
