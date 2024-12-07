package com.jaymie.translateocr.ui.view

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.jaymie.translateocr.R
import com.jaymie.translateocr.data.model.TranslationService
import com.jaymie.translateocr.databinding.DialogApiKeyBinding
import com.jaymie.translateocr.databinding.FragmentHomeBinding
import com.jaymie.translateocr.ui.adapter.TranslationServiceAdapter
import com.jaymie.translateocr.ui.viewmodel.HomeViewModel
import com.jaymie.translateocr.ui.viewmodel.HomeViewModel.ValidationResult
import com.jaymie.translateocr.utils.AccessibilityUtils
import com.jaymie.translateocr.utils.DialogUtils
import com.jaymie.translateocr.utils.Event
import com.jaymie.translateocr.utils.FloatingButtonManager
import com.jaymie.translateocr.utils.OverlayManager
import com.jaymie.translateocr.utils.PermissionUtils

class Home : Fragment() {
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var binding: FragmentHomeBinding
    private lateinit var floatingButtonManager: FloatingButtonManager

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermissionsAndStart()
    }

    private val screenRecordingPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            viewModel.startScreenCapture(result.resultCode, result.data!!)
            Toast.makeText(requireContext(), "Screen recording permission granted.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Screen recording permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    private val languageSelectLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                val code = data.getStringExtra(LanguageSelect.EXTRA_SELECTED_LANGUAGE) ?: return@let
                val displayName = data.getStringExtra(LanguageSelect.EXTRA_LANGUAGE_DISPLAY_NAME) ?: code
                val isFromLanguage = data.getBooleanExtra(LanguageSelect.EXTRA_IS_FROM_LANGUAGE, true)

                if (isFromLanguage) {
                    viewModel.setSourceLanguage(code, displayName)
                } else {
                    viewModel.setTargetLanguage(code, displayName)
                }
            }
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

        setupLanguageButtons()
        setupServiceDropdown()
        observeViewModel()

        binding.start.setOnClickListener {
            checkPermissionsAndStart()
        }

        binding.stop.setOnClickListener {
            viewModel.stopScreenCapture()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
        checkAccessibilityService()
    }

    private fun setupLanguageButtons() {
        binding.btnLanguageFrom.setOnClickListener {
            startLanguageSelect(true)
        }

        binding.btnLanguageTo.setOnClickListener {
            startLanguageSelect(false)
        }

        binding.swapLanguages.apply {
            setOnClickListener {
                viewModel.swapLanguages()
            }
            isClickable = true
            isFocusable = true
        }
    }

    private fun setupServiceDropdown() {
        val services = TranslationService.entries
        val serviceAdapter = TranslationServiceAdapter(requireContext(), services)
        
        binding.serviceDropdown.apply {
            setAdapter(serviceAdapter)
            setText(TranslationService.GOOGLE_TRANSLATE.displayName, false)
            
            setOnItemClickListener { _, _, position, _ ->
                val selectedService = services[position]
                viewModel.setTranslationService(selectedService)
            }
        }

        viewModel.showApiKeyDialog.observe(viewLifecycleOwner) { event: Event<TranslationService> ->
            event.getContentIfNotHandled()?.let { service ->
                showApiKeyDialog(service)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.showFloatingButton.observe(viewLifecycleOwner) { shouldShow ->
            if (shouldShow) {
                floatingButtonManager.showFloatingButton {
                    viewModel.onFloatingButtonClicked()
                }
            } else {
                floatingButtonManager.removeFloatingButton()
            }
        }

        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        viewModel.ocrResult.observe(viewLifecycleOwner) { textBlocks ->
            OverlayManager.getInstance().updateOverlayText(textBlocks)
        }

        viewModel.sourceLanguageText.observe(viewLifecycleOwner) { text ->
            text?.let { binding.btnLanguageFrom.text = it }
        }

        viewModel.targetLanguageText.observe(viewLifecycleOwner) { text ->
            text?.let { binding.btnLanguageTo.text = it }
        }

        viewModel.selectedService.observe(viewLifecycleOwner) { service ->
            binding.serviceDropdown.setText(service.displayName, false)
        }
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
                val intent = PermissionUtils.getScreenRecordingPermissionIntent(requireContext())
                screenRecordingPermissionLauncher.launch(intent)
            }
        }
    }

    private fun showOverlayPermissionDialog() {
        PermissionUtils.showOverlayPermissionDialog(requireContext()) {
            val intent = PermissionUtils.getOverlayPermissionIntent(requireContext())
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun showScreenRecordingPermissionDialog() {
        PermissionUtils.showScreenRecordingPermissionDialog(requireContext()) {
            val intent = PermissionUtils.getScreenRecordingPermissionIntent(requireContext())
            screenRecordingPermissionLauncher.launch(intent)
        }
    }

    private fun startLanguageSelect(isFromLanguage: Boolean) {
        val intent = Intent(requireContext(), LanguageSelect::class.java).apply {
            putExtra(LanguageSelect.EXTRA_IS_FROM_LANGUAGE, isFromLanguage)
            putExtra(
                LanguageSelect.EXTRA_TRANSLATION_SERVICE,
                viewModel.selectedService.value == TranslationService.DEEPL
            )
        }
        languageSelectLauncher.launch(intent)
    }

    private fun showApiKeyDialog(service: TranslationService) {
        val dialog = Dialog(requireContext(), R.style.Theme_Dialog)
        val dialogBinding = DialogApiKeyBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        fun setLoadingState(isLoading: Boolean) {
            dialogBinding.apply {
                confirmButton.isEnabled = !isLoading
                apiKeyInput.isEnabled = !isLoading
                confirmButton.text = if (isLoading) "Validating..." else "Confirm"
            }
        }

        dialogBinding.apply {
            dialogTitle.text = when (service) {
                TranslationService.GOOGLE_TRANSLATE_API -> "Enter Google Translate API Key"
                TranslationService.DEEPL_API -> "Enter DeepL API Key"
                else -> "Enter API Key"
            }
            
            confirmButton.setOnClickListener {
                val apiKey = apiKeyInput.text.toString()
                if (apiKey.isNotBlank()) {
                    viewModel.validateAndSaveApiKey(service, apiKey)
                } else {
                    Toast.makeText(requireContext(), "Please enter an API key", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val observer = Observer<ValidationResult> { result ->
            when (result) {
                is ValidationResult.Loading -> setLoadingState(true)
                is ValidationResult.Success -> {
                    Toast.makeText(requireContext(), "API key saved successfully", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    binding.serviceDropdown.setText(service.displayName, false)
                }
                is ValidationResult.Error -> {
                    setLoadingState(false)
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.validationResult.removeObservers(viewLifecycleOwner)
        viewModel.validationResult.observe(viewLifecycleOwner, observer)

        dialog.setOnDismissListener {
            viewModel.onApiKeyDialogDismissed(service)
        }

        dialog.show()
    }

    private fun setupUI() {
        binding.highPrecisionSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !isAccessibilityServiceEnabled()) {
                binding.highPrecisionSwitch.isChecked = false
                showAccessibilityConsentDialog()
            }
        }
    }

    private fun showAccessibilityConsentDialog() {
        DialogUtils.showAccessibilityConsentDialog(requireContext()) {
            openAccessibilitySettings()
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(AccessibilityUtils.openAccessibilitySettings())
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        return AccessibilityUtils.isAccessibilityServiceEnabled(requireContext(), requireContext().packageName)
    }

    private fun checkAccessibilityService() {
        binding.highPrecisionSwitch.isChecked = isAccessibilityServiceEnabled()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkState()
        checkAccessibilityService()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (requireActivity().isFinishing) {
            floatingButtonManager.removeFloatingButton()
            OverlayManager.getInstance().removeOverlay()
            viewModel.clearTranslationState()
        }
    }

}