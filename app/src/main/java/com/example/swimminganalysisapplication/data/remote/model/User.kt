package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("user_name")
    val userName: String,
    @SerializedName("user_email")
    val userEmail: String,
    @SerializedName("user_birthday")
    val userBirthday: String?,
    @SerializedName("user_create_at")
    val userCreateAt: String,
    @SerializedName("user_update_at")
    val userUpdateAt: String
)
