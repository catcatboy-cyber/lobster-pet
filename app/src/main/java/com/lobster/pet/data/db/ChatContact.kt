package com.lobster.pet.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 代聊联系人配置
 */
@Entity(tableName = "chat_contacts")
data class ChatContact(
    @PrimaryKey
    val contactId: String,          // 唯一标识：昵称+备注+头像hash
    val contactName: String,        // 显示名称
    val isEnabled: Boolean = false, // 是否启用代聊
    val systemPrompt: String = "",  // AI 人设
    val createdAt: Long = System.currentTimeMillis()
)
