package br.com.brasaxpress.alarme

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Alarm(
    val id: Int,
    var hour: Int,
    var minute: Int,
    var days: Set<Int>, // Calendar.DAY_OF_WEEK (1=dom ... 7=sab); vazio = uma vez
    var volume: Int,
    var enabled: Boolean
)

object Store {
    private fun p(c: Context) = c.getSharedPreferences("alarme", Context.MODE_PRIVATE)

    fun alarms(c: Context): MutableList<Alarm> {
        val arr = JSONArray(p(c).getString("alarms", "[]"))
        return MutableList(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val d = o.getJSONArray("days")
            Alarm(
                o.getInt("id"), o.getInt("h"), o.getInt("m"),
                (0 until d.length()).map { d.getInt(it) }.toSet(),
                o.getInt("v"), o.getBoolean("on")
            )
        }
    }

    fun save(c: Context, list: List<Alarm>) {
        val arr = JSONArray()
        list.forEach { a ->
            arr.put(
                JSONObject().put("id", a.id).put("h", a.hour).put("m", a.minute)
                    .put("days", JSONArray(a.days.toList())).put("v", a.volume).put("on", a.enabled)
            )
        }
        p(c).edit().putString("alarms", arr.toString()).apply()
    }

    fun upsert(c: Context, a: Alarm) {
        val l = alarms(c)
        val i = l.indexOfFirst { it.id == a.id }
        if (i >= 0) l[i] = a else l.add(a)
        save(c, l)
    }

    fun remove(c: Context, id: Int) = save(c, alarms(c).filter { it.id != id })

    fun waOn(c: Context) = p(c).getBoolean("waOn", false)
    fun setWaOn(c: Context, v: Boolean) = p(c).edit().putBoolean("waOn", v).apply()
    fun waTerms(c: Context) = p(c).getString("waTerms", "") ?: ""
    fun setWaTerms(c: Context, v: String) = p(c).edit().putString("waTerms", v).apply()
    fun waVolume(c: Context) = p(c).getInt("waVol", 100)
    fun setWaVolume(c: Context, v: Int) = p(c).edit().putInt("waVol", v).apply()
}
