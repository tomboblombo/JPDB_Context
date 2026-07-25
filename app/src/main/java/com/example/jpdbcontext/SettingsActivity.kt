package com.example.jpdbcontext

import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val apiKeyEditText: EditText = findViewById(R.id.api_key_edit_text)
        val saveButton: Button = findViewById(R.id.save_button)

        val prefs = getSharedPreferences("jpdb_prefs", Context.MODE_PRIVATE)
        apiKeyEditText.setText(prefs.getString("api_key", ""))

        saveButton.setOnClickListener {
            val apiKey = apiKeyEditText.text.toString()
            prefs.edit().putString("api_key", apiKey).apply()
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        }
    }
}
