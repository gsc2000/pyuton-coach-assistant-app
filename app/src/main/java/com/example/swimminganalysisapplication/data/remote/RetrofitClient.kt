package com.example.swimminganalysisapplication.data.remote // パッケージ名は適宜調整してください

import android.content.Context
import com.example.swimminganalysisapplication.data.storage.UserPreferences
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // private const val BASE_URL = "https://swimming-race-analysis-db-e037cf1a2449.herokuapp.com/" // Heroku (Production)
//    private const val BASE_URL = "http://10.0.2.2:8000/" // Local FastAPI for Emulator (Development)
    private const val BASE_URL = "http://100.98.57.38/" // Local FastAPI for Emulator (Development)

    private var INSTANCE: ApiService? = null

    // シングルトンインスタンスの取得
    fun getInstance(context: Context): ApiService {
        return INSTANCE ?: synchronized(this) {
            val instance = buildRetrofit(context).create(ApiService::class.java)
            INSTANCE = instance
            instance
        }
    }

    // Retrofitインスタンスのビルド
    private fun buildRetrofit(context: Context): Retrofit {
        val userPreferences = UserPreferences(context.applicationContext)
        val authInterceptor = AuthInterceptor(userPreferences)

        // OkHttpクライアントの設定 (ログ出力インターセプターを含む)
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // リクエスト/レスポンスの詳細をログ出力
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor) // 認証インターセプター
            .addInterceptor(loggingInterceptor) // ログ出力インターセプター
            .connectTimeout(30, TimeUnit.SECONDS) // 接続タイムアウト
            .readTimeout(30, TimeUnit.SECONDS)    // 読み取りタイムアウト
            .writeTimeout(30, TimeUnit.SECONDS)   // 書き込みタイムアウト
            .build()

        // Retrofitビルダー
        return Retrofit.Builder()
            .baseUrl(BASE_URL) // 現在は開発用URLが使用される
            .client(okHttpClient) // カスタムOkHttpクライアントを設定
            .addConverterFactory(GsonConverterFactory.create()) // JSONコンバータ (Gson)
            // .addConverterFactory(ScalarsConverterFactory.create()) // もしレスポンスが本当にプレーンな文字列の場合、こちらが必要になることも
            .build()
    }
}