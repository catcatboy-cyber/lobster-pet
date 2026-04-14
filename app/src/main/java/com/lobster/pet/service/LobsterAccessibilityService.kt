package com.lobster.pet.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 辅助功能服务 - 用于自动输入文字和操作界面
 */
class LobsterAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不监听事件，避免性能问题
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

            return try {
                // 查找当前聚焦的输入框
                val focusedNode = findFocusedEditText(rootNode)
                if (focusedNode != null) {
                    val arguments = Bundle()
                    arguments.putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        text
                    )
                    val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                    focusedNode.recycle() // 回收节点
                    result
                } else {
                    false
                }
            } finally {
                rootNode.recycle() // 回收根节点
            }
        }

        /**
         * 查找当前聚焦的输入框 - 使用栈而非递归避免深度问题
         */
        private fun findFocusedEditText(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            val stack = ArrayDeque<AccessibilityNodeInfo>()
            stack.add(rootNode)

            while (stack.isNotEmpty()) {
                val node = stack.removeLast()

                if (node.isFocused && node.isEditable) {
                    return node
                }

                // 添加子节点到栈
                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let { child ->
                        stack.add(child)
                    }
                }

                // 如果当前节点不是我们要找的，回收它
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
