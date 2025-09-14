package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class ChatThread(
    @SerializedName("chatthread_id")
    val chatThreadId: Int,
    @SerializedName("chatthread_title")
    val chatThreadTitle: String?,
    @SerializedName("chatthread_create_at")
    val chatThreadCreateAt: String,
    @SerializedName("chat_id")
    val chatId: Int
)
