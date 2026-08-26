package com.andrew.hamsterpet

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.button.MaterialButton

@SuppressLint("ViewConstructor")
class FamilyPageView(
    context: Context,
    state: PetState,
    now: Long,
    onBreedingAction: () -> Unit,
    private val onCareBabies: (BabyCareType) -> Unit,
) : ScrollView(context), FamilyBreedingUi {
    private val babyViews = mutableListOf<BabySpriteView>()
    private lateinit var breedingButton: MaterialButton
    private lateinit var breedingSupportingText: TextView
    private var actionRunning = false

    init {
        isFillViewport = true
        val action = PetUiModel.breedingAction(state, now)
        addView(UiComponents.page(context).apply {
            addView(LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(UiComponents.title(context, "仓鼠家庭"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(UiComponents.statusPill(context, "${state.babies.size} / ${PetGameEngine.MAX_BABIES} 幼崽", state.babies.isNotEmpty()))
            })
            addView(UiComponents.body(context, "准备好温暖的小窝，新的家庭成员就会到来。", muted = true).apply {
                setPadding(0, 7.dp, 0, 14.dp)
            })
            addView(FrameLayout(context).apply {
                background = roundedDrawable(PetColors.habitat, 8)
                addView(SpriteAnimationView(context).apply {
                    applyAppearance(state)
                    applyMotionMode(state.motionMode)
                    play(PetAnimation.IDLE)
                    contentDescription = "成年仓鼠"
                }, FrameLayout.LayoutParams(138.dp, 170.dp, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { marginEnd = 55.dp })
                state.babies.take(3).forEachIndexed { index, baby ->
                    val babyView = BabySpriteView(context, baby.variant, PetGameEngine.stageFor(baby))
                    babyViews += babyView
                    addView(babyView, FrameLayout.LayoutParams(55.dp, 72.dp, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                        leftMargin = 88.dp + index * 26.dp
                        bottomMargin = 12.dp + (index % 2) * 7.dp
                    })
                }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 190.dp))
            addView(UiComponents.sectionLabel(context, "繁殖条件"))
            addView(UiComponents.infoRow(context, "二级小窝", "为幼崽准备足够空间", if (state.nestLevel >= 2) "已完成" else "未完成"))
            addView(UiComponents.infoRow(context, "饱食度达到 70", "当前 ${state.satiety}", if (state.satiety >= 70) "已满足" else "还差 ${70 - state.satiety}"))
            addView(UiComponents.infoRow(context, "亲密度达到 60", "当前 ${state.affection}", if (state.affection >= 60) "已满足" else "还差 ${60 - state.affection}"))
            breedingButton = UiComponents.primaryButton(context, action.label).apply {
                isEnabled = action.enabled
                setOnClickListener { onBreedingAction() }
            }
            addView(breedingButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 52.dp).apply { topMargin = 12.dp })
            breedingSupportingText = UiComponents.body(context, action.supportingText, muted = true).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(0, 7.dp, 0, 0)
                textSize = 12f
            }
            addView(breedingSupportingText)
            addView(UiComponents.sectionLabel(context, "幼崽成长"))
            if (state.babies.isEmpty()) {
                addView(UiComponents.body(context, "幼崽出生后会显示在这里，并拥有独立的毛色、姿势和成长阶段。", muted = true))
            } else {
                state.babies.forEachIndexed { index, baby ->
                    addView(LinearLayout(context).apply {
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(10.dp, 10.dp, 10.dp, 10.dp)
                        background = roundedDrawable(PetColors.surface, 8, PetColors.border)
                        val babyView = BabySpriteView(context, baby.variant, PetGameEngine.stageFor(baby))
                        babyViews += babyView
                        addView(babyView, LinearLayout.LayoutParams(62.dp, 62.dp))
                        addView(LinearLayout(context).apply {
                            orientation = LinearLayout.VERTICAL
                            addView(TextView(context).apply {
                                text = context.getString(R.string.baby_title, index + 1, BabyAssets.nameFor(baby.variant))
                                textSize = 14f
                                setTextColor(PetColors.text)
                                setTypeface(typeface, android.graphics.Typeface.BOLD)
                            })
                            val stage = PetGameEngine.stageFor(baby)
                            val targetFeeds = if (stage == BabyStage.NEWBORN) PetGameEngine.BABY_TODDLER_FEEDS else PetGameEngine.BABY_YOUNG_FEEDS
                            val targetPlays = if (stage == BabyStage.NEWBORN) PetGameEngine.BABY_TODDLER_PLAYS else PetGameEngine.BABY_YOUNG_PLAYS
                            val progressText = if (stage == BabyStage.YOUNG) "成长目标已完成" else "下一阶段：喂食 ${baby.feedingCount}/$targetFeeds · 陪玩 ${baby.playCount}/$targetPlays"
                            addView(UiComponents.body(context, "${stageName(stage)} · $progressText", muted = true).apply {
                                textSize = 12f
                            })
                        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = 12.dp })
                    }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 8.dp })
                }
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    val feedButton = UiComponents.secondaryButton(context, "喂幼崽")
                    val playButton = UiComponents.secondaryButton(context, "陪幼崽玩")
                    val buttons = listOf(feedButton, playButton)
                    feedButton.setOnClickListener { runBabyAction(BabyCareType.FEED, buttons) }
                    playButton.setOnClickListener { runBabyAction(BabyCareType.PLAY, buttons) }
                    addView(feedButton, LinearLayout.LayoutParams(0, 50.dp, 1f))
                    addView(playButton, LinearLayout.LayoutParams(0, 50.dp, 1f).apply { leftMargin = 8.dp })
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 50.dp).apply { topMargin = 4.dp })
            }
        })
    }

    override fun updateBreedingAction(state: PetState, now: Long) {
        val action = PetUiModel.breedingAction(state, now)
        breedingButton.text = action.label
        breedingButton.isEnabled = action.enabled
        breedingSupportingText.text = action.supportingText
    }

    private fun runBabyAction(careType: BabyCareType, buttons: List<MaterialButton>) {
        if (actionRunning) return
        actionRunning = true
        buttons.forEach { it.isEnabled = false }
        val animation = if (careType == BabyCareType.FEED) BabyAnimation.FEED else BabyAnimation.PLAY
        babyViews.forEach { it.play(animation) }
        PetAudio.play(context, if (careType == BabyCareType.FEED) PetSound.EAT else PetSound.PLAY)
        val duration = animation.frameDurationMs * animation.frameCount * animation.repeatCount
        postDelayed({
            if (isAttachedToWindow) onCareBabies(careType)
        }, duration)
    }

    private fun stageName(stage: BabyStage): String = when (stage) {
        BabyStage.NEWBORN -> "新生期"
        BabyStage.TODDLER -> "幼年期"
        BabyStage.YOUNG -> "成长期"
    }
}
