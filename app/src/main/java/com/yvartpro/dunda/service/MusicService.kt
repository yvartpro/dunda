package com.yvartpro.dunda.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.yvartpro.dunda.logic.MusicTrack
import com.yvartpro.dunda.util.NOW_PLAYING_NOTIFICATION_ID
import com.yvartpro.dunda.util.buildNowPlayingNotification
import com.yvartpro.dunda.util.createNowPlayingChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicService : Service() {

  private val binder = MusicBinder()
  private var player: ExoPlayer? = null

  private val serviceJob = Job()
  private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

  private var tracks: List<MusicTrack> = emptyList()
  private var currentTrackIndex: Int = -1

  // State flows to be observed by the ViewModel
  private val _currentTrack = MutableStateFlow<MusicTrack?>(null)
  val currentTrack = _currentTrack.asStateFlow()
  private val _isPlaying = MutableStateFlow(false)
  val isPlaying = _isPlaying.asStateFlow()
  private val _progress = MutableStateFlow(0)
  val progress = _progress.asStateFlow()
  private val _duration = MutableStateFlow(0)
  val duration = _duration.asStateFlow()
  private val _isShuffling = MutableStateFlow(false)
  val isShuffling = _isShuffling.asStateFlow()
  private val _isLooping = MutableStateFlow(false)
  val isLooping = _isLooping.asStateFlow()
  private val _audioSessionId = MutableStateFlow<Int?>(null)
  val audioSessionId = _audioSessionId.asStateFlow()

  inner class MusicBinder : Binder() {
    fun getService(): MusicService = this@MusicService
  }

  companion object {
    const val ACTION_PLAY = "com.yvartpro.dunda.service.ACTION_PLAY"
    const val ACTION_PAUSE = "com.yvartpro.dunda.service.ACTION_PAUSE"
    const val ACTION_NEXT = "com.yvartpro.dunda.service.ACTION_NEXT"
    const val ACTION_PREV = "com.yvartpro.dunda.service.ACTION_PREV"
  }

  override fun onBind(intent: Intent): IBinder = binder

  override fun onCreate() {
    super.onCreate()
    createNowPlayingChannel(this)
    initializePlayer()
  }

  private fun initializePlayer() {
    val audioAttributes = AudioAttributes.Builder()
      .setUsage(C.USAGE_MEDIA)
      .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
      .build()

    player = ExoPlayer.Builder(this)
      .setAudioAttributes(audioAttributes, true) // true = handle audio focus automatically
      .build()
      .apply {
        repeatMode = if (_isLooping.value) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        shuffleModeEnabled = _isShuffling.value
        addListener(object : Player.Listener {
          override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
              startProgressUpdater()
              updateNotification()
            } else {
              stopProgressUpdater()
              updateNotification()
            }
          }

          override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
             val index = currentMediaItemIndex
             if (index >= 0 && index < tracks.size) {
                 currentTrackIndex = index
                 _currentTrack.value = tracks[index]
                 _duration.value = duration.toInt()
                 updateNotification()
             }
          }

          override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _duration.value = duration.toInt()
                _audioSessionId.value = audioSessionId
            }
          }
        })
      }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_PLAY -> resume()
      ACTION_PAUSE -> pause()
      ACTION_NEXT -> playNext()
      ACTION_PREV -> playPrev()
    }
    return START_NOT_STICKY
  }

  fun setTrackList(newTracks: List<MusicTrack>) {
    this.tracks = newTracks
    serviceScope.launch {
        player?.setMediaItems(newTracks.map { MediaItem.fromUri(it.uri) })
        player?.prepare()
    }
  }

  fun playTrack(track: MusicTrack) {
    if (tracks.isEmpty()) return
    currentTrackIndex = tracks.indexOf(track)
    if (currentTrackIndex == -1) return

    serviceScope.launch {
        player?.let {
            it.seekTo(currentTrackIndex, 0L)
            it.playWhenReady = true
        }
    }
  }

  fun queueRandomTrack() {
    if (tracks.isEmpty()) return
    val randomIndex = tracks.indices.random()
    currentTrackIndex = randomIndex
    serviceScope.launch {
        player?.let {
            it.seekTo(currentTrackIndex, 0L)
            it.playWhenReady = false
        }
    }
  }

  private fun resume() {
    serviceScope.launch { player?.play() }
  }

  private fun pause() {
    serviceScope.launch { player?.pause() }
  }

  fun togglePlayPause() {
    serviceScope.launch {
        player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }
  }

  fun playNext() {
    serviceScope.launch { player?.seekToNext() }
  }

  fun playPrev() {
    serviceScope.launch { player?.seekToPrevious() }
  }
  
  fun seekTo(position: Int) {
    serviceScope.launch {
        player?.seekTo(position.toLong())
        _progress.value = position
    }
  }

  fun toggleShuffle() {
    _isShuffling.value = !_isShuffling.value
    serviceScope.launch { player?.shuffleModeEnabled = _isShuffling.value }
  }

  fun toggleLoop() {
    _isLooping.value = !_isLooping.value
    serviceScope.launch {
        player?.repeatMode = if (_isLooping.value) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }
  }

  private fun startProgressUpdater() {
    stopProgressUpdater() // Ensure only one updater is running
    serviceScope.launch {
      while (isActive) {
        player?.let {
          if (it.isPlaying) {
            _progress.value = it.currentPosition.toInt()
          }
        }
        delay(500)
      }
    }
  }

  private fun stopProgressUpdater() {
    serviceScope.coroutineContext.cancelChildren()
  }

  private fun updateNotification() {
    _currentTrack.value?.let { track ->
      val notification = buildNowPlayingNotification(this, track, _isPlaying.value)
      startForeground(NOW_PLAYING_NOTIFICATION_ID, notification)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    serviceJob.cancel()
    player?.release()
    player = null
  }
}