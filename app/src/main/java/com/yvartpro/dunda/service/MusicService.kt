package com.yvartpro.dunda.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
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

class MusicService : Service(), AudioManager.OnAudioFocusChangeListener {

    private val binder = MusicBinder()
    private var player: MediaPlayer? = null
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

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
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNowPlayingChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> if (requestAudioFocus()) resume()
            ACTION_PAUSE -> pause()
            ACTION_NEXT -> playNext()
            ACTION_PREV -> playPrev()
        }
        return START_NOT_STICKY
    }

    fun setTrackList(newTracks: List<MusicTrack>) {
        this.tracks = newTracks
    }

    fun playTrack(track: MusicTrack) {
        if (tracks.isEmpty()) return
        currentTrackIndex = tracks.indexOf(track)
        if (currentTrackIndex == -1) return

        if (!requestAudioFocus()) return

        _currentTrack.value = track
        player?.release()
        player = MediaPlayer.create(this, track.uri).apply {
            setOnPreparedListener { 
                _duration.value = it.duration
                resume()
            }
            setOnCompletionListener { 
                playNext()
            }
        }
    }
    
    private fun resume() {
        player?.start()
        _isPlaying.value = true
        updateNotification()
        startProgressUpdater()
    }

    private fun pause() {
        player?.pause()
        _isPlaying.value = false
        abandonAudioFocus()
        updateNotification()
        stopProgressUpdater()
    }

    fun togglePlayPause() {
        if (isPlaying.value) pause() else resume()
    }

    fun playNext() {
        if (tracks.isEmpty()) return
        currentTrackIndex = if (isShuffling.value) {
            tracks.indices.random()
        } else {
            (currentTrackIndex + 1) % tracks.size
        }
        playTrack(tracks[currentTrackIndex])
    }

    fun playPrev() {
        if (tracks.isEmpty()) return
        currentTrackIndex = if (isShuffling.value) {
            tracks.indices.random()
        } else {
            if (currentTrackIndex > 0) currentTrackIndex - 1 else tracks.size - 1
        }
        playTrack(tracks[currentTrackIndex])
    }
    
    fun seekTo(position: Int) {
        player?.seekTo(position)
        _progress.value = position
    }

    fun toggleShuffle() {
        _isShuffling.value = !_isShuffling.value
    }

    fun toggleLoop() {
        _isLooping.value = !_isLooping.value
        player?.isLooping = _isLooping.value
    }

    private fun startProgressUpdater() {
        serviceScope.launch {
            while (isActive) {
                player?.let {
                    if (it.isPlaying) {
                        _progress.value = it.currentPosition
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
        currentTrack.value?.let {
            val notification = buildNowPlayingNotification(this, it, isPlaying.value)
            startForeground(NOW_PLAYING_NOTIFICATION_ID, notification)
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (isPlaying.value) pause()
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        // ... (requestAudioFocus logic remains the same)
        return true
    }

    private fun abandonAudioFocus() {
        // ... (abandonAudioFocus logic remains the same)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        player?.release()
        abandonAudioFocus()
    }
}