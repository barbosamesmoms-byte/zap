package br.com.brasaxpress.alarme

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.Calendar

class WaListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (AlarmPlayer.active) return
        val n = sbn.notification
        // Ignora resumo ("3 mensagens de 2 conversas"), notificações fixas (sincronização) e chamadas
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (n.flags and Notification.FLAG_ONGOING_EVENT != 0) return
        if (n.category == Notification.CATEGORY_CALL || n.category == Notification.CATEGORY_SERVICE) return

        val e = n.extras
        val isGroup = e.getBoolean("android.isGroupConversation", false) ||
            e.getCharSequence("android.conversationTitle") != null

        val agora = Calendar.getInstance()
        val r = Store.rules(this)
            .filter { it.enabled && it.pkg == sbn.packageName && Rules.active(it, agora) && Rules.typeOk(it, isGroup) }
            .maxByOrNull { it.volume } ?: return

        val quem = e.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: r.label
        AlarmPlayer.trigger(this, "${r.label}: $quem", r.volume)
    }
}
