package com.jaymie.translateocr.ui.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jaymie.translateocr.databinding.FragmentHistoryBinding
import com.jaymie.translateocr.ui.adapter.TranslationHistoryAdapter
import com.jaymie.translateocr.ui.viewmodel.HistoryViewModel

class History : Fragment() {
    private lateinit var binding: FragmentHistoryBinding
    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var historyAdapter: TranslationHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        if (viewModel.isUserLoggedIn()) {
            showHistoryView()
        } else {
            showLoginPrompt()
        }
    }

    private fun showHistoryView() {
        binding.apply {
            swipeRefresh.visibility = View.VISIBLE
            loginPromptContainer.visibility = View.GONE
        }
        setupRecyclerView()
        setupSwipeRefresh()
    }

    private fun showLoginPrompt() {
        binding.apply {
            swipeRefresh.visibility = View.GONE
            loginPromptContainer.visibility = View.VISIBLE
            loginButton.setOnClickListener {
                startActivity(Intent(requireContext(), Login::class.java))
            }
        }
    }

    private fun observeViewModel() {
        viewModel.translations.observe(viewLifecycleOwner) { translations ->
            historyAdapter.submitList(translations)
            binding.emptyView.visibility = 
                if (translations.isEmpty() && viewModel.isUserLoggedIn()) View.VISIBLE 
                else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = TranslationHistoryAdapter()
        binding.historyList.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadTranslations()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh translations when returning to fragment
        if (viewModel.isUserLoggedIn()) {
            viewModel.loadTranslations()
        }
    }
}