package com.lobster.pet.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 聊天记录（最近10条）
 */
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactId: String,      // 关联 ChatContact
    val content: String,        // 消息内容
    val isFromMe: Boolean,      // 是否我发送的
    val timestamp: Long = System.currentTimeMillis()
)
