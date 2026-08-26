package com.andrew.hamsterpet

data class AnimationFrame(val index: Int, val complete: Boolean)

enum class PetAnimation(
    val row: Int,
    val frameDurationMs: Long,
    val repeatCount: Int,
    val frameCount: Int = 8,
) {
    IDLE(0, 150, -1),
    WALK(1, 95, -1),
    DRAG(2, 105, -1),
    PAT(3, 95, 1),
    JUMP(4, 85, 1),
    DANCE(5, 100, 2),
    EAT(6, 90, 2),
    SLEEP_ENTER(7, 120, 1),
    SLEEP(8, 190, -1),
    WAKE(9, 105, 1),
    BUILD(10, 90, 2),
    HAPPY(11, 90, 2),
    ;

    fun frameAt(elapsedMs: Long): AnimationFrame {
        val safeElapsed = elapsedMs.coerceAtLeast(0L)
        if (repeatCount < 0) {
            return AnimationFrame(((safeElapsed / frameDurationMs) % frameCount).toInt(), complete = false)
        }
        val totalDuration = frameCount * frameDurationMs * repeatCount
        if (safeElapsed >= totalDuration) return AnimationFrame(frameCount - 1, complete = true)
        return AnimationFrame(((safeElapsed / frameDurationMs) % frameCount).toInt(), complete = false)
    }
}
