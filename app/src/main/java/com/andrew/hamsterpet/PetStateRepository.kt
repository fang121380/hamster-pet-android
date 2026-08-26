package com.andrew.hamsterpet

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.CopyOnWriteArraySet

interface PetStateStorage {
    fun read(): String?
    fun write(value: String)
}

class PetStateStore(
    private val storage: PetStateStorage,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val listeners = CopyOnWriteArraySet<(PetState) -> Unit>()
    private var state: PetState

    init {
        val timestamp = now()
        state = PetStateCodec.decode(storage.read(), timestamp)
        storage.write(PetStateCodec.encode(state))
    }

    @Synchronized
    fun current(): PetState = state

    @Synchronized
    fun update(transform: (PetState) -> PetState): PetState {
        val updated = PetGameEngine.sanitize(transform(state), now())
        if (updated == state) return state
        state = updated
        storage.write(PetStateCodec.encode(updated))
        listeners.forEach { it(updated) }
        return updated
    }

    fun addListener(listener: (PetState) -> Unit): () -> Unit {
        listeners += listener
        return { listeners -= listener }
    }
}

class PetStateRepository private constructor(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val store = PetStateStore(SharedPreferencesStorage(preferences))

    fun current(): PetState = store.current()

    fun refresh(now: Long = System.currentTimeMillis()): PetState = store.update { state ->
        val decayed = PetGameEngine.applyDecay(state, now)
        if (PetGameEngine.isBirthReady(decayed, now)) {
            PetGameEngine.completeBreeding(decayed, now)
        } else {
            decayed
        }
    }

    fun update(transform: (PetState) -> PetState): PetState = store.update(transform)

    fun addListener(listener: (PetState) -> Unit): () -> Unit = store.addListener(listener)

    fun reset(now: Long = System.currentTimeMillis()): PetState = store.update {
        PetState(lastSatietyUpdateAt = now)
    }

    private class SharedPreferencesStorage(
        private val preferences: SharedPreferences,
    ) : PetStateStorage {
        override fun read(): String? = preferences.getString(KEY_STATE, null)

        override fun write(value: String) {
            preferences.edit().putString(KEY_STATE, value).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "hamster_pet_state"
        private const val KEY_STATE = "pet_state_v2"

        @Volatile
        private var instance: PetStateRepository? = null

        fun get(context: Context): PetStateRepository = instance ?: synchronized(this) {
            instance ?: PetStateRepository(context).also { instance = it }
        }
    }
}
