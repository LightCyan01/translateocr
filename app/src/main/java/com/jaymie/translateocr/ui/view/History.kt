package com.jaymie.translateocr.ui.view

import android.content.Intent
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.auth.FirebaseAuth
import com.jaymie.translateocr.databinding.FragmentHistoryBinding
import com.jaymie.translateocr.ui.adapter.TranslationHistoryAdapter
import com.jaymie.translateocr.ui.viewmodel.HistoryViewModel
import com.jaymie.translateocr.ui.view.Login

class History : Fragment() {
    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var binding: FragmentHistoryBinding
    private lateinit var historyAdapter: TranslationHistoryAdapter
    private val auth = FirebaseAuth.getInstance()

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
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser != null) {
            loadTranslations()
        }
    }

    private fun updateUI() {
        if (auth.currentUser != null) {
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
        observeViewModel()
        loadTranslations()
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

    private fun setupRecyclerView() {
        historyAdapter = TranslationHistoryAdapter()
        binding.historyRecyclerView.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadTranslations()
        }
    }

    private fun loadTranslations() {
        viewModel.loadTranslations()
    }

    private fun observeViewModel() {
        viewModel.translations.observe(viewLifecycleOwner) { translations ->
            historyAdapter.submitList(translations)
            binding.swipeRefresh.isRefreshing = false
            binding.emptyState.visibility = if (translations.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}