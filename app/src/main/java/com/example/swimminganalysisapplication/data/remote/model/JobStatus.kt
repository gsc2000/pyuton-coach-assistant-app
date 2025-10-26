package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class JobStatus(
    @SerializedName("job_id")
    val jobId: String,
    val status: String,
    val detail: String? = null
)
