package com.andrew.hamsterpet

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

data class ChatTurn(
    val role: String,
    val content: String,
)

object DeepSeekChatClient {
    private const val ENDPOINT = "https://api.deepseek.com/chat/completions"
    private const val MODEL = "deepseek-chat"
    private const val MAX_HISTORY_TURNS = 10

    fun buildRequestBody(
        userMessage: String,
        state: PetState,
        history: List<ChatTurn>,
    ): String {
        val messages = JSONArray().put(message("system", systemPrompt(state)))
        history.takeLast(MAX_HISTORY_TURNS).forEach { turn ->
            if (turn.role in setOf("user", "assistant") && turn.content.isNotBlank()) {
                messages.put(message(turn.role, turn.content))
            }
        }
        messages.put(message("user", userMessage))
        return JSONObject()
            .put("model", MODEL)
            .put("temperature", 0.8)
            .put("max_tokens", 220)
            .put("messages", messages)
            .toString()
    }

    fun parseReply(responseBody: String): String? = runCatching {
        JSONObject(responseBody)
            .optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }.getOrNull()

    fun fetchReply(
        apiKey: String,
        userMessage: String,
        state: PetState,
        history: List<ChatTurn>,
    ): Result<String> = runCatching {
        require(apiKey.isNotBlank()) { "DeepSeek API key is unavailable" }
        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }
        try {
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(buildRequestBody(userMessage, state, history))
            }
            val status = connection.responseCode
            val responseStream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = responseStream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            check(status in 200..299) { "DeepSeek request failed with status $status" }
            parseReply(responseBody) ?: error("DeepSeek returned an empty reply")
        } finally {
            connection.disconnect()
        }
    }

    private fun message(role: String, content: String) = JSONObject()
        .put("role", role)
        .put("content", content)

    private fun systemPrompt(state: PetState): String = buildString {
        append("你是安卓桌宠应用《仓鼠小梵》里的通用 AI 聊天伙伴。")
        append("请始终使用自然、简短、有帮助的简体中文回答，并保留仓鼠伙伴的温暖、活泼和一点俏皮感。")
        append("可以偶尔用小爪子、囤粮或小窝作轻松比喻，但不要过度卖萌，也不要影响信息的准确与清晰。")
        append("你可以正常回答日常、学习、工作、技术、写作和其他一般问题；不要把无关问题强行转成仓鼠话题。")
        append("遇到信息不足、需要实时资料或无法确定的内容，要直接说明限制，并给出下一步建议。")
        append("仓鼠状态仅在用户主动询问桌宠、喂食、休息、建窝或幼崽时作为背景参考：")
        append("satiety=${state.satiety}, affection=${state.affection}, ")
        append("nestLevel=${state.nestLevel}, babies=${state.babies.size}, carePoints=${state.carePoints}.")
    }
}
