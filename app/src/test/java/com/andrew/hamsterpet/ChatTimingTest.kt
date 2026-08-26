package com.andrew.hamsterpet

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatTimingTest {
    @Test
    fun `keeps thinking feedback visible for at least seven hundred milliseconds`() {
        assertEquals(700L, ChatTiming.remainingThinkingDelay(startedAt = 1_000L, now = 1_000L))
        assertEquals(250L, ChatTiming.remainingThinkingDelay(startedAt = 1_000L, now = 1_450L))
        assertEquals(0L, ChatTiming.remainingThinkingDelay(startedAt = 1_000L, now = 1_800L))
    }
}
