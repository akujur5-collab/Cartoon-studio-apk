package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.*
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ToonCraft", appName)
    }

    @Test
    fun projectSerialization_isAccurate() {
        val proj = Project(
            title = "Test Cartoon",
            durationMs = 5000L,
            scenes = listOf(
                SceneData(
                    name = "Scene 1",
                    backgroundType = BackgroundType.PRESET_CITY,
                    characters = listOf(
                        CharacterLayerData(
                            name = "Rex",
                            type = CharacterType.PRESET_DINO,
                            mouthAnchorX = 0.5f,
                            mouthAnchorY = 0.45f
                        )
                    )
                )
            )
        )
        val json = ProjectJsonSerializer.projectToJson(proj)
        val deserialized = ProjectJsonSerializer.projectFromJson(json)

        assertEquals("Test Cartoon", deserialized.title)
        assertEquals(1, deserialized.scenes.size)
        assertEquals("Rex", deserialized.scenes[0].characters[0].name)
    }
}
