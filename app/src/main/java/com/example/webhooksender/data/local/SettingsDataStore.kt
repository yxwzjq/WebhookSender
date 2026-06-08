package com.example.webhooksender.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        val WEBHOOK_URL = stringPreferencesKey("webhook_url")
    }

    val webhookUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[WEBHOOK_URL] ?: ""
    }

    suspend fun saveWebhookUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[WEBHOOK_URL] = url
        }
    }
}
