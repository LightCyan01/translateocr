package com.jaymie.translateocr.ui.view

import android.app.Dialog
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
import com.jaymie.translateocr.data.service.ScreenCaptureService
import com.jaymie.translateocr.databinding.FragmentHomeBinding
import com.jaymie.translateocr.ui.viewmodel.HomeViewModel
import com.jaymie.translateocr.utils.FloatingButtonManager
import com.jaymie.translateocr.utils.OverlayManager
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


    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(requireContext(), ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_INIT
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_DATA, result.data)
            }
            requireContext().startForegroundService(serviceIntent)
            floatingButtonManager.showFloatingButton {
                viewModel.captureScreen()
            }
        } else {
            Log.e("HomeFragment", "Screen capture permission denied or invalid")
            Toast.makeText(requireContext(), "Screen capture permission denied.", Toast.LENGTH_SHORT).show()
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
            viewModel.stopScreenCaptureService()
        }

        observeViewModel()

        return binding.root
    }

    private fun observeViewModel() {
        // Observe OCR results from ViewModel
        viewModel.ocrResult.observe(viewLifecycleOwner) { result ->
            Log.d("HomeFragment", "Received OCR result: $result")
            OverlayManager.getInstance().showOverlay(requireContext(), result)
        }

        // TODO: Observe LiveData for translation results in the future
        // viewModel.translationResult.observe(viewLifecycleOwner) { translatedText ->
        //     OverlayManager.getInstance().updateOverlayText(translatedText)
        // }
    }

    private fun checkPermissionsAndStart() {
        Log.d("HomeFragment", "Checking permissions")
        if (!Settings.canDrawOverlays(requireContext())) {
            Log.d("HomeFragment", "Overlay permission not granted")
            showOverlayPermissionDialog()
        } else {
            Log.d("HomeFragment", "Overlay permission granted, requesting screen capture")
            requestScreenCapturePermission()
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

    private fun requestScreenCapturePermission() {
        val mediaProjectionManager = requireContext().getSystemService(AppCompatActivity.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        screenCaptureLauncher.launch(captureIntent)
    }

}
