package com.jaymie.translateocr.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.view.Surface
import java.nio.ByteBuffer

class ScreenCaptureManager(private val context: Context) {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var screenCaptureCallback: ((Bitmap) -> Unit)? = null

    companion object {
        private const val VIRTUAL_DISPLAY_NAME = "ScreenCapture"
    }

    /**
     * Initialize screen capture with a MediaProjection instance.
     */
    fun initialize(mediaProjection: MediaProjection, callback: (Bitmap) -> Unit) {
        this.mediaProjection = mediaProjection
        this.screenCaptureCallback = callback

        val metrics = context.resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        // Set up ImageReader for capturing frames
        imageReader = ImageReader.newInstance(
            screenWidth,
            screenHeight,
            PixelFormat.RGBA_8888,
            2
        )
        val surface: Surface = imageReader!!.surface

        // Create Virtual Display
        virtualDisplay = mediaProjection.createVirtualDisplay(
            VIRTUAL_DISPLAY_NAME,
            screenWidth,
            screenHeight,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface,
            null,
            null
        )

        // Set up the ImageReader listener to capture a frame
        imageReader!!.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener

            // Process the image in a background thread
            Thread {
                val planes = image.planes
                val buffer: ByteBuffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * screenWidth

                val bitmapWidth = screenWidth + rowPadding / pixelStride

                val bitmap = Bitmap.createBitmap(
                    bitmapWidth,
                    screenHeight,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()

                // Crop the bitmap to the screen dimensions
                val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)

                // Stop the capture after one frame
                stopCapture()

                // Invoke the callback on the main thread
                Handler(Looper.getMainLooper()).post {
                    callback(croppedBitmap)
                }
            }.start()
        }, Handler(Looper.getMainLooper()))
    }

    /**
     * Stop the screen capture and release resources.
     */
    fun stopCapture() {
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
    }
}