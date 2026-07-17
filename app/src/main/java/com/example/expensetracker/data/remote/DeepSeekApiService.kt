package com.example.expensetracker.data.remote

import com.example.expensetracker.data.remote.dto.ChatRequest
import com.example.expensetracker.data.remote.dto.ChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface DeepSeekApiService {

    @POST("chat/completions")
    suspend fun chatCompletions(@Body request: ChatRequest): ChatResponse
}
