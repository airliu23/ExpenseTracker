package com.example.expensetracker.data.remote

import com.example.expensetracker.data.remote.dto.ChatRequest
import com.example.expensetracker.data.remote.dto.ChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface DeepSeekApiService {

    /** 返回 Response 包装：HTTP 非 2xx（限流/网关错误）时上层可直接判断并重试 */
    @POST("chat/completions")
    suspend fun chatCompletions(@Body request: ChatRequest): Response<ChatResponse>
}
