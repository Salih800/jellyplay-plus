package com.raulshma.jellyplay.feature.player.video.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.raulshma.jellyplay.feature.player.video.engine.EngineVideoStats
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoStatsOverlayTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun compactViewport_scrollsToEngineAndKeepsCloseVisible() {
        var closed = false
        rule.setContent {
            MaterialTheme {
                Box(Modifier.size(340.dp, 240.dp)) {
                    VideoStatsOverlay(
                        statsFlow = MutableStateFlow(EngineVideoStats(
                            videoCodec = "h264", videoResolution = "1280x720",
                            audioCodec = "AAC (Advanced Audio Coding)", videoBitrate = 1_000_000, audioBitrate = 128_000,
                        )),
                        currentPositionFlow = MutableStateFlow(1000L), durationMs = 10_000,
                        playbackSpeed = 1f, isPlaying = true, playbackState = "Playing",
                        playMethod = "Transcode", streamingQuality = "720p", playerType = "ExoPlayer",
                        decoderMode = "Hardware preferred", audioSessionId = 0,
                        onClose = { closed = true },
                    )
                }
            }
        }
        rule.onNodeWithTag("video-stats-content").performScrollToNode(hasText("AAC"))
        rule.onNodeWithText("AAC").assertIsDisplayed()
        rule.onNodeWithText("AAC (Advanced Audio Coding)").assertDoesNotExist()
        rule.onNodeWithTag("video-stats-content").performScrollToNode(hasText("720p"))
        rule.onNodeWithTag("video-stats-content").performScrollToNode(hasText("1.1 Mbps"))
        rule.onNodeWithText("1.1 Mbps").assertIsDisplayed()
        rule.onNodeWithTag("video-stats-content").performScrollToNode(hasText("ExoPlayer"))
        rule.onNodeWithText("ExoPlayer").assertIsDisplayed()
        rule.onNodeWithContentDescription("Close playback statistics").assertIsDisplayed().performClick()
        assertTrue(closed)
    }
}
