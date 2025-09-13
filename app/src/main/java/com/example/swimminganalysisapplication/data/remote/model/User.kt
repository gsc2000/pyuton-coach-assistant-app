package com.example.swimminganalysisapplication.data.remote.model

import java.util.UUID

data class User(
    val id: UUID,
    val username: String,
    val email: String?,
    val created_at: String,
    val updated_at: String
)
