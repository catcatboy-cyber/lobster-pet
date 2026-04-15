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
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.lobster.pet.LobsterApp
import com.lobster.pet.MainActivity
import com.lobster.pet.MenuActivity
import com.lobster.pet.R
import com.lobster.pet.view.LobsterView
import com.lobster.pet.voice.VoiceRecognizer
import com.lobster.pet.voice.CommandProcessor
import com.lobster.pet.lifecycle.LifeSyncManager
import kotlin.random.Random

/**
 * 悬浮窗服务 - 类似 iOS 辅助触控的小白点
 * 特点：不影响屏幕滑动，点击龙虾有交互
 */
class FloatingLobsterService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var lobsterView: LobsterView
    private lateinit var params: WindowManager.LayoutParams

    private val handler = Handler(Looper.getMainLooper())
    private var isMoving = true
    private var isTouching = false
    private var isMenuOpen = false
    private val random = Random
    
    // 饲养状态
    private var hungerLevel = 50 // 0-100，越低越饿
    private var happinessLevel = 70 // 0-100，心情值
    
    // 动画控制
    private var currentAnimation: Runnable? = null
    private var menuCloseRunnable: Runnable? = null
    
    // 语音控制
    private var voiceRecognizer: VoiceRecognizer? = null
    private var commandProcessor: CommandProcessor? = null
    private var isListening = false
    
    // 生活同步
    private var lifeSyncManager: LifeSyncManager? = null
    private var lastGreeting: String? = null
    
    private val moveRunnable = object : Runnable {
        override fun run() {
            if (isMoving && !isTouching && !isMenuOpen && !isListening) {
                randomMove()
            }
            // 随机间隔 2-6 秒
            handler.postDelayed(this, (2000 + random.nextInt(4000)).toLong())
        }
    }

    override fun onCreate() {
        super.onCreate()
        // 确保 instance 指向当前实例
        if (instance != null && instance !== this) {
            // 如果之前有旧实例，清理引用
        }
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        initFloatingWindow()
        startHungerTimer()
        initVoice()
        initLifeSync()
    }
    
    /**
     * 初始化生活同步
     */
    private fun initLifeSync() {
        lifeSyncManager = LifeSyncManager(this).apply {
            onStateChanged = { state ->
                // 更新龙虾外观
                lobsterView.updateByLifeState(state)
                
                // 显示问候语（每天首次或状态变化时）
                if (lastGreeting != state.greeting) {
                    lastGreeting = state.greeting
                    showToast(state.greeting)
                }
            }
            start()
        }
        
        // 启动主动提醒定时器
        startReminderTimer()
    }
    
    /**
     * 启动提醒定时器
     */
    private fun startReminderTimer() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                lifeSyncManager?.let { manager ->
                    when {
                        manager.shouldRemindWater() -> showToast("该喝水啦~ 🥤")
                        manager.shouldRemindSleep() -> showToast("很晚了，该睡觉了 🌙")
                        manager.shouldRemindBreak() -> showToast("工作很久了，休息一下吧 ☕")
                    }
                }
                handler.postDelayed(this, 60000) // 每分钟检查
            }
        }, 60000)
    }

    /**
     * 初始化语音识别
     */
    private fun initVoice() {
        commandProcessor = CommandProcessor(this)
        voiceRecognizer = VoiceRecognizer(this, object : VoiceRecognizer.OnVoiceResultListener {
            override fun onResult(text: String) {
                isListening = false
                lobsterView.showNormal()
                showToast("你说: $text")
                commandProcessor?.processCommand(text)
            }

            override fun onError(error: String) {
                isListening = false
                lobsterView.showNormal()
                showToast(error)
            }
        })
    }

    /**
     * 开始语音指令
     */
    fun startVoiceCommand() {
        if (isListening) return
        if (voiceRecognizer == null) {
            showToast("语音识别不可用")
            return
        }
        isListening = true
        lobsterView.showListening()
        voiceRecognizer?.startListening()
        showToast("请说话...")
    }

    private fun initFloatingWindow() {
        lobsterView = LobsterView(this)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // 关键配置：不拦截触摸事件，像 iOS 辅助触控一样
        params = WindowManager.LayoutParams(
            120.dpToPx(), // 固定小尺寸，只覆盖龙虾
            120.dpToPx(),
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or      // 不获取焦点
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or    // 触摸事件穿透
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,     // 可以超出屏幕边界
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 500
        }

        // 设置龙虾视图大小
        lobsterView.layoutParams = android.widget.FrameLayout.LayoutParams(
            120.dpToPx(),
            120.dpToPx()
        )

        // 触摸处理：单击菜单，长按拖动
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var isDragging = false
        var touchStartTime = 0L

        lobsterView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isTouching = true
                    isDragging = false
                    touchStartTime = System.currentTimeMillis()
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = Math.abs(event.rawX - touchX)
                    val deltaY = Math.abs(event.rawY - touchY)
                    val moveThreshold = 15.dpToPx()
                    
                    if (deltaX > moveThreshold || deltaY > moveThreshold) {
                        isDragging = true
                        params.x = initialX + (event.rawX - touchX).toInt()
                        params.y = initialY + (event.rawY - touchY).toInt()
                        updateViewPosition()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    isTouching = false
                    val touchDuration = System.currentTimeMillis() - touchStartTime
                    
                    if (!isDragging && touchDuration < 300) {
                        // 单击 - 显示菜单
                        showMenu()
                    }
                    true
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

    /**
     * 显示菜单：喂食、退出
     */
    private fun showMenu() {
        if (isMenuOpen) return
        isMenuOpen = true
        isMoving = false
        
        // 取消之前的关闭任务
        menuCloseRunnable?.let { handler.removeCallbacks(it) }
        
        // 创建菜单对话框
        val menuIntent = Intent(this, MenuActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("hunger", hungerLevel)
            putExtra("happiness", happinessLevel)
        }
        startActivity(menuIntent)
        
        // 3秒后恢复（如果菜单没手动关闭）
        menuCloseRunnable = Runnable {
            if (isMenuOpen) {
                isMenuOpen = false
                isMoving = true
            }
        }
        handler.postDelayed(menuCloseRunnable!!, 3000)
    }

    /**
     * 菜单关闭时调用（由 MenuActivity 触发）
     */
    fun onMenuClosed() {
        menuCloseRunnable?.let { handler.removeCallbacks(it) }
        isMenuOpen = false
        isMoving = true
    }

    /**
     * 喂食
     */
    fun feed() {
        hungerLevel = (hungerLevel + 30).coerceAtMost(100)
        happinessLevel = (happinessLevel + 10).coerceAtMost(100)
        lobsterView.showEating()
        showToast("龙虾吃得很开心！🦞")
    }

    /**
     * 退出龙虾
     */
    fun quit() {
        showToast("龙虾退下了，再见！👋")
        stopSelf()
    }

    /**
     * 饥饿计时器
     */
    private fun startHungerTimer() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!isMenuOpen) {
                    hungerLevel = (hungerLevel - 1).coerceAtLeast(0)
                    happinessLevel = (happinessLevel - 1).coerceAtLeast(0)
                    
                    // 根据状态改变外观
                    when {
                        hungerLevel < 20 -> lobsterView.showHungry()
                        happinessLevel < 30 -> lobsterView.showSad()
                        else -> lobsterView.showNormal()
                    }
                }
                handler.postDelayed(this, 30000) // 每30秒减少一点
            }
        }, 30000)
    }

    private fun randomMove() {
        if (isTouching || isMenuOpen) return

        val deltaX = random.nextInt(120) - 60
        val deltaY = random.nextInt(120) - 60

        val (screenWidth, screenHeight) = getScreenSize()
        val newX = (params.x + deltaX).coerceIn(0, screenWidth - 120.dpToPx())
        val newY = (params.y + deltaY).coerceIn(100, screenHeight - 200.dpToPx())

        animateMove(params.x, params.y, newX, newY)
    }

    private fun animateMove(startX: Int, startY: Int, endX: Int, endY: Int) {
        currentAnimation?.let { handler.removeCallbacks(it) }
        
        val duration = 600L
        val startTime = System.currentTimeMillis()

        currentAnimation = object : Runnable {
            override fun run() {
                if (isTouching || isMenuOpen || !::lobsterView.isInitialized) return
                
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                val eased = progress * (2 - progress)

                params.x = (startX + (endX - startX) * eased).toInt()
                params.y = (startY + (endY - startY) * eased).toInt()

                updateViewPosition()

                if (progress < 1f && !isTouching) {
                    handler.postDelayed(this, 16)
                }
            }
        }

        handler.post(currentAnimation!!)
    }

    private fun updateViewPosition() {
        try {
            windowManager.updateViewLayout(lobsterView, params)
        } catch (e: Exception) {
            // 忽略
        }
    }

    private fun getScreenSize(): Pair<Int, Int> {
        return try {
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(metrics)
            Pair(metrics.widthPixels, metrics.heightPixels)
        } catch (e: Exception) {
            Pair(1080, 1920)
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * applicationContext.resources.displayMetrics.density).toInt()
    }

    private fun showToast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
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
            .setContentTitle("龙虾宠物运行中 🦞")
            .setContentText("点击龙虾进行互动")
            .setSmallIcon(R.drawable.ic_lobster)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        lifeSyncManager?.stop()
        lifeSyncManager = null
        handler.removeCallbacksAndMessages(null)
        voiceRecognizer?.destroy()
        voiceRecognizer = null
        try {
            if (::lobsterView.isInitialized) {
                windowManager.removeView(lobsterView)
            }
        } catch (e: Exception) {
            // 忽略
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        
        // 供 MenuActivity 调用
        var instance: FloatingLobsterService? = null
    }
}
