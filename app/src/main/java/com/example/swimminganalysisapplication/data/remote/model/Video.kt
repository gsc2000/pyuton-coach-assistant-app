package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class Video(
    @SerializedName("video_id")
    val videoId: Int,
    @SerializedName("video_is_pose")
    val videoIsPose: Boolean,
    @SerializedName("video_uploaded_at")
    val videoUploadedAt: String,
    @SerializedName("user_id")
    val userId: Int
)
