package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * APIレスポンス用 ユーザー情報スキーマ (OpenAPI仕様準拠)
 */
data class User(
    @SerializedName("userId")
    val userId: Int,
    @SerializedName("userName")
    val userName: String,
    @SerializedName("userEmail")
    val userEmail: String,
    @SerializedName("userBirthday")
    val userBirthday: String?,
    @SerializedName("userHashPassword")
    val userHashPassword: String,
    @SerializedName("userCreateAt")
    val userCreateAt: String,
    @SerializedName("userUpdateAt")
    val userUpdateAt: String?,
    @SerializedName("userIsActive")
    val userIsActive: Boolean
)

/**
 * APIリクエスト用 ユーザー作成スキーマ (OpenAPI仕様準拠)
 */
data class UserCreate(
    @SerializedName("userName")
    val userName: String,
    @SerializedName("userEmail")
    val userEmail: String,
    @SerializedName("userPassword")
    val userPassword: String,
    @SerializedName("userBirthday")
    val userBirthday: String? = null
)
