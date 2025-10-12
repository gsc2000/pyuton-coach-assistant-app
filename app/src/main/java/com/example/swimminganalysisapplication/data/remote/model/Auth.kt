package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * トークンレスポンス用スキーマ (OpenAPI仕様準拠)
 */
data class Token(
    @SerializedName("accessToken")
    val accessToken: String,
    @SerializedName("tokenType")
    val tokenType: String = "bearer",
    @SerializedName("expiresIn")
    val expiresIn: Int
)

/**
 * ユーザーログイン用スキーマ (OpenAPI仕様準拠)
 */
data class UserLogin(
    @SerializedName("userEmail")
    val userEmail: String,
    @SerializedName("userPassword")
    val userPassword: String
)