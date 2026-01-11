package com.yvartpro.dunda.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.yvartpro.dunda.MainActivity
import com.yvartpro.dunda.R
import com.yvartpro.dunda.logic.MusicTrack
import com.yvartpro.dunda.service.MusicService

const val NOW_PLAYING_CHANNEL_ID = "now_playing_channel"
const val NOW_PLAYING_NOTIFICATION_ID = 1

fun createNowPlayingChannel(context: Context) {
  val name = "Now Playing"
  val descriptionText = "Notifications for the currently playing song"
  val importance = NotificationManager.IMPORTANCE_LOW
  val channel = NotificationChannel(NOW_PLAYING_CHANNEL_ID, name, importance).apply {
    description = descriptionText
  }
  val notificationManager: NotificationManager =
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  notificationManager.createNotificationChannel(channel)
}

fun buildNowPlayingNotification(
    context: Context,
    track: MusicTrack,
    isPlaying: Boolean
): Notification {
  val intent = Intent(context, MainActivity::class.java).apply {
    action = Intent.ACTION_MAIN
    addCategory(Intent.CATEGORY_LAUNCHER)
  }
  val pendingIntent: PendingIntent = PendingIntent.getActivity(
    context, 0, intent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
  )

  val nextAction = NotificationCompat.Action(
    R.drawable.skip_next,
    "Next",
    PendingIntent.getService(
      context, 0, Intent(context, MusicService::class.java).setAction(MusicService.ACTION_NEXT),
      PendingIntent.FLAG_IMMUTABLE
    )
  )

  val prevAction = NotificationCompat.Action(
    R.drawable.skip_prev,
    "Prev",
    PendingIntent.getService(
      context, 0, Intent(context, MusicService::class.java).setAction(MusicService.ACTION_PREV),
      PendingIntent.FLAG_IMMUTABLE
    )
  )

  val playPauseAction = if (isPlaying) {
    NotificationCompat.Action(
      R.drawable.pause,
      "Pause",
      PendingIntent.getService(
        context, 0, Intent(context, MusicService::class.java).setAction(MusicService.ACTION_PAUSE),
        PendingIntent.FLAG_IMMUTABLE
      )
    )
  } else {
    NotificationCompat.Action(
      R.drawable.play,
      "Play",
      PendingIntent.getService(
        context, 0, Intent(context, MusicService::class.java).setAction(MusicService.ACTION_PLAY),
        PendingIntent.FLAG_IMMUTABLE
      )
    )
  }

  return NotificationCompat.Builder(context, NOW_PLAYING_CHANNEL_ID)
    .setSmallIcon(R.mipmap.ic_launcher)
    .setContentTitle("Now Playing")
    .setContentText("${track.title} - ${track.artist ?: "Unknown"}")
    .setContentIntent(pendingIntent)
    .setOngoing(isPlaying)
    .addAction(prevAction)
    .addAction(playPauseAction)
    .addAction(nextAction)
    .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
      .setShowActionsInCompactView(0)
    )
    .build()
}