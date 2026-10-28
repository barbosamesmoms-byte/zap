package br.com.brasaxpress.alarme

import android.app.Service
import android.content.Intent
import android.os.IBinder

class AlarmService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            AlarmPlayer.ACTION_STOP -> {
                AlarmPlayer.stop(this)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            AlarmPlayer.ACTION_START -> {
                val t = intent.getStringExtra("title") ?: "Alarme"
                val vol = intent.getIntExtra("volume", 100)
                AlarmPlayer.title = t
                // É obrigatório promover o serviço imediatamente para primeiro plano.
                startForeground(AlarmPlayer.ID, AlarmPlayer.notif(this, t))
                AlarmPlayer.startNow(this, vol)
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

    override fun onDestroy() {
        AlarmPlayer.active = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
