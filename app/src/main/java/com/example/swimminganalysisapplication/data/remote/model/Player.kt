package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class Player(
    @SerializedName("player_id")
    val playerId: Int,
    @SerializedName("player_name")
    val playerName: String,
    @SerializedName("player_birthday")
    val playerBirthday: String?,
    @SerializedName("player_contract_start_date")
    val playerContractStartDate: String?,
    @SerializedName("player_contract_end_date")
    val playerContractEndDate: String?,
    @SerializedName("player_create_at")
    val playerCreateAt: String,
    @SerializedName("player_update_at")
    val playerUpdateAt: String,
    @SerializedName("user_id")
    val userId: Int
)
