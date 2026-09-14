package com.tenban.learnenglish

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.database.MatrixCursor
import android.os.Bundle
import android.provider.BaseColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.cursoradapter.widget.CursorAdapter
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var rvTopics: RecyclerView
    private lateinit var tvReviewCount: TextView
    private lateinit var adapter: TopicAdapter
    private val topics = mutableListOf<Topic>()
    private lateinit var db: AppDatabase

    private val allSearchResults = mutableListOf<SearchResult>()
    private var lastFilteredResults = listOf<SearchResult>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        rvTopics = findViewById(R.id.rvTopics)
        tvReviewCount = findViewById(R.id.tvReviewCount)
        
        observeDueCount()
        loadTopicsFromRoom()
        checkBadges()
    }

    private fun checkBadges() {
        lifecycleScope.launch {
            val newlyEarned = BadgeManager.checkBadges(this@MainActivity, db)
            newlyEarned.forEach { badge ->
                showBadgeDialog(badge)
            }
        }
    }

    private fun showBadgeDialog(badge: Badge) {
        AlertDialog.Builder(this)
            .setTitle("Mở khóa thành tựu!")
            .setMessage("Chúc mừng! Bạn đã nhận được huy hiệu: ${badge.name}\n${badge.description}")
            .setIcon(android.R.drawable.star_on) // placeholder
            .setPositiveButton("Tuyệt vời", null)
            .show()
    }

    private fun loadTopicsFromRoom() {
        lifecycleScope.launch {
            db.progressDao().getAllTopicsWithWords().collectLatest { roomTopics ->
                if (roomTopics.isEmpty()) {
                    val initialTopics = VocabularyManager.loadTopics(this@MainActivity)
                    initialTopics.forEach { topic ->
                        val entity = TopicEntity(topic.id, topic.name, topic.icon ?: "📚")
                        val wordEntities = topic.words.map { 
                            WordEntity(topicId = topic.id, word = it.word, meaning = it.meaning, example = it.example)
                        }
                        db.progressDao().saveFullTopic(entity, wordEntities)
                    }
                } else {
                    topics.clear()
                    roomTopics.forEach { topicWithWords ->
                        topics.add(Topic(
                            id = topicWithWords.topic.id,
                            name = topicWithWords.topic.name,
                            icon = topicWithWords.topic.icon,
                            words = topicWithWords.words.map { 
                                Word(it.word, it.meaning, it.example)
                            }
                        ))
                    }
                    
                    allSearchResults.clear()
                    topics.forEach { topic ->
                        topic.words.forEachIndexed { index, word ->
                            allSearchResults.add(SearchResult(word, topic, index))
                        }
                    }
                    
                    updateRecyclerView()
                }
            }
        }
    }

    private fun updateRecyclerView() {
        adapter = TopicAdapter(
            topics = topics,
            onTopicClick = { topic ->
                val intent = Intent(this, LearnActivity::class.java)
                intent.putExtra("EXTRA_TOPIC", topic)
                startActivity(intent)
            },
            onQuizClick = { topic ->
                val intent = Intent(this, QuizActivity::class.java)
                intent.putExtra("EXTRA_TOPIC", topic)
                startActivity(intent)
            }
        )
        rvTopics.adapter = adapter
    }

    private fun observeDueCount() {
        lifecycleScope.launch {
            db.progressDao().getDueCount(System.currentTimeMillis()).collectLatest { count ->
                tvReviewCount.text = "Hôm nay bạn có $count từ cần ôn tập"
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView
        
        val from = arrayOf("title", "subtitle")
        val to = intArrayOf(android.R.id.text1, android.R.id.text2)
        val cursorAdapter = SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, null, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER)
        
        searchView?.apply {
            suggestionsAdapter = cursorAdapter
            queryHint = "Tìm từ hoặc nghĩa..."
            
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    performSearch(query)
                    return true
                }
                override fun onQueryTextChange(newText: String?): Boolean {
                    filterSuggestions(newText, cursorAdapter)
                    return true
                }
            })

            setOnSuggestionListener(object : SearchView.OnSuggestionListener {
                override fun onSuggestionSelect(position: Int): Boolean = false
                override fun onSuggestionClick(position: Int): Boolean {
                    val result = lastFilteredResults.getOrNull(position)
                    result?.let {
                        val intent = Intent(this@MainActivity, LearnActivity::class.java)
                        intent.putExtra("EXTRA_TOPIC", it.topic)
                        intent.putExtra("EXTRA_WORD_INDEX", it.index)
                        startActivity(intent)
                    }
                    return true
                }
            })
        }
        return true
    }

    private fun filterSuggestions(query: String?, cursorAdapter: SimpleCursorAdapter) {
        val cursor = MatrixCursor(arrayOf(BaseColumns._ID, "title", "subtitle"))
        if (!query.isNullOrBlank()) {
            lastFilteredResults = allSearchResults.filter { 
                it.word.word.contains(query, ignoreCase = true) || 
                it.word.meaning.contains(query, ignoreCase = true) 
            }.take(5)

            lastFilteredResults.forEachIndexed { index, result ->
                cursor.addRow(arrayOf(index, result.word.word, "${result.word.meaning} (${result.topic.name})"))
            }
        }
        cursorAdapter.changeCursor(cursor)
    }

    private fun performSearch(query: String?) {
        if (query.isNullOrBlank()) return
        val filteredResults = allSearchResults.filter { 
            it.word.word.contains(query, ignoreCase = true) || 
            it.word.meaning.contains(query, ignoreCase = true) 
        }
        if (filteredResults.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy kết quả", Toast.LENGTH_SHORT).show()
            return
        }
        showSearchDialog(filteredResults)
    }

    private fun showSearchDialog(results: List<SearchResult>) {
        val rvSearchResults = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            setPadding(32, 32, 32, 32)
            clipToPadding = false
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Kết quả tìm kiếm")
            .setView(rvSearchResults)
            .setNegativeButton("Đóng", null)
            .create()
        val searchAdapter = SearchAdapter(results) { result ->
            val intent = Intent(this, LearnActivity::class.java)
            intent.putExtra("EXTRA_TOPIC", result.topic)
            intent.putExtra("EXTRA_WORD_INDEX", result.index)
            startActivity(intent)
            dialog.dismiss()
        }
        rvSearchResults.adapter = searchAdapter
        dialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_import -> {
                showAddTopicDialog()
                true
            }
            R.id.action_achievements -> {
                startActivity(Intent(this, AchievementsActivity::class.java))
                true
            }
            R.id.action_stats -> {
                startActivity(Intent(this, StatsActivity::class.java))
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAddTopicDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_topic, null)
        val etTopicName = dialogView.findViewById<EditText>(R.id.etTopicName)
        val etVocabText = dialogView.findViewById<EditText>(R.id.etVocabularyText)
        val tvValidCount = dialogView.findViewById<TextView>(R.id.tvValidCount)
        val btnCopyPrompt = dialogView.findViewById<Button>(R.id.btnCopyPrompt)
        val btnSeeExample = dialogView.findViewById<Button>(R.id.btnSeeExample)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveTopic)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        etVocabText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val result = parseVocabularyText(s.toString())
                tvValidCount.text = "Số từ hợp lệ: ${result.successCount}"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnCopyPrompt.setOnClickListener {
            val prompt = "Hãy tạo cho tôi danh sách từ vựng về chủ đề [Tên chủ đề] theo định dạng: word - meaning - example. Mỗi từ một dòng."
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("AI Prompt", prompt))
            Toast.makeText(this, "Đã sao chép câu lệnh mẫu", Toast.LENGTH_SHORT).show()
        }

        btnSeeExample.setOnClickListener {
            Toast.makeText(this, "Ví dụ:\napple - quả táo - I eat an apple\nbanana - quả chuối - Monkeys love bananas", Toast.LENGTH_LONG).show()
        }

        btnSave.setOnClickListener {
            val text = etVocabText.text.toString()
            val result = parseVocabularyText(text)
            
            if (result.successCount == 0) {
                Toast.makeText(this, "Không có dòng hợp lệ. Xem lại định dạng", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (result.errorLines.isNotEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Cảnh báo")
                    .setMessage("Chỉ ${result.successCount}/${result.successCount + result.errorLines.size} dòng hợp lệ. Vẫn lưu?")
                    .setPositiveButton("Lưu") { _, _ -> 
                        saveTopicToRoom(etTopicName.text.toString(), result.words)
                        dialog.dismiss() 
                    }
                    .setNegativeButton("Sửa lại", null)
                    .show()
            } else {
                saveTopicToRoom(etTopicName.text.toString(), result.words)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun saveTopicToRoom(name: String, words: List<Word>) {
        val finalName = if (name.isBlank()) {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        } else name

        val topicId = System.currentTimeMillis()
        val topicEntity = TopicEntity(topicId, finalName)
        val wordEntities = words.map { 
            WordEntity(topicId = topicId, word = it.word, meaning = it.meaning, example = it.example)
        }

        lifecycleScope.launch {
            db.progressDao().saveFullTopic(topicEntity, wordEntities)
            Toast.makeText(this@MainActivity, "Đã lưu chủ đề: $finalName", Toast.LENGTH_SHORT).show()
        }
    }
}
