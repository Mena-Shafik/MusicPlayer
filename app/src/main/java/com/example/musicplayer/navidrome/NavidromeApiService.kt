package com.example.musicplayer.navidrome

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

interface NavidromeApiService {
    @GET("rest/ping.view")
    suspend fun ping(): SubsonicResponseWrapper

    @GET("rest/getAlbumList2.view")
    suspend fun getAlbumList2(
        @Query("type") type: String = "alphabeticalByName",
        @Query("size") size: Int = 500,
        @Query("offset") offset: Int = 0
    ): SubsonicResponseWrapper

    @GET("rest/search3.view")
    suspend fun search3(
        @Query("query") query: String = "",
        @Query("songCount") songCount: Int = 500,
        @Query("songOffset") songOffset: Int = 0,
        @Query("albumCount") albumCount: Int = 0,
        @Query("artistCount") artistCount: Int = 0
    ): SubsonicResponseWrapper

    companion object {
        private const val API_VERSION = "1.16.1"
        private const val CLIENT_ID = "MusicPlayerApp"

        // Subsonic authenticates via query params on every request, so credentials are injected through an interceptor rather than repeated on each endpoint method.
        fun create(baseUrl: String, username: String, password: String, trustSelfSigned: Boolean): NavidromeApiService {
            val normalizedBaseUrl = normalizeBaseUrl(baseUrl)

            val authInterceptor = Interceptor { chain ->
                val salt = generateSalt()
                val token = md5Hex(password + salt)
                val url = chain.request().url.newBuilder()
                    .addQueryParameter("u", username)
                    .addQueryParameter("t", token)
                    .addQueryParameter("s", salt)
                    .addQueryParameter("v", API_VERSION)
                    .addQueryParameter("c", CLIENT_ID)
                    .addQueryParameter("f", "json")
                    .build()
                chain.proceed(chain.request().newBuilder().url(url).build())
            }

            val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

            val clientBuilder = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logger)

            if (trustSelfSigned) {
                applyTrustAllCerts(clientBuilder)
            }

            val retrofit = Retrofit.Builder()
                .baseUrl(normalizedBaseUrl)
                .client(clientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(NavidromeApiService::class.java)
        }

        private fun normalizeBaseUrl(baseUrl: String): String =
            if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        // Plain (non-Retrofit) authenticated URL for streaming one song -- handed to the existing local pipeline as Song.path, which already plays http(s) sources (PlayerForegroundService.prepareCurrent).
        fun buildStreamUrl(baseUrl: String, username: String, password: String, songId: String): String {
            val salt = generateSalt()
            val token = md5Hex(password + salt)
            return (normalizeBaseUrl(baseUrl) + "rest/stream.view").toHttpUrl().newBuilder()
                .addQueryParameter("id", songId)
                .addQueryParameter("u", username)
                .addQueryParameter("t", token)
                .addQueryParameter("s", salt)
                .addQueryParameter("v", API_VERSION)
                .addQueryParameter("c", CLIENT_ID)
                .addQueryParameter("f", "json")
                .build()
                .toString()
        }

        // Plain authenticated cover-art URL, loaded directly via Util.loadBitmapFromUrl -- much cheaper than streaming the whole track to read embedded art tags.
        fun buildCoverArtUrl(baseUrl: String, username: String, password: String, coverArtId: String, size: Int = 300): String {
            val salt = generateSalt()
            val token = md5Hex(password + salt)
            return (normalizeBaseUrl(baseUrl) + "rest/getCoverArt.view").toHttpUrl().newBuilder()
                .addQueryParameter("id", coverArtId)
                .addQueryParameter("size", size.toString())
                .addQueryParameter("u", username)
                .addQueryParameter("t", token)
                .addQueryParameter("s", salt)
                .addQueryParameter("v", API_VERSION)
                .addQueryParameter("c", CLIENT_ID)
                .build()
                .toString()
        }

        private fun generateSalt(): String {
            val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
            val random = SecureRandom()
            return (1..8).map { chars[random.nextInt(chars.length)] }.joinToString("")
        }

        fun md5Hex(input: String): String {
            val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }

        // Only applied when the user explicitly opts in per-server ("Allow self-signed certificate"); scoped to this one client instance, never global -- internal (not private) so NavidromeRepository's reachability check can reuse it.
        internal fun applyTrustAllCerts(builder: OkHttpClient.Builder) {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier(HostnameVerifier { _, _ -> true })
        }
    }
}
