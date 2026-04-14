package com.lobster.pet.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.lobster.pet.ai.AIChatManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 辅助功能服务 - 用于自动输入文字、读取微信消息、AI 代聊
 */
class LobsterAccessibilityService : AccessibilityService() {
    
    private val TAG = "LobsterAccessibility"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var aiChatManager: AIChatManager? = null
    private var currentContactName: String? = null
    private var currentContactId: String? = null
    private var lastProcessedMessage: String? = null
    private var isProcessing = false
    
    // 微信包名
    private val WECHAT_PACKAGE = "com.tencent.mm"
    
    override fun onCreate() {
        super.onCreate()
        aiChatManager = AIChatManager(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // 只处理微信
        if (event.packageName?.toString() != WECHAT_PACKAGE) return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // 检查当前是否在聊天界面
                checkChatWindow()
            }
        }
    }
    
    /**
     * 检查当前是否在微信聊天界面
     */
    private fun checkChatWindow() {
        val rootNode = rootInActiveWindow ?: return
        
        try {
            // 获取窗口标题（聊天对象名称）
            val windowTitle = rootNode.findAccessibilityNodeInfosByViewId(
                "$WECHAT_PACKAGE:id/title"
            ).firstOrNull()?.text?.toString()
            
            if (windowTitle != null && windowTitle != currentContactName) {
                // 切换到新的聊天对象
                currentContactName = windowTitle
                currentContactId = generateContactId(windowTitle, rootNode)
                Log.d(TAG, "Switched to chat with: $windowTitle, id: $currentContactId")
            }
            
            // 如果当前在聊天界面，检查新消息
            if (currentContactName != null && !isProcessing) {
                checkNewMessages(rootNode)
            }
        } finally {
            rootNode.recycle()
        }
    }
    
    /**
     * 检查并处理新消息
     */
    private fun checkNewMessages(rootNode: AccessibilityNodeInfo) {
        // 获取消息列表
        val messageNodes = rootNode.findAccessibilityNodeInfosByViewId(
            "$WECHAT_PACKAGE:id/chatting_item_content_cvs"
        )
        
        if (messageNodes.isEmpty()) return
        
        // 找到最后一条对方发来的消息
        val lastMessage = findLastIncomingMessage(messageNodes)
        messageNodes.forEach { it.recycle() }
        
        if (lastMessage != null && lastMessage != lastProcessedMessage) {
            lastProcessedMessage = lastMessage
            Log.d(TAG, "New message from $currentContactName: $lastMessage")
            
            // 触发 AI 回复
            handleAIReply(lastMessage)
        }
    }
    
    /**
     * 找到最后一条对方发来的文字消息
     */
    private fun findLastIncomingMessage(messageNodes: List<AccessibilityNodeInfo>): String? {
        // 遍历消息节点，找到最后一条不是"我"发的消息
        for (i in messageNodes.size - 1 downTo 0) {
            val node = messageNodes[i]
            
            // 检查是否是对方消息（通过判断是否有特定属性或位置）
            // 微信中，对方消息在左边，我的消息在右边
            // 这里简化处理，假设能获取到消息内容的就是有效消息
            
            val content = extractMessageContent(node)
            if (content != null && !isMyMessage(node)) {
                return content
            }
        }
        return null
    }
    
    /**
     * 提取消息内容
     */
    private fun extractMessageContent(node: AccessibilityNodeInfo): String? {
        // 尝试不同的 ID 获取消息文本
        val textIds = arrayOf(
            "chatting_content_itv",  // 文字消息
            "record_downloading_tv", // 语音转文字
            "msg_content"            // 通用内容
        )
        
        for (id in textIds) {
            val textNode = node.findAccessibilityNodeInfosByViewId(
                "$WECHAT_PACKAGE:id/$id"
            ).firstOrNull()
            
            if (textNode != null) {
                val text = textNode.text?.toString()
                textNode.recycle()
                if (!text.isNullOrBlank()) {
                    return text
                }
            }
        }
        
        // 如果找不到特定 ID，尝试直接获取节点的 text
        return node.text?.toString()?.takeIf { it.isNotBlank() }
    }
    
    /**
     * 判断是否是我发的消息
     * 通过检查消息是否有"已发送"标记或头像位置判断
     */
    private fun isMyMessage(node: AccessibilityNodeInfo): Boolean {
        // 简化实现：检查消息节点是否有"已读"、"发送中"等标记
        val statusNodes = node.findAccessibilityNodeInfosByViewId(
            "$WECHAT_PACKAGE:id/chatting_status_tv"
        )
        val isMine = statusNodes.isNotEmpty()
        statusNodes.forEach { it.recycle() }
        return isMine
    }
    
    /**
     * 处理 AI 回复
     */
    private fun handleAIReply(message: String) {
        val contactId = currentContactId ?: return
        val contactName = currentContactName ?: return
        val manager = aiChatManager ?: return
        
        // 从 SharedPreferences 读取 API Key
        val prefs = getSharedPreferences("lobster_prefs", MODE_PRIVATE)
        val apiKey = prefs.getString("glm_api_key", null)
        
        if (apiKey.isNullOrBlank()) {
            Log.w(TAG, "API Key not set")
            return
        }
        
        isProcessing = true
        
        serviceScope.launch {
            try {
                // 延迟一下，模拟人类阅读时间
                delay(2000)
                
                // 调用 AI 生成回复
                val reply = manager.handleIncomingMessage(
                    contactId = contactId,
                    contactName = contactName,
                    messageContent = message,
                    apiKey = apiKey
                )
                
                if (reply != null) {
                    Log.d(TAG, "AI reply: $reply")
                    
                    // 再延迟一下，模拟打字思考
                    delay(1000)
                    
                    // 自动输入并发送
                    sendReply(reply)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling AI reply", e)
            } finally {
                isProcessing = false
            }
        }
    }
    
    /**
     * 发送回复消息
     */
    private suspend fun sendReply(text: String) {
        // 1. 找到输入框并输入文字
        val rootNode = rootInActiveWindow ?: return
        
        try {
            // 查找输入框
            val inputIds = arrayOf(
                "chatting_content_et",  // 主输入框
                "input"                 // 备用
            )
            
            var inputNode: AccessibilityNodeInfo? = null
            for (id in inputIds) {
                inputNode = rootNode.findAccessibilityNodeInfosByViewId(
                    "$WECHAT_PACKAGE:id/$id"
                ).firstOrNull()
                if (inputNode != null) break
            }
            
            if (inputNode == null) {
                Log.w(TAG, "Input field not found")
                return
            }
            
            // 输入文字
            val arguments = Bundle()
            arguments.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )
            val success = inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            inputNode.recycle()
            
            if (!success) {
                Log.w(TAG, "Failed to input text")
                return
            }
            
            // 2. 点击发送按钮
            delay(500)
            
            val sendIds = arrayOf(
                "chatting_send_btn",  // 发送按钮
                "send"                // 备用
            )
            
            val rootNode2 = rootInActiveWindow ?: return
            var sendNode: AccessibilityNodeInfo? = null
            
            try {
                for (id in sendIds) {
                    sendNode = rootNode2.findAccessibilityNodeInfosByViewId(
                        "$WECHAT_PACKAGE:id/$id"
                    ).firstOrNull()
                    if (sendNode != null) break
                }
                
                if (sendNode != null) {
                    sendNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    sendNode.recycle()
                    Log.d(TAG, "Message sent")
                } else {
                    // 如果找不到发送按钮，尝试点击屏幕右下角
                    val metrics = resources.displayMetrics
                    click(metrics.widthPixels * 0.9f, metrics.heightPixels * 0.9f)
                }
            } finally {
                rootNode2.recycle()
            }
            
        } finally {
            rootNode.recycle()
        }
    }
    
    /**
     * 生成联系人唯一 ID
     */
    private fun generateContactId(name: String, rootNode: AccessibilityNodeInfo): String {
        // 简化实现：使用名称哈希
        // 后续可以加上备注名、头像特征等
        return "wechat_${name.hashCode()}"
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Service connected")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        Log.d(TAG, "Service unbound")
        return super.onUnbind(intent)
    }

    companion object {
        private var instance: LobsterAccessibilityService? = null

        fun getInstance(): LobsterAccessibilityService? = instance

        /**
         * 在当前输入框中输入文字（供外部调用）
         */
        fun inputText(text: String): Boolean {
            val service = instance ?: return false
            val rootNode = service.rootInActiveWindow ?: return false

            return try {
                val focusedNode = findFocusedEditText(rootNode)
                if (focusedNode != null) {
                    val arguments = Bundle()
                    arguments.putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        text
                    )
                    val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                    focusedNode.recycle()
                    result
                } else {
                    false
                }
            } finally {
                rootNode.recycle()
            }
        }

        private fun findFocusedEditText(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            val stack = ArrayDeque<AccessibilityNodeInfo>()
            stack.add(rootNode)

            while (stack.isNotEmpty()) {
                val node = stack.removeLast()

                if (node.isFocused && node.isEditable) {
                    return node
                }

                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let { child ->
                        stack.add(child)
                    }
                }

                if (node !== rootNode) {
                    node.recycle()
                }
            }
            return null
        }

        /**
         * 点击屏幕指定位置
         */
        fun click(x: Float, y: Float): Boolean {
            val service = instance ?: return false

            val path = Path().apply {
                moveTo(x, y)
            }

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                .build()

            return service.dispatchGesture(gesture, null, null)
        }
    }
}
