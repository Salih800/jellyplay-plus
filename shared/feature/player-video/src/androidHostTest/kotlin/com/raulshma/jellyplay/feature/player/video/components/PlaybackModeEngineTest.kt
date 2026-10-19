package com.raulshma.jellyplay.feature.player.video.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsNotEnabled
import com.raulshma.jellyplay.core.model.StreamingQuality
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.raulshma.jellyplay.core.model.PlaybackMode
import com.raulshma.jellyplay.core.model.PlayerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackModeEngineTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun forcedDirectPlayback_disablesResolutionSelection() {
        composeTestRule.setContent {
            MaterialTheme {
                PlaybackModeSheet(currentMode = PlaybackMode.FORCE_DIRECT_PLAY, onSelect = {}, onDismiss = {})
            }
        }
        composeTestRule.onNodeWithText("720p").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun transcodeQuality_dispatchesWithoutClosingTheOptions() {
        var quality: StreamingQuality? = null
        var dismissed = false
        composeTestRule.setContent {
            MaterialTheme {
                PlaybackModeSheet(
                    currentMode = PlaybackMode.FORCE_TRANSCODE, onSelect = {}, onDismiss = { dismissed = true },
                    onQualitySelect = { quality = it },
                )
            }
        }
        composeTestRule.onNodeWithText("720p").performScrollTo().performClick()
        assertEquals(StreamingQuality.HD_720P, quality)
        assertFalse(dismissed)
    }

    @Test
    fun selectingAnotherEngine_dispatchesTheChoiceAndKeepsOptionsOpen() {
        var selected: PlayerType? = null
        var dismissed = false
        composeTestRule.setContent {
            MaterialTheme {
                PlaybackModeSheet(
                    currentMode = PlaybackMode.AUTO, onSelect = {}, onDismiss = { dismissed = true },
                    currentEngineName = "ExoPlayer", availablePlayers = listOf(PlayerType.EXO_PLAYER, PlayerType.MPV),
                    onPlayerSelect = { selected = it },
                )
            }
        }
        composeTestRule.onNodeWithText("mpv").performClick()
        assertEquals(PlayerType.MPV, selected)
        assertFalse(dismissed)
    }

    @Test
    fun selectingActiveEngine_doesNotReloadPlayback() {
        var selected: PlayerType? = null
        composeTestRule.setContent {
            MaterialTheme {
                PlaybackModeSheet(
                    currentMode = PlaybackMode.AUTO, onSelect = {}, onDismiss = {},
                    currentEngineName = "ExoPlayer", availablePlayers = listOf(PlayerType.EXO_PLAYER, PlayerType.MPV),
                    onPlayerSelect = { selected = it },
                )
            }
        }
        composeTestRule.onNodeWithText("ExoPlayer").performClick()
        assertNull(selected)
    }
}
