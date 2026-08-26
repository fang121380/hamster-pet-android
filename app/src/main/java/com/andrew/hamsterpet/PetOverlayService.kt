package com.andrew.hamsterpet

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlin.math.max
import kotlin.math.min

class PetOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var repository: PetStateRepository
    private var petView: PetOverlayView? = null
    private var params: WindowManager.LayoutParams? = null
    private var walkAnimator: ValueAnimator? = null
    private var bgmController: BgmController? = null
    private var shuttingDown = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        repository = PetStateRepository.get(this)
        PetAudio.initialize(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == PetServiceActions.STOP) {
            shutdown()
            return START_NOT_STICKY
        }
        if (intent?.action != PetServiceActions.START) {
            shutdown()
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this)) {
            repository.update { it.copy(overlayRunning = false) }
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, notification())
        if (petView == null) attachPet()
        if (bgmController == null) bgmController = BgmController(this, repository).also { it.start() }
        repository.update { it.copy(overlayRunning = true) }
        return START_NOT_STICKY
    }

    private fun attachPet() {
        val layout = WindowManager.LayoutParams(
            236.dp,
            286.dp,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 18.dp
            y = 150.dp
        }
        val view = PetOverlayView(this, repository, object : PetOverlayView.Host {
            override fun moveBy(dx: Int, dy: Int) {
                walkAnimator?.cancel()
                val metrics = resources.displayMetrics
                layout.x = (layout.x + dx).coerceIn(0, max(0, metrics.widthPixels - layout.width))
                layout.y = (layout.y + dy).coerceIn(0, max(0, metrics.heightPixels - layout.height))
                updateLayout()
            }

            override fun walk(direction: Int, onEnd: () -> Unit) {
                walkAnimator?.cancel()
                val metrics = resources.displayMetrics
                val from = layout.x
                val edge = max(0, metrics.widthPixels - layout.width)
                val gentle = repository.current().motionMode == MotionMode.GENTLE
                val distance = if (gentle) 90.dp else 170.dp
                val target = if (direction > 0) min(edge, from + distance) else max(0, from - distance)
                walkAnimator = ValueAnimator.ofInt(from, target).apply {
                    duration = if (gentle) 1900 else 1150
                    addUpdateListener {
                        layout.x = it.animatedValue as Int
                        updateLayout()
                    }
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) = onEnd()
                    })
                    start()
                }
            }

            override fun resize(width: Int, height: Int) {
                if (layout.width == width && layout.height == height) return
                val centerX = layout.x + layout.width / 2
                val centerY = layout.y + layout.height / 2
                layout.width = width
                layout.height = height
                val metrics = resources.displayMetrics
                layout.x = centerX - width / 2
                layout.y = centerY - height / 2
                layout.x = layout.x.coerceIn(0, max(0, metrics.widthPixels - layout.width))
                layout.y = layout.y.coerceIn(0, max(0, metrics.heightPixels - layout.height))
                updateLayout()
            }

            override fun openChat() {
                startActivity(Intent(this@PetOverlayService, ChatActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }

            override fun toggleMusic() {
                repository.update { it.copy(musicEnabled = !it.musicEnabled) }
            }

            override fun nextTrack() {
                repository.update { it.copy(bgmTrackIndex = BgmTrack.nextIndex(it.bgmTrackIndex), musicEnabled = true) }
            }

            override fun closePet() = shutdown()
        })
        params = layout
        petView = view
        windowManager.addView(view, layout)
    }

    private fun updateLayout() {
        val view = petView ?: return
        val layout = params ?: return
        runCatching { windowManager.updateViewLayout(view, layout) }
    }

    private fun shutdown() {
        if (shuttingDown) return
        shuttingDown = true
        walkAnimator?.cancel()
        walkAnimator = null
        bgmController?.release()
        bgmController = null
        petView?.dispose()
        petView?.let { view -> runCatching { windowManager.removeViewImmediate(view) } }
        petView = null
        params = null
        repository.update { it.copy(overlayRunning = false) }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        walkAnimator?.cancel()
        bgmController?.release()
        bgmController = null
        petView?.dispose()
        petView?.let { view -> runCatching { windowManager.removeViewImmediate(view) } }
        petView = null
        repository.update { it.copy(overlayRunning = false) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.overlay_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = getString(R.string.overlay_channel_description) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun notification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val closeIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, PetOverlayService::class.java).setAction(PetServiceActions.STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.star_big_on)
            .setContentTitle("仓鼠桌宠正在陪伴你")
            .setContentText("轻点打开小窝，长按仓鼠展开互动")
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "关闭桌宠", closeIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "hamster_pet_overlay"
        private const val NOTIFICATION_ID = 410
    }
}
