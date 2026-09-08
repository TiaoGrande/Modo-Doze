package com.example.data.model

data class DozeSession(
    val id: String = System.currentTimeMillis().toString(),
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationMinutes: Long,
    val batteryStart: Int,
    val batteryEnd: Int,
    val batteryDelta: Int
)

data class DozeEventLog(
    val id: String = System.currentTimeMillis().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val isSuccess: Boolean = true
)

data class WhitelistAppInfo(
    val packageName: String,
    val appName: String,
    val isWhitelisted: Boolean,
    val isSystemApp: Boolean
)

data class DozeConfig(
    val isServiceEnabled: Boolean = false,
    val cutoffWifi: Boolean = true,
    val cutoffBluetooth: Boolean = true,
    val cutoffMobileData: Boolean = false,
    val cutoffAirplaneMode: Boolean = false,
    val maintenanceWindowEnabled: Boolean = true,
    val maintenanceIntervalMinutes: Int = 60,
    val disconnectUnstableNetwork: Boolean = true,
    val whitelistedPackages: Set<String> = setOf("com.whatsapp", "org.telegram.messenger")
)
