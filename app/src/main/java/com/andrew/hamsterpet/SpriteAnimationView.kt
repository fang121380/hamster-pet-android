package com.andrew.hamsterpet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View

class SpriteAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val atlas: Bitmap = SpriteAtlasCache.get(resources, R.drawable.hamster_sprite_atlas)
    private val paint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
        isDither = false
    }
    private val source = Rect()
    private val destination = Rect()
    private var animation = PetAnimation.IDLE
    private var frameIndex = 0
    private var startedAt = SystemClock.uptimeMillis()
    private var completion: (() -> Unit)? = null
    private var running = true
    private var playbackRate = 1f
    var facingRight: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    private val ticker = object : Runnable {
        override fun run() {
            if (!running || !isAttachedToWindow) return
            val elapsed = ((SystemClock.uptimeMillis() - startedAt) * playbackRate).toLong()
            val frame = animation.frameAt(elapsed)
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
        contentDescription = "动画仓鼠"
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    fun play(next: PetAnimation, restart: Boolean = true, onComplete: (() -> Unit)? = null) {
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

    fun currentAnimation(): PetAnimation = animation

    fun applyAppearance(state: PetState) {
        val color = state.furTint.color ?: state.skin.color
        paint.colorFilter = color?.let { PorterDuffColorFilter(it, PorterDuff.Mode.MULTIPLY) }
        invalidate()
    }

    fun applyMotionMode(mode: MotionMode) {
        playbackRate = if (mode == MotionMode.GENTLE) .72f else 1f
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
        super.onDraw(canvas)
        source.set(
            frameIndex * CELL_SIZE,
            animation.row * CELL_SIZE,
            (frameIndex + 1) * CELL_SIZE,
            (animation.row + 1) * CELL_SIZE,
        )
        destination.set(0, 0, width, height)
        if (!facingRight) {
            canvas.save()
            canvas.scale(-1f, 1f, width / 2f, height / 2f)
        }
        canvas.drawBitmap(atlas, source, destination, paint)
        if (!facingRight) canvas.restore()
    }

    companion object {
        const val CELL_SIZE = 128
    }
}
