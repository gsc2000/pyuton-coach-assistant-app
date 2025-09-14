package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class ApiTag(
    @SerializedName("tag_id")
    val tagId: Int,
    @SerializedName("tag_name")
    val tagName: String
)
