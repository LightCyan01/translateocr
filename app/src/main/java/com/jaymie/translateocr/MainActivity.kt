package com.jaymie.translateocr

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.jaymie.translateocr.databinding.ActivityMainBinding
import com.jaymie.translateocr.ui.view.Login
import com.jaymie.translateocr.ui.view.Profile
import com.jaymie.translateocr.utils.ProfileImageManager

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupToolbar()
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile picture when returning to MainActivity
        loadProfilePicture()
    }

    private fun setupToolbar() {
        binding.toolbar.profileButton.setOnClickListener {
            if (auth.currentUser != null) {
                startActivity(Intent(this, Profile::class.java))
            } else {
                startActivity(Intent(this, Login::class.java))
            }
        }
        loadProfilePicture()
    }

    private fun loadProfilePicture() {
        ProfileImageManager.loadProfileImage(binding.toolbar.profileButton)
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)
    }
}
