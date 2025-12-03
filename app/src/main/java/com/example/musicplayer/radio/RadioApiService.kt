package com.example.musicplayer.radio

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

interface RadioApiService {
    // Example: https://api.radio-browser.info/json/stations/search?name=jazz
    @GET("json/stations/search")
    suspend fun searchStations(@Query("name") name: String, @Query("limit") limit: Int = 50): List<RadioStation>

    @GET("json/stations/topvote")
    suspend fun topVoted(@Query("limit") limit: Int = 50): List<RadioStation>

    companion object {
        private const val BASE_URL = "https://api.radio-browser.info/"

        fun create(): RadioApiService {
            val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            val client = OkHttpClient.Builder().addInterceptor(logger).build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(RadioApiService::class.java)
        }
    }
}
