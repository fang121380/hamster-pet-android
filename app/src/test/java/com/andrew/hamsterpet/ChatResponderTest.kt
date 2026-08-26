package com.andrew.hamsterpet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatResponderTest {
    @Test
    fun replies_to_care_topics_with_relevant_text() {
        assertTrue(ChatResponder.replyFor("你饿了吗").contains("点心"))
        assertTrue(ChatResponder.replyFor("该睡觉了").contains("小窝"))
        assertTrue(ChatResponder.replyFor("我们去建窝吧").contains("照料"))
    }

    @Test
    fun family_reply_reflects_the_current_baby_count() {
        assertTrue(ChatResponder.replyFor("宝宝怎么样", babyCount = 2).contains("2"))
        assertTrue(ChatResponder.replyFor("宝宝怎么样", babyCount = 0).contains("二级"))
    }

    @Test
    fun blank_input_has_a_stable_prompt() {
        assertEquals("我在这里，慢慢说。", ChatResponder.replyFor("   "))
    }
}
