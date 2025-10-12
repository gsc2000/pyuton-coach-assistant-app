package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class Menu(
    // ★★★ ここから修正 ★★★
    // @SerializedName の値を、APIが返すJSONのキー名（キャメルケース）に合わせる
    @SerializedName("menuId")
    val menuId: Int,

    @SerializedName("menuOrgId")
    val menuOrgId: Int?,

    // クラッシュ防止のため、nullを許容する型に変更
    @SerializedName("menuTitle")
    val menuTitle: String?,

    @SerializedName("menuDescription")
    val menuDescription: String?,

    @SerializedName("menuIsPublic")
    val menuIsPublic: Boolean,

    @SerializedName("menuIsForked")
    val menuIsForked: Boolean,

    @SerializedName("menuForkedFromMenuId")
    val menuForkedFromMenuId: Int?,

    @SerializedName("menuVersion")
    val menuVersion: Int,

    @SerializedName("menuCreateAt")
    val menuCreateAt: String,

    @SerializedName("menuUpdateAt")
    val menuUpdateAt: String,

    @SerializedName("userId")
    val userId: Int,

    @SerializedName("playerId")
    val playerId: Int?,

    // この項目はJSONレスポンスに含まれていないため、変更しない
    @SerializedName("menu_tag_id")
    val menuTagId: Int?
    // ★★★ ここまで修正 ★★★
)