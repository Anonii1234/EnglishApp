package com.tenban.learnenglish

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            // Theme change listener
            findPreference<ListPreference>("theme")?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    applyTheme(newValue as String)
                    true
                }

            // Rate App
            findPreference<Preference>("rate_app")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${requireContext().packageName}"))
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${requireContext().packageName}"))
                    startActivity(intent)
                }
                true
            }

            // Feedback
            findPreference<Preference>("feedback")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("support@tenban.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "Feedback for Learn English App")
                }
                try {
                    startActivity(Intent.createChooser(intent, "Gửi email..."))
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Không tìm thấy ứng dụng email", Toast.LENGTH_SHORT).show()
                }
                true
            }

            // Tutorial
            findPreference<Preference>("tutorial")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Hướng dẫn")
                    .setMessage("1. Chọn chủ đề để bắt đầu học.\n2. Chạm vào thẻ để xem nghĩa.\n3. Vuốt trái/phải để chuyển từ.\n4. Đánh dấu 'Quên' để ôn lại sau.")
                    .setPositiveButton("Đã hiểu", null)
                    .show()
                true
            }
        }

        private fun applyTheme(theme: String) {
            when (theme) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }
}
