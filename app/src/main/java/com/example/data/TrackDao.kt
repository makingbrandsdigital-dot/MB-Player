package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks")
    fun getAllTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE category = :category")
    fun getTracksByCategory(category: String): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE isDownloaded = 1")
    fun getDownloadedTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): Track?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: Track)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTracks(tracks: List<Track>)

    @Query("UPDATE tracks SET isDownloaded = :isDownloaded, localFilePath = :localFilePath WHERE id = :id")
    suspend fun updateTrackDownloadStatus(id: String, isDownloaded: Boolean, localFilePath: String?)

    @Delete
    suspend fun deleteTrack(track: Track)
}
