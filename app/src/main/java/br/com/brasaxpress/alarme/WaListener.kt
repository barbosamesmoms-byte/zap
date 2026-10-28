package br.com.brasaxpress.alarme

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WaListener : NotificationListenerService() {
    private fun diagnostic(message: String) {
        val stamp = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date())
        getSharedPreferences("alarme_debug", MODE_PRIVATE).edit()
            .putString("ultima", "$stamp · $message")
            .apply()
    }

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
        // Ignora apenas resumos. Notificações contínuas/de serviço também podem
        // conter mensagens válidas em algumas versões do WhatsApp.
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            diagnostic("Ignorada: resumo de notificações de ${sbn.packageName}")
            return
        }
        if (n.category == Notification.CATEGORY_CALL) {
            diagnostic("Ignorada: chamada de ${sbn.packageName}")
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
            diagnostic("Notificação de ${sbn.packageName}, sem regra ativa correspondente. Regras deste app: ${candidatas.size}")
            return
        }

        val quem = e.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: r.label
        diagnostic("Regra encontrada: ${r.label}; iniciando alarme para ${sbn.packageName}")
        AlarmPlayer.trigger(this, "${r.label}: $quem", r.volume)
    }
}
