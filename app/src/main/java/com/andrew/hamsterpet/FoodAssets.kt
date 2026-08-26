package com.andrew.hamsterpet

object FoodAssets {
    fun nameFor(food: FoodType): String = when (food) {
        FoodType.SEED -> "瓜子"
        FoodType.CARROT -> "胡萝卜"
        FoodType.APPLE -> "苹果"
        FoodType.BISCUIT -> "小饼干"
        FoodType.CORN -> "玉米"
        FoodType.CUCUMBER -> "黄瓜"
        FoodType.STRAWBERRY -> "草莓"
        FoodType.CHEESE -> "奶酪"
    }
}
