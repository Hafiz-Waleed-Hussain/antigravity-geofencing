package com.antigravity.geofencing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay

class GeofenceWorker(context: Context, params: WorkerParameters) :
        CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val notificationId = 123
        setForeground(createForegroundInfo(notificationId))

        // Loop playing sound or just wait while notification is active
        // Simplification: We just wait here simulating the alarm playing service
        // The UI should observe this via WorkInfo or shared Event.

        // Notify UI via broadcast mostly
        val intent = android.content.Intent("ACTION_WORKER_ALARM")
        applicationContext.sendBroadcast(intent)

        // Keep alive for 1 minute or until cancelled
        delay(60000)

        return Result.success()
    }

    private fun createForegroundInfo(notificationId: Int): ForegroundInfo {
        val channelId = "alarm_channel"
        val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as
                        NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                    NotificationChannel(channelId, "Alarm", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent =
                applicationContext.packageManager.getLaunchIntentForPackage(
                                applicationContext.packageName
                        )
                        ?.apply {
                            flags =
                                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }

        val pendingIntent =
                if (openAppIntent != null) {
                    android.app.PendingIntent.getActivity(
                            applicationContext,
                            0,
                            openAppIntent,
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                                    android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                } else null

        val notification =
                NotificationCompat.Builder(applicationContext, channelId)
                        .setContentTitle("Geofence Alarm")
                        .setContentText("You are in the zone!")
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setOngoing(true)
                        .setContentIntent(pendingIntent)
                        .build()

        return ForegroundInfo(notificationId, notification)
    }
}
