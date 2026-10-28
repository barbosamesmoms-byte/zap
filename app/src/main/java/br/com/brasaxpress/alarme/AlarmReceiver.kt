package br.com.brasaxpress.alarme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val id = i.getIntExtra("id", -1)
        val a = Store.alarms(c).firstOrNull { it.id == id } ?: return
        if (a.days.isEmpty()) {
            a.enabled = false
            Store.upsert(c, a)
        }
        Scheduler.schedule(c, a) // reagenda o proximo (ou cancela se era unico)
        AlarmService.start(c, a.volume, "Alarme %02d:%02d".format(a.hour, a.minute))
    }
}
