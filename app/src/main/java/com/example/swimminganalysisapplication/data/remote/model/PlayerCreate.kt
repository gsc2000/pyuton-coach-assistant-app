package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class PlayerCreate(
    @SerializedName("player_name")
    val playerName: String,

    @SerializedName("player_birthday")
    val playerBirthday: String? = null,

    @SerializedName("player_contract_start_date")
    val playerContractStartDate: String? = null,

    @SerializedName("player_contract_end_date")
    val playerContractEndDate: String? = null,

    @SerializedName("user_id")
    val userId: Int
)