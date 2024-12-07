package com.jaymie.translateocr.ui.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.jaymie.translateocr.MainActivity
import com.jaymie.translateocr.R
import com.jaymie.translateocr.databinding.ActivityLoginBinding
import com.jaymie.translateocr.ui.viewmodel.LoginViewModel

class Login : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        setupToolbar()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbarLogin.setNavigationOnClickListener {
            finish()
        }
    }

    private fun observeViewModel() {
        viewModel.loginResult.observe(this) { event ->
            event.getContentIfNotHandled()?.let { result ->
                when (result) {
                    is LoginViewModel.LoginResult.Loading -> {
                        binding.loginButton.isEnabled = false
                        binding.registerButton.isEnabled = false
                        binding.loginButton.text = "Please wait..."
                        binding.registerButton.text = "Please wait..."
                    }
                    is LoginViewModel.LoginResult.Success -> {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    is LoginViewModel.LoginResult.Error -> {
                        Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                        binding.loginButton.isEnabled = true
                        binding.registerButton.isEnabled = true
                        binding.loginButton.text = "Login"
                        binding.registerButton.text = "Register"
                    }
                }
            }
        }
    }
}