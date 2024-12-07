package com.jaymie.translateocr.ui.view

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaymie.translateocr.R
import com.jaymie.translateocr.databinding.ActivityLanguageSelectBinding
import com.jaymie.translateocr.ui.adapter.LanguageAdapter
import com.jaymie.translateocr.utils.DeepLConstants
import com.jaymie.translateocr.utils.GoogleLanguages
import com.jaymie.translateocr.utils.ModelManager
import com.jaymie.translateocr.data.model.Language
import com.jaymie.translateocr.service.ModelDownloadService
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.activity.viewModels
import com.jaymie.translateocr.ui.viewmodel.DownloadEvent
import com.jaymie.translateocr.ui.viewmodel.LanguageSelectUiState
import com.jaymie.translateocr.ui.viewmodel.LanguageSelectViewModel

/**
 * Activity for selecting source and target languages.
 * Uses ViewBinding and observes ViewModel for state updates.
 */
class LanguageSelect : AppCompatActivity() {
    private lateinit var binding: ActivityLanguageSelectBinding
    private val viewModel: LanguageSelectViewModel by viewModels()
    private lateinit var adapter: LanguageAdapter

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ModelDownloadService.ACTION_DOWNLOAD_COMPLETE) {
                intent.getStringExtra(ModelDownloadService.EXTRA_LANGUAGE_CODE)?.let { code ->
                    viewModel.updateDownloadState(code, true)
                    Toast.makeText(this@LanguageSelect, "Model downloaded successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(downloadReceiver, IntentFilter(ModelDownloadService.ACTION_DOWNLOAD_COMPLETE))
        binding = ActivityLanguageSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        val isGoogleTranslate = !intent.getBooleanExtra(EXTRA_TRANSLATION_SERVICE, false)
        adapter = LanguageAdapter(
            context = this,
            isGoogleTranslate = isGoogleTranslate,
            modelManager = ModelManager(this),
            onLanguageClick = { language -> viewModel.onLanguageClick(language) },
            onDownloadClick = { language -> showDownloadDialog(language) }
        )
        binding.languageList.adapter = adapter
        binding.languageList.layoutManager = LinearLayoutManager(this)
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            updateUI(state)
        }

        viewModel.downloadEvent.observe(this) { event ->
            event.getContentIfNotHandled()?.let { downloadEvent ->
                when (downloadEvent) {
                    is DownloadEvent.StartDownload -> startDownload(downloadEvent.languageCode)
                    is DownloadEvent.ReturnLanguage -> returnLanguage(downloadEvent.languageCode)
                }
            }
        }
    }

    private fun updateUI(state: LanguageSelectUiState) {
        binding.toolbar.title = if (state.isFromLanguage) {
            getString(R.string.select_source_language)
        } else {
            getString(R.string.select_target_language)
        }
        adapter.submitList(state.languages)
    }

    private fun showDownloadDialog(language: Language) {
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle(R.string.download_model_title)
            .setMessage(R.string.download_model_message)
            .setPositiveButton(R.string.download) { _, _ ->
                viewModel.onDownloadClick(language)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun startDownload(languageCode: String) {
        startService(Intent(this, ModelDownloadService::class.java).apply {
            putExtra(ModelDownloadService.EXTRA_LANGUAGE_CODE, languageCode)
        })
    }

    private fun returnLanguage(languageCode: String) {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(EXTRA_SELECTED_LANGUAGE, languageCode)
            putExtra(EXTRA_IS_FROM_LANGUAGE, intent.getBooleanExtra(EXTRA_IS_FROM_LANGUAGE, true))
        })
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(downloadReceiver)
    }

    companion object {
        const val EXTRA_SELECTED_LANGUAGE = "selected_language"
        const val EXTRA_IS_FROM_LANGUAGE = "is_from_language"
        const val EXTRA_TRANSLATION_SERVICE = "translation_service"
    }
}