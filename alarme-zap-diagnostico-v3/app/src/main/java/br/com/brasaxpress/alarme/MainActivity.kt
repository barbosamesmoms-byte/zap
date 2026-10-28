package br.com.brasaxpress.alarme

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import androidx.core.app.NotificationManagerCompat

class MainActivity : Activity() {
    private val dias = arrayOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
    private val tipos = arrayOf("Individual e grupo", "Só individual", "Só grupo")
    private lateinit var box: LinearLayout
    private lateinit var status: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun txt(s: String, size: Float = 16f) =
        TextView(this).apply { text = s; textSize = size; setPadding(0, dp(12), 0, dp(4)) }
    private fun fmt(m: Int) = "%02d:%02d".format(m / 60, m % 60)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(32), dp(16), dp(32))
        }
        root.addView(txt("Alarme de mensagens", 26f))
        status = txt("")
        root.addView(status)
        root.addView(Button(this).apply { text = "+ Nova regra"; setOnClickListener { novaRegra() } })
        box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(box)

        root.addView(txt("Ajustes", 22f))
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
            setOnClickListener { AlarmPlayer.trigger(this@MainActivity, "Teste de alarme", 80) }
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        val ok = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        val accessText = if (ok) "✅ Acesso às notificações liberado"
        else "⚠️ Falta liberar o acesso às notificações (botão 1, abaixo)"
        val debug = getSharedPreferences("alarme_debug", MODE_PRIVATE)
        val listener = debug.getString("listener", "aguardando conexão")
        val ultima = debug.getString("ultima", "Nenhuma notificação recebida desde a instalação desta versão")
        status.text = "$accessText\nServiço de notificações: $listener\nDiagnóstico: $ultima"
        render()
    }

    private fun render() {
        box.removeAllViews()
        val regras = Store.rules(this)
        if (regras.isEmpty()) box.addView(txt("Nenhuma regra ainda. Toque em + Nova regra."))
        regras.forEach { r ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(8))
            }
            val d = if (r.days.size == 7) "todos os dias" else r.days.sorted().joinToString(" ") { dias[it - 1] }
            val fim = if (r.end == 0) 1440 else r.end
            val tp = if (Rules.isWhatsApp(r.pkg)) tipos[r.type] else "qualquer notificação"
            row.addView(TextView(this).apply {
                text = "${r.label}\n$d · ${fmt(r.start)}–${fmt(fim)}\n$tp · vol ${r.volume}%"
                textSize = 18f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(Switch(this).apply {
                isChecked = r.enabled
                setOnCheckedChangeListener { _, on ->
                    r.enabled = on
                    Store.upsert(this@MainActivity, r)
                }
            })
            row.setOnLongClickListener {
                AlertDialog.Builder(this).setTitle("Apagar esta regra?")
                    .setPositiveButton("Apagar") { _, _ -> Store.remove(this, r.id); render() }
                    .setNegativeButton("Cancelar", null).show()
                true
            }
            box.addView(row)
        }
        if (regras.isNotEmpty()) box.addView(txt("Segure o dedo numa regra para apagar.", 13f))
    }

    // ---- Criar regra: app -> tipo -> dias -> início -> fim -> volume ----

    private fun novaRegra() {
        val nomes = arrayOf("WhatsApp Business", "WhatsApp", "Outro app...")
        AlertDialog.Builder(this).setTitle("Qual app dispara o alarme?")
            .setItems(nomes) { _, i ->
                when (i) {
                    0 -> passoTipo("com.whatsapp.w4b", "WhatsApp Business")
                    1 -> passoTipo("com.whatsapp", "WhatsApp")
                    else -> escolherOutroApp()
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun escolherOutroApp() {
        val pm = packageManager
        val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.loadLabel(pm).toString() to it.activityInfo.packageName }
            .distinctBy { it.second }
            .sortedBy { it.first.lowercase() }
        AlertDialog.Builder(this).setTitle("Escolha o app")
            .setItems(apps.map { it.first }.toTypedArray()) { _, i -> passoTipo(apps[i].second, apps[i].first) }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun passoTipo(pkg: String, label: String) {
        if (!Rules.isWhatsApp(pkg)) { passoDias(pkg, label, 0); return }
        AlertDialog.Builder(this).setTitle("Quais mensagens?")
            .setItems(tipos) { _, i -> passoDias(pkg, label, i) }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun passoDias(pkg: String, label: String, tipo: Int) {
        val sel = booleanArrayOf(false, true, true, true, true, true, true) // seg a sáb
        AlertDialog.Builder(this).setTitle("Dias da semana")
            .setMultiChoiceItems(dias, sel) { _, i, on -> sel[i] = on }
            .setPositiveButton("Próximo") { _, _ ->
                val d = sel.indices.filter { sel[it] }.map { it + 1 }.toSet().ifEmpty { (1..7).toSet() }
                passoInicio(pkg, label, tipo, d)
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun passoInicio(pkg: String, label: String, tipo: Int, d: Set<Int>) {
        TimePickerDialog(this, { _, h, m -> passoFim(pkg, label, tipo, d, h * 60 + m) }, 18, 0, true)
            .apply { setTitle("Começa às") }.show()
    }

    private fun passoFim(pkg: String, label: String, tipo: Int, d: Set<Int>, ini: Int) {
        TimePickerDialog(this, { _, h, m -> passoVolume(pkg, label, tipo, d, ini, h * 60 + m) }, 0, 0, true)
            .apply { setTitle("Termina às (00:00 = meia-noite)") }.show()
    }

    private fun passoVolume(pkg: String, label: String, tipo: Int, d: Set<Int>, ini: Int, fim: Int) {
        val sb = SeekBar(this).apply { max = 100; progress = 100; setPadding(dp(24), dp(24), dp(24), 0) }
        AlertDialog.Builder(this).setTitle("Volume do alarme").setView(sb)
            .setPositiveButton("Salvar") { _, _ ->
                val id = (System.currentTimeMillis() / 1000).toInt()
                Store.upsert(this, Rule(id, pkg, label, d, ini, fim, tipo, sb.progress.coerceAtLeast(5), true))
                render()
            }
            .setNegativeButton("Cancelar", null).show()
    }
}
