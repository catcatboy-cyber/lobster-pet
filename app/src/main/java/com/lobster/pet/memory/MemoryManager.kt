package com.lobster.pet.memory

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*

/**
 * 记忆管理器
 * 负责学习、存储、回忆用户相关信息
 */
class MemoryManager(context: Context) {

    private val TAG = "MemoryManager"
    private val database = MemoryDatabase.getInstance(context)
    private val dao = database.memoryDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 缓存常用数据
    private var cachedPreferences = mutableMapOf<String, String>()
    private var recentMemories = listOf<UserMemory>()

    init {
        // 启动时加载偏好设置到内存
        scope.launch {
            dao.getAllPreferences().collect { prefs ->
                cachedPreferences.clear()
                prefs.forEach { cachedPreferences[it.key] = it.value }
            }
        }
        
        // 定期刷新近期记忆
        scope.launch {
            while (isActive) {
                refreshRecentMemories()
                delay(300000) // 5分钟刷新一次
            }
        }
    }

    /**
     * 记住用户的喜好
     */
    fun rememberLike(thing: String, context: String? = null) {
        scope.launch {
            dao.insertMemory(UserMemory(
                type = MemoryType.LIKES,
                content = thing,
                context = context
            ))
            Log.d(TAG, "Remembered like: $thing")
        }
    }

    /**
     * 记住用户的厌恶
     */
    fun rememberDislike(thing: String, context: String? = null) {
        scope.launch {
            dao.insertMemory(UserMemory(
                type = MemoryType.DISLIKES,
                content = thing,
                context = context
            ))
            Log.d(TAG, "Remembered dislike: $thing")
        }
    }

    /**
     * 记住习惯
     */
    fun rememberHabit(habit: String, context: String? = null) {
        scope.launch {
            dao.insertMemory(UserMemory(
                type = MemoryType.HABIT,
                content = habit,
                context = context
            ))
            Log.d(TAG, "Remembered habit: $habit")
        }
    }

    /**
     * 记住用户说过的话/事情
     */
    fun rememberFact(content: String, type: MemoryType = MemoryType.RANDOM) {
        scope.launch {
            dao.insertMemory(UserMemory(
                type = type,
                content = content
            ))
            Log.d(TAG, "Remembered fact: $content")
        }
    }

    /**
     * 记住用户当前做的事情/状态
     */
    fun rememberCurrentActivity(activity: String) {
        scope.launch {
            dao.setPreference(UserPreference(
                key = "current_activity",
                value = activity
            ))
            cachedPreferences["current_activity"] = activity
        }
    }

    /**
     * 记录聊天话题
     */
    fun rememberChatTopic(topic: String, summary: String, userMood: String? = null) {
        scope.launch {
            dao.insertChatHistory(ChatHistory(
                topic = topic,
                summary = summary,
                userMood = userMood
            ))
        }
    }

    /**
     * 添加重要日期
     */
    fun addImportantDate(title: String, date: Date, type: DateType, isRecurring: Boolean = true) {
        scope.launch {
            dao.insertImportantDate(ImportantDate(
                title = title,
                date = date.time,
                type = type,
                isRecurring = isRecurring
            ))
        }
    }

    /**
     * 设置用户偏好
     */
    fun setPreference(key: String, value: String) {
        scope.launch {
            dao.setPreference(UserPreference(key = key, value = value))
            cachedPreferences[key] = value
        }
    }

    /**
     * 获取用户偏好
     */
    fun getPreference(key: String): String? {
        return cachedPreferences[key]
    }

    /**
     * 获取用户名字
     */
    fun getUserName(): String? {
        return cachedPreferences["user_name"]
    }

    /**
     * 设置用户名字
     */
    fun setUserName(name: String) {
        setPreference("user_name", name)
    }

    /**
     * 获取最近的记忆
     */
    suspend fun getRecentMemories(limit: Int = 5): List<UserMemory> {
        return dao.getRecentMemories(limit)
    }

    private suspend fun refreshRecentMemories() {
        recentMemories = dao.getRecentMemories(10)
    }

    /**
     * 获取用户喜欢的所有东西
     */
    fun getLikes(): Flow<List<UserMemory>> {
        return dao.getMemoriesByType(MemoryType.LIKES)
    }

    /**
     * 获取用户讨厌的东西
     */
    fun getDislikes(): Flow<List<UserMemory>> {
        return dao.getMemoriesByType(MemoryType.DISLIKES)
    }

    /**
     * 获取习惯
     */
    fun getHabits(): Flow<List<UserMemory>> {
        return dao.getMemoriesByType(MemoryType.HABIT)
    }

    /**
     * 获取即将到期的日期
     */
    suspend fun getUpcomingDates(days: Int = 7): List<ImportantDate> {
        val now = Calendar.getInstance()
        val end = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }
        return dao.getDatesInRange(now.timeInMillis, end.timeInMillis)
    }

    /**
     * 生成个性化问候语
     */
    suspend fun generatePersonalizedGreeting(): String {
        val name = getUserName()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val recentChats = dao.getRecentChats(3)
        
        val baseGreeting = when (hour) {
            in 6..11 -> listOf("早上好", "早安", "新的一天开始了")
            in 12..13 -> listOf("中午好", "午安")
            in 14..17 -> listOf("下午好", "下午精神怎么样")
            in 18..21 -> listOf("晚上好", "今天过得如何")
            else -> listOf("还没睡啊", "注意休息")
        }.random()
        
        val namePart = if (name != null) "，$name" else ""
        
        // 根据最近聊天记录生成后续
        val followUp = when {
            recentChats.isEmpty() -> ""
            recentChats.any { it.topic.contains("工作") || it.topic.contains("项目") } -> 
                listOf("昨天说的工作还顺利吗？", "项目进度怎么样了？").random()
            recentChats.any { it.userMood == "sad" || it.userMood == "tired" } ->
                listOf("今天心情好点了吗？", "别太累了").random()
            else -> ""
        }
        
        return "$baseGreeting$namePart$followUp"
    }

    /**
     * 分析用户话语中的记忆点
     */
    fun analyzeAndRemember(text: String) {
        // 简单关键词提取，实际可以用AI分析
        when {
            text.contains("喜欢") || text.contains("爱") -> {
                val thing = extractAfterKeyword(text, listOf("喜欢", "爱"))
                if (thing.isNotEmpty()) rememberLike(thing, text)
            }
            text.contains("讨厌") || text.contains("不喜欢") || text.contains("烦") -> {
                val thing = extractAfterKeyword(text, listOf("讨厌", "不喜欢"))
                if (thing.isNotEmpty()) rememberDislike(thing, text)
            }
            text.contains("习惯") || text.contains("经常") || text.contains("总是") -> {
                rememberHabit(text)
            }
            text.contains("我是") || text.contains("我叫") -> {
                val name = extractName(text)
                if (name.isNotEmpty()) setUserName(name)
            }
        }
    }

    private fun extractAfterKeyword(text: String, keywords: List<String>): String {
        for (keyword in keywords) {
            val index = text.indexOf(keyword)
            if (index != -1) {
                return text.substring(index + keyword.length)
                    .trim()
                    .take(20)
                    .replace(Regex("[，。！？,.!?]"), "")
            }
        }
        return ""
    }

    private fun extractName(text: String): String {
        val patterns = listOf(
            "我叫(.+?)[，。]".toRegex(),
            "我是(.+?)[，。]".toRegex()
        )
        for (pattern in patterns) {
            pattern.find(text)?.groupValues?.get(1)?.let { return it.trim() }
        }
        return ""
    }

    /**
     * 清理
     */
    fun cleanup() {
        scope.cancel()
    }
}
