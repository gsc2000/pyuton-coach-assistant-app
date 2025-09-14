package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class Chat(
    @SerializedName("chat_id")
    val chatId: Int,
    @SerializedName("chat_content")
    val chatContent: String,
    @SerializedName("chat_sent_at")
    val chatSentAt: String,
    @SerializedName("chatthread_id")
    val chatThreadId: Int?,
    @SerializedName("user_id")
    val userId: Int
)
