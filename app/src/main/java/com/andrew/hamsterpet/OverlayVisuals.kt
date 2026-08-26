package com.andrew.hamsterpet

import android.annotation.SuppressLint
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View

@SuppressLint("ViewConstructor")
class OverlayFoodView(context: Context, private val food: FoodType) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val detail = Paint(Paint.ANTI_ALIAS_FLAG)
    private val carrot = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        carrot.reset()
        carrot.moveTo(w * .31f, h * .27f)
        carrot.lineTo(w * .74f, h * .25f)
        carrot.lineTo(w * .48f, h * .89f)
        carrot.close()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        when (food) {
            FoodType.SEED -> {
                paint.color = Color.rgb(91, 65, 43)
                detail.color = Color.rgb(244, 201, 82)
                canvas.drawOval(w * .27f, h * .10f, w * .73f, h * .88f, paint)
                canvas.drawOval(w * .40f, h * .16f, w * .61f, h * .82f, detail)
            }
            FoodType.CARROT -> {
                paint.color = Color.rgb(241, 110, 55)
                detail.color = Color.rgb(64, 139, 86)
                canvas.drawPath(carrot, paint)
                canvas.drawOval(w * .20f, h * .08f, w * .49f, h * .36f, detail)
                canvas.drawOval(w * .48f, h * .05f, w * .79f, h * .32f, detail)
            }
            FoodType.APPLE -> {
                paint.color = Color.rgb(218, 71, 65)
                detail.color = Color.rgb(77, 137, 75)
                canvas.drawCircle(w * .43f, h * .56f, minOf(w, h) * .25f, paint)
                canvas.drawCircle(w * .61f, h * .56f, minOf(w, h) * .25f, paint)
                canvas.drawOval(w * .50f, h * .10f, w * .72f, h * .34f, detail)
            }
            FoodType.BISCUIT -> {
                paint.color = Color.rgb(211, 153, 76)
                detail.color = Color.rgb(112, 74, 43)
                canvas.drawRoundRect(w * .20f, h * .18f, w * .80f, h * .82f, 8f, 8f, paint)
                listOf(.36f to .37f, .64f to .38f, .48f to .62f).forEach { (x, y) ->
                    canvas.drawCircle(w * x, h * y, 2.5f, detail)
                }
            }
            FoodType.CORN -> {
                paint.color = Color.rgb(249, 198, 62)
                detail.color = Color.rgb(224, 157, 40)
                canvas.drawRoundRect(w * .30f, h * .13f, w * .70f, h * .88f, w * .16f, w * .16f, paint)
                for (row in 0..3) for (column in 0..1) {
                    canvas.drawCircle(w * (.40f + column * .20f), h * (.28f + row * .15f), w * .055f, detail)
                }
            }
            FoodType.CUCUMBER -> {
                paint.color = Color.rgb(57, 132, 67)
                detail.color = Color.rgb(205, 236, 173)
                canvas.drawRoundRect(w * .19f, h * .31f, w * .81f, h * .72f, h * .20f, h * .20f, paint)
                listOf(.34f, .50f, .66f).forEach { x -> canvas.drawCircle(w * x, h * .51f, w * .045f, detail) }
            }
            FoodType.STRAWBERRY -> {
                paint.color = Color.rgb(224, 65, 87)
                detail.color = Color.rgb(248, 224, 134)
                canvas.drawOval(w * .27f, h * .20f, w * .73f, h * .90f, paint)
                listOf(.40f to .45f, .59f to .48f, .49f to .68f).forEach { (x, y) ->
                    canvas.drawCircle(w * x, h * y, w * .035f, detail)
                }
                detail.color = Color.rgb(61, 139, 80)
                canvas.drawOval(w * .30f, h * .08f, w * .70f, h * .34f, detail)
            }
            FoodType.CHEESE -> {
                paint.color = Color.rgb(255, 200, 47)
                detail.color = Color.rgb(220, 146, 32)
                val cheese = Path().apply {
                    moveTo(w * .22f, h * .75f)
                    lineTo(w * .77f, h * .82f)
                    lineTo(w * .66f, h * .20f)
                    close()
                }
                canvas.drawPath(cheese, paint)
                canvas.drawCircle(w * .49f, h * .55f, w * .07f, detail)
                canvas.drawCircle(w * .59f, h * .72f, w * .045f, detail)
            }
        }
    }
}

class FeedingEffectView(context: Context) : View(context) {
    private data class Burst(val x: Float, val y: Float, var progress: Float = 0f)

    private val paint = Paint().apply { isAntiAlias = false }
    private val bursts = mutableListOf<Burst>()
    private val animators = mutableListOf<ValueAnimator>()
    private val vectors = listOf(
        -22f to -18f,
        -12f to -28f,
        4f to -31f,
        17f to -22f,
        25f to -8f,
        -27f to -5f,
    )
    private val colors = intArrayOf(
        Color.rgb(244, 190, 70),
        Color.rgb(210, 126, 55),
        Color.rgb(255, 225, 128),
    )

    fun burst(x: Float, y: Float) {
        val burst = Burst(x, y)
        bursts += burst
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 360L
            addUpdateListener {
                burst.progress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    bursts -= burst
                    animators -= animation as ValueAnimator
                    invalidate()
                }
            })
            animators += this
            start()
        }
    }

    fun dispose() {
        animators.toList().forEach { it.cancel() }
        animators.clear()
        bursts.clear()
    }

    override fun onDraw(canvas: Canvas) {
        val density = resources.displayMetrics.density
        val size = 4f * density
        bursts.forEach { burst ->
            val progress = burst.progress
            vectors.forEachIndexed { index, (dx, dy) ->
                paint.color = colors[index % colors.size]
                paint.alpha = ((1f - progress) * 255).toInt().coerceIn(0, 255)
                val left = burst.x + dx * density * progress
                val top = burst.y + (dy * progress + 18f * progress * progress) * density
                canvas.drawRect(left, top, left + size, top + size, paint)
            }
        }
    }
}

class OverlayNestView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var level: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        if (level <= 0) return
        val w = width.toFloat()
        val h = height.toFloat()
        paint.color = Color.rgb(153, 101, 59)
        canvas.drawOval(0f, h * .24f, w, h, paint)
        paint.color = Color.rgb(218, 166, 102)
        canvas.drawOval(w * .04f, h * .08f, w * .96f, h * .76f, paint)
        paint.color = Color.rgb(126, 77, 49)
        canvas.drawOval(w * .22f, h * .30f, w * .78f, h * 1.04f, paint)
        if (level >= 2) {
            paint.color = Color.rgb(111, 164, 125)
            canvas.drawRoundRect(w * .12f, 0f, w * .88f, h * .23f, 9f, 9f, paint)
            paint.color = Color.rgb(246, 209, 99)
            canvas.drawCircle(w * .50f, h * .11f, h * .08f, paint)
        }
    }
}
