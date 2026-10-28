package br.com.brasaxpress.alarme

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat

object AlarmPlayer {
    const val CH = "alarme"
    const val ID = 1
    const val ACTION_START = "br.com.brasaxpress.alarme.START"
    const val ACTION_STOP = "br.com.brasaxpress.alarme.STOP"

    @Volatile var active = false
    @Volatile var title = "Alarme"
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    fun notif(ctx: Context, t: String): Notification {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH, "Alarme", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(false)
                setBypassDnd(false)
            }
        )
        val pi = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, RingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(ctx, CH)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(t)
            .setContentText("Toque e deslize para parar")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .build()
    }

    fun trigger(ctx: Context, t: String, vol: Int) {
        if (active) return
        val i = Intent(ctx, AlarmService::class.java).apply {
            action = ACTION_START
            putExtra("title", t)
            putExtra("volume", vol)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        } catch (_: Exception) {
            // O Android pode bloquear a inicialização em segundo plano se as permissões
            // ou configurações do sistema não permitirem iniciar o serviço.
        }
    }

    fun startNow(ctx: Context, vol: Int) {
        if (active) return
        active = true
        try {
            val am = ctx.getSystemService(AudioManager::class.java)
            val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, (max * vol / 100).coerceIn(1, max), 0)
        } catch (_: Exception) {}
        try {
            player?.release()
            player = MediaPlayer().apply {
                setWakeMode(ctx, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(ctx, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {}
        try {
            vibrator = ctx.getSystemService(Vibrator::class.java)
            vibrator?.cancel()
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0))
        } catch (_: Exception) {}
    }

    fun stop(ctx: Context) {
        active = false
        try { player?.stop() } catch (_: Exception) {}
        try { player?.reset() } catch (_: Exception) {}
        try { player?.release() } catch (_: Exception) {}
        player = null
        try { vibrator?.cancel() } catch (_: Exception) {}
        try { ctx.getSystemService(Vibrator::class.java)?.cancel() } catch (_: Exception) {}
        try { ctx.getSystemService(NotificationManager::class.java).cancel(ID) } catch (_: Exception) {}
        try { ctx.stopService(Intent(ctx, AlarmService::class.java)) } catch (_: Exception) {}
    }
}
