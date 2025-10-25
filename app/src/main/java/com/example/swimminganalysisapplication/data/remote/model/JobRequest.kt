package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * Request body for the POST /api/v1/job/inference endpoint.
 */
data class JobRequest(
    @SerializedName("fileId")
    val fileId: String
)