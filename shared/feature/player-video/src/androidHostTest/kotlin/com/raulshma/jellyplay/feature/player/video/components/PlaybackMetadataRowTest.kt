package com.raulshma.jellyplay.feature.player.video.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.raulshma.jellyplay.feature.player.video.engine.PlaybackMetadataSnapshot
import com.raulshma.jellyplay.core.model.MediaStream
import com.raulshma.jellyplay.core.model.StreamType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackMetadataRowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun methodAndEngineChanges_replaceSourceMetadataWithTheCurrentDecodedStream() {
        val method = mutableStateOf("Direct Play")
        val engine = mutableStateOf("ExoPlayer")
        val directStats = PlaybackMetadataSnapshot(
            videoCodec = "hevc", videoResolution = "3840x2160", videoHdrType = "Dolby Vision",
            audioCodec = "truehd", audioChannels = 8,
        )
        val stats = mutableStateOf(directStats)
        val source = listOf(
            MediaStream(index = 0, type = StreamType.VIDEO, codec = "hevc", width = 3840, height = 2160, videoRange = "HDR", videoDoViTitle = "Dolby Vision"),
            MediaStream(index = 1, type = StreamType.AUDIO, codec = "truehd", channels = 8, title = "Atmos"),
        )
        composeTestRule.setContent {
            MaterialTheme {
                PlaybackMetadataRow(
                    playMethod = method.value, engineName = engine.value,
                    isDirectPlayForced = false, hdrType = "HDR", mediaStreams = source,
                    videoStats = stats.value, audioTracks = emptyList(),
                )
            }
        }
        for (label in listOf("Exo · Direct Play", "4K", "HEVC", "Dolby Vision", "TrueHD Atmos", "7.1")) {
            composeTestRule.onNodeWithText(label).assertIsDisplayed()
        }
        // The new engine has not reported its format yet. Do not keep the
        // previous stream or fall back to the original file's HDR/HEVC tags.
        composeTestRule.runOnIdle { method.value = "Transcode"; stats.value = PlaybackMetadataSnapshot() }
        composeTestRule.onNodeWithText("Exo · Transcode").assertIsDisplayed()
        for (label in listOf("4K", "HEVC", "Dolby Vision", "TrueHD Atmos", "7.1")) {
            composeTestRule.onNodeWithText(label).assertDoesNotExist()
        }
        composeTestRule.runOnIdle {
            stats.value = PlaybackMetadataSnapshot(
                videoCodec = "h264", videoResolution = "1280x720",
                audioCodec = "AAC (Advanced Audio Coding)", audioChannels = 2,
            )
        }
        for (label in listOf("720p", "H.264", "AAC", "Stereo")) {
            composeTestRule.onNodeWithText(label).assertIsDisplayed()
        }
        composeTestRule.onNodeWithText("Dolby Vision").assertDoesNotExist()
        composeTestRule.onNodeWithText("AAC Atmos").assertDoesNotExist()
        composeTestRule.runOnIdle { method.value = "Direct Play"; engine.value = "mpv"; stats.value = directStats }
        for (label in listOf("MPV · Direct Play", "HEVC", "4K", "Dolby Vision")) {
            composeTestRule.onNodeWithText(label).assertIsDisplayed()
        }
        composeTestRule.onNodeWithText("H.264").assertDoesNotExist()
    }

    @Test
    fun offlinePlayback_showsEngineCodecsResolutionAndHdr_withoutServerStreams() {
        val metadata = mutableStateOf(PlaybackMetadataSnapshot())
        composeTestRule.setContent {
            MaterialTheme {
                PlaybackMetadataRow(
                    playMethod = "Offline",
                    isDirectPlayForced = false,
                    hdrType = null,
                    mediaStreams = emptyList(),
                    videoStats = metadata.value,
                    audioTracks = emptyList(),
                )
            }
        }

        composeTestRule.onNodeWithText("Offline").assertIsDisplayed()
        composeTestRule.onNodeWithText("HEVC").assertDoesNotExist()
        composeTestRule.runOnIdle {
            metadata.value = PlaybackMetadataSnapshot(
                videoCodec = "hevc",
                videoResolution = "3840x2160",
                videoHdrType = "HDR10",
                audioCodec = "mp4a-latm",
                audioChannels = 2,
            )
        }
        for (label in listOf("Offline", "4K", "HEVC", "HDR10", "AAC", "Stereo")) {
            composeTestRule.onNodeWithText(label).assertIsDisplayed()
        }
    }
}
