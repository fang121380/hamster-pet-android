package com.andrew.hamsterpet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PetGameEngineTest {
    private val now = 10_000_000L

    @Test
    fun seed_increases_satiety_and_affection() {
        val state = PetState(satiety = 50, affection = 10, lastSatietyUpdateAt = now)

        val result = PetGameEngine.feed(state, FoodType.SEED, now)

        assertTrue(result.accepted)
        assertEquals(62, result.state.satiety)
        assertEquals(11, result.state.affection)
        assertEquals(1, result.state.feedingCount)
    }

    @Test
    fun all_foods_apply_their_declared_effects() {
        FoodType.entries.forEach { food ->
            val state = PetState(satiety = 40, affection = 20, lastSatietyUpdateAt = now)
            val result = PetGameEngine.feed(state, food, now)
            assertEquals(40 + food.satietyGain, result.state.satiety)
            assertEquals(20 + food.affectionGain, result.state.affection)
        }
    }

    @Test
    fun feeding_remains_available_until_the_high_satiety_limit() {
        val state = PetState(satiety = 96, affection = 20, lastSatietyUpdateAt = now, lastAffectionUpdateAt = now)

        assertTrue(PetGameEngine.feed(state, FoodType.BISCUIT, now).accepted)

        val full = state.copy(satiety = 98)
        val result = PetGameEngine.feed(full, FoodType.BISCUIT, now)

        assertFalse(result.accepted)
        assertEquals(CareBlockReason.FULL, result.blockReason)
        assertEquals(full, result.state)
    }

    @Test
    fun food_catalog_offers_eight_distinct_choices() {
        assertEquals(8, FoodType.entries.size)
        assertEquals(8, FoodType.entries.map { it.name }.toSet().size)
    }

    @Test
    fun satiety_decays_once_per_twenty_minutes_and_stops_at_twenty() {
        val twentyMinutes = 20L * 60L * 1000L
        val state = PetState(satiety = 22, lastSatietyUpdateAt = now)

        val decayed = PetGameEngine.applyDecay(state, now + twentyMinutes * 5)

        assertEquals(20, decayed.satiety)
        assertEquals(now + twentyMinutes * 5, decayed.lastSatietyUpdateAt)
    }

    @Test
    fun affection_decays_every_thirty_minutes_and_stops_at_fifteen() {
        val thirtyMinutes = 30L * 60L * 1000L
        val state = PetState(affection = 22, lastSatietyUpdateAt = now, lastAffectionUpdateAt = now)

        val decayed = PetGameEngine.applyDecay(state, now + thirtyMinutes * 5)

        assertEquals(15, decayed.affection)
        assertEquals(now + thirtyMinutes * 5, decayed.lastAffectionUpdateAt)
    }

    @Test
    fun low_care_state_requires_attention() {
        assertTrue(PetGameEngine.needsAttention(PetState(satiety = 35, affection = 70)))
        assertTrue(PetGameEngine.needsAttention(PetState(satiety = 70, affection = 25)))
        assertFalse(PetGameEngine.needsAttention(PetState(satiety = 50, affection = 50)))
    }

    @Test
    fun feeding_and_interaction_progress_are_recorded_separately() {
        val state = PetState(satiety = 30, lastSatietyUpdateAt = now)
        val first = PetGameEngine.feed(state, FoodType.APPLE, now)
        val second = PetGameEngine.interact(first.state, now + 20_000)
        val third = PetGameEngine.interact(second.state, now + 31_000)

        assertEquals(1, third.state.feedingCount)
        assertEquals(2, third.state.interactionCount)
    }

    @Test
    fun nest_uses_explicit_feeding_and_interaction_requirements() {
        val unready = PetState(feedingCount = 3, interactionCount = 1)
        val firstReady = PetState(feedingCount = 3, interactionCount = 2)
        val secondReady = PetState(feedingCount = 8, interactionCount = 6, nestLevel = 1)

        assertFalse(PetGameEngine.canBuildNest(unready))
        assertTrue(PetGameEngine.canBuildNest(firstReady))
        assertEquals(1, PetGameEngine.buildNest(firstReady).nestLevel)
        assertTrue(PetGameEngine.canBuildNest(secondReady))
        assertEquals(2, PetGameEngine.buildNest(secondReady).nestLevel)
    }

    @Test
    fun breeding_reports_the_first_unmet_requirement() {
        assertEquals(BreedingBlockReason.NEST, PetGameEngine.breedingBlockReason(PetState()))
        assertEquals(
            BreedingBlockReason.SATIETY,
            PetGameEngine.breedingBlockReason(PetState(nestLevel = 2, satiety = 69)),
        )
        assertEquals(
            BreedingBlockReason.AFFECTION,
            PetGameEngine.breedingBlockReason(PetState(nestLevel = 2, satiety = 70, affection = 59)),
        )
        assertNull(PetGameEngine.breedingBlockReason(readyToBreed()))
    }

    @Test
    fun breeding_completes_after_five_minutes_and_chooses_an_unused_variant() {
        val started = PetGameEngine.startBreeding(
            readyToBreed().copy(
                babies = listOf(BabyState(1, BabyVariant.CREAM, now, 0)),
            ),
            now,
        )

        assertFalse(PetGameEngine.isBirthReady(started, now + PetGameEngine.BREEDING_DURATION_MS - 1))
        val completed = PetGameEngine.completeBreeding(started, now + PetGameEngine.BREEDING_DURATION_MS)

        assertEquals(2, completed.babies.size)
        assertEquals(BabyVariant.GRAY_WHITE, completed.babies.last().variant)
        assertEquals(0, completed.breedingStartedAt)
    }

    @Test
    fun breeding_never_exceeds_three_babies() {
        val babies = BabyVariant.entries.mapIndexed { index, variant ->
            BabyState(index.toLong(), variant, now, 0)
        }
        val state = readyToBreed().copy(babies = babies)

        assertEquals(BreedingBlockReason.FAMILY_FULL, PetGameEngine.breedingBlockReason(state))
        assertEquals(state, PetGameEngine.startBreeding(state, now))
    }

    @Test
    fun baby_stage_requires_explicit_feeding_and_play_progress() {
        assertEquals(BabyStage.NEWBORN, PetGameEngine.stageFor(BabyState(1, BabyVariant.CREAM, now, feedingCount = 3, playCount = 1)))
        assertEquals(BabyStage.TODDLER, PetGameEngine.stageFor(BabyState(1, BabyVariant.CREAM, now, feedingCount = 3, playCount = 2)))
        assertEquals(BabyStage.YOUNG, PetGameEngine.stageFor(BabyState(1, BabyVariant.CREAM, now, feedingCount = 8, playCount = 5)))
    }

    @Test
    fun baby_feeding_and_play_are_separate_actions() {
        val state = PetState(babies = listOf(BabyState(1, BabyVariant.CREAM, now)))

        val fed = PetGameEngine.careForBabies(state, BabyCareType.FEED)
        val played = PetGameEngine.careForBabies(fed, BabyCareType.PLAY)

        assertEquals(1, played.babies.single().feedingCount)
        assertEquals(1, played.babies.single().playCount)
    }

    @Test
    fun sanitize_clamps_values_and_discards_excess_babies() {
        val babies = (0..4).map { BabyState(it.toLong(), BabyVariant.CREAM, now, -2) }
        val dirty = PetState(satiety = 150, affection = -4, nestLevel = 9, babies = babies)

        val clean = PetGameEngine.sanitize(dirty, now)

        assertEquals(100, clean.satiety)
        assertEquals(0, clean.affection)
        assertEquals(2, clean.nestLevel)
        assertEquals(3, clean.babies.size)
        assertTrue(clean.babies.all { it.feedingCount == 0 && it.playCount == 0 })
    }

    private fun readyToBreed() = PetState(
        satiety = 75,
        affection = 65,
        nestLevel = 2,
        lastSatietyUpdateAt = now,
    )
}
