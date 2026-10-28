package br.com.brasaxpress.alarme

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private val dias = arrayOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
    private lateinit var box: LinearLayout

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun txt(s: String, size: Float = 16f) =
        TextView(this).apply { text = s; textSize = size; setPadding(0, dp(12), 0, dp(4)) }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(32), dp(16), dp(32))
        }
        root.addView(txt("Alarmes", 28f))
        root.addView(Button(this).apply { text = "+ Novo alarme"; setOnClickListener { novoAlarme() } })
        box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(box)

        root.addView(txt("WhatsApp", 28f))
        root.addView(Switch(this).apply {
            text = "Disparar alarme por mensagem"
            isChecked = Store.waOn(this@MainActivity)
            setOnCheckedChangeListener { _, on -> Store.setWaOn(this@MainActivity, on) }
        })
        root.addView(EditText(this).apply {
            hint = "Contatos ou palavras (separe por vírgula)"
            setText(Store.waTerms(this@MainActivity))
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) { Store.setWaTerms(this@MainActivity, s.toString()) }
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            })
        })
        root.addView(txt("Volume do alarme do WhatsApp"))
        root.addView(SeekBar(this).apply {
            max = 100
            progress = Store.waVolume(this@MainActivity)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                    Store.setWaVolume(this@MainActivity, p.coerceAtLeast(5))
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })
        root.addView(Button(this).apply {
            text = "1. Liberar acesso às notificações"
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        })
        root.addView(Button(this).apply {
            text = "2. Tirar da economia de bateria"
            setOnClickListener { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
        })
        root.addView(Button(this).apply {
            text = "Testar alarme"
            setOnClickListener { AlarmService.start(this@MainActivity, 80, "Teste") }
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        box.removeAllViews()
        Store.alarms(this).sortedWith(compareBy({ it.hour }, { it.minute })).forEach { a ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(8))
            }
            val d = if (a.days.isEmpty()) "uma vez" else a.days.sorted().joinToString(" ") { dias[it - 1] }
            row.addView(TextView(this).apply {
                text = "%02d:%02d\n%s · vol %d%%".format(a.hour, a.minute, d, a.volume)
                textSize = 20f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(Switch(this).apply {
                isChecked = a.enabled
                setOnCheckedChangeListener { _, on ->
                    a.enabled = on
                    Store.upsert(this@MainActivity, a)
                    Scheduler.schedule(this@MainActivity, a)
                }
            })
            row.setOnLongClickListener {
                AlertDialog.Builder(this).setTitle("Apagar alarme?")
                    .setPositiveButton("Apagar") { _, _ ->
                        Scheduler.cancel(this, a)
                        Store.remove(this, a.id)
                        render()
                    }
                    .setNegativeButton("Cancelar", null).show()
                true
            }
            box.addView(row)
        }
    }

    private fun novoAlarme() {
        TimePickerDialog(this, { _, h, m -> escolherDias(h, m) }, 7, 0, true).show()
    }

    private fun escolherDias(h: Int, m: Int) {
        val sel = BooleanArray(7)
        AlertDialog.Builder(this).setTitle("Dias (nenhum = só uma vez)")
            .setMultiChoiceItems(dias, sel) { _, i, on -> sel[i] = on }
            .setPositiveButton("Próximo") { _, _ ->
                escolherVolume(h, m, sel.indices.filter { sel[it] }.map { it + 1 }.toSet())
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun escolherVolume(h: Int, m: Int, days: Set<Int>) {
        val sb = SeekBar(this).apply { max = 100; progress = 100; setPadding(dp(24), dp(24), dp(24), 0) }
        AlertDialog.Builder(this).setTitle("Volume").setView(sb)
            .setPositiveButton("Salvar") { _, _ ->
                val a = Alarm((System.currentTimeMillis() / 1000).toInt(), h, m, days, sb.progress.coerceAtLeast(5), true)
                Store.upsert(this, a)
                Scheduler.schedule(this, a)
                render()
            }
            .setNegativeButton("Cancelar", null).show()
    }
}
