package com.example.jpdbcontext

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast

class AddToDeckActivity : AppCompatActivity() {
    private lateinit var jpdbApi: JpdbApi
    private lateinit var deckSpinner: Spinner
    private lateinit var adapter: ArrayAdapter<String>
    private val deckList = mutableListOf<Deck>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("jpdb_prefs", MODE_PRIVATE)
        if (prefs.getString("api_key", "").isNullOrEmpty()) {
            Toast.makeText(this, "Please set your API key first", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_add_to_deck)

        jpdbApi = JpdbApi(this)

        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
        
        deckSpinner = findViewById(R.id.deck_spinner)
        val refreshButton: Button = findViewById(R.id.refresh_button)
        val addButton: Button = findViewById(R.id.add_button)
        val settingsButton: Button = findViewById(R.id.settings_button)
        val coffeeButton: Button = findViewById(R.id.coffee_button)

        adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deckSpinner.adapter = adapter

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        coffeeButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/tomboblombo"))
            startActivity(intent)
        }

        refreshButton.setOnClickListener {
            loadDecks(forceRefresh = true)
        }

        addButton.setOnClickListener {
            val selectedIndex = deckSpinner.selectedItemPosition
            if (selectedIndex >= 0 && selectedIndex < deckList.size) {
                val selectedDeck = deckList[selectedIndex]
                saveLastUsedDeck(selectedDeck)
                addToDeck(selectedDeck, text?.toString() ?: "")
            } else {
                Toast.makeText(this, "Please select a deck", Toast.LENGTH_SHORT).show()
            }
        }

        loadDecks(forceRefresh = false)
    }

    private fun loadDecks(forceRefresh: Boolean) {
        if (forceRefresh) {
            Toast.makeText(this, "Refreshing decks...", Toast.LENGTH_SHORT).show()
        }
        
        jpdbApi.getDecks(forceRefresh, object : JpdbApi.ApiCallback<List<Deck>> {
            override fun onSuccess(result: List<Deck>) {
                runOnUiThread {
                    deckList.clear()
                    deckList.addAll(result)
                    
                    adapter.clear()
                    adapter.addAll(result.map { it.name })

                    // Try to pre-select last used deck
                    val prefs = getSharedPreferences("jpdb_prefs", MODE_PRIVATE)
                    val lastDeckId = prefs.getInt("last_deck_id", -1)
                    if (lastDeckId != -1) {
                        val index = deckList.indexOfFirst { it.id == lastDeckId }
                        if (index != -1) {
                            deckSpinner.post {
                                deckSpinner.setSelection(index)
                            }
                        }
                    }
                }
            }

            override fun onError(error: String) {
                runOnUiThread {
                    Toast.makeText(this@AddToDeckActivity, error, Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun saveLastUsedDeck(deck: Deck) {
        getSharedPreferences("jpdb_prefs", MODE_PRIVATE).edit().apply {
            putInt("last_deck_id", deck.id)
            putString("last_deck_name", deck.name)
            apply()
        }
    }

    private fun addToDeck(deck: Deck, text: String) {
        if (text.isEmpty()) {
            Toast.makeText(this, "No text selected", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Adding to ${deck.name}...", Toast.LENGTH_SHORT).show()
        
        jpdbApi.addVocabularyToDeck(deck.id, text, object : JpdbApi.ApiCallback<Unit> {
            override fun onSuccess(result: Unit) {
                runOnUiThread {
                    Toast.makeText(this@AddToDeckActivity, "Added successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onError(error: String) {
                runOnUiThread {
                    Toast.makeText(this@AddToDeckActivity, "Failed: $error", Toast.LENGTH_LONG).show()
                }
            }
        })
    }
}
