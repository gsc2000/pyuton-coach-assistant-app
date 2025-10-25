package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class Player(
    @SerializedName("playerId")
    val playerId: Int,
    // ★★★ 型を String? に変更 ★★★
    @SerializedName("playerName")
    val playerName: String?,
    @SerializedName("userId")
    val userId: Int,
    @SerializedName("playerBirthday")
    val playerBirthday: String?,
    @SerializedName("playerContractStartDate")
    val playerContractStartDate: String?,
    @SerializedName("playerContractEndDate")
    val playerContractEndDate: String?,
    @SerializedName("playerCreateAt")
    val playerCreateAt: String,
    @SerializedName("playerUpdateAt")
    val playerUpdateAt: String
)