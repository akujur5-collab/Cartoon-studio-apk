package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.ProjectEntity
import com.example.data.engine.AudioRecorderManager
import com.example.data.engine.RecordingState
import com.example.data.engine.VideoExporter
import com.example.data.model.*
import com.example.data.repository.ProjectRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.UUID

enum class StudioTab(val label: String) {
    CHARACTER("Character"),
    BACKGROUND("Background"),
    VOICE("Voice"),
    LIP_SYNC("Lip Sync"),
    TIMELINE("Timeline"),
    EFFECTS("Effects"),
    PREVIEW("Preview"),
    EXPORT("Export")
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository.create(application)
    val recorderManager = AudioRecorderManager(application)

    val allProjectEntities: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentProject = MutableStateFlow(repository.createStarterProject())
    val currentProject: StateFlow<Project> = _currentProject.asStateFlow()

    private val _playbackTimeMs = MutableStateFlow(0L)
    val playbackTimeMs: StateFlow<Long> = _playbackTimeMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _selectedCharacterId = MutableStateFlow<String?>("char_dino_1")
    val selectedCharacterId: StateFlow<String?> = _selectedCharacterId.asStateFlow()

    private val _activeTab = MutableStateFlow(StudioTab.CHARACTER)
    val activeTab: StateFlow<StudioTab> = _activeTab.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow(0)
    val exportProgress: StateFlow<Int> = _exportProgress.asStateFlow()

    private val _exportedVideoFile = MutableStateFlow<File?>(null)
    val exportedVideoFile: StateFlow<File?> = _exportedVideoFile.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var playbackJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureDefaultProjects()
            val list = repository.allProjects.first()
            if (list.isNotEmpty()) {
                val proj = repository.getProject(list.first().id)
                if (proj != null) {
                    _currentProject.value = proj
                    _selectedCharacterId.value = proj.scenes.firstOrNull()?.characters?.firstOrNull()?.id
                }
            }
        }
    }

    fun setActiveTab(tab: StudioTab) {
        _activeTab.value = tab
    }

    fun selectCharacter(id: String?) {
        _selectedCharacterId.value = id
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    fun startPlayback() {
        playbackJob?.cancel()
        _isPlaying.value = true
        val duration = _currentProject.value.durationMs

        playbackJob = viewModelScope.launch(Dispatchers.Default) {
            val stepMs = 33L // ~30 FPS
            while (isActive && _isPlaying.value) {
                var next = _playbackTimeMs.value + stepMs
                if (next > duration) {
                    next = 0L // Loop
                }
                _playbackTimeMs.value = next
                delay(stepMs)
            }
        }
    }

    fun pausePlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
        recorderManager.stopPlayback()
    }

    fun seekTo(timeMs: Long) {
        val bounded = timeMs.coerceIn(0L, _currentProject.value.durationMs)
        _playbackTimeMs.value = bounded
    }

    fun restartPlayback() {
        seekTo(0L)
        startPlayback()
    }

    fun updateProjectTitle(newTitle: String) {
        _currentProject.update { it.copy(title = newTitle, updatedAt = System.currentTimeMillis()) }
        saveCurrentProject()
    }

    fun updateProjectDuration(durationMs: Long) {
        _currentProject.update { p ->
            val updatedScenes = p.scenes.map { it.copy(durationMs = durationMs) }
            p.copy(durationMs = durationMs, scenes = updatedScenes, updatedAt = System.currentTimeMillis())
        }
        saveCurrentProject()
    }

    // --- Character Actions ---

    fun addCharacter(type: CharacterType, imageUri: String? = null, name: String? = null) {
        val p = _currentProject.value
        val defaultScene = p.scenes.firstOrNull() ?: SceneData()
        val defaultName = name ?: when (type) {
            CharacterType.PRESET_DINO -> "Rex the Dino"
            CharacterType.PRESET_ROBOT -> "Pip the Robot"
            CharacterType.PRESET_CAT -> "Luna the Cat"
            CharacterType.PRESET_BOB -> "Hero Bob"
            CharacterType.CUSTOM_IMAGE -> "Custom Toon"
        }
        val newChar = CharacterLayerData(
            id = UUID.randomUUID().toString(),
            name = defaultName,
            type = type,
            imageUri = imageUri,
            zIndex = defaultScene.characters.size + 1,
            movement = MovementData(
                startX = 0.5f,
                startY = 0.6f,
                endX = 0.5f,
                endY = 0.6f,
                durationMs = p.durationMs
            )
        )
        val updatedCharacters = defaultScene.characters + newChar
        val updatedScenes = p.scenes.mapIndexed { idx, s ->
            if (idx == 0) s.copy(characters = updatedCharacters) else s
        }
        _currentProject.value = p.copy(scenes = updatedScenes, updatedAt = System.currentTimeMillis())
        _selectedCharacterId.value = newChar.id
        saveCurrentProject()
    }

    fun updateCharacterPosition(id: String, x: Float, y: Float) {
        updateCharacter(id) {
            val m = it.movement
            it.copy(
                movement = m.copy(startX = x, startY = y, endX = if (m.preset == MovementPreset.STATIC) x else m.endX, endY = if (m.preset == MovementPreset.STATIC) y else m.endY)
            )
        }
    }

    fun updateCharacterScale(id: String, scale: Float) {
        updateCharacter(id) {
            val clamped = scale.coerceIn(0.2f, 3.0f)
            val m = it.movement
            it.copy(movement = m.copy(startScale = clamped, endScale = clamped))
        }
    }

    fun updateCharacterRotation(id: String, rotation: Float) {
        updateCharacter(id) {
            val m = it.movement
            it.copy(movement = m.copy(startRotation = rotation, endRotation = rotation))
        }
    }

    fun updateCharacterMovement(id: String, movement: MovementData) {
        updateCharacter(id) { it.copy(movement = movement) }
    }

    fun updateCharacterLipSync(
        id: String,
        enabled: Boolean,
        sensitivity: Float,
        audioTrackId: String?,
        manualViseme: VisemeType?
    ) {
        updateCharacter(id) {
            it.copy(
                lipSyncEnabled = enabled,
                lipSyncSensitivity = sensitivity,
                assignedAudioTrackId = audioTrackId,
                manualViseme = manualViseme
            )
        }
    }

    fun updateMouthAnchor(id: String, anchorX: Float, anchorY: Float, scale: Float = 1.0f) {
        updateCharacter(id) {
            it.copy(
                mouthAnchorX = anchorX.coerceIn(0.05f, 0.95f),
                mouthAnchorY = anchorY.coerceIn(0.05f, 0.95f),
                mouthScale = scale.coerceIn(0.4f, 2.5f)
            )
        }
    }

    fun flipCharacter(id: String) {
        updateCharacter(id) { it.copy(isFlippedH = !it.isFlippedH) }
    }

    fun toggleLockCharacter(id: String) {
        updateCharacter(id) { it.copy(isLocked = !it.isLocked) }
    }

    fun duplicateCharacter(id: String) {
        val p = _currentProject.value
        val char = p.scenes.flatMap { it.characters }.firstOrNull { it.id == id } ?: return
        val dup = char.copy(
            id = UUID.randomUUID().toString(),
            name = "${char.name} (Copy)",
            movement = char.movement.copy(
                startX = (char.movement.startX + 0.08f).coerceAtMost(0.9f),
                endX = (char.movement.endX + 0.08f).coerceAtMost(0.9f)
            )
        )
        val updatedScenes = p.scenes.map { s ->
            if (s.characters.any { it.id == id }) {
                s.copy(characters = s.characters + dup)
            } else s
        }
        _currentProject.value = p.copy(scenes = updatedScenes, updatedAt = System.currentTimeMillis())
        _selectedCharacterId.value = dup.id
        saveCurrentProject()
    }

    fun deleteCharacter(id: String) {
        val p = _currentProject.value
        val updatedScenes = p.scenes.map { s ->
            s.copy(characters = s.characters.filter { it.id != id })
        }
        _currentProject.value = p.copy(scenes = updatedScenes, updatedAt = System.currentTimeMillis())
        if (_selectedCharacterId.value == id) {
            _selectedCharacterId.value = updatedScenes.firstOrNull()?.characters?.firstOrNull()?.id
        }
        saveCurrentProject()
    }

    fun reorderCharacter(id: String, bringForward: Boolean) {
        val p = _currentProject.value
        val updatedScenes = p.scenes.map { s ->
            val list = s.characters.toMutableList()
            val idx = list.indexOfFirst { it.id == id }
            if (idx != -1) {
                if (bringForward && idx < list.size - 1) {
                    val item = list.removeAt(idx)
                    list.add(idx + 1, item)
                } else if (!bringForward && idx > 0) {
                    val item = list.removeAt(idx)
                    list.add(idx - 1, item)
                }
            }
            s.copy(characters = list)
        }
        _currentProject.value = p.copy(scenes = updatedScenes, updatedAt = System.currentTimeMillis())
        saveCurrentProject()
    }

    private fun updateCharacter(id: String, transform: (CharacterLayerData) -> CharacterLayerData) {
        val p = _currentProject.value
        val updatedScenes = p.scenes.map { scene ->
            val chars = scene.characters.map { c ->
                if (c.id == id) transform(c) else c
            }
            scene.copy(characters = chars)
        }
        _currentProject.value = p.copy(scenes = updatedScenes, updatedAt = System.currentTimeMillis())
        saveCurrentProject()
    }

    // --- Background Actions ---

    fun setBackgroundType(type: BackgroundType, customUri: String? = null, colorHex: Long? = null) {
        val p = _currentProject.value
        val updatedScenes = p.scenes.mapIndexed { idx, s ->
            if (idx == 0) {
                s.copy(
                    backgroundType = type,
                    backgroundCustomUri = customUri ?: s.backgroundCustomUri,
                    backgroundColorHex = colorHex ?: s.backgroundColorHex
                )
            } else s
        }
        _currentProject.value = p.copy(scenes = updatedScenes, updatedAt = System.currentTimeMillis())
        saveCurrentProject()
    }

    fun setSceneTransition(transition: SceneTransition) {
        val p = _currentProject.value
        val updatedScenes = p.scenes.mapIndexed { idx, s ->
            if (idx == 0) s.copy(transition = transition) else s
        }
        _currentProject.value = p.copy(scenes = updatedScenes, updatedAt = System.currentTimeMillis())
        saveCurrentProject()
    }

    // --- Voice Recording & Audio Actions ---

    fun startVoiceRecording() {
        recorderManager.startRecording()
    }

    fun pauseVoiceRecording() {
        recorderManager.pauseRecording()
    }

    fun resumeVoiceRecording() {
        recorderManager.resumeRecording()
    }

    fun stopVoiceRecording(assignToSelectedCharacter: Boolean = true) {
        val result = recorderManager.stopRecording() ?: return
        val newTrack = AudioTrackData(
            id = UUID.randomUUID().toString(),
            name = "Voice ${System.currentTimeMillis() % 1000}",
            filePath = result.file.absolutePath,
            startTimeMs = _playbackTimeMs.value,
            durationMs = result.durationMs,
            volume = 1.0f,
            isVoiceOver = true,
            amplitudes = result.amplitudes
        )
        val p = _currentProject.value
        val updatedTracks = p.audioTracks + newTrack

        // Update project duration if recording exceeds it
        val newDur = maxOf(p.durationMs, newTrack.startTimeMs + newTrack.durationMs + 500L)

        // Auto assign to selected character if requested
        val charId = _selectedCharacterId.value
        val updatedScenes = p.scenes.map { s ->
            s.copy(
                characters = s.characters.map { c ->
                    if (assignToSelectedCharacter && c.id == charId) {
                        c.copy(assignedAudioTrackId = newTrack.id, lipSyncEnabled = true)
                    } else c
                }
            )
        }

        _currentProject.value = p.copy(
            audioTracks = updatedTracks,
            durationMs = newDur,
            scenes = updatedScenes,
            updatedAt = System.currentTimeMillis()
        )
        saveCurrentProject()
        _statusMessage.value = "Voice recorded (${result.durationMs / 1000}s) & lip-sync enabled!"
    }

    fun importAudio(uri: Uri) {
        val result = recorderManager.importAudioUri(uri)
        if (result != null) {
            val newTrack = AudioTrackData(
                id = UUID.randomUUID().toString(),
                name = "Imported Audio",
                filePath = result.file.absolutePath,
                startTimeMs = _playbackTimeMs.value,
                durationMs = result.durationMs,
                volume = 1.0f,
                isVoiceOver = true,
                amplitudes = result.amplitudes
            )
            val p = _currentProject.value
            _currentProject.value = p.copy(
                audioTracks = p.audioTracks + newTrack,
                durationMs = maxOf(p.durationMs, newTrack.startTimeMs + newTrack.durationMs),
                updatedAt = System.currentTimeMillis()
            )
            saveCurrentProject()
            _statusMessage.value = "Audio imported (${result.durationMs / 1000}s)!"
        }
    }

    fun deleteAudioTrack(trackId: String) {
        val p = _currentProject.value
        _currentProject.value = p.copy(
            audioTracks = p.audioTracks.filter { it.id != trackId },
            updatedAt = System.currentTimeMillis()
        )
        saveCurrentProject()
    }

    // --- Effects Actions ---

    fun addEffect(type: EffectType, targetCharId: String? = null, durationMs: Long = 2000L, intensity: Float = 0.8f) {
        val p = _currentProject.value
        val newEffect = EffectData(
            id = UUID.randomUUID().toString(),
            type = type,
            targetCharacterId = targetCharId,
            startTimeMs = _playbackTimeMs.value,
            durationMs = durationMs,
            intensity = intensity
        )
        val updatedScenes = p.scenes.mapIndexed { idx, s ->
            if (idx == 0) s.copy(effects = s.effects + newEffect) else s
        }
        _currentProject.value = p.copy(scenes = updatedScenes, updatedAt = System.currentTimeMillis())
        saveCurrentProject()
    }

    fun deleteEffect(effectId: String) {
        val p = _currentProject.value
        val updatedScenes = p.scenes.map { s ->
            s.copy(effects = s.effects.filter { it.id != effectId })
        }
        _currentProject.value = p.copy(scenes = updatedScenes, updatedAt = System.currentTimeMillis())
        saveCurrentProject()
    }

    // --- Export Video ---

    fun exportVideo(fps: Int = 30, is1080p: Boolean = false) {
        if (_isExporting.value) return
        _isExporting.value = true
        _exportProgress.value = 0
        _exportedVideoFile.value = null

        val w = if (is1080p) 1080 else 720
        val h = if (is1080p) 1920 else 1280
        val options = VideoExporter.ExportOptions(
            width = w,
            height = h,
            fps = fps,
            bitrate = if (is1080p) 8_000_000 else 4_000_000
        )

        viewModelScope.launch {
            val result = VideoExporter.exportProject(
                context = getApplication(),
                project = _currentProject.value,
                options = options
            ) { progress ->
                _exportProgress.value = progress
            }

            _isExporting.value = false
            result.onSuccess { file ->
                _exportedVideoFile.value = file
                _statusMessage.value = "Video exported successfully! (${file.length() / 1024} KB)"
            }.onFailure { err ->
                _errorMessage.value = "Export failed: ${err.localizedMessage}"
            }
        }
    }

    fun saveExportedToGallery() {
        val file = _exportedVideoFile.value ?: return
        val uri = VideoExporter.saveToGallery(getApplication(), file)
        if (uri != null) {
            _statusMessage.value = "Video saved to Gallery / Movies!"
        } else {
            _errorMessage.value = "Failed to save video to Gallery"
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _statusMessage.value = null
    }

    // --- Project Database Management ---

    fun saveCurrentProject() {
        viewModelScope.launch {
            repository.saveProject(_currentProject.value)
        }
    }

    fun loadProject(id: String) {
        viewModelScope.launch {
            val p = repository.getProject(id)
            if (p != null) {
                _currentProject.value = p
                _playbackTimeMs.value = 0L
                _selectedCharacterId.value = p.scenes.firstOrNull()?.characters?.firstOrNull()?.id
            }
        }
    }

    fun createNewProject() {
        viewModelScope.launch {
            val newP = Project(
                id = UUID.randomUUID().toString(),
                title = "New Cartoon Video",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                durationMs = 6000L,
                scenes = listOf(
                    SceneData(
                        id = UUID.randomUUID().toString(),
                        name = "Scene 1",
                        backgroundType = BackgroundType.PRESET_CITY,
                        characters = listOf(
                            CharacterLayerData(
                                id = UUID.randomUUID().toString(),
                                name = "Rex the Dino",
                                type = CharacterType.PRESET_DINO,
                                mouthAnchorX = 0.52f,
                                mouthAnchorY = 0.48f,
                                movement = MovementData(startX = 0.5f, startY = 0.6f, endX = 0.5f, endY = 0.6f)
                            )
                        )
                    )
                )
            )
            repository.saveProject(newP)
            _currentProject.value = newP
            _playbackTimeMs.value = 0L
            _selectedCharacterId.value = newP.scenes.firstOrNull()?.characters?.firstOrNull()?.id
        }
    }

    fun duplicateProject(id: String) {
        viewModelScope.launch {
            repository.duplicateProject(id)
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        recorderManager.release()
    }
}
