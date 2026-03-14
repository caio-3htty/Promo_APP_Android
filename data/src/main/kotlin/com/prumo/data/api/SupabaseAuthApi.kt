package com.prumo.data.api

import com.prumo.data.model.AuthResponseDto
import com.prumo.core.model.SupabaseConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class SupabaseAuthApi(
    private val config: SupabaseConfig,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private val client = OkHttpClient()
    private val mediaType = "application/json".toMediaType()

    fun login(email: String, password: String): AuthResponseDto {
        val body = """{"email":"$email","password":"$password"}""".toRequestBody(mediaType)
        val request = Request.Builder()
            .url("${config.baseUrl}/auth/v1/token?grant_type=password")
            .header("apikey", config.anonKey)
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("Auth login failed (${response.code}): $payload")
            return json.decodeFromString(AuthResponseDto.serializer(), payload)
        }
    }

    fun refresh(refreshToken: String): AuthResponseDto {
        val body = """{"refresh_token":"$refreshToken"}""".toRequestBody(mediaType)
        val request = Request.Builder()
            .url("${config.baseUrl}/auth/v1/token?grant_type=refresh_token")
            .header("apikey", config.anonKey)
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("Auth refresh failed (${response.code}): $payload")
            return json.decodeFromString(AuthResponseDto.serializer(), payload)
        }
    }
}