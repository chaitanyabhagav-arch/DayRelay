package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.BridgeConfig
import com.example.data.model.BridgeProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.bridgeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dayrelay_bridge_prefs"
)

class BridgePreferencesDataStore(private val context: Context) {

    private object Keys {
        val BRIDGE_ENABLED = booleanPreferencesKey("bridge_enabled")
        val PROVIDER_TYPE = stringPreferencesKey("provider_type")
        val NTFY_BASE_URL = stringPreferencesKey("ntfy_base_url")
        val NTFY_TOPIC = stringPreferencesKey("ntfy_topic")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val WEBHOOK_URL = stringPreferencesKey("webhook_url")
        val DEFAULT_SNOOZE_MINUTES = intPreferencesKey("default_snooze_minutes")
        val INCLUDE_PRIORITY_BADGES = booleanPreferencesKey("include_priority_badges")
    }

    val configFlow: Flow<BridgeConfig> = context.bridgeDataStore.data.map { prefs ->
        val provider = prefs[Keys.PROVIDER_TYPE]?.let {
            runCatching { BridgeProviderType.valueOf(it) }.getOrNull()
        } ?: BridgeProviderType.NTFY

        BridgeConfig(
            bridgeEnabled = prefs[Keys.BRIDGE_ENABLED] ?: true,
            providerType = provider,
            ntfyBaseUrl = prefs[Keys.NTFY_BASE_URL] ?: "https://ntfy.sh",
            ntfyTopic = prefs[Keys.NTFY_TOPIC] ?: "",
            authToken = prefs[Keys.AUTH_TOKEN] ?: "",
            webhookUrl = prefs[Keys.WEBHOOK_URL] ?: "",
            defaultSnoozeMinutes = prefs[Keys.DEFAULT_SNOOZE_MINUTES] ?: 10,
            includePriorityBadges = prefs[Keys.INCLUDE_PRIORITY_BADGES] ?: true
        )
    }

    suspend fun getConfigOnce(): BridgeConfig = configFlow.first()

    suspend fun saveConfig(config: BridgeConfig) {
        context.bridgeDataStore.edit { prefs ->
            prefs[Keys.BRIDGE_ENABLED] = config.bridgeEnabled
            prefs[Keys.PROVIDER_TYPE] = config.providerType.name
            prefs[Keys.NTFY_BASE_URL] = config.ntfyBaseUrl.trim().ifBlank { "https://ntfy.sh" }
            prefs[Keys.NTFY_TOPIC] = config.ntfyTopic.trim()
            prefs[Keys.AUTH_TOKEN] = config.authToken.trim()
            prefs[Keys.WEBHOOK_URL] = config.webhookUrl.trim()
            prefs[Keys.DEFAULT_SNOOZE_MINUTES] = config.defaultSnoozeMinutes
            prefs[Keys.INCLUDE_PRIORITY_BADGES] = config.includePriorityBadges
        }
    }

    suspend fun ensureDefaultTopicIfEmpty(): String {
        val current = getConfigOnce()
        if (current.ntfyTopic.isNotBlank()) return current.ntfyTopic
        val generated = "dayrelay-${UUID.randomUUID().toString().take(8)}"
        saveConfig(current.copy(ntfyTopic = generated))
        return generated
    }

    fun generateFreshTopicName(): String {
        return "dayrelay-${UUID.randomUUID().toString().take(8)}"
    }
}
