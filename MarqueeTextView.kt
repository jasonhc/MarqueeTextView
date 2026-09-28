
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.TypedValue
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.core.view.ViewCompat

/**
 * 支持通过 app:marqueeSpeed, app:marqueeDelay 自定义跑马灯速度和两轮之间的暂停时间
 *
 * 通过 setSelected() 控制滚动启停：selected=true 时开始滚动，selected=false 时停止滚动.
 * 文字宽度未超出 View 宽度时不滚动.
 */
class MarqueeTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : TextView(context, attrs, defStyleAttr) {

    /* 滚动速度，单位 dp/second */
    var marqueeSpeed: Int = DEFAULT_SPEED
        set(value) {
            field = value
            if (isSelected) {
                restartMarquee()
            }
        }

    /* 每轮结束后的暂停时间，单位 ms */
    var marqueeDelay: Int = DEFAULT_DELAY
        set(value) {
            field = value
            if (isSelected) {
                restartMarquee()
            }
        }

    private var currentOffset: Float = 0f
    private var animator: ValueAnimator? = null
    private var textWidth: Float = 0f
    private var needsMarquee: Boolean = false

    private val restartRunnable = Runnable {
        if (isSelected) {
            startMarquee()
        }
    }

    companion object {
        private const val DEFAULT_SPEED = 30   // dp/second, 与原生 TextView.MARQUEE_DP_PER_SECOND 一致
        private const val DEFAULT_DELAY = 1200 // ms，与原生 TextView.MARQUEE_DELAY 一致
        private const val FADE_DP = 20f         // 两侧渐隐长度
    }

    init {
        context.obtainStyledAttributes(attrs, R.styleable.MarqueeTextView).apply {
            marqueeSpeed = getInt(R.styleable.MarqueeTextView_marqueeSpeed, DEFAULT_SPEED)
            marqueeDelay = getInt(R.styleable.MarqueeTextView_marqueeDelay, DEFAULT_DELAY)
            recycle()
        }
        isSingleLine = true
        ellipsize = null

        // 开启原生渐隐效果
        setHorizontalFadingEdgeEnabled(true)
        setFadingEdgeLength((FADE_DP * resources.displayMetrics.density).toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateNeedsMarquee()
        if (needsMarquee && isSelected) {
            startMarquee()
        }
    }

    override fun getLeftFadingEdgeStrength(): Float {
        // 文字开始滚动后才显示左侧渐隐，且当左边缘正在显示的是两轮文本之间的空白时不显示
        return if (needsMarquee && isSelected && currentOffset > 0 && currentOffset <= textWidth) 1f else 0f
    }

    override fun getRightFadingEdgeStrength(): Float {
        return if (needsMarquee && isSelected) 1f else 0f
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
        if (width > 0) {
            updateNeedsMarquee()
        }
        if (isSelected) {
            restartMarquee()
        }
    }

    private fun restartMarquee() {
        stopMarquee()
        startMarquee()
    }

    override fun setSelected(selected: Boolean) {
        super.setSelected(selected)
        if (selected) {
            startMarquee()
        } else {
            stopMarquee()
        }
    }

    private fun updateNeedsMarquee() {
        textWidth = paint.measureText(text?.toString() ?: "")
        needsMarquee = textWidth > (width - paddingLeft - paddingRight)
    }

    override fun onDraw(canvas: Canvas) {
        if (!needsMarquee || layout == null) {
            super.onDraw(canvas)
            return
        }

        val viewWidth = (width - paddingLeft - paddingRight).toFloat()
        val totalWidth = calcTotalWidth()

        paint.color = currentTextColor

        // 限制绘制区域在 padding 之内
        canvas.clipRect(paddingLeft.toFloat(), 0f, (width - paddingRight).toFloat(), height.toFloat())

        // 第一份文字
        canvas.save()
        canvas.translate(paddingLeft - currentOffset, paddingTop.toFloat())
        layout.draw(canvas)
        canvas.restore()

        // 第一份文字快滚完时，第二份文字从右侧进入，实现无缝循环
        if (currentOffset > totalWidth - viewWidth) {
            canvas.save()
            canvas.translate(paddingLeft - currentOffset + totalWidth, paddingTop.toFloat())
            layout.draw(canvas)
            canvas.restore()
        }
    }

    private fun startMarquee() {
        if (!ViewCompat.isAttachedToWindow(this)) return
        if (width == 0) return

        updateNeedsMarquee()
        if (!needsMarquee) {
            currentOffset = 0f
            invalidate()
            return
        }

        val totalWidth = calcTotalWidth()
        val density = resources.displayMetrics.density
        val durationMs = (totalWidth / (marqueeSpeed * density) * 1000).toLong()

        animator?.cancel()
        currentOffset = 0f
        animator = ValueAnimator.ofFloat(0f, totalWidth).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            repeatCount = 0  // 每次只播一轮，结束后延迟一段时间重启
            addUpdateListener {
                currentOffset = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                private var isCancelled = false

                override fun onAnimationCancel(animation: Animator) {
                    isCancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!isCancelled) {
                        postDelayed(restartRunnable, marqueeDelay.toLong())
                    }
                }
            })
            start()
        }
    }

    private fun calcTotalWidth(): Float {
        val gap = (width - paddingLeft - paddingRight).toFloat() / 3f
        return textWidth + gap
    }

    private fun stopMarquee() {
        removeCallbacks(restartRunnable)
        animator?.cancel()
        animator = null
        currentOffset = 0f
        invalidate()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!isSelected) return

        if (hasWindowFocus) {
            // Activity 回到前台：重新开始滚动动画
            startMarquee()
        } else {
            // Activity 进入后台（或被其他 Window 遮挡）：停止滚动动画
            stopMarquee()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopMarquee()
    }
}
