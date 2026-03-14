package com.prumo.androidclient

import android.app.Application
import com.prumo.core.model.SupabaseConfig
import com.prumo.data.repository.AppContainer
import com.prumo.data.storage.EncryptedSessionStore

class PromoAndroidApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        val baseUrl = BuildConfig.SUPABASE_URL.trim()
        val anon = BuildConfig.SUPABASE_ANON_KEY.trim()

        check(baseUrl.isNotBlank()) { "SUPABASE_URL nao configurada" }
        check(anon.isNotBlank()) { "SUPABASE_ANON_KEY nao configurada" }

        container = AppContainer(
            config = SupabaseConfig(baseUrl = baseUrl, anonKey = anon),
            sessionStore = EncryptedSessionStore(this)
        )
    }
}