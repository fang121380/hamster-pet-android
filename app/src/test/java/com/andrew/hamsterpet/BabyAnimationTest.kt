package com.andrew.hamsterpet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BabyAnimationTest {
    @Test
    fun idle_loops_through_all_pixel_frames() {
        val frame = BabyAnimation.IDLE.frameAt(BabyAnimation.IDLE.frameDurationMs * 9)

        assertEquals(1, frame.index)
        assertFalse(frame.complete)
    }

    @Test
    fun feed_and_play_complete_after_two_cycles() {
        val feed = BabyAnimation.FEED.frameAt(BabyAnimation.FEED.frameDurationMs * 16)
        val play = BabyAnimation.PLAY.frameAt(BabyAnimation.PLAY.frameDurationMs * 16)

        assertTrue(feed.complete)
        assertTrue(play.complete)
        assertEquals(7, feed.index)
        assertEquals(7, play.index)
    }
}
