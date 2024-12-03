package com.jaymie.translateocr.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import com.jaymie.translateocr.R
import kotlin.math.absoluteValue

class FloatingButtonManager(private val context: Context) {

    private var floatingButtonView: View? = null
    private var windowManager: WindowManager? = null

    @SuppressLint("ClickableViewAccessibility")
    fun showFloatingButton(onClick: () -> Unit) {
        // If the floating button already exists, do not create it again
        if (floatingButtonView != null) return

        // Get the application context to avoid memory leaks
        val appContext = context.applicationContext

        // Initialize WindowManager if it's null
        if (windowManager == null) {
            windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.TOP or Gravity.END
        layoutParams.x = dpToPx(16)
        layoutParams.y = dpToPx(16)

        // Inflate the floating button layout
        floatingButtonView = LayoutInflater.from(context).inflate(R.layout.floating_button, null)
        val floatingButton = floatingButtonView?.findViewById<ImageView>(R.id.floatingButton)
        val loadingSpinner = floatingButtonView?.findViewById<ProgressBar>(R.id.loadingSpinner)

        loadingSpinner?.visibility = View.GONE

        // Set onClick listener
        floatingButton?.setOnClickListener {
            // Darken the button to show a tap animation
            floatingButton.alpha = 0.5f

            // Show the loading spinner
            loadingSpinner?.visibility = View.VISIBLE

            onClick() // Let the caller handle initiating screen capture

            // Simulate loading delay (e.g., translation processing)
            Handler(Looper.getMainLooper()).postDelayed({
                // Reset button appearance and hide spinner
                floatingButton.alpha = 1.0f
                loadingSpinner?.visibility = View.GONE

            }, 2000)
        }

        // Handle drag functionality
        floatingButton?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // Record the initial position of the floating button
                        initialX = layoutParams.x
                        initialY = layoutParams.y

                        // Record the touch's starting point
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // Calculate the movement distance
                        val deltaX = (event.rawX - initialTouchX).toInt()
                        val deltaY = (event.rawY - initialTouchY).toInt()

                        // Update position only if the movement exceeds a threshold
                        if (deltaX.absoluteValue > 10 || deltaY.absoluteValue > 10) {
                            layoutParams.x = initialX - deltaX
                            layoutParams.y = initialY + deltaY
                            windowManager?.updateViewLayout(floatingButtonView, layoutParams)
                            isDragging = true
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            v.performClick()
                        }
                        return true
                    }
                }
                return false
            }
        })

        // Add the view to the window
        windowManager?.addView(floatingButtonView, layoutParams)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    fun removeFloatingButton() {
        if (floatingButtonView != null) {
            windowManager?.removeView(floatingButtonView)
            floatingButtonView = null
        }
    }
}
