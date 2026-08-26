package com.andrew.hamsterpet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSeekChatClientTest {
    @Test
    fun builds_a_deepseek_chat_request_with_pet_context() {
        val state = PetState(satiety = 61, affection = 48, nestLevel = 2)

        val body = DeepSeekChatClient.buildRequestBody(
            userMessage = "你好",
            state = state,
            history = listOf(ChatTurn("assistant", "欢迎回来")),
        )

        assertTrue(body.contains("deepseek-chat"))
        assertTrue(body.contains("简体中文"))
        assertTrue(body.contains("通用 AI 聊天伙伴"))
        assertTrue(body.contains("不要把无关问题强行转成仓鼠话题"))
        assertTrue(body.contains("satiety=61"))
        assertTrue(body.contains("你好"))
    }

    @Test
    fun extracts_the_first_non_blank_reply() {
        val response = """{"choices":[{"message":{"content":"  你好呀  "}}]}"""

        assertEquals("你好呀", DeepSeekChatClient.parseReply(response))
    }

    @Test
    fun returns_null_for_a_response_without_content() {
        assertNull(DeepSeekChatClient.parseReply("""{"choices":[]}"""))
    }
}
