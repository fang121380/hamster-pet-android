package com.andrew.hamsterpet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetUiModelTest {
    @Test
    fun nest_action_names_each_missing_action_and_enables_at_threshold() {
        val waiting = PetUiModel.nestAction(PetState(feedingCount = 1, interactionCount = 1))
        val ready = PetUiModel.nestAction(PetState(feedingCount = 3, interactionCount = 2))

        assertFalse(waiting.enabled)
        assertEquals("还需喂食 2 次、互动 1 次", waiting.supportingText)
        assertTrue(ready.enabled)
        assertEquals("建造小窝", ready.label)
    }

    @Test
    fun breeding_action_counts_down_then_becomes_birth_ready() {
        val startedAt = 1_000L
        val state = PetState(
            nestLevel = 2,
            satiety = 80,
            affection = 80,
            breedingStartedAt = startedAt,
        )

        val running = PetUiModel.breedingAction(state, startedAt + 61_000L)
        val ready = PetUiModel.breedingAction(state, startedAt + PetGameEngine.BREEDING_DURATION_MS)

        assertFalse(running.enabled)
        assertEquals("等待出生 03:59", running.label)
        assertTrue(ready.enabled)
        assertEquals("迎接幼崽", ready.label)
    }
}
