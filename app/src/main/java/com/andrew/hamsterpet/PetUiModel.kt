package com.andrew.hamsterpet

data class ActionUi(
    val label: String,
    val supportingText: String,
    val enabled: Boolean,
)

object PetUiModel {
    fun nestAction(state: PetState): ActionUi = when (state.nestLevel) {
        0 -> nestProgressAction(state, PetGameEngine.NEST_LEVEL_ONE_FEEDS, PetGameEngine.NEST_LEVEL_ONE_INTERACTIONS, "建造小窝")
        1 -> nestProgressAction(state, PetGameEngine.NEST_LEVEL_TWO_FEEDS, PetGameEngine.NEST_LEVEL_TWO_INTERACTIONS, "升级小窝")
        else -> ActionUi("小窝已升级完成", "二级小窝已解锁繁殖", enabled = false)
    }

    fun breedingAction(state: PetState, now: Long): ActionUi {
        if (state.breedingStartedAt > 0L) {
            val remaining = PetGameEngine.BREEDING_DURATION_MS - (now - state.breedingStartedAt)
            if (remaining <= 0L) return ActionUi("迎接幼崽", "倒计时已完成", enabled = true)
            val totalSeconds = (remaining + 999L) / 1000L
            val minutes = totalSeconds / 60L
            val seconds = totalSeconds % 60L
            return ActionUi(
                label = "等待出生 %02d:%02d".format(minutes, seconds),
                supportingText = "关闭 App 后倒计时仍会继续",
                enabled = false,
            )
        }
        return when (PetGameEngine.breedingBlockReason(state)) {
            BreedingBlockReason.NEST -> ActionUi("尚未解锁繁殖", "需要二级小窝", false)
            BreedingBlockReason.SATIETY -> ActionUi("饱食度还不够", "饱食度需要达到 70", false)
            BreedingBlockReason.AFFECTION -> ActionUi("亲密度还不够", "亲密度需要达到 60", false)
            BreedingBlockReason.FAMILY_FULL -> ActionUi("家庭已经满员", "最多可以拥有 3 只幼崽", false)
            BreedingBlockReason.ALREADY_BREEDING -> ActionUi("繁殖进行中", "请等待幼崽出生", false)
            null -> ActionUi("开始繁殖", "幼崽将在 5 分钟后出生", true)
        }
    }

    private fun nestProgressAction(state: PetState, requiredFeeds: Int, requiredInteractions: Int, label: String): ActionUi {
        val missingFeeds = (requiredFeeds - state.feedingCount).coerceAtLeast(0)
        val missingInteractions = (requiredInteractions - state.interactionCount).coerceAtLeast(0)
        val missingParts = buildList {
            if (missingFeeds > 0) add("喂食 $missingFeeds 次")
            if (missingInteractions > 0) add("互动 $missingInteractions 次")
        }
        return ActionUi(
            label = label,
            supportingText = if (missingParts.isEmpty()) "条件已满足" else "还需${missingParts.joinToString("、")}",
            enabled = missingParts.isEmpty(),
        )
    }
}
