package com.jaymie.translateocr.ui.view

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jaymie.translateocr.R
import com.jaymie.translateocr.ui.viewmodel.SettingsViewModel
import com.jaymie.translateocr.ui.adapter.SettingsAdapter
import com.jaymie.translateocr.data.model.SettingsItem
import com.jaymie.translateocr.databinding.FragmentSettingsBinding
import com.jaymie.translateocr.databinding.DialogApiKeyBinding
import android.app.Dialog
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.Toast
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.lifecycle.Observer
import com.jaymie.translateocr.data.model.SettingsType

/**
 * Fragment for managing API keys and other settings
 */
class Settings : Fragment() {

    companion object {
        fun newInstance() = Settings()
    }

    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var binding: FragmentSettingsBinding
    private lateinit var settingsAdapter: SettingsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        setupRecyclerView()
        observeViewModel()
        return binding.root
    }

    private fun setupRecyclerView() {
        settingsAdapter = SettingsAdapter { item ->
            showApiKeyDialog(item)
        }

        binding.settingsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = settingsAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.settingsItems.observe(viewLifecycleOwner) { items ->
            settingsAdapter.submitList(items)
        }
    }

    private fun showApiKeyDialog(item: SettingsItem) {
        val dialog = Dialog(requireContext(), R.style.Theme_Dialog)
        val dialogBinding = DialogApiKeyBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        
        setupDialogWindow(dialog)
        setupDialogContent(dialogBinding, item, dialog)
        setupValidationObserver(dialogBinding, dialog)
        
        dialog.show()
    }

    private fun setupDialogWindow(dialog: Dialog) {
        dialog.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    private fun setupDialogContent(
        dialogBinding: DialogApiKeyBinding,
        item: SettingsItem
    ) {
        // Get existing API key
        val existingKey = when (item.type) {
            SettingsType.GOOGLE_API_KEY -> viewModel.getGoogleApiKey()
            SettingsType.DEEPL_API_KEY -> viewModel.getDeepLApiKey()
        }

        dialogBinding.apply {
            dialogTitle.text = item.title
            apiKeyInput.setText(existingKey)
            
            // Select all text when focused
            apiKeyInput.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) apiKeyInput.selectAll()
            }

            confirmButton.setOnClickListener {
                val apiKey = apiKeyInput.text.toString()
                if (apiKey.isNotBlank()) {
                    viewModel.validateAndSaveApiKey(item.type, apiKey)
                } else {
                    Toast.makeText(requireContext(), "Please enter an API key", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupValidationObserver(
        dialogBinding: DialogApiKeyBinding,
        dialog: Dialog
    ) {
        fun setLoadingState(isLoading: Boolean) {
            dialogBinding.apply {
                confirmButton.isEnabled = !isLoading
                apiKeyInput.isEnabled = !isLoading
                confirmButton.text = if (isLoading) "Validating..." else "Confirm"
            }
        }

        val observer = Observer<SettingsViewModel.ValidationResult> { result ->
            when (result) {
                is SettingsViewModel.ValidationResult.Loading -> setLoadingState(true)
                is SettingsViewModel.ValidationResult.Success -> {
                    Toast.makeText(requireContext(), "API key saved successfully", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                is SettingsViewModel.ValidationResult.Error -> {
                    setLoadingState(false)
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.validationResult.removeObservers(viewLifecycleOwner)
        viewModel.validationResult.observe(viewLifecycleOwner, observer)
    }
}