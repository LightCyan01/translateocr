package com.jaymie.translateocr.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jaymie.translateocr.R
import com.jaymie.translateocr.data.model.Language
import com.jaymie.translateocr.databinding.ItemLanguageBinding

class LanguageAdapter(
    private val isGoogleTranslate: Boolean = true,
    private val onLanguageClick: (Language) -> Unit,
    private val onDownloadClick: (Language) -> Unit
) : ListAdapter<Language, LanguageAdapter.ViewHolder>(LanguageDiffCallback()) {

    private val downloadingStates = mutableMapOf<String, Boolean>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLanguageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemLanguageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(language: Language) {
            binding.languageName.text = language.name
            
            if (isGoogleTranslate) {
                binding.downloadContainer.visibility = View.VISIBLE
                val isDownloading = downloadingStates[language.code] ?: false
                
                when {
                    language.isDownloaded -> {
                        binding.downloadIcon.setImageResource(R.drawable.ic_checkmark)
                        binding.downloadIcon.visibility = View.VISIBLE
                        binding.downloadProgress.visibility = View.GONE
                        binding.downloadIcon.isClickable = false
                    }
                    isDownloading -> {
                        binding.downloadIcon.visibility = View.GONE
                        binding.downloadProgress.visibility = View.VISIBLE
                    }
                    else -> {
                        binding.downloadIcon.setImageResource(R.drawable.ic_download)
                        binding.downloadIcon.visibility = View.VISIBLE
                        binding.downloadProgress.visibility = View.GONE
                        binding.downloadIcon.setOnClickListener {
                            onDownloadClick(language)
                            updateDownloadingState(language.code, true)
                        }
                    }
                }
            } else {
                binding.downloadContainer.visibility = View.GONE
            }

            binding.root.setOnClickListener { onLanguageClick(language) }
        }
    }

    fun updateDownloadingState(languageCode: String, isDownloading: Boolean) {
        if (isDownloading) {
            downloadingStates[languageCode] = true
        } else {
            downloadingStates.remove(languageCode)
        }
        notifyItemChanged(currentList.indexOfFirst { it.code == languageCode })
    }

    private class LanguageDiffCallback : DiffUtil.ItemCallback<Language>() {
        override fun areItemsTheSame(oldItem: Language, newItem: Language) = 
            oldItem.code == newItem.code
        override fun areContentsTheSame(oldItem: Language, newItem: Language) = 
            oldItem == newItem && oldItem.isDownloaded == newItem.isDownloaded
    }
} 