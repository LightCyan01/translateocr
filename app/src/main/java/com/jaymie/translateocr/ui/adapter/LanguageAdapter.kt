package com.jaymie.translateocr.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jaymie.translateocr.data.model.Language
import com.jaymie.translateocr.databinding.LanguageListBinding

class LanguageAdapter(
    private val languages: List<Language>,
    private val showDownloadButton: Boolean = true,
    private val onLanguageSelected: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    inner class LanguageViewHolder(
        private val binding: LanguageListBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(language: Language) {
            binding.textLanguageName.text = language.name
            
            binding.iconDownload.visibility = if (showDownloadButton) View.VISIBLE else View.GONE
            
            binding.root.setOnClickListener {
                onLanguageSelected(language)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = LanguageListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(languages[position])
    }

    override fun getItemCount() = languages.size
} 