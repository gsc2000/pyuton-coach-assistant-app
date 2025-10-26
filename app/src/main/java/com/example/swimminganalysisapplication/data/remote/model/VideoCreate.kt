package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class VideoCreate(
    @SerializedName("video_title")
    val videoTitle: String,
    @SerializedName("video_is_pose")
    val videoIsPose: Boolean = false,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("video_uuid")
    val videoUuid: String
)
