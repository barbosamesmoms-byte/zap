package br.com.brasaxpress.alarme

import android.app.Notification
import android.app.NotificationManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class WaListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != "com.whatsapp" && sbn.packageName != "com.whatsapp.w4b") return
        if (!Store.waOn(this) || AlarmPlayer.active) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val terms = Store.waTerms(this).split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        if (terms.isEmpty()) return

        val e = sbn.notification.extras
        val title = e.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = e.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val hay = "$title $text".lowercase()
        if (terms.none { hay.contains(it) }) return

        // O listener ja roda em processo vivo: toca direto, sem iniciar servico em segundo plano.
        val t = "WhatsApp: $title"
        AlarmPlayer.title = t
        getSystemService(NotificationManager::class.java).notify(AlarmPlayer.ID, AlarmPlayer.notif(this, t))
        AlarmPlayer.start(this, Store.waVolume(this))
    }
}
