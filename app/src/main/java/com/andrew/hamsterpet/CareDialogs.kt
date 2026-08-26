package com.andrew.hamsterpet

import android.content.Context
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog

object CareDialogs {
    fun showFoodPicker(context: Context, onFood: (FoodType) -> Unit) {
        val dialog = AlertDialog.Builder(context).setTitle("选择食物").create()
        dialog.setView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp, 4.dp, 16.dp, 8.dp)
            FoodType.entries.forEach { food ->
                val label = "${FoodAssets.nameFor(food)} · 饱食 +${food.satietyGain} · 亲密 +${food.affectionGain}"
                addView(UiComponents.secondaryButton(context, label).apply {
                    setOnClickListener {
                        dialog.dismiss()
                        onFood(food)
                    }
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 52.dp).apply { topMargin = 8.dp })
            }
        })
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "取消") { _, _ -> dialog.dismiss() }
        dialog.show()
    }
}
