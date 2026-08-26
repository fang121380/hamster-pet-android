package com.andrew.hamsterpet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BgmTrackTest {
    @Test
    fun catalog_contains_five_unique_cc0_tracks() {
        assertEquals(5, BgmTrack.entries.size)
        assertEquals(5, BgmTrack.entries.map { it.resourceId }.distinct().size)
        assertTrue(BgmTrack.entries.all { it.license == "CC0" && it.sourceUrl.startsWith("https://") })
    }

    @Test
    fun track_selection_wraps_for_saved_and_next_indices() {
        assertEquals(BgmTrack.MY_STREET, BgmTrack.fromIndex(5))
        assertEquals(BgmTrack.DANCE_FIELD, BgmTrack.fromIndex(-4))
        assertEquals(0, BgmTrack.nextIndex(4))
    }

    @Test
    fun volume_is_clamped_and_converted_to_linear_gain() {
        assertEquals(0, BgmTrack.clampVolume(-2))
        assertEquals(100, BgmTrack.clampVolume(130))
        assertEquals(.42f, BgmTrack.volumeGain(42), .001f)
    }
}
