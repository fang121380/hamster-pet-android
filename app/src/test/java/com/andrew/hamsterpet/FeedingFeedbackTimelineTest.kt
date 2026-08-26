package com.andrew.hamsterpet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedingFeedbackTimelineTest {
    @Test
    fun food_snaps_to_mouth_before_eating_feedback_begins() {
        assertEquals(220L, FeedingFeedbackTimeline.SNAP_DURATION_MS)
        assertEquals(
            FeedingFeedbackCue.START_EATING,
            FeedingFeedbackTimeline.cues.first { it.offsetMs >= FeedingFeedbackTimeline.SNAP_DURATION_MS }.cue,
        )
        assertTrue(FeedingFeedbackTimeline.cues.none {
            it.cue == FeedingFeedbackCue.START_EATING && it.offsetMs < FeedingFeedbackTimeline.SNAP_DURATION_MS
        })
    }

    @Test
    fun chewing_has_multiple_sound_and_crumb_pulses_after_the_bite() {
        val chewSounds = FeedingFeedbackTimeline.cues.filter { it.cue == FeedingFeedbackCue.CHEW_SOUND }
        val crumbs = FeedingFeedbackTimeline.cues.filter { it.cue == FeedingFeedbackCue.CRUMBS }

        assertTrue(chewSounds.size >= 3)
        assertTrue(crumbs.size >= 3)
        assertTrue(chewSounds.all { it.offsetMs >= FeedingFeedbackTimeline.SNAP_DURATION_MS })
        assertEquals(FeedingFeedbackTimeline.COMPLETE_AT_MS, FeedingFeedbackTimeline.cues.last().offsetMs)
    }
}
