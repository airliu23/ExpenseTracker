package com.example.expensetracker.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatResponse(
    @Json(name = "id") val id: String? = null,
    @Json(name = "choices") val choices: List<Choice> = emptyList(),
    @Json(name = "usage") val usage: Usage? = null
)

@JsonClass(generateAdapter = true)
data class Choice(
    @Json(name = "index") val index: Int = 0,
    @Json(name = "message") val message: ResponseMessage? = null,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class ResponseMessage(
    @Json(name = "role") val role: String = "",
    @Json(name = "content") val content: String = ""
)

@JsonClass(generateAdapter = true)
data class Usage(
    @Json(name = "prompt_tokens") val promptTokens: Int = 0,
    @Json(name = "completion_tokens") val completionTokens: Int = 0,
    @Json(name = "total_tokens") val totalTokens: Int = 0
)
