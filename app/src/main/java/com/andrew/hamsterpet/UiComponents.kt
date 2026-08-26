package com.andrew.hamsterpet

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator

object UiComponents {
    fun page(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(20.dp, 20.dp, 20.dp, 96.dp)
        setBackgroundColor(PetColors.background)
    }

    fun title(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        textSize = 25f
        setTextColor(PetColors.text)
        setTypeface(typeface, Typeface.BOLD)
        letterSpacing = 0f
    }

    fun body(context: Context, text: String, muted: Boolean = false): TextView = TextView(context).apply {
        this.text = text
        textSize = 15f
        setTextColor(if (muted) PetColors.textMuted else PetColors.text)
        letterSpacing = 0f
        setLineSpacing(2.dp.toFloat(), 1f)
    }

    fun sectionLabel(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        textSize = 13f
        setTextColor(PetColors.textMuted)
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, 24.dp, 0, 8.dp)
        letterSpacing = 0f
    }

    fun statusPill(context: Context, text: String, active: Boolean = true): TextView = TextView(context).apply {
        this.text = text
        textSize = 12f
        gravity = Gravity.CENTER
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(if (active) PetColors.status else PetColors.textMuted)
        setPadding(11.dp, 7.dp, 11.dp, 7.dp)
        background = roundedDrawable(if (active) PetColors.statusSurface else PetColors.disabled, 18)
    }

    fun primaryButton(context: Context, text: String): MaterialButton = MaterialButton(context).apply {
        this.text = text
        textSize = 14f
        isAllCaps = false
        minHeight = 48.dp
        cornerRadius = 8.dp
        backgroundTintList = colorState(PetColors.primary)
        setTextColor(colorState(Color.WHITE, PetColors.textMuted))
        letterSpacing = 0f
    }

    fun secondaryButton(context: Context, text: String): MaterialButton = MaterialButton(context).apply {
        this.text = text
        textSize = 14f
        isAllCaps = false
        minHeight = 48.dp
        cornerRadius = 8.dp
        backgroundTintList = colorState(PetColors.surface)
        strokeColor = colorState(PetColors.border)
        strokeWidth = 1.dp
        setTextColor(colorState(PetColors.text, PetColors.textMuted))
        letterSpacing = 0f
    }

    fun metric(context: Context, label: String, value: Int, color: Int): View = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(12.dp, 11.dp, 12.dp, 11.dp)
        background = roundedDrawable(PetColors.surface, 8, PetColors.border)
        addView(body(context, "$label $value").apply {
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
        })
        addView(LinearProgressIndicator(context).apply {
            max = 100
            setProgressCompat(value, false)
            setIndicatorColor(color)
            trackColor = PetColors.disabled
            trackCornerRadius = 3.dp
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 7.dp).apply {
                topMargin = 9.dp
            }
        })
    }

    fun infoRow(context: Context, title: String, subtitle: String, trailing: String): View =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12.dp, 12.dp, 12.dp, 12.dp)
            background = roundedDrawable(Color.TRANSPARENT, 8, PetColors.border)
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(body(context, title).apply { setTypeface(typeface, Typeface.BOLD) })
                addView(body(context, subtitle, muted = true).apply { textSize = 12f })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(body(context, trailing).apply {
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
            })
        }
}
