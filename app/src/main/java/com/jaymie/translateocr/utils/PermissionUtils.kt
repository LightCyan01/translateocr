package com.jaymie.translateocr.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

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

    // Check if storage permission is granted
    fun hasStoragePermission(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Check if we should show permission rationale
    fun shouldShowStoragePermissionRationale(activity: Activity): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                android.Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    // Request storage permission
    fun requestStoragePermission(activity: Activity, requestCode: Int) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                ),
                requestCode
            )
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                requestCode
            )
        }
    }
}
