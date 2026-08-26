package com.andrew.hamsterpet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetAudioLoadQueueTest {
    @Test
    fun request_before_load_is_replayed_once_when_sample_finishes_loading() {
        val queue = SoundLoadQueue()
        queue.register(PetSound.EAT, sampleId = 17)

        assertTrue(queue.request(PetSound.EAT, volume = .6f).isEmpty())
        assertEquals(listOf(SoundPlayback(17, .6f)), queue.loaded(sampleId = 17, succeeded = true))
        assertTrue(queue.loaded(sampleId = 17, succeeded = true).isEmpty())
    }

    @Test
    fun loaded_sample_plays_immediately_and_failed_load_drops_pending_request() {
        val queue = SoundLoadQueue()
        queue.register(PetSound.EAT, sampleId = 21)
        queue.register(PetSound.PAT, sampleId = 22)

        assertTrue(queue.loaded(sampleId = 21, succeeded = true).isEmpty())
        assertEquals(listOf(SoundPlayback(21, .72f)), queue.request(PetSound.EAT, .72f))

        queue.request(PetSound.PAT, .5f)
        assertTrue(queue.loaded(sampleId = 22, succeeded = false).isEmpty())
        assertTrue(queue.loaded(sampleId = 22, succeeded = true).isEmpty())
    }
}
