package com.jaymie.translateocr.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jaymie.translateocr.data.model.SettingsItem
import com.jaymie.translateocr.databinding.ItemSettingsBinding

class SettingsAdapter(
    private val onItemClick: (SettingsItem) -> Unit
) : RecyclerView.Adapter<SettingsAdapter.SettingsViewHolder>() {

    private var items = listOf<SettingsItem>()

    inner class SettingsViewHolder(
        private val binding: ItemSettingsBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: SettingsItem) {
            binding.settingsTitle.text = item.title
            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsViewHolder {
        val binding = ItemSettingsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SettingsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SettingsViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun submitList(newItems: List<SettingsItem>) {
        items = newItems
        notifyDataSetChanged()
    }
} 