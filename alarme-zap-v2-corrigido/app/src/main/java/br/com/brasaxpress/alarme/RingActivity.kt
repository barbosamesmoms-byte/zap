package br.com.brasaxpress.alarme

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextClock
import android.widget.TextView

class RingActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (!AlarmPlayer.active) { finish(); return }

        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
            setPadding(pad, pad, pad, pad)
        }
        root.addView(TextClock(this).apply {
            format24Hour = "HH:mm"; format12Hour = "HH:mm"
            textSize = 72f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = AlarmPlayer.title; textSize = 24f
            setTextColor(Color.WHITE); gravity = Gravity.CENTER; setPadding(0, pad, 0, pad * 3)
        })
        root.addView(TextView(this).apply {
            text = "Deslize até o fim para parar  ➜"; textSize = 18f
            setTextColor(Color.LTGRAY); gravity = Gravity.CENTER
        })
        root.addView(SeekBar(this).apply {
            max = 100
            setPadding(pad * 2, pad, pad * 2, pad)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {}
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {
                    if ((s?.progress ?: 0) >= 95) {
                        AlarmPlayer.stop(this@RingActivity)
                        finish()
                    } else s?.progress = 0
                }
            })
        })
        setContentView(root)
    }

    @Deprecated("sem voltar")
    override fun onBackPressed() {}
}
