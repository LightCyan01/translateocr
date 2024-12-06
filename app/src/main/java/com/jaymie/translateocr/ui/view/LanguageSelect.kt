package com.jaymie.translateocr.ui.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jaymie.translateocr.databinding.ActivityLanguageSelectBinding
import com.jaymie.translateocr.ui.adapter.LanguageAdapter
import com.jaymie.translateocr.utils.DeepLConstants
import com.jaymie.translateocr.utils.GoogleLanguages

class LanguageSelect : AppCompatActivity() {
    private lateinit var binding: ActivityLanguageSelectBinding

    companion object {
        const val EXTRA_SELECTED_LANGUAGE = "selected_language"
        const val EXTRA_IS_FROM_LANGUAGE = "is_from_language"
        const val EXTRA_TRANSLATION_SERVICE = "translation_service"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isFromLanguage = intent.getBooleanExtra(EXTRA_IS_FROM_LANGUAGE, true)
        val isDeepL = intent.getBooleanExtra(EXTRA_TRANSLATION_SERVICE, false)

        setupToolbar(isFromLanguage)
        setupRecyclerView(isFromLanguage, isDeepL)
    }

    private fun setupToolbar(isFromLanguage: Boolean) {
        binding.toolbarLanguageList.apply {
            title = if (isFromLanguage) "Translate From" else "Translate To"
            setNavigationOnClickListener { finish() }
        }
    }

    private fun setupRecyclerView(isFromLanguage: Boolean, isDeepL: Boolean) {
        val languages = if (isDeepL) {
            if (isFromLanguage) DeepLConstants.SUPPORTED_SOURCE_LANGUAGES
            else DeepLConstants.SUPPORTED_TARGET_LANGUAGES
        } else {
            GoogleLanguages.SUPPORTED_LANGUAGES
        }

        val adapter = LanguageAdapter(
            languages = languages,
            showDownloadButton = !isDeepL,
            onLanguageSelected = { selectedLanguage ->
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_SELECTED_LANGUAGE, selectedLanguage.code)
                    putExtra(EXTRA_IS_FROM_LANGUAGE, isFromLanguage)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        )

        binding.recyclerLanguageList.apply {
            layoutManager = LinearLayoutManager(this@LanguageSelect)
            this.adapter = adapter
        }
    }
}