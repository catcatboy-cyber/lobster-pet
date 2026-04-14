package com.lobster.pet.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * 龙虾动画View
 * 使用简单绘制实现龙虾外观
 */
class LobsterView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var scale = 1f
    private var isListening = false
    private var wavePhase = 0f

    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 2000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            wavePhase = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        animator.start()
    }

    fun showListening() {
        isListening = true
        invalidate()
    }

    fun showNormal() {
        isListening = false
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        // 绘制龙虾身体（简化的红色椭圆）
        paint.color = if (isListening) Color.parseColor("#FF6B6B") else Color.parseColor("#E74C3C")
        paint.style = Paint.Style.FILL

        // 身体
        canvas.drawOval(centerX - 40f, centerY - 20f, centerX + 40f, centerY + 30f, paint)

        // 头部
        paint.color = if (isListening) Color.parseColor("#FF8585") else Color.parseColor("#C0392B")
        canvas.drawCircle(centerX, centerY - 35f, 35f, paint)

        // 眼睛
        paint.color = Color.WHITE
        canvas.drawCircle(centerX - 12f, centerY - 45f, 8f, paint)
        canvas.drawCircle(centerX + 12f, centerY - 45f, 8f, paint)

        paint.color = Color.BLACK
        canvas.drawCircle(centerX - 12f, centerY - 45f, 4f, paint)
        canvas.drawCircle(centerX + 12f, centerY - 45f, 4f, paint)

        // 钳子（带简单动画）
        val waveOffset = Math.sin(Math.toRadians(wavePhase.toDouble())).toFloat() * 10f

        paint.color = if (isListening) Color.parseColor("#FF6B6B") else Color.parseColor("#E74C3C")

        // 左钳
        canvas.save()
        canvas.rotate(-20f + waveOffset, centerX - 35f, centerY - 20f)
        drawClaw(canvas, centerX - 35f, centerY - 20f, -1f)
        canvas.restore()

        // 右钳
        canvas.save()
        canvas.rotate(20f - waveOffset, centerX + 35f, centerY - 20f)
        drawClaw(canvas, centerX + 35f, centerY - 20f, 1f)
        canvas.restore()

        // 尾巴
        drawTail(canvas, centerX, centerY + 30f)

        // 如果正在听，显示声波效果
        if (isListening) {
            drawWaveEffect(canvas, centerX, centerY)
        }
    }

    private fun drawClaw(canvas: Canvas, x: Float, y: Float, direction: Float) {
        val path = Path()
        path.moveTo(x, y)
        path.lineTo(x + direction * 30f, y - 20f)
        path.lineTo(x + direction * 40f, y - 10f)
        path.lineTo(x + direction * 25f, y)
        path.close()
        canvas.drawPath(path, paint)

        paint.color = Color.parseColor("#C0392B")
        canvas.drawCircle(x + direction * 35f, y - 15f, 10f, paint)
    }

    private fun drawTail(canvas: Canvas, x: Float, y: Float) {
        paint.color = Color.parseColor("#C0392B")
        val waveOffset = Math.sin(Math.toRadians(wavePhase.toDouble())).toFloat() * 5f

        for (i in 0..3) {
            val tailY = y + i * 15f
            val tailX = x + waveOffset * (i + 1) * 0.3f
            canvas.drawCircle(tailX, tailY, 12f - i * 2f, paint)
        }
    }

    private fun drawWaveEffect(canvas: Canvas, x: Float, y: Float) {
        paint.color = Color.parseColor("#4ECDC4")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f

        val radius = 50f + (wavePhase % 60f)
        paint.alpha = (255 * (1 - radius / 110f)).toInt()
        canvas.drawCircle(x, y, radius, paint)

        val radius2 = 70f + (wavePhase % 60f)
        paint.alpha = (255 * (1 - radius2 / 130f)).toInt()
        canvas.drawCircle(x, y, radius2, paint)

        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }
}
