package com.example.swimminganalysisapplication.data.remote

import com.example.swimminganalysisapplication.data.remote.model.Race
import com.example.swimminganalysisapplication.data.remote.model.Result
import com.example.swimminganalysisapplication.data.remote.model.Swimmer
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- Swimmer Endpoints ---
    @GET("api/swimmers")
    suspend fun getAllSwimmers(): Response<List<Swimmer>>

    @GET("api/swimmers/{id}")
    suspend fun getSwimmer(@Path("id") id: Int): Response<Swimmer>

    @POST("api/swimmers")
    suspend fun createSwimmer(@Body swimmer: Swimmer): Response<Swimmer> // Swimmerオブジェクトをリクエストボディとして送信

    @PUT("api/swimmers/{id}")
    suspend fun updateSwimmer(@Path("id") id: Int, @Body swimmer: Swimmer): Response<Swimmer>

    @DELETE("api/swimmers/{id}")
    suspend fun deleteSwimmer(@Path("id") id: Int): Response<Unit> // 成功時はボディなし(204 No Contentなど)を想定

    // --- Race Endpoints ---
    @GET("api/races")
    suspend fun getAllRaces(): Response<List<Race>>

    @GET("api/races/{id}")
    suspend fun getRace(@Path("id") id: Int): Response<Race>

    @POST("api/races")
    suspend fun createRace(@Body race: Race): Response<Race>

    @PUT("api/races/{id}")
    suspend fun updateRace(@Path("id") id: Int, @Body race: Race): Response<Race>

    @DELETE("api/races/{id}")
    suspend fun deleteRace(@Path("id") id: Int): Response<Unit>

    // --- Result Endpoints ---
    @GET("api/results")
    suspend fun getAllResults(): Response<List<Result>>

    @GET("api/results/{id}")
    suspend fun getResult(@Path("id") id: Int): Response<Result>

    @POST("api/results")
    suspend fun createResult(@Body result: Result): Response<Result>

    @PUT("api/results/{id}")
    suspend fun updateResult(@Path("id") id: Int, @Body result: Result): Response<Result>

    @DELETE("api/results/{id}")
    suspend fun deleteResult(@Path("id") id: Int): Response<Unit>
}
