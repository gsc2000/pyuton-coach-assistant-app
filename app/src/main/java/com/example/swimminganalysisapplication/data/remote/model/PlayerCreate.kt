package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class PlayerCreate(
    @SerializedName("player_name")
    val playerName: String
)
