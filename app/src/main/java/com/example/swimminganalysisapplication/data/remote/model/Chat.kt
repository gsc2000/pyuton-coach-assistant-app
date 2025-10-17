package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class Chat(
    val chatContent: String,
    
    @SerializedName("chatthreadId") // JSONの 'chatthreadId' とマッピング
    val chatThreadId: Int?,

    val userId: Int,
    val menuId: Int?,
    val chatId: Int,
    val chatSentAt: String
)

data class ChatCreate(
    val chatContent: String,
    val userId: Int,
    val menuId: Int?,

    @SerializedName("chatthreadId") // JSONの 'chatthreadId' とマッピング
    val chatThreadId: Int?
)
