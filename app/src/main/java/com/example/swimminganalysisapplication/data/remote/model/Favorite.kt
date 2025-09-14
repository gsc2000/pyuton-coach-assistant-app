package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class Favorite(
    @SerializedName("favorite_id")
    val favoriteId: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("menu_id")
    val menuId: Int,
    @SerializedName("favorite_create_at")
    val favoriteCreateAt: String
)
