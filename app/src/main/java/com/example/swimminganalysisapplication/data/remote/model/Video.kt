package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class Video(
    @SerializedName("videoId")
    val videoId: Int,
    @SerializedName("videoTitle")
    val videoTitle: String?,
    @SerializedName("videoIsPose")
    val videoIsPose: Boolean?,
    @SerializedName("videoUuid")
    val videoUuid: String,
    @SerializedName("videoUploadedAt")
    val videoUploadedAt: String,
    @SerializedName("userId")
    val userId: Int
)
