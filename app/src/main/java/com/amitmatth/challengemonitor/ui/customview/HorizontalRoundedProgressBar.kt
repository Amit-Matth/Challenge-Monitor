package com.amitmatth.challengemonitor.ui.customview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.amitmatth.challengemonitor.R
import androidx.core.content.withStyledAttributes

class HorizontalRoundedProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var targetProgress: Int = 0
    private var currentDisplayProgress: Float = 0f

    private var maxProgress: Int = 100
    private var progressColor: Int = ContextCompat.getColor(context, R.color.neon_cyan)
    private var progressGradientStartColor: Int = progressColor
    private var progressGradientEndColor: Int = darkenColor(progressColor, 0.8f)
    private var bgColor: Int = ContextCompat.getColor(context, R.color.grey_dark)
    private var cornerRadius: Float = 20f
    private var barThickness: Float = 30f

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressRect = RectF()
    private val backgroundRect = RectF()
    private val path = Path()

    private var progressAnimator: ValueAnimator? = null
    private val animationDuration = 750L

    init {
        attrs?.let {
            context.withStyledAttributes(
                it,
                R.styleable.HorizontalRoundedProgressBar,
                defStyleAttr,
                0
            ) {
                targetProgress =
                    getInt(R.styleable.HorizontalRoundedProgressBar_hrp_progress, targetProgress)
                maxProgress =
                    getInt(R.styleable.HorizontalRoundedProgressBar_hrp_maxProgress, maxProgress)
                progressColor = getColor(
                    R.styleable.HorizontalRoundedProgressBar_hrp_progressColor,
                    progressColor
                )
                bgColor =
                    getColor(R.styleable.HorizontalRoundedProgressBar_hrp_backgroundColor, bgColor)
                cornerRadius = getDimension(
                    R.styleable.HorizontalRoundedProgressBar_hrp_cornerRadius,
                    cornerRadius
                )
                barThickness = getDimension(
                    R.styleable.HorizontalRoundedProgressBar_hrp_barThickness,
                    barThickness
                )
            }
        }
        updateProgressColorAndGradient(progressColor)
        backgroundPaint.color = bgColor
        currentDisplayProgress = 0f
    }

    private fun updateProgressColorAndGradient(baseColor: Int) {
        progressColor = baseColor
        progressGradientStartColor = baseColor
        progressGradientEndColor = darkenColor(baseColor, 0.7f)
        updateShader()
    }

    private fun updateShader() {
        if (width > 0 && height > 0) {
            progressPaint.shader = LinearGradient(
                0f, 0f, width.toFloat(), 0f,
                progressGradientStartColor,
                progressGradientEndColor,
                Shader.TileMode.CLAMP
            )
        }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateShader()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = barThickness.toInt() + paddingTop + paddingBottom
        val defaultWidth = (barThickness * 5).toInt() + paddingLeft + paddingRight

        val height = resolveSize(desiredHeight, heightMeasureSpec)
        val width = resolveSize(defaultWidth, widthMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val actualBarThickness = height - paddingTop - paddingBottom
        val viewWidth = width - paddingLeft - paddingRight

        if (actualBarThickness <= 0 || viewWidth <= 0) return

        backgroundRect.set(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            paddingLeft + viewWidth.toFloat(),
            paddingTop + actualBarThickness.toFloat()
        )
        path.reset()
        path.addRoundRect(backgroundRect, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.drawPath(path, backgroundPaint)

        if (currentDisplayProgress > 0.01f && maxProgress > 0) {
            val progressRatio = currentDisplayProgress / maxProgress.toFloat()
            val progressWidth = progressRatio * viewWidth

            progressRect.set(
                paddingLeft.toFloat(),
                paddingTop.toFloat(),
                paddingLeft + progressWidth,
                paddingTop + actualBarThickness.toFloat()
            )

            val radii = floatArrayOf(
                cornerRadius, cornerRadius,
                0f, 0f,
                0f, 0f,
                cornerRadius, cornerRadius
            )

            if (progressWidth >= viewWidth - 0.1f) {
                radii[2] = cornerRadius; radii[3] = cornerRadius
                radii[4] = cornerRadius; radii[5] = cornerRadius
            }

            path.reset()
            path.addRoundRect(progressRect, radii, Path.Direction.CW)
            canvas.drawPath(path, progressPaint)
        }
    }

    fun setProgress(newProgressValue: Int) {
        val newTarget = newProgressValue.coerceIn(0, maxProgress)
        this.targetProgress = newTarget

        progressAnimator?.cancel()

        progressAnimator =
            ValueAnimator.ofFloat(currentDisplayProgress, newTarget.toFloat()).apply {
                duration = animationDuration
                interpolator = DecelerateInterpolator()
                addUpdateListener { animation ->
                    currentDisplayProgress = animation.animatedValue as Float
                    invalidate()
                }
            }
        progressAnimator?.start()
    }

    fun setProgressImmediate(newProgressValue: Int) {
        targetProgress = newProgressValue.coerceIn(0, maxProgress)
        currentDisplayProgress = targetProgress.toFloat()
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        progressAnimator?.cancel()
        progressAnimator = null
    }

    fun setMaxProgress(value: Int) {
        if (value > 0) {
            maxProgress = value
            targetProgress = targetProgress.coerceIn(0, maxProgress)
            currentDisplayProgress = currentDisplayProgress.coerceIn(0f, maxProgress.toFloat())
            invalidate()
        }
    }

    fun setProgressColor(color: Int) {
        updateProgressColorAndGradient(color)
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).toInt()
        val g = (Color.green(color) * factor).toInt()
        val b = (Color.blue(color) * factor).toInt()
        return Color.argb(a, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }
}