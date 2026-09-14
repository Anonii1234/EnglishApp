package com.tenban.learnenglish

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class WordWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        scope.launch {
            val db = AppDatabase.getDatabase(context)
            val allTopics = withContext(Dispatchers.IO) {
                db.progressDao().getAllTopicsWithWords().first()
            }

            if (allTopics.isNotEmpty()) {
                // To ensure it's the same word for the day, we can seed the random with the date
                val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date()).toLong()
                val random = Random(today)
                
                val randomTopicWithWords = allTopics[random.nextInt(allTopics.size)]
                if (randomTopicWithWords.words.isNotEmpty()) {
                    val randomWordIndex = random.nextInt(randomTopicWithWords.words.size)
                    val wordEntity = randomTopicWithWords.words[randomWordIndex]

                    val topic = Topic(
                        id = randomTopicWithWords.topic.id,
                        name = randomTopicWithWords.topic.name,
                        icon = randomTopicWithWords.topic.icon,
                        words = randomTopicWithWords.words.map { 
                            Word(it.word, it.meaning, it.example, it.pronunciation)
                        }
                    )

                    val views = RemoteViews(context.packageName, R.layout.word_widget)
                    val prefix = context.getString(R.string.widget_title_prefix)
                    val fullText = "$prefix ${wordEntity.word} - ${wordEntity.meaning}"
                    views.setTextViewText(R.id.tvWidgetFullContent, fullText)

                    // Intent to open LearnActivity
                    val intent = Intent(context, LearnActivity::class.java).apply {
                        putExtra("EXTRA_TOPIC", topic)
                        putExtra("EXTRA_WORD_INDEX", randomWordIndex)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    
                    val pendingIntent = PendingIntent.getActivity(
                        context, 
                        appWidgetId, 
                        intent, 
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        scope.cancel()
    }
}
