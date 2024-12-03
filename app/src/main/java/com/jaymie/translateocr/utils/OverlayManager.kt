package com.jaymie.translateocr.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import com.jaymie.translateocr.R
import android.util.Log
import android.view.ContextThemeWrapper
import com.google.android.material.button.MaterialButton
import android.widget.FrameLayout
import com.google.mlkit.vision.text.Text
import android.graphics.Paint
import android.util.TypedValue
import android.util.DisplayMetrics
import android.text.TextUtils

class OverlayManager private constructor() {

    private var overlayView: View? = null
    private lateinit var windowManager: WindowManager
    private var overlayContainer: FrameLayout? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: OverlayManager? = null

        private const val TAG = "OverlayManager"

        fun getInstance(): OverlayManager {
            return INSTANCE ?: synchronized(this) {
                val instance = OverlayManager()
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Shows the overlay on the screen.
     */
    fun showOverlay(context: Context) {
        try {
            if (overlayView != null) {
                Log.d(TAG, "Overlay already showing")
                return
            }

            Log.d(TAG, "Creating new overlay")
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val themedContext = ContextThemeWrapper(context, R.style.Theme_Overlay)

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            layoutParams.gravity = Gravity.TOP or Gravity.START

            try {
                overlayView = LayoutInflater.from(themedContext).inflate(R.layout.overlay_layout, null)
                overlayContainer = overlayView?.findViewById(R.id.ocr_overlay_container)
            } catch (e: Exception) {
                Log.e(TAG, "Error inflating overlay layout", e)
                throw e
            }

            val exitButton = overlayView?.findViewById<MaterialButton>(R.id.exit_button)
            exitButton?.setOnClickListener {
                removeOverlay()
            }

            try {
                windowManager.addView(overlayView, layoutParams)
                Log.d(TAG, "Overlay added successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding overlay view to window", e)
                throw e
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing overlay", e)
            throw e
        }
    }

    /**
     * Removes the overlay from the screen.
     */
    fun removeOverlay() {
        if (overlayView != null) {
            windowManager.removeView(overlayView)
            overlayView = null
        }
    }

    fun showLoading(show: Boolean) {
        overlayView?.let { view ->
            val loadingView = view.findViewById<ProgressBar>(R.id.ocr_loading)
            loadingView?.visibility = if (show) View.VISIBLE else View.GONE
            
            overlayContainer?.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    private fun getScreenDimensions(context: Context): Pair<Int, Int> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        return Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }

    fun updateOverlayText(textBlocks: List<Text.TextBlock>) {
        try {
            overlayContainer?.let { container ->
                container.removeAllViews()
                
                val displayMetrics = container.context.resources.displayMetrics
                val maxWidth = displayMetrics.widthPixels
                val maxHeight = displayMetrics.heightPixels
                
                for (block in textBlocks) {
                    for (line in block.lines) {
                        val textView = LayoutInflater.from(container.context)
                            .inflate(R.layout.ocr_text_overlay, container, false) as TextView
                        
                        textView.text = line.text
                        
                        val params = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                        
                        line.boundingBox?.let { box ->
                            // Calculate text size
                            val baseTextSize = box.height().toFloat()
                            val maxTextSize = maxHeight * 0.1f
                            val minTextSize = displayMetrics.density * 12f
                            val textSizeInPixels = baseTextSize.coerceIn(minTextSize, maxTextSize)
                            
                            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeInPixels)
                            
                            // Calculate text dimensions with padding consideration
                            val paint = Paint()
                            paint.textSize = textSizeInPixels
                            val textWidth = paint.measureText(line.text)
                            val horizontalPadding = (textSizeInPixels * 0.3f).toInt() // Padding on each side
                            
                            // Calculate total width needed
                            val totalWidthNeeded = textWidth + (horizontalPadding * 2)
                            
                            // Set width to accommodate full text
                            val desiredWidth = maxOf(box.width(), totalWidthNeeded.toInt())
                            params.width = minOf(desiredWidth, maxWidth - 20) // Leave small margin
                            
                            // Adjust height
                            val desiredHeight = (textSizeInPixels * 1.3f).toInt()
                            params.height = minOf(desiredHeight, maxHeight / 4)
                            
                            // Position overlay
                            val desiredLeft = box.left
                            val desiredTop = box.top - params.height // Position above text
                            
                            // Ensure overlay stays within screen bounds
                            params.leftMargin = desiredLeft.coerceIn(0, maxWidth - params.width)
                            params.topMargin = desiredTop.coerceIn(0, maxHeight - params.height)
                            
                            // Add padding
                            val verticalPadding = (textSizeInPixels * 0.15f).toInt()
                            textView.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
                            
                            // Only ellipsize if absolutely necessary
                            if (totalWidthNeeded > maxWidth - 20) {
                                textView.ellipsize = TextUtils.TruncateAt.END
                            } else {
                                textView.ellipsize = null
                            }
                        }
                        
                        textView.elevation = 1000f
                        params.gravity = Gravity.NO_GRAVITY
                        
                        container.addView(textView, params)
                    }
                }
                showLoading(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating overlay text", e)
        }
    }
}
