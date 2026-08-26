package com.andrew.hamsterpet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetMotionPolicyTest {
    @Test
    fun direct_interactions_interrupt_idle_and_walking() {
        assertTrue(PetMotionPolicy.canStart(PetMotionAction.PAT, PetMotionAction.IDLE))
        assertTrue(PetMotionPolicy.canStart(PetMotionAction.FEED, PetMotionAction.WALK))
        assertTrue(PetMotionPolicy.canStart(PetMotionAction.DRAG, PetMotionAction.WALK))
    }

    @Test
    fun idle_never_interrupts_an_active_interaction() {
        assertFalse(PetMotionPolicy.canStart(PetMotionAction.IDLE, PetMotionAction.PAT))
        assertFalse(PetMotionPolicy.canStart(PetMotionAction.IDLE, PetMotionAction.SLEEP))
        assertFalse(PetMotionPolicy.canStart(PetMotionAction.IDLE, PetMotionAction.FEED))
    }

    @Test
    fun close_has_the_highest_priority() {
        PetMotionAction.entries.filterNot { it == PetMotionAction.CLOSE }.forEach { current ->
            assertTrue(PetMotionPolicy.canStart(PetMotionAction.CLOSE, current))
            assertFalse(PetMotionPolicy.canStart(current, PetMotionAction.CLOSE))
        }
    }
}
