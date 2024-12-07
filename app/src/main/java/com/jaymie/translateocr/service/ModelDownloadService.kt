package com.jaymie.translateocr.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jaymie.translateocr.R
import com.jaymie.translateocr.utils.ModelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Foreground service for downloading ML Kit translation models.
 * Shows progress notifications during download and completion status.
 * Uses coroutines for async operations.
 */
class ModelDownloadService : Service() {
    private val modelManager by lazy { ModelManager(this) }
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Handles download requests.
     * Shows progress notification and updates UI on completion.
     * 
     * @param intent Contains EXTRA_LANGUAGE_CODE for the model to download
     * @return START_REDELIVER_INTENT to retry failed downloads
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val languageCode = intent?.getStringExtra(EXTRA_LANGUAGE_CODE) ?: return START_NOT_STICKY
        
        val notification = createDownloadNotification(languageCode)
        startForeground(NOTIFICATION_ID, notification)

        scope.launch {
            try {
                modelManager.downloadModel(languageCode).onSuccess {
                    notificationManager.notify(
                        NOTIFICATION_ID,
                        createCompletionNotification(languageCode, true)
                    )
                    sendBroadcast(Intent(ACTION_DOWNLOAD_COMPLETE).apply {
                        putExtra(EXTRA_LANGUAGE_CODE, languageCode)
                    })
                }.onFailure {
                    notificationManager.notify(
                        NOTIFICATION_ID,
                        createCompletionNotification(languageCode, false)
                    )
                }
            } finally {
                stopForeground(true)
                stopSelf()
            }
        }
        
        return START_REDELIVER_INTENT
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Model Downloads",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createDownloadNotification(languageCode: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("Downloading Language Model")
            .setContentText("Downloading model for $languageCode")
            .setProgress(0, 0, true)
            .build()
    }

    private fun createCompletionNotification(languageCode: String, success: Boolean): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(if (success) R.drawable.ic_checkmark else R.drawable.ic_error)
            .setContentTitle(if (success) "Download Complete" else "Download Failed")
            .setContentText(if (success) "Model for $languageCode is ready" else "Failed to download model for $languageCode")
            .build()
    }

    companion object {
        const val EXTRA_LANGUAGE_CODE = "language_code"
        const val ACTION_DOWNLOAD_COMPLETE = "com.jaymie.translateocr.DOWNLOAD_COMPLETE"
        private const val CHANNEL_ID = "model_downloads"
        private const val NOTIFICATION_ID = 1
    }
} 