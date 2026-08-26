package com.andrew.hamsterpet

import kotlin.math.max

object PetGameEngine {
    const val SATIETY_DECAY_INTERVAL_MS = 20L * 60L * 1000L
    const val AFFECTION_DECAY_INTERVAL_MS = 30L * 60L * 1000L
    const val CARE_POINT_COOLDOWN_MS = 30_000L
    const val BREEDING_DURATION_MS = 5L * 60L * 1000L
    const val MAX_BABIES = 3

    fun applyDecay(state: PetState, now: Long): PetState {
        val satietyUpdatedAt = if (state.lastSatietyUpdateAt <= 0L) now else state.lastSatietyUpdateAt
        val affectionUpdatedAt = if (state.lastAffectionUpdateAt <= 0L) now else state.lastAffectionUpdateAt
        val satietySteps = ((now - satietyUpdatedAt).coerceAtLeast(0L) / SATIETY_DECAY_INTERVAL_MS).toInt()
        val affectionSteps = ((now - affectionUpdatedAt).coerceAtLeast(0L) / AFFECTION_DECAY_INTERVAL_MS).toInt()
        return state.copy(
            satiety = if (satietySteps > 0) max(20, state.satiety - satietySteps) else state.satiety,
            affection = if (affectionSteps > 0) max(15, state.affection - affectionSteps * 2) else state.affection,
            lastSatietyUpdateAt = if (satietySteps > 0) now else satietyUpdatedAt,
            lastAffectionUpdateAt = if (affectionSteps > 0) now else affectionUpdatedAt,
        )
    }

    fun needsAttention(state: PetState): Boolean = state.satiety <= 35 || state.affection <= 25

    fun feed(state: PetState, food: FoodType, now: Long): CareResult {
        val current = applyDecay(state, now)
        if (current.satiety >= FEED_REFUSAL_SATIETY) {
            return CareResult(current, accepted = false, blockReason = CareBlockReason.FULL)
        }
        val fed = current.copy(
            satiety = (current.satiety + food.satietyGain).coerceAtMost(100),
            affection = (current.affection + food.affectionGain).coerceAtMost(100),
            feedingCount = current.feedingCount + 1,
        )
        return CareResult(awardCarePoint(fed, now), accepted = true)
    }

    fun interact(state: PetState, now: Long): CareResult {
        val current = applyDecay(state, now)
        val interacted = current.copy(
            affection = (current.affection + 2).coerceAtMost(100),
            interactionCount = current.interactionCount + 1,
        )
        return CareResult(awardCarePoint(interacted, now), accepted = true)
    }

    fun canBuildNest(state: PetState): Boolean = when (state.nestLevel) {
        0 -> state.feedingCount >= NEST_LEVEL_ONE_FEEDS && state.interactionCount >= NEST_LEVEL_ONE_INTERACTIONS
        1 -> state.feedingCount >= NEST_LEVEL_TWO_FEEDS && state.interactionCount >= NEST_LEVEL_TWO_INTERACTIONS
        else -> false
    }

    fun buildNest(state: PetState): PetState = if (canBuildNest(state)) {
        state.copy(nestLevel = (state.nestLevel + 1).coerceAtMost(2))
    } else {
        state
    }

    fun breedingBlockReason(state: PetState): BreedingBlockReason? = when {
        state.nestLevel < 2 -> BreedingBlockReason.NEST
        state.satiety < 70 -> BreedingBlockReason.SATIETY
        state.affection < 60 -> BreedingBlockReason.AFFECTION
        state.babies.size >= MAX_BABIES -> BreedingBlockReason.FAMILY_FULL
        state.breedingStartedAt > 0 -> BreedingBlockReason.ALREADY_BREEDING
        else -> null
    }

    fun startBreeding(state: PetState, now: Long): PetState =
        if (breedingBlockReason(state) == null) state.copy(breedingStartedAt = now) else state

    fun isBirthReady(state: PetState, now: Long): Boolean =
        state.breedingStartedAt > 0L && now - state.breedingStartedAt >= BREEDING_DURATION_MS

    fun completeBreeding(state: PetState, now: Long): PetState {
        if (!isBirthReady(state, now) || state.babies.size >= MAX_BABIES) return state
        val used = state.babies.mapTo(mutableSetOf()) { it.variant }
        val variant = BabyVariant.entries.firstOrNull { it !in used }
            ?: BabyVariant.entries[state.babies.size % BabyVariant.entries.size]
        val baby = BabyState(
            id = nextBabyId(state, now),
            variant = variant,
            bornAt = now,
        )
        return state.copy(
            breedingStartedAt = 0L,
            babies = state.babies + baby,
        )
    }

    fun careForBabies(state: PetState, careType: BabyCareType): PetState = state.copy(
        babies = state.babies.map { baby ->
            when (careType) {
                BabyCareType.FEED -> baby.copy(feedingCount = baby.feedingCount + 1)
                BabyCareType.PLAY -> baby.copy(playCount = baby.playCount + 1)
            }
        },
    )

    fun stageFor(baby: BabyState): BabyStage = when {
        baby.feedingCount >= BABY_YOUNG_FEEDS && baby.playCount >= BABY_YOUNG_PLAYS -> BabyStage.YOUNG
        baby.feedingCount >= BABY_TODDLER_FEEDS && baby.playCount >= BABY_TODDLER_PLAYS -> BabyStage.TODDLER
        else -> BabyStage.NEWBORN
    }

    fun sanitize(state: PetState, now: Long): PetState {
        val nestLevel = state.nestLevel.coerceIn(0, 2)
        val minimumNestFeeds = when (nestLevel) {
            1 -> NEST_LEVEL_ONE_FEEDS
            2 -> NEST_LEVEL_TWO_FEEDS
            else -> 0
        }
        val minimumNestInteractions = when (nestLevel) {
            1 -> NEST_LEVEL_ONE_INTERACTIONS
            2 -> NEST_LEVEL_TWO_INTERACTIONS
            else -> 0
        }
        val babies = state.babies
            .distinctBy { it.id }
            .take(MAX_BABIES)
            .map {
                it.copy(
                    feedingCount = it.feedingCount.coerceAtLeast(0),
                    playCount = it.playCount.coerceAtLeast(0),
                )
            }
        return state.copy(
            satiety = state.satiety.coerceIn(20, 100),
            affection = state.affection.coerceIn(0, 100),
            carePoints = state.carePoints.coerceAtLeast(0),
            feedingCount = max(state.feedingCount, minimumNestFeeds),
            interactionCount = max(state.interactionCount, minimumNestInteractions),
            nestLevel = nestLevel,
            petScale = if (state.petScale.isFinite()) state.petScale.coerceIn(MIN_PET_SCALE, MAX_PET_SCALE) else 1f,
            bgmTrackIndex = state.bgmTrackIndex.coerceAtLeast(0),
            bgmVolume = state.bgmVolume.coerceIn(0, 100),
            lastSatietyUpdateAt = if (state.lastSatietyUpdateAt <= 0L) now else state.lastSatietyUpdateAt,
            lastAffectionUpdateAt = if (state.lastAffectionUpdateAt <= 0L) now else state.lastAffectionUpdateAt,
            lastCarePointAt = state.lastCarePointAt.coerceAtLeast(0L),
            breedingStartedAt = state.breedingStartedAt.coerceAtLeast(0L),
            babies = babies,
        )
    }

    private fun awardCarePoint(state: PetState, now: Long): PetState {
        val canAward = state.lastCarePointAt == 0L || now - state.lastCarePointAt >= CARE_POINT_COOLDOWN_MS
        return if (canAward) {
            state.copy(carePoints = state.carePoints + 1, lastCarePointAt = now)
        } else {
            state
        }
    }

    private fun nextBabyId(state: PetState, now: Long): Long {
        val used = state.babies.mapTo(mutableSetOf()) { it.id }
        var candidate = now
        while (candidate in used) candidate++
        return candidate
    }

    const val NEST_LEVEL_ONE_FEEDS = 3
    const val NEST_LEVEL_ONE_INTERACTIONS = 2
    const val NEST_LEVEL_TWO_FEEDS = 8
    const val NEST_LEVEL_TWO_INTERACTIONS = 6
    const val BABY_TODDLER_FEEDS = 3
    const val BABY_TODDLER_PLAYS = 2
    const val BABY_YOUNG_FEEDS = 8
    const val BABY_YOUNG_PLAYS = 5
    const val MIN_PET_SCALE = .35f
    const val MAX_PET_SCALE = 2.8f
    private const val FEED_REFUSAL_SATIETY = 98
}
