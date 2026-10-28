package br.com.brasaxpress.alarme

import android.app.Service
import android.content.Intent
import android.os.IBinder

class AlarmService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            AlarmPlayer.ACTION_STOP -> {
                // Avoid calling AlarmPlayer.stop() here: it also sends ACTION_STOP.
                AlarmPlayer.active = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            AlarmPlayer.ACTION_START -> {
                val t = intent.getStringExtra("title") ?: "Alarme"
                val vol = intent.getIntExtra("volume", 100)
                AlarmPlayer.title = t
                try {
                    // Promova o serviço antes de iniciar reprodução ou abrir a tela.
                    startForeground(AlarmPlayer.ID, AlarmPlayer.notif(this, t))
                    AlarmPlayer.startNow(this, vol)
                    getSharedPreferences("alarme_debug", MODE_PRIVATE).edit()
                        .putString("alarme", "Serviço em primeiro plano iniciado")
                        .apply()
                } catch (e: Exception) {
                    getSharedPreferences("alarme_debug", MODE_PRIVATE).edit()
                        .putString("alarme", "Falha ao iniciar serviço em primeiro plano: ${e.javaClass.simpleName}")
                        .apply()
                    // Tenta manter o alerta audível mesmo quando o Android recusa
                    // a promoção. O diagnóstico acima registra a limitação.
                    AlarmPlayer.startNow(this, vol)
                    stopSelf(startId)
                    return START_NOT_STICKY
                }
                try {
                    val screen = Intent(this, RingActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    startActivity(screen)
                } catch (_: Exception) {}
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
