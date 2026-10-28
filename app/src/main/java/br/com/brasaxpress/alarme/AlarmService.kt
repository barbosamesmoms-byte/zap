package br.com.brasaxpress.alarme

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

class AlarmService : Service() {
    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int {
        val t = i?.getStringExtra("title") ?: "Alarme"
        val vol = i?.getIntExtra("vol", 100) ?: 100
        AlarmPlayer.title = t
        ServiceCompat.startForeground(
            this, AlarmPlayer.ID, AlarmPlayer.notif(this, t),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
        AlarmPlayer.start(this, vol)
        return START_NOT_STICKY
    }

    companion object {
        fun start(c: Context, vol: Int, title: String) {
            ContextCompat.startForegroundService(
                c, Intent(c, AlarmService::class.java).putExtra("vol", vol).putExtra("title", title)
            )
        }
    }
}
