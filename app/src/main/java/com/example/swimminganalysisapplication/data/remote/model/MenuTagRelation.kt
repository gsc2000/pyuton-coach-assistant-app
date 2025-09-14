package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class MenuTagRelation(
    @SerializedName("menu_tag_id")
    val menuTagId: Int,
    @SerializedName("menu_id")
    val menuId: Int,
    @SerializedName("tag_id")
    val tagId: Int
)
