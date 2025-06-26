package com.example.pbb_app

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AddCategoryActivity : BaseActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var etCategory: EditText
    private lateinit var etCategoryDescription: EditText
    private lateinit var etCategoryAmount: EditText
    private lateinit var btnAddCategory: Button
    private lateinit var tvCategoryDisplay: TextView
    private val auth = FirebaseAuth.getInstance()
    private var categoriesListener: ListenerRegistration? = null

    override fun getLayoutResourceId(): Int = R.layout.activity_addcategory

    override fun setupUI() {
        db = FirebaseFirestore.getInstance()

        // Initialize views
        etCategory = findViewById(R.id.etCategory)
        etCategoryDescription = findViewById(R.id.etCategoryDescription)
        etCategoryAmount = findViewById(R.id.etCategoryAmount)
        btnAddCategory = findViewById(R.id.btnAddCategory)
        tvCategoryDisplay = findViewById(R.id.tvCategoryDisplay)

        // Add Category Button Click
        btnAddCategory.setOnClickListener {
            val category = etCategory.text.toString().trim()
            val description = etCategoryDescription.text.toString().trim()
            val amountStr = etCategoryAmount.text.toString().trim()
            val amount = amountStr.toDoubleOrNull()
            if (category.isNotEmpty() && description.isNotEmpty() && amount != null) {
                val newCategory = hashMapOf(
                    "name" to category,
                    "description" to description,
                    "amount" to amount
                )
                saveCategory(newCategory)
            } else {
                Toast.makeText(this, "Please enter category, description, and a valid amount", Toast.LENGTH_SHORT).show()
            }
        }

        // Bottom Navigation
        homeIcon.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }

        categoryIcon.setOnClickListener {
            Toast.makeText(this, "You're already in Categories", Toast.LENGTH_SHORT).show()
        }

        val statsIcon = findViewById<ImageView>(R.id.stats_icon)
        statsIcon.setOnClickListener {
            startActivity(android.content.Intent(this, StatsActivity::class.java))
        }

        profileIcon.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        listenToCategories()
    }

    private fun listenToCategories() {
        categoriesListener?.remove()
        val userId = auth.currentUser?.uid ?: return
        categoriesListener = db.collection("users").document(userId).collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val categories = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name")
                    val description = doc.getString("description")
                    val amount = doc.getDouble("amount")
                    if (!name.isNullOrBlank() && amount != null) {
                        val amountStr = "R %.2f".format(amount)
                        if (!description.isNullOrBlank()) "$name ($amountStr): $description" else "$name ($amountStr)"
                    } else null
                }
                tvCategoryDisplay.text = if (categories.isEmpty()) {
                    "Your categories will appear here"
                } else {
                    categories.joinToString("\n")
                }
            }
    }

    private fun saveCategory(category: Map<String, Any>) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        val categoryCollection = db.collection("users").document(userId).collection("categories")
        categoryCollection.add(category)
            .addOnSuccessListener { documentReference ->
                unlockAchievement("category_creator")
                Log.d(TAG, "Category added with ID: ${documentReference.id}")
                Toast.makeText(this, "Category saved successfully", Toast.LENGTH_SHORT).show()
                // Clear the form after successful save
                etCategory.text.clear()
                etCategoryDescription.text.clear()
                etCategoryAmount.text.clear()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding category", e)
                Toast.makeText(this, "Failed to save category: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun unlockAchievement(id: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("achievements").document(id)
            .set(mapOf("unlocked" to true))
    }

    override fun onDestroy() {
        super.onDestroy()
        categoriesListener?.remove()
    }
}
