package com.yvartpro.dunda.logic

import android.app.Application
import android.content.ContentUris
import android.media.MediaPlayer
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MusicTrack(
    val id: Long,
    val title: String,
    val artist: String?,
    val uri: Uri
)

class MusicViewModel(app: Application) : AndroidViewModel(app) {
    private val _tracks = MutableStateFlow<List<MusicTrack>>(emptyList())

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _currentTrack = MutableStateFlow<MusicTrack?>(null)
    val currentTrack: StateFlow<MusicTrack?> = _currentTrack

    private val _isPlaying = MutableStateFlow(true)
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

    private val _shownTrack = MutableStateFlow<MusicTrack?>(null)
    val showTrack = _shownTrack.asStateFlow()

    fun toggleShownTrack(track: MusicTrack) {
        _shownTrack.value = track
    }

    fun toggleShowSheet() {
        _showSheet.value = !_showSheet.value
    }

    init {
        loadTracks()
    }

    /**
     * Load all .mp3 files from MediaStore
     */
    private fun loadTracks() {
        _loading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val musicList = mutableListOf<MusicTrack>()
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.MIME_TYPE
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

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol)
                    val artist = cursor.getString(artistCol)
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    )
                    musicList.add(MusicTrack(id, title, artist, uri))
                }
            }
            _tracks.value = musicList
            _filtered.value = musicList
        }
        _loading.value = false
    }

    fun playTrack(track: MusicTrack) {
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
            } else {
                it.start()
                _isPlaying.value = true
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