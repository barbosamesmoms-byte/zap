package br.com.brasaxpress.alarme

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * Uma regra: "quando chegar notificação do app [pkg], toque o alarme,
 * nos dias [days] entre [start] e [end] (minutos desde 00:00)".
 * end = 0 significa meia-noite (24:00). type: 0 = individual e grupo, 1 = só individual, 2 = só grupo.
 */
data class Rule(
    val id: Int,
    val pkg: String,
    val label: String,
    val days: Set<Int>, // Calendar.DAY_OF_WEEK: 1=dom ... 7=sab
    val start: Int,
    val end: Int,
    val type: Int,
    val volume: Int,
    var enabled: Boolean
)

object Store {
    private fun p(c: Context) = c.getSharedPreferences("alarme_regras", Context.MODE_PRIVATE)

    fun rules(c: Context): MutableList<Rule> {
        val arr = JSONArray(p(c).getString("rules", "[]"))
        return MutableList(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val d = o.getJSONArray("days")
            Rule(
                o.getInt("id"), o.getString("pkg"), o.getString("label"),
                (0 until d.length()).map { d.getInt(it) }.toSet(),
                o.getInt("s"), o.getInt("e"), o.getInt("t"), o.getInt("v"), o.getBoolean("on")
            )
        }
    }

    fun save(c: Context, list: List<Rule>) {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(
                JSONObject().put("id", r.id).put("pkg", r.pkg).put("label", r.label)
                    .put("days", JSONArray(r.days.toList())).put("s", r.start).put("e", r.end)
                    .put("t", r.type).put("v", r.volume).put("on", r.enabled)
            )
        }
        p(c).edit().putString("rules", arr.toString()).apply()
    }

    fun upsert(c: Context, r: Rule) {
        val l = rules(c)
        val i = l.indexOfFirst { it.id == r.id }
        if (i >= 0) l[i] = r else l.add(r)
        save(c, l)
    }

    fun remove(c: Context, id: Int) = save(c, rules(c).filter { it.id != id })
}

object Rules {
    /** A regra está dentro do dia/horário agora? Aceita janela que vira a meia-noite (ex.: 18:00 às 02:00). */
    fun active(r: Rule, c: Calendar): Boolean {
        val t = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        val today = c.get(Calendar.DAY_OF_WEEK)
        val yesterday = if (today == 1) 7 else today - 1
        val s = r.start
        val e = if (r.end == 0) 1440 else r.end
        return if (s < e) today in r.days && t >= s && t < e
        else (today in r.days && t >= s) || (yesterday in r.days && t < e)
    }

    fun isWhatsApp(pkg: String) = pkg.startsWith("com.whatsapp")

    fun typeOk(r: Rule, isGroup: Boolean): Boolean {
        if (!isWhatsApp(r.pkg) || r.type == 0) return true
        return if (r.type == 1) !isGroup else isGroup
    }
}
