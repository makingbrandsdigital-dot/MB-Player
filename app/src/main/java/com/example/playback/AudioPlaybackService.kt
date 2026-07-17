package com.example.playback

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class AudioPlaybackService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "grace_audio_playback_channel"

        const val ACTION_PLAY_PAUSE = "com.example.playback.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.playback.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.playback.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.example.playback.ACTION_STOP"

        fun startService(context: Context) {
            val intent = Intent(context, AudioPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AudioPlaybackService::class.java)
            context.stopService(intent)
        }

        fun stopForegroundState(context: Context) {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                putExtra("STOP_FOREGROUND_ONLY", true)
            }
            context.startService(intent)
        }
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundWithPlaceholder()
        
        // Observe PlaybackState to dynamically update notification
        serviceScope.launch {
            PlaybackManager.state.collectLatest { state ->
                if (state.currentTrack != null) {
                    showNotification(state.currentTrack, state.isPlaying)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                    stopSelf()
                }
            }
        }
    }

    private fun startForegroundWithPlaceholder() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Audio Playback")
            .setContentText("Initializing...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val stopForegroundOnly = intent.getBooleanExtra("STOP_FOREGROUND_ONLY", false)
            if (stopForegroundOnly) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(false)
                }
                return START_STICKY
            }

            when (intent.action) {
                ACTION_PLAY_PAUSE -> PlaybackManager.togglePlayPause()
                ACTION_NEXT -> PlaybackManager.next()
                ACTION_PREVIOUS -> PlaybackManager.previous()
                ACTION_STOP -> PlaybackManager.stop()
            }
        }

        // Keep service in foreground if something is active
        val currentTrack = PlaybackManager.state.value.currentTrack
        val isPlaying = PlaybackManager.state.value.isPlaying
        if (currentTrack != null) {
            showNotification(currentTrack, isPlaying)
        }

        return START_STICKY
    }

    private fun showNotification(track: Track, isPlaying: Boolean) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action Intents
        val playPauseIntent = Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE }
        val pPlayPause = PendingIntent.getService(this, 1, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val nextIntent = Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_NEXT }
        val pNext = PendingIntent.getService(this, 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val prevIntent = Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_PREVIOUS }
        val pPrev = PendingIntent.getService(this, 3, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_STOP }
        val pStop = PendingIntent.getService(this, 4, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val iconRes = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(track.title)
            .setContentText(track.artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "Previous", pPrev)
            .addAction(iconRes, if (isPlaying) "Pause" else "Play", pPlayPause)
            .addAction(android.R.drawable.ic_media_next, "Next", pNext)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close", pStop)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(pStop)
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GraceAudio Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background audio playback notifications"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
