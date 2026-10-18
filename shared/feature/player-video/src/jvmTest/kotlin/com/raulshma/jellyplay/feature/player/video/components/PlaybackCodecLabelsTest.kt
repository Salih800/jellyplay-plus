package com.raulshma.jellyplay.feature.player.video.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlaybackCodecLabelsTest {
    @Test
    fun audioCodecNames_doNotIncludeDecoderDescriptions() {
        for ((raw, expected) in listOf(
            "AAC (Advanced Audio Coding)" to "AAC",
            "aac (AAC (Advanced Audio Coding))" to "AAC",
            "OPUS (Opus Interactive Audio Codec)" to "Opus",
            "mp4a-latm" to "AAC",
            "audio/aac" to "AAC",
            "EAC3 (Dolby Digital Plus)" to "E-AC-3",
            "flac (Free Lossless Audio Codec)" to "FLAC",
            "dca (DTS Coherent Acoustics)" to "DTS",
        )) assertEquals(expected, formatAudioCodec(raw), raw)
    }

    @Test
    fun videoCodecNames_normalizeEngineAliases() {
        assertEquals("H.264", formatVideoCodec("h264 (H.264 / AVC)"))
        assertEquals("HEVC", formatVideoCodec("video/hevc"))
        assertEquals("AV1", formatVideoCodec("av01"))
    }

    @Test
    fun hdrNames_distinguishSdrAndHdrFormats() {
        assertNull(formatPlaybackHdr("SDR"))
        assertNull(formatPlaybackHdr(null))
        assertEquals("Dolby Vision", formatPlaybackHdr("DOVIWithHDR10"))
        assertEquals("Dolby Vision", formatPlaybackHdr("Dolby Vision"))
        assertEquals("HDR10+", formatPlaybackHdr("hdr10_plus"))
    }

    @Test
    fun engineName_isCompactForTheCombinedMethodChip() {
        assertEquals("Exo", compactPlaybackEngineName("ExoPlayer"))
        assertEquals("MPV", compactPlaybackEngineName("mpv"))
        assertEquals("VLC", compactPlaybackEngineName("LibVLC"))
        assertNull(compactPlaybackEngineName(null))
    }
}
