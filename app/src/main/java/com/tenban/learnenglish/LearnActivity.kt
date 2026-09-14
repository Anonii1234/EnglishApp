package com.tenban.learnenglish

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class LearnActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var topic: Topic
    private var currentIndex = 0
    private var isShowingFront = true
    private var isEnViMode = true
    private val wrongWords = mutableListOf<Word>()

    private lateinit var tvTopicInfo: TextView
    private lateinit var tvContent: TextView
    private lateinit var tvPronunciation: TextView
    private lateinit var tvExample: TextView
    private lateinit var cvWord: CardView
    private lateinit var btnPrevious: Button
    private lateinit var btnNext: Button
    private lateinit var btnForget: Button
    private lateinit var btnSpeak: ImageButton
    private lateinit var pbProgress: ProgressBar
    private lateinit var rgMode: RadioGroup
    private lateinit var rbEnVi: RadioButton
    private lateinit var rbViEn: RadioButton

    private lateinit var db: AppDatabase
    private lateinit var gestureDetector: GestureDetector
    private var tts: TextToSpeech? = null
    private var startTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learn)

        db = AppDatabase.getDatabase(this)
        tts = TextToSpeech(this, this)
        startTime = System.currentTimeMillis()

        // Nhận topic từ Intent
        val receivedTopic = intent.getSerializableExtra("EXTRA_TOPIC") as? Topic
        if (receivedTopic == null) {
            finish()
            return
        }
        topic = receivedTopic
        currentIndex = intent.getIntExtra("EXTRA_WORD_INDEX", 0)

        // Khởi tạo Views
        tvTopicInfo = findViewById(R.id.tvTopicInfo)
        tvContent = findViewById(R.id.tvContent)
        tvPronunciation = findViewById(R.id.tvPronunciation)
        tvExample = findViewById(R.id.tvExample)
        cvWord = findViewById(R.id.cvWord)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)
        btnForget = findViewById(R.id.btnForget)
        btnSpeak = findViewById(R.id.btnSpeak)
        pbProgress = findViewById(R.id.pbProgress)
        rgMode = findViewById(R.id.rgMode)
        rbEnVi = findViewById(R.id.rbEnVi)
        rbViEn = findViewById(R.id.rbViEn)

        loadMode()
        updateUI()

        rgMode.setOnCheckedChangeListener { _, checkedId ->
            isEnViMode = checkedId == R.id.rbEnVi
            saveMode()
            isShowingFront = true
            updateUI()
        }

        cvWord.setOnClickListener { flipCardWithAnimation() }

        btnPrevious.setOnClickListener { showPreviousWord() }

        btnNext.setOnClickListener {
            // "Tôi đã nhớ"
            updateProgress(true)
            showNextWord()
        }

        btnForget.setOnClickListener {
            // "Chưa nhớ"
            vibrate()
            updateProgress(false)
            markAsWrong()
        }

        btnSpeak.setOnClickListener {
            speakCurrentWord()
        }

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                val diffX = e2.x - (e1?.x ?: 0f)
                if (abs(diffX) > 100 && abs(vx) > 100) {
                    if (diffX > 0) {
                        showPreviousWord()
                    } else {
                        updateProgress(true)
                        showNextWord()
                    }
                    return true
                }
                return false
            }
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                flipCardWithAnimation()
                return true
            }
        })

        cvWord.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) v.performClick()
            true
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    private fun speakCurrentWord() {
        val wordToSpeak = topic.words[currentIndex].word
        tts?.speak(wordToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun loadMode() {
        val sharedPref = getSharedPreferences("Settings", MODE_PRIVATE)
        isEnViMode = sharedPref.getBoolean("isEnViMode", true)
        if (isEnViMode) rbEnVi.isChecked = true else rbViEn.isChecked = true
    }

    private fun saveMode() {
        getSharedPreferences("Settings", MODE_PRIVATE).edit {
            putBoolean("isEnViMode", isEnViMode)
        }
    }

    private fun updateUI() {
        val currentWord = topic.words[currentIndex]
        tvTopicInfo.text = "Chủ đề: ${topic.name} - ${currentIndex + 1}/${topic.words.size}"
        
        // Cập nhật ProgressBar
        pbProgress.max = topic.words.size
        pbProgress.progress = currentIndex + 1

        if (isEnViMode) {
            if (isShowingFront) {
                tvContent.text = currentWord.word
                tvPronunciation.text = currentWord.pronunciation ?: ""
                tvPronunciation.visibility = if (!currentWord.pronunciation.isNullOrBlank()) TextView.VISIBLE else TextView.GONE
                tvExample.visibility = TextView.GONE
            } else {
                tvContent.text = currentWord.meaning
                tvExample.text = currentWord.example
                tvExample.visibility = TextView.VISIBLE
                tvPronunciation.visibility = TextView.GONE
            }
        } else {
            if (isShowingFront) {
                tvContent.text = currentWord.meaning
                tvPronunciation.visibility = TextView.GONE
                tvExample.visibility = TextView.GONE
            } else {
                tvContent.text = currentWord.word
                tvPronunciation.text = currentWord.pronunciation ?: ""
                tvPronunciation.visibility = if (!currentWord.pronunciation.isNullOrBlank()) TextView.VISIBLE else TextView.GONE
                tvExample.text = currentWord.example
                tvExample.visibility = TextView.VISIBLE
            }
        }
    }

    private fun flipCardWithAnimation() {
        cvWord.animate()
            .rotationY(90f)
            .setDuration(150)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                isShowingFront = !isShowingFront
                updateUI()
                cvWord.rotationY = -90f
                cvWord.animate()
                    .rotationY(0f)
                    .setDuration(150)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }.start()
    }

    private fun markAsWrong() {
        val currentWord = topic.words[currentIndex]
        if (!wrongWords.contains(currentWord)) {
            wrongWords.add(currentWord)
        }
        showNextWord()
    }

    private fun updateProgress(isRemembered: Boolean) {
        val topicId = topic.id
        val wordIndex = currentIndex
        lifecycleScope.launch {
            val progress = db.progressDao().getProgress(topicId, wordIndex) ?: Progress(topicId, wordIndex)
            val now = System.currentTimeMillis()
            
            if (isRemembered) {
                progress.level = min(progress.level + 1, 5)
                progress.nextReviewDate = now + (progress.level * 24L * 60 * 60 * 1000)
                // Cập nhật số từ học trong ngày
                incrementDailyWordCount()
            } else {
                progress.level = max(progress.level - 1, 1)
                progress.nextReviewDate = now + (60L * 60 * 1000) // 1h
            }
            progress.lastUpdated = now
            db.progressDao().insertOrUpdate(progress)
        }
    }

    private suspend fun incrementDailyWordCount() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val stats = db.progressDao().getDailyStats(today) ?: DailyStats(today, 0)
        stats.wordCount++
        db.progressDao().insertDailyStats(stats)
    }

    private fun showNextWord() {
        if (currentIndex < topic.words.size - 1) {
            cvWord.animate()
                .translationX(-1000f)
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    currentIndex++
                    isShowingFront = true
                    updateUI()
                    cvWord.translationX = 1000f
                    cvWord.animate()
                        .translationX(0f)
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                }.start()
        } else {
            showFinishDialog()
        }
    }

    private fun showPreviousWord() {
        if (currentIndex > 0) {
            cvWord.animate()
                .translationX(1000f)
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    currentIndex--
                    isShowingFront = true
                    updateUI()
                    cvWord.translationX = -1000f
                    cvWord.animate()
                        .translationX(0f)
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                }.start()
        }
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    private fun showFinishDialog() {
        lifecycleScope.launch {
            val newlyEarned = BadgeManager.checkBadges(this@LearnActivity, db)
            if (newlyEarned.isNotEmpty()) {
                newlyEarned.forEach { badge ->
                    AlertDialog.Builder(this@LearnActivity)
                        .setTitle("Mở khóa thành tựu!")
                        .setMessage("Chúc mừng! Bạn đã nhận được huy hiệu: ${badge.name}\n${badge.description}")
                        .setPositiveButton("Tuyệt vời", null)
                        .show()
                }
            }
            
            if (wrongWords.isEmpty()) {
                Toast.makeText(this@LearnActivity, "Chúc mừng! Bạn đã nhớ hết từ vựng.", Toast.LENGTH_LONG).show()
                finish()
            } else {
                AlertDialog.Builder(this@LearnActivity)
                    .setTitle("Hoàn thành!")
                    .setMessage("Bạn có ${wrongWords.size} từ chưa nhớ. Bạn có muốn ôn lại không?")
                    .setPositiveButton("Ôn lại") { _, _ ->
                        val reviewTopic = Topic(topic.id, "${topic.name} (Ôn tập)", topic.icon, ArrayList(wrongWords))
                        val intent = Intent(this@LearnActivity, LearnActivity::class.java)
                        intent.putExtra("EXTRA_TOPIC", reviewTopic)
                        startActivity(intent)
                        finish()
                    }
                    .setNegativeButton("Để sau") { _, _ -> finish() }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    override fun onDestroy() {
        saveStudyTime()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    private fun saveStudyTime() {
        val duration = (System.currentTimeMillis() - startTime) / 1000 // seconds
        val sharedPref = getSharedPreferences("Stats", MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDay = sharedPref.getString("last_active_day", "")
        
        // Cập nhật streak
        if (lastDay != today) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            
            var streak = sharedPref.getInt("current_streak", 0)
            if (lastDay == yesterday) {
                streak++
            } else if (lastDay != today) {
                streak = 1
            }
            sharedPref.edit {
                putInt("current_streak", streak)
                putString("last_active_day", today)
            }
        }

        // Cập nhật thời gian học
        val currentDayTime = sharedPref.getLong("total_study_time_$today", 0L)
        sharedPref.edit {
            putLong("total_study_time_$today", currentDayTime + duration)
        }
    }
}
