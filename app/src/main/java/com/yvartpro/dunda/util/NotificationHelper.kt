package com.yvartpro.dunda.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yvartpro.dunda.R
import com.yvartpro.dunda.logic.MusicTrack

fun showNowPlayingNotification(context: Context, track: MusicTrack) {
    val channelId = "now_playing_channel"

  val name = "Now Playing"
  val descriptionText = "Notifications for the currently playing song"
  val importance = NotificationManager.IMPORTANCE_LOW
  val channel = NotificationChannel(channelId, name, importance).apply {
      description = descriptionText
  }
  val notificationManager: NotificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  notificationManager.createNotificationChannel(channel)

  val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("Now Playing")
        .setContentText(track.title)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOnlyAlertOnce(true)
        .setAutoCancel(true)

    with(NotificationManagerCompat.from(context)) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notify(1, builder.build())
        }
    }
}