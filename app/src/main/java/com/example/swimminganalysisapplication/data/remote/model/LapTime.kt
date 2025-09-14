package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class LapTime(
    @SerializedName("lap_number")
    val lapNumber: Int,
    @SerializedName("lap_time")
    val lapTime: Double,
    @SerializedName("stroke_count")
    val strokeCount: Int
)
