package com.raulshma.jellyplay.feature.player.video.engine

import android.os.Handler
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode

@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.PAUSED)
class MpvFileLoadedDispatchTest {
    @Test
    fun subtitleWork_doesNotBlockTheNativeEventCallback() {
        val events = mutableListOf<String>()
        val callback = Thread {
            postMpvFileLoadedWork(Handler(Looper.getMainLooper()), isReleased = { false }) {
                events += "sub-add"
                false
            }
            events += "observer-returned"
        }
        callback.start()
        callback.join(1000)
        assertFalse(callback.isAlive)
        assertEquals(listOf("observer-returned"), events)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf("observer-returned", "sub-add"), events)
    }

    @Test
    fun releaseBeforeDispatch_skipsWorkAgainstTheRetiredNativeHandle() {
        var released = false
        var called = false
        postMpvFileLoadedWork(Handler(Looper.getMainLooper()), isReleased = { released }) {
            called = true
            false
        }
        released = true
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(called)
    }

    @Test
    fun subtitleBatches_yieldToInputAndStopWhenReleased() {
        val events = mutableListOf<String>()
        val handler = Handler(Looper.getMainLooper())
        var released = false
        postMpvFileLoadedWork(handler, isReleased = { released }) {
            events += "sub-add"
            true
        }
        handler.post {
            events += "input"
            released = true
        }
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf("sub-add", "input"), events)
    }
}
