package com.yvartpro.dunda.logic

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yvartpro.dunda.util.showNowPlayingNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class MusicTrack(
    val id: Long,
    val title: String,
    val artist: String?,
    val uri: Uri,
    val folderPath: String
)

data class Folder(
    val name: String,
    val path: String,
    val tracks: List<MusicTrack>,
)
class MusicViewModel(app: Application) : AndroidViewModel(app) {
    private val _tracks = MutableStateFlow<List<MusicTrack>>(emptyList())

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders = _folders.asStateFlow()

    private val _currentTrack = MutableStateFlow<MusicTrack?>(null)
    val currentTrack: StateFlow<MusicTrack?> = _currentTrack

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isSearch = MutableStateFlow(false)
    val isSearch = _isSearch.asStateFlow()

    private val _filtered = MutableStateFlow<List<MusicTrack>>(emptyList())
    val filtered = _filtered.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    private val _duration = MutableStateFlow(0f)
    val duration = _duration.asStateFlow()

    private val _isLooping = MutableStateFlow(false)
    val isLooping = _isLooping.asStateFlow()

    private val _isShuffling = MutableStateFlow(false)
    val isShuffling = _isShuffling.asStateFlow()

    private var player: MediaPlayer? = null

    private val _showSheet = MutableStateFlow(false)
    val showSheet = _showSheet.asStateFlow()

    private val _showFolderSheet = MutableStateFlow(false)
    val showFolderSheet = _showFolderSheet.asStateFlow()

    private val _shownTrack = MutableStateFlow<MusicTrack?>(null)
    val showTrack = _shownTrack.asStateFlow()

    private val audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wasPlayingBeforeTransientLoss = false
    private var isDucked = false

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (_isPlaying.value) {
                    togglePlayPause() // Pauses and abandons focus
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (_isPlaying.value) {
                    wasPlayingBeforeTransientLoss = true
                    player?.pause()
                    _isPlaying.value = false
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (_isPlaying.value) {
                    player?.setVolume(0.001f, 0.001f)
                    isDucked = true
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (isDucked) {
                    player?.setVolume(1.0f, 1.0f)
                    isDucked = false
                }
                if (wasPlayingBeforeTransientLoss) {
                    player?.start()
                    _isPlaying.value = true
                    wasPlayingBeforeTransientLoss = false
                }
            }
        }
    }

    fun toggleShownTrack(track: MusicTrack) {
        _shownTrack.value = track
    }

    fun toggleShowSheet() {
        _showSheet.value = !_showSheet.value
    }

    fun toggleShowFolderSheet() {
        _showFolderSheet.value = !_showFolderSheet.value
    }

    init {
        loadTracks()
        audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onCleared() {
        super.onCleared()
        player?.release()
        abandonAudioFocus()
    }

    private fun requestAudioFocus(): Boolean {
        val result: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            result = audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            result = audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    /**
     * Load all .mp3 files from MediaStore
     */
    private fun loadTracks() {
        viewModelScope.launch(Dispatchers.IO) {
            val musicList = mutableListOf<MusicTrack>()
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.DATA
            )
            val selection = "${MediaStore.Audio.Media.MIME_TYPE}=?"
            val selectionArgs = arrayOf("audio/mpeg") // only mp3

            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            val resolver = getApplication<Application>().contentResolver
            resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol)
                    val artist = cursor.getString(artistCol)
                    val fullPath = cursor.getString(dataCol)
                    val folderPath = File(fullPath).parent ?: ""
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    )
                    musicList.add(MusicTrack(id, title, artist, uri, folderPath))
                }
            }
            _tracks.value = musicList
            _filtered.value = musicList
        }
    }

    fun loadFolders() {
        viewModelScope.launch (Dispatchers.IO){
            loadTracks()
            loadTracks()
            val musicList = _tracks.value
            val grouped = musicList.groupBy { it.folderPath }
            _folders.value = grouped.map { (path, tracks) ->
                Folder(
                    path = path,
                    tracks = tracks,
                    name = path.substringAfterLast("/")
                )
            }
        }
    }

    fun selectFolder(folder: Folder) {
        _tracks.value = folder.tracks
        _filtered.value = _tracks.value
    }
    fun playTrack(track: MusicTrack) {
        if (!requestAudioFocus()) {
            return
        }

        if (_currentTrack.value?.id != track.id) { // Only show notification if the track is new
            showNowPlayingNotification(getApplication(), track)
        }

        player?.release()
        player = MediaPlayer.create(getApplication(), track.uri).apply {
            isLooping = false

            setOnPreparedListener {
                _duration.value = it.duration / 1000f
                it.start()
                _isPlaying.value = true
            }

            setOnCompletionListener {
                _isPlaying.value = false
                _progress.value = 0f

                if (_isLooping.value) {
                    playTrack(track)
                } else {
                    playNext()
                }
            }
        }

        // Smooth progress updater
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val p = player
                if (p != null && _isPlaying.value) {
                    try {
                        _progress.value = p.currentPosition / 1000f
                        _duration.value = p.duration / 1000f
                    } catch (_: IllegalStateException) { }
                }
                delay(500L)
            }
        }
        _currentTrack.value = track
    }

    fun seekTo(position: Int) {
        player?.seekTo(position * 1000)
        _progress.value = position.toFloat()
    }

    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                abandonAudioFocus()
            } else {
                if (requestAudioFocus()) {
                    it.start()
                    _isPlaying.value = true
                }
            }
        }
    }

    fun playNext() {
        val list = _tracks.value
        if (list.isEmpty()) return
        val currentIndex = list.indexOf(_currentTrack.value)
        val nextIndex = if (isShuffling.value) list.indices.random() else (currentIndex + 1) % list.size
        playTrack(list[nextIndex])
    }

    fun playPrev() {
        val list = _tracks.value
        if (list.isEmpty()) return
        val currentIndex = list.indexOf(_currentTrack.value)
        val prevIndex = if (isShuffling.value) list.indices.random() else (currentIndex - 1 + list.size) % list.size
        playTrack(list[prevIndex])
    }

    fun toggleLoop() {
        _isLooping.value = !_isLooping.value
        player?.isLooping = isLooping.value
    }

    fun toggleShuffle() {
        _isShuffling.value = !_isShuffling.value
    }

    fun toggleSearch() {
        _isSearch.value = !_isSearch.value
    }

    fun filter(input: String) {
        val songs = _tracks.value
        _filtered.value = if (input.isBlank()) {
            songs
        } else {
            songs.filter { track ->
                track.title.contains(input, ignoreCase = true) ||
                        (track.artist?.contains(input, ignoreCase = true) ?: false)
            }
        }
    }
}