package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.db.ProjectDao
import com.example.data.db.ProjectEntity
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

class ProjectRepository(private val projectDao: ProjectDao) {

    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun getProject(id: String): Project? = withContext(Dispatchers.IO) {
        val entity = projectDao.getProjectById(id) ?: return@withContext null
        ProjectJsonSerializer.projectFromJson(entity.dataJson)
    }

    suspend fun saveProject(project: Project) = withContext(Dispatchers.IO) {
        val json = ProjectJsonSerializer.projectToJson(project)
        val entity = ProjectEntity(
            id = project.id,
            title = project.title,
            updatedAt = System.currentTimeMillis(),
            durationMs = project.durationMs,
            previewThumbnail = null,
            dataJson = json
        )
        projectDao.insertProject(entity)
    }

    suspend fun deleteProject(id: String) = withContext(Dispatchers.IO) {
        projectDao.deleteProjectById(id)
    }

    suspend fun duplicateProject(id: String): Project? = withContext(Dispatchers.IO) {
        val original = getProject(id) ?: return@withContext null
        val newProject = original.copy(
            id = UUID.randomUUID().toString(),
            title = "${original.title} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        saveProject(newProject)
        newProject
    }

    suspend fun ensureDefaultProjects() = withContext(Dispatchers.IO) {
        val current = projectDao.getAllProjects().first()
        if (current.isEmpty()) {
            val starterProject = createStarterProject()
            saveProject(starterProject)
        }
    }

    fun createStarterProject(): Project {
        val charDino = CharacterLayerData(
            id = "char_dino_1",
            name = "Rex the Dino",
            type = CharacterType.PRESET_DINO,
            zIndex = 1,
            isLocked = false,
            mouthAnchorX = 0.52f,
            mouthAnchorY = 0.48f,
            mouthScale = 1.0f,
            assignedAudioTrackId = "track_voice_1",
            lipSyncEnabled = true,
            lipSyncSensitivity = 1.2f,
            movement = MovementData(
                preset = MovementPreset.BOUNCE,
                startX = 0.35f,
                startY = 0.60f,
                endX = 0.35f,
                endY = 0.60f,
                startScale = 1.05f,
                endScale = 1.05f,
                startTimeMs = 0L,
                durationMs = 4000L,
                easing = EasingType.BOUNCE
            )
        )

        val charBot = CharacterLayerData(
            id = "char_bot_1",
            name = "Pip the Robot",
            type = CharacterType.PRESET_ROBOT,
            zIndex = 2,
            isLocked = false,
            isFlippedH = true,
            mouthAnchorX = 0.50f,
            mouthAnchorY = 0.54f,
            mouthScale = 0.95f,
            assignedAudioTrackId = "track_voice_2",
            lipSyncEnabled = true,
            lipSyncSensitivity = 1.1f,
            movement = MovementData(
                preset = MovementPreset.RIGHT_TO_LEFT,
                startX = 0.85f,
                startY = 0.62f,
                endX = 0.70f,
                endY = 0.62f,
                startScale = 0.95f,
                endScale = 0.95f,
                startTimeMs = 0L,
                durationMs = 2500L,
                easing = EasingType.EASE_IN_OUT
            )
        )

        // Generate synthetic expressive speech amplitudes for the demo track
        val demoAmplitudes1 = listOf(
            0.05f, 0.12f, 0.35f, 0.58f, 0.82f, 0.64f, 0.22f, 0.08f,
            0.15f, 0.42f, 0.75f, 0.88f, 0.61f, 0.30f, 0.12f, 0.05f,
            0.10f, 0.28f, 0.65f, 0.92f, 0.70f, 0.35f, 0.10f, 0.04f,
            0.18f, 0.52f, 0.80f, 0.60f, 0.25f, 0.08f, 0.02f, 0.0f
        )

        val demoAmplitudes2 = listOf(
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            0.05f, 0.22f, 0.45f, 0.72f, 0.85f, 0.62f, 0.38f, 0.15f,
            0.08f, 0.32f, 0.78f, 0.90f, 0.74f, 0.40f, 0.18f, 0.06f,
            0.12f, 0.36f, 0.68f, 0.82f, 0.54f, 0.20f, 0.05f, 0.0f
        )

        val audioTrack1 = AudioTrackData(
            id = "track_voice_1",
            name = "Rex's Hello",
            filePath = "",
            startTimeMs = 200L,
            durationMs = 2800L,
            volume = 1.0f,
            isVoiceOver = true,
            amplitudes = demoAmplitudes1
        )

        val audioTrack2 = AudioTrackData(
            id = "track_voice_2",
            name = "Pip's Reply",
            filePath = "",
            startTimeMs = 2600L,
            durationMs = 3000L,
            volume = 1.0f,
            isVoiceOver = true,
            amplitudes = demoAmplitudes2
        )

        val scene1 = SceneData(
            id = "scene_starter_1",
            name = "City Meetup",
            backgroundType = BackgroundType.PRESET_CITY,
            startTimeMs = 0L,
            durationMs = 6000L,
            transition = SceneTransition.NONE,
            characters = listOf(charDino, charBot),
            effects = listOf(
                EffectData(
                    id = "fx_sparkle",
                    type = EffectType.SPARKLE,
                    startTimeMs = 0L,
                    durationMs = 6000L,
                    intensity = 0.7f
                )
            )
        )

        return Project(
            id = UUID.randomUUID().toString(),
            title = "Rex & Pip Animation",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            durationMs = 6000L,
            fps = 30,
            width = 1080,
            height = 1920,
            scenes = listOf(scene1),
            audioTracks = listOf(audioTrack1, audioTrack2)
        )
    }

    companion object {
        fun create(context: Context): ProjectRepository {
            val db = AppDatabase.getDatabase(context)
            return ProjectRepository(db.projectDao())
        }
    }
}
