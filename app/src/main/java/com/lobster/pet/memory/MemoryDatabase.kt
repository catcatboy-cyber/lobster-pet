package com.lobster.pet.memory

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * 记忆数据库 - 存储用户相关信息
 */
@Database(entities = [UserMemory::class, UserPreference::class, ChatHistory::class, ImportantDate::class], version = 1)
abstract class MemoryDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: MemoryDatabase? = null

        fun getInstance(context: Context): MemoryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MemoryDatabase::class.java,
                    "memory.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

/**
 * 用户记忆实体
 */
@Entity(tableName = "user_memories")
data class UserMemory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: MemoryType,
    val content: String,
    val context: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 用户偏好设置
 */
@Entity(tableName = "user_preferences")
data class UserPreference(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 聊天记录摘要
 */
@Entity(tableName = "chat_history")
data class ChatHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topic: String,
    val summary: String,
    val userMood: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 重要日期
 */
@Entity(tableName = "important_dates")
data class ImportantDate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: Long,
    val type: DateType,
    val isRecurring: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class MemoryType {
    LIKES,          // 喜欢的东西
    DISLIKES,       // 讨厌的东西
    HABIT,          // 习惯
    WORK,           // 工作相关信息
    FAMILY,         // 家庭信息
    FRIEND,         // 朋友信息
    MOOD,           // 情绪记录
    GOAL,           // 目标
    RANDOM          // 其他
}

enum class DateType {
    BIRTHDAY,       // 生日
    ANNIVERSARY,    // 纪念日
    DEADLINE,       // 截止日期
    EVENT,          // 事件
    CUSTOM          // 自定义
}

@Dao
interface MemoryDao {
    // 记忆相关
    @Insert
    suspend fun insertMemory(memory: UserMemory): Long

    @Query("SELECT * FROM user_memories WHERE type = :type ORDER BY createdAt DESC")
    fun getMemoriesByType(type: MemoryType): Flow<List<UserMemory>>

    @Query("SELECT * FROM user_memories ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentMemories(limit: Int = 10): List<UserMemory>

    @Query("DELETE FROM user_memories WHERE id = :id")
    suspend fun deleteMemory(id: Long)

    @Query("SELECT * FROM user_memories WHERE content LIKE '%' || :keyword || '%'")
    suspend fun searchMemories(keyword: String): List<UserMemory>

    // 偏好相关
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreference(pref: UserPreference)

    @Query("SELECT * FROM user_preferences WHERE key = :key")
    suspend fun getPreference(key: String): UserPreference?

    @Query("SELECT * FROM user_preferences")
    fun getAllPreferences(): Flow<List<UserPreference>>

    // 聊天记录
    @Insert
    suspend fun insertChatHistory(chat: ChatHistory): Long

    @Query("SELECT * FROM chat_history ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentChats(limit: Int = 5): List<ChatHistory>

    // 重要日期
    @Insert
    suspend fun insertImportantDate(date: ImportantDate): Long

    @Query("SELECT * FROM important_dates WHERE date BETWEEN :start AND :end")
    suspend fun getDatesInRange(start: Long, end: Long): List<ImportantDate>

    @Query("SELECT * FROM important_dates ORDER BY date ASC")
    fun getAllImportantDates(): Flow<List<ImportantDate>>

    @Query("DELETE FROM important_dates WHERE id = :id")
    suspend fun deleteImportantDate(id: Long)
}
