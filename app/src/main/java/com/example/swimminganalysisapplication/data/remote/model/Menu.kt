package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class Menu(
    @SerializedName("menu_id")
    val menuId: Int,
    @SerializedName("menu_org_id")
    val menuOrgId: Int?,
    @SerializedName("menu_title")
    val menuTitle: String,
    @SerializedName("menu_description")
    val menuDescription: String?,
    @SerializedName("menu_is_public")
    val menuIsPublic: Boolean,
    @SerializedName("menu_is_forked")
    val menuIsForked: Boolean,
    @SerializedName("menu_forked_from_menu_id")
    val menuForkedFromMenuId: Int?,
    @SerializedName("menu_version")
    val menuVersion: Int,
    @SerializedName("menu_create_at")
    val menuCreateAt: String,
    @SerializedName("menu_update_at")
    val menuUpdateAt: String,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("player_id")
    val playerId: Int?,
    @SerializedName("menu_tag_id")
    val menuTagId: Int?
)
