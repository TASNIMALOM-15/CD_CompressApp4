package compress.joshattic.us.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import compress.joshattic.us.MainActivity
import compress.joshattic.us.R

/** Builds and posts the live-progress / finished notifications. */
object CompressionNotifier {
    const val CHANNEL_ID = "compression_progress"
    const val PROGRESS_ID = 1001
    const val DONE_ID = 1002

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildProgress(
        context: Context,
        percent: Int,
        name: String?,
        queueIndex: Int,
        queueTotal: Int
    ): Notification {
        ensureChannel(context)
        val title = if (queueTotal > 1) {
            context.getString(R.string.notif_compressing_queue, queueIndex, queueTotal)
        } else {
            context.getString(R.string.notif_compressing)
        }
        val text = if (name.isNullOrBlank()) "$percent%" else "$percent% • $name"
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(100, percent.coerceIn(0, 100), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(openAppIntent(context))
            .build()
    }

    fun updateProgress(
        context: Context,
        percent: Int,
        name: String?,
        queueIndex: Int,
        queueTotal: Int
    ) {
        try {
            val manager = NotificationManagerCompat.from(context)
            if (!manager.areNotificationsEnabled()) return
            manager.notify(PROGRESS_ID, buildProgress(context, percent, name, queueIndex, queueTotal))
        } catch (e: SecurityException) {
            // Notification permission missing: ignore, compression still works
        }
    }

    fun showDone(context: Context, message: String) {
        try {
            ensureChannel(context)
            val manager = NotificationManagerCompat.from(context)
            if (!manager.areNotificationsEnabled()) return
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(context.getString(R.string.notif_done_title))
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(openAppIntent(context))
                .build()
            manager.notify(DONE_ID, notification)
        } catch (e: SecurityException) {
            // ignore
        }
    }

    fun clearProgress(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(PROGRESS_ID)
        } catch (e: SecurityException) {
            // ignore
        }
    }
}
