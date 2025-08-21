package com.example.swimminganalysisapplication.data.remote // パッケージ名は適宜調整してください

import com.example.swimminganalysisapplication.data.remote.model.HTTPValidationError // 先ほど作成したデータクラス
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ApiService {

    @FormUrlEncoded // リクエストボディを application/x-www-form-urlencoded 形式にする
    @POST("api/token") // エンドポイントのパス
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("grant_type") grantType: String? = "password", // 一般的なデフォルト値
        @Field("scope") scope: String? = null,
        @Field("client_id") clientId: String? = null,
        @Field("client_secret") clientSecret: String? = null
    ): Response<String> // 成功時はトークン文字列そのものが返ると仮定。ResponseでラップしてHTTPステータス等も取得
    // もし LoginResponse のようなJSONオブジェクトが返る場合は Response<LoginResponse> に変更
}

