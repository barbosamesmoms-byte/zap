package br.com.brasaxpress.alarme

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.TextView

/** Cores e pequenos ajudantes de visual (tema escuro com laranja "brasa"). */
object Ui {
    val BG = Color.parseColor("#0E0B16")
    val CARD = Color.parseColor("#1A1626")
    val CARD2 = Color.parseColor("#262034")
    val ACCENT = Color.parseColor("#FF7A1A")
    val AMBER = Color.parseColor("#FFB347")
    val TEXT = Color.parseColor("#F4F1FA")
    val MUTED = Color.parseColor("#9B93AD")
    val GREEN = Color.parseColor("#2BD67B")
    val RED = Color.parseColor("#FF5A5F")

    fun dp(c: Context, v: Int): Int = (v * c.resources.displayMetrics.density).toInt()
    fun dpf(c: Context, v: Float): Float = v * c.resources.displayMetrics.density

    fun rounded(c: Context, fill: Int, radiusDp: Float): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dpf(c, radiusDp)
        }

    fun gradient(c: Context, radiusDp: Float): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(AMBER, ACCENT)).apply {
            cornerRadius = dpf(c, radiusDp)
        }

    fun text(c: Context, s: String, sizeSp: Float, cor: Int = TEXT, bold: Boolean = false): TextView =
        TextView(c).apply {
            text = s
            textSize = sizeSp
            setTextColor(cor)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }
}
