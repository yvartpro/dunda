package com.yvartpro.dunda.logic

import android.annotation.SuppressLint
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
import android.media.audiofx.Equalizer
import com.yvartpro.dunda.service.MusicService
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import android.os.Environment
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.yvartpro.dunda.ui.component.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
  val folderPath: String,
  val isVideo: Boolean = false
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
  @SuppressLint("StaticFieldLeak")
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

  // Equalizer state
  private var equalizer: Equalizer? = null
  private val _isEqualizerEnabled = MutableStateFlow(false)
  val isEqualizerEnabled = _isEqualizerEnabled.asStateFlow()
  private val _equalizerBands = MutableStateFlow<List<EqualizerBand>>(emptyList())
  val equalizerBands = _equalizerBands.asStateFlow()
  private val _equalizerFreqRange = MutableStateFlow<Pair<Int, Int>>(0 to 0)
  val equalizerFreqRange = _equalizerFreqRange.asStateFlow()

  data class EqualizerBand(
    val index: Short,
    val centerFreq: Int,
    val level: Short
  )


  // UI-specific state
  private val _isSearch = MutableStateFlow(false)
  val isSearch = _isSearch.asStateFlow()
  private val _showSheet = MutableStateFlow(false)
  val showSheet = _showSheet.asStateFlow()
  private val _showFolderSheet = MutableStateFlow(false)
  val showFolderSheet = _showFolderSheet.asStateFlow()
  private val _shownTrack = MutableStateFlow<MusicTrack?>(null)
  val showTrack = _shownTrack.asStateFlow()

  // Extraction state
  private val _exportingTrackId = MutableStateFlow<Long?>(null)
  val exportingTrackId = _exportingTrackId.asStateFlow()
  private val _exportProgress = MutableStateFlow(0f)
  val exportProgress = _exportProgress.asStateFlow()


    private val connection = object : ServiceConnection {
      override fun onServiceConnected(className: ComponentName, service: IBinder) {
        val binder = service as MusicService.MusicBinder
        musicService = binder.getService()
        isBound = true
        observeServiceState()
        // Now that the service is connected, load the tracks with force refresh.
        loadTracks(force = true)
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
        it.audioSessionId.onEach { sessionId ->
            if (sessionId != null) setupEqualizer(sessionId)
        }.launchIn(viewModelScope)
      }
    }

    private fun setupEqualizer(sessionId: Int) {
        try {
            equalizer?.release()
            equalizer = Equalizer(0, sessionId).apply {
                enabled = _isEqualizerEnabled.value
                val bands = mutableListOf<EqualizerBand>()
                for (i in 0 until numberOfBands) {
                    val bandIndex = i.toShort()
                    bands.add(
                        EqualizerBand(
                            index = bandIndex,
                            centerFreq = getCenterFreq(bandIndex),
                            level = getBandLevel(bandIndex)
                        )
                    )
                }
                _equalizerBands.value = bands
                _equalizerFreqRange.value = bandLevelRange[0].toInt() to bandLevelRange[1].toInt()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleEqualizer() {
        _isEqualizerEnabled.value = !_isEqualizerEnabled.value
        equalizer?.enabled = _isEqualizerEnabled.value
    }

    fun setBandLevel(bandIndex: Short, level: Short) {
        equalizer?.setBandLevel(bandIndex, level)
        _equalizerBands.value = _equalizerBands.value.map {
            if (it.index == bandIndex) it.copy(level = level) else it
        }
    }

  @OptIn(UnstableApi::class)
  fun extractAudio(track: MusicTrack) {
        if (_exportingTrackId.value != null) return
        _exportingTrackId.value = track.id
        _exportProgress.value = 0f

        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val dundaDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Dunda")
                if (!dundaDir.exists()) dundaDir.mkdirs()

                // Change extension to .m4a as MP3 is not supported for output by Transformer's default muxer
                val outputFileName = "Dunda_${track.title.replace(" ", "_")}.m4a"
                val outputFile = File(dundaDir, outputFileName)

                val transformer = Transformer.Builder(context)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC) // Use AAC instead of MPEG
                    .build()

                val editedMediaItem = EditedMediaItem.Builder(MediaItem.fromUri(track.uri))
                    .setRemoveVideo(true)
                    .build()

                transformer.addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, result: ExportResult) {
                        _exportingTrackId.value = null
                        _exportProgress.value = 1f
                        Logger.d("Extract", "Extracted song: $outputFileName")
                        loadTracks(force = true) // Refresh list to see new file
                    }

                    override fun onError(
                        composition: Composition,
                        result: ExportResult,
                        exception: ExportException
                    ) {
                        _exportingTrackId.value = null
                        Logger.e("Extract","Error exporting audio: ${exception.message}")
                        exception.printStackTrace()
                    }
                })

                transformer.start(editedMediaItem, outputFile.absolutePath)

                // Progress polling loop
                while (_exportingTrackId.value == track.id) {
                    val progressHolder = androidx.media3.transformer.ProgressHolder()
                    val state = transformer.getProgress(progressHolder)
                    if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                        _exportProgress.value = progressHolder.progress / 100f
                        Logger.d("Extract", "Progress for ${track.title}: ${progressHolder.progress}%")
                    }
                    delay(500)
                }
            } catch (e: Exception) {
                _exportingTrackId.value = null
                Logger.e("Extract","Unexpected error: ${e.message}")
                e.printStackTrace()
            }
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
      musicService?.playTrackWithList(track, filtered.value)
    }

    fun togglePlayPause() { musicService?.togglePlayPause() }
    fun playNext() { musicService?.playNext() }
    fun playPrev() { musicService?.playPrev() }
    fun seekTo(position: Int) { musicService?.seekTo(position * 1000) }
    fun toggleLoop() { musicService?.toggleLoop() }
    fun toggleShuffle() { musicService?.toggleShuffle() }

    // --- Media Library Methods ---
    private fun loadTracks(force: Boolean = false) {
        if (!force && _tracks.value.isNotEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val mediaList = mutableListOf<MusicTrack>()
            val minDurationMs = 5000 // 5 seconds

            // Unified and more precise blocklist for junk folders
            val excludedPathSegments = listOf(
                "/WhatsApp Voice Notes/", 
                "/WhatsApp Business Voice Notes/", 
                "/Call Recordings/", 
                "/SoundRecorder/",
                "/Recordings/",
                "/Recordings/Music/"
            )
            val excludedTitlePrefixes = listOf("PTT-", "MSG-")

            // Query for audio files
            val audioProjection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION
            )
            val audioSelection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
            val audioSelectionArgs = arrayOf(minDurationMs.toString())
            val audioQueryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

            getApplication<Application>().contentResolver.query(
                audioQueryUri, audioProjection, audioSelection, audioSelectionArgs, null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val fullPath = cursor.getString(dataCol)
                    val title = cursor.getString(titleCol)

                    val isJunkPath = excludedPathSegments.any { fullPath.contains(it, ignoreCase = true) }
                    val isJunkTitle = excludedTitlePrefixes.any { title.startsWith(it, ignoreCase = true) }

                    if (!isJunkPath && !isJunkTitle) {
                        val id = cursor.getLong(idCol)
                        val artist = cursor.getString(artistCol)
                        val folderPath = File(fullPath).parent ?: ""
                        val uri = ContentUris.withAppendedId(audioQueryUri, id)
                        mediaList.add(MusicTrack(id, title, artist, uri, folderPath, isVideo = false))
                    }
                }
            }

            // Query for video files
            val videoProjection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION
            )
            val videoSelection = "${MediaStore.Video.Media.DURATION} >= ?"
            val videoSelectionArgs = arrayOf(minDurationMs.toString())
            val videoQueryUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

            getApplication<Application>().contentResolver.query(
                videoQueryUri, videoProjection, videoSelection, videoSelectionArgs, null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

                while (cursor.moveToNext()) {
                    val fullPath = cursor.getString(dataCol)
                    // Use the unified blocklist for videos as well
                    val isJunkPath = excludedPathSegments.any { fullPath.contains(it, ignoreCase = true) }

                    if (!isJunkPath) {
                        val id = cursor.getLong(idCol)
                        val title = cursor.getString(titleCol)
                        val artist: String? = null
                        val folderPath = File(fullPath).parent ?: ""
                        val uri = ContentUris.withAppendedId(videoQueryUri, id)
                        mediaList.add(MusicTrack(id, title, artist, uri, folderPath, isVideo = true))
                    }
                }
            }

            mediaList.sortBy { it.title }

            android.util.Log.d("MusicViewModel", "Total: ${mediaList.size}, Audio: ${mediaList.count { !it.isVideo }}, Video: ${mediaList.count { it.isVideo }}")

            _tracks.value = mediaList
            _filtered.value = mediaList
            
            musicService?.setTrackList(mediaList)
            if (mediaList.isNotEmpty()) {
                musicService?.queueRandomTrack()
            }
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
    }

    fun filter(input: String) {
        _filtered.value = if (input.isBlank()) {
            _tracks.value
        } else {
            _tracks.value.filter {
                it.title.contains(input, ignoreCase = true) || (it.artist?.contains(input, ignoreCase = true) ?: false)
            }
        }
    }
}
