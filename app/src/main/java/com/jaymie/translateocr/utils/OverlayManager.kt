package com.jaymie.translateocr.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.jaymie.translateocr.R

class OverlayManager private constructor() {

    private var overlayView: View? = null
    private lateinit var windowManager: WindowManager
    private var textView: TextView? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: OverlayManager? = null

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
    fun showOverlay(context: Context, text: String) {
        if (overlayView != null) return

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val windowType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.TOP or Gravity.START

        // Inflate the overlay layout
        overlayView = LayoutInflater.from(context).inflate(R.layout.overlay_layout, null)

        // Set the text to display on the overlay
        textView = overlayView?.findViewById<TextView>(R.id.ocr_result_text)
        textView?.text = text

        // Set up the exit button
        val exitButton = overlayView?.findViewById<View>(R.id.exit_button)
        exitButton?.setOnClickListener {
            removeOverlay()
        }

        // Add the overlay view to the window
        windowManager.addView(overlayView, layoutParams)
    }

    /**
     * Updates the overlay text, useful for translation updates.
     */
    fun updateOverlayText(newText: String) {
        textView?.text = newText
    }

    /**
     * Removes the overlay from the screen.
     */
    fun removeOverlay() {
        if (overlayView != null) {
            windowManager.removeView(overlayView)
            overlayView = null
            textView = null
        }
    }
}
