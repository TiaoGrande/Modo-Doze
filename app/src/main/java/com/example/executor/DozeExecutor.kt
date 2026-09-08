package com.example.executor

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

data class CommandResult(
    val command: String,
    val exitCode: Int,
    val output: String,
    val error: String,
    val executedViaShizuku: Boolean
) {
    val isSuccess: Boolean get() = exitCode == 0
}

object DozeExecutor {

    private const val TAG = "DozeExecutor"

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
    }

    fun isShizukuPermissionGranted(): Boolean {
        return try {
            if (isShizukuAvailable()) {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun hasDumpPermission(context: Context): Boolean {
        return context.checkCallingOrSelfPermission(android.Manifest.permission.DUMP) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun hasWriteSecureSettings(context: Context): Boolean {
        return context.checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
                PackageManager.PERMISSION_GRANTED
    }

    suspend fun execute(command: String): CommandResult = withContext(Dispatchers.IO) {
        val useShizuku = isShizukuAvailable() && isShizukuPermissionGranted()
        try {
            val process: Process = if (useShizuku) {
                try {
                    val method = Shizuku::class.java.getDeclaredMethod(
                        "newProcess",
                        Array<String>::class.java,
                        Array<String>::class.java,
                        String::class.java
                    )
                    method.isAccessible = true
                    @Suppress("UNCHECKED_CAST")
                    method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
                } catch (t: Throwable) {
                    Log.w(TAG, "Shizuku process invocation failed, falling back to Runtime.exec: ${t.message}")
                    Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                }
            } else {
                Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            }

            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val error = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val exitCode = process.waitFor()

            Log.d(TAG, "CMD: [$command] Exit: $exitCode via ${if (useShizuku) "Shizuku" else "Runtime"}")
            CommandResult(
                command = command,
                exitCode = exitCode,
                output = output.trim(),
                error = error.trim(),
                executedViaShizuku = useShizuku
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed executing [$command]: ${e.message}")
            CommandResult(
                command = command,
                exitCode = -1,
                output = "",
                error = e.message ?: "Unknown error",
                executedViaShizuku = useShizuku
            )
        }
    }

    // --- Core Doze Commands ---
    suspend fun forceDeepDoze(): CommandResult {
        return execute("dumpsys deviceidle force-idle deep")
    }

    suspend fun stepIdle(): CommandResult {
        return execute("dumpsys deviceidle step")
    }

    suspend fun unforceDoze(): CommandResult {
        return execute("dumpsys deviceidle unforce")
    }

    suspend fun resetDoze(): CommandResult {
        return execute("dumpsys deviceidle reset")
    }

    // --- Whitelist Management ---
    suspend fun addAppToWhitelist(packageName: String): CommandResult {
        return execute("dumpsys deviceidle whitelist +$packageName")
    }

    suspend fun removeAppFromWhitelist(packageName: String): CommandResult {
        return execute("dumpsys deviceidle whitelist -$packageName")
    }

    // --- Master Sync ---
    suspend fun setMasterSync(enabled: Boolean): CommandResult {
        return execute("content call --uri content://sync --method setMasterSyncAutomatically --extra boolean:$enabled")
    }

    // --- Hardware Radios ---
    suspend fun setWifi(enabled: Boolean): CommandResult {
        val state = if (enabled) "enabled" else "disabled"
        return execute("cmd wifi set-wifi-enabled $state")
    }

    suspend fun setBluetooth(enabled: Boolean): CommandResult {
        val state = if (enabled) "enable" else "disable"
        return execute("cmd bluetooth_manager $state")
    }

    suspend fun setMobileData(enabled: Boolean): CommandResult {
        val state = if (enabled) "enable" else "disable"
        return execute("cmd phone data $state")
    }

    suspend fun setAirplaneMode(enabled: Boolean): CommandResult {
        val state = if (enabled) 1 else 0
        val cmd = "settings put global airplane_mode_on $state && am broadcast -a android.intent.action.AIRPLANE_MODE --ez state $enabled"
        return execute(cmd)
    }

    suspend fun getDeviceIdleStatus(): String = withContext(Dispatchers.IO) {
        val res = execute("dumpsys deviceidle get")
        if (res.isSuccess) res.output else "Estado indisponível"
    }
}
