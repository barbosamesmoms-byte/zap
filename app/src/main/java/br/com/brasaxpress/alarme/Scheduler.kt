package br.com.brasaxpress.alarme

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object Scheduler {
    private fun pending(c: Context, id: Int): PendingIntent =
        PendingIntent.getBroadcast(
            c, id, Intent(c, AlarmReceiver::class.java).putExtra("id", id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    fun nextTime(a: Alarm): Long {
        val now = Calendar.getInstance()
        for (add in 0..7) {
            val c = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, add)
                set(Calendar.HOUR_OF_DAY, a.hour)
                set(Calendar.MINUTE, a.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (c.after(now) && (a.days.isEmpty() || c.get(Calendar.DAY_OF_WEEK) in a.days)) return c.timeInMillis
        }
        return -1
    }

    fun schedule(c: Context, a: Alarm) {
        val am = c.getSystemService(AlarmManager::class.java)
        val pi = pending(c, a.id)
        am.cancel(pi)
        if (!a.enabled) return
        val t = nextTime(a)
        if (t < 0) return
        val show = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        am.setAlarmClock(AlarmManager.AlarmClockInfo(t, show), pi)
    }

    fun cancel(c: Context, a: Alarm) {
        c.getSystemService(AlarmManager::class.java).cancel(pending(c, a.id))
    }

    fun scheduleAll(c: Context) = Store.alarms(c).forEach { schedule(c, it) }
}
