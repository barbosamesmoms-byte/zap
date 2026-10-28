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
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat

object AlarmPlayer {
    const val CH = "alarme"
    const val ID = 1

    @Volatile var active = false
    @Volatile var title = "Alarme"
    private var player: MediaPlayer? = null

    fun notif(ctx: Context, t: String): Notification {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH, "Alarme", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(false)
            }
        )
        val pi = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, RingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(ctx, CH)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(t)
            .setContentText("Toque e deslize para parar")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .build()
    }

    fun start(ctx: Context, vol: Int) {
        if (active) return
        active = true
        try {
            val am = ctx.getSystemService(AudioManager::class.java)
            val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, (max * vol / 100).coerceIn(1, max), 0)
        } catch (_: Exception) {}
        player = MediaPlayer().apply {
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
        ctx.getSystemService(Vibrator::class.java)
            .vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0))
    }

    fun stop(ctx: Context) {
        try { player?.stop() } catch (_: Exception) {}
        player?.release()
        player = null
        ctx.getSystemService(Vibrator::class.java).cancel()
        ctx.getSystemService(NotificationManager::class.java).cancel(ID)
        ctx.stopService(Intent(ctx, AlarmService::class.java))
        active = false
    }
}
