package com.andrew.hamsterpet

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

data class OverlayPoint(val x: Float, val y: Float)
data class OverlayWindowSize(val widthDp: Int, val heightDp: Int)

object OverlayInteractionPolicy {
    private const val PET_WIDTH_DP = 140f
    private const val PET_HEIGHT_DP = 140f

    fun clampScale(value: Float): Float = when {
        !value.isFinite() -> 1f
        else -> value.coerceIn(PetGameEngine.MIN_PET_SCALE, PetGameEngine.MAX_PET_SCALE)
    }

    fun windowSize(scale: Float, expanded: Boolean, foodTrayVisible: Boolean): OverlayWindowSize {
        val safeScale = clampScale(scale)
        val petWidth = (PET_WIDTH_DP * safeScale).roundToInt()
        val petHeight = (PET_HEIGHT_DP * safeScale).roundToInt()
        if (!expanded) {
            return OverlayWindowSize(
                widthDp = max(206, petWidth + 16),
                heightDp = max(238, petHeight + 30),
            )
        }
        val petTop = if (foodTrayVisible) 352 else 264
        return OverlayWindowSize(
            widthDp = max(300, petWidth + 16),
            heightDp = petTop + petHeight + 14,
        )
    }

    fun isFoodDropAccepted(
        tokenCenter: OverlayPoint,
        mouthCenter: OverlayPoint,
        petWidth: Float,
        density: Float = 1f,
    ): Boolean {
        val safeDensity = density.takeIf { it.isFinite() && it > 0f } ?: 1f
        val radius = max(34f * safeDensity, petWidth * .28f)
        return hypot(tokenCenter.x - mouthCenter.x, tokenCenter.y - mouthCenter.y) <= radius
    }
}
