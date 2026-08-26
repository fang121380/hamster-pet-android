package com.andrew.hamsterpet

object BabyAssets {
    fun nameFor(variant: BabyVariant): String = when (variant) {
        BabyVariant.CREAM -> "奶油色"
        BabyVariant.GRAY_WHITE -> "灰白色"
        BabyVariant.GOLDEN_BROWN -> "金棕色"
    }
}
