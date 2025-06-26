package com.example.pbb_app

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*
import android.app.Dialog
import android.net.Uri
import android.view.ViewGroup
import java.io.File
import com.bumptech.glide.Glide

class DashboardActivity : BaseActivity() {

    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalSpent: TextView
    private lateinit var tvRemaining: TextView
    private lateinit var budgetListLayout: LinearLayout
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var tvWelcome: TextView
    private lateinit var tvDate: TextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var transactionsListener: ListenerRegistration? = null

    override fun getLayoutResourceId(): Int = R.layout.activity_dashboard

    override fun setupUI() {
        // Initialize views
        tvTotalIncome = findViewById(R.id.balance)
        tvTotalSpent = findViewById(R.id.budget)
        tvRemaining = findViewById(R.id.expense)
        budgetListLayout = findViewById(R.id.recyclerview)
        fabAdd = findViewById(R.id.addBtn)
        tvWelcome = findViewById(R.id.tvWelcome)
        tvDate = findViewById(R.id.tvDate)

        val user = auth.currentUser
        val name = user?.displayName
        if (!name.isNullOrBlank()) {
            tvWelcome.text = "Welcome back, $name!"
        } else {
            tvWelcome.text = "Welcome back!"
        }
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
        tvDate.text = dateFormat.format(Date())

        fabAdd.setOnClickListener {
            startActivity(Intent(this, ExpenseActivity::class.java))
        }

        setupBottomNav()

        // Remove dummy data and listen to Firestore
        listenToTransactions()

        val statsIcon = findViewById<ImageView>(R.id.stats_icon)
        statsIcon.setOnClickListener {
            startActivity(android.content.Intent(this, StatsActivity::class.java))
        }
    }

    private fun listenToTransactions() {
        transactionsListener?.remove()
        val userId = auth.currentUser?.uid ?: return
        transactionsListener = db.collection("users").document(userId).collection("transactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                budgetListLayout.removeAllViews()
                var totalIncome = 0.0
                var totalSpent = 0.0
                for (doc in snapshot.documents) {
                    val type = doc.getString("type") ?: "expense"
                    val amount = doc.getDouble("amount") ?: 0.0
                    if (type == "income") {
                        totalIncome += amount
                    } else {
                        totalSpent += amount
                    }

                    val category = doc.getString("category") ?: "N/A"
                    val description = doc.getString("description") ?: ""
                    val date = doc.getString("date") ?: ""
                    val imagePath = doc.getString("imagePath")

                    addBudgetItem(category, amount, type, description, date, imagePath)
                }

                val remaining = totalIncome - totalSpent
                tvTotalIncome.text = "R %.2f".format(totalIncome)
                tvTotalSpent.text = "R %.2f".format(totalSpent)
                tvRemaining.text = "R %.2f".format(remaining)
            }
    }

    private fun addBudgetItem(category: String, amount: Double, type: String, description: String, date: String, imagePath: String?) {
        val itemContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
            setBackgroundResource(R.drawable.budget_item_background)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 8) }
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val detailsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val categoryText = TextView(this).apply {
            text = category
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        }

        val descriptionText = TextView(this).apply {
            text = description
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        }

        val dateText = TextView(this).apply {
            text = date
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setPadding(0, 4, 0, 0)
        }

        detailsLayout.addView(categoryText)
        detailsLayout.addView(descriptionText)
        detailsLayout.addView(dateText)

        val amountText = TextView(this).apply {
            text = "R %.2f".format(amount)
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, if (type == "income") R.color.income_green else R.color.expense_red))
            textAlignment = TextView.TEXT_ALIGNMENT_TEXT_END
        }

        topRow.addView(detailsLayout)
        topRow.addView(amountText)
        itemContainer.addView(topRow)

        if (!imagePath.isNullOrEmpty()) {
            val imageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    120
                ).apply { topMargin = 16 }
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

            val imageFile = File(imagePath)
            if (imageFile.exists()) {
                Glide.with(this).load(imageFile).into(imageView)
            }

            imageView.setOnClickListener {
                showImageDialog(imagePath)
            }
            itemContainer.addView(imageView)
        }

        budgetListLayout.addView(itemContainer)
        budgetListLayout.setPadding(0, 0, 0, 100)
    }

    private fun showImageDialog(imagePath: String) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_image_viewer)
        val imageView = dialog.findViewById<ImageView>(R.id.ivFullImage)
        val imageFile = File(imagePath)
        if (imageFile.exists()) {
            Glide.with(this).load(imageFile).into(imageView)
        }
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun setupBottomNav() {
        homeIcon.setOnClickListener {
            Toast.makeText(this, "Dashboard", Toast.LENGTH_SHORT).show()
        }
        categoryIcon.setOnClickListener {
            startActivity(Intent(this, AddCategoryActivity::class.java))
        }
        profileIcon.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        transactionsListener?.remove()
    }
}
