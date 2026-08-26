package com.andrew.hamsterpet

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

interface ChatHistoryStorage {
    fun read(): String?
    fun write(value: String)
    fun clear()
}

class ChatHistoryStore(
    private val storage: ChatHistoryStorage,
) {
    fun read(): List<ChatTurn> = ChatHistoryCodec.decode(storage.read())

    fun save(history: List<ChatTurn>) {
        storage.write(ChatHistoryCodec.encode(history))
    }

    fun clear() {
        storage.clear()
    }
}

object ChatHistoryCodec {
    const val MAX_SAVED_TURNS = 80
    private const val MAX_MESSAGE_LENGTH = 2_000

    fun encode(history: List<ChatTurn>): String {
        val turns = JSONArray()
        sanitize(history).forEach { turn ->
            turns.put(JSONObject().put("role", turn.role).put("content", turn.content))
        }
        return JSONObject()
            .put("version", 1)
            .put("turns", turns)
            .toString()
    }

    fun decode(raw: String?): List<ChatTurn> = runCatching {
        val turns = JSONObject(raw.orEmpty()).optJSONArray("turns") ?: return emptyList()
        buildList {
            for (index in 0 until turns.length()) {
                val item = turns.optJSONObject(index) ?: continue
                val role = item.optString("role")
                val content = item.optString("content")
                if (role in VALID_ROLES && content.isNotBlank()) {
                    add(ChatTurn(role, content.take(MAX_MESSAGE_LENGTH)))
                }
            }
        }.takeLast(MAX_SAVED_TURNS)
    }.getOrDefault(emptyList())

    private fun sanitize(history: List<ChatTurn>): List<ChatTurn> = history
        .asSequence()
        .filter { it.role in VALID_ROLES && it.content.isNotBlank() }
        .map { ChatTurn(it.role, it.content.take(MAX_MESSAGE_LENGTH)) }
        .toList()
        .takeLast(MAX_SAVED_TURNS)

    private val VALID_ROLES = setOf("user", "assistant")
}

class ChatHistoryRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val store = ChatHistoryStore(SharedPreferencesStorage(preferences))

    fun read(): List<ChatTurn> = store.read()

    fun save(history: List<ChatTurn>) {
        store.save(history)
    }

    fun clear() {
        store.clear()
    }

    private class SharedPreferencesStorage(
        private val preferences: SharedPreferences,
    ) : ChatHistoryStorage {
        override fun read(): String? = preferences.getString(KEY_HISTORY, null)

        override fun write(value: String) {
            preferences.edit().putString(KEY_HISTORY, value).apply()
        }

        override fun clear() {
            preferences.edit().remove(KEY_HISTORY).apply()
        }
    }

    private companion object {
        const val PREFS_NAME = "hamster_pet_chat"
        const val KEY_HISTORY = "history_v1"
    }
}
