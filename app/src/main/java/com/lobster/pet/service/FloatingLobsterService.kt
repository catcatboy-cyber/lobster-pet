package com.lobster.pet.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.lobster.pet.LobsterApp
import com.lobster.pet.MainActivity
import com.lobster.pet.R
import com.lobster.pet.view.LobsterView
import com.lobster.pet.voice.CommandProcessor
import com.lobster.pet.voice.VoiceRecognizer
import kotlin.random.Random

class FloatingLobsterService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var lobsterView: LobsterView
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var voiceRecognizer: VoiceRecognizer
    private val commandProcessor = CommandProcessor(this)

    private val handler = Handler(Looper.getMainLooper())
    private var isMoving = true
    private val random = Random

    // 随机游动相关
    private val moveRunnable = object : Runnable {
        override fun run() {
            if (isMoving) {
                randomMove()
                // 随机间隔 2-5 秒
                handler.postDelayed(this, (2000 + random.nextInt(3000)).toLong())
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        initFloatingWindow()
        initVoiceRecognizer()
    }

    private fun initFloatingWindow() {
        lobsterView = LobsterView(this)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 500
        }

        // 点击监听 - 点击龙虾开始听指令
        lobsterView.setOnClickListener {
            lobsterView.showListening()
            voiceRecognizer.startListening()
        }

        // 长按拖动
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f

        lobsterView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isMoving = false
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).toInt()
                    params.y = initialY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(lobsterView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    isMoving = true
                    false
                }
                else -> false
            }
        }

        windowManager.addView(lobsterView, params)
        handler.post(moveRunnable)
    }

    private fun initVoiceRecognizer() {
        voiceRecognizer = VoiceRecognizer(this, object : VoiceRecognizer.OnVoiceResultListener {
            override fun onResult(text: String) {
                lobsterView.showNormal()
                commandProcessor.processCommand(text)
            }

            override fun onError(error: String) {
                lobsterView.showNormal()
                // 错误处理，可以显示一个小提示
            }
        })
    }

    private fun randomMove() {
        // 计算新位置（小范围移动）
        val deltaX = random.nextInt(200) - 100
        val deltaY = random.nextInt(200) - 100

        val newX = (params.x + deltaX).coerceIn(0, getScreenWidth() - 200)
        val newY = (params.y + deltaY).coerceIn(0, getScreenHeight() - 200)

        // 动画移动
        val startX = params.x
        val startY = params.y
        val duration = 1000L
        val startTime = System.currentTimeMillis()

        val animation = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)

                params.x = (startX + (newX - startX) * progress).toInt()
                params.y = (startY + (newY - startY) * progress).toInt()

                try {
                    windowManager.updateViewLayout(lobsterView, params)
                } catch (e: Exception) {
                    return
                }

                if (progress < 1f) {
                    handler.postDelayed(this, 16)
                }
            }
        }

        handler.post(animation)
    }

    private fun getScreenWidth(): Int {
        return windowManager.defaultDisplay.width
    }

    private fun getScreenHeight(): Int {
        return windowManager.defaultDisplay.height
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, LobsterApp.CHANNEL_ID)
            .setContentTitle("龙虾宠物运行中")
            .setContentText("点击龙虾说出指令")
            .setSmallIcon(R.drawable.ic_lobster)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(moveRunnable)
        voiceRecognizer.destroy()
        if (::lobsterView.isInitialized) {
            windowManager.removeView(lobsterView)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
