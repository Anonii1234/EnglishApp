package com.tenban.learnenglish

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.io.Serializable

data class VocabularyResponse(
    val topics: List<Topic>
) : Serializable

data class Topic(
    val id: Long,
    var name: String,
    val icon: String? = null,
    val words: List<Word>
) : Serializable

data class Word(
    val word: String,
    val meaning: String,
    val example: String,
    val pronunciation: String? = null
) : Serializable

data class ParseResult(
    val words: List<Word>,
    val successCount: Int,
    val errorLines: List<Int>,
    val errorMessage: String?
)

fun parseVocabularyText(input: String): ParseResult {
    val resultWords = mutableListOf<Word>()
    val errorLines = mutableListOf<Int>()
    
    // 1. Chuẩn hóa các loại dấu gạch ngang thành '-'
    val normalizedInput = input.replace(Regex("[—–]"), "-")
    
    // 2. Tách dòng, bỏ qua dòng trống
    normalizedInput.lines().forEachIndexed { index, line ->
        val lineNumber = index + 1
        if (line.isBlank()) return@forEachIndexed
        
        // 3. Tách theo regex "\s-\s" (có thể linh hoạt hơn để đảm bảo tính ổn định)
        // Dùng regex: \s?-\s? để chấp nhận cả "word-meaning" và "word - meaning"
        val parts = line.split(Regex("\\s-\\s"))
        
        if (parts.size >= 2) {
            val word = parts[0].trim()
            val meaning = parts[1].trim()
            val example = if (parts.size >= 3) parts[2].trim() else ""
            
            if (word.isNotEmpty() && meaning.isNotEmpty()) {
                resultWords.add(Word(word, meaning, example))
            } else {
                errorLines.add(lineNumber)
            }
        } else {
            errorLines.add(lineNumber)
        }
    }
    
    val errorMessage = if (errorLines.isNotEmpty()) {
        "Định dạng không hợp lệ tại dòng: ${errorLines.joinToString(", ")}"
    } else null
    
    return ParseResult(resultWords, resultWords.size, errorLines, errorMessage)
}

object VocabularyManager {
    private const val PREFS_NAME = "VocabularyPrefs"
    private const val KEY_TOPICS = "topics_list"

    fun loadTopics(context: Context): MutableList<Topic> {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = sharedPref.getString(KEY_TOPICS, null)

        return if (jsonString != null) {
            val type = object : TypeToken<MutableList<Topic>>() {}.type
            Gson().fromJson(jsonString, type)
        } else {
            val fromAssets = loadVocabularyFromAssets(context)?.topics ?: mutableListOf()
            saveTopics(context, fromAssets)
            fromAssets.toMutableList()
        }
    }

    fun saveTopics(context: Context, topics: List<Topic>) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = Gson().toJson(topics)
        sharedPref.edit {
            putString(KEY_TOPICS, jsonString)
        }
    }

    private fun loadVocabularyFromAssets(context: Context): VocabularyResponse? {
        val jsonString: String = try {
            context.assets.open("vocabulary.json").bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return null
        }
        return Gson().fromJson(jsonString, VocabularyResponse::class.java)
    }
}
