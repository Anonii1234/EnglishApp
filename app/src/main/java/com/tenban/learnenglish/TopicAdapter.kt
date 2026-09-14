package com.tenban.learnenglish

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TopicAdapter(
    private val topics: List<Topic>,
    private val onTopicClick: (Topic) -> Unit,
    private val onQuizClick: (Topic) -> Unit
) : RecyclerView.Adapter<TopicAdapter.TopicViewHolder>() {

    class TopicViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvTopicIcon)
        val tvName: TextView = view.findViewById(R.id.tvTopicName)
        val tvCount: TextView = view.findViewById(R.id.tvWordCount)
        val btnQuiz: Button = view.findViewById(R.id.btnQuiz)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_topic, parent, false)
        return TopicViewHolder(view)
    }

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        val topic = topics[position]
        holder.tvIcon.text = topic.icon ?: "📁"
        holder.tvName.text = topic.name
        holder.tvCount.text = "${topic.words.size} từ"
        
        holder.itemView.setOnClickListener {
            onTopicClick(topic)
        }
        
        holder.btnQuiz.setOnClickListener {
            onQuizClick(topic)
        }
    }

    override fun getItemCount(): Int = topics.size
}
