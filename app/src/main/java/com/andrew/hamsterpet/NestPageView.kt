package com.andrew.hamsterpet

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import com.google.android.material.progressindicator.LinearProgressIndicator

@SuppressLint("ViewConstructor")
class NestPageView(
    context: Context,
    state: PetState,
    onBuild: () -> Unit,
) : ScrollView(context) {
    init {
        isFillViewport = true
        val action = PetUiModel.nestAction(state)
        val requiredFeeds = if (state.nestLevel == 0) PetGameEngine.NEST_LEVEL_ONE_FEEDS else PetGameEngine.NEST_LEVEL_TWO_FEEDS
        val requiredInteractions = if (state.nestLevel == 0) PetGameEngine.NEST_LEVEL_ONE_INTERACTIONS else PetGameEngine.NEST_LEVEL_TWO_INTERACTIONS
        val completed = state.feedingCount.coerceAtMost(requiredFeeds) + state.interactionCount.coerceAtMost(requiredInteractions)
        val progress = if (state.nestLevel >= 2) 100 else completed * 100 / (requiredFeeds + requiredInteractions)
        addView(UiComponents.page(context).apply {
            addView(LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(UiComponents.title(context, "我的小窝"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(UiComponents.statusPill(context, "等级 ${state.nestLevel}", state.nestLevel > 0))
            })
            addView(UiComponents.body(context, "完成明确的喂食与互动目标，就能建造和升级小窝。", muted = true).apply {
                setPadding(0, 7.dp, 0, 14.dp)
            })
            addView(FrameLayout(context).apply {
                background = roundedDrawable(PetColors.habitat, 8)
                addView(OverlayNestView(context).apply {
                    level = state.nestLevel.coerceAtLeast(1)
                    contentDescription = if (state.nestLevel == 0) "尚未建成的小窝" else "${state.nestLevel}级小窝"
                }, FrameLayout.LayoutParams(180.dp, 76.dp, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = 26.dp })
                addView(SpriteAnimationView(context).apply {
                    applyAppearance(state)
                    applyMotionMode(state.motionMode)
                    play(if (state.nestLevel > 0) PetAnimation.SLEEP else PetAnimation.IDLE)
                    contentDescription = "窝里的仓鼠"
                }, FrameLayout.LayoutParams(100.dp, 128.dp, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = 34.dp })
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 220.dp))
            addView(UiComponents.sectionLabel(context, "建设进度"))
            addView(UiComponents.body(context, action.supportingText, muted = true))
            addView(LinearProgressIndicator(context).apply {
                max = 100
                setProgressCompat(progress, false)
                setIndicatorColor(PetColors.status)
                trackColor = PetColors.disabled
                trackCornerRadius = 4.dp
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 9.dp).apply { topMargin = 9.dp })
            addView(UiComponents.infoRow(context, "喂食进度", "成功吃下食物才计数", "${state.feedingCount.coerceAtMost(requiredFeeds)} / $requiredFeeds"))
            addView(UiComponents.infoRow(context, "互动进度", "摸头、跳舞或陪玩都会计数", "${state.interactionCount.coerceAtMost(requiredInteractions)} / $requiredInteractions"))
            addView(UiComponents.infoRow(context, "一级小窝", "喂食 3 次 + 互动 2 次", if (state.nestLevel >= 1) "已建成" else "未完成"))
            addView(UiComponents.infoRow(context, "二级小窝", "累计喂食 8 次 + 互动 6 次", if (state.nestLevel >= 2) "已完成" else "未完成"))
            addView(UiComponents.primaryButton(context, action.label).apply {
                isEnabled = action.enabled
                setOnClickListener { onBuild() }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 52.dp).apply { topMargin = 14.dp })
        })
    }
}
