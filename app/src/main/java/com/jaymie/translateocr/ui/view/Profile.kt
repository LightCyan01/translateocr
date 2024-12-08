package com.jaymie.translateocr.ui.view

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.jaymie.translateocr.MainActivity
import com.jaymie.translateocr.R
import com.jaymie.translateocr.databinding.ActivityProfileBinding
import com.jaymie.translateocr.ui.viewmodel.ProfileViewModel
import com.jaymie.translateocr.utils.PermissionUtils

class Profile : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val viewModel: ProfileViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { imageUri ->
            // Show loading state
            binding.profileImageProgress.visibility = View.VISIBLE
            viewModel.uploadProfilePicture(imageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Check if user is logged in
        if (auth.currentUser == null) {
            startActivity(Intent(this, Login::class.java))
            finish()
            return
        }

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Make both the container and edit button trigger image picker
        binding.profilePictureContainer.setOnClickListener {
            checkStoragePermissionAndPickImage()
        }

        binding.editProfileButton.setOnClickListener {
            checkStoragePermissionAndPickImage()
        }

        binding.toolbarProfile.setNavigationOnClickListener {
            finish()
        }

        // Inflate menu and handle sign out action
        binding.toolbarProfile.inflateMenu(R.menu.profile_menu)
        binding.toolbarProfile.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_sign_out -> {
                    viewModel.signOut()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        binding.logoutButton.setOnClickListener {
            viewModel.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun checkStoragePermissionAndPickImage() {
        when {
            PermissionUtils.hasStoragePermission(this) -> {
                launchImagePicker()
            }
            PermissionUtils.shouldShowStoragePermissionRationale(this) -> {
                showPermissionRationaleDialog()
            }
            else -> {
                PermissionUtils.requestStoragePermission(this, STORAGE_PERMISSION_CODE)
            }
        }
    }

    private fun launchImagePicker() {
        pickImage.launch("image/*")
    }

    private fun showPermissionRationaleDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Photo Access Required")
            .setMessage("Access to photos is needed to select a profile picture from your device.")
            .setPositiveButton("Grant") { _, _ ->
                PermissionUtils.requestStoragePermission(this, STORAGE_PERMISSION_CODE)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.username.observe(this) { username ->
            binding.usernameText.text = username
        }

        viewModel.translatedWords.observe(this) { wordCount ->
            binding.wordsTranslatedText.text = "Total Words Translated: $wordCount"
        }

        viewModel.profilePictureState.observe(this) { state ->
            when (state) {
                is ProfileViewModel.ProfilePictureState.Loading -> {
                    binding.profileImageProgress.visibility = View.VISIBLE
                }
                is ProfileViewModel.ProfilePictureState.Success -> {
                    binding.profileImageProgress.visibility = View.GONE
                    // Use Glide to load and cache the image
                    Glide.with(this)
                        .load(state.uri)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .centerCrop()
                        .into(binding.profileImage)
                }
                is ProfileViewModel.ProfilePictureState.Error -> {
                    binding.profileImageProgress.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Add companion object with permission code
    companion object {
        private const val STORAGE_PERMISSION_CODE = 100
    }

    // Override onRequestPermissionsResult
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    launchImagePicker()
                } else {
                    Toast.makeText(this, "Photo access is required to change profile picture", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}