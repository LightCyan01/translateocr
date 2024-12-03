package com.jaymie.translateocr.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.jaymie.translateocr.R
import com.jaymie.translateocr.data.repository.OcrRepository
import com.jaymie.translateocr.utils.ScreenCaptureManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ScreenCaptureService : Service() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "ScreenCaptureServiceChannel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_DATA = "EXTRA_DATA"
        const val ACTION_INIT = "com.jaymie.translateocr.ACTION_INIT"
        const val ACTION_CAPTURE_SCREEN = "com.jaymie.translateocr.ACTION_CAPTURE_SCREEN"

        private var savedResultCode: Int = -1
        private var savedData: Intent? = null
    }

    private var mediaProjection: MediaProjection? = null
    private var screenCaptureManager: ScreenCaptureManager? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ScreenCaptureService", "onStartCommand called with intent: $intent")

        when (intent?.action) {
            ACTION_INIT -> {
                Log.d("ScreenCaptureService", "Handling ACTION_INIT")
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_DATA)
                }

                if (resultCode != -1 && data != null) {
                    Log.d("ScreenCaptureService", "Saving screen capture permission data")
                    savedResultCode = resultCode
                    savedData = data

                    initializeMediaProjection(resultCode, data)
                } else {
                    Log.e("ScreenCaptureService", "Invalid screen capture permission")
                    stopSelf()
                }
            }
            ACTION_CAPTURE_SCREEN -> {
                Log.d("ScreenCaptureService", "Handling ACTION_CAPTURE_SCREEN")
                if (mediaProjection == null && savedResultCode != -1 && savedData != null) {
                    initializeMediaProjection(savedResultCode, savedData!!)
                }

                if (mediaProjection != null) {
                    startScreenCaptureAndOCR()
                } else {
                    Log.e("ScreenCaptureService", "Cannot capture screen: MediaProjection is null")
                }
            }
            else -> {
                Log.d("ScreenCaptureService", "Unrecognized action")
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        screenCaptureManager?.stopCapture()
        mediaProjection?.stop()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Screen Capture Service",
            NotificationManager.IMPORTANCE_HIGH // Set to HIGH to ensure visibility
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Screen Capture Service")
            .setContentText("Service is running...")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Set to HIGH
            .build()
    }

    private fun startScreenCaptureAndOCR() {
        Log.d("ScreenCaptureService", "Starting screen capture and OCR")
        screenCaptureManager = ScreenCaptureManager(this)
        screenCaptureManager?.initialize(mediaProjection!!) { bitmap ->
            // Process the bitmap with OCR
            processImageWithOCR(bitmap)
        }
    }

    private fun processImageWithOCR(bitmap: Bitmap) {
        Log.d("ScreenCaptureService", "Processing image with OCR")
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val visionText = recognizer.process(image).await()
                val resultText = visionText.text
                Log.d("ScreenCaptureService", "OCR processing complete, result: $resultText")

                // Post the result to the repository
                OcrRepository.postOcrResult(resultText)

                // TODO: Initiate translation process with resultText
                // TranslationRepository.translateText(resultText)

            } catch (e: Exception) {
                // Handle exceptions
                Log.e("ScreenCaptureService", "Error during OCR processing: ${e.message}")
            } finally {
                // Stop the screen capture after processing
                screenCaptureManager?.stopCapture()
                screenCaptureManager = null
                // Optionally, stop the service if no longer needed
                // stopSelf()
            }
        }
    }
    private fun initializeMediaProjection(resultCode: Int, data: Intent) {
        try {
            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            Log.d("ScreenCaptureService", "MediaProjection initialized successfully")
        } catch (e: Exception) {
            Log.e("ScreenCaptureService", "Failed to initialize MediaProjection", e)
            stopSelf()
        }
    }
}
