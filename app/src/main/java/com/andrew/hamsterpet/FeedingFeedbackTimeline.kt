package com.andrew.hamsterpet

enum class FeedingFeedbackCue {
    START_EATING,
    HIDE_FOOD,
    CHEW_SOUND,
    CRUMBS,
    COMPLETE,
}

data class TimedFeedingCue(val offsetMs: Long, val cue: FeedingFeedbackCue)

object FeedingFeedbackTimeline {
    const val SNAP_DURATION_MS = 220L
    const val COMPLETE_AT_MS = 1_660L

    val cues: List<TimedFeedingCue> = listOf(
        TimedFeedingCue(SNAP_DURATION_MS, FeedingFeedbackCue.START_EATING),
        TimedFeedingCue(SNAP_DURATION_MS, FeedingFeedbackCue.HIDE_FOOD),
        TimedFeedingCue(SNAP_DURATION_MS, FeedingFeedbackCue.CHEW_SOUND),
        TimedFeedingCue(SNAP_DURATION_MS, FeedingFeedbackCue.CRUMBS),
        TimedFeedingCue(520L, FeedingFeedbackCue.CHEW_SOUND),
        TimedFeedingCue(520L, FeedingFeedbackCue.CRUMBS),
        TimedFeedingCue(820L, FeedingFeedbackCue.CHEW_SOUND),
        TimedFeedingCue(820L, FeedingFeedbackCue.CRUMBS),
        TimedFeedingCue(COMPLETE_AT_MS, FeedingFeedbackCue.COMPLETE),
    )
}
