package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.DozeConfig
import com.example.data.model.DozeEventLog
import com.example.data.model.DozeSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "doze_preferences")

class DozePreferences(private val context: Context) {

    companion object {
        val KEY_SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val KEY_CUTOFF_WIFI = booleanPreferencesKey("cutoff_wifi")
        val KEY_CUTOFF_BT = booleanPreferencesKey("cutoff_bt")
        val KEY_CUTOFF_DATA = booleanPreferencesKey("cutoff_data")
        val KEY_CUTOFF_AIRPLANE = booleanPreferencesKey("cutoff_airplane")
        val KEY_MAINTENANCE_ENABLED = booleanPreferencesKey("maintenance_enabled")
        val KEY_MAINTENANCE_INTERVAL = intPreferencesKey("maintenance_interval")
        val KEY_UNSTABLE_NET = booleanPreferencesKey("unstable_net")
        val KEY_WHITELIST = stringSetPreferencesKey("whitelist_packages")

        val KEY_LAST_OFF_TIME = longPreferencesKey("last_off_time")
        val KEY_LAST_OFF_BATTERY = intPreferencesKey("last_off_battery")

        val KEY_SESSIONS_DATA = stringPreferencesKey("sessions_data")
        val KEY_LOGS_DATA = stringPreferencesKey("logs_data")
    }

    val configFlow: Flow<DozeConfig> = context.dataStore.data.map { prefs ->
        DozeConfig(
            isServiceEnabled = prefs[KEY_SERVICE_ENABLED] ?: false,
            cutoffWifi = prefs[KEY_CUTOFF_WIFI] ?: true,
            cutoffBluetooth = prefs[KEY_CUTOFF_BT] ?: true,
            cutoffMobileData = prefs[KEY_CUTOFF_DATA] ?: false,
            cutoffAirplaneMode = prefs[KEY_CUTOFF_AIRPLANE] ?: false,
            maintenanceWindowEnabled = prefs[KEY_MAINTENANCE_ENABLED] ?: true,
            maintenanceIntervalMinutes = prefs[KEY_MAINTENANCE_INTERVAL] ?: 60,
            disconnectUnstableNetwork = prefs[KEY_UNSTABLE_NET] ?: true,
            whitelistedPackages = prefs[KEY_WHITELIST] ?: setOf("com.whatsapp", "org.telegram.messenger")
        )
    }

    val sessionsFlow: Flow<List<DozeSession>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_SESSIONS_DATA] ?: ""
        parseSessions(raw)
    }

    val logsFlow: Flow<List<DozeEventLog>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_LOGS_DATA] ?: ""
        parseLogs(raw)
    }

    val lastScreenOffState: Flow<Pair<Long, Int>> = context.dataStore.data.map { prefs ->
        Pair(prefs[KEY_LAST_OFF_TIME] ?: 0L, prefs[KEY_LAST_OFF_BATTERY] ?: -1)
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SERVICE_ENABLED] = enabled }
    }

    suspend fun setWifiCutoff(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CUTOFF_WIFI] = enabled }
    }

    suspend fun setBluetoothCutoff(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CUTOFF_BT] = enabled }
    }

    suspend fun setMobileDataCutoff(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CUTOFF_DATA] = enabled }
    }

    suspend fun setAirplaneModeCutoff(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CUTOFF_AIRPLANE] = enabled }
    }

    suspend fun setMaintenanceWindowEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_MAINTENANCE_ENABLED] = enabled }
    }

    suspend fun setMaintenanceIntervalMinutes(minutes: Int) {
        context.dataStore.edit { it[KEY_MAINTENANCE_INTERVAL] = minutes }
    }

    suspend fun setDisconnectUnstableNetwork(enabled: Boolean) {
        context.dataStore.edit { it[KEY_UNSTABLE_NET] = enabled }
    }

    suspend fun setWhitelist(packages: Set<String>) {
        context.dataStore.edit { it[KEY_WHITELIST] = packages }
    }

    suspend fun toggleWhitelistPackage(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[KEY_WHITELIST] ?: emptySet()).toMutableSet()
            if (current.contains(packageName)) {
                current.remove(packageName)
            } else {
                current.add(packageName)
            }
            prefs[KEY_WHITELIST] = current
        }
    }

    suspend fun saveScreenOffState(timeMillis: Long, batteryLevel: Int) {
        context.dataStore.edit {
            it[KEY_LAST_OFF_TIME] = timeMillis
            it[KEY_LAST_OFF_BATTERY] = batteryLevel
        }
    }

    suspend fun addSession(session: DozeSession) {
        context.dataStore.edit { prefs ->
            val current = parseSessions(prefs[KEY_SESSIONS_DATA] ?: "").toMutableList()
            current.add(0, session)
            // keep up to 50 recent sessions
            val trimmed = current.take(50)
            prefs[KEY_SESSIONS_DATA] = serializeSessions(trimmed)
        }
    }

    suspend fun addLog(tag: String, message: String, isSuccess: Boolean = true) {
        context.dataStore.edit { prefs ->
            val current = parseLogs(prefs[KEY_LOGS_DATA] ?: "").toMutableList()
            current.add(0, DozeEventLog(tag = tag, message = message, isSuccess = isSuccess))
            val trimmed = current.take(60)
            prefs[KEY_LOGS_DATA] = serializeLogs(trimmed)
        }
    }

    suspend fun clearLogs() {
        context.dataStore.edit { it.remove(KEY_LOGS_DATA) }
    }

    suspend fun resetAllToDefaults() {
        context.dataStore.edit { prefs ->
            prefs.clear()
            prefs[KEY_WHITELIST] = emptySet()
        }
    }

    // Ultra-lean string serialization without heavy JSON dependencies
    private fun serializeSessions(sessions: List<DozeSession>): String {
        return sessions.joinToString(";") { s ->
            "${s.id},${s.startTimeMillis},${s.endTimeMillis},${s.durationMinutes},${s.batteryStart},${s.batteryEnd},${s.batteryDelta}"
        }
    }

    private fun parseSessions(raw: String): List<DozeSession> {
        if (raw.isBlank()) return emptyList()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split(",")
            if (parts.size == 7) {
                try {
                    DozeSession(
                        id = parts[0],
                        startTimeMillis = parts[1].toLong(),
                        endTimeMillis = parts[2].toLong(),
                        durationMinutes = parts[3].toLong(),
                        batteryStart = parts[4].toInt(),
                        batteryEnd = parts[5].toInt(),
                        batteryDelta = parts[6].toInt()
                    )
                } catch (_: Exception) {
                    null
                }
            } else null
        }
    }

    private fun serializeLogs(logs: List<DozeEventLog>): String {
        return logs.joinToString(";") { l ->
            "${l.id}###${l.timestamp}###${l.tag}###${l.message.replace(";", ",")}###${l.isSuccess}"
        }
    }

    private fun parseLogs(raw: String): List<DozeEventLog> {
        if (raw.isBlank()) return emptyList()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split("###")
            if (parts.size == 5) {
                try {
                    DozeEventLog(
                        id = parts[0],
                        timestamp = parts[1].toLong(),
                        tag = parts[2],
                        message = parts[3],
                        isSuccess = parts[4].toBoolean()
                    )
                } catch (_: Exception) {
                    null
                }
            } else null
        }
    }
}
