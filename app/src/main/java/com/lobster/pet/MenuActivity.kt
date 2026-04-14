package com.lobster.pet

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.lobster.pet.service.FloatingLobsterService

/**
 * 龙虾菜单 - 点击龙虾后弹出
 */
class MenuActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置窗口为悬浮对话框样式
        window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG)
        window.setGravity(Gravity.CENTER)
        
        // 获取状态
        val hunger = intent.getIntExtra("hunger", 50)
        val happiness = intent.getIntExtra("happiness", 70)
        
        // 创建界面
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
        }
        
        // 标题
        val title = TextView(this).apply {
            text = "🦞 龙虾菜单"
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        layout.addView(title)
        
        // 状态显示
        val status = TextView(this).apply {
            text = getStatusText(hunger, happiness)
            textSize = 14f
            setPadding(0, 0, 0, 30)
        }
        layout.addView(status)
        
        // 语音指令按钮
        val btnVoice = Button(this).apply {
            text = "🎤 语音指令"
            setOnClickListener {
                FloatingLobsterService.instance?.startVoiceCommand()
                finish()
            }
        }
        layout.addView(btnVoice)
        
        // 喂食按钮
        val btnFeed = Button(this).apply {
            text = "🍤 喂食"
            setOnClickListener {
                FloatingLobsterService.instance?.feed()
                finish()
            }
        }
        layout.addView(btnFeed)
        
        // 退出按钮
        val btnQuit = Button(this).apply {
            text = "👋 退出龙虾"
            setOnClickListener {
                FloatingLobsterService.instance?.quit()
                finish()
            }
        }
        layout.addView(btnQuit)
        
        // 取消按钮
        val btnCancel = Button(this).apply {
            text = "返回"
            setOnClickListener {
                finish()
            }
        }
        layout.addView(btnCancel)
        
        setContentView(layout)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 通知服务菜单已关闭
        FloatingLobsterService.instance?.onMenuClosed()
    }
    
    private fun getStatusText(hunger: Int, happiness: Int): String {
        val hungerText = when {
            hunger > 80 -> "饱饱的 😊"
            hunger > 50 -> "还行吧 😐"
            hunger > 20 -> "有点饿 😟"
            else -> "快饿死了 😫"
        }
        
        val happyText = when {
            happiness > 80 -> "超开心 🥰"
            happiness > 50 -> "还不错 🙂"
            happiness > 20 -> "有点闷 😕"
            else -> "很难过 😢"
        }
        
        return "饱食度: $hungerText\n心情: $happyText"
    }
}
