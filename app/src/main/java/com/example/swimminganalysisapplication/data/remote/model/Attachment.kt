package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class Attachment(
    @SerializedName("attachment_id")
    val attachmentId: Int,
    @SerializedName("attachment_file_path")
    val attachmentFilePath: String,
    @SerializedName("attachment_type")
    val attachmentType: String
)
