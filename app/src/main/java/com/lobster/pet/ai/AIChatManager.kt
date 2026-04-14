package com.lobster.pet.ai

import android.content.Context
import android.util.Log
import com.lobster.pet.data.db.AppDatabase
import com.lobster.pet.data.db.ChatContact
import com.lobster.pet.data.db.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * AI 代聊管理器
 * 
 * 使用智谱 AI GLM-4 API
 * 用户需要提供自己的 API Key
 */
class AIChatManager(private val context: Context) {
    
    private val db = AppDatabase.getInstance(context)
    private val TAG = "AIChatManager"
    
    // 默认人设
    private val defaultSystemPrompt = "你是用户的好朋友，回复简短自然，像日常微信聊天一样。不要长篇大论，保持轻松友好的语气。"
    
    /**
     * 处理收到的消息
     * @param contactId 联系人唯一标识
     * @param contactName 联系人显示名称
     * @param messageContent 收到的消息内容
     * @param apiKey 智谱 AI API Key
     * @return AI 生成的回复，如果该联系人未启用代聊则返回 null
     */
    suspend fun handleIncomingMessage(
        contactId: String,
        contactName: String,
        messageContent: String,
        apiKey: String
    ): String? = withContext(Dispatchers.IO) {
        // 1. 检查该联系人是否启用代聊
        val contact = db.chatContactDao().getById(contactId)
        if (contact == null || !contact.isEnabled) {
            Log.d(TAG, "Contact $contactName not enabled for AI chat")
            return@withContext null
        }
        
        // 2. 保存对方消息
        db.chatMessageDao().insert(
            ChatMessage(
                contactId = contactId,
                content = messageContent,
                isFromMe = false
            )
        )
        
        // 3. 获取最近10条消息作为上下文
        val recentMessages = db.chatMessageDao().getRecentMessages(contactId, 10)
        
        // 4. 调用大模型生成回复
        val reply = callLLM(contact, recentMessages, apiKey)
        
        // 5. 保存 AI 回复
        if (reply != null) {
            db.chatMessageDao().insert(
                ChatMessage(
                    contactId = contactId,
                    content = reply,
                    isFromMe = true
                )
            )
            // 清理旧消息，只保留最近10条
            db.chatMessageDao().trimOldMessages(contactId)
        }
        
        return@withContext reply
    }
    
    /**
     * 调用大模型 API
     */
    private suspend fun callLLM(
        contact: ChatContact,
        messages: List<ChatMessage>,
        apiKey: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = contact.systemPrompt.takeIf { it.isNotBlank() } ?: defaultSystemPrompt
            
            // 组装 messages 数组
            val messagesArray = mutableListOf<Map<String, String>>()
            messagesArray.add(mapOf("role" to "system", "content" to systemPrompt))
            
            // 按时间顺序添加历史消息（从旧到新）
            messages.sortedBy { it.timestamp }.forEach { msg ->
                val role = if (msg.isFromMe) "assistant" else "user"
                messagesArray.add(mapOf("role" to role, "content" to msg.content))
            }
            
            // 构建请求体
            val requestBody = JSONObject().apply {
                put("model", "glm-4")
                put("messages", org.json.JSONArray(messagesArray))
                put("temperature", 0.7)
                put("max_tokens", 150)  // 简短回复
            }
            
            // 发送请求
            val url = URL("https://open.bigmodel.cn/api/paas/v4/chat/completions")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 30000
            }
            
            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray())
            }
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                val choices = jsonResponse.getJSONArray("choices")
                if (choices.length() > 0) {
                    return@withContext choices.getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim()
                }
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e(TAG, "API error: $responseCode, $error")
            }
            
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call LLM", e)
            return@withContext null
        }
    }
    
    /**
     * 添加/更新联系人
     */
    suspend fun saveContact(contact: ChatContact) {
        db.chatContactDao().insert(contact)
    }
    
    /**
     * 启用/禁用代聊
     */
    suspend fun toggleContactEnabled(contactId: String, enabled: Boolean) {
        val contact = db.chatContactDao().getById(contactId)
        contact?.let {
            db.chatContactDao().update(it.copy(isEnabled = enabled))
        }
    }
    
    /**
     * 获取所有联系人
     */
    fun getAllContacts() = db.chatContactDao().getAll()
    
    /**
     * 删除联系人
     */
    suspend fun deleteContact(contact: ChatContact) {
        db.chatMessageDao().clearMessages(contact.contactId)
        db.chatContactDao().delete(contact)
    }
}
