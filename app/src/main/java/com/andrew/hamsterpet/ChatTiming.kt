package com.andrew.hamsterpet

object ChatTiming {
    private const val MIN_THINKING_DURATION_MS = 700L

    fun remainingThinkingDelay(startedAt: Long, now: Long): Long =
        (MIN_THINKING_DURATION_MS - (now - startedAt)).coerceAtLeast(0L)
}
