package com.tenban.learnenglish

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "progress", primaryKeys = ["topicId", "wordIndex"])
data class Progress(
    val topicId: Long,
    val wordIndex: Int,
    var level: Int = 1,
    var nextReviewDate: Long = 0L,
    var lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_stats")
data class DailyStats(
    @PrimaryKey val date: String, // Format: YYYY-MM-DD
    var wordCount: Int = 0
)

@Entity(tableName = "quiz_history")
data class QuizHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topicId: Long,
    val topicName: String,
    val score: Int,
    val total: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val icon: String = "📚"
)

@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topicId: Long,
    val word: String,
    val meaning: String,
    val example: String,
    val pronunciation: String? = null
)

data class TopicWithWords(
    @Embedded val topic: TopicEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "topicId"
    )
    val words: List<WordEntity>
)

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress WHERE topicId = :topicId AND wordIndex = :wordIndex")
    suspend fun getProgress(topicId: Long, wordIndex: Int): Progress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: Progress)

    @Query("SELECT COUNT(*) FROM progress WHERE nextReviewDate <= :currentTime")
    fun getDueCount(currentTime: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM progress WHERE level > 1")
    suspend fun getTotalLearnedWords(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyStats(stats: DailyStats)

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getDailyStats(date: String): DailyStats?

    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT 7")
    fun getWeeklyStats(): Flow<List<DailyStats>>

    @Insert
    suspend fun insertQuizHistory(history: QuizHistory)

    @Query("SELECT * FROM quiz_history ORDER BY timestamp DESC")
    fun getAllQuizHistory(): Flow<List<QuizHistory>>

    @Transaction
    @Query("SELECT * FROM topics")
    fun getAllTopicsWithWords(): Flow<List<TopicWithWords>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)

    @Transaction
    suspend fun saveFullTopic(topic: TopicEntity, words: List<WordEntity>) {
        insertTopic(topic)
        insertWords(words)
    }
}

@Database(entities = [Progress::class, QuizHistory::class, TopicEntity::class, WordEntity::class, DailyStats::class], version = 5)
abstract class AppDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "learn_english_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
