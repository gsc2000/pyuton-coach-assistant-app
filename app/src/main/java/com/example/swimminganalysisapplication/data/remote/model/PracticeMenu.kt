package com.example.swimminganalysisapplication.data.remote.model

import java.util.UUID

data class PracticeMenu(
    val id: UUID,
    val user_id: UUID,
    val title: String,
    val description: String?,
    val is_public: Boolean,
    val tags: List<String>?,
    val version: Int,
    val is_forked: Boolean,
    val forked_from_menu_id: UUID?,
    val created_at: String,
    val updated_at: String
)
