package com.jaymie.translateocr.utils

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.provider.Settings

object PermissionUtils {
    // Check if the app has permission to draw over other apps
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    // Check if the screen recording permission is granted
    fun hasScreenRecordingPermission(): Boolean {
        // MediaProjectionManager does not have a "granted" state, so request it explicitly
        return false
    }

    // Get intent to request permission to draw over other apps
    fun getOverlayPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }

    // Get intent to request screen recording permission
    fun getScreenRecordingPermissionIntent(context: Context): Intent {
        val mediaProjectionManager =
            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return mediaProjectionManager.createScreenCaptureIntent()
    }
}
