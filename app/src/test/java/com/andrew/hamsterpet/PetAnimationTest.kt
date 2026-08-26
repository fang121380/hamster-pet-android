package com.andrew.hamsterpet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetAnimationTest {
    @Test
    fun atlas_contract_has_twelve_unique_eight_frame_rows() {
        assertEquals(12, PetAnimation.entries.size)
        assertEquals(12, PetAnimation.entries.map { it.row }.distinct().size)
        assertTrue(PetAnimation.entries.all { it.frameCount == 8 })
    }

    @Test
    fun finite_animation_marks_completion_on_its_last_frame() {
        val animation = PetAnimation.EAT
        val duration = animation.frameCount * animation.frameDurationMs * animation.repeatCount

        val beforeEnd = animation.frameAt(duration - 1)
        val completed = animation.frameAt(duration)

        assertFalse(beforeEnd.complete)
        assertEquals(animation.frameCount - 1, completed.index)
        assertTrue(completed.complete)
    }

    @Test
    fun looping_animation_wraps_without_completing() {
        val animation = PetAnimation.IDLE
        val elapsed = animation.frameCount * animation.frameDurationMs * 3L

        assertEquals(0, animation.frameAt(elapsed).index)
        assertFalse(animation.frameAt(elapsed).complete)
    }
}
