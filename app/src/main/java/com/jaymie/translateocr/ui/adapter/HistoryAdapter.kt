package com.jaymie.translateocr.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jaymie.translateocr.databinding.ItemHistoryBinding
import com.jaymie.translateocr.data.model.Translation

class HistoryAdapter : ListAdapter<Translation, HistoryAdapter.HistoryViewHolder>(TranslationDiffCallback()) {

    inner class HistoryViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(translation: Translation) {
            binding.apply {
                originalText.text = translation.originalText
                translatedText.text = translation.translatedText
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
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