package com.andrew.hamsterpet

enum class MotionMode { GENTLE, STANDARD }

enum class HamsterSkin(val label: String, val color: Int?) {
    CLASSIC("经典", null),
    CREAM("奶油", 0xFFFFE2A8.toInt()),
    SILVER("银灰", 0xFFD4DCE4.toInt()),
    HONEY("蜜糖", 0xFFFFC46B.toInt()),
}

enum class FurTint(val label: String, val color: Int?) {
    NATURAL("原色", null),
    ROSE("樱粉", 0xFFFFB6C1.toInt()),
    MINT("薄荷", 0xFF9FE3D5.toInt()),
    SKY("晴蓝", 0xFFAED8FF.toInt()),
}

enum class FoodType(
    val satietyGain: Int,
    val affectionGain: Int,
) {
    SEED(12, 1),
    CARROT(10, 2),
    APPLE(8, 3),
    BISCUIT(15, 1),
    CORN(9, 2),
    CUCUMBER(7, 2),
    STRAWBERRY(6, 4),
    CHEESE(11, 3),
}

enum class CareBlockReason { FULL }

enum class BreedingBlockReason {
    NEST,
    SATIETY,
    AFFECTION,
    ALREADY_BREEDING,
    FAMILY_FULL,
}

enum class BabyVariant { CREAM, GRAY_WHITE, GOLDEN_BROWN }

enum class BabyStage { NEWBORN, TODDLER, YOUNG }

enum class BabyCareType { FEED, PLAY }

data class BabyState(
    val id: Long,
    val variant: BabyVariant,
    val bornAt: Long,
    val feedingCount: Int = 0,
    val playCount: Int = 0,
)

data class PetState(
    val satiety: Int = 70,
    val affection: Int = 35,
    val carePoints: Int = 0,
    val feedingCount: Int = 0,
    val interactionCount: Int = 0,
    val nestLevel: Int = 0,
    val overlayRunning: Boolean = false,
    val motionMode: MotionMode = MotionMode.STANDARD,
    val skin: HamsterSkin = HamsterSkin.CLASSIC,
    val furTint: FurTint = FurTint.NATURAL,
    val petScale: Float = 1f,
    val musicEnabled: Boolean = true,
    val bgmTrackIndex: Int = 0,
    val bgmVolume: Int = 42,
    val lastSatietyUpdateAt: Long = 0,
    val lastAffectionUpdateAt: Long = 0,
    val lastCarePointAt: Long = 0,
    val breedingStartedAt: Long = 0,
    val babies: List<BabyState> = emptyList(),
)

data class CareResult(
    val state: PetState,
    val accepted: Boolean,
    val blockReason: CareBlockReason? = null,
)
