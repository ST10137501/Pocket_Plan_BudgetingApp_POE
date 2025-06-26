package com.example.pbb_app

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AchievementsActivity : BaseActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    data class Achievement(
        val id: String,
        val name: String,
        val description: String,
        val iconRes: Int,
        var unlocked: Boolean = false
    )

    private val achievements = listOf(
        Achievement("first_transaction", "First Transaction", "Add your first income or expense.", R.drawable.ic_achievement_first_transaction),
        Achievement("category_creator", "Category Creator", "Create your first custom category.", R.drawable.ic_achievement_category_creator),
        Achievement("budget_starter", "Budget Starter", "Set a budget for a category.", R.drawable.ic_achievement_budget_starter),
        Achievement("consistent_logger", "Consistent Logger", "Add transactions 7 days in a row.", R.drawable.ic_achievement_consistent_logger),
        Achievement("big_saver", "Big Saver", "Save R1000 in total income minus expenses.", R.drawable.ic_achievement_big_saver),
        Achievement("expense_tracker", "Expense Tracker", "Log 10 expenses.", R.drawable.ic_achievement_expense_tracker),
        Achievement("income_earner", "Income Earner", "Log 10 incomes.", R.drawable.ic_achievement_income_earner),
        Achievement("category_master", "Category Master", "Create 5 different categories.", R.drawable.ic_achievement_category_master),
        Achievement("goal_setter", "Goal Setter", "Set a goal for a category or overall budget.", R.drawable.ic_achievement_goal_setter),
        Achievement("first_picture", "First Picture", "Attach a picture to a transaction.", R.drawable.ic_achievement_first_picture),
        Achievement("stats_explorer", "Stats Explorer", "View the statistics page for the first time.", R.drawable.ic_achievement_stats_explorer),
        Achievement("profile_complete", "Profile Complete", "Add a profile picture and phone number.", R.drawable.ic_achievement_profile_complete),
        Achievement("all_time_high", "All-Time High", "Record your highest single income.", R.drawable.ic_achievement_all_time_high),
        Achievement("all_time_low", "All-Time Low", "Record your highest single expense.", R.drawable.ic_achievement_all_time_low),
        Achievement("achievement_hunter", "Achievement Hunter", "Unlock 10 other achievements.", R.drawable.ic_achievement_achievement_hunter)
    )

    override fun getLayoutResourceId(): Int = R.layout.activity_achievements

    override fun setupUI() {
        val container = findViewById<LinearLayout>(R.id.achievementsContainer)
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showAchievements(container, achievements)
            return
        }
        db.collection("users").document(userId).collection("achievements").get()
            .addOnSuccessListener { snapshot ->
                val unlockedIds = snapshot.documents.mapNotNull { it.id.takeIf { docId -> it.getBoolean("unlocked") == true } }
                val updated = achievements.map { it.copy(unlocked = unlockedIds.contains(it.id)) }
                showAchievements(container, updated)
            }
            .addOnFailureListener {
                showAchievements(container, achievements)
            }
    }

    private fun showAchievements(container: LinearLayout, achievements: List<Achievement>) {
        container.removeAllViews()
        for (ach in achievements) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 16, 16, 16)
                gravity = Gravity.CENTER_VERTICAL
            }
            val icon = ImageView(this).apply {
                setImageResource(ach.iconRes)
                setColorFilter(if (ach.unlocked) Color.parseColor("#FFD600") else Color.parseColor("#BDBDBD"))
                layoutParams = LinearLayout.LayoutParams(100, 100)
            }
            val textLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 0, 0, 0)
            }
            val name = TextView(this).apply {
                text = ach.name
                textSize = 18f
                setTextColor(if (ach.unlocked) Color.BLACK else Color.GRAY)
            }
            val desc = TextView(this).apply {
                text = ach.description
                textSize = 14f
                setTextColor(if (ach.unlocked) Color.DKGRAY else Color.LTGRAY)
            }
            textLayout.addView(name)
            textLayout.addView(desc)
            row.addView(icon)
            row.addView(textLayout)
            container.addView(row)
        }
    }
} 