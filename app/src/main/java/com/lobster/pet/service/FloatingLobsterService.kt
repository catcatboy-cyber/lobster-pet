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
import android.util.DisplayMetrics
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
    private var isTouching = false
    private val random = Random
    
    // 动画控制
    private var currentAnimation: Runnable? = null
    private val moveRunnable = object : Runnable {
        override fun run() {
            if (isMoving && !isTouching) {
                randomMove()
                handler.postDelayed(this, (3000 + random.nextInt(4000)).toLong())
            } else {
                handler.postDelayed(this, 1000)
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

        // 点击监听
        lobsterView.setOnClickListener {
            lobsterView.showListening()
            voiceRecognizer.startListening()
        }

        // 长按拖动
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var isDragging = false

        lobsterView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isTouching = true
                    isDragging = false
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = Math.abs(event.rawX - touchX)
                    val deltaY = Math.abs(event.rawY - touchY)
                    if (deltaX > 10 || deltaY > 10) {
                        isDragging = true
                        params.x = initialX + (event.rawX - touchX).toInt()
                        params.y = initialY + (event.rawY - touchY).toInt()
                        try {
                            windowManager.updateViewLayout(lobsterView, params)
                        } catch (e: Exception) {
                            // 忽略更新失败
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    isTouching = false
                    isDragging
                }
                else -> false
            }
        }

        try {
            windowManager.addView(lobsterView, params)
            handler.postDelayed(moveRunnable, 2000)
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun initVoiceRecognizer() {
        try {
            voiceRecognizer = VoiceRecognizer(this, object : VoiceRecognizer.OnVoiceResultListener {
                override fun onResult(text: String) {
                    lobsterView.showNormal()
                    commandProcessor.processCommand(text)
                }

                override fun onError(error: String) {
                    lobsterView.showNormal()
                }
            })
        } catch (e: Exception) {
            // 语音识别初始化失败，继续运行但不支持语音
        }
    }

    private fun randomMove() {
        if (isTouching) return

        val deltaX = random.nextInt(150) - 75
        val deltaY = random.nextInt(150) - 75

        val (screenWidth, screenHeight) = getScreenSize()
        val newX = (params.x + deltaX).coerceIn(50, screenWidth - 250)
        val newY = (params.y + deltaY).coerceIn(100, screenHeight - 300)

        animateMove(params.x, params.y, newX, newY)
    }

    private fun animateMove(startX: Int, startY: Int, endX: Int, endY: Int) {
        // 取消之前的动画
        currentAnimation?.let { handler.removeCallbacks(it) }
        
        val duration = 800L
        val startTime = System.currentTimeMillis()

        currentAnimation = object : Runnable {
            override fun run() {
                if (isTouching || !::lobsterView.isInitialized) return
                
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                // 使用缓动函数
                val eased = progress * (2 - progress)

                params.x = (startX + (endX - startX) * eased).toInt()
                params.y = (startY + (endY - startY) * eased).toInt()

                try {
                    windowManager.updateViewLayout(lobsterView, params)
                } catch (e: Exception) {
                    return
                }

                if (progress < 1f && !isTouching) {
                    handler.postDelayed(this, 16)
                }
            }
        }

        handler.post(currentAnimation!!)
    }

    private fun getScreenSize(): Pair<Int, Int> {
        return try {
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            Pair(metrics.widthPixels, metrics.heightPixels)
        } catch (e: Exception) {
            Pair(1080, 1920) // 默认值
        }
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
        handler.removeCallbacksAndMessages(null)
        try {
            if (::voiceRecognizer.isInitialized) {
                voiceRecognizer.destroy()
            }
            if (::lobsterView.isInitialized) {
                windowManager.removeView(lobsterView)
            }
        } catch (e: Exception) {
            // 忽略清理错误
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
