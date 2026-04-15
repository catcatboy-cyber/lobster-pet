package com.lobster.pet.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.view.animation.LinearInterpolator
import com.lobster.pet.lifecycle.LifeSyncManager

/**
 * 龙虾动画View
 * 使用简单绘制实现龙虾外观
 * 支持时间、天气、节日配饰
 */
class LobsterView(context: Context) : View(context) {

    enum class State {
        NORMAL, LISTENING, EATING, HUNGRY, SAD, SLEEPY
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var scale = 1f
    private var currentState = State.NORMAL
    private var wavePhase = 0f
    
    // 生活同步配饰
    private var currentAccessory: LifeSyncManager.AccessoryType = LifeSyncManager.AccessoryType.NONE
    private var accessoryAlpha = 255

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
    
    fun showSleepy() {
        currentState = State.SLEEPY
        invalidate()
    }
    
    /**
     * 设置配饰
     */
    fun setAccessory(accessory: LifeSyncManager.AccessoryType) {
        currentAccessory = accessory
        invalidate()
    }
    
    /**
     * 根据生活状态更新外观
     */
    fun updateByLifeState(state: LifeSyncManager.LifeState) {
        setAccessory(state.accessory)
        
        // 根据心情和时间更新状态
        when {
            state.timeOfDay == LifeSyncManager.TimeOfDay.MIDNIGHT || 
            state.timeOfDay == LifeSyncManager.TimeOfDay.NIGHT -> showSleepy()
            state.mood == LifeSyncManager.Mood.SAD -> showSad()
            state.mood == LifeSyncManager.Mood.TIRED -> showSleepy()
            else -> showNormal()
        }
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
            State.SLEEPY -> Color.parseColor("#9B59B6")
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
            State.SLEEPY -> Color.parseColor("#AF7AC5")
            else -> Color.parseColor("#C0392B")
        }
        paint.color = headColor
        canvas.drawCircle(centerX, centerY - 35f, 35f, paint)

        // 绘制配饰（在头部上方）
        drawAccessory(canvas, centerX, centerY - 35f)

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
            State.SLEEPY -> drawSleepyFace(canvas, centerX, centerY)
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
    
    /**
     * 绘制困倦表情
     */
    private fun drawSleepyFace(canvas: Canvas, x: Float, y: Float) {
        // 闭着的眼睛（弧线）
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawArc(x - 20f, y - 50f, x - 4f, y - 40f, 0f, 180f, false, paint)
        canvas.drawArc(x + 4f, y - 50f, x + 20f, y - 40f, 0f, 180f, false, paint)
        
        // 小嘴巴
        canvas.drawArc(x - 8f, y - 35f, x + 8f, y - 28f, 0f, 180f, false, paint)
        
        // "zzz" 气泡
        paint.style = Paint.Style.FILL
        paint.textSize = 20f
        paint.textAlign = Paint.Align.CENTER
        val zOffset = (System.currentTimeMillis() % 2000) / 2000f * 10f
        canvas.drawText("z", x + 25f, y - 55f - zOffset, paint)
        canvas.drawText("z", x + 35f, y - 65f - zOffset * 1.5f, paint)
        paint.style = Paint.Style.FILL
    }
    
    /**
     * 绘制配饰
     */
    private fun drawAccessory(canvas: Canvas, centerX: Float, centerY: Float) {
        when (currentAccessory) {
            LifeSyncManager.AccessoryType.SUNGLASSES -> drawSunglasses(canvas, centerX, centerY)
            LifeSyncManager.AccessoryType.UMBRELLA -> drawUmbrella(canvas, centerX, centerY)
            LifeSyncManager.AccessoryType.SCARF -> drawScarf(canvas, centerX, centerY)
            LifeSyncManager.AccessoryType.SANTA_HAT -> drawSantaHat(canvas, centerX, centerY)
            LifeSyncManager.AccessoryType.PARTY_HAT -> drawPartyHat(canvas, centerX, centerY)
            LifeSyncManager.AccessoryType.RED_ENVELOPE -> drawRedEnvelope(canvas, centerX, centerY)
            LifeSyncManager.AccessoryType.MOONCAKE -> drawMooncake(canvas, centerX, centerY)
            else -> {} // 无配饰
        }
    }
    
    /**
     * 绘制墨镜
     */
    private fun drawSunglasses(canvas: Canvas, x: Float, y: Float) {
        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        // 左镜片
        canvas.drawRoundRect(x - 25f, y - 50f, x - 5f, y - 35f, 5f, 5f, paint)
        // 右镜片
        canvas.drawRoundRect(x + 5f, y - 50f, x + 25f, y - 35f, 5f, 5f, paint)
        // 鼻梁架
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(x - 5f, y - 45f, x + 5f, y - 45f, paint)
        paint.style = Paint.Style.FILL
    }
    
    /**
     * 绘制雨伞
     */
    private fun drawUmbrella(canvas: Canvas, x: Float, y: Float) {
        // 伞面
        paint.color = Color.parseColor("#3498DB")
        paint.style = Paint.Style.FILL
        val path = Path()
        path.moveTo(x - 30f, y - 10f)
        path.quadTo(x, y - 50f, x + 30f, y - 10f)
        path.lineTo(x + 25f, y - 5f)
        path.quadTo(x, y - 35f, x - 25f, y - 5f)
        path.close()
        canvas.drawPath(path, paint)
        
        // 伞柄
        paint.color = Color.parseColor("#7F8C8D")
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(x, y - 25f, x, y + 10f, paint)
        canvas.drawArc(x - 5f, y + 5f, x + 5f, y + 15f, 0f, 180f, false, paint)
        paint.style = Paint.Style.FILL
    }
    
    /**
     * 绘制围巾
     */
    private fun drawScarf(canvas: Canvas, x: Float, y: Float) {
        paint.color = Color.parseColor("#E74C3C")
        paint.style = Paint.Style.FILL
        // 围巾主体
        canvas.drawRect(x - 30f, y - 10f, x + 30f, y + 5f, paint)
        // 围巾飘带
        canvas.drawRect(x + 15f, y + 5f, x + 25f, y + 25f, paint)
        // 条纹
        paint.color = Color.parseColor("#C0392B")
        canvas.drawRect(x - 30f, y - 5f, x + 30f, y, paint)
        paint.style = Paint.Style.FILL
    }
    
    /**
     * 绘制圣诞帽
     */
    private fun drawSantaHat(canvas: Canvas, x: Float, y: Float) {
        // 白色帽檐
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawRect(x - 30f, y - 55f, x + 30f, y - 45f, paint)
        
        // 红色帽子
        paint.color = Color.parseColor("#C0392B")
        val path = Path()
        path.moveTo(x - 25f, y - 50f)
        path.lineTo(x + 25f, y - 50f)
        path.lineTo(x, y - 90f)
        path.close()
        canvas.drawPath(path, paint)
        
        // 白色绒球
        paint.color = Color.WHITE
        canvas.drawCircle(x, y - 90f, 8f, paint)
    }
    
    /**
     * 绘制派对帽
     */
    private fun drawPartyHat(canvas: Canvas, x: Float, y: Float) {
        // 帽子
        paint.color = Color.parseColor("#9B59B6")
        paint.style = Paint.Style.FILL
        val path = Path()
        path.moveTo(x - 20f, y - 50f)
        path.lineTo(x + 20f, y - 50f)
        path.lineTo(x, y - 85f)
        path.close()
        canvas.drawPath(path, paint)
        
        // 彩色条纹
        paint.color = Color.parseColor("#F1C40F")
        canvas.drawCircle(x - 8f, y - 65f, 3f, paint)
        paint.color = Color.parseColor("#E74C3C")
        canvas.drawCircle(x + 5f, y - 60f, 3f, paint)
        
        // 顶部装饰
        paint.color = Color.parseColor("#F1C40F")
        canvas.drawCircle(x, y - 88f, 5f, paint)
    }
    
    /**
     * 绘制红包
     */
    private fun drawRedEnvelope(canvas: Canvas, x: Float, y: Float) {
        paint.color = Color.parseColor("#C0392B")
        paint.style = Paint.Style.FILL
        // 红包主体
        canvas.drawRect(x - 15f, y - 75f, x + 15f, y - 45f, paint)
        
        // 金色装饰
        paint.color = Color.parseColor("#F1C40F")
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        canvas.drawRect(x - 12f, y - 72f, x + 12f, y - 48f, paint)
        
        // "福"字位置
        paint.style = Paint.Style.FILL
        paint.textSize = 12f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("福", x, y - 55f, paint)
    }
    
    /**
     * 绘制月饼
     */
    private fun drawMooncake(canvas: Canvas, x: Float, y: Float) {
        paint.color = Color.parseColor("#D4AC0D")
        paint.style = Paint.Style.FILL
        // 月饼主体
        canvas.drawCircle(x + 25f, y - 60f, 12f, paint)
        
        // 花纹
        paint.color = Color.parseColor("#B7950B")
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        for (i in 0..7) {
            val angle = i * 45f
            val startX = x + 25f + kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat() * 6f
            val startY = y - 60f + kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() * 6f
            val endX = x + 25f + kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat() * 10f
            val endY = y - 60f + kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() * 10f
            canvas.drawLine(startX, startY, endX, endY, paint)
        }
        paint.style = Paint.Style.FILL
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
