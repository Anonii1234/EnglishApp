package com.tenban.learnenglish

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AchievementsActivity : AppCompatActivity() {

    private lateinit var rvAchievements: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rvAchievements = findViewById(R.id.rvAchievements)
        rvAchievements.layoutManager = LinearLayoutManager(this)

        loadAchievements()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadAchievements() {
        val earnedIds = BadgeManager.getEarnedBadgeIds(this)
        val adapter = AchievementsAdapter(BadgeManager.allBadges, earnedIds)
        rvAchievements.adapter = adapter
    }

    class AchievementsAdapter(
        private val badges: List<Badge>,
        private val earnedIds: Set<String>
    ) : RecyclerView.Adapter<AchievementsAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvIcon: TextView = view.findViewById(R.id.tvBadgeIcon)
            val tvName: TextView = view.findViewById(R.id.tvBadgeName)
            val tvDescription: TextView = view.findViewById(R.id.tvBadgeDescription)
            val ivLocked: ImageView = view.findViewById(R.id.ivLocked)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_badge, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val badge = badges[position]
            val isEarned = earnedIds.contains(badge.id)

            holder.tvName.text = badge.name
            holder.tvDescription.text = badge.description
            
            if (isEarned) {
                holder.tvIcon.text = badge.icon
                holder.ivLocked.visibility = View.GONE
                holder.itemView.alpha = 1.0f
            } else {
                holder.tvIcon.text = "🔒"
                holder.ivLocked.visibility = View.VISIBLE
                holder.itemView.alpha = 0.5f
            }
        }

        override fun getItemCount() = badges.size
    }
}
