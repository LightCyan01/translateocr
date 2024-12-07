package com.jaymie.translateocr.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.jaymie.translateocr.R
import com.jaymie.translateocr.data.model.TranslationService
import com.google.android.material.textview.MaterialTextView

/**
 * Adapter for displaying translation service options in a dropdown
 */
class TranslationServiceAdapter(
    context: Context,
    private val services: List<TranslationService>
) : ArrayAdapter<TranslationService>(context, R.layout.item_dropdown, services) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createOrUpdateView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createOrUpdateView(position, convertView, parent)
    }

    private fun createOrUpdateView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_dropdown, parent, false)

        (view as? MaterialTextView)?.text = getItem(position).displayName
        return view
    }

    override fun getCount(): Int = services.size

    override fun getItem(position: Int): TranslationService = services[position]
} 