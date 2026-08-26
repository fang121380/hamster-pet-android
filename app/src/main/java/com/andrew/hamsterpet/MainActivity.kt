package com.andrew.hamsterpet

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private enum class Page(val label: String) { HOME("首页"), NEST("小窝"), FAMILY("家庭"), SETTINGS("设置") }

    private lateinit var repository: PetStateRepository
    private lateinit var content: FrameLayout
    private lateinit var navigation: LinearLayout
    private var page = Page.HOME
    private var removeListener: (() -> Unit)? = null
    private val scrollOffsets = mutableMapOf<Page, Int>()
    private var attentionNoticeShown = false
    private val handler = Handler(Looper.getMainLooper())
    private val familyPageTicker by lazy(LazyThreadSafetyMode.NONE) {
        FamilyPageTicker(
            stateProvider = repository::current,
            familyPageProvider = { content.getChildAt(0) as? FamilyBreedingUi },
        )
    }
    private val ticker = object : Runnable {
        override fun run() {
            familyPageTicker.tick(page == Page.FAMILY, System.currentTimeMillis())
            handler.postDelayed(this, 1_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = PetStateRepository.get(this)
        page = savedInstanceState?.getString(KEY_PAGE)?.let { runCatching { Page.valueOf(it) }.getOrNull() } ?: Page.HOME
        content = FrameLayout(this)
        navigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(PetColors.surface)
            elevation = 8f
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(PetColors.background)
            addView(content, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(navigation, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 64.dp))
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(0, statusBars.top, 0, 0)
            insets
        }
        setContentView(root)
        removeListener = repository.addListener { state -> runOnUiThread { render(state) } }
        render(repository.refresh())
        resumePetIfNeeded()
        requestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized) {
            val state = repository.refresh()
            render(state)
            if (!attentionNoticeShown && PetGameEngine.needsAttention(state)) {
                val message = when {
                    state.satiety <= 35 -> "小梵有点饿了，记得喂点东西"
                    else -> "小梵想你了，陪它互动一下吧"
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                attentionNoticeShown = true
            }
        }
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    override fun onPause() {
        handler.removeCallbacks(ticker)
        super.onPause()
    }

    override fun onDestroy() {
        removeListener?.invoke()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_PAGE, page.name)
        super.onSaveInstanceState(outState)
    }

    private fun render(state: PetState) {
        (content.getChildAt(0) as? ScrollView)?.let { scrollOffsets[page] = it.scrollY }
        content.removeAllViews()
        val pageView = when (page) {
            Page.HOME -> HomePageView(this, state, ::setOverlayRunning, ::feedFromHome, ::interact, { selectPage(Page.NEST) }, ::openChat)
            Page.NEST -> NestPageView(this, state, ::buildNest)
            Page.FAMILY -> FamilyPageView(this, state, System.currentTimeMillis(), ::breedingAction, ::careForBabies)
            Page.SETTINGS -> SettingsPageView(
                this,
                state,
                ::setOverlayRunning,
                ::setMotionMode,
                ::setSkin,
                ::setFurTint,
                ::setMusicEnabled,
                ::switchBgmTrack,
                ::setBgmVolume,
                ::confirmReset,
            )
        }
        content.addView(pageView)
        (pageView as? ScrollView)?.post { pageView.scrollTo(0, scrollOffsets[page] ?: 0) }
        renderNavigation()
    }

    private fun renderNavigation() {
        navigation.removeAllViews()
        Page.entries.forEach { target ->
            navigation.addView(TextView(this).apply {
                text = target.label
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(if (page == target) PetColors.primary else PetColors.textMuted)
                if (page == target) setTypeface(typeface, android.graphics.Typeface.BOLD)
                background = selectableBackground()
                contentDescription = "${target.label}页面"
                setOnClickListener { selectPage(target) }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        }
    }

    private fun selectPage(target: Page) {
        (content.getChildAt(0) as? ScrollView)?.let { scrollOffsets[page] = it.scrollY }
        content.removeAllViews()
        page = target
        render(repository.refresh())
    }

    private fun setOverlayRunning(running: Boolean) {
        if (running) startPet() else stopPet()
    }

    private fun startPet() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this, "请允许仓鼠显示在其他应用上层", Toast.LENGTH_LONG).show()
            render(repository.update { it.copy(overlayRunning = false) })
            return
        }
        repository.update { it.copy(overlayRunning = true) }
        ContextCompat.startForegroundService(
            this,
            Intent(this, PetOverlayService::class.java).setAction(PetServiceActions.START),
        )
    }

    private fun resumePetIfNeeded() {
        if (!repository.current().overlayRunning || !Settings.canDrawOverlays(this)) return
        ContextCompat.startForegroundService(
            this,
            Intent(this, PetOverlayService::class.java).setAction(PetServiceActions.START),
        )
    }

    private fun stopPet() {
        repository.update { it.copy(overlayRunning = false) }
        startService(Intent(this, PetOverlayService::class.java).setAction(PetServiceActions.STOP))
    }

    private fun feedFromHome(food: FoodType) {
        val result = PetGameEngine.feed(repository.current(), food, System.currentTimeMillis())
        if (result.accepted) {
            val complete = {
                repository.update { result.state }
                Toast.makeText(this, "小梵吃得很开心", Toast.LENGTH_SHORT).show()
                Unit
            }
            val home = content.getChildAt(0) as? HomePageView
            if (home != null) home.playFeed(food, complete) else complete()
        } else {
            Toast.makeText(this, "已经吃饱啦，等一会儿再喂", Toast.LENGTH_SHORT).show()
        }
    }

    private fun interact() {
        val result = PetGameEngine.interact(repository.current(), System.currentTimeMillis())
        val complete = {
            repository.update { result.state }
            Toast.makeText(this, "亲密度增加了", Toast.LENGTH_SHORT).show()
            Unit
        }
        val home = content.getChildAt(0) as? HomePageView
        if (home != null) home.playInteract(complete) else complete()
    }

    private fun buildNest() {
        val before = repository.current()
        val after = PetGameEngine.buildNest(before)
        if (after == before) {
            Toast.makeText(this, PetUiModel.nestAction(before).supportingText, Toast.LENGTH_SHORT).show()
        } else {
            repository.update { after }
            PetAudio.play(this, PetSound.BUILD)
            Toast.makeText(this, if (after.nestLevel == 1) "小窝建好啦" else "小窝升级完成", Toast.LENGTH_SHORT).show()
        }
    }

    private fun breedingAction() {
        val now = System.currentTimeMillis()
        val state = repository.refresh(now)
        val updated = if (PetGameEngine.isBirthReady(state, now)) {
            PetGameEngine.completeBreeding(state, now)
        } else {
            PetGameEngine.startBreeding(state, now)
        }
        repository.update { updated }
    }

    private fun careForBabies(careType: BabyCareType) {
        repository.update { PetGameEngine.careForBabies(it, careType) }
        val message = if (careType == BabyCareType.FEED) "幼崽们吃饱了，喂食进度 +1" else "幼崽们玩得很开心，陪玩进度 +1"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setMotionMode(mode: MotionMode) {
        repository.update { it.copy(motionMode = mode) }
    }

    private fun setSkin(skin: HamsterSkin) {
        repository.update { it.copy(skin = skin) }
    }

    private fun setFurTint(tint: FurTint) {
        repository.update { it.copy(furTint = tint) }
    }

    private fun setMusicEnabled(enabled: Boolean) {
        repository.update { it.copy(musicEnabled = enabled) }
    }

    private fun switchBgmTrack() {
        val next = repository.update {
            it.copy(bgmTrackIndex = BgmTrack.nextIndex(it.bgmTrackIndex), musicEnabled = true)
        }
        Toast.makeText(this, "已切换：${BgmTrack.fromIndex(next.bgmTrackIndex).displayName}", Toast.LENGTH_SHORT).show()
    }

    private fun setBgmVolume(volume: Int) {
        repository.update { it.copy(bgmVolume = BgmTrack.clampVolume(volume)) }
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle("重置养成进度？")
            .setMessage("饱食度、亲密度、小窝和幼崽都会恢复初始状态。桌宠也会关闭。")
            .setNegativeButton("取消", null)
            .setPositiveButton("重置") { _, _ ->
                stopPet()
                repository.reset()
                selectPage(Page.HOME)
            }
            .show()
    }

    private fun openChat() {
        startActivity(Intent(this, ChatActivity::class.java))
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
    }

    companion object {
        private const val KEY_PAGE = "selected_page"
        private const val REQUEST_NOTIFICATIONS = 7
    }
}
