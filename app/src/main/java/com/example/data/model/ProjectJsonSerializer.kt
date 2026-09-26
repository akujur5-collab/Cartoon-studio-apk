package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

object ProjectJsonSerializer {

    fun projectToJson(project: Project): String {
        val root = JSONObject()
        root.put("id", project.id)
        root.put("title", project.title)
        root.put("createdAt", project.createdAt)
        root.put("updatedAt", project.updatedAt)
        root.put("durationMs", project.durationMs)
        root.put("fps", project.fps)
        root.put("width", project.width)
        root.put("height", project.height)

        // Scenes
        val scenesArr = JSONArray()
        for (scene in project.scenes) {
            val sObj = JSONObject()
            sObj.put("id", scene.id)
            sObj.put("name", scene.name)
            sObj.put("backgroundType", scene.backgroundType.name)
            sObj.put("backgroundCustomUri", scene.backgroundCustomUri ?: "")
            sObj.put("backgroundColorHex", scene.backgroundColorHex)
            sObj.put("startTimeMs", scene.startTimeMs)
            sObj.put("durationMs", scene.durationMs)
            sObj.put("transition", scene.transition.name)

            // Characters
            val charsArr = JSONArray()
            for (char in scene.characters) {
                val cObj = JSONObject()
                cObj.put("id", char.id)
                cObj.put("name", char.name)
                cObj.put("type", char.type.name)
                cObj.put("imageUri", char.imageUri ?: "")
                cObj.put("zIndex", char.zIndex)
                cObj.put("isLocked", char.isLocked)
                cObj.put("isFlippedH", char.isFlippedH)
                cObj.put("mouthAnchorX", char.mouthAnchorX.toDouble())
                cObj.put("mouthAnchorY", char.mouthAnchorY.toDouble())
                cObj.put("mouthScale", char.mouthScale.toDouble())
                cObj.put("assignedAudioTrackId", char.assignedAudioTrackId ?: "")
                cObj.put("lipSyncEnabled", char.lipSyncEnabled)
                cObj.put("lipSyncSensitivity", char.lipSyncSensitivity.toDouble())
                if (char.manualViseme != null) {
                    cObj.put("manualViseme", char.manualViseme.name)
                }

                // Movement
                val mObj = JSONObject()
                val m = char.movement
                mObj.put("preset", m.preset.name)
                mObj.put("startX", m.startX.toDouble())
                mObj.put("startY", m.startY.toDouble())
                mObj.put("endX", m.endX.toDouble())
                mObj.put("endY", m.endY.toDouble())
                mObj.put("startScale", m.startScale.toDouble())
                mObj.put("endScale", m.endScale.toDouble())
                mObj.put("startRotation", m.startRotation.toDouble())
                mObj.put("endRotation", m.endRotation.toDouble())
                mObj.put("startTimeMs", m.startTimeMs)
                mObj.put("durationMs", m.durationMs)
                mObj.put("easing", m.easing.name)

                val waypointsArr = JSONArray()
                for (wp in m.waypoints) {
                    val wpObj = JSONObject()
                    wpObj.put("x", wp.x.toDouble())
                    wpObj.put("y", wp.y.toDouble())
                    waypointsArr.put(wpObj)
                }
                mObj.put("waypoints", waypointsArr)
                cObj.put("movement", mObj)

                charsArr.put(cObj)
            }
            sObj.put("characters", charsArr)

            // Effects
            val effArr = JSONArray()
            for (eff in scene.effects) {
                val eObj = JSONObject()
                eObj.put("id", eff.id)
                eObj.put("type", eff.type.name)
                eObj.put("targetCharacterId", eff.targetCharacterId ?: "")
                eObj.put("startTimeMs", eff.startTimeMs)
                eObj.put("durationMs", eff.durationMs)
                eObj.put("intensity", eff.intensity.toDouble())
                effArr.put(eObj)
            }
            sObj.put("effects", effArr)

            scenesArr.put(sObj)
        }
        root.put("scenes", scenesArr)

        // Audio Tracks
        val audioArr = JSONArray()
        for (track in project.audioTracks) {
            val aObj = JSONObject()
            aObj.put("id", track.id)
            aObj.put("name", track.name)
            aObj.put("filePath", track.filePath)
            aObj.put("startTimeMs", track.startTimeMs)
            aObj.put("durationMs", track.durationMs)
            aObj.put("volume", track.volume.toDouble())
            aObj.put("fadeInMs", track.fadeInMs)
            aObj.put("fadeOutMs", track.fadeOutMs)
            aObj.put("isVoiceOver", track.isVoiceOver)

            val ampArr = JSONArray()
            for (amp in track.amplitudes) {
                ampArr.put(amp.toDouble())
            }
            aObj.put("amplitudes", ampArr)
            audioArr.put(aObj)
        }
        root.put("audioTracks", audioArr)

        return root.toString()
    }

    fun projectFromJson(jsonStr: String): Project {
        return try {
            val root = JSONObject(jsonStr)
            val id = root.optString("id")
            val title = root.optString("title", "Untitled Project")
            val createdAt = root.optLong("createdAt", System.currentTimeMillis())
            val updatedAt = root.optLong("updatedAt", System.currentTimeMillis())
            val durationMs = root.optLong("durationMs", 6000L)
            val fps = root.optInt("fps", 30)
            val width = root.optInt("width", 1080)
            val height = root.optInt("height", 1920)

            val scenes = mutableListOf<SceneData>()
            val scenesArr = root.optJSONArray("scenes")
            if (scenesArr != null) {
                for (i in 0 until scenesArr.length()) {
                    val sObj = scenesArr.getJSONObject(i)
                    val sId = sObj.optString("id")
                    val sName = sObj.optString("name", "Scene ${i + 1}")
                    val bgTypeStr = sObj.optString("backgroundType", BackgroundType.PRESET_CITY.name)
                    val bgType = runCatching { BackgroundType.valueOf(bgTypeStr) }.getOrDefault(BackgroundType.PRESET_CITY)
                    val bgCustomUri = sObj.optString("backgroundCustomUri").takeIf { it.isNotBlank() }
                    val bgColorHex = sObj.optLong("backgroundColorHex", 0xFF1E293BL)
                    val sStartMs = sObj.optLong("startTimeMs", 0L)
                    val sDurMs = sObj.optLong("durationMs", 6000L)
                    val transStr = sObj.optString("transition", SceneTransition.NONE.name)
                    val trans = runCatching { SceneTransition.valueOf(transStr) }.getOrDefault(SceneTransition.NONE)

                    val characters = mutableListOf<CharacterLayerData>()
                    val charsArr = sObj.optJSONArray("characters")
                    if (charsArr != null) {
                        for (j in 0 until charsArr.length()) {
                            val cObj = charsArr.getJSONObject(j)
                            val cId = cObj.optString("id")
                            val cName = cObj.optString("name", "Character ${j + 1}")
                            val cTypeStr = cObj.optString("type", CharacterType.PRESET_DINO.name)
                            val cType = runCatching { CharacterType.valueOf(cTypeStr) }.getOrDefault(CharacterType.PRESET_DINO)
                            val imageUri = cObj.optString("imageUri").takeIf { it.isNotBlank() }
                            val zIndex = cObj.optInt("zIndex", j)
                            val isLocked = cObj.optBoolean("isLocked", false)
                            val isFlippedH = cObj.optBoolean("isFlippedH", false)
                            val mouthAnchorX = cObj.optDouble("mouthAnchorX", 0.5).toFloat()
                            val mouthAnchorY = cObj.optDouble("mouthAnchorY", 0.52).toFloat()
                            val mouthScale = cObj.optDouble("mouthScale", 1.0).toFloat()
                            val assignedAudio = cObj.optString("assignedAudioTrackId").takeIf { it.isNotBlank() }
                            val lipSyncEnabled = cObj.optBoolean("lipSyncEnabled", true)
                            val lipSyncSensitivity = cObj.optDouble("lipSyncSensitivity", 1.0).toFloat()
                            val manualVisemeStr = cObj.optString("manualViseme").takeIf { it.isNotBlank() }
                            val manualViseme = manualVisemeStr?.let { runCatching { VisemeType.valueOf(it) }.getOrNull() }

                            // Movement
                            val mObj = cObj.optJSONObject("movement")
                            val movement = if (mObj != null) {
                                val mPresetStr = mObj.optString("preset", MovementPreset.STATIC.name)
                                val mPreset = runCatching { MovementPreset.valueOf(mPresetStr) }.getOrDefault(MovementPreset.STATIC)
                                val startX = mObj.optDouble("startX", 0.5).toFloat()
                                val startY = mObj.optDouble("startY", 0.55).toFloat()
                                val endX = mObj.optDouble("endX", 0.5).toFloat()
                                val endY = mObj.optDouble("endY", 0.55).toFloat()
                                val startScale = mObj.optDouble("startScale", 1.0).toFloat()
                                val endScale = mObj.optDouble("endScale", 1.0).toFloat()
                                val startRot = mObj.optDouble("startRotation", 0.0).toFloat()
                                val endRot = mObj.optDouble("endRotation", 0.0).toFloat()
                                val mStartMs = mObj.optLong("startTimeMs", 0L)
                                val mDurMs = mObj.optLong("durationMs", 3000L)
                                val easingStr = mObj.optString("easing", EasingType.EASE_IN_OUT.name)
                                val easing = runCatching { EasingType.valueOf(easingStr) }.getOrDefault(EasingType.EASE_IN_OUT)

                                val wps = mutableListOf<PointFData>()
                                val wpArr = mObj.optJSONArray("waypoints")
                                if (wpArr != null) {
                                    for (w in 0 until wpArr.length()) {
                                        val wpO = wpArr.getJSONObject(w)
                                        wps.add(PointFData(wpO.optDouble("x", 0.0).toFloat(), wpO.optDouble("y", 0.0).toFloat()))
                                    }
                                }
                                MovementData(
                                    preset = mPreset,
                                    startX = startX,
                                    startY = startY,
                                    endX = endX,
                                    endY = endY,
                                    startScale = startScale,
                                    endScale = endScale,
                                    startRotation = startRot,
                                    endRotation = endRot,
                                    startTimeMs = mStartMs,
                                    durationMs = mDurMs,
                                    easing = easing,
                                    waypoints = wps
                                )
                            } else {
                                MovementData()
                            }

                            characters.add(
                                CharacterLayerData(
                                    id = cId,
                                    name = cName,
                                    type = cType,
                                    imageUri = imageUri,
                                    zIndex = zIndex,
                                    isLocked = isLocked,
                                    isFlippedH = isFlippedH,
                                    mouthAnchorX = mouthAnchorX,
                                    mouthAnchorY = mouthAnchorY,
                                    mouthScale = mouthScale,
                                    assignedAudioTrackId = assignedAudio,
                                    lipSyncEnabled = lipSyncEnabled,
                                    lipSyncSensitivity = lipSyncSensitivity,
                                    movement = movement,
                                    manualViseme = manualViseme
                                )
                            )
                        }
                    }

                    // Effects
                    val effects = mutableListOf<EffectData>()
                    val effArr = sObj.optJSONArray("effects")
                    if (effArr != null) {
                        for (k in 0 until effArr.length()) {
                            val eObj = effArr.getJSONObject(k)
                            val eId = eObj.optString("id")
                            val eTypeStr = eObj.optString("type", EffectType.NONE.name)
                            val eType = runCatching { EffectType.valueOf(eTypeStr) }.getOrDefault(EffectType.NONE)
                            val targetCharId = eObj.optString("targetCharacterId").takeIf { it.isNotBlank() }
                            val eStartMs = eObj.optLong("startTimeMs", 0L)
                            val eDurMs = eObj.optLong("durationMs", 2000L)
                            val intensity = eObj.optDouble("intensity", 0.8).toFloat()

                            effects.add(
                                EffectData(
                                    id = eId,
                                    type = eType,
                                    targetCharacterId = targetCharId,
                                    startTimeMs = eStartMs,
                                    durationMs = eDurMs,
                                    intensity = intensity
                                )
                            )
                        }
                    }

                    scenes.add(
                        SceneData(
                            id = sId,
                            name = sName,
                            backgroundType = bgType,
                            backgroundCustomUri = bgCustomUri,
                            backgroundColorHex = bgColorHex,
                            startTimeMs = sStartMs,
                            durationMs = sDurMs,
                            transition = trans,
                            characters = characters,
                            effects = effects
                        )
                    )
                }
            }

            val audioTracks = mutableListOf<AudioTrackData>()
            val audioArr = root.optJSONArray("audioTracks")
            if (audioArr != null) {
                for (a in 0 until audioArr.length()) {
                    val aObj = audioArr.getJSONObject(a)
                    val aId = aObj.optString("id")
                    val aName = aObj.optString("name", "Voice ${a + 1}")
                    val filePath = aObj.optString("filePath", "")
                    val aStartMs = aObj.optLong("startTimeMs", 0L)
                    val aDurMs = aObj.optLong("durationMs", 4000L)
                    val volume = aObj.optDouble("volume", 1.0).toFloat()
                    val fadeInMs = aObj.optLong("fadeInMs", 0L)
                    val fadeOutMs = aObj.optLong("fadeOutMs", 0L)
                    val isVoiceOver = aObj.optBoolean("isVoiceOver", true)

                    val amps = mutableListOf<Float>()
                    val ampArr = aObj.optJSONArray("amplitudes")
                    if (ampArr != null) {
                        for (m in 0 until ampArr.length()) {
                            amps.add(ampArr.optDouble(m, 0.0).toFloat())
                        }
                    }

                    audioTracks.add(
                        AudioTrackData(
                            id = aId,
                            name = aName,
                            filePath = filePath,
                            startTimeMs = aStartMs,
                            durationMs = aDurMs,
                            volume = volume,
                            fadeInMs = fadeInMs,
                            fadeOutMs = fadeOutMs,
                            isVoiceOver = isVoiceOver,
                            amplitudes = amps
                        )
                    )
                }
            }

            Project(
                id = id,
                title = title,
                createdAt = createdAt,
                updatedAt = updatedAt,
                durationMs = durationMs,
                fps = fps,
                width = width,
                height = height,
                scenes = scenes,
                audioTracks = audioTracks
            )
        } catch (e: Exception) {
            Project(title = "Default Cartoon Project")
        }
    }
}
