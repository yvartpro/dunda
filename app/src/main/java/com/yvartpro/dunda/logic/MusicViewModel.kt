package com.yvartpro.dunda.logic

import android.app.Application
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yvartpro.dunda.service.MusicService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    // Media library state
    private val _tracks = MutableStateFlow<List<MusicTrack>>(emptyList())
    val tracks = _tracks.asStateFlow()
    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders = _folders.asStateFlow()
    private val _filtered = MutableStateFlow<List<MusicTrack>>(emptyList())
    val filtered = _filtered.asStateFlow()

    // Service connection
    private var musicService: MusicService? = null
    private var isBound = false

    // Playback state - Now observed from the service
    private val _currentTrack = MutableStateFlow<MusicTrack?>(null)
    val currentTrack = _currentTrack.asStateFlow()
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()
    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()
    private val _duration = MutableStateFlow(0f)
    val duration = _duration.asStateFlow()
    private val _isLooping = MutableStateFlow(false)
    val isLooping = _isLooping.asStateFlow()
    private val _isShuffling = MutableStateFlow(false)
    val isShuffling = _isShuffling.asStateFlow()

    // UI-specific state
    private val _isSearch = MutableStateFlow(false)
    val isSearch = _isSearch.asStateFlow()
    private val _showSheet = MutableStateFlow(false)
    val showSheet = _showSheet.asStateFlow()
    private val _showFolderSheet = MutableStateFlow(false)
    val showFolderSheet = _showFolderSheet.asStateFlow()
    private val _shownTrack = MutableStateFlow<MusicTrack?>(null)
    val showTrack = _shownTrack.asStateFlow()


    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true
            observeServiceState()
            // Now that the service is connected, load the tracks.
            loadTracks()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    private fun observeServiceState() {
        musicService?.let {
            it.currentTrack.onEach { track -> _currentTrack.value = track }.launchIn(viewModelScope)
            it.isPlaying.onEach { playing -> _isPlaying.value = playing }.launchIn(viewModelScope)
            it.progress.onEach { prog -> _progress.value = prog / 1000f }.launchIn(viewModelScope)
            it.duration.onEach { dur -> _duration.value = dur / 1000f }.launchIn(viewModelScope)
            it.isShuffling.onEach { shuffling -> _isShuffling.value = shuffling }.launchIn(viewModelScope)
            it.isLooping.onEach { looping -> _isLooping.value = looping }.launchIn(viewModelScope)
        }
    }

    init {
        // Just bind to the service. Track loading will start when the service is connected.
        Intent(app, MusicService::class.java).also { intent ->
            app.startService(intent) // Start the service to keep it running
            app.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onCleared() {
        if (isBound) {
            getApplication<Application>().unbindService(connection)
            isBound = false
        }
        super.onCleared()
    }

    // --- UI State Control Methods ---
    fun toggleShownTrack(track: MusicTrack) { _shownTrack.value = track }
    fun toggleShowSheet() { _showSheet.value = !_showSheet.value }
    fun toggleShowFolderSheet() { _showFolderSheet.value = !_showFolderSheet.value }
    fun toggleSearch() { _isSearch.value = !_isSearch.value }

    // --- Playback Control Methods (delegating to service) ---
    fun playTrack(track: MusicTrack) {
        musicService?.setTrackList(filtered.value)
        musicService?.playTrack(track)
    }

    fun togglePlayPause() { musicService?.togglePlayPause() }
    fun playNext() { musicService?.playNext() }
    fun playPrev() { musicService?.playPrev() }
    fun seekTo(position: Int) { musicService?.seekTo(position * 1000) }
    fun toggleLoop() { musicService?.toggleLoop() }
    fun toggleShuffle() { musicService?.toggleShuffle() }

    // --- Media Library Methods ---
    private fun loadTracks() {
        // Prevent re-loading if tracks are already loaded
        if (_tracks.value.isNotEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val musicList = mutableListOf<MusicTrack>()
            val projection = arrayOf(
                MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.DATA
            )
            val selection = "${MediaStore.Audio.Media.MIME_TYPE}=?"
            val selectionArgs = arrayOf("audio/mpeg")
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            getApplication<Application>().contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, sortOrder
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
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    musicList.add(MusicTrack(id, title, artist, uri, folderPath))
                }
            }
            _tracks.value = musicList
            _filtered.value = musicList
            
            // Since this is only called after the service is bound, musicService is guaranteed to be non-null
            musicService?.setTrackList(musicList)
            musicService?.queueRandomTrack()
        }
    }

    fun loadFolders() {
        viewModelScope.launch(Dispatchers.IO) {
            val musicList = _tracks.value
            val grouped = musicList.groupBy { it.folderPath }
            _folders.value = grouped.map { (path, tracks) ->
                Folder(name = path.substringAfterLast("/"), path = path, tracks = tracks)
            }
        }
    }

    fun selectFolder(folder: Folder) {
        _filtered.value = folder.tracks
        musicService?.setTrackList(folder.tracks)
    }

    fun filter(input: String) {
        _filtered.value = if (input.isBlank()) {
            _tracks.value
        } else {
            _tracks.value.filter {
                it.title.contains(input, ignoreCase = true) || (it.artist?.contains(input, ignoreCase = true) ?: false)
            }
        }
        musicService?.setTrackList(_filtered.value)
    }
}