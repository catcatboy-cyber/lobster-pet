package com.lobster.pet.memory

import android.content.Context
import android.util.Log
import com.lobster.pet.lifecycle.LifeSyncManager
import kotlinx.coroutines.*
import java.util.*

/**
 * 主动交互管理器
 * 根据记忆和生活状态，主动发起对话
 */
class ProactiveInteractionManager(
    private val context: Context,
    private val memoryManager: MemoryManager,
    private val onSpeak: (String) -> Unit
) {

    private val TAG = "ProactiveInteraction"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // 随机事件定时器
    private var randomInteractionJob: Job? = null
    
    // 避免短时间内多次触发
    private var lastInteractionTime = 0L
    private val MIN_INTERVAL = 300000L // 5分钟
    
    // 今日已说过的内容
    private val todaySpoken = mutableSetOf<String>()

    fun start() {
        // 启动龙虾时的首次问候
        scope.launch {
            delay(2000) // 等龙虾完全显示
            sayGreeting()
        }
        
        // 启动随机主动交互
        startRandomInteractions()
        
        // 定时检查特殊时机
        startScheduledChecks()
    }

    fun stop() {
        randomInteractionJob?.cancel()
        scope.cancel()
    }

    /**
     * 启动随机主动交互
     */
    private fun startRandomInteractions() {
        randomInteractionJob = scope.launch {
            while (isActive) {
                // 随机间隔 10-30 分钟
                val delayMinutes = (10..30).random()
                delay(delayMinutes * 60 * 1000L)
                
                // 只在用户可能使用手机的时间触发
                if (isAppropriateTime()) {
                    triggerRandomInteraction()
                }
            }
        }
    }

    /**
     * 启动定时检查
     */
    private fun startScheduledChecks() {
        scope.launch {
            while (isActive) {
                delay(60000) // 每分钟检查一次
                checkSpecialMoments()
            }
        }
    }

    /**
     * 判断是否适合主动说话的时机
     */
    private fun isAppropriateTime(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        // 避开深夜
        if (hour in 1..6) return false
        
        // 检查间隔
        if (System.currentTimeMillis() - lastInteractionTime < MIN_INTERVAL) return false
        
        return true
    }

    /**
     * 检查特殊时刻
     */
    private suspend fun checkSpecialMoments() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        // 首次开启时的问候（早上）
        if (hour == 8 && minute == 0 && !todaySpoken.contains("morning_greeting")) {
            todaySpoken.add("morning_greeting")
            sayGreeting()
        }
        
        // 深夜关怀
        if (hour == 23 && minute == 30 && !todaySpoken.contains("late_night")) {
            todaySpoken.add("late_night")
            speak("很晚了，早点休息吧")
        }
        
        // 检查重要日期
        val upcoming = memoryManager.getUpcomingDates(3)
        for (date in upcoming) {
            val key = "date_${date.id}"
            if (!todaySpoken.contains(key)) {
                todaySpoken.add(key)
                speak("${date.title}快到了，记得准备一下")
            }
        }
    }

    /**
     * 触发随机交互
     */
    private suspend fun triggerRandomInteraction() {
        val interactions = listOf(
            ::sayRandomMemory,
            ::sayRandomConcern,
            ::sayRandomFact,
            ::askRandomQuestion
        )
        
        interactions.random().invoke()
    }

    /**
     * 问候语
     */
    private suspend fun sayGreeting() {
        val greeting = memoryManager.generatePersonalizedGreeting()
        speak(greeting)
    }

    /**
     * 唤起随机记忆
     */
    private suspend fun sayRandomMemory() {
        val memories = memoryManager.getRecentMemories(5)
        if (memories.isEmpty()) {
            sayRandomFact()
            return
        }
        
        val memory = memories.random()
        val message = when (memory.type) {
            MemoryType.LIKES -> "记得你说过喜欢${memory.content}，最近有关注吗？"
            MemoryType.DISLIKES -> "听说你不喜欢${memory.content}，我记住了"
            MemoryType.HABIT -> "你平时${memory.content}，今天呢？"
            MemoryType.WORK -> "之前说${memory.content}，进度怎么样了？"
            else -> "还记得${memory.content}吗？"
        }
        speak(message)
    }

    /**
     * 表达关心
     */
    private fun sayRandomConcern() {
        val concerns = listOf(
            "今天过得怎么样？",
            "有什么我可以帮忙的吗？",
            "累了就休息一下",
            "记得喝水",
            "眼睛累了看看远处",
            "别一直盯着屏幕",
            "工作多久了？休息一下吧"
        )
        speak(concerns.random())
    }

    /**
     * 分享随机趣事
     */
    private fun sayRandomFact() {
        val facts = listOf(
            "龙虾可以活到100岁你知道吗？",
            "我今天学了新东西，要不要听？",
            "听说今天是个好天气",
            "我在思考虾生...",
            "你知道虾有心脏吗？在背上！",
            "我今天心情不错~"
        )
        speak(facts.random())
    }

    /**
     * 问随机问题
     */
    private suspend fun askRandomQuestion() {
        val name = memoryManager.getUserName()
        val questions = mutableListOf(
            "最近在忙什么？",
            "有什么新鲜事吗？",
            "今天有什么计划？",
            "想聊天吗？"
        )
        
        if (name != null) {
            questions.add("$name，今天开心吗？")
        }
        
        speak(questions.random())
    }

    /**
     * 用户长时间未操作后的关心
     */
    fun onUserInactive(duration: Long) {
        if (duration > 300000 && isAppropriateTime()) { // 5分钟未操作
            scope.launch {
                speak("你还在吗？")
            }
        }
    }

    /**
     * 用户解锁手机时的反应
     */
    fun onScreenUnlock() {
        if (!isAppropriateTime()) return
        
        scope.launch {
            delay(1000) // 稍等一下
            val reactions = listOf(
                "回来啦~",
                "欢迎回来！",
                "想我了吗？",
                "龙虾在此守候！"
            )
            speak(reactions.random())
        }
    }

    /**
     * 用户打开特定应用时的反应
     */
    fun onAppOpened(packageName: String) {
        when (packageName) {
            "com.tencent.mm" -> {
                if (isAppropriateTime()) {
                    speak("去聊天啦？记得有我陪你就够了")
                }
            }
            "com.android.chrome", "com.tencent.mm" -> {
                // 浏览器或微信
            }
        }
    }

    /**
     * 说话
     */
    private fun speak(message: String) {
        if (message.isBlank()) return
        
        lastInteractionTime = System.currentTimeMillis()
        onSpeak(message)
        Log.d(TAG, "Speaking: $message")
    }

    /**
     * 外部调用的说话方法（用户触发）
     */
    fun speakFromUser(text: String) {
        // 分析用户话语，记住关键信息
        memoryManager.analyzeAndRemember(text)
    }

    /**
     * 获取建议的主动对话（供定时触发）
     */
    fun getSuggestedDialogue(): String? {
        if (!isAppropriateTime()) return null
        
        return null // 由异步方法处理
    }
}
