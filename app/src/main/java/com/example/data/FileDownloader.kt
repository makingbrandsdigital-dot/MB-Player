package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object FileDownloader {
    private const val TAG = "FileDownloader"

    suspend fun downloadTrack(
        context: Context,
        track: Track,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        
        try {
            val url = URL(track.audioUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP ${connection.responseCode}")
                return@withContext null
            }

            val fileLength = connection.contentLength
            val destinationFile = File(context.filesDir, "track_${track.id}.mp3")
            
            inputStream = connection.inputStream
            outputStream = FileOutputStream(destinationFile)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            
            while (inputStream.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    val progress = ((total * 100) / fileLength).toInt()
                    onProgress(progress)
                }
                outputStream.write(data, 0, count)
            }
            
            onProgress(100)
            Log.d(TAG, "Download complete: ${destinationFile.absolutePath}")
            destinationFile
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading track: ${track.title}", e)
            null
        } finally {
            try {
                outputStream?.close()
                inputStream?.close()
            } catch (ignored: Exception) {}
            connection?.disconnect()
        }
    }
    
    fun deleteDownloadedFile(context: Context, track: Track): Boolean {
        val file = File(context.filesDir, "track_${track.id}.mp3")
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
}
