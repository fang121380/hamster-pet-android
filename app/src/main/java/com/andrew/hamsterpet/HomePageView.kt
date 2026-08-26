package com.andrew.hamsterpet

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import com.google.android.material.materialswitch.MaterialSwitch

@SuppressLint("ViewConstructor")
class HomePageView(
    context: Context,
    state: PetState,
    onOverlayChanged: (Boolean) -> Unit,
    onFeed: (FoodType) -> Unit,
    onInteract: () -> Unit,
    onNest: () -> Unit,
    onChat: () -> Unit,
) : ScrollView(context) {
    private lateinit var habitat: FrameLayout
    private lateinit var hamster: SpriteAnimationView
    private lateinit var foodTray: GridLayout
    private var dragFood: OverlayFoodView? = null
    private var dragSource: View? = null
    private var actionRunning = false

    init {
        isFillViewport = true
        addView(UiComponents.page(context).apply {
            addView(LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(UiComponents.title(context, "仓鼠小梵"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(UiComponents.statusPill(context, if (state.overlayRunning) "桌宠运行中" else "桌宠已关闭", state.overlayRunning))
            })
            addView(UiComponents.body(context, "照顾它、陪它玩，再一起把小窝变得更温暖。", muted = true).apply {
                setPadding(0, 7.dp, 0, 14.dp)
            })
            habitat = FrameLayout(context).apply {
                background = roundedDrawable(PetColors.habitat, 8)
                contentDescription = "仓鼠和小窝场景"
                if (state.nestLevel > 0) {
                    addView(OverlayNestView(context).apply {
                        level = state.nestLevel
                        contentDescription = "${state.nestLevel}级小窝"
                    }, FrameLayout.LayoutParams(162.dp, 78.dp, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                        marginEnd = 105.dp
                        bottomMargin = 10.dp
                    })
                }
                hamster = SpriteAnimationView(context).apply {
                    applyAppearance(state)
                    applyMotionMode(state.motionMode)
                    play(PetAnimation.IDLE)
                    contentDescription = "成年仓鼠"
                }
                addView(hamster, FrameLayout.LayoutParams(190.dp, 215.dp, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                    bottomMargin = 8.dp
                })
                foodTray = GridLayout(context).apply {
                    columnCount = 4
                    visibility = View.GONE
                    setPadding(4.dp, 4.dp, 4.dp, 4.dp)
                    background = roundedDrawable(android.graphics.Color.argb(248, 255, 255, 255), 8, PetColors.border)
                    elevation = 12f
                    FoodType.entries.forEach { food ->
                        val foodView = OverlayFoodView(context, food).apply {
                            contentDescription = "${FoodAssets.nameFor(food)}，拖到小梵嘴边"
                            isClickable = true
                            isFocusable = true
                            setOnTouchListener { source, event -> handleHomeFoodTouch(food, source, event, onFeed) }
                        }
                        addView(foodView, GridLayout.LayoutParams().apply {
                            width = 44.dp
                            height = 44.dp
                            setMargins(1.dp, 1.dp, 1.dp, 1.dp)
                        })
                    }
                }
                addView(foodTray, FrameLayout.LayoutParams(192.dp, 100.dp, Gravity.TOP or Gravity.START).apply {
                    leftMargin = 8.dp
                    topMargin = 8.dp
                })
            }
            addView(habitat, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 238.dp))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                val metricParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(UiComponents.metric(context, "饱食度", state.satiety, PetColors.primary), metricParams)
                addView(UiComponents.metric(context, "亲密度", state.affection, PetColors.status), LinearLayout.LayoutParams(metricParams).apply { leftMargin = 8.dp })
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 12.dp
            })
            addView(LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 14.dp, 0, 10.dp)
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(UiComponents.body(context, "显示桌宠").apply { setTypeface(typeface, android.graphics.Typeface.BOLD) })
                    addView(UiComponents.body(context, "关闭后保留全部养成进度", muted = true).apply { textSize = 12f })
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(MaterialSwitch(context).apply {
                    isChecked = state.overlayRunning
                    contentDescription = "显示桌宠开关"
                    setOnCheckedChangeListener { _, checked ->
                        if (checked != state.overlayRunning) onOverlayChanged(checked)
                    }
                })
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                val actions = listOf("喂食" to { toggleFoodTray() }, "互动" to onInteract, "进入小窝" to onNest)
                actions.forEachIndexed { index, (label, action) ->
                    addView(UiComponents.primaryButton(context, label).apply {
                        maxLines = 1
                        includeFontPadding = false
                        gravity = Gravity.CENTER
                        minWidth = 0
                        insetTop = 0
                        insetBottom = 0
                        setPadding(4.dp, 0, 4.dp, 0)
                        setOnClickListener { action() }
                    }, LinearLayout.LayoutParams(0, 56.dp, 1f).apply { if (index > 0) leftMargin = 8.dp })
                }
            })
            addView(UiComponents.secondaryButton(context, "和仓鼠聊聊天").apply {
                setOnClickListener { onChat() }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 52.dp).apply { topMargin = 10.dp })
            addView(UiComponents.sectionLabel(context, "今天的家庭"))
            val nextNestRequirement = when (state.nestLevel) {
                0 -> "一级窝：喂食 3 次 + 互动 2 次"
                1 -> "二级窝：累计喂食 8 次 + 互动 6 次"
                else -> "二级小窝已完成，可以准备繁殖"
            }
            addView(UiComponents.infoRow(
                context,
                title = "小窝成长",
                subtitle = nextNestRequirement,
                trailing = if (state.nestLevel >= 2) "已完成" else "等级 ${state.nestLevel}",
            ))
            addView(UiComponents.infoRow(
                context,
                title = if (state.babies.isEmpty()) "还没有幼崽" else "${state.babies.size} 只幼崽正在成长",
                subtitle = if (state.nestLevel < 2) "升级到二级小窝后可繁殖" else "在家庭页面查看成长进度",
                trailing = "${state.babies.size} / ${PetGameEngine.MAX_BABIES}",
            ))
        })
    }

    private fun toggleFoodTray() {
        foodTray.visibility = if (foodTray.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun handleHomeFoodTouch(
        food: FoodType,
        source: View,
        event: MotionEvent,
        onFeed: (FoodType) -> Unit,
    ): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                source.parent?.requestDisallowInterceptTouchEvent(true)
                dragSource = source
                source.alpha = .35f
                dragFood?.let(habitat::removeView)
                dragFood = OverlayFoodView(context, food).also { token ->
                    token.elevation = 20f
                    habitat.addView(token, FrameLayout.LayoutParams(52.dp, 52.dp))
                    moveHomeFood(token, event.rawX, event.rawY)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                dragFood?.let { moveHomeFood(it, event.rawX, event.rawY) }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                source.parent?.requestDisallowInterceptTouchEvent(false)
                source.alpha = 1f
                val token = dragFood
                if (event.actionMasked == MotionEvent.ACTION_UP && token != null && isHomeFoodAtMouth(token)) {
                    habitat.removeView(token)
                    dragFood = null
                    dragSource = null
                    foodTray.visibility = View.GONE
                    onFeed(food)
                } else {
                    token?.animate()?.alpha(0f)?.scaleX(.4f)?.scaleY(.4f)?.setDuration(160L)?.withEndAction {
                        token.let(habitat::removeView)
                    }?.start()
                    dragFood = null
                    dragSource = null
                }
                return true
            }
        }
        return false
    }

    private fun moveHomeFood(token: View, rawX: Float, rawY: Float) {
        val location = IntArray(2)
        habitat.getLocationOnScreen(location)
        token.x = rawX - location[0] - token.width / 2f
        token.y = rawY - location[1] - token.height / 2f
    }

    private fun isHomeFoodAtMouth(token: View): Boolean = OverlayInteractionPolicy.isFoodDropAccepted(
        OverlayPoint(token.x + token.width / 2f, token.y + token.height / 2f),
        OverlayPoint(habitat.width * .55f, habitat.height * .47f),
        hamster.width.toFloat(),
        resources.displayMetrics.density,
    )

    fun playFeed(food: FoodType, onComplete: () -> Unit) {
        if (actionRunning) {
            onComplete()
            return
        }
        actionRunning = true
        val foodView = OverlayFoodView(context, food).apply {
            alpha = .98f
            scaleX = .85f
            scaleY = .85f
        }
        habitat.addView(foodView, FrameLayout.LayoutParams(52.dp, 52.dp, Gravity.BOTTOM or Gravity.START).apply {
            leftMargin = 34.dp
            bottomMargin = 34.dp
        })
        PetAudio.play(context, PetSound.EAT)
        foodView.post {
            foodView.animate()
                .x(habitat.width * .52f)
                .y(habitat.height * .42f)
                .scaleX(.18f)
                .scaleY(.18f)
                .alpha(.2f)
                .setDuration(620L)
                .withEndAction {
                    hamster.play(PetAnimation.EAT, onComplete = {
                        habitat.removeView(foodView)
                        hamster.play(PetAnimation.HAPPY, onComplete = {
                            hamster.play(PetAnimation.IDLE)
                            actionRunning = false
                            onComplete()
                        })
                    })
                }
                .start()
        }
    }

    fun playInteract(onComplete: () -> Unit) {
        if (actionRunning) {
            onComplete()
            return
        }
        actionRunning = true
        PetAudio.play(context, PetSound.PAT)
        hamster.play(PetAnimation.PAT, onComplete = {
            hamster.play(PetAnimation.HAPPY, onComplete = {
                hamster.play(PetAnimation.IDLE)
                actionRunning = false
                onComplete()
            })
        })
    }
}
