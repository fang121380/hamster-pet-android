package com.andrew.hamsterpet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetStateStoreTest {
    @Test
    fun missing_storage_initializes_state_with_current_time() {
        val storage = MemoryStorage()

        val store = PetStateStore(storage, now = { 500L })

        assertEquals(500L, store.current().lastSatietyUpdateAt)
        assertEquals(500L, store.current().lastAffectionUpdateAt)
        assertTrue(storage.value?.contains("\"version\":4") == true)
    }

    @Test
    fun update_persists_and_notifies_listener() {
        val storage = MemoryStorage()
        val store = PetStateStore(storage, now = { 500L })
        val observed = mutableListOf<PetState>()
        val remove = store.addListener(observed::add)

        val updated = store.update { it.copy(affection = 77) }
        remove()
        store.update { it.copy(affection = 88) }

        assertEquals(77, updated.affection)
        assertEquals(88, PetStateCodec.decode(storage.value, 500L).affection)
        assertEquals(listOf(77), observed.map { it.affection })
    }

    @Test
    fun unchanged_update_does_not_write_or_notify() {
        val storage = MemoryStorage()
        val store = PetStateStore(storage, now = { 500L })
        var notifications = 0
        store.addListener { notifications++ }
        val writesAfterInit = storage.writeCount

        store.update { it }

        assertEquals(writesAfterInit, storage.writeCount)
        assertEquals(0, notifications)
    }

    private class MemoryStorage : PetStateStorage {
        var value: String? = null
        var writeCount: Int = 0
        override fun read(): String? = value
        override fun write(value: String) {
            this.value = value
            writeCount++
        }
    }
}
