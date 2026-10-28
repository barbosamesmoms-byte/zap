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
        val e = n.extras
        val pkg = sbn.packageName

        // Só registra diagnóstico de apps que têm regra (evita lixo de outros apps)
        val candidatas = Store.rules(this).filter { it.enabled && it.pkg == pkg }
        if (candidatas.isEmpty()) return

        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            diagnostic("Ignorada: resumo de notificações de $pkg")
            return
        }
        if (n.category == Notification.CATEGORY_CALL) {
            diagnostic("Ignorada: chamada de $pkg")
            return
        }

        val titulo = e.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val texto = (e.getCharSequence(Notification.EXTRA_TEXT) ?: e.getCharSequence(Notification.EXTRA_BIG_TEXT))
            ?.toString().orEmpty()

        // "WhatsApp procurando novas mensagens" e similares não são mensagens
        if (Rules.isWhatsApp(pkg) && Rules.isSync(n, titulo, texto)) {
            diagnostic("Ignorada: aviso fixo/sincronização do WhatsApp (\"$titulo $texto\")")
            return
        }

        val isGroup = e.getBoolean("android.isGroupConversation", false) ||
            e.getCharSequence("android.conversationTitle") != null

        val agora = Calendar.getInstance()
        val r = candidatas
            .filter { Rules.active(it, agora) && Rules.typeOk(it, isGroup) }
            .maxByOrNull { it.volume }
        if (r == null) {
            diagnostic("Mensagem de $pkg fora do dia/horário ou do tipo da regra")
            return
        }

        val quem = titulo.ifEmpty { r.label }
        diagnostic("Mensagem de $quem → tocando alarme")
        AlarmPlayer.trigger(this, "${r.label}: $quem", r.volume)
    }
}
