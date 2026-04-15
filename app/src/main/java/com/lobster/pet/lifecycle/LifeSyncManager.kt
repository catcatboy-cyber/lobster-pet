package com.lobster.pet.lifecycle

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.util.Log
import com.lobster.pet.view.LobsterView
import kotlinx.coroutines.*
import java.util.*

/**
 * 生活同步管理器
 * 处理时间、天气、节日等生活场景的同步
 */
class LifeSyncManager(private val context: Context) {

    private val TAG = "LifeSyncManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // 当前状态
    private var currentTimeOfDay = TimeOfDay.MORNING
    private var currentWeather = Weather.SUNNY
    private var currentSeason = Season.SPRING
    private var isHoliday = false
    private var holidayType: HolidayType? = null
    
    // 回调
    var onStateChanged: ((LifeState) -> Unit)? = null
    
    // 定时器
    private var timeCheckJob: Job? = null
    private var weatherCheckJob: Job? = null

    /**
     * 时间段枚举
     */
    enum class TimeOfDay {
        DAWN,      // 黎明 5-7
        MORNING,   // 早上 7-9
        WORK,      // 工作 9-12, 14-18
        LUNCH,     // 午休 12-14
        EVENING,   // 傍晚 18-22
        NIGHT,     // 深夜 22-24
        MIDNIGHT   // 凌晨 0-5
    }

    /**
     * 天气枚举
     */
    enum class Weather {
        SUNNY,     // 晴天
        CLOUDY,    // 多云
        RAINY,     // 雨天
        SNOWY,     // 雪天
        FOGGY,     // 雾天
        THUNDER    // 雷雨
    }

    /**
     * 季节枚举
     */
    enum class Season {
        SPRING, SUMMER, AUTUMN, WINTER
    }

    /**
     * 节日类型
     */
    enum class HolidayType {
        NEW_YEAR,       // 元旦
        SPRING_FESTIVAL,// 春节
        VALENTINE,      // 情人节
        LABOR_DAY,      // 劳动节
        DRAGON_BOAT,    // 端午节
        MID_AUTUMN,     // 中秋节
        NATIONAL_DAY,   // 国庆节
        CHRISTMAS,      // 圣诞节
        BIRTHDAY        // 用户生日
    }

    /**
     * 生活状态数据类
     */
    data class LifeState(
        val timeOfDay: TimeOfDay,
        val weather: Weather,
        val season: Season,
        val isHoliday: Boolean,
        val holidayType: HolidayType?,
        val greeting: String,
        val accessory: AccessoryType,
        val mood: Mood
    )

    /**
     * 配饰类型
     */
    enum class AccessoryType {
        NONE,           // 无
        SUNGLASSES,     // 墨镜（晴天）
        UMBRELLA,       // 雨伞（雨天）
        SCARF,          // 围巾（冬天/雪天）
        SANTA_HAT,      // 圣诞帽
        PARTY_HAT,      // 派对帽
        RED_ENVELOPE,   // 红包（春节）
        MOONCAKE        // 月饼（中秋）
    }

    /**
     * 心情
     */
    enum class Mood {
        ENERGETIC,  // 精力充沛
        HAPPY,      // 开心
        NORMAL,     // 正常
        SLEEPY,     // 困倦
        TIRED,      // 疲惫
        SAD         // 难过
    }

    fun start() {
        updateTimeOfDay()
        updateSeason()
        checkHoliday()
        
        // 每分钟检查一次时间变化
        timeCheckJob = scope.launch {
            while (isActive) {
                delay(60000) // 1分钟
                updateTimeOfDay()
                checkHoliday()
            }
        }
        
        // 每30分钟检查一次天气
        weatherCheckJob = scope.launch {
            while (isActive) {
                updateWeather()
                delay(1800000) // 30分钟
            }
        }
    }

    fun stop() {
        timeCheckJob?.cancel()
        weatherCheckJob?.cancel()
        scope.cancel()
    }

    /**
     * 更新时间段
     */
    private fun updateTimeOfDay() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val newTimeOfDay = when (hour) {
            in 5..6 -> TimeOfDay.DAWN
            in 7..8 -> TimeOfDay.MORNING
            in 9..11, in 14..17 -> TimeOfDay.WORK
            in 12..13 -> TimeOfDay.LUNCH
            in 18..21 -> TimeOfDay.EVENING
            in 22..23 -> TimeOfDay.NIGHT
            else -> TimeOfDay.MIDNIGHT
        }
        
        if (newTimeOfDay != currentTimeOfDay) {
            currentTimeOfDay = newTimeOfDay
            notifyStateChanged()
            Log.d(TAG, "Time of day changed to: $currentTimeOfDay")
        }
    }

    /**
     * 更新季节
     */
    private fun updateSeason() {
        val month = Calendar.getInstance().get(Calendar.MONTH) + 1
        currentSeason = when (month) {
            in 3..5 -> Season.SPRING
            in 6..8 -> Season.SUMMER
            in 9..11 -> Season.AUTUMN
            else -> Season.WINTER
        }
    }

    /**
     * 检查是否是节日
     */
    private fun checkHoliday() {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        holidayType = when {
            month == 1 && day == 1 -> HolidayType.NEW_YEAR
            month == 2 && day == 14 -> HolidayType.VALENTINE
            month == 5 && day == 1 -> HolidayType.LABOR_DAY
            month == 10 && day == 1 -> HolidayType.NATIONAL_DAY
            month == 12 && day == 25 -> HolidayType.CHRISTMAS
            // 农历节日需要更复杂的计算，这里简化处理
            month == 1 && day in 20..31 -> HolidayType.SPRING_FESTIVAL // 简化
            month == 6 && day in 5..15 -> HolidayType.DRAGON_BOAT // 简化
            month == 9 && day in 10..20 -> HolidayType.MID_AUTUMN // 简化
            else -> null
        }
        
        isHoliday = holidayType != null
        if (isHoliday) {
            Log.d(TAG, "Holiday detected: $holidayType")
        }
    }

    /**
     * 更新天气（简化版，实际应调用天气API）
     */
    private suspend fun updateWeather() {
        // 实际项目中应该调用天气API
        // 这里简化处理，根据季节随机
        currentWeather = when (currentSeason) {
            Season.SPRING -> if (Math.random() > 0.6) Weather.RAINY else Weather.SUNNY
            Season.SUMMER -> Weather.SUNNY
            Season.AUTUMN -> Weather.CLOUDY
            Season.WINTER -> if (Math.random() > 0.7) Weather.SNOWY else Weather.CLOUDY
        }
        notifyStateChanged()
    }

    /**
     * 获取当前生活状态
     */
    fun getCurrentState(): LifeState {
        return LifeState(
            timeOfDay = currentTimeOfDay,
            weather = currentWeather,
            season = currentSeason,
            isHoliday = isHoliday,
            holidayType = holidayType,
            greeting = generateGreeting(),
            accessory = determineAccessory(),
            mood = determineMood()
        )
    }

    /**
     * 生成问候语
     */
    private fun generateGreeting(): String {
        return when (currentTimeOfDay) {
            TimeOfDay.DAWN -> listOf("天亮了，新的一天开始了！", "早起的龙虾有虫吃~").random()
            TimeOfDay.MORNING -> listOf("早上好！今天也要元气满满！", "早安！记得吃早餐哦~").random()
            TimeOfDay.WORK -> listOf("加油工作！", "专注模式开启！", "努力工作，晚点摸鱼~").random()
            TimeOfDay.LUNCH -> listOf("午饭时间到！", "干饭时间！", "吃饱了才有力气干活~").random()
            TimeOfDay.EVENING -> listOf("下班啦！放松一下~", "晚上好！今天过得怎么样？").random()
            TimeOfDay.NIGHT -> listOf("该准备睡觉啦~", "熬夜伤身体，早点休息吧", "已经很晚了...").random()
            TimeOfDay.MIDNIGHT -> listOf("...zzz", "快去睡觉！", "你明天不用上班吗？").random()
        }
    }

    /**
     * 确定配饰
     */
    private fun determineAccessory(): AccessoryType {
        return when {
            isHoliday && holidayType == HolidayType.CHRISTMAS -> AccessoryType.SANTA_HAT
            isHoliday && holidayType == HolidayType.NEW_YEAR -> AccessoryType.PARTY_HAT
            isHoliday && holidayType == HolidayType.SPRING_FESTIVAL -> AccessoryType.RED_ENVELOPE
            isHoliday && holidayType == HolidayType.MID_AUTUMN -> AccessoryType.MOONCAKE
            currentWeather == Weather.SUNNY && currentSeason == Season.SUMMER -> AccessoryType.SUNGLASSES
            currentWeather == Weather.RAINY -> AccessoryType.UMBRELLA
            currentSeason == Season.WINTER || currentWeather == Weather.SNOWY -> AccessoryType.SCARF
            else -> AccessoryType.NONE
        }
    }

    /**
     * 确定心情
     */
    private fun determineMood(): Mood {
        return when (currentTimeOfDay) {
            TimeOfDay.DAWN, TimeOfDay.MORNING -> Mood.ENERGETIC
            TimeOfDay.NIGHT -> Mood.SLEEPY
            TimeOfDay.MIDNIGHT -> Mood.TIRED
            else -> when (currentWeather) {
                Weather.SUNNY -> Mood.HAPPY
                Weather.RAINY -> Mood.SAD
                else -> Mood.NORMAL
            }
        }
    }

    private fun notifyStateChanged() {
        onStateChanged?.invoke(getCurrentState())
    }

    /**
     * 主动提醒检查
     */
    fun shouldRemindWater(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        // 上午10点、下午3点提醒喝水
        return hour == 10 || hour == 15
    }

    fun shouldRemindSleep(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        // 晚上11点后提醒睡觉
        return hour >= 23
    }

    fun shouldRemindBreak(): Boolean {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        // 工作时段每小时的45分提醒休息
        return currentTimeOfDay == TimeOfDay.WORK && minute == 45
    }
}
