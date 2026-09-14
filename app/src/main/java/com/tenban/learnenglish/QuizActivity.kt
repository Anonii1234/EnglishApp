package com.tenban.learnenglish

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class QuizActivity : AppCompatActivity() {

    private lateinit var topic: Topic
    private var quizWords = listOf<Word>()
    private var currentIndex = 0
    private var score = 0
    private val totalQuestions = 10

    private lateinit var tvScore: TextView
    private lateinit var tvQuestionCount: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var btnOptions: List<Button>
    private lateinit var btnNext: Button
    
    private lateinit var db: AppDatabase
    private var isAnswered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        db = AppDatabase.getDatabase(this)
        topic = intent.getSerializableExtra("EXTRA_TOPIC") as Topic
        
        // Chọn ngẫu nhiên 10 từ (hoặc ít hơn nếu chủ đề không đủ từ)
        quizWords = topic.words.shuffled().take(totalQuestions)

        tvScore = findViewById(R.id.tvScore)
        tvQuestionCount = findViewById(R.id.tvQuestionCount)
        tvQuestion = findViewById(R.id.tvQuestion)
        btnOptions = listOf(
            findViewById(R.id.btnOption1),
            findViewById(R.id.btnOption2),
            findViewById(R.id.btnOption3),
            findViewById(R.id.btnOption4)
        )
        btnNext = findViewById(R.id.btnNextQuestion)

        btnNext.setOnClickListener {
            if (currentIndex < quizWords.size - 1) {
                currentIndex++
                showQuestion()
            } else {
                finishQuiz()
            }
        }

        showQuestion()
    }

    private fun showQuestion() {
        isAnswered = false
        btnNext.visibility = Button.GONE
        val currentWord = quizWords[currentIndex]
        
        tvQuestionCount.text = "Câu ${currentIndex + 1}/${quizWords.size}"
        tvScore.text = "Điểm: $score"
        tvQuestion.text = currentWord.word

        // Tạo 4 đáp án
        val correctAnswer = currentWord.meaning
        val otherMeanings = topic.words
            .filter { it.meaning != correctAnswer }
            .map { it.meaning }
            .shuffled()
            .take(3)
        
        val allOptions = (otherMeanings + correctAnswer).shuffled()

        btnOptions.forEachIndexed { index, button ->
            button.text = allOptions.getOrNull(index) ?: ""
            button.isEnabled = true
            button.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            // Reset button style to default outlined
            button.setTextColor(ContextCompat.getColor(this, R.color.black)) // Standard fallback
            
            button.setOnClickListener {
                if (!isAnswered) {
                    checkAnswer(button, button.text.toString(), correctAnswer)
                }
            }
        }
    }

    private fun checkAnswer(selectedButton: Button, selectedAnswer: String, correctAnswer: String) {
        isAnswered = true
        btnOptions.forEach { it.isEnabled = false }
        
        if (selectedAnswer == correctAnswer) {
            score++
            selectedButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
            Toast.makeText(this, "Chính xác!", Toast.LENGTH_SHORT).show()
        } else {
            selectedButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            // Hiện đáp án đúng
            btnOptions.find { it.text == correctAnswer }?.setBackgroundColor(
                ContextCompat.getColor(this, android.R.color.holo_green_light)
            )
            Toast.makeText(this, "Sai rồi! Đáp án: $correctAnswer", Toast.LENGTH_SHORT).show()
        }

        tvScore.text = "Điểm: $score"
        btnNext.visibility = Button.VISIBLE

        // Tự động chuyển sau 1.5s
        Handler(Looper.getMainLooper()).postDelayed({
            if (currentIndex < quizWords.size - 1) {
                btnNext.performClick()
            } else {
                finishQuiz()
            }
        }, 1500)
    }

    private fun finishQuiz() {
        lifecycleScope.launch {
            db.progressDao().insertQuizHistory(
                QuizHistory(
                    topicId = topic.id,
                    topicName = topic.name,
                    score = score,
                    total = quizWords.size
                )
            )
        }

        AlertDialog.Builder(this)
            .setTitle("Kết thúc!")
            .setMessage("Bạn đạt được $score/${quizWords.size} điểm.")
            .setPositiveButton("Làm lại") { _, _ ->
                currentIndex = 0
                score = 0
                quizWords = topic.words.shuffled().take(totalQuestions)
                showQuestion()
            }
            .setNegativeButton("Về màn hình chính") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
