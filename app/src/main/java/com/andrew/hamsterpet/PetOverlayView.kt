package com.andrew.hamsterpet

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.view.GestureDetector
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class PetOverlayView(
    context: android.content.Context,
    private val repository: PetStateRepository,
    private val host: Host,
) : FrameLayout(context) {
    interface Host {
        fun moveBy(dx: Int, dy: Int)
        fun walk(direction: Int, onEnd: () -> Unit)
        fun resize(width: Int, height: Int)
        fun openChat()
        fun toggleMusic()
        fun nextTrack()
        fun closePet()
    }

    private val nest = OverlayNestView(context).apply { visibility = GONE }
    private val babyLayer = FrameLayout(context)
    private val hamster = SpriteAnimationView(context)
    private val feedingEffect = FeedingEffectView(context)
    private val bubble = TextView(context).apply {
        visibility = GONE
        setTextColor(PetColors.text)
        textSize = 13f
        gravity = Gravity.CENTER
        setPadding(10.dp, 7.dp, 10.dp, 7.dp)
        background = roundedDrawable(Color.argb(250, 255, 255, 255), 8, PetColors.border)
        elevation = 10f
    }
    private val menu = GridLayout(context).apply {
        visibility = GONE
        columnCount = 3
        setPadding(5.dp, 5.dp, 5.dp, 5.dp)
        background = roundedDrawable(Color.argb(253, 255, 251, 246), 8, PetColors.border)
        elevation = 12f
    }
    private val foodTray = GridLayout(context).apply {
        visibility = GONE
        columnCount = 4
        setPadding(6.dp, 5.dp, 6.dp, 5.dp)
        background = roundedDrawable(Color.argb(253, 255, 255, 255), 8, PetColors.border)
        elevation = 13f
    }
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, GestureListener())
    private var expanded = false
    private var sleeping = false
    private var disposed = false
    private var scaling = false
    private var draggingPet = false
    private var facingRight = true
    private var petScale = OverlayInteractionPolicy.clampScale(repository.current().petScale)
    private var downRawX = 0f
    private var downRawY = 0f
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var selectedFood: FoodType? = null
    private var dragFood: OverlayFoodView? = null
    private var dragFoodSource: View? = null
    private var foodDownRawX = 0f
    private var foodDownRawY = 0f
    private var draggingFood = false
    private var feeding = false
    private val feedingCallbacks = mutableListOf<Runnable>()
    private var repositoryListener: (() -> Unit)? = null

    init {
        clipChildren = false
        clipToPadding = false
        addView(nest)
        addView(babyLayer)
        addView(hamster)
        addView(feedingEffect)
        addView(bubble)
        addView(menu)
        addView(foodTray)
        buildMenu()
        buildFoodTray()
        hamster.setOnTouchListener { _, event -> handlePetTouch(event) }
        repositoryListener = repository.addListener { state -> post { renderState(state) } }
        renderState(repository.refresh())
        applyLayout()
        hamster.play(PetAnimation.IDLE)
    }

    private fun buildMenu() {
        addMenu("摸摸头") { pat() }
        addMenu("拖拽喂食") { showFoodTray() }
        addMenu("散步") { walk() }
        addMenu("跳一跳") { jump() }
        addMenu("跳舞") { dance() }
        addMenu("回窝睡觉") { sleep() }
        addMenu("建造小窝") { buildNest() }
        addMenu("开心庆祝") { celebrate() }
        addMenu("陪我聊天") { closeMenuVisuals(); host.openChat() }
        addMenu("音乐开关") { host.toggleMusic(); showBubble("背景音乐设置已切换") }
        addMenu("下一首") { host.nextTrack(); showBubble("已经换了一首音乐") }
        addMenu("关闭桌宠", destructive = true) { closePet() }
    }

    private fun addMenu(label: String, destructive: Boolean = false, action: () -> Unit) {
        menu.addView(TextView(context).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 12.5f
            maxLines = 2
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(if (destructive) PetColors.danger else PetColors.text)
            background = roundedDrawable(if (destructive) PetColors.dangerSurface else PetColors.surface, 7, PetColors.border)
            isClickable = true
            isFocusable = true
            setOnClickListener { performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK); action() }
        }, GridLayout.LayoutParams().apply {
            width = 90.dp
            height = 48.dp
            setMargins(2.dp, 2.dp, 2.dp, 2.dp)
        })
    }

    private fun buildFoodTray() {
        FoodType.entries.forEach { food ->
            val foodView = OverlayFoodView(context, food).apply {
                contentDescription = "${food.displayName()}，拖到仓鼠嘴边"
                isClickable = true
                isFocusable = true
            }
            foodView.setOnTouchListener { source, event -> handleFoodTouch(food, source, event) }
            foodTray.addView(foodView, GridLayout.LayoutParams().apply {
                width = 64.dp
                height = 56.dp
                setMargins(1.dp, 1.dp, 1.dp, 1.dp)
            })
        }
    }

    private fun handlePetTouch(event: MotionEvent): Boolean {
        if (feeding) return true
        scaleDetector.onTouchEvent(event)
        if (!scaling) gestureDetector.onTouchEvent(event)
        if (event.pointerCount > 1 || scaling) {
            draggingPet = false
            return true
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                lastRawX = event.rawX
                lastRawY = event.rawY
                draggingPet = false
            }
            MotionEvent.ACTION_MOVE -> {
                val totalX = event.rawX - downRawX
                val totalY = event.rawY - downRawY
                if (abs(totalX) > 7.dp || abs(totalY) > 7.dp) {
                    draggingPet = true
                    val dx = (event.rawX - lastRawX).roundToInt()
                    val dy = (event.rawY - lastRawY).roundToInt()
                    if (dx != 0) facingRight = dx > 0
                    hamster.facingRight = facingRight
                    hamster.play(PetAnimation.DRAG, restart = false)
                    host.moveBy(dx, dy)
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (draggingPet) finishToIdle()
                draggingPet = false
            }
        }
        return true
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (draggingPet || scaling) return false
            if (sleeping) wakeUp() else pat()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (!draggingPet && !scaling) jump()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            if (!draggingPet && !scaling) toggleMenu()
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            scaling = true
            hamster.pause()
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            petScale = OverlayInteractionPolicy.clampScale(petScale * detector.scaleFactor)
            applyLayout()
            showBubble("大小 ${(petScale * 100).roundToInt()}%", 900)
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            repository.update { it.copy(petScale = petScale) }
            scaling = false
            if (sleeping) hamster.play(PetAnimation.SLEEP) else finishToIdle()
        }
    }

    private fun toggleMenu() {
        if (feeding) return
        if (expanded) closeMenuVisuals() else {
            expanded = true
            menu.visibility = VISIBLE
            foodTray.visibility = GONE
            applyLayout()
            showBubble("今天想一起做什么？")
        }
    }

    private fun closeMenuVisuals() {
        expanded = false
        menu.visibility = GONE
        foodTray.visibility = GONE
        selectedFood = null
        applyLayout()
    }

    private fun showFoodTray() {
        if (feeding) return
        if (sleeping) wakeUp()
        expanded = true
        menu.visibility = VISIBLE
        foodTray.visibility = VISIBLE
        applyLayout()
        showBubble("按住食物，拖到仓鼠嘴边")
    }

    private fun handleFoodTouch(food: FoodType, source: View, event: MotionEvent): Boolean {
        if (feeding) return true
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                foodDownRawX = event.rawX
                foodDownRawY = event.rawY
                draggingFood = false
                dragFoodSource = source
                source.alpha = .42f
                createDragFood(food, event.rawX, event.rawY)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (abs(event.rawX - foodDownRawX) > 6.dp || abs(event.rawY - foodDownRawY) > 6.dp) draggingFood = true
                moveDragFood(event.rawX, event.rawY)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                source.alpha = 1f
                if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
                    clearDragFood()
                } else if (draggingFood) {
                    moveDragFood(event.rawX, event.rawY)
                    if (isFoodAtMouth()) beginEating(food) else returnFoodToTray(source)
                } else if (selectedFood == food) {
                    beginEating(food)
                } else {
                    selectedFood = food
                    clearDragFood()
                    showBubble("已选${food.displayName()}，拖到嘴边；再点一次也可投喂")
                }
                draggingFood = false
                dragFoodSource = null
                return true
            }
        }
        return false
    }

    private fun createDragFood(food: FoodType, rawX: Float, rawY: Float) {
        clearDragFood()
        dragFood = OverlayFoodView(context, food).apply {
            elevation = 20f
            alpha = .98f
        }.also { token ->
            addView(token, LayoutParams(54.dp, 54.dp))
            moveDragFood(rawX, rawY)
        }
    }

    private fun moveDragFood(rawX: Float, rawY: Float) {
        val token = dragFood ?: return
        val location = IntArray(2)
        getLocationOnScreen(location)
        token.x = rawX - location[0] - token.width / 2f
        token.y = rawY - location[1] - token.height / 2f
    }

    private fun isFoodAtMouth(): Boolean {
        val token = dragFood ?: return false
        val petBounds = hamster.layoutParams as LayoutParams
        val petWidth = hamster.width.takeIf { it > 0 } ?: petBounds.width
        val petHeight = hamster.height.takeIf { it > 0 } ?: petBounds.height
        val mouthX = petBounds.leftMargin + petWidth * if (facingRight) .62f else .38f
        val mouthY = petBounds.topMargin + petHeight * .48f
        return OverlayInteractionPolicy.isFoodDropAccepted(
            OverlayPoint(token.x + token.width / 2f, token.y + token.height / 2f),
            OverlayPoint(mouthX, mouthY),
            petWidth.toFloat(),
            resources.displayMetrics.density,
        )
    }

    private fun beginEating(food: FoodType) {
        if (feeding) return
        val preview = PetGameEngine.feed(repository.current(), food, System.currentTimeMillis())
        if (!preview.accepted) {
            clearDragFood()
            showBubble("肚子已经饱饱的，稍后再喂吧")
            hamster.play(PetAnimation.PAT) { finishToIdle() }
            return
        }
        feeding = true
        val token = dragFood ?: OverlayFoodView(context, food).also {
            addView(it, LayoutParams(54.dp, 54.dp))
            val source = dragFoodSource
            it.x = (source?.x ?: width / 2f) + foodTray.x
            it.y = (source?.y ?: foodTray.y) + foodTray.y
        }
        dragFood = token
        selectedFood = null
        expanded = false
        menu.visibility = GONE
        foodTray.visibility = GONE
        applyLayout()
        val petBounds = hamster.layoutParams as LayoutParams
        val petWidth = hamster.width.takeIf { it > 0 } ?: petBounds.width
        val petHeight = hamster.height.takeIf { it > 0 } ?: petBounds.height
        val mouthX = petBounds.leftMargin + petWidth * if (facingRight) .62f else .38f
        val mouthY = petBounds.topMargin + petHeight * .48f
        token.animate().cancel()
        token.animate()
            .x(mouthX - token.width / 2f)
            .y(mouthY - token.height / 2f)
            .scaleX(.62f)
            .scaleY(.62f)
            .alpha(1f)
            .setDuration(FeedingFeedbackTimeline.SNAP_DURATION_MS)
            .withEndAction { startEatingFeedback(food) }
            .start()
    }

    private fun startEatingFeedback(food: FoodType) {
        if (!feeding || disposed) return
        clearDragFood()
        val petBounds = hamster.layoutParams as LayoutParams
        val mouthX = petBounds.leftMargin + petBounds.width * if (facingRight) .62f else .38f
        val mouthY = petBounds.topMargin + petBounds.height * .48f
        performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        FeedingFeedbackTimeline.cues.forEach { timedCue ->
            val action = Runnable {
                if (!feeding || disposed) return@Runnable
                when (timedCue.cue) {
                    FeedingFeedbackCue.START_EATING -> hamster.play(PetAnimation.EAT)
                    FeedingFeedbackCue.HIDE_FOOD -> clearDragFood()
                    FeedingFeedbackCue.CHEW_SOUND -> PetAudio.play(context, PetSound.EAT, .66f)
                    FeedingFeedbackCue.CRUMBS -> feedingEffect.burst(mouthX, mouthY)
                    FeedingFeedbackCue.COMPLETE -> completeEating(food)
                }
            }
            val delay = timedCue.offsetMs - FeedingFeedbackTimeline.SNAP_DURATION_MS
            feedingCallbacks += action
            if (delay == 0L) action.run() else postDelayed(action, delay)
        }
    }

    private fun completeEating(food: FoodType) {
        if (!feeding) return
        feeding = false
        feedingCallbacks.forEach(::removeCallbacks)
        feedingCallbacks.clear()
        val result = PetGameEngine.feed(repository.current(), food, System.currentTimeMillis())
        if (result.accepted) repository.update { result.state }
        showBubble(if (result.accepted) "${food.displayName()}吃完啦，饱食度 +${food.satietyGain}" else "已经吃饱啦")
        finishToIdle()
    }

    private fun returnFoodToTray(source: View) {
        val token = dragFood ?: return
        val rootLocation = IntArray(2)
        val sourceLocation = IntArray(2)
        getLocationOnScreen(rootLocation)
        source.getLocationOnScreen(sourceLocation)
        token.animate().cancel()
        token.animate()
            .x((sourceLocation[0] - rootLocation[0]).toFloat())
            .y((sourceLocation[1] - rootLocation[1]).toFloat())
            .alpha(.35f)
            .setDuration(180L)
            .withEndAction { clearDragFood() }
            .start()
        showBubble("再靠近嘴边一点")
    }

    private fun clearDragFood() {
        dragFood?.animate()?.cancel()
        dragFood?.let(::removeView)
        dragFood = null
        dragFoodSource?.alpha = 1f
    }

    private fun pat() = runInteraction(PetAnimation.PAT, listOf("头顶暖暖的", "再摸一下也可以", "今天也很喜欢你").random())

    private fun jump() = runInteraction(PetAnimation.JUMP, "接住我！")

    private fun dance() = runInteraction(PetAnimation.DANCE, "一起跳舞吧")

    private fun celebrate() = runInteraction(PetAnimation.HAPPY, "今天也要开心")

    private fun runInteraction(animation: PetAnimation, message: String) {
        if (sleeping) {
            wakeUp()
            return
        }
        closeMenuVisuals()
        val result = PetGameEngine.interact(repository.current(), System.currentTimeMillis())
        repository.update { result.state }
        val sound = if (animation == PetAnimation.PAT) PetSound.PAT else PetSound.PLAY
        PetAudio.play(context, sound)
        hamster.play(animation) { finishToIdle() }
        showBubble(message)
    }

    private fun walk() {
        if (sleeping) wakeUp()
        closeMenuVisuals()
        val direction = if (facingRight) 1 else -1
        hamster.play(PetAnimation.WALK)
        host.walk(direction) { finishToIdle() }
        showBubble("出发散步啦")
    }

    private fun sleep() {
        val state = repository.current()
        if (state.nestLevel < 1) {
            showBubble("先完成喂食 3 次和互动 2 次，再建好小窝")
            return
        }
        closeMenuVisuals()
        sleeping = true
        nest.visibility = VISIBLE
        PetAudio.play(context, PetSound.SLEEP, .55f)
        hamster.play(PetAnimation.SLEEP_ENTER) { if (sleeping) hamster.play(PetAnimation.SLEEP) }
        showBubble("晚安，轻点我就会醒来")
    }

    private fun wakeUp() {
        if (!sleeping) return
        sleeping = false
        hamster.play(PetAnimation.WAKE) { finishToIdle() }
        showBubble("睡得好舒服")
    }

    private fun buildNest() {
        val before = repository.current()
        val after = PetGameEngine.buildNest(before)
        if (after == before) {
            showBubble(PetUiModel.nestAction(before).supportingText)
            return
        }
        repository.update { after }
        closeMenuVisuals()
        nest.visibility = VISIBLE
        PetAudio.play(context, PetSound.BUILD)
        hamster.play(PetAnimation.BUILD) {
            showBubble(if (after.nestLevel == 1) "小窝建好啦" else "二级小窝升级完成")
            finishToIdle()
        }
    }

    private fun closePet() {
        clearDragFood()
        closeMenuVisuals()
        showBubble("我先回小窝休息啦")
        hamster.play(PetAnimation.SLEEP_ENTER) { if (!disposed) host.closePet() }
    }

    private fun finishToIdle() {
        if (!disposed && !sleeping) hamster.play(PetAnimation.IDLE)
    }

    private fun renderState(state: PetState) {
        petScale = OverlayInteractionPolicy.clampScale(state.petScale)
        hamster.applyAppearance(state)
        hamster.applyMotionMode(state.motionMode)
        if (!sleeping) nest.visibility = if (state.nestLevel > 0) VISIBLE else GONE
        nest.level = state.nestLevel
        babyLayer.removeAllViews()
        state.babies.take(3).forEachIndexed { index, baby ->
            babyLayer.addView(BabySpriteView(context, baby.variant, PetGameEngine.stageFor(baby)), LayoutParams(52.dp, 52.dp).apply {
                leftMargin = index * 43.dp
                topMargin = (index % 2) * 7.dp
            })
        }
        applyLayout()
    }

    private fun applyLayout() {
        val foodVisible = foodTray.visibility == VISIBLE
        val window = OverlayInteractionPolicy.windowSize(petScale, expanded, foodVisible)
        val windowWidth = window.widthDp.dp
        val windowHeight = window.heightDp.dp
        host.resize(windowWidth, windowHeight)

        val petWidth = (140f * petScale).roundToInt().dp
        val petHeight = (140f * petScale).roundToInt().dp
        val petTop = if (expanded) (if (foodVisible) 352.dp else 264.dp) else windowHeight - petHeight
        setBounds(hamster, petWidth, petHeight, (windowWidth - petWidth) / 2, petTop)
        setBounds(feedingEffect, windowWidth, windowHeight, 0, 0)
        hamster.facingRight = facingRight

        val nestWidth = (160f * petScale.coerceIn(.55f, 2f)).roundToInt().dp
        val nestHeight = (70f * petScale.coerceIn(.55f, 2f)).roundToInt().dp
        setBounds(nest, nestWidth, nestHeight, (windowWidth - nestWidth) / 2, petTop + (petHeight * .55f).roundToInt())
        setBounds(babyLayer, 142.dp, 62.dp, (windowWidth - 142.dp) / 2, petTop + petHeight - 58.dp)

        setBounds(menu, 284.dp, 214.dp, (windowWidth - 284.dp) / 2, 0)
        setBounds(foodTray, 276.dp, 128.dp, (windowWidth - 276.dp) / 2, 218.dp)
        val bubbleWidth = minOf(220.dp, windowWidth - 12.dp)
        val bubbleTop = if (expanded) petTop - 48.dp else 4.dp
        setBounds(bubble, bubbleWidth, 44.dp, (windowWidth - bubbleWidth) / 2, bubbleTop.coerceAtLeast(0))
    }

    private fun setBounds(view: View, width: Int, height: Int, left: Int, top: Int) {
        view.layoutParams = LayoutParams(width, height).apply {
            leftMargin = left
            topMargin = top
        }
    }

    private fun showBubble(message: String, duration: Long = 2_400L) {
        bubble.text = message
        bubble.visibility = VISIBLE
        bubble.removeCallbacks(hideBubble)
        bubble.postDelayed(hideBubble, duration)
    }

    private val hideBubble = Runnable { bubble.visibility = GONE }

    fun dispose() {
        disposed = true
        feeding = false
        feedingCallbacks.forEach(::removeCallbacks)
        feedingCallbacks.clear()
        feedingEffect.dispose()
        clearDragFood()
        hamster.pause()
        bubble.removeCallbacks(hideBubble)
        repositoryListener?.invoke()
        repositoryListener = null
    }
}

private fun FoodType.displayName() = FoodAssets.nameFor(this)
