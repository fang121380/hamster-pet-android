package com.andrew.hamsterpet

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.SystemClock
import android.view.View

enum class BabyAnimation(
    val rowGroup: Int,
    val frameDurationMs: Long,
    val repeatCount: Int,
    val frameCount: Int = 8,
) {
    IDLE(0, 180L, -1),
    FEED(1, 105L, 2),
    PLAY(2, 90L, 2),
    ;

    fun frameAt(elapsedMs: Long): AnimationFrame {
        val safeElapsed = elapsedMs.coerceAtLeast(0L)
        if (repeatCount < 0) {
            return AnimationFrame(((safeElapsed / frameDurationMs) % frameCount).toInt(), complete = false)
        }
        val totalDuration = frameCount * frameDurationMs * repeatCount
        if (safeElapsed >= totalDuration) return AnimationFrame(frameCount - 1, complete = true)
        return AnimationFrame(((safeElapsed / frameDurationMs) % frameCount).toInt(), complete = false)
    }
}

@SuppressLint("ViewConstructor")
class BabySpriteView(
    context: Context,
    private val variant: BabyVariant,
    private val stage: BabyStage,
) : View(context) {
    private val atlas = SpriteAtlasCache.get(resources, R.drawable.baby_sprite_atlas)
    private val paint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
        isDither = false
    }
    private val source = Rect()
    private val destination = Rect()
    private var animation = BabyAnimation.IDLE
    private var frameIndex = 0
    private var startedAt = SystemClock.uptimeMillis()
    private var completion: (() -> Unit)? = null
    private var running = true

    private val ticker = object : Runnable {
        override fun run() {
            if (!running || !isAttachedToWindow) return
            val frame = animation.frameAt(SystemClock.uptimeMillis() - startedAt)
            if (frame.index != frameIndex) {
                frameIndex = frame.index
                invalidate()
            }
            if (frame.complete) {
                running = false
                completion?.also { callback ->
                    completion = null
                    callback()
                }
            } else {
                postOnAnimation(this)
            }
        }
    }

    init {
        contentDescription = "${BabyAssets.nameFor(variant)}${stageLabel(stage)}幼崽"
    }

    fun play(next: BabyAnimation, restart: Boolean = true, onComplete: (() -> Unit)? = null) {
        if (!restart && next == animation && running) return
        removeCallbacks(ticker)
        animation = next
        frameIndex = 0
        startedAt = SystemClock.uptimeMillis()
        completion = onComplete
        running = true
        invalidate()
        if (isAttachedToWindow) postOnAnimation(ticker)
    }

    fun pause() {
        running = false
        removeCallbacks(ticker)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (running) postOnAnimation(ticker)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(ticker)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val row = animation.rowGroup * VARIANT_COUNT * STAGE_COUNT + stage.ordinal * VARIANT_COUNT + variant.ordinal
        source.set(
            frameIndex * CELL_SIZE,
            row * CELL_SIZE,
            (frameIndex + 1) * CELL_SIZE,
            (row + 1) * CELL_SIZE,
        )
        destination.set(0, 0, width, height)
        canvas.drawBitmap(atlas, source, destination, paint)
    }

    private fun stageLabel(value: BabyStage) = when (value) {
        BabyStage.NEWBORN -> "新生期"
        BabyStage.TODDLER -> "幼年期"
        BabyStage.YOUNG -> "成长期"
    }

    companion object {
        const val CELL_SIZE = 96
        private const val VARIANT_COUNT = 3
        private const val STAGE_COUNT = 3
    }
}
