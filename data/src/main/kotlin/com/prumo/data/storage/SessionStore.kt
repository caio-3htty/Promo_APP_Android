package com.prumo.data.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.prumo.core.model.SessionToken
import com.prumo.core.model.SessionUser
import org.json.JSONObject

interface SessionStore {
    fun read(): SessionToken?
    fun save(token: SessionToken)
    fun clear()
}

class EncryptedSessionStore(context: Context) : SessionStore {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "promo_secure_session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun read(): SessionToken? {
        val raw = prefs.getString(KEY, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            SessionToken(
                accessToken = json.getString("accessToken"),
                refreshToken = json.getString("refreshToken"),
                expiresAtEpochSeconds = json.getLong("expiresAtEpochSeconds"),
                user = SessionUser(
                    userId = json.getString("userId"),
                    email = json.getString("email"),
                    fullName = nullableString(json, "fullName"),
                    role = nullableString(json, "role"),
                    tenantId = json.getString("tenantId"),
                    obraScope = json.optJSONArray("obraScope")?.let { array ->
                        buildList {
                            for (i in 0 until array.length()) add(array.getString(i))
                        }
                    } ?: emptyList()
                )
            )
        }.getOrNull()
    }

    override fun save(token: SessionToken) {
        val json = JSONObject()
            .put("accessToken", token.accessToken)
            .put("refreshToken", token.refreshToken)
            .put("expiresAtEpochSeconds", token.expiresAtEpochSeconds)
            .put("userId", token.user.userId)
            .put("email", token.user.email)
            .put("fullName", token.user.fullName)
            .put("role", token.user.role)
            .put("tenantId", token.user.tenantId)
            .put("obraScope", org.json.JSONArray(token.user.obraScope))

        prefs.edit().putString(KEY, json.toString()).apply()
    }

    override fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private companion object {
        const val KEY = "session"

        private fun nullableString(json: JSONObject, key: String): String? {
            if (json.isNull(key)) return null
            return json.optString(key).ifBlank { null }
        }
    }
}
