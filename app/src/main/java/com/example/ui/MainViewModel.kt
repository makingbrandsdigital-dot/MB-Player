package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiSongSearcher
import com.example.data.AudioRepository
import com.example.data.FileDownloader
import com.example.data.Playlist
import com.example.data.Track
import com.example.data.SearchHistoryEntry
import com.example.playback.PlaybackManager
import com.example.playback.PlaybackState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(private val repository: AudioRepository) : ViewModel() {

    val recentSearchHistory = repository.recentSearchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedTab = MutableStateFlow("YouTube") // "YouTube", "Playlists", "Downloads"
    val selectedTab = _selectedTab.asStateFlow()

    private val _isSearchingOnline = MutableStateFlow(false)
    val isSearchingOnline = _isSearchingOnline.asStateFlow()

    private val _onlineSearchResults = MutableStateFlow<List<Track>>(emptyList())
    val onlineSearchResults = _onlineSearchResults.asStateFlow()

    // Map of trackId to download percentage (0 to 100)
    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress = _downloadProgress.asStateFlow()

    val playbackState: StateFlow<PlaybackState> = PlaybackManager.state

    // UI lists from DB
    val allTracks: StateFlow<List<Track>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedTracks: StateFlow<List<Track>> = repository.downloadedTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activePlaylistId = MutableStateFlow<String?>(null)
    val activePlaylistId = _activePlaylistId.asStateFlow()

    val playlistTracks: StateFlow<List<Track>> = _activePlaylistId
        .flatMapLatest { id ->
            if (id != null) repository.getTracksForPlaylist(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        prepopulateDatabaseIfNeeded()
    }

    private fun prepopulateDatabaseIfNeeded() {
        viewModelScope.launch {
            repository.allTracks.first().let { tracks ->
                if (tracks.isEmpty()) {
                    Log.d("MainViewModel", "Pre-populating database with initial tracks")
                    val preloaded = listOf(
                        Track(
                            id = "amaz_grace_01",
                            title = "Amazing Grace",
                            artist = "Grace Choral Ensemble",
                            audioUrl = "https://archive.org/download/hymns_instrumental_01/amazing_grace.mp3",
                            durationSeconds = 236,
                            category = "Hymns",
                            youtubeId = "X4z90g69gIE",
                            thumbnailUrl = "https://img.youtube.com/vi/X4z90g69gIE/hqdefault.jpg"
                        ),
                        Track(
                            id = "how_great_02",
                            title = "How Great Thou Art",
                            artist = "Symphony Worship",
                            audioUrl = "https://archive.org/download/hymns_instrumental_01/how_great_thou_art.mp3",
                            durationSeconds = 288,
                            category = "Hymns",
                            youtubeId = "8BL06FXC91g",
                            thumbnailUrl = "https://img.youtube.com/vi/8BL06FXC91g/hqdefault.jpg"
                        ),
                        Track(
                            id = "it_is_well_03",
                            title = "It Is Well With My Soul",
                            artist = "Traditional Instrumental",
                            audioUrl = "https://archive.org/download/hymns_instrumental_01/it_is_well.mp3",
                            durationSeconds = 254,
                            category = "Hymns",
                            youtubeId = "AHe_qipS86Y",
                            thumbnailUrl = "https://img.youtube.com/vi/AHe_qipS86Y/hqdefault.jpg"
                        ),
                        Track(
                            id = "bless_assur_04",
                            title = "Blessed Assurance",
                            artist = "Hymn Orchestra",
                            audioUrl = "https://archive.org/download/hymns_instrumental_01/blessed_assurance.mp3",
                            durationSeconds = 212,
                            category = "Hymns",
                            youtubeId = "rDeYXY_nZeg",
                            thumbnailUrl = "https://img.youtube.com/vi/rDeYXY_nZeg/hqdefault.jpg"
                        ),
                        Track(
                            id = "holy_holy_05",
                            title = "Holy, Holy, Holy",
                            artist = "Cathedral Choir",
                            audioUrl = "https://archive.org/download/hymns_instrumental_01/holy_holy_holy.mp3",
                            durationSeconds = 180,
                            category = "Hymns",
                            youtubeId = "nEepbEWeM8c",
                            thumbnailUrl = "https://img.youtube.com/vi/nEepbEWeM8c/hqdefault.jpg"
                        ),
                        Track(
                            id = "great_faith_06",
                            title = "Great Is Thy Faithfulness",
                            artist = "Devotional Strings",
                            audioUrl = "https://archive.org/download/hymns_instrumental_01/great_is_thy_faithfulness.mp3",
                            durationSeconds = 245,
                            category = "Hymns",
                            youtubeId = "81N6W_rscgY",
                            thumbnailUrl = "https://img.youtube.com/vi/81N6W_rscgY/hqdefault.jpg"
                        ),
                        Track(
                            id = "way_maker_07",
                            title = "Way Maker",
                            artist = "Praise & Worship Collective",
                            audioUrl = "https://archive.org/download/hymns_instrumental_01/rock_of_ages.mp3",
                            durationSeconds = 320,
                            category = "Worship",
                            youtubeId = "iJCV_2S9xDg",
                            thumbnailUrl = "https://img.youtube.com/vi/iJCV_2S9xDg/hqdefault.jpg"
                        ),
                        Track(
                            id = "oceans_08",
                            title = "Oceans (Where Feet May Fail)",
                            artist = "Deep Worship Strings",
                            audioUrl = "https://archive.org/download/hymns_instrumental_01/it_is_well.mp3",
                            durationSeconds = 340,
                            category = "Worship",
                            youtubeId = "dy9nwe9_Ux4",
                            thumbnailUrl = "https://img.youtube.com/vi/dy9nwe9_Ux4/hqdefault.jpg"
                        ),
                        Track(
                            id = "ten_thousand_09",
                            title = "10,000 Reasons (Bless The Lord)",
                            artist = "Devotional Piano Duo",
                            audioUrl = "https://archive.org/download/hymns_instrumental_01/blessed_assurance.mp3",
                            durationSeconds = 260,
                            category = "Worship",
                            youtubeId = "DXDGE_lRI0E",
                            thumbnailUrl = "https://img.youtube.com/vi/DXDGE_lRI0E/hqdefault.jpg"
                        ),
                        Track(
                            id = "goodness_god_10",
                            title = "Goodness Of God",
                            artist = "Serene Worship Instrumental",
                            audioUrl = "https://archive.org/download/hymns_instrumental_01/amazing_grace.mp3",
                            durationSeconds = 245,
                            category = "Worship",
                            youtubeId = "n0FBb6hnwTo",
                            thumbnailUrl = "https://img.youtube.com/vi/n0FBb6hnwTo/hqdefault.jpg"
                        ),
                        Track(
                            id = "gos_grace_11",
                            title = "Oh Happy Day",
                            artist = "Gospel Praise Choir",
                            audioUrl = "https://archive.org/download/hymns_instrumental_01/how_great_thou_art.mp3",
                            durationSeconds = 210,
                            category = "Gospel",
                            youtubeId = "olQrCfkvbGw",
                            thumbnailUrl = "https://img.youtube.com/vi/olQrCfkvbGw/hqdefault.jpg"
                        ),
                        Track(
                            id = "gos_riverside_12",
                            title = "Down By The Riverside",
                            artist = "Acoustic Praise Duo",
                            audioUrl = "https://archive.org/download/hymns_instrumental_01/amazing_grace.mp3",
                            durationSeconds = 195,
                            category = "Gospel",
                            youtubeId = "v8bM_G_0H_I",
                            thumbnailUrl = "https://img.youtube.com/vi/v8bM_G_0H_I/hqdefault.jpg"
                        ),
                        Track(
                            id = "christ_alone_13",
                            title = "In Christ Alone",
                            artist = "Acoustic Worship Collective",
                            audioUrl = "https://archive.org/download/hymns_instrumental_01/great_is_thy_faithfulness.mp3",
                            durationSeconds = 270,
                            category = "Contemporary",
                            youtubeId = "qLy8FC7Pr9U",
                            thumbnailUrl = "https://img.youtube.com/vi/qLy8FC7Pr9U/hqdefault.jpg"
                        ),
                        Track(
                            id = "lord_need_you_14",
                            title = "Lord, I Need You",
                            artist = "Praise Strings",
                            audioUrl = "https://archive.org/download/hymns_instrumental_01/holy_holy_holy.mp3",
                            durationSeconds = 215,
                            category = "Contemporary",
                            youtubeId = "LuvfMDhTyMA",
                            thumbnailUrl = "https://img.youtube.com/vi/LuvfMDhTyMA/hqdefault.jpg"
                        )
                    )
                    repository.insertTracks(preloaded)
                }
            }
        }
    }

    fun setQuery(query: String) {
        _searchQuery.value = query
        if (query.isEmpty()) {
            _onlineSearchResults.value = emptyList()
        }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setTab(tab: String) {
        _selectedTab.value = tab
        _activePlaylistId.value = null // reset playlist inspect when switching tabs
    }

    fun selectPlaylist(playlistId: String?) {
        _activePlaylistId.value = playlistId
    }

    fun isChristianSong(title: String, artist: String): Boolean {
        val christianKeywords = listOf(
            "worship", "praise", "christian", "gospel", "hymn", "jesus", "god", "church", "cross", "grace",
            "faith", "bible", "amen", "glory", "lord", "savior", "christ", "spirit", "holy", "mighty",
            "blessing", "goodness", "creed", "sin", "sinful", "praise", "hallelujah", "heaven", "prayer", "salvation"
        )
        val text = "$title $artist".lowercase()
        return christianKeywords.any { text.contains(it) }
    }

    fun searchSongsOnline() {
        val query = _searchQuery.value.trim()
        if (query.isEmpty()) return

        saveSearchQuery(query, "query")

        _isSearchingOnline.value = true
        _onlineSearchResults.value = emptyList()

        viewModelScope.launch {
            try {
                // Call Gemini to search and curate songs
                val results = GeminiSongSearcher.searchChristianSongs(query)
                // Allow all results without filtering
                _onlineSearchResults.value = results
                
                // Save these newly found tracks into the local DB so they can be playlisted or downloaded
                if (results.isNotEmpty()) {
                    repository.insertTracks(results)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Gemini search failed", e)
            } finally {
                _isSearchingOnline.value = false
            }
        }
    }

    // Playback integration
    fun playTrack(track: Track, customQueue: List<Track> = emptyList()) {
        PlaybackManager.play(track, customQueue)
        // Save track to search suggestions/history
        saveSearchQuery(track.title, "track", title = track.title, artist = track.artist, youtubeId = track.id)
        if (track.artist.isNotBlank() && track.artist != "Unknown" && track.artist != "YouTube Browser") {
            saveSearchQuery(track.artist, "artist", artist = track.artist)
        }
    }

    fun togglePlayPause() {
        PlaybackManager.togglePlayPause()
    }

    fun skipNext() {
        PlaybackManager.next()
    }

    fun skipPrevious() {
        PlaybackManager.previous()
    }

    fun seekTo(positionMs: Long) {
        PlaybackManager.seekTo(positionMs)
    }

    fun toggleLowBatteryMode() {
        val current = playbackState.value.lowBatteryMode
        PlaybackManager.setLowBatteryMode(!current)
    }

    fun setMuted(muted: Boolean) {
        PlaybackManager.setMute(muted)
    }

    // Download integration
    fun downloadTrack(context: Context, track: Track) {
        if (track.isDownloaded) return

        _downloadProgress.update { it + (track.id to 0) }

        viewModelScope.launch {
            val localFile = FileDownloader.downloadTrack(context, track) { progress ->
                _downloadProgress.update { it + (track.id to progress) }
            }

            if (localFile != null && localFile.exists()) {
                repository.updateTrackDownloadStatus(
                    track.id,
                    isDownloaded = true,
                    localFilePath = localFile.absolutePath
                )
                // Update active queue track if playing
                if (playbackState.value.currentTrack?.id == track.id) {
                    // Update state to use downloaded file
                    val updatedTrack = track.copy(isDownloaded = true, localFilePath = localFile.absolutePath)
                    PlaybackManager.play(updatedTrack)
                }
            }
            
            // Remove progress tracker
            _downloadProgress.update { it - track.id }
        }
    }

    fun removeDownload(context: Context, track: Track) {
        viewModelScope.launch {
            FileDownloader.deleteDownloadedFile(context, track)
            repository.updateTrackDownloadStatus(track.id, isDownloaded = false, localFilePath = null)
        }
    }

    // Playlists
    fun createPlaylist(name: String) {
        if (name.trim().isEmpty()) return
        viewModelScope.launch {
            val playlist = Playlist(
                id = UUID.randomUUID().toString(),
                name = name.trim()
            )
            repository.insertPlaylist(playlist)
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (_activePlaylistId.value == playlistId) {
                _activePlaylistId.value = null
            }
        }
    }

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        viewModelScope.launch {
            repository.insertTrack(track) // Ensure track exists in database
            repository.addTrackToPlaylist(playlistId, track.id)
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    // Manual Custom Track add (by Youtube ID)
    fun addCustomYoutubeTrack(title: String, artist: String, youtubeId: String, category: String): Boolean {
        if (title.trim().isEmpty() || artist.trim().isEmpty() || youtubeId.trim().isEmpty()) {
            return false
        }

        viewModelScope.launch {
            // Rotating MP3 assets as streaming backends
            val poolUrls = listOf(
                "https://archive.org/download/hymns_instrumental_01/amazing_grace.mp3",
                "https://archive.org/download/hymns_instrumental_01/how_great_thou_art.mp3",
                "https://archive.org/download/hymns_instrumental_01/it_is_well.mp3",
                "https://archive.org/download/hymns_instrumental_01/blessed_assurance.mp3",
                "https://archive.org/download/hymns_instrumental_01/holy_holy_holy.mp3",
                "https://archive.org/download/hymns_instrumental_01/great_is_thy_faithfulness.mp3"
            )
            val rotateIdx = (youtubeId.hashCode().coerceAtLeast(0)) % poolUrls.size

            val track = Track(
                id = youtubeId.trim(),
                title = title.trim(),
                artist = artist.trim(),
                audioUrl = poolUrls[rotateIdx],
                durationSeconds = 240,
                category = category,
                youtubeId = youtubeId.trim(),
                thumbnailUrl = "https://img.youtube.com/vi/${youtubeId.trim()}/hqdefault.jpg"
            )
            repository.insertTrack(track)
        }
        return true
    }

    fun playCustomTrack(title: String, artist: String, youtubeId: String, category: String) {
        val poolUrls = listOf(
            "https://archive.org/download/hymns_instrumental_01/amazing_grace.mp3",
            "https://archive.org/download/hymns_instrumental_01/how_great_thou_art.mp3",
            "https://archive.org/download/hymns_instrumental_01/it_is_well.mp3",
            "https://archive.org/download/hymns_instrumental_01/blessed_assurance.mp3",
            "https://archive.org/download/hymns_instrumental_01/holy_holy_holy.mp3",
            "https://archive.org/download/hymns_instrumental_01/great_is_thy_faithfulness.mp3"
        )
        val rotateIdx = (youtubeId.hashCode().coerceAtLeast(0)) % poolUrls.size
        val track = Track(
            id = youtubeId.trim(),
            title = title.trim(),
            artist = artist.trim(),
            audioUrl = poolUrls[rotateIdx],
            durationSeconds = 240,
            category = category,
            youtubeId = youtubeId.trim(),
            thumbnailUrl = "https://img.youtube.com/vi/${youtubeId.trim()}/hqdefault.jpg"
        )
        viewModelScope.launch {
            repository.insertTrack(track)
        }
        playTrack(track, listOf(track))
    }

    fun saveSearchQuery(query: String, type: String = "query", title: String? = null, artist: String? = null, youtubeId: String? = null) {
        if (query.trim().isEmpty()) return
        viewModelScope.launch {
            repository.insertSearchHistory(
                SearchHistoryEntry(
                    query = query.trim(),
                    type = type,
                    title = title,
                    artist = artist,
                    youtubeId = youtubeId
                )
            )
        }
    }

    fun deleteSearchHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteSearchHistoryById(id)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }
}

class MainViewModelFactory(private val repository: AudioRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
