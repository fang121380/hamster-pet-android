package com.andrew.hamsterpet

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import com.google.android.material.materialswitch.MaterialSwitch

@SuppressLint("ViewConstructor")
class SettingsPageView(
    context: Context,
    state: PetState,
    onOverlayChanged: (Boolean) -> Unit,
    onMotionModeChanged: (MotionMode) -> Unit,
    onSkinChanged: (HamsterSkin) -> Unit,
    onTintChanged: (FurTint) -> Unit,
    onMusicEnabledChanged: (Boolean) -> Unit,
    onSwitchMusic: () -> Unit,
    onVolumeChanged: (Int) -> Unit,
    onReset: () -> Unit,
) : ScrollView(context) {
    init {
        isFillViewport = true
        addView(UiComponents.page(context).apply {
            addView(UiComponents.title(context, "仓鼠小梵设置"))
            addView(UiComponents.body(context, "养成数据只保存在这台手机上。", muted = true).apply {
                setPadding(0, 7.dp, 0, 8.dp)
            })

            addView(UiComponents.sectionLabel(context, "运行"))
            addView(settingSwitch(
                context = context,
                title = "显示桌宠",
                subtitle = "关闭仓鼠、小窝、幼崽和常驻服务",
                checked = state.overlayRunning,
                description = "显示桌宠开关",
                onChanged = onOverlayChanged,
            ))
            addView(UiComponents.infoRow(context, "通知快捷关闭", "常驻通知提供关闭按钮", "已启用"))

            addView(UiComponents.sectionLabel(context, "动作强度"))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                listOf(MotionMode.GENTLE to "轻柔", MotionMode.STANDARD to "标准").forEachIndexed { index, (mode, label) ->
                    val button = if (state.motionMode == mode) {
                        UiComponents.primaryButton(context, label)
                    } else {
                        UiComponents.secondaryButton(context, label)
                    }
                    button.setOnClickListener { onMotionModeChanged(mode) }
                    addView(button, LinearLayout.LayoutParams(0, 48.dp, 1f).apply {
                        if (index > 0) leftMargin = 8.dp
                    })
                }
            })
            addView(UiComponents.infoRow(context, "动态效果", "轻柔模式动作幅度更小、节奏更慢", if (state.motionMode == MotionMode.GENTLE) "轻柔" else "标准"))

            addView(UiComponents.sectionLabel(context, "仓鼠外观"))
            addView(choiceRow(context, HamsterSkin.entries.map { it to it.label }, state.skin, onSkinChanged))
            addView(choiceRow(context, FurTint.entries.map { it to it.label }, state.furTint, onTintChanged), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                44.dp,
            ).apply { topMargin = 8.dp })

            addView(UiComponents.sectionLabel(context, "音乐"))
            addView(settingSwitch(
                context = context,
                title = "背景音乐",
                subtitle = "桌宠开启后自动播放，关闭后同步停止",
                checked = state.musicEnabled,
                description = "背景音乐开关",
                onChanged = onMusicEnabledChanged,
            ))
            addView(UiComponents.secondaryButton(context, "切换下一首").apply {
                setOnClickListener { onSwitchMusic() }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 48.dp).apply { topMargin = 8.dp })
            addView(UiComponents.body(context, "音量 ${state.bgmVolume}%", muted = true).apply {
                setPadding(0, 10.dp, 0, 0)
            })
            addView(SeekBar(context).apply {
                max = 100
                progress = state.bgmVolume
                contentDescription = "背景音乐音量"
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    private var pending = state.bgmVolume
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (fromUser) pending = progress
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                    override fun onStopTrackingTouch(seekBar: SeekBar?) = onVolumeChanged(pending)
                })
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 48.dp))

            addView(UiComponents.sectionLabel(context, "数据"))
            addView(UiComponents.secondaryButton(context, "重置全部养成进度").apply {
                setTextColor(PetColors.danger)
                setOnClickListener { onReset() }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 52.dp))
        })
    }

    private fun settingSwitch(
        context: Context,
        title: String,
        subtitle: String,
        checked: Boolean,
        description: String,
        onChanged: (Boolean) -> Unit,
    ) = LinearLayout(context).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, 9.dp, 0, 9.dp)
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(UiComponents.body(context, title).apply { setTypeface(typeface, Typeface.BOLD) })
            addView(UiComponents.body(context, subtitle, muted = true).apply { textSize = 12f })
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(MaterialSwitch(context).apply {
            isChecked = checked
            contentDescription = description
            setOnCheckedChangeListener { _, value -> if (value != checked) onChanged(value) }
        })
    }

    private fun <T> choiceRow(
        context: Context,
        choices: List<Pair<T, String>>,
        selected: T,
        onSelected: (T) -> Unit,
    ) = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        choices.forEachIndexed { index, (choice, label) ->
            val button = if (choice == selected) UiComponents.primaryButton(context, label) else UiComponents.secondaryButton(context, label)
            button.maxLines = 1
            button.includeFontPadding = false
            button.setPadding(4.dp, 0, 4.dp, 0)
            button.setOnClickListener { onSelected(choice) }
            addView(button, LinearLayout.LayoutParams(0, 44.dp, 1f).apply { if (index > 0) leftMargin = 4.dp })
        }
    }
}
