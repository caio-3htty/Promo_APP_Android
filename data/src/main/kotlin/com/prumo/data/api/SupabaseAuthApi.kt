package com.prumo.data.api

import com.prumo.data.model.AuthResponseDto
import com.prumo.data.model.AccessRequestGetResponseDto
import com.prumo.data.model.AccessSignupResponseDto
import com.prumo.core.model.SupabaseConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class SupabaseAuthApi(
    private val config: SupabaseConfig,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private val client = OkHttpClient()
    private val mediaType = "application/json".toMediaType()

    fun login(email: String, password: String): AuthResponseDto {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .toString()
            .toRequestBody(mediaType)
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
        val body = JSONObject()
            .put("refresh_token", refreshToken)
            .toString()
            .toRequestBody(mediaType)
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

    fun registerCompany(
        email: String,
        password: String,
        fullName: String,
        username: String,
        companyName: String,
        jobTitle: String,
        origin: String
    ): AccessSignupResponseDto {
        val body = JSONObject()
            .put("action", "register_company")
            .put("email", email)
            .put("password", password)
            .put("fullName", fullName)
            .put("username", username)
            .put("companyName", companyName)
            .put("jobTitle", jobTitle)
            .put("requestedRole", "master")
            .put("origin", origin)
            .toString()
        return postAccessAction(body)
    }

    fun registerInternal(
        email: String,
        password: String,
        fullName: String,
        username: String,
        companyName: String,
        jobTitle: String,
        requestedRole: String,
        origin: String
    ): AccessSignupResponseDto {
        val body = JSONObject()
            .put("action", "register_internal")
            .put("email", email)
            .put("password", password)
            .put("fullName", fullName)
            .put("username", username)
            .put("companyName", companyName)
            .put("jobTitle", jobTitle)
            .put("requestedRole", requestedRole)
            .put("origin", origin)
            .toString()
        return postAccessAction(body)
    }

    fun getAccessRequest(token: String): AccessRequestGetResponseDto {
        val body = JSONObject()
            .put("action", "get_request")
            .put("token", token)
            .toString()
        val request = Request.Builder()
            .url("${config.baseUrl}/functions/v1/account-access-request")
            .header("apikey", config.anonKey)
            .header("Authorization", "Bearer ${config.anonKey}")
            .header("Content-Type", "application/json")
            .post(body.toRequestBody(mediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("Access request get failed (${response.code}): $payload")
            return json.decodeFromString(AccessRequestGetResponseDto.serializer(), payload)
        }
    }

    fun reviewAccessRequest(
        token: String,
        decision: String,
        reviewedUsername: String,
        reviewedJobTitle: String,
        reviewedRole: String,
        reviewNotes: String
    ): AccessSignupResponseDto {
        val body = JSONObject()
            .put("action", "review_request")
            .put("token", token)
            .put("decision", decision)
            .put("reviewedUsername", reviewedUsername)
            .put("reviewedJobTitle", reviewedJobTitle)
            .put("reviewedRole", reviewedRole)
            .put("reviewNotes", reviewNotes)
            .toString()
        return postAccessAction(body)
    }

    private fun postAccessAction(body: String): AccessSignupResponseDto {
        val request = Request.Builder()
            .url("${config.baseUrl}/functions/v1/account-access-request")
            .header("apikey", config.anonKey)
            .header("Authorization", "Bearer ${config.anonKey}")
            .header("Content-Type", "application/json")
            .post(body.toRequestBody(mediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("Access request failed (${response.code}): $payload")
            return json.decodeFromString(AccessSignupResponseDto.serializer(), payload)
        }
    }
}
