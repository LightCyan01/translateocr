package com.jaymie.translateocr.ui.view

import android.app.Dialog
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import com.jaymie.translateocr.R
import com.jaymie.translateocr.databinding.FragmentHomeBinding
import com.jaymie.translateocr.ui.viewmodel.HomeViewModel
import com.jaymie.translateocr.utils.FloatingButtonManager
import com.jaymie.translateocr.utils.PermissionUtils

class Home : Fragment() {
    companion object {
        fun newInstance() = Home()
    }

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var floatingButtonManager: FloatingButtonManager
    private lateinit var binding: FragmentHomeBinding

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermissionsAndStart()
    }

    private val screenRecordingPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            Toast.makeText(requireContext(), "Screen recording permission granted.", Toast.LENGTH_SHORT).show()
            // Proceed to show the floating button
            floatingButtonManager.showFloatingButton {
                // Delegate the click action to the ViewModel
                viewModel.onFloatingButtonClicked()
            }
        } else {
            Toast.makeText(requireContext(), "Screen recording permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        floatingButtonManager = FloatingButtonManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.start.setOnClickListener {
            checkPermissionsAndStart()
        }

        binding.stop.setOnClickListener {
            floatingButtonManager.removeFloatingButton()
        }

        observeViewModel()

        return binding.root
    }

    private fun observeViewModel() {
        // Observe LiveData from ViewModel to update UI or handle events
        viewModel.showFloatingButton.observe(viewLifecycleOwner) { shouldShow ->
            if (shouldShow) {
                floatingButtonManager.showFloatingButton {
                    viewModel.onFloatingButtonClicked()
                }
            }
        }

        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        // TODO: Observe other LiveData for OCR results and update UI accordingly
    }

    private fun checkPermissionsAndStart() {
        when {
            !Settings.canDrawOverlays(requireContext()) -> {
                showOverlayPermissionDialog()
            }
            !viewModel.hasScreenRecordingPermission() -> {
                showScreenRecordingPermissionDialog()
            }
            else -> {
                // All permissions granted, show the floating button
                floatingButtonManager.showFloatingButton {
                    viewModel.onFloatingButtonClicked()
                }
            }
        }
    }

    private fun showOverlayPermissionDialog() {
        val dialog = Dialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.permission_dialog, null)

        dialog.setContentView(dialogView)

        val button = dialogView.findViewById<Button>(R.id.permissionButton)
        button.setOnClickListener {
            val intent = PermissionUtils.getOverlayPermissionIntent(requireContext())
            overlayPermissionLauncher.launch(intent)
            dialog.dismiss()
        }
        dialog.setCancelable(true)
        dialog.show()
    }

    private fun showScreenRecordingPermissionDialog() {
        val dialog = Dialog(requireContext())
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.screen_recording_permission_dialog, null)
        dialog.setContentView(dialogView)

        val acceptButton = dialogView.findViewById<Button>(R.id.permissionButton)
        acceptButton.setOnClickListener {
            val intent = PermissionUtils.getScreenRecordingPermissionIntent(requireContext())
            screenRecordingPermissionLauncher.launch(intent)
            dialog.dismiss()
        }

        dialog.setCancelable(true)
        dialog.show()
    }

}
