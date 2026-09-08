package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.util.Log
import com.example.data.DozePreferences
import com.example.data.model.DozeSession
import com.example.executor.DozeExecutor
import com.example.service.DozeForegroundService
import com.example.service.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.max

class ScreenStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ScreenStateReceiver"

        fun getBatteryPercentage(context: Context): Int {
            return try {
                val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val capacity = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                if (capacity in 0..100) return capacity

                val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                val batteryStatus = context.registerReceiver(null, iFilter)
                val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (level >= 0 && scale > 0) (level * 100 / scale) else 100
            } catch (e: Exception) {
                100
            }
        }

        private fun isWifiSignalWeak(context: Context): Boolean {
            return try {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val info = wm?.connectionInfo ?: return false
                val rssi = info.rssi
                val level = WifiManager.calculateSignalLevel(rssi, 5)
                level <= 1
            } catch (_: Exception) {
                false
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val prefs = DozePreferences(context.applicationContext)
        val scope = CoroutineScope(Dispatchers.IO)

        when (action) {
            Intent.ACTION_SCREEN_OFF -> {
                Log.d(TAG, "ACTION_SCREEN_OFF triggered")
                scope.launch {
                    val config = prefs.configFlow.first()
                    if (!config.isServiceEnabled) {
                        Log.d(TAG, "Doze service is not enabled in preferences. Skipping.")
                        return@launch
                    }

                    val now = System.currentTimeMillis()
                    val batteryLevel = getBatteryPercentage(context)
                    prefs.saveScreenOffState(now, batteryLevel)

                    prefs.addLog(
                        tag = "SCREEN_OFF",
                        message = "Tela desligada. Bateria: $batteryLevel%. Iniciando protocolo Doze."
                    )

                    // 1. Check weak signals if unstable network disconnect is on
                    if (config.disconnectUnstableNetwork && isWifiSignalWeak(context)) {
                        DozeExecutor.setWifi(false)
                        prefs.addLog(
                            tag = "NET_UNSTABLE",
                            message = "Sinal Wi-Fi fraco (<1 barra). Desativado para evitar aquecimento."
                        )
                    }

                    // 2. Hardware radio cutoffs
                    if (config.cutoffWifi) DozeExecutor.setWifi(false)
                    if (config.cutoffBluetooth) DozeExecutor.setBluetooth(false)
                    if (config.cutoffMobileData) DozeExecutor.setMobileData(false)
                    if (config.cutoffAirplaneMode) DozeExecutor.setAirplaneMode(true)

                    // 3. Dynamic Whitelist injection
                    for (pkg in config.whitelistedPackages) {
                        DozeExecutor.addAppToWhitelist(pkg)
                    }

                    // 4. Freeze Native Master Sync
                    DozeExecutor.setMasterSync(false)

                    // 5. Force Deep Doze
                    val dozeResult = DozeExecutor.forceDeepDoze()
                    prefs.addLog(
                        tag = "DEEP_DOZE",
                        message = if (dozeResult.isSuccess) "Deep Doze forçado com sucesso." else "Comando Deep Doze despachado.",
                        isSuccess = dozeResult.isSuccess
                    )

                    // 6. Signal service to schedule periodic maintenance cycle
                    DozeForegroundService.startMaintenanceCycle(context)
                }
            }

            Intent.ACTION_SCREEN_ON -> {
                Log.d(TAG, "ACTION_SCREEN_ON triggered")
                scope.launch {
                    // Cancel maintenance cycle while screen is ON
                    DozeForegroundService.stopMaintenanceCycle()

                    val config = prefs.configFlow.first()
                    if (!config.isServiceEnabled) return@launch

                    val (offTime, startBattery) = prefs.lastScreenOffState.first()
                    val now = System.currentTimeMillis()
                    val endBattery = getBatteryPercentage(context)

                    if (offTime > 0 && now > offTime) {
                        val durationMinutes = max(1L, (now - offTime) / (60 * 1000))
                        val batteryDelta = if (startBattery > 0 && endBattery <= startBattery) {
                            startBattery - endBattery
                        } else 0

                        val session = DozeSession(
                            startTimeMillis = offTime,
                            endTimeMillis = now,
                            durationMinutes = durationMinutes,
                            batteryStart = startBattery,
                            batteryEnd = endBattery,
                            batteryDelta = batteryDelta
                        )
                        prefs.addSession(session)
                        prefs.addLog(
                            tag = "SCREEN_ON",
                            message = "Tela ligada. $durationMinutes min em Doze | Consumo: $batteryDelta%"
                        )

                        NotificationHelper.showSessionSummaryNotification(
                            context = context,
                            durationMinutes = durationMinutes,
                            batteryDeltaPct = batteryDelta,
                            batteryStart = startBattery,
                            batteryEnd = endBattery
                        )
                    }

                    // Staggered radio reconnections to prevent CPU spikes
                    DozeExecutor.setMasterSync(true)

                    if (config.cutoffAirplaneMode) {
                        delay(150)
                        DozeExecutor.setAirplaneMode(false)
                    }

                    if (config.cutoffWifi) {
                        delay(250)
                        DozeExecutor.setWifi(true)
                    }

                    if (config.cutoffMobileData) {
                        delay(250)
                        DozeExecutor.setMobileData(true)
                    }

                    if (config.cutoffBluetooth) {
                        delay(250)
                        DozeExecutor.setBluetooth(true)
                    }

                    prefs.addLog(
                        tag = "RECONNECT",
                        message = "Reconexão suave e escalonada das antenas concluída."
                    )
                }
            }
        }
    }
}
