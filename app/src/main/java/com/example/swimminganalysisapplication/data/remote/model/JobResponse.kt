package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * Response body from the POST /api/v1/job/inference endpoint.
 */
data class JobResponse(
    @SerializedName("jobId")
    val jobId: String
)