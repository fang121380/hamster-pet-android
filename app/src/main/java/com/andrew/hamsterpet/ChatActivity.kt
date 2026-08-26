package com.andrew.hamsterpet

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class ChatActivity : AppCompatActivity() {
    private lateinit var transcript: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var repository: PetStateRepository
    private lateinit var chatHistoryRepository: ChatHistoryRepository
    private lateinit var sendButton: MaterialButton
    private lateinit var hamsterAvatar: SpriteAnimationView
    private lateinit var ideaSpark: TextView
    private lateinit var companionStatus: TextView
    private val history = mutableListOf<ChatTurn>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var sending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = PetStateRepository.get(this)
        chatHistoryRepository = ChatHistoryRepository(this)
        window.statusBarColor = PetColors.background
        window.navigationBarColor = PetColors.surface

        transcript = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp, 8.dp, 16.dp, 20.dp)
        }
        scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(transcript)
        }
        input = EditText(this).apply {
            hint = "和仓鼠说点什么"
            textSize = 15f
            setTextColor(PetColors.text)
            setHintTextColor(PetColors.textMuted)
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_SEND
            background = roundedDrawable(PetColors.surface, 8, PetColors.border)
            setPadding(14.dp, 0, 14.dp, 0)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage()
                    true
                } else false
            }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(PetColors.background)
            addView(header(), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 76.dp))
            addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(composer(), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 72.dp))
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(0, statusBars.top, 0, 0)
            insets
        }
        setContentView(root)
        restoreHistory()
    }

    private fun header() = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(18.dp, 10.dp, 18.dp, 10.dp)
        setBackgroundColor(PetColors.surface)
        addView(TextView(this@ChatActivity).apply {
            text = "‹"
            textSize = 34f
            gravity = Gravity.CENTER
            setTextColor(PetColors.text)
            contentDescription = "返回"
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(40.dp, 48.dp))
        hamsterAvatar = SpriteAnimationView(this@ChatActivity).apply {
            contentDescription = "仓鼠头像"
            applyAppearance(repository.current())
            applyMotionMode(repository.current().motionMode)
            play(PetAnimation.IDLE)
        }
        addView(hamsterAvatar, LinearLayout.LayoutParams(48.dp, 48.dp).apply { marginStart = 4.dp })
        ideaSpark = TextView(this@ChatActivity).apply {
            text = "✦"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(android.graphics.Color.rgb(255, 180, 30))
            contentDescription = "灵光一现"
            visibility = View.GONE
        }
        addView(ideaSpark, LinearLayout.LayoutParams(28.dp, 28.dp))
        addView(LinearLayout(this@ChatActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@ChatActivity).apply {
                text = "仓鼠伙伴"
                textSize = 18f
                setTextColor(PetColors.text)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            companionStatus = TextView(this@ChatActivity).apply {
                text = "正在陪伴你"
                textSize = 12f
                setTextColor(PetColors.status)
            }
            addView(companionStatus)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = 10.dp })
        addView(TextView(this@ChatActivity).apply {
            text = "清空"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(PetColors.textMuted)
            contentDescription = "清空聊天记录"
            setOnClickListener { confirmClearHistory() }
        }, LinearLayout.LayoutParams(46.dp, 48.dp))
    }

    private fun composer() = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(12.dp, 9.dp, 12.dp, 9.dp)
        setBackgroundColor(PetColors.surface)
        addView(input, LinearLayout.LayoutParams(0, 52.dp, 1f))
        sendButton = MaterialButton(this@ChatActivity).apply {
            text = "发送"
            textSize = 14f
            isAllCaps = false
            maxLines = 1
            gravity = Gravity.CENTER
            includeFontPadding = false
            insetTop = 0
            insetBottom = 0
            minHeight = 48.dp
            setPadding(8.dp, 0, 8.dp, 0)
            cornerRadius = 8.dp
            backgroundTintList = colorState(PetColors.primary)
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener { sendMessage() }
        }
        addView(sendButton, LinearLayout.LayoutParams(88.dp, 52.dp).apply { marginStart = 8.dp })
    }

    private fun sendMessage() {
        if (sending) return
        val message = input.text.toString().trim()
        if (message.isEmpty()) return
        addBubble(message, fromPet = false)
        history += ChatTurn("user", message)
        persistHistory()
        input.text.clear()
        setSending(true)
        companionStatus.text = "正在认真思考"
        hamsterAvatar.play(PetAnimation.PAT)
        val pendingBubble = addBubble("正在思考…", fromPet = true)
        val state = repository.refresh()
        val requestHistory = history.dropLast(1)
        val thinkingStartedAt = SystemClock.uptimeMillis()

        Thread {
            val result = DeepSeekChatClient.fetchReply(
                apiKey = BuildConfig.DEEPSEEK_API_KEY,
                userMessage = message,
                state = state,
                history = requestHistory,
            )
            val remainingDelay = ChatTiming.remainingThinkingDelay(
                startedAt = thinkingStartedAt,
                now = SystemClock.uptimeMillis(),
            )
            mainHandler.postDelayed({
                if (isFinishing || isDestroyed) return@postDelayed
                transcript.removeView(pendingBubble)
                showIdeaMotion()
                val reply = result.getOrElse {
                    "AI 暂时没有连上，${ChatResponder.replyFor(message, state.babies.size)}"
                }
                history += ChatTurn("assistant", reply)
                persistHistory()
                addBubble(reply, fromPet = true)
                setSending(false)
            }, remainingDelay)
        }.start()
    }

    private fun setSending(isSending: Boolean) {
        sending = isSending
        input.isEnabled = !isSending
        sendButton.isEnabled = !isSending
    }

    private fun restoreHistory() {
        history += chatHistoryRepository.read()
        if (history.isEmpty()) {
            val greeting = "今天也要元气满满。想聊小窝、点心，还是幼崽？"
            history += ChatTurn("assistant", greeting)
            persistHistory()
        }
        history.forEach { turn ->
            addBubble(turn.content, fromPet = turn.role == "assistant")
        }
    }

    private fun persistHistory() {
        if (history.size > ChatHistoryCodec.MAX_SAVED_TURNS) {
            history.subList(0, history.size - ChatHistoryCodec.MAX_SAVED_TURNS).clear()
        }
        chatHistoryRepository.save(history)
    }

    private fun confirmClearHistory() {
        AlertDialog.Builder(this)
            .setTitle("清空聊天记录")
            .setMessage("将删除这台手机上的全部聊天记录。")
            .setNegativeButton("取消", null)
            .setPositiveButton("清空") { _, _ ->
                history.clear()
                chatHistoryRepository.clear()
                transcript.removeAllViews()
                restoreHistory()
            }
            .show()
    }


    private fun showIdeaMotion() {
        companionStatus.text = "想到啦"
        mainHandler.postDelayed({
            if (!isFinishing && !isDestroyed) companionStatus.text = "正在陪伴你"
        }, 1_100L)
        hamsterAvatar.play(PetAnimation.HAPPY, onComplete = {
            hamsterAvatar.play(PetAnimation.IDLE)
        })
        ideaSpark.visibility = View.VISIBLE
        ideaSpark.scaleX = .4f
        ideaSpark.scaleY = .4f
        ideaSpark.alpha = 0f
        ideaSpark.animate().alpha(1f).scaleX(1.2f).scaleY(1.2f).setDuration(180).withEndAction {
            ideaSpark.animate().alpha(0f).scaleX(.7f).scaleY(.7f).setDuration(360).withEndAction {
                ideaSpark.visibility = View.GONE
            }.start()
        }.start()
    }

    private fun addBubble(message: String, fromPet: Boolean): View {
        val container = FrameLayout(this).apply {
            setPadding(0, 5.dp, 0, 5.dp)
            addView(TextView(this@ChatActivity).apply {
                text = message
                textSize = 15f
                setTextColor(if (fromPet) PetColors.text else android.graphics.Color.WHITE)
                setPadding(13.dp, 10.dp, 13.dp, 10.dp)
                background = roundedDrawable(if (fromPet) PetColors.surface else PetColors.primary, 8, if (fromPet) PetColors.border else null)
            }, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                if (fromPet) Gravity.START else Gravity.END,
            ).apply { width = (resources.displayMetrics.widthPixels * .76f).toInt() })
        }
        transcript.addView(container, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
        return container
    }
}
