package com.example.data

import kotlinx.coroutines.flow.Flow

class AudioRepository(
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao,
    private val searchHistoryDao: SearchHistoryDao
) {
    val allTracks: Flow<List<Track>> = trackDao.getAllTracks()
    val downloadedTracks: Flow<List<Track>> = trackDao.getDownloadedTracks()

    // Search History
    val recentSearchHistory: Flow<List<SearchHistoryEntry>> = searchHistoryDao.getRecentSearchHistory()

    suspend fun insertSearchHistory(entry: SearchHistoryEntry) {
        // To avoid exact duplicates under same type, delete existing matching query first
        searchHistoryDao.deleteByQueryAndType(entry.query, entry.type)
        searchHistoryDao.insertSearchHistory(entry)
    }

    suspend fun deleteSearchHistoryById(id: Long) {
        searchHistoryDao.deleteSearchHistoryById(id)
    }

    suspend fun clearSearchHistory() {
        searchHistoryDao.clearSearchHistory()
    }

    fun getTracksByCategory(category: String): Flow<List<Track>> {
        return trackDao.getTracksByCategory(category)
    }

    suspend fun getTrackById(id: String): Track? {
        return trackDao.getTrackById(id)
    }

    suspend fun insertTrack(track: Track) {
        trackDao.insertTrack(track)
    }

    suspend fun insertTracks(tracks: List<Track>) {
        trackDao.insertTracks(tracks)
    }

    suspend fun updateTrackDownloadStatus(id: String, isDownloaded: Boolean, localFilePath: String?) {
        trackDao.updateTrackDownloadStatus(id, isDownloaded, localFilePath)
    }

    suspend fun deleteTrack(track: Track) {
        trackDao.deleteTrack(track)
    }

    // Playlists
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    suspend fun insertPlaylist(playlist: Playlist) {
        playlistDao.insertPlaylist(playlist)
    }

    suspend fun deletePlaylist(playlistId: String) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: String, trackId: String) {
        playlistDao.addTrackToPlaylist(PlaylistTrackCrossRef(playlistId, trackId))
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    fun getTracksForPlaylist(playlistId: String): Flow<List<Track>> {
        return playlistDao.getTracksForPlaylist(playlistId)
    }
}
