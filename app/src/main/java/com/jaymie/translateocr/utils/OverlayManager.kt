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
import android.view.WindowInsets
import com.jaymie.translateocr.service.TranslateAccessibilityService

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
    @SuppressLint("InflateParams")
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
        try {
            if (overlayView != null) {
                windowManager.removeView(overlayView)
                overlayView = null
            }
        } catch (e: Exception) {
            // Handle removal errors silently
        }
    }

    fun showLoading(show: Boolean) {
        overlayView?.let { view ->
            val loadingView = view.findViewById<ProgressBar>(R.id.ocr_loading)
            loadingView?.visibility = if (show) View.VISIBLE else View.GONE
            
            overlayContainer?.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    fun updateOverlayText(textBlocks: List<Text.TextBlock>, translatedText: String? = null) {
        if (overlayView == null) return  // Don't update if overlay is not showing
        
        try {
            overlayContainer?.let { container ->
                container.removeAllViews()
                
                val displayMetrics = container.context.resources.displayMetrics
                val maxWidth = displayMetrics.widthPixels
                val maxHeight = displayMetrics.heightPixels
                
                val statusBarHeight = getStatusBarHeight(container.context)
                
                // Split translated text into lines if available
                val translations = translatedText?.split("\n")?.filter { it.isNotBlank() }
                var translationIndex = 0
                
                // Filter out text blocks in the status bar area
                val validTextBlocks = textBlocks.flatMap { block ->
                    block.lines.filter { line ->
                        line.boundingBox?.let { box ->
                            box.top > statusBarHeight
                        } ?: false
                    }
                }
                
                for (line in validTextBlocks) {
                    line.boundingBox?.let { box ->
                        val textView = LayoutInflater.from(container.context)
                            .inflate(R.layout.ocr_text_overlay, container, false) as TextView
                        
                        // Use translated text if available, otherwise use original text
                        val displayText = if (translations != null && translationIndex < translations.size) {
                            translations[translationIndex++]
                        } else {
                            line.text
                        }
                        
                        textView.text = displayText
                        
                        val params = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                        
                        // Calculate text size based on original box height
                        val baseTextSize = box.height().toFloat()
                        val maxTextSize = maxHeight * 0.1f
                        val minTextSize = displayMetrics.density * 12f
                        val textSizeInPixels = baseTextSize.coerceIn(minTextSize, maxTextSize)
                        
                        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeInPixels)
                        
                        // Calculate dimensions based on translated text
                        val paint = Paint()
                        paint.textSize = textSizeInPixels
                        val translatedTextWidth = paint.measureText(displayText)  // Use translated text width
                        val horizontalPadding = (textSizeInPixels * 0.3f).toInt()
                        
                        // Calculate total width needed for translated text
                        val totalWidthNeeded = translatedTextWidth + (horizontalPadding * 2).toFloat()
                        
                        // Set width to accommodate translated text
                        val desiredWidth = maxOf(box.width().toFloat(), totalWidthNeeded)
                        params.width = minOf(desiredWidth.toInt(), maxWidth - 20)
                        
                        // Adjust height
                        val desiredHeight = (textSizeInPixels * 1.5f).toInt()
                        params.height = minOf(desiredHeight, maxHeight / 3)
                        
                        // Position overlay
                        val desiredLeft = box.left
                        val desiredTop = (box.top - statusBarHeight) - (params.height / 2)
                        
                        // Ensure overlay stays within screen bounds
                        params.leftMargin = desiredLeft.coerceIn(0, maxWidth - params.width)
                        params.topMargin = desiredTop.coerceIn(0, maxHeight - params.height)
                        
                        // Add padding
                        val verticalPadding = (textSizeInPixels * 0.2f).toInt()
                        textView.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
                        
                        textView.elevation = 10f
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

    private fun getStatusBarHeight(context: Context): Int {
        // Method 1: Use WindowInsets (for API 30+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val windowManager = context.getSystemService(WindowManager::class.java)
            val currentWindowMetrics = windowManager.currentWindowMetrics
            return currentWindowMetrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.statusBars()
            ).top
        }

        // Method 2: Fallback - use density-based calculation
        return (24 * context.resources.displayMetrics.density).toInt()
    }

    fun getValidTextForTranslation(textBlocks: List<Text.TextBlock>, context: Context): String {
        val statusBarHeight = getStatusBarHeight(context)
        
        // Filter out status bar text and combine remaining text
        return textBlocks.flatMap { block ->
            block.lines.filter { line ->
                line.boundingBox?.let { box ->
                    box.top > statusBarHeight
                } ?: false
            }
        }.joinToString("\n") { it.text }
    }

    fun getValidTextBlocks(textBlocks: List<Text.TextBlock>, context: Context): List<Text.Line> {
        val statusBarHeight = getStatusBarHeight(context)
        
        // Filter out status bar text and return valid lines
        return textBlocks.flatMap { block ->
            block.lines.filter { line ->
                line.boundingBox?.let { box ->
                    box.top > statusBarHeight
                } ?: false
            }
        }
    }

    fun updateOverlayWithHighPrecision(
        textBlocks: List<TranslateAccessibilityService.TextBlock>,
        translations: List<String>
    ) {
        if (overlayView == null) return

        try {
            overlayContainer?.let { container ->
                container.removeAllViews()
                
                textBlocks.forEachIndexed { index, block ->
                    val translation = translations.getOrNull(index) ?: return@forEachIndexed
                    
                    val textView = LayoutInflater.from(container.context)
                        .inflate(R.layout.ocr_text_overlay, container, false) as TextView
                    
                    textView.text = translation
                    
                    val params = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                    
                    // Use the exact bounds from accessibility service
                    val bounds = block.bounds
                    params.leftMargin = bounds.left
                    params.topMargin = bounds.top
                    params.width = bounds.width()
                    params.height = bounds.height()
                    
                    // Add padding
                    val padding = (bounds.height() * 0.1f).toInt()
                    textView.setPadding(padding, padding, padding, padding)
                    
                    textView.elevation = 10f
                    params.gravity = Gravity.NO_GRAVITY
                    
                    container.addView(textView, params)
                }
                
                showLoading(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating overlay with high precision", e)
        }
    }
}