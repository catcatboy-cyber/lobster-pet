package com.lobster.pet.data.db

import androidx.room.*

/**
 * 消息数据访问对象
 */
@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE contactId = :contactId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(contactId: String, limit: Int = 10): List<ChatMessage>
    
    @Insert
    suspend fun insert(message: ChatMessage)
    
    @Query("DELETE FROM chat_messages WHERE contactId = :contactId AND id NOT IN (SELECT id FROM chat_messages WHERE contactId = :contactId ORDER BY timestamp DESC LIMIT 10)")
    suspend fun trimOldMessages(contactId: String)
    
    @Query("DELETE FROM chat_messages WHERE contactId = :contactId")
    suspend fun clearMessages(contactId: String)
}
