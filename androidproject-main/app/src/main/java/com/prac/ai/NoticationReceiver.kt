package com.prac.ai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val notificationManager = context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = intent?.getIntExtra("NOTIFICATION_ID", 0) ?: 0
        val notificationTitle = intent?.getStringExtra("NOTIFICATION_TITLE") ?: "none"
        val notificationContent = intent?.getStringExtra("NOTIFICATION_CONTENT") ?: "none"
        val channelId = "daily_notification_channel"

        // Create a notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Daily Notification", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.baseline_alarm_24)
            .setContentTitle(notificationTitle)
            .setContentText(notificationContent)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}

