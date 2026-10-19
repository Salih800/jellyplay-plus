package com.raulshma.jellyplay.feature.player.video.engine

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.raulshma.jellyplay.feature.player.video.subtitle.AndroidFontProvider
import okhttp3.OkHttpClient
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DecoderCounters
import androidx.media3.common.Format
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The [MediaEngine] contract laws against the REAL ExoPlayerEngine adapter
 * (the jvmTest lane runs the same laws against the headless fake — see
 * MediaEngineContractLawTest). Scoped to what a headless Robolectric
 * environment can drive through the interface without a live media period:
 * the fresh-engine idle surface, release idempotence (double release, and
 * the transport commands' null-player guards), the no-buffer-surface
 * default, and the capability-reporting contract (the EXO matrix row).
 * The chassis-level reset choreography these rides on is pinned in
 * player-contract's EngineStateChassisTest; the load/prepare path needs a
 * real media pipeline and stays out of scope here.
 */
@RunWith(AndroidJUnit4::class)
class ExoPlayerEngineContractLawTest {

    private fun engine(): ExoPlayerEngine = ExoPlayerEngine(
        context = androidx.test.core.app.ApplicationProvider.getApplicationContext(),
        streamingOkHttpClient = OkHttpClient(),
        fontProvider = AndroidFontProvider(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
        ),
    )

    @Test
    fun stats_reportAllDroppedFramesAndTheActualDecoderComponent() {
        val engine = engine()
        val format = Format.Builder()
            .setSampleMimeType("video/avc").setCodecs("avc1.640028").setWidth(1280).setHeight(720).build()
        val player = Proxy.newProxyInstance(
            ExoPlayer::class.java.classLoader, arrayOf(ExoPlayer::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getBufferedPosition" -> 1000L
                "getCurrentPosition" -> 100L
                "getVideoFormat" -> format
                else -> when (method.returnType) {
                    java.lang.Boolean.TYPE -> false
                    java.lang.Integer.TYPE -> 0
                    java.lang.Long.TYPE -> 0L
                    else -> null
                }
            }
        } as ExoPlayer
        val counters = DecoderCounters().apply {
            droppedBufferCount = 17
            droppedInputBufferCount = 2
            droppedToKeyframeCount = 1
            renderedOutputBufferCount = 83
        }
        fun set(name: String, value: Any) {
            ExoPlayerEngine::class.java.getDeclaredField(name).apply { isAccessible = true }.set(engine, value)
        }
        set("player", player)
        set("videoDecoderCounters", counters)
        set("videoDecoderName", "c2.android.avc.decoder")
        ExoPlayerEngine::class.java.getDeclaredMethod("updateVideoStats").apply { isAccessible = true }.invoke(engine)
        assertEquals(17L, engine.videoStats.value.droppedFrames)
        assertEquals(83L, engine.videoStats.value.totalVideoFrames)
        assertEquals("c2.android.avc.decoder", engine.videoStats.value.videoDecoder)
        assertEquals("1280x720", engine.videoStats.value.videoResolution)
        engine.release()
    }

    @Test
    fun freshEngine_publishesTheIdleSurface() {
        val engine: MediaEngine = engine()

        assertEquals(EnginePlaybackState.IDLE, engine.playbackState.value)
        assertFalse(engine.isPlaying.value)
        assertEquals(0L, engine.currentPositionMs)
        assertEquals(0L, engine.durationMs)
        assertTrue(engine.availableTracks.value.isEmpty())
        assertTrue("no buffer surface without a media period — never a fake full-track band", engine.bufferedRanges.value.isEmpty())
        assertTrue(engine.currentCues.value.isEmpty())
    }

    @Test
    fun release_isIdempotent_andLeavesTheIdleSurface() {
        val engine: MediaEngine = engine()

        engine.release()
        engine.release() // the law: repeatable — no throw, no state flip-flop

        assertEquals(EnginePlaybackState.IDLE, engine.playbackState.value)
        assertFalse(engine.isPlaying.value)
        // A release-fresh engine still obeys the interface (the next load
        // rebuilds through BasePlayerEngine's self-healing scope — pinned at
        // the base level in BasePlayerEngineScopeLawTest).
        engine.pause()
        engine.stop()
        assertEquals(EnginePlaybackState.IDLE, engine.playbackState.value)
    }

    @Test
    fun capabilities_carryTheExoMatrixRow_stably() {
        val engine: MediaEngine = engine()

        assertSame("a stable value — the UI reads it once to show/hide controls", engine.capabilities, engine.capabilities)
        assertEquals(EngineCapabilityMatrix.EXO_PLAYER, engine.capabilities)
        assertTrue(engine.capabilities.supportsPip)
        assertTrue(engine.capabilities.supportsScreenshot)
    }
}
