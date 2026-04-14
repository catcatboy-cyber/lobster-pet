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

    enum class State {
        NORMAL, LISTENING, EATING, HUNGRY, SAD
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var scale = 1f
    private var currentState = State.NORMAL
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
        currentState = State.LISTENING
        invalidate()
    }

    fun showNormal() {
        currentState = State.NORMAL
        invalidate()
    }

    fun showEating() {
        currentState = State.EATING
        invalidate()
        // 2秒后恢复正常
        postDelayed({ showNormal() }, 2000)
    }

    fun showHungry() {
        currentState = State.HUNGRY
        invalidate()
    }

    fun showSad() {
        currentState = State.SAD
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        // 根据状态选择颜色
        val bodyColor = when (currentState) {
            State.LISTENING -> Color.parseColor("#FF6B6B")
            State.EATING -> Color.parseColor("#FFA07A")
            State.HUNGRY -> Color.parseColor("#D35400")
            State.SAD -> Color.parseColor("#7F8C8D")
            else -> Color.parseColor("#E74C3C")
        }

        // 绘制龙虾身体
        paint.color = bodyColor
        paint.style = Paint.Style.FILL

        // 身体
        canvas.drawOval(centerX - 40f, centerY - 20f, centerX + 40f, centerY + 30f, paint)

        // 头部
        val headColor = when (currentState) {
            State.LISTENING -> Color.parseColor("#FF8585")
            State.EATING -> Color.parseColor("#FFB6C1")
            State.HUNGRY -> Color.parseColor("#BA4A00")
            State.SAD -> Color.parseColor("#616A6B")
            else -> Color.parseColor("#C0392B")
        }
        paint.color = headColor
        canvas.drawCircle(centerX, centerY - 35f, 35f, paint)

        // 眼睛
        paint.color = Color.WHITE
        canvas.drawCircle(centerX - 12f, centerY - 45f, 8f, paint)
        canvas.drawCircle(centerX + 12f, centerY - 45f, 8f, paint)

        paint.color = Color.BLACK
        canvas.drawCircle(centerX - 12f, centerY - 45f, 4f, paint)
        canvas.drawCircle(centerX + 12f, centerY - 45f, 4f, paint)

        // 根据状态绘制表情
        when (currentState) {
            State.SAD, State.HUNGRY -> drawSadMouth(canvas, centerX, centerY)
            State.EATING -> drawEatingMouth(canvas, centerX, centerY)
            else -> drawNormalMouth(canvas, centerX, centerY)
        }

        // 钳子动画
        val waveOffset = Math.sin(Math.toRadians(wavePhase.toDouble())).toFloat() * 10f
        val eatingOffset = if (currentState == State.EATING) {
            Math.sin(Math.toRadians(wavePhase * 3.toDouble())).toFloat() * 20f
        } else 0f

        paint.color = bodyColor

        // 左钳
        canvas.save()
        val leftClawAngle = when (currentState) {
            State.EATING -> -20f + eatingOffset
            State.SAD, State.HUNGRY -> -10f
            else -> -20f + waveOffset
        }
        canvas.rotate(leftClawAngle, centerX - 35f, centerY - 20f)
        drawClaw(canvas, centerX - 35f, centerY - 20f, -1f)
        canvas.restore()

        // 右钳
        canvas.save()
        val rightClawAngle = when (currentState) {
            State.EATING -> 20f - eatingOffset
            State.SAD, State.HUNGRY -> 10f
            else -> 20f - waveOffset
        }
        canvas.rotate(rightClawAngle, centerX + 35f, centerY - 20f)
        drawClaw(canvas, centerX + 35f, centerY - 20f, 1f)
        canvas.restore()

        // 尾巴
        drawTail(canvas, centerX, centerY + 30f)

        // 特效
        when (currentState) {
            State.LISTENING -> drawWaveEffect(canvas, centerX, centerY)
            State.EATING -> drawFoodParticle(canvas, centerX, centerY)
            State.HUNGRY -> drawHungrySymbol(canvas, centerX, centerY - 60f)
            State.SAD -> drawTear(canvas, centerX, centerY - 35f)
            else -> {}
        }
    }

    private fun drawNormalMouth(canvas: Canvas, x: Float, y: Float) {
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawArc(x - 15f, y - 35f, x + 15f, y - 25f, 0f, 180f, false, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawSadMouth(canvas: Canvas, x: Float, y: Float) {
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawArc(x - 15f, y - 40f, x + 15f, y - 30f, 0f, -180f, false, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawEatingMouth(canvas: Canvas, x: Float, y: Float) {
        paint.color = Color.parseColor("#8B4513")
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x, y - 30f, 8f, paint)
    }

    private fun drawHungrySymbol(canvas: Canvas, x: Float, y: Float) {
        paint.color = Color.parseColor("#F39C12")
        paint.textSize = 30f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("🍤", x, y, paint)
    }

    private fun drawTear(canvas: Canvas, x: Float, y: Float) {
        paint.color = Color.parseColor("#3498DB")
        val tearOffset = (System.currentTimeMillis() % 1000) / 1000f * 20f
        canvas.drawCircle(x + 15f, y + 10f + tearOffset, 3f, paint)
    }

    private fun drawFoodParticle(canvas: Canvas, x: Float, y: Float) {
        paint.color = Color.parseColor("#F39C12")
        val particles = listOf(
            Pair(-20f, -50f), Pair(0f, -60f), Pair(20f, -50f),
            Pair(-10f, -40f), Pair(10f, -40f)
        )
        particles.forEach { (px, py) ->
            canvas.drawCircle(x + px, y + py, 5f, paint)
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
