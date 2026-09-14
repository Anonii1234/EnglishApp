package com.tenban.learnenglish

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class SearchResult(val word: Word, val topic: Topic, val index: Int)

class SearchAdapter(
    private var results: List<SearchResult>,
    private val onClick: (SearchResult) -> Unit
) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvWordMeaning: TextView = view.findViewById(R.id.tvWordMeaning)
        val tvTopicName: TextView = view.findViewById(R.id.tvTopicName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = results[position]
        holder.tvWordMeaning.text = "${result.word.word} - ${result.word.meaning}"
        holder.tvTopicName.text = "Chủ đề: ${result.topic.name}"
        holder.itemView.setOnClickListener { onClick(result) }
    }

    override fun getItemCount() = results.size

    fun updateData(newResults: List<SearchResult>) {
        results = newResults
        notifyDataSetChanged()
    }
}
