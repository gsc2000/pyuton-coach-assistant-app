package com.example.swimminganalysisapplication.data.remote.model

import java.util.UUID

data class Comment(
    val id: UUID,
    val menu_id: UUID,
    val user_id: UUID,
    val text: String,
    val created_at: String
)
