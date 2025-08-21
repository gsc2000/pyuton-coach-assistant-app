package com.example.swimminganalysisapplication.data.remote.model // パッケージ名は適宜調整してください

import com.google.gson.annotations.SerializedName

// ログイン成功時のレスポンス。
// APIドキュメントのスキーマが "string" なので、
// レスポンスボディ全体がトークン文字列そのものであると仮定します。
// もし {"access_token": "value", "token_type": "bearer"} のようなJSONオブジェクトの場合、
// 以下のようなデータクラスに変更します。
// data class LoginResponse(
//     @SerializedName("access_token") val accessToken: String,
//     @SerializedName("token_type") val tokenType: String
// )

// 422 Validation Error のレスポンス用
data class HTTPValidationError(
    @SerializedName("detail") val detail: List<ValidationErrorDetail>?
)

data class ValidationErrorDetail(
    @SerializedName("loc") val loc: List<Any>?, // "string" or 0 のような混在リストのため Any
    @SerializedName("msg") val msg: String?,
    @SerializedName("type") val type: String?
)