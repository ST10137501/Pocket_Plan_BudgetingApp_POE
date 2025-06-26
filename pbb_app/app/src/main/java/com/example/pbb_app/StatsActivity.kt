package com.example.pbb_app

import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*
import kotlin.collections.HashMap
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class StatsActivity : BaseActivity() {
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var spinnerPeriod: Spinner
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun getLayoutResourceId(): Int = R.layout.activity_stats

    override fun setupUI() {
        unlockAchievement("stats_explorer")
        pieChart = findViewById(R.id.pieChart)
        barChart = findViewById(R.id.barChart)
        spinnerPeriod = findViewById(R.id.spinnerPeriod)
        val periods = listOf("Last 7 days", "Last 30 days", "This month", "All time")
        spinnerPeriod.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, periods)

        spinnerPeriod.setSelection(1) // Default to last 30 days
        spinnerPeriod.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                loadAndDisplayCharts()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        })

        loadAndDisplayCharts()
    }

    private fun loadAndDisplayCharts() {
        val userId = auth.currentUser?.uid ?: return
        val now = Date()
        val calendar = Calendar.getInstance()
        calendar.time = now
        val period = spinnerPeriod.selectedItemPosition
        val startDate: Date? = when (period) {
            0 -> { calendar.add(Calendar.DAY_OF_YEAR, -7); calendar.time }
            1 -> { calendar.add(Calendar.DAY_OF_YEAR, -30); calendar.time }
            2 -> { calendar.set(Calendar.DAY_OF_MONTH, 1); calendar.time }
            else -> null // All time
        }
        var query: Query = db.collection("users").document(userId).collection("transactions")
        if (startDate != null) {
            query = query.whereGreaterThanOrEqualTo("timestamp", startDate)
        }
        query.get().addOnSuccessListener { snapshot ->
            val categoryTotals = HashMap<String, Double>()
            for (doc in snapshot.documents) {
                val type = doc.getString("type") ?: "expense"
                if (type != "expense") continue // Only show expenses in chart
                val category = doc.getString("category") ?: "Other"
                val amount = doc.getDouble("amount") ?: 0.0
                categoryTotals[category] = categoryTotals.getOrDefault(category, 0.0) + amount
            }
            showPieChart(categoryTotals)
            showBarChart(categoryTotals)
            showCategoryTotals(categoryTotals)
        }
    }

    private fun showPieChart(categoryTotals: Map<String, Double>) {
        val entries = categoryTotals.map { PieEntry(it.value.toFloat(), it.key) }
        val dataSet = PieDataSet(entries, "Spending by Category")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 14f
        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.setUsePercentValues(true)
        pieChart.description = Description().apply { text = "" }
        pieChart.centerText = "Spending"
        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    private fun showBarChart(categoryTotals: Map<String, Double>) {
        val entries = categoryTotals.entries.mapIndexed { idx, entry ->
            BarEntry(idx.toFloat(), entry.value.toFloat())
        }
        val dataSet = BarDataSet(entries, "Spending by Category")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 14f
        val data = BarData(dataSet)
        barChart.data = data
        barChart.xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(categoryTotals.keys.toList())
        barChart.xAxis.granularity = 1f
        barChart.xAxis.setDrawGridLines(false)
        barChart.axisLeft.axisMinimum = 0f
        barChart.axisRight.isEnabled = false
        barChart.description = Description().apply { text = "" }
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun showCategoryTotals(categoryTotals: Map<String, Double>) {
        val container = findViewById<LinearLayout>(R.id.categoryTotalsContainer)
        container.removeAllViews()
        for ((category, total) in categoryTotals) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(8, 8, 8, 8)
            }
            val categoryText = TextView(this).apply {
                text = category
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val amountText = TextView(this).apply {
                text = "R %.2f".format(total)
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            row.addView(categoryText)
            row.addView(amountText)
            container.addView(row)
        }
    }

    private fun unlockAchievement(id: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("achievements").document(id)
            .set(mapOf("unlocked" to true))
    }
} 