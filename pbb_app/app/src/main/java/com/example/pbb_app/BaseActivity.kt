package com.example.pbb_app

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {
    
    protected lateinit var homeIcon: ImageView
    protected lateinit var categoryIcon: ImageView
    protected lateinit var profileIcon: ImageView
    protected lateinit var statsIcon: ImageView
    protected lateinit var achievementsIcon: ImageView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutResourceId())
        initializeViews()
        setupBottomNavigation()
        setupUI()
    }
    
    abstract fun getLayoutResourceId(): Int
    abstract fun setupUI()
    
    private fun initializeViews() {
        homeIcon = findViewById(R.id.home_icon)
        categoryIcon = findViewById(R.id.category_icon)
        profileIcon = findViewById(R.id.profile_icon)
        statsIcon = findViewById(R.id.stats_icon)
        achievementsIcon = findViewById(R.id.achievements_icon)
    }
    
    private fun setupBottomNavigation() {
        homeIcon.setOnClickListener {
            if (this !is DashboardActivity) {
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "You're already on Dashboard", Toast.LENGTH_SHORT).show()
            }
        }
        
        categoryIcon.setOnClickListener {
            if (this !is AddCategoryActivity) {
                startActivity(Intent(this, AddCategoryActivity::class.java))
            } else {
                Toast.makeText(this, "You're already in Categories", Toast.LENGTH_SHORT).show()
            }
        }
        
        statsIcon.setOnClickListener {
            if (this !is StatsActivity) {
                startActivity(Intent(this, StatsActivity::class.java))
            } else {
                Toast.makeText(this, "You're already in Stats", Toast.LENGTH_SHORT).show()
            }
        }
        
        profileIcon.setOnClickListener {
            if (this !is ProfileActivity) {
                startActivity(Intent(this, ProfileActivity::class.java))
            } else {
                Toast.makeText(this, "You're already in Profile", Toast.LENGTH_SHORT).show()
            }
        }
        
        achievementsIcon.setOnClickListener {
            if (this !is AchievementsActivity) {
                startActivity(Intent(this, AchievementsActivity::class.java))
            } else {
                Toast.makeText(this, "You're already in Achievements", Toast.LENGTH_SHORT).show()
            }
        }
    }
} 