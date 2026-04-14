package com.lobster.pet.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.lobster.pet.voice.CommandProcessor

/**
 * 辅助功能服务 - 用于自动输入文字和操作界面
 */
class LobsterAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 监听窗口变化（可选）
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    companion object {
        private var instance: LobsterAccessibilityService? = null

        fun getInstance(): LobsterAccessibilityService? = instance

        /**
         * 在当前输入框中输入文字
         */
        fun inputText(text: String): Boolean {
            val service = instance ?: return false
            val rootNode = service.rootInActiveWindow ?: return false

            // 查找当前聚焦的输入框
            val focusedNode = findFocusedEditText(rootNode)
            return if (focusedNode != null) {
                val arguments = Bundle()
                arguments.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text
                )
                focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            } else {
                false
            }
        }

        /**
         * 查找当前聚焦的输入框
         */
        private fun findFocusedEditText(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (rootNode.isFocused && rootNode.isEditable) {
                return rootNode
            }

            for (i in 0 until rootNode.childCount) {
                val child = rootNode.getChild(i) ?: continue
                val result = findFocusedEditText(child)
                if (result != null) return result
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
