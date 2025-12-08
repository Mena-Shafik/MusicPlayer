package com.example.musicplayer.radio

import com.example.musicplayer.model.RadioStation
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Interceptor

interface RadioApiService {
    // Example: https://api.radio-browser.info/json/stations/search?name=jazz
    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("name") name: String,
        @Query("limit") limit: Int = 50,
        @Query("country") country: String? = null,
        @Query("state") state: String? = null
    ): List<RadioStation>

    /**
     * Search stations near a geographic point.
     * Assumption: Radio Browser exposes a `json/stations/nearby` endpoint that accepts
     * `lat` and `lng` query parameters and optional `limit` and `distance` (km).
     * If the server uses different parameter names, this can be adjusted later.
     */
    @GET("json/stations/nearby")
    suspend fun searchStationsNearby(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("limit") limit: Int = 50,
        @Query("distance") distanceKm: Int? = 80
    ): List<RadioStation>

    @GET("json/stations/topvote")
    suspend fun topVoted(@Query("limit") limit: Int = 50): List<RadioStation>

    companion object {
        // Use the de1 instance (ensure trailing slash for Retrofit)
        private const val BASE_URL = "https://de1.api.radio-browser.info/"

        fun create(): RadioApiService {
            val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

            // Interceptor that logs request method+URL and a peek of the response body (safe/non-destructive)
            val reqRespLogger = Interceptor { chain ->
                val req = chain.request()
                try { android.util.Log.d("RadioApiService", "Request: ${req.method} ${req.url}") } catch (_: Throwable) {}

                val startNs = System.nanoTime()
                val resp = try {
                    chain.proceed(req)
                } catch (e: Exception) {
                    try { android.util.Log.w("RadioApiService", "HTTP request failed: ${e.message}") } catch (_: Throwable) {}
                    throw e
                }
                val tookMs = (System.nanoTime() - startNs) / 1_000_000

                try {
                    val code = resp.code
                    // Peek up to 64KB of the response body so we don't consume the stream
                    val bodyPreview = resp.peekBody(65536).string().replace("\n", " ")
                    val truncated = if (bodyPreview.length > 10000) bodyPreview.substring(0, 10000) + "..." else bodyPreview
                    android.util.Log.d("RadioApiService", "HTTP Response: $code ${resp.message} (took=${tookMs}ms) body=${truncated}")
                } catch (_: Throwable) {}

                resp
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(reqRespLogger)
                .addInterceptor(logger)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(RadioApiService::class.java)
        }
    }
}
