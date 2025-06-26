package com.example.pbb_app

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.util.*

class ExpenseActivity : BaseActivity() {

    private lateinit var spinnerCategory: Spinner
    private lateinit var etExpenseAmount: EditText
    private lateinit var etExpenseDescription: EditText
    private lateinit var btnAddExpense: Button
    private lateinit var tvExpenseHeading: TextView
    private lateinit var rgType: RadioGroup
    private lateinit var rbExpense: RadioButton
    private lateinit var rbIncome: RadioButton
    private lateinit var etDate: EditText
    private lateinit var etStartTime: EditText
    private lateinit var etEndTime: EditText
    private lateinit var btnUploadPicture: Button
    private lateinit var ivPicturePreview: ImageView
    private var selectedImageUri: Uri? = null
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val IMAGE_PICK_CODE = 2001

    override fun getLayoutResourceId(): Int = R.layout.activity_expense

    override fun setupUI() {
        // Initialize views
        tvExpenseHeading = findViewById(R.id.tvExpenseHeading)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        etExpenseAmount = findViewById(R.id.etExpenseAmount)
        etExpenseDescription = findViewById(R.id.etExpenseDescription)
        btnAddExpense = findViewById(R.id.btnAddExpense)
        rgType = findViewById(R.id.rgType)
        rbExpense = findViewById(R.id.rbExpense)
        rbIncome = findViewById(R.id.rbIncome)
        etDate = findViewById(R.id.etDate)
        etStartTime = findViewById(R.id.etStartTime)
        etEndTime = findViewById(R.id.etEndTime)
        btnUploadPicture = findViewById(R.id.btnUploadPicture)
        ivPicturePreview = findViewById(R.id.ivPicturePreview)

        // Set up Spinner (Category selection)
        val categories = listOf("🍕 Food & Dining", "🚗 Transportation", "🛒 Shopping", "💡 Utilities", "🎬 Entertainment", "🏥 Healthcare", "📚 Education", "🏠 Housing", "👕 Clothing", "💻 Technology", "🎯 Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        // Date picker
        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(this,
                { _, year, month, dayOfMonth -> etDate.setText("%04d-%02d-%02d".format(year, month + 1, dayOfMonth)) },
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            datePicker.show()
        }

        // Start time picker
        etStartTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val timePicker = TimePickerDialog(this,
                { _, hourOfDay, minute -> etStartTime.setText("%02d:%02d".format(hourOfDay, minute)) },
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
            timePicker.show()
        }

        // End time picker
        etEndTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val timePicker = TimePickerDialog(this,
                { _, hourOfDay, minute -> etEndTime.setText("%02d:%02d".format(hourOfDay, minute)) },
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
            timePicker.show()
        }

        // Image upload
        btnUploadPicture.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_PICK_CODE)
        }

        // Add Expense Button Click
        btnAddExpense.setOnClickListener {
            val amountStr = etExpenseAmount.text.toString().trim()
            val description = etExpenseDescription.text.toString().trim()
            val category = spinnerCategory.selectedItem.toString()
            val type = if (rbIncome.isChecked) "income" else "expense"
            val date = etDate.text.toString().trim()
            val startTime = etStartTime.text.toString().trim()
            val endTime = etEndTime.text.toString().trim()

            if (amountStr.isNotEmpty()) {
                try {
                    val amountValue = amountStr.toDouble()
                    if (amountValue > 0) {
                        val imagePath = selectedImageUri?.let { uri -> saveImageLocally(uri) }
                        saveTransactionToFirestore(category, amountValue, description, type, date, startTime, endTime, imagePath)
                    } else {
                        Toast.makeText(this, "Please enter a valid amount greater than 0", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }

        // Bottom Navigation item clicks
        homeIcon.setOnClickListener {
            // Navigate to HomeActivity (replace with your HomeActivity class)
            val homeIntent = Intent(this, DashboardActivity::class.java)
            startActivity(homeIntent)
            finish() // Close current activity if needed
        }

        categoryIcon.setOnClickListener {
            // Navigate to CategoryActivity (replace with your CategoryActivity class)
            val categoryIntent = Intent(this, AddCategoryActivity::class.java)
            startActivity(categoryIntent)
        }

        statsIcon.setOnClickListener {
            // Stay in the current activity or handle any logic if needed
            Toast.makeText(this, "Expense Section", Toast.LENGTH_SHORT).show()
        }

        profileIcon.setOnClickListener {
            // Navigate to ProfileActivity (replace with your ProfileActivity class)
            val profileIntent = Intent(this, ProfileActivity::class.java)
            startActivity(profileIntent)
        }
    }

    private fun saveImageLocally(imageUri: Uri): String? {
        val context = applicationContext
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val fileName = "transaction_${UUID.randomUUID()}.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            if (selectedImageUri != null) {
                ivPicturePreview.setImageURI(selectedImageUri)
                ivPicturePreview.visibility = View.VISIBLE
            }
        }
    }

    private fun saveTransactionToFirestore(category: String, amount: Double, description: String, type: String, date: String, startTime: String, endTime: String, imagePath: String?) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        val transaction = hashMapOf<String, Any>(
            "category" to category,
            "amount" to amount,
            "description" to description,
            "type" to type,
            "timestamp" to Date(),
            "date" to date,
            "startTime" to startTime,
            "endTime" to endTime
        )
        if (imagePath != null) {
            transaction["imagePath"] = imagePath
        }
        db.collection("users").document(userId).collection("transactions")
            .add(transaction)
            .addOnSuccessListener {
                unlockAchievement("first_transaction")
                if (imagePath != null) unlockAchievement("first_picture")
                Toast.makeText(this, "Transaction Added!", Toast.LENGTH_SHORT).show()
                etExpenseAmount.text.clear()
                etExpenseDescription.text.clear()
                etDate.text.clear()
                etStartTime.text.clear()
                etEndTime.text.clear()
                ivPicturePreview.setImageDrawable(null)
                ivPicturePreview.visibility = View.GONE
                selectedImageUri = null
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to add transaction: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun unlockAchievement(id: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("achievements").document(id)
            .set(mapOf("unlocked" to true))
    }
}
