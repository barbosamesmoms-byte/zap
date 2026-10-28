package br.com.brasaxpress.alarme

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat

class MainActivity : Activity() {
    private val dias = arrayOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
    private val tipos = arrayOf("Todas", "Individual", "Grupo")
    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    private lateinit var content: LinearLayout

    private fun dp(v: Int) = Ui.dp(this, v)
    private fun fmt(m: Int) = "%02d:%02d".format((m / 60) % 24, m % 60)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = Ui.BG
        window.navigationBarColor = Ui.BG
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(40), dp(20), dp(32))
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Ui.BG)
            isFillViewport = true
            addView(content)
        }
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        build()
    }

    // ---------- pequenos ajudantes ----------

    private fun row() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = Ui.rounded(this@MainActivity, Ui.CARD, 20f)
        setPadding(dp(16), dp(16), dp(16), dp(16))
        layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(12) }
    }

    private fun botao(t: String, onClick: () -> Unit): TextView =
        Ui.text(this, t, 15f, Color.BLACK, true).apply {
            gravity = Gravity.CENTER
            background = Ui.gradient(this@MainActivity, 14f)
            setPadding(dp(16), dp(13), dp(16), dp(13))
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(12) }
            setOnClickListener { onClick() }
        }

    private fun appIcon(pkg: String): Drawable? =
        try { packageManager.getApplicationIcon(pkg) } catch (_: Exception) { getDrawable(R.mipmap.ic_launcher) }

    private fun diasTexto(d: Set<Int>): String = when {
        d.size == 7 -> "Todos os dias"
        d == setOf(2, 3, 4, 5, 6, 7) -> "Seg a Sáb"
        d == setOf(2, 3, 4, 5, 6) -> "Seg a Sex"
        d == setOf(1, 7) -> "Fim de semana"
        else -> d.sorted().joinToString(" · ") { dias[it - 1] }
    }

    private fun horarioTexto(r: Rule): String =
        if (r.start == 0 && r.end == 0) "O dia todo" else "${fmt(r.start)} – ${fmt(r.end)}"

    private fun linhaAjuste(t: String, d: String, onClick: () -> Unit): LinearLayout = row().apply {
        background = Ui.rounded(this@MainActivity, Ui.CARD, 16f)
        setPadding(dp(16), dp(14), dp(16), dp(14))
        layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(10) }
        val col = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
        }
        col.addView(Ui.text(this@MainActivity, t, 15f, Ui.TEXT, true))
        col.addView(Ui.text(this@MainActivity, d, 12f, Ui.MUTED))
        addView(col)
        addView(Ui.text(this@MainActivity, "›", 22f, Ui.MUTED))
        setOnClickListener { onClick() }
    }

    private fun pedirBateria() {
        val pm = getSystemService(PowerManager::class.java)
        try {
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
            } else {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    // ---------- tela principal ----------

    private fun build() {
        content.removeAllViews()
        val ok = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        val regras = Store.rules(this)

        // Cabeçalho
        val topo = row()
        topo.addView(ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
        })
        val titulos = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
        }
        titulos.addView(Ui.text(this, "Alarme Zap", 26f, Ui.TEXT, true))
        titulos.addView(Ui.text(this, "Toca quando chega mensagem", 14f, Ui.MUTED))
        topo.addView(titulos)
        content.addView(topo)
        content.addView(View(this), LinearLayout.LayoutParams(MATCH, dp(20)))

        // Situação
        val st = card()
        val linha = row()
        linha.addView(View(this).apply {
            background = Ui.rounded(this@MainActivity, if (ok) Ui.GREEN else Ui.AMBER, 8f)
            layoutParams = LinearLayout.LayoutParams(dp(12), dp(12)).apply { rightMargin = dp(12) }
        })
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
        }
        col.addView(Ui.text(this, if (ok) "Monitorando notificações" else "Acesso às notificações desligado", 16f, Ui.TEXT, true))
        val sub = when {
            !ok -> "Sem isso o alarme não toca."
            regras.none { it.enabled } -> "Crie uma regra para começar."
            else -> "Pronto: o alarme toca quando chegar mensagem."
        }
        col.addView(Ui.text(this, sub, 13f, Ui.MUTED))
        linha.addView(col)
        st.addView(linha)
        if (!ok) st.addView(botao("Liberar acesso") { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) })
        content.addView(st)

        // Regras
        val cab = row().apply { setPadding(0, dp(12), 0, dp(10)) }
        cab.addView(Ui.text(this, "Regras", 20f, Ui.TEXT, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
        })
        cab.addView(Ui.text(this, "+ Nova regra", 14f, Color.BLACK, true).apply {
            background = Ui.gradient(this@MainActivity, 18f)
            setPadding(dp(16), dp(9), dp(16), dp(9))
            setOnClickListener { editor(null) }
        })
        content.addView(cab)

        if (regras.isEmpty()) {
            val vazio = card()
            vazio.addView(Ui.text(this, "Nenhuma regra ainda", 16f, Ui.TEXT, true))
            vazio.addView(Ui.text(this, "Toque em + Nova regra para escolher o app, os dias e o horário em que o alarme pode tocar.", 13f, Ui.MUTED))
            content.addView(vazio)
        }
        regras.forEach { r ->
            val item = row().apply {
                background = Ui.rounded(this@MainActivity, Ui.CARD, 20f)
                setPadding(dp(14), dp(14), dp(14), dp(14))
                layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(10) }
                alpha = if (r.enabled) 1f else 0.55f
                setOnClickListener { editor(r) }
            }
            item.addView(ImageView(this).apply {
                setImageDrawable(appIcon(r.pkg))
                layoutParams = LinearLayout.LayoutParams(dp(46), dp(46)).apply { rightMargin = dp(14) }
            })
            val textos = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
            }
            textos.addView(Ui.text(this, r.label, 16f, Ui.TEXT, true))
            textos.addView(Ui.text(this, "${diasTexto(r.days)} · ${horarioTexto(r)}", 13f, Ui.AMBER))
            val tp = if (Rules.isWhatsApp(r.pkg)) tipos[r.type] else "Qualquer notificação"
            textos.addView(Ui.text(this, "$tp · volume ${r.volume}%", 12f, Ui.MUTED))
            item.addView(textos)
            val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
            item.addView(Switch(this).apply {
                thumbTintList = ColorStateList(states, intArrayOf(Ui.ACCENT, Ui.MUTED))
                trackTintList = ColorStateList(states, intArrayOf(Color.parseColor("#66FF7A1A"), Color.parseColor("#33FFFFFF")))
                isChecked = r.enabled
                setOnCheckedChangeListener { _, on ->
                    r.enabled = on
                    Store.upsert(this@MainActivity, r)
                    item.alpha = if (on) 1f else 0.55f
                }
            })
            content.addView(item)
        }

        // Ajustes
        content.addView(Ui.text(this, "Ajustes", 20f, Ui.TEXT, true).apply { setPadding(0, dp(18), 0, dp(10)) })
        content.addView(linhaAjuste("Acesso às notificações", "Necessário para ler as mensagens") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
        content.addView(linhaAjuste("Rodar em segundo plano", "Tira o app da economia de bateria") { pedirBateria() })
        content.addView(linhaAjuste("Testar alarme", "Toca agora para conferir som e tela") {
            AlarmPlayer.trigger(this, "Teste de alarme", 80)
        })

        // Diagnóstico (escondido)
        val dbg = getSharedPreferences("alarme_debug", MODE_PRIVATE)
        val diagTxt = Ui.text(
            this,
            "Serviço de notificações: ${dbg.getString("listener", "aguardando conexão")}\n" +
                "Última notificação: ${dbg.getString("ultima", "nenhuma ainda")}\n" +
                "Último alarme: ${dbg.getString("alarme", "nenhuma tentativa")}",
            12f, Ui.MUTED
        ).apply { visibility = View.GONE; setPadding(dp(4), dp(4), dp(4), 0) }
        content.addView(Ui.text(this, "Diagnóstico  ▸", 13f, Ui.MUTED).apply {
            setPadding(dp(4), dp(14), dp(4), dp(4))
            setOnClickListener { diagTxt.visibility = if (diagTxt.visibility == View.GONE) View.VISIBLE else View.GONE }
        })
        content.addView(diagTxt)
    }

    // ---------- editor de regra (uma tela só) ----------

    private fun secao(t: String) = Ui.text(this, t.uppercase(), 11f, Ui.MUTED, true).apply {
        letterSpacing = 0.1f
        setPadding(0, dp(18), 0, dp(8))
    }

    private fun caixaHora(rotulo: String, valor: Int, onPick: (Int) -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = Ui.rounded(this@MainActivity, Ui.CARD2, 16f)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f).apply { leftMargin = dp(4); rightMargin = dp(4) }
            addView(Ui.text(this@MainActivity, rotulo, 12f, Ui.MUTED))
            addView(Ui.text(this@MainActivity, fmt(valor), 26f, Ui.TEXT, true))
            setOnClickListener {
                TimePickerDialog(this@MainActivity, { _, h, m -> onPick(h * 60 + m) }, (valor / 60) % 24, valor % 60, true).show()
            }
        }

    private fun editor(old: Rule?) {
        var pkg = old?.pkg ?: "com.whatsapp.w4b"
        var label = old?.label ?: "WhatsApp Business"
        var tipo = old?.type ?: 0
        val days = (old?.days ?: setOf(2, 3, 4, 5, 6, 7)).toMutableSet()
        var ini = old?.start ?: (18 * 60)
        var fim = old?.end ?: 0
        var vol = old?.volume ?: 100

        val dlg = Dialog(this)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(14))
        }
        dlg.setContentView(ScrollView(this).apply { addView(body) })
        dlg.window?.setBackgroundDrawable(Ui.rounded(this, Ui.CARD, 24f))

        fun render() {
            body.removeAllViews()
            body.addView(Ui.text(this, if (old == null) "Nova regra" else "Editar regra", 20f, Ui.TEXT, true))

            // App
            body.addView(secao("App que dispara o alarme"))
            val campoApp = row().apply {
                background = Ui.rounded(this@MainActivity, Ui.CARD2, 16f)
                setPadding(dp(12), dp(10), dp(14), dp(10))
            }
            campoApp.addView(ImageView(this).apply {
                setImageDrawable(appIcon(pkg))
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { rightMargin = dp(12) }
            })
            campoApp.addView(Ui.text(this, label, 16f, Ui.TEXT, true).apply {
                layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
            })
            campoApp.addView(Ui.text(this, "Trocar", 13f, Ui.AMBER, true))
            campoApp.setOnClickListener {
                escolherApp { p, l ->
                    pkg = p
                    label = l
                    if (!Rules.isWhatsApp(p)) tipo = 0
                    render()
                }
            }
            body.addView(campoApp)

            // Tipo de mensagem (só WhatsApp)
            if (Rules.isWhatsApp(pkg)) {
                body.addView(secao("Mensagens"))
                val linhaTipo = row()
                for (i in tipos.indices) {
                    val sel = tipo == i
                    linhaTipo.addView(Ui.text(this, tipos[i], 14f, if (sel) Color.BLACK else Ui.TEXT, true).apply {
                        gravity = Gravity.CENTER
                        background = if (sel) Ui.gradient(this@MainActivity, 20f) else Ui.rounded(this@MainActivity, Ui.CARD2, 20f)
                        layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply { leftMargin = dp(3); rightMargin = dp(3) }
                        setOnClickListener { tipo = i; render() }
                    })
                }
                body.addView(linhaTipo)
            }

            // Dias
            body.addView(secao("Dias da semana"))
            val linhaDias = row()
            for (i in 0..6) {
                val d = i + 1
                val sel = d in days
                linhaDias.addView(Ui.text(this, dias[i], 12f, if (sel) Color.BLACK else Ui.TEXT, true).apply {
                    gravity = Gravity.CENTER
                    background = if (sel) Ui.gradient(this@MainActivity, 20f) else Ui.rounded(this@MainActivity, Ui.CARD2, 20f)
                    layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply { leftMargin = dp(2); rightMargin = dp(2) }
                    setOnClickListener { if (d in days) days.remove(d) else days.add(d); render() }
                })
            }
            body.addView(linhaDias)

            // Horário
            body.addView(secao("Horário (00:00 = meia-noite)"))
            val linhaHora = row()
            linhaHora.addView(caixaHora("Começa", ini) { ini = it; render() })
            linhaHora.addView(caixaHora("Termina", fim) { fim = it; render() })
            body.addView(linhaHora)

            // Volume
            body.addView(secao("Volume do alarme"))
            val volTxt = Ui.text(this, "$vol%", 14f, Ui.AMBER, true)
            val sb = SeekBar(this).apply {
                max = 100
                progress = vol
                progressTintList = ColorStateList.valueOf(Ui.ACCENT)
                thumbTintList = ColorStateList.valueOf(Ui.ACCENT)
                layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                        vol = p.coerceAtLeast(5)
                        volTxt.text = "$vol%"
                    }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) {}
                })
            }
            val linhaVol = row()
            linhaVol.addView(sb)
            linhaVol.addView(volTxt)
            body.addView(linhaVol)

            // Ações
            val acoes = row().apply { setPadding(0, dp(22), 0, 0) }
            if (old != null) {
                acoes.addView(Ui.text(this, "Apagar", 15f, Ui.RED, true).apply {
                    setPadding(dp(4), dp(12), dp(16), dp(12))
                    setOnClickListener { Store.remove(this@MainActivity, old.id); dlg.dismiss(); build() }
                })
            }
            acoes.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
            acoes.addView(Ui.text(this, "Cancelar", 15f, Ui.MUTED, true).apply {
                setPadding(dp(16), dp(12), dp(16), dp(12))
                setOnClickListener { dlg.dismiss() }
            })
            acoes.addView(Ui.text(this, "Salvar", 15f, Color.BLACK, true).apply {
                background = Ui.gradient(this@MainActivity, 16f)
                setPadding(dp(26), dp(12), dp(26), dp(12))
                setOnClickListener {
                    val id = old?.id ?: (System.currentTimeMillis() / 1000).toInt()
                    val diasFinal = days.toSet().ifEmpty { (1..7).toSet() }
                    val tp = if (Rules.isWhatsApp(pkg)) tipo else 0
                    Store.upsert(this@MainActivity, Rule(id, pkg, label, diasFinal, ini, fim, tp, vol, old?.enabled ?: true))
                    dlg.dismiss()
                    build()
                }
            })
            body.addView(acoes)
        }

        render()
        dlg.show()
        dlg.window?.setLayout((resources.displayMetrics.widthPixels * 0.92f).toInt(), WRAP)
    }

    private fun escolherApp(onPick: (String, String) -> Unit) {
        val pm = packageManager
        val outros = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.loadLabel(pm).toString() to it.activityInfo.packageName }
            .filter { it.second != packageName && !Rules.isWhatsApp(it.second) }
            .distinctBy { it.second }
            .sortedBy { it.first.lowercase() }
        val lista = listOf("WhatsApp Business" to "com.whatsapp.w4b", "WhatsApp" to "com.whatsapp") + outros

        val dlg = Dialog(this)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val lv = ListView(this).apply {
            divider = null
            setPadding(dp(8), dp(8), dp(8), dp(8))
            adapter = object : BaseAdapter() {
                override fun getCount() = lista.size
                override fun getItem(position: Int) = lista[position]
                override fun getItemId(position: Int) = position.toLong()
                override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                    val (nome, pk) = lista[position]
                    val r = row().apply { setPadding(dp(12), dp(10), dp(12), dp(10)) }
                    r.addView(ImageView(this@MainActivity).apply {
                        setImageDrawable(appIcon(pk))
                        layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply { rightMargin = dp(14) }
                    })
                    r.addView(Ui.text(this@MainActivity, nome, 16f, Ui.TEXT))
                    return r
                }
            }
            setOnItemClickListener { _, _, i, _ ->
                dlg.dismiss()
                onPick(lista[i].second, lista[i].first)
            }
        }
        dlg.setContentView(lv)
        dlg.window?.setBackgroundDrawable(Ui.rounded(this, Ui.CARD, 24f))
        dlg.show()
        dlg.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            (resources.displayMetrics.heightPixels * 0.7f).toInt()
        )
    }
}
