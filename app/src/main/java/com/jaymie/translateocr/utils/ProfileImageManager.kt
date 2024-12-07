package com.jaymie.translateocr.utils

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.jaymie.translateocr.R

object ProfileImageManager {
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance().reference

    fun loadProfileImage(imageView: ImageView) {
        auth.currentUser?.let { user ->
            val imageRef = storage.child("users/${user.uid}/profile_pictures/profile.jpg")
            imageRef.downloadUrl
                .addOnSuccessListener { uri ->
                    Glide.with(imageView.context)
                        .load(uri)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
                        .into(object : CustomViewTarget<ImageView, Drawable>(imageView) {
                            override fun onLoadFailed(errorDrawable: Drawable?) {
                                view.setImageDrawable(errorDrawable)
                                view.setColorFilter(android.graphics.Color.WHITE)
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                transition: Transition<in Drawable>?
                            ) {
                                view.clearColorFilter()
                                view.setImageDrawable(resource)
                            }

                            override fun onResourceCleared(placeholder: Drawable?) {
                                view.setImageDrawable(placeholder)
                                view.setColorFilter(android.graphics.Color.WHITE)
                            }
                        })
                }
                .addOnFailureListener {
                    imageView.setImageResource(R.drawable.ic_person)
                    imageView.setColorFilter(android.graphics.Color.WHITE)
                }
        } ?: run {
            imageView.setImageResource(R.drawable.ic_person)
            imageView.setColorFilter(android.graphics.Color.WHITE)
        }
    }
} 