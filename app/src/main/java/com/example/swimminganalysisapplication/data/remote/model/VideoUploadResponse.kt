package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class VideoUploadResponse(
    @SerializedName("filename")
    val filename: String,
    @SerializedName("id")
    val id: String
)