package com.example.pbb_app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : BaseActivity() {

    private lateinit var ivProfilePicture: ImageView
    private lateinit var btnUploadPicture: Button
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var etPhone: EditText
    private lateinit var btnSavePhone: Button
    private lateinit var btnLogout: Button
    private lateinit var btnClose: ImageView
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    private val IMAGE_PICK_CODE = 1010

    override fun getLayoutResourceId(): Int = R.layout.activity_profile

    override fun setupUI() {
        auth = FirebaseAuth.getInstance()
        ivProfilePicture = findViewById(R.id.ivProfilePicture)
        btnUploadPicture = findViewById(R.id.btnUploadPicture)
        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)
        etPhone = findViewById(R.id.etPhone)
        btnSavePhone = findViewById(R.id.btnSavePhone)
        btnLogout = findViewById(R.id.btnLogout)
        btnClose = findViewById(R.id.btnClose)

        loadUserData()
        loadPhoneNumber()

        val openGallery = {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            startActivityForResult(intent, IMAGE_PICK_CODE)
        }

        ivProfilePicture.setOnClickListener { openGallery() }
        btnUploadPicture.setOnClickListener { openGallery() }

        btnSavePhone.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            val userId = auth.currentUser?.uid
            if (userId != null) {
                db.collection("users").document(userId)
                    .update("phone", phone)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Phone number updated", Toast.LENGTH_SHORT).show()
                        checkAndUnlockProfileAchievement()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to update phone: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        btnClose.setOnClickListener {
            finish()
        }

        // Bottom Navigation item clicks
        homeIcon.setOnClickListener {
            val homeIntent = Intent(this, DashboardActivity::class.java)
            startActivity(homeIntent)
            finish()
        }

        categoryIcon.setOnClickListener {
            val categoryIntent = Intent(this, AddCategoryActivity::class.java)
            startActivity(categoryIntent)
        }

        val statsIcon = findViewById<ImageView>(R.id.stats_icon)
        statsIcon.setOnClickListener {
            startActivity(android.content.Intent(this, StatsActivity::class.java))
        }

        profileIcon.setOnClickListener {
            Toast.makeText(this, "Profile Section", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            tvEmail.text = currentUser.email ?: "No email"
            tvName.text = currentUser.displayName ?: "User"
        } else {
            tvEmail.text = "Not logged in"
            tvName.text = "Guest User"
        }
    }

    private fun loadPhoneNumber() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val phone = doc.getString("phone")
                etPhone.setText(phone ?: "")
            }
    }

    private fun checkAndUnlockProfileAchievement() {
        val userId = auth.currentUser?.uid ?: return
        val hasPhone = etPhone.text.toString().trim().isNotEmpty()
        val hasProfilePic = ivProfilePicture.drawable != null // crude check
        if (hasPhone && hasProfilePic) {
            db.collection("users").document(userId).collection("achievements").document("profile_complete")
                .set(mapOf("unlocked" to true))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            ivProfilePicture.setImageURI(imageUri)
            checkAndUnlockProfileAchievement()
        }
    }
}
