package com.raulshma.jellyplay.feature.player.video.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import com.raulshma.jellyplay.feature.player.video.engine.EngineVideoStats
import com.raulshma.jellyplay.core.model.MediaSource
import com.raulshma.jellyplay.core.model.MediaStream
import com.raulshma.jellyplay.core.model.StreamType
import org.junit.Rule
import org.junit.Test
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackInfoOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mediaSource = MediaSource(
        id = "1",
        name = "video.mkv",
        container = "mkv",
        size = 1_073_741_824L,
        bitrate = 8_000_000L,
    )

    private val mediaStreams = listOf(
        MediaStream(
            index = 0,
            type = StreamType.VIDEO,
            codec = "hevc",
            width = 3840,
            height = 2160,
            bitRate = 6_000_000L,
            realFrameRate = 23.976f,
            videoRange = "HDR10",
        ),
        MediaStream(
            index = 1,
            type = StreamType.AUDIO,
            codec = "dts",
            language = "eng",
            title = "Surround",
            channels = 6,
        ),
        MediaStream(
            index = 2,
            type = StreamType.SUBTITLE,
            language = "eng",
            title = "English",
            isDefault = true,
        ),
    )

    @Test
    fun activeStream_updatesWithoutFallingBackToOriginalFile() {
        val stats = MutableStateFlow(EngineVideoStats(videoCodec = "h264", videoResolution = "1280x720", audioCodec = "eac3"))
        val method = mutableStateOf("Transcode")
        composeTestRule.setContent {
            MaterialTheme {
                PlaybackInfoOverlay(mediaSource, stats, mediaStreams, method.value, playerType = "mpv")
            }
        }
        composeTestRule.onNodeWithText("H.264").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("1280x720").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("E-AC-3").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("HEVC").assertDoesNotExist()
        composeTestRule.onNodeWithText("3840x2160").assertDoesNotExist()
        composeTestRule.runOnIdle { stats.value = EngineVideoStats() }
        composeTestRule.onNodeWithText("Waiting for stream information…").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("1280x720").assertDoesNotExist()
        composeTestRule.onNodeWithText("HEVC").assertDoesNotExist()
        composeTestRule.runOnIdle {
            method.value = "Direct Play"
            stats.value = EngineVideoStats(videoCodec = "hevc", videoResolution = "3840x2160")
        }
        composeTestRule.onNodeWithText("HEVC").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("3840x2160").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun originalFile_isExplicitAndCollapsedUntilRequested() {
        val stats = MutableStateFlow(EngineVideoStats(videoCodec = "h264", videoResolution = "1280x720"))
        composeTestRule.setContent {
            MaterialTheme {
                PlaybackInfoOverlay(mediaSource, stats, mediaStreams, "Transcode")
            }
        }
        composeTestRule.onNodeWithText("HEVC").assertDoesNotExist()
        composeTestRule.onNodeWithText("Original file").performScrollTo().performClick()
        composeTestRule.onNodeWithText("HEVC").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("3840x2160").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun playbackInfoOverlay_displaysTitle() {
        composeTestRule.setContent {
            MaterialTheme {
                PlaybackInfoOverlay(
                    mediaSource = mediaSource,
                    mediaStreams = mediaStreams,
                    playMethod = "Direct Play",
                )
            }
        }
        composeTestRule.onNodeWithText("Playback Info").assertIsDisplayed()
    }

    @Test
    fun playbackInfoOverlay_displaysMediaSourceSection() {
        composeTestRule.setContent {
            MaterialTheme {
                PlaybackInfoOverlay(
                    mediaSource = mediaSource,
                    mediaStreams = mediaStreams,
                    playMethod = "Direct Play",
                )
            }
        }
        composeTestRule.onNodeWithText("Original file").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Media Source").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("mkv").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Direct Play").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun playbackInfoOverlay_displaysVideoSection() {
        composeTestRule.setContent {
            MaterialTheme {
                PlaybackInfoOverlay(
                    mediaSource = mediaSource,
                    mediaStreams = mediaStreams,
                    playMethod = "Direct Play",
                )
            }
        }
        composeTestRule.onNodeWithText("Original file").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Video").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("HEVC").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("3840x2160").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun playbackInfoOverlay_displaysFrameRate() {
        composeTestRule.setContent {
            MaterialTheme {
                PlaybackInfoOverlay(
                    mediaSource = mediaSource,
                    mediaStreams = mediaStreams,
                    playMethod = "Direct Play",
                )
            }
        }
        composeTestRule.onNodeWithText("Original file").performScrollTo().performClick()
        composeTestRule.onNodeWithText("23.98 fps").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun playbackInfoOverlay_displaysHdrType() {
        composeTestRule.setContent {
            MaterialTheme {
                PlaybackInfoOverlay(
                    mediaSource = mediaSource,
                    mediaStreams = mediaStreams,
                    playMethod = "Direct Play",
                    hdrType = "HDR10",
                )
            }
        }
        composeTestRule.onNodeWithText("Original file").performScrollTo().performClick()
        composeTestRule.onNodeWithText("HDR10").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun playbackInfoOverlay_displaysAudioSection() {
        composeTestRule.setContent {
            MaterialTheme {
                PlaybackInfoOverlay(
                    mediaSource = mediaSource,
                    mediaStreams = mediaStreams,
                    playMethod = "Direct Play",
                )
            }
        }
        composeTestRule.onNodeWithText("Original file").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Audio").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("DTS").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("6ch").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun playbackInfoOverlay_displaysSubtitleSection() {
        composeTestRule.setContent {
            MaterialTheme {
                PlaybackInfoOverlay(
                    mediaSource = mediaSource,
                    mediaStreams = mediaStreams,
                    playMethod = "Direct Play",
                )
            }
        }
        composeTestRule.onNodeWithText("Original file").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Subtitles").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Default").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun playbackInfoOverlay_nullMediaSource_showsUnknown() {
        composeTestRule.setContent {
            MaterialTheme {
                PlaybackInfoOverlay(
                    mediaSource = null,
                    mediaStreams = emptyList(),
                    playMethod = "Transcode",
                )
            }
        }
        composeTestRule.onNodeWithText("Original file").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Media Source").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun playbackInfoOverlay_noStreams_showsOnlyPlaybackInfoTitle() {
        composeTestRule.setContent {
            MaterialTheme {
                PlaybackInfoOverlay(
                    mediaSource = mediaSource,
                    mediaStreams = emptyList(),
                    playMethod = "Direct Play",
                )
            }
        }
        composeTestRule.onNodeWithText("Playback Info").assertIsDisplayed()
        composeTestRule.onNodeWithText("Video").assertDoesNotExist()
        composeTestRule.onNodeWithText("Audio").assertDoesNotExist()
    }

    @Test
    fun playbackInfoOverlay_displaysFileSize() {
        composeTestRule.setContent {
            MaterialTheme {
                PlaybackInfoOverlay(
                    mediaSource = mediaSource.copy(size = 1_073_741_824L),
                    mediaStreams = emptyList(),
                    playMethod = "Direct Play",
                )
            }
        }
        composeTestRule.onNodeWithText("Original file").performScrollTo().performClick()
        composeTestRule.onNodeWithText("1.00 GB").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun playbackInfoOverlay_sdrVideo_noHdrRow() {
        // An actually-SDR stream: no HDR range marker on the stream. (The
        // default fixture's videoRange = "HDR10" legitimately renders a Range
        // row even with hdrType = null — that is the stream-derived range,
        // not the HDR-format row this test means to exclude.)
        val sdrStreams = listOf(
            mediaStreams.first().copy(videoRange = "SDR"),
        )
        composeTestRule.setContent {
            MaterialTheme {
                PlaybackInfoOverlay(
                    mediaSource = mediaSource,
                    mediaStreams = sdrStreams,
                    playMethod = "Direct Play",
                    hdrType = null,
                )
            }
        }
        composeTestRule.onNodeWithText("Original file").performScrollTo().performClick()
        composeTestRule.onNodeWithText("HDR10").assertDoesNotExist()
        composeTestRule.onNodeWithText("HDR Format").assertDoesNotExist()
    }

    @Test
    fun playbackInfoOverlay_nonZeroSubtitleDelay_showsSubtitleDelayRow() {
        composeTestRule.setContent {
            MaterialTheme {
                PlaybackInfoOverlay(
                    mediaSource = mediaSource,
                    mediaStreams = mediaStreams,
                    playMethod = "Direct Play",
                    subtitleDelayMs = 1500L,
                )
            }
        }
        // The row lives in the Active Controls section, below the fold of the
        // overlay's scrollable column — scroll it into view first.
        composeTestRule.onNodeWithText("Subtitle Delay").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("+1.5s").assertIsDisplayed()
    }
}
