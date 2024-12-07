package com.jaymie.translateocr.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jaymie.translateocr.data.model.Translation
import com.jaymie.translateocr.databinding.ItemTranslationHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TranslationHistoryAdapter : ListAdapter<Translation, TranslationHistoryAdapter.ViewHolder>(TranslationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTranslationHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemTranslationHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        fun bind(translation: Translation) {
            binding.apply {
                originalText.text = translation.originalText
                translatedText.text = translation.translatedText
                timestamp.text = dateFormat.format(Date(translation.timestamp))
                languagePair.text = "${translation.fromLanguage} → ${translation.toLanguage}"
            }
        }
    }

    private class TranslationDiffCallback : DiffUtil.ItemCallback<Translation>() {
        override fun areItemsTheSame(oldItem: Translation, newItem: Translation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Translation, newItem: Translation): Boolean {
            return oldItem == newItem
        }
    }
}