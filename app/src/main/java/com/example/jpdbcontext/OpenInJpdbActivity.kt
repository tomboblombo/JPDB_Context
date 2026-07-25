package com.example.jpdbcontext

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast

class OpenInJpdbActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("jpdb_prefs", MODE_PRIVATE)
        if (prefs.getString("api_key", "").isNullOrEmpty()) {
            Toast.makeText(this, "Please set your API key first", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
            return
        }

        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)

        if (text != null) {
            val url = "https://jpdb.io/search?q=${Uri.encode(text.toString())}"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        }
        finish()
    }
}
