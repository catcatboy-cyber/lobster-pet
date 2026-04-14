package com.lobster.pet.voice

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.lobster.pet.service.LobsterAccessibilityService

/**
 * 指令处理器
 */
class CommandProcessor(private val context: Context) {

    private val socialApps = mapOf(
        "微信" to "com.tencent.mm",
        "QQ" to "com.tencent.mobileqq",
        "微博" to "com.sina.weibo",
        "钉钉" to "com.alibaba.android.rimet",
        "飞书" to "com.ss.android.lark"
    )

    fun processCommand(command: String) {
        val lowerCmd = command.lowercase()

        when {
            // 打开APP
            lowerCmd.contains("打开") -> {
                val appName = socialApps.keys.find { command.contains(it) }
                if (appName != null) {
                    openApp(appName)
                } else {
                    showToast("暂不支持打开这个APP")
                }
            }

            // 发送文字
            lowerCmd.contains("发送") || lowerCmd.contains("输入") -> {
                val text = command.replace("发送", "")
                    .replace("输入", "")
                    .trim()
                if (text.isNotEmpty()) {
                    inputText(text)
                } else {
                    showToast("要发送什么内容呢？")
                }
            }

            // 隐藏龙虾
            lowerCmd.contains("退下") || lowerCmd.contains("消失") -> {
                showToast("龙虾退下了，点击通知栏可以唤回")
                // 可以发送广播让服务隐藏龙虾
            }

            // 召唤龙虾
            lowerCmd.contains("过来") || lowerCmd.contains("龙虾") -> {
                showToast("龙虾来啦！")
            }

            // 打招呼
            lowerCmd.contains("你好") -> {
                showToast("你好呀！我是你的语音龙虾助手🦞")
            }

            else -> {
                showToast("指令: $command")
            }
        }
    }

    private fun openApp(appName: String) {
        val packageName = socialApps[appName] ?: return
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            showToast("正在打开 $appName")
        } else {
            showToast("未安装 $appName")
        }
    }

    private fun inputText(text: String) {
        val success = LobsterAccessibilityService.inputText(text)
        if (success) {
            showToast("已输入: $text")
        } else {
            showToast("输入失败，请确保已开启辅助功能权限")
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}
