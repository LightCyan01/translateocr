package com.jaymie.translateocr.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaymie.translateocr.R

/**
 * Utility class for dialog-related operations
 */
object DialogUtils {
    fun showAccessibilityConsentDialog(context: Context, onAccept: () -> Unit) {
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_accessibility_consent, null)

        val dialog = MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogView.findViewById<MaterialButton>(R.id.btn_accept).setOnClickListener {
            dialog.dismiss()
            onAccept()
        }

        dialogView.findViewById<MaterialButton>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
} 