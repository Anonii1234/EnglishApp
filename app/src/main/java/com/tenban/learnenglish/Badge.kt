package com.tenban.learnenglish

import android.content.Context
import androidx.core.content.edit

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val goalType: GoalType,
    val goalValue: Int
)

enum class GoalType {
    WORDS_LEARNED,
    STREAK_DAYS,
    QUIZ_COMPLETED
}

object BadgeManager {
    val allBadges = listOf(
        Badge("b1", "Người mới bắt đầu", "Học được 10 từ vựng", "🌱", GoalType.WORDS_LEARNED, 10),
        Badge("b2", "Chăm chỉ", "Đạt chuỗi 3 ngày học", "🔥", GoalType.STREAK_DAYS, 3),
        Badge("b3", "Vua từ vựng", "Học được 50 từ vựng", "👑", GoalType.WORDS_LEARNED, 50),
        Badge("b4", "Kiên trì", "Đạt chuỗi 7 ngày học", "💪", GoalType.STREAK_DAYS, 7),
        Badge("b5", "Thông thái", "Học được 100 từ vựng", "🎓", GoalType.WORDS_LEARNED, 100)
    )

    fun getEarnedBadgeIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences("Badges", Context.MODE_PRIVATE)
        return prefs.getStringSet("earned_ids", emptySet()) ?: emptySet()
    }

    fun markBadgeAsEarned(context: Context, badgeId: String) {
        val prefs = context.getSharedPreferences("Badges", Context.MODE_PRIVATE)
        val earned = getEarnedBadgeIds(context).toMutableSet()
        if (earned.add(badgeId)) {
            prefs.edit { putStringSet("earned_ids", earned) }
        }
    }

    suspend fun checkBadges(context: Context, db: AppDatabase): List<Badge> {
        val earnedIds = getEarnedBadgeIds(context)
        val newlyEarned = mutableListOf<Badge>()
        
        // Lấy dữ liệu thực tế
        val totalLearned = db.progressDao().getTotalLearnedWords()
        val statsPrefs = context.getSharedPreferences("Stats", Context.MODE_PRIVATE)
        val currentStreak = statsPrefs.getInt("current_streak", 0)

        allBadges.forEach { badge ->
            if (!earnedIds.contains(badge.id)) {
                val isAchieved = when (badge.goalType) {
                    GoalType.WORDS_LEARNED -> totalLearned >= badge.goalValue
                    GoalType.STREAK_DAYS -> currentStreak >= badge.goalValue
                    GoalType.QUIZ_COMPLETED -> false // Có thể mở rộng sau
                }

                if (isAchieved) {
                    markBadgeAsEarned(context, badge.id)
                    newlyEarned.add(badge)
                }
            }
        }
        return newlyEarned
    }
}
