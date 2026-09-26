package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CharacterLayerData
import com.example.ui.components.AnimationCanvas
import com.example.ui.components.BottomStudioNav
import com.example.ui.components.FullScreenPreviewDialog
import com.example.ui.components.ProjectsDialog
import com.example.ui.panels.*
import com.example.ui.viewmodel.StudioTab
import com.example.ui.viewmodel.StudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val project by viewModel.currentProject.collectAsState()
    val currentTimeMs by viewModel.playbackTimeMs.collectAsState()
    val selectedCharId by viewModel.selectedCharacterId.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()
    val errorMsg by viewModel.errorMessage.collectAsState()

    var showProjectsDialog by remember { mutableStateOf(false) }
    var showFullScreenPreview by remember { mutableStateOf(false) }
    var contextMenuChar by remember { mutableStateOf<CharacterLayerData?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMsg) {
        statusMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(errorMsg) {
        errorMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0C0E17),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("ToonCraft", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFF4F46E5),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "9:16",
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            project.title,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = if (isPlaying) Color(0xFF818CF8) else Color.White
                        )
                    }
                    IconButton(
                        onClick = { showProjectsDialog = true },
                        modifier = Modifier.testTag("projects_folder_button")
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = "Projects", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF131622)
                )
            )
        },
        bottomBar = {
            BottomStudioNav(
                activeTab = activeTab,
                onTabSelected = { viewModel.setActiveTab(it) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Upper Section: 9:16 Animation Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.95f)
                    .background(Color(0xFF090B12)),
                contentAlignment = Alignment.Center
            ) {
                AnimationCanvas(
                    project = project,
                    currentTimeMs = currentTimeMs,
                    selectedCharacterId = selectedCharId,
                    activeTab = activeTab,
                    onSelectCharacter = { viewModel.selectCharacter(it) },
                    onUpdateCharacterPosition = { id, x, y -> viewModel.updateCharacterPosition(id, x, y) },
                    onUpdateCharacterScale = { id, s -> viewModel.updateCharacterScale(id, s) },
                    onUpdateCharacterRotation = { id, r -> viewModel.updateCharacterRotation(id, r) },
                    onUpdateMouthAnchor = { id, mx, my -> viewModel.updateMouthAnchor(id, mx, my) },
                    onLongPressCharacter = { char -> contextMenuChar = char },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Lower Section: Active Panel
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.05f),
                color = Color(0xFF131622),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                when (activeTab) {
                    StudioTab.CHARACTER -> CharacterPanel(viewModel = viewModel)
                    StudioTab.BACKGROUND -> BackgroundPanel(viewModel = viewModel)
                    StudioTab.VOICE -> VoicePanel(viewModel = viewModel)
                    StudioTab.LIP_SYNC -> LipSyncPanel(viewModel = viewModel)
                    StudioTab.TIMELINE -> TimelinePanel(viewModel = viewModel)
                    StudioTab.EFFECTS -> EffectsPanel(viewModel = viewModel)
                    StudioTab.PREVIEW -> PreviewPanel(
                        viewModel = viewModel,
                        onOpenFullScreenPreview = { showFullScreenPreview = true }
                    )
                    StudioTab.EXPORT -> ExportPanel(viewModel = viewModel)
                }
            }
        }
    }

    // Projects Dialog
    if (showProjectsDialog) {
        ProjectsDialog(
            viewModel = viewModel,
            onDismiss = { showProjectsDialog = false }
        )
    }

    // Full Screen Preview
    if (showFullScreenPreview) {
        FullScreenPreviewDialog(
            viewModel = viewModel,
            onDismiss = { showFullScreenPreview = false }
        )
    }

    // Quick Character Context Menu Sheet
    contextMenuChar?.let { char ->
        AlertDialog(
            onDismissRequest = { contextMenuChar = null },
            title = { Text(char.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = {
                            viewModel.duplicateCharacter(char.id)
                            contextMenuChar = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Duplicate Character")
                    }
                    TextButton(
                        onClick = {
                            viewModel.flipCharacter(char.id)
                            contextMenuChar = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Flip Horizontally")
                    }
                    TextButton(
                        onClick = {
                            viewModel.reorderCharacter(char.id, bringForward = true)
                            contextMenuChar = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bring Forward")
                    }
                    TextButton(
                        onClick = {
                            viewModel.toggleLockCharacter(char.id)
                            contextMenuChar = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(if (char.isLocked) Icons.Default.LockOpen else Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (char.isLocked) "Unlock" else "Lock Position")
                    }
                    TextButton(
                        onClick = {
                            viewModel.deleteCharacter(char.id)
                            contextMenuChar = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFF43F5E))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete", color = Color(0xFFF43F5E))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { contextMenuChar = null }) {
                    Text("Close")
                }
            }
        )
    }
}
