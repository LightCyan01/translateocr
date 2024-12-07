package com.jaymie.translateocr.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

/**
 * Utility class for accessibility-related operations
 */
object AccessibilityUtils {
    fun isAccessibilityServiceEnabled(context: Context, packageName: String): Boolean {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(packageName) == true
    }

    fun openAccessibilitySettings(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }
} 