package com.example.data.model

import java.util.UUID

enum class CharacterType {
    PRESET_ROBOT,
    PRESET_DINO,
    PRESET_CAT,
    PRESET_BOB,
    CUSTOM_IMAGE
}

enum class VisemeType(val displayName: String, val phoneticExample: String) {
    CLOSED("Closed", "Silence / Rest"),
    OPEN("Open", "Normal Talk"),
    WIDE_OPEN("Wide Open", "Shouting / Exclamation"),
    SMILE("Smile", "Happy / Grin"),
    O("O / Oh", "Round: o, u, oo"),
    E("E / Ee", "Spread: e, i, ay"),
    A("A / Ah", "Open: a, uh"),
    FV("F / V", "Teeth-to-lip: f, v"),
    MBP("M / B / P", "Closed lips: m, b, p")
}

enum class MovementPreset(val displayName: String) {
    STATIC("Static (No Move)"),
    LEFT_TO_RIGHT("Left → Right"),
    RIGHT_TO_LEFT("Right → Left"),
    TOP_TO_BOTTOM("Top → Bottom"),
    BOTTOM_TO_TOP("Bottom → Top"),
    ZOOM_IN("Zoom In"),
    ZOOM_OUT("Zoom Out"),
    BOUNCE("Playful Bounce"),
    SHAKE("Nervous Shake"),
    CUSTOM_WAYPOINTS("Custom Path")
}

enum class EasingType(val displayName: String) {
    LINEAR("Linear"),
    EASE_IN_OUT("Smooth Ease In-Out"),
    BOUNCE("Bounce Out"),
    ELASTIC("Spring Elastic")
}

enum class BackgroundType {
    PRESET_CITY,
    PRESET_SPACE,
    PRESET_FOREST,
    PRESET_STUDIO,
    PRESET_SUNSET,
    CUSTOM_IMAGE,
    SOLID_COLOR
}

enum class SceneTransition(val displayName: String) {
    NONE("Cut"),
    FADE("Fade Through Black"),
    SLIDE_LEFT("Slide Left"),
    SLIDE_RIGHT("Slide Right"),
    ZOOM_IN("Zoom Transition")
}

enum class EffectType(val displayName: String) {
    NONE("None"),
    FADE_IN("Fade In"),
    FADE_OUT("Fade Out"),
    ZOOM_PULSE("Pulse Zoom"),
    SHAKE("Screen Shake"),
    BOUNCE("Bounce"),
    GLOW("Magic Glow"),
    SPARKLE("Sparkles"),
    VINTAGE_FILM("Vintage Film")
}

data class PointFData(
    val x: Float = 0f,
    val y: Float = 0f
)

data class MovementData(
    val preset: MovementPreset = MovementPreset.STATIC,
    val startX: Float = 0.5f,
    val startY: Float = 0.55f,
    val endX: Float = 0.5f,
    val endY: Float = 0.55f,
    val startScale: Float = 1.0f,
    val endScale: Float = 1.0f,
    val startRotation: Float = 0f,
    val endRotation: Float = 0f,
    val startTimeMs: Long = 0L,
    val durationMs: Long = 3000L,
    val easing: EasingType = EasingType.EASE_IN_OUT,
    val waypoints: List<PointFData> = emptyList()
)

data class CharacterLayerData(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Character",
    val type: CharacterType = CharacterType.PRESET_DINO,
    val imageUri: String? = null,
    val zIndex: Int = 0,
    val isLocked: Boolean = false,
    val isFlippedH: Boolean = false,
    val mouthAnchorX: Float = 0.5f,
    val mouthAnchorY: Float = 0.52f,
    val mouthScale: Float = 1.0f,
    val assignedAudioTrackId: String? = null,
    val lipSyncEnabled: Boolean = true,
    val lipSyncSensitivity: Float = 1.0f,
    val movement: MovementData = MovementData(),
    val manualViseme: VisemeType? = null
)

data class AudioTrackData(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Voice-over",
    val filePath: String = "",
    val startTimeMs: Long = 0L,
    val durationMs: Long = 4000L,
    val volume: Float = 1.0f,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
    val isVoiceOver: Boolean = true,
    val amplitudes: List<Float> = emptyList()
)

data class EffectData(
    val id: String = UUID.randomUUID().toString(),
    val type: EffectType = EffectType.NONE,
    val targetCharacterId: String? = null,
    val startTimeMs: Long = 0L,
    val durationMs: Long = 2000L,
    val intensity: Float = 0.8f
)

data class SceneData(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Scene 1",
    val backgroundType: BackgroundType = BackgroundType.PRESET_CITY,
    val backgroundCustomUri: String? = null,
    val backgroundColorHex: Long = 0xFF1E293B,
    val startTimeMs: Long = 0L,
    val durationMs: Long = 6000L,
    val transition: SceneTransition = SceneTransition.NONE,
    val characters: List<CharacterLayerData> = emptyList(),
    val effects: List<EffectData> = emptyList()
)

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Cartoon Video",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val durationMs: Long = 6000L,
    val fps: Int = 30,
    val width: Int = 1080,
    val height: Int = 1920,
    val scenes: List<SceneData> = emptyList(),
    val audioTracks: List<AudioTrackData> = emptyList()
)
