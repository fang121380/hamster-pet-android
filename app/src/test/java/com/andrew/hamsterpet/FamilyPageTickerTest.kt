package com.andrew.hamsterpet

import org.junit.Assert.assertEquals
import org.junit.Test

class FamilyPageTickerTest {
    @Test
    fun active_breeding_updates_the_existing_family_page_on_every_tick() {
        val state = PetState(breedingStartedAt = 1_000L)
        val updates = mutableListOf<Pair<PetState, Long>>()
        var pageLookupCount = 0
        val page = object : FamilyBreedingUi {
            override fun updateBreedingAction(state: PetState, now: Long) {
                updates += state to now
            }
        }
        val ticker = FamilyPageTicker(
            stateProvider = { state },
            familyPageProvider = {
                pageLookupCount++
                page
            },
        )

        ticker.tick(isFamilyPage = true, now = 2_000L)
        ticker.tick(isFamilyPage = true, now = 3_000L)

        assertEquals(listOf(state to 2_000L, state to 3_000L), updates)
        assertEquals(2, pageLookupCount)
    }

    @Test
    fun tick_ignores_other_pages_and_idle_family_state() {
        var state = PetState(breedingStartedAt = 1_000L)
        var updateCount = 0
        val page = object : FamilyBreedingUi {
            override fun updateBreedingAction(state: PetState, now: Long) {
                updateCount++
            }
        }
        val ticker = FamilyPageTicker(stateProvider = { state }, familyPageProvider = { page })

        ticker.tick(isFamilyPage = false, now = 2_000L)
        state = PetState(breedingStartedAt = 0L)
        ticker.tick(isFamilyPage = true, now = 3_000L)

        assertEquals(0, updateCount)
    }
}
