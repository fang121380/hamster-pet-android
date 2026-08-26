package com.andrew.hamsterpet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetStateCodecTest {
    @Test
    fun round_trip_preserves_family_and_preferences() {
        val state = PetState(
            satiety = 82,
            affection = 63,
            carePoints = 8,
            feedingCount = 9,
            interactionCount = 7,
            nestLevel = 2,
            overlayRunning = true,
            motionMode = MotionMode.GENTLE,
            skin = HamsterSkin.SILVER,
            furTint = FurTint.SKY,
            petScale = 2.35f,
            musicEnabled = false,
            bgmTrackIndex = 4,
            bgmVolume = 63,
            lastSatietyUpdateAt = 44,
            lastAffectionUpdateAt = 45,
            lastCarePointAt = 33,
            breedingStartedAt = 22,
            babies = listOf(BabyState(7, BabyVariant.GRAY_WHITE, 99, feedingCount = 5, playCount = 3)),
        )

        val decoded = PetStateCodec.decode(PetStateCodec.encode(state), 100)

        assertEquals(state, decoded)
    }

    @Test
    fun old_baby_care_count_migrates_without_losing_progress() {
        val decoded = PetStateCodec.decode(
            """{"version":2,"babies":[{"id":3,"variant":"CREAM","bornAt":33,"careCount":6}]}""",
            100,
        )

        assertEquals(6, decoded.babies.single().feedingCount)
        assertEquals(3, decoded.babies.single().playCount)
    }

    @Test
    fun completed_legacy_nest_backfills_the_new_explicit_care_counters() {
        val decoded = PetStateCodec.decode(
            """{"version":2,"carePoints":8,"nestLevel":2}""",
            100,
        )

        assertEquals(PetGameEngine.NEST_LEVEL_TWO_FEEDS, decoded.feedingCount)
        assertEquals(PetGameEngine.NEST_LEVEL_TWO_INTERACTIONS, decoded.interactionCount)
    }

    @Test
    fun invalid_json_returns_a_sanitized_default() {
        val decoded = PetStateCodec.decode("not json", 500)

        assertEquals(70, decoded.satiety)
        assertEquals(500, decoded.lastSatietyUpdateAt)
        assertTrue(decoded.babies.isEmpty())
    }
}
