package com.andrew.hamsterpet

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View

object PetColors {
    val background = Color.rgb(247, 248, 246)
    val surface = Color.WHITE
    val text = Color.rgb(53, 41, 35)
    val textMuted = Color.rgb(101, 103, 99)
    val primary = Color.rgb(218, 91, 73)
    val primaryPressed = Color.rgb(187, 69, 54)
    val status = Color.rgb(47, 142, 113)
    val statusSurface = Color.rgb(221, 243, 234)
    val habitat = Color.rgb(223, 242, 240)
    val border = Color.rgb(220, 224, 219)
    val disabled = Color.rgb(224, 227, 223)
    val danger = Color.rgb(180, 44, 44)
    val dangerSurface = Color.rgb(255, 236, 233)
}

val Int.dp: Int
    get() = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()

fun roundedDrawable(
    color: Int,
    radiusDp: Int = 8,
    strokeColor: Int? = null,
    strokeWidthDp: Int = 1,
): GradientDrawable = GradientDrawable().apply {
    setColor(color)
    cornerRadius = radiusDp.dp.toFloat()
    strokeColor?.let { setStroke(strokeWidthDp.dp, it) }
}

fun Context.selectableBackground(): android.graphics.drawable.Drawable? {
    val out = TypedValue()
    theme.resolveAttribute(android.R.attr.selectableItemBackground, out, true)
    return androidx.appcompat.content.res.AppCompatResources.getDrawable(this, out.resourceId)
}

fun View.setVisible(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

fun colorState(enabled: Int, disabled: Int = PetColors.disabled): ColorStateList = ColorStateList(
    arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
    intArrayOf(disabled, enabled),
)
