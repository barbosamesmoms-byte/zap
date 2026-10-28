package br.com.brasaxpress.alarme

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import kotlin.math.abs

class RingActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.parseColor("#0E0B16")
        if (!AlarmPlayer.active) { finish(); return }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#33195C"), Color.parseColor("#0E0B16"))
            )
            setPadding(Ui.dp(this@RingActivity, 28), Ui.dp(this@RingActivity, 56), Ui.dp(this@RingActivity, 28), Ui.dp(this@RingActivity, 44))
        }

        // Sino com halo pulsando
        val frame = FrameLayout(this)
        val halo = View(this).apply {
            background = GradientDrawable().apply {
                gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientRadius = Ui.dpf(this@RingActivity, 130f)
                colors = intArrayOf(Color.parseColor("#66FF7A1A"), Color.parseColor("#00FF7A1A"))
            }
        }
        frame.addView(halo, FrameLayout.LayoutParams(Ui.dp(this, 260), Ui.dp(this, 260), Gravity.CENTER))
        val bell = ImageView(this).apply { setImageResource(R.drawable.ic_bell) }
        frame.addView(bell, FrameLayout.LayoutParams(Ui.dp(this, 112), Ui.dp(this, 112), Gravity.CENTER))
        root.addView(frame, LinearLayout.LayoutParams(Ui.dp(this, 260), Ui.dp(this, 260)))

        ObjectAnimator.ofFloat(bell, "rotation", 0f, -14f, 14f, -10f, 10f, 0f).apply {
            duration = 1100
            repeatCount = ValueAnimator.INFINITE
            start()
        }
        ObjectAnimator.ofPropertyValuesHolder(
            halo,
            PropertyValuesHolder.ofFloat("scaleX", 0.85f, 1.1f),
            PropertyValuesHolder.ofFloat("scaleY", 0.85f, 1.1f),
            PropertyValuesHolder.ofFloat("alpha", 1f, 0.45f)
        ).apply {
            duration = 900
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }

        root.addView(TextClock(this).apply {
            format24Hour = "HH:mm"
            format12Hour = "HH:mm"
            textSize = 68f
            setTextColor(Ui.TEXT)
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            gravity = Gravity.CENTER
        })
        root.addView(TextClock(this).apply {
            format24Hour = "EEEE, d 'de' MMMM"
            format12Hour = "EEEE, d 'de' MMMM"
            textSize = 15f
            setTextColor(Ui.MUTED)
            gravity = Gravity.CENTER
        })
        root.addView(Ui.text(this, AlarmPlayer.title, 22f, Ui.TEXT, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, Ui.dp(this@RingActivity, 22), 0, 0)
        })
        root.addView(Ui.text(this, "Chegou uma mensagem", 14f, Ui.AMBER).apply {
            gravity = Gravity.CENTER
            setPadding(0, Ui.dp(this@RingActivity, 4), 0, 0)
        })

        root.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f))

        root.addView(SlideToStop(this) {
            AlarmPlayer.stop(this@RingActivity)
            finish()
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 72)))

        setContentView(root)
    }

    @Deprecated("sem voltar")
    override fun onBackPressed() {}
}

/** Controle "deslize para parar": arraste a bolinha até o fim. */
class SlideToStop(ctx: Context, private val onDone: () -> Unit) : View(ctx) {
    private val d = resources.displayMetrics.density
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#262034") }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumb = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val seta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF7A1A")
        style = Paint.Style.STROKE
        strokeWidth = 3.5f * d
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B9B1CC")
        textSize = 16f * d
        textAlign = Paint.Align.CENTER
    }
    private val rect = RectF()
    private val path = Path()
    private var progress = 0f
    private var dragging = false

    private val pad get() = 6f * d
    private val tr get() = height / 2f - pad
    private val range get() = width - 2 * pad - 2 * tr

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = h / 2f
        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, r, r, track)

        val cx = pad + tr + progress * range
        fill.shader = LinearGradient(0f, 0f, w, 0f, Color.parseColor("#FFB347"), Color.parseColor("#FF7A1A"), Shader.TileMode.CLAMP)
        fill.alpha = (70 + 185 * progress).toInt().coerceIn(0, 255)
        rect.set(0f, 0f, cx + tr + pad, h)
        canvas.drawRoundRect(rect, r, r, fill)

        label.alpha = (255 * (1f - progress)).toInt().coerceIn(0, 255)
        canvas.drawText("Deslize para parar", w / 2f + tr / 2f, h / 2f - (label.ascent() + label.descent()) / 2f, label)

        canvas.drawCircle(cx, h / 2f, tr, thumb)
        path.reset()
        path.moveTo(cx - 5f * d, h / 2f - 8f * d)
        path.lineTo(cx + 4f * d, h / 2f)
        path.lineTo(cx - 5f * d, h / 2f + 8f * d)
        canvas.drawPath(path, seta)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                val cx = pad + tr + progress * range
                if (abs(e.x - cx) <= tr * 2f) {
                    dragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
                return false
            }
            MotionEvent.ACTION_MOVE -> if (dragging) {
                progress = ((e.x - pad - tr) / range).coerceIn(0f, 1f)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (dragging) {
                dragging = false
                if (progress >= 0.9f) {
                    progress = 1f
                    invalidate()
                    onDone()
                } else {
                    ValueAnimator.ofFloat(progress, 0f).apply {
                        duration = 200
                        addUpdateListener { progress = it.animatedValue as Float; invalidate() }
                        start()
                    }
                }
                return true
            }
        }
        return super.onTouchEvent(e)
    }
}
