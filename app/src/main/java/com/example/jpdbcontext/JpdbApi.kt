package com.example.jpdbcontext

import android.content.Context
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class Deck(val id: Int, val name: String)

class JpdbApi(context: Context) {
    private val client = OkHttpClient()
    private val prefs = context.getSharedPreferences("jpdb_prefs", Context.MODE_PRIVATE)

    interface ApiCallback<T> {
        fun onSuccess(result: T)
        fun onError(error: String)
    }

    fun getDecks(forceRefresh: Boolean, callback: ApiCallback<List<Deck>>) {
        val cachedDecks = getCachedDecks()
        if (!forceRefresh && cachedDecks.isNotEmpty()) {
            callback.onSuccess(cachedDecks)
            return
        }

        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            callback.onError("API key not set in settings")
            return
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val jsonBody = JSONObject().apply {
            put("fields", JSONArray().apply {
                put("id")
                put("name")
            })
        }.toString()

        val request = Request.Builder()
            .url("https://jpdb.io/api/v1/list-user-decks")
            .header("Authorization", "Bearer $apiKey")
            .post(jsonBody.toRequestBody(mediaType))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e.message ?: "Unknown error")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { 
                    val body = it.body?.string()
                    if (!it.isSuccessful || body == null) {
                        callback.onError("Error: ${it.code}")
                        return
                    }

                    try {
                        val json = JSONObject(body)
                        val decksArray = json.getJSONArray("decks")
                        val decks = mutableListOf<Deck>()
                        for (i in 0 until decksArray.length()) {
                            val deckEntry = decksArray.getJSONArray(i)
                            decks.add(Deck(deckEntry.getInt(0), deckEntry.getString(1)))
                        }

                        if (decks.isNotEmpty()) {
                            saveCachedDecks(body)
                        }
                        callback.onSuccess(decks)
                    } catch (e: Exception) {
                        callback.onError("Failed to parse response")
                    }
                }
            }
        })
    }

    fun addVocabularyToDeck(deckId: Int, text: String, callback: ApiCallback<Unit>) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            callback.onError("API key not set")
            return
        }

        // 1. Parse text to get vid/sid
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val parseJson = JSONObject().apply {
            put("text", text)
            put("token_fields", JSONArray())
            put("vocabulary_fields", JSONArray().apply {
                put("vid")
                put("sid")
            })
        }.toString()

        val parseRequest = Request.Builder()
            .url("https://jpdb.io/api/v1/parse")
            .header("Authorization", "Bearer $apiKey")
            .post(parseJson.toRequestBody(mediaType))
            .build()

        client.newCall(parseRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError("Parse failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { 
                    val body = it.body?.string()
                    if (!it.isSuccessful || body == null) {
                        callback.onError("Parse error: ${it.code}")
                        return
                    }

                    try {
                        val json = JSONObject(body)
                        val vocabulary = json.getJSONArray("vocabulary")
                        if (vocabulary.length() == 0) {
                            callback.onError("No vocabulary found in text")
                            return
                        }

                        // Use all found vocabulary pairs
                        performAdd(deckId, vocabulary, apiKey, callback)
                    } catch (e: Exception) {
                        callback.onError("Parse response handling failed")
                    }
                }
            }
        })
    }

    private fun performAdd(deckId: Int, vocabulary: JSONArray, apiKey: String, callback: ApiCallback<Unit>) {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val addJson = JSONObject().apply {
            put("id", deckId)
            put("vocabulary", vocabulary)
            put("ignore_unknown", true)
        }.toString()

        val request = Request.Builder()
            .url("https://jpdb.io/api/v1/deck/add-vocabulary")
            .header("Authorization", "Bearer $apiKey")
            .post(addJson.toRequestBody(mediaType))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError("Add failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { 
                    if (it.isSuccessful) {
                        callback.onSuccess(Unit)
                    } else {
                        callback.onError("Add error: ${it.code}")
                    }
                }
            }
        })
    }

    private fun getApiKey() = prefs.getString("api_key", "") ?: ""

    private fun getCachedDecks(): List<Deck> {
        val body = prefs.getString("cached_decks", null) ?: return emptyList()
        return try {
            val json = JSONObject(body)
            val decksArray = json.getJSONArray("decks")
            val decks = mutableListOf<Deck>()
            for (i in 0 until decksArray.length()) {
                val deckEntry = decksArray.getJSONArray(i)
                decks.add(Deck(deckEntry.getInt(0), deckEntry.getString(1)))
            }
            decks
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveCachedDecks(jsonBody: String) {
        prefs.edit().putString("cached_decks", jsonBody).apply()
    }
}
