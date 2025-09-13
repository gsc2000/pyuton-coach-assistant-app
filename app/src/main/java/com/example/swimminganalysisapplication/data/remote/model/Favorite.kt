package com.example.swimminganalysisapplication.data.remote.model

import java.util.UUID

data class Favorite(
    val user_id: UUID,
    val menu_id: UUID,
    val favorited_at: String
)
