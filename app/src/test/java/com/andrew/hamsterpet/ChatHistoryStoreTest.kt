package com.andrew.hamsterpet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ChatHistoryStoreTest {
    @Test
    fun saves_and_restores_valid_turns() {
        val storage = MemoryStorage()
        val store = ChatHistoryStore(storage)
        val turns = listOf(
            ChatTurn("assistant", "欢迎回来"),
            ChatTurn("user", "今天吃什么？"),
        )

        store.save(turns)

        assertEquals(turns, store.read())
    }

    @Test
    fun ignores_invalid_or_malformed_saved_content() {
        val storage = MemoryStorage(
            """{"turns":[{"role":"system","content":"skip"},{"role":"user","content":"保留"}]}""",
        )
        val store = ChatHistoryStore(storage)

        assertEquals(listOf(ChatTurn("user", "保留")), store.read())
        storage.value = "not json"
        assertEquals(emptyList<ChatTurn>(), store.read())
    }

    @Test
    fun keeps_the_most_recent_eighty_turns_and_can_clear() {
        val storage = MemoryStorage()
        val store = ChatHistoryStore(storage)
        val turns = (1..82).map { ChatTurn("user", "第 $it 条") }

        store.save(turns)

        assertEquals(80, store.read().size)
        assertEquals("第 3 条", store.read().first().content)
        store.clear()
        assertFalse(storage.value?.isNotEmpty() == true)
    }

    private class MemoryStorage(initialValue: String? = null) : ChatHistoryStorage {
        var value: String? = initialValue

        override fun read(): String? = value

        override fun write(value: String) {
            this.value = value
        }

        override fun clear() {
            value = null
        }
    }
}
