package com.jaymie.translateocr.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

class ScreenCaptureManager(private val context: Context) {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val coroutineScope = CoroutineScope(Job() + Dispatchers.Main)

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val metrics = context.resources.displayMetrics
    
    companion object {
        private const val TAG = "ScreenCaptureManager"
    }

    suspend fun startProjection(resultCode: Int, data: Intent) {
        withContext(Dispatchers.IO) {
            try {
                delay(100)
                
                withContext(Dispatchers.Main) {
                    val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    mediaProjection = projectionManager.getMediaProjection(resultCode, data)
                    setupVirtualDisplay()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting projection", e)
            }
        }
    }

    private fun setupVirtualDisplay() {
        imageReader?.close()
        imageReader = ImageReader.newInstance(
            metrics.widthPixels,
            metrics.heightPixels,
            PixelFormat.RGBA_8888,
            2
        )

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
    }

    suspend fun captureScreen(): Bitmap? = withContext(Dispatchers.IO) {
        var image: Image? = null
        try {
            while (image == null) {
                image = imageReader?.acquireLatestImage()
                if (image != null) break
                delay(100)
            }

            image?.use {
                Log.d(TAG, "Image acquired, converting to bitmap")
                val plane = it.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * metrics.widthPixels

                return@withContext withContext(Dispatchers.Default) {
                    try {
                        val bitmap = Bitmap.createBitmap(
                            metrics.widthPixels + rowPadding / pixelStride,
                            metrics.heightPixels,
                            Bitmap.Config.ARGB_8888
                        )
                        buffer.rewind()
                        bitmap.copyPixelsFromBuffer(buffer)
                        bitmap
                    } catch (e: Exception) {
                        Log.e(TAG, "Error creating bitmap", e)
                        null
                    }
                }
            } ?: run {
                Log.e(TAG, "Failed to get image after 5 attempts")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing screen", e)
            image?.close()
            null
        }
    }

    fun stopProjection() {
        try {
            virtualDisplay?.release()
            imageReader?.close()
            mediaProjection?.stop()
            
            virtualDisplay = null
            imageReader = null
            mediaProjection = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping projection", e)
        }
    }
} 