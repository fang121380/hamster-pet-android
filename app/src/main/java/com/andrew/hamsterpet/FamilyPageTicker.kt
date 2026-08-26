package com.andrew.hamsterpet

internal interface FamilyBreedingUi {
    fun updateBreedingAction(state: PetState, now: Long)
}

internal class FamilyPageTicker(
    private val stateProvider: () -> PetState,
    private val familyPageProvider: () -> FamilyBreedingUi?,
) {
    fun tick(isFamilyPage: Boolean, now: Long) {
        if (!isFamilyPage) return
        val state = stateProvider()
        if (state.breedingStartedAt <= 0L) return
        familyPageProvider()?.updateBreedingAction(state, now)
    }
}
