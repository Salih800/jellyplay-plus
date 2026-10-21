package com.raulshma.jellyplay.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.Settings
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingListItemLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun narrowRowKeepsLongSelectedValueBelowReadableTitle() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(360.dp)) {
                    SettingListItem(
                        icon = Tabler.Outline.Settings,
                        title = "Selection Preset",
                        subtitle = "Choose how audio and subtitles are selected.",
                        trailingText = "Subbed shows (original audio with preferred subtitles)",
                        onClick = {},
                    )
                }
            }
        }
        val title = compose.onNodeWithText("Selection Preset", useUnmergedTree = true)
        val value = compose.onNodeWithText("Subbed shows (original audio with preferred subtitles)", useUnmergedTree = true)
        title.assertIsDisplayed()
        value.assertIsDisplayed()
        val titleBounds = title.getUnclippedBoundsInRoot()
        assertTrue("Title bounds: $titleBounds", titleBounds.right - titleBounds.left > 100.dp)
        assertTrue(value.getUnclippedBoundsInRoot().top >= title.getUnclippedBoundsInRoot().bottom)
    }
}
