package com.example.swimminganalysisapplication.data.remote // パッケージ名は適宜調整してください

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://sra-app-dev-c8e37e4463c6.herokuapp.com/"

    // OkHttpクライアントの設定 (ログ出力インターセプターを含む)
    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // リクエスト/レスポンスの詳細をログ出力
        }
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS) // 接続タイムアウト
            .readTimeout(30, TimeUnit.SECONDS)    // 読み取りタイムアウト
            .writeTimeout(30, TimeUnit.SECONDS)   // 書き込みタイムアウト
            .build()
    }

    // Retrofitインスタンスの生成
    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // カスタムOkHttpクライアントを設定
            .addConverterFactory(GsonConverterFactory.create()) // JSONコンバータ (Gson)
            // .addConverterFactory(ScalarsConverterFactory.create()) // もしレスポンスが本当にプレーンな文字列の場合、こちらが必要になることも
            .build()
            .create(ApiService::class.java)
    }
}
