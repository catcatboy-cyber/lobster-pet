package com.lobster.pet.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.animation.doOnEnd

/**
 * 龙虾说话气泡
 * 显示在龙虾上方的对话气泡
 */
class LobsterBubbleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val textView: TextView
    private var autoHideRunnable: Runnable? = null
    
    init {
        // 设置气泡背景
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 32f
            setColor(Color.WHITE)
            setStroke(4, Color.parseColor("#E74C3C"))
        }
        background = drawable
        
        // 添加阴影
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            elevation = 12f
        }
        
        // 内边距
        setPadding(32, 24, 32, 32)
        
        // 创建TextView
        textView = TextView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
            setTextColor(Color.parseColor("#2C3E50"))
            textSize = 14f
            maxWidth = (context.resources.displayMetrics.widthPixels * 0.6).toInt()
            gravity = Gravity.CENTER
        }
        addView(textView)
        
        // 初始不可见
        alpha = 0f
        visibility = View.GONE
    }

    /**
     * 显示消息
     * @param message 消息内容
     * @param duration 显示时长（毫秒），0表示不自动隐藏
     */
    fun show(message: String, duration: Long = 5000) {
        // 取消之前的隐藏任务
        autoHideRunnable?.let { removeCallbacks(it) }
        
        textView.text = message
        visibility = View.VISIBLE
        
        // 动画显示
        animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .setInterpolator(android.view.animation.OvershootInterpolator())
            .start()
        
        // 自动隐藏
        if (duration > 0) {
            autoHideRunnable = Runnable { hide() }
            postDelayed(autoHideRunnable, duration)
        }
    }

    /**
     * 隐藏气泡
     */
    fun hide() {
        autoHideRunnable?.let { removeCallbacks(it) }
        autoHideRunnable = null
        
        animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(200)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                }
            })
            .start()
    }

    /**
     * 打字机效果显示
     */
    fun showWithTyping(message: String, duration: Long = 5000) {
        visibility = View.VISIBLE
        alpha = 1f
        textView.text = ""
        
        var index = 0
        val typingRunnable = object : Runnable {
            override fun run() {
                if (index <= message.length) {
                    textView.text = message.substring(0, index)
                    index++
                    postDelayed(this, 50)
                } else {
                    if (duration > 0) {
                        autoHideRunnable = Runnable { hide() }
                        postDelayed(autoHideRunnable, duration)
                    }
                }
            }
        }
        post(typingRunnable)
    }

    /**
     * 快速隐藏（无动画）
     */
    fun hideImmediately() {
        autoHideRunnable?.let { removeCallbacks(it) }
        autoHideRunnable = null
        visibility = View.GONE
        alpha = 0f
    }

    companion object {
        /**
         * 创建并添加到父布局
         */
        fun showAboveLobster(
            parent: ViewGroup,
            lobsterX: Int,
            lobsterY: Int,
            message: String,
            duration: Long = 5000
        ): LobsterBubbleView {
            val bubble = LobsterBubbleView(parent.context)
            
            // 计算位置（龙虾上方居中）
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                // 在龙虾上方
                val bubbleWidth = (parent.context.resources.displayMetrics.widthPixels * 0.6).toInt()
                leftMargin = (lobsterX - bubbleWidth / 2 + 60).coerceAtLeast(20)
                topMargin = (lobsterY - 150).coerceAtLeast(20)
            }
            
            parent.addView(bubble, params)
            bubble.show(message, duration)
            
            // 动画结束后自动移除
            bubble.postDelayed({
                if (bubble.parent != null) {
                    parent.removeView(bubble)
                }
            }, duration + 1000)
            
            return bubble
        }
    }
}
