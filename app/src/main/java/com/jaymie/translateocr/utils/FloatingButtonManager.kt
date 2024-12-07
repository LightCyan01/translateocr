package com.jaymie.translateocr.utils

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.util.DisplayMetrics
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
    private var isShowing = false
    private var currentLayoutParams: WindowManager.LayoutParams? = null

    private val configurationChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_CONFIGURATION_CHANGED) {
                updateFloatingButtonPosition()
            }
        }
    }

    init {
        context.applicationContext.registerReceiver(
            configurationChangeReceiver,
            IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED)
        )
    }

    private fun updateFloatingButtonPosition() {
        if (isShowing && floatingButtonView != null && currentLayoutParams != null) {
            try {
                // Get new screen dimensions
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val metrics = DisplayMetrics()
                windowManager.defaultDisplay?.getMetrics(metrics)

                // Ensure the button stays within screen bounds
                currentLayoutParams?.let { params ->
                    params.x = params.x.coerceIn(0, metrics.widthPixels - (floatingButtonView?.width ?: 0))
                    params.y = params.y.coerceIn(0, metrics.heightPixels - (floatingButtonView?.height ?: 0))
                    this.windowManager?.updateViewLayout(floatingButtonView, params)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showFloatingButton(onClick: () -> Unit) {
        if (isShowing) return

        try {
            val appContext = context.applicationContext
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

            currentLayoutParams = layoutParams
            removeFloatingButton()

            floatingButtonView = LayoutInflater.from(context).inflate(R.layout.floating_button, null)
            val floatingButton = floatingButtonView?.findViewById<ImageView>(R.id.floatingButton)
            val loadingSpinner = floatingButtonView?.findViewById<ProgressBar>(R.id.loadingSpinner)

            loadingSpinner?.visibility = View.GONE

            // Set onClick listener
            floatingButton?.setOnClickListener {
                loadingSpinner?.visibility = View.VISIBLE
                onClick()
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
            isShowing = true
        } catch (e: Exception) {
            e.printStackTrace()
            isShowing = false
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    fun removeFloatingButton() {
        try {
            if (floatingButtonView != null && isShowing) {
                windowManager?.removeView(floatingButtonView)
                floatingButtonView = null
                currentLayoutParams = null
                isShowing = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cleanup() {
        try {
            removeFloatingButton()
            context.applicationContext.unregisterReceiver(configurationChangeReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isFloatingButtonShowing(): Boolean = isShowing
}
