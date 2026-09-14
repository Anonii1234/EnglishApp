package com.tenban.learnenglish

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StatsActivity : AppCompatActivity() {

    private lateinit var tvStreak: TextView
    private lateinit var tvTotalWords: TextView
    private lateinit var tvTimeToday: TextView
    private lateinit var chartContainer: LinearLayout
    private lateinit var labelContainer: LinearLayout
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        db = AppDatabase.getDatabase(this)

        tvStreak = findViewById(R.id.tvStreak)
        tvTotalWords = findViewById(R.id.tvTotalWords)
        tvTimeToday = findViewById(R.id.tvTimeToday)
        chartContainer = findViewById(R.id.chartContainer)
        labelContainer = findViewById(R.id.labelContainer)

        loadStats()
        observeWeeklyProgress()
    }

    private fun loadStats() {
        val sharedPref = getSharedPreferences("Stats", MODE_PRIVATE)
        val streak = sharedPref.getInt("current_streak", 0)
        tvStreak.text = "🔥 $streak ngày"

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val secondsToday = sharedPref.getLong("total_study_time_$today", 0L)
        val minutesToday = secondsToday / 60
        tvTimeToday.text = "$minutesToday phút"

        lifecycleScope.launch {
            val totalWords = db.progressDao().getTotalLearnedWords()
            tvTotalWords.text = totalWords.toString()
        }
    }

    private fun observeWeeklyProgress() {
        lifecycleScope.launch {
            db.progressDao().getWeeklyStats().collectLatest { stats ->
                updateChart(stats)
            }
        }
    }

    private fun updateChart(stats: List<DailyStats>) {
        chartContainer.removeAllViews()
        labelContainer.removeAllViews()

        if (stats.isEmpty()) return

        val maxWords = stats.maxOf { it.wordCount }.coerceAtLeast(1)
        val sortedStats = stats.sortedBy { it.date }

        sortedStats.forEach { stat ->
            // Cột biểu đồ
            val bar = View(this)
            val heightPercent = stat.wordCount.toFloat() / maxWords
            val barHeight = (heightPercent * 150 * resources.displayMetrics.density).toInt().coerceAtLeast(10)
            
            val barParams = LinearLayout.LayoutParams(0, barHeight, 1f).apply {
                setMargins(8, 0, 8, 0)
            }
            bar.layoutParams = barParams
            bar.setBackgroundColor(resources.getColor(android.R.color.holo_blue_light, theme))
            chartContainer.addView(bar)

            // Nhãn ngày (ví dụ: 20/05)
            val dateLabel = TextView(this)
            val originalDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(stat.date)
            val displayDate = SimpleDateFormat("dd/MM", Locale.getDefault()).format(originalDate ?: Date())
            
            dateLabel.text = displayDate
            dateLabel.textSize = 10f
            dateLabel.gravity = Gravity.CENTER
            dateLabel.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            labelContainer.addView(dateLabel)
        }
    }
}
