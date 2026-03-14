package com.prumo.data.api

import com.prumo.core.model.SupabaseConfig
import com.prumo.data.storage.SessionStore
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

class SupabaseRestClient(
    private val config: SupabaseConfig,
    private val sessionStore: SessionStore,
    private val authApi: SupabaseAuthApi,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private val mediaType = "application/json".toMediaType()

    private val client = OkHttpClient.Builder()
        .authenticator(TokenRefreshAuthenticator(sessionStore, authApi))
        .addInterceptor { chain ->
            val session = sessionStore.read()
            val request = chain.request().newBuilder()
                .header("apikey", config.anonKey)
                .header("Content-Type", "application/json")
                .apply {
                    if (session?.accessToken?.isNotBlank() == true) {
                        header("Authorization", "Bearer ${session.accessToken}")
                    }
                }
                .build()
            chain.proceed(request)
        }
        .build()

    suspend fun <T> getList(path: String, query: Map<String, String>, deserializer: KSerializer<T>): List<T> {
        val urlBuilder = "${config.baseUrl}/rest/v1/$path".toHttpUrl().newBuilder()
        query.forEach { (key, value) -> urlBuilder.addQueryParameter(key, value) }

        val request = Request.Builder().url(urlBuilder.build()).get().build()
        return client.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("GET $path failed (${response.code}): $payload")
            json.decodeFromString(ListSerializer(deserializer), payload)
        }
    }

    suspend fun post(path: String, bodyJson: String) {
        val request = Request.Builder()
            .url("${config.baseUrl}/rest/v1/$path")
            .header("Prefer", "return=minimal")
            .post(bodyJson.toRequestBody(mediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("POST $path failed (${response.code}): $payload")
        }
    }

    suspend fun patch(path: String, query: Map<String, String>, bodyJson: String) {
        val urlBuilder = "${config.baseUrl}/rest/v1/$path".toHttpUrl().newBuilder()
        query.forEach { (key, value) -> urlBuilder.addQueryParameter(key, value) }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .header("Prefer", "return=minimal")
            .patch(bodyJson.toRequestBody(mediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("PATCH $path failed (${response.code}): $payload")
        }
    }
}

private class TokenRefreshAuthenticator(
    private val sessionStore: SessionStore,
    private val authApi: SupabaseAuthApi
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header("Authorization") == null) return null
        if (responseCount(response) >= 2) return null

        val current = sessionStore.read() ?: return null
        val refreshed = runCatching { authApi.refresh(current.refreshToken) }.getOrNull() ?: return null

        val renewed = current.copy(
            accessToken = refreshed.accessToken,
            refreshToken = refreshed.refreshToken,
            expiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + refreshed.expiresIn
        )
        sessionStore.save(renewed)

        return response.request.newBuilder()
            .header("Authorization", "Bearer ${renewed.accessToken}")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
