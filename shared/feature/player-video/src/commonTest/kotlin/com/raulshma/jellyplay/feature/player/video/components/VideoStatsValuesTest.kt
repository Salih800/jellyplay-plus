package com.raulshma.jellyplay.feature.player.video.components

import com.raulshma.jellyplay.feature.player.video.engine.EngineVideoStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VideoStatsValuesTest {
    @Test fun streamBitrate_sumsAudioAndVideoAndPreservesUnknown() {
        assertNull(streamBitrate(EngineVideoStats()))
        assertEquals(1_128_000, streamBitrate(EngineVideoStats(videoBitrate = 1_000_000, audioBitrate = 128_000)))
        assertEquals(128_000, streamBitrate(EngineVideoStats(audioBitrate = 128_000)))
        assertNull(streamBitrate(EngineVideoStats(videoBitrate = -1, audioBitrate = 0)))
    }

    @Test fun descriptiveAudioCodec_usesCompactName() {
        assertEquals("AAC", formatAudioCodec("AAC (Advanced Audio Coding)"))
        assertEquals("AAC", formatAudioCodec("Advanced Audio Codec"))
        assertEquals("AC-3", formatAudioCodec("ac3"))
        assertEquals("E-AC-3", formatAudioCodec("eac3"))
        assertEquals("AC-3 / E-AC-3", formatAudioCodec("ATSC A/52B (AC-3, E-AC-3)"))
    }
}
