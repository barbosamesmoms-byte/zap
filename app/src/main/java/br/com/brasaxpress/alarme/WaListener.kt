package br.com.brasaxpress.alarme

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.Calendar

class WaListener : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        getSharedPreferences("alarme_debug", MODE_PRIVATE).edit()
            .putString("listener", "conectado")
            .putString("ultima", "Acesso às notificações conectado ao Android")
            .apply()
    }

    override fun onListenerDisconnected() {
        getSharedPreferences("alarme_debug", MODE_PRIVATE).edit()
            .putString("listener", "desconectado")
            .putString("ultima", "O Android desconectou o acesso às notificações")
            .apply()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val n = sbn.notification
        getSharedPreferences("alarme_debug", MODE_PRIVATE).edit()
            .putString("ultima", "Notificação recebida: ${sbn.packageName}; categoria=${n.category ?: "sem categoria"}; flags=${n.flags}")
            .apply()
        // Ignora resumo ("3 mensagens de 2 conversas"), notificações fixas (sincronização) e chamadas
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            getSharedPreferences("alarme_debug", MODE_PRIVATE).edit().putString("ultima", "Ignorada: resumo de notificações de ${sbn.packageName}").apply()
            return
        }
        if (n.flags and Notification.FLAG_ONGOING_EVENT != 0) {
            getSharedPreferences("alarme_debug", MODE_PRIVATE).edit().putString("ultima", "Ignorada: notificação contínua de ${sbn.packageName}").apply()
            return
        }
        if (n.category == Notification.CATEGORY_CALL || n.category == Notification.CATEGORY_SERVICE) {
            getSharedPreferences("alarme_debug", MODE_PRIVATE).edit().putString("ultima", "Ignorada: chamada/serviço de ${sbn.packageName}").apply()
            return
        }

        val e = n.extras
        val isGroup = e.getBoolean("android.isGroupConversation", false) ||
            e.getCharSequence("android.conversationTitle") != null

        val agora = Calendar.getInstance()
        val regras = Store.rules(this)
        val candidatas = regras.filter { it.enabled && it.pkg == sbn.packageName }
        val r = candidatas
            .filter { Rules.active(it, agora) && Rules.typeOk(it, isGroup) }
            .maxByOrNull { it.volume }
        if (r == null) {
            getSharedPreferences("alarme_debug", MODE_PRIVATE).edit()
                .putString("ultima", "Notificação recebida de ${sbn.packageName}, mas nenhuma regra ativa correspondeu. Regras deste app: ${candidatas.size}")
                .apply()
            return
        }

        val quem = e.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: r.label
        getSharedPreferences("alarme_debug", MODE_PRIVATE).edit()
            .putString("ultima", "Regra encontrada: ${r.label}; iniciando alarme para ${sbn.packageName}")
            .apply()
        AlarmPlayer.trigger(this, "${r.label}: $quem", r.volume)
    }
}
