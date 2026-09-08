package com.example.ui

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DozePreferences
import com.example.data.model.DozeConfig
import com.example.data.model.DozeEventLog
import com.example.data.model.DozeSession
import com.example.data.model.WhitelistAppInfo
import com.example.executor.DozeExecutor
import com.example.service.DozeForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class DozeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = DozePreferences(application)

    val config: StateFlow<DozeConfig> = prefs.configFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DozeConfig())

    val sessions: StateFlow<List<DozeSession>> = prefs.sessionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<DozeEventLog>> = prefs.logsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _installedApps = MutableStateFlow<List<WhitelistAppInfo>>(emptyList())
    val installedApps: StateFlow<List<WhitelistAppInfo>> = _installedApps.asStateFlow()

    private val _isShizukuAvailable = MutableStateFlow(false)
    val isShizukuAvailable: StateFlow<Boolean> = _isShizukuAvailable.asStateFlow()

    private val _isShizukuGranted = MutableStateFlow(false)
    val isShizukuGranted: StateFlow<Boolean> = _isShizukuGranted.asStateFlow()

    private val _hasDump = MutableStateFlow(false)
    val hasDump: StateFlow<Boolean> = _hasDump.asStateFlow()

    private val _hasWriteSecure = MutableStateFlow(false)
    val hasWriteSecure: StateFlow<Boolean> = _hasWriteSecure.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        refreshPermissions()
        loadInstalledApps()
        updateServiceRunningState()
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun updateServiceRunningState() {
        _isServiceRunning.value = DozeForegroundService.isRunning()
    }

    fun refreshPermissions() {
        val app = getApplication<Application>()
        _isShizukuAvailable.value = DozeExecutor.isShizukuAvailable()
        _isShizukuGranted.value = DozeExecutor.isShizukuPermissionGranted()
        _hasDump.value = DozeExecutor.hasDumpPermission(app)
        _hasWriteSecure.value = DozeExecutor.hasWriteSecureSettings(app)
    }

    fun requestShizukuPermission() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(1001)
                }
            }
        } catch (e: Exception) {
            _toastMessage.value = "Falha ao solicitar Shizuku: ${e.message}"
        }
    }

    fun toggleService(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setServiceEnabled(enabled)
            val context = getApplication<Application>()
            if (enabled) {
                DozeForegroundService.startService(context)
                prefs.addLog("SERVICE", "Serviço Modo Doze Definitivo ativado.")
            } else {
                DozeForegroundService.stopService(context)
                prefs.addLog("SERVICE", "Serviço Modo Doze desativado.")
            }
            delay(200)
            updateServiceRunningState()
        }
    }

    fun setWifiCutoff(enabled: Boolean) {
        viewModelScope.launch { prefs.setWifiCutoff(enabled) }
    }

    fun setBluetoothCutoff(enabled: Boolean) {
        viewModelScope.launch { prefs.setBluetoothCutoff(enabled) }
    }

    fun setMobileDataCutoff(enabled: Boolean) {
        viewModelScope.launch { prefs.setMobileDataCutoff(enabled) }
    }

    fun setAirplaneModeCutoff(enabled: Boolean) {
        viewModelScope.launch { prefs.setAirplaneModeCutoff(enabled) }
    }

    fun setMaintenanceEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setMaintenanceWindowEnabled(enabled) }
    }

    fun setMaintenanceInterval(minutes: Int) {
        viewModelScope.launch { prefs.setMaintenanceIntervalMinutes(minutes) }
    }

    fun setDisconnectUnstable(enabled: Boolean) {
        viewModelScope.launch { prefs.setDisconnectUnstableNetwork(enabled) }
    }

    fun toggleWhitelistPackage(packageName: String) {
        viewModelScope.launch {
            prefs.toggleWhitelistPackage(packageName)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            prefs.clearLogs()
            _toastMessage.value = "Histórico de logs limpo."
        }
    }

    fun testForceDeepDozeNow() {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.addLog("MANUAL_TEST", "Executando teste manual de Deep Doze...")
            val res = DozeExecutor.forceDeepDoze()
            if (res.isSuccess) {
                _toastMessage.value = "Comando Deep Doze enviado com sucesso!"
                prefs.addLog("MANUAL_TEST", "Deep Doze executado com sucesso.")
            } else {
                _toastMessage.value = "Erro ou permissão ausente: ${res.error}"
                prefs.addLog("MANUAL_TEST", "Falha: ${res.error}", isSuccess = false)
            }
        }
    }

    fun executeSafeReset() {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.addLog("SAFE_RESET", "Iniciando Safe Reset Total...")

            // 1. Revert hardware
            DozeExecutor.setWifi(true)
            DozeExecutor.setBluetooth(true)
            DozeExecutor.setMobileData(true)
            DozeExecutor.setAirplaneMode(false)

            // 2. Re-enable master sync
            DozeExecutor.setMasterSync(true)

            // 3. Clear whitelist in deviceidle
            val currentConfig = config.value
            for (pkg in currentConfig.whitelistedPackages) {
                DozeExecutor.removeAppFromWhitelist(pkg)
            }

            // 4. Reset Doze manager
            val resetRes = DozeExecutor.resetDoze()
            prefs.addLog(
                tag = "SAFE_RESET",
                message = if (resetRes.isSuccess) "Doze resetado com sucesso (dumpsys deviceidle reset)." else "Comando reset enviado."
            )

            // 5. Stop Service and clear preferences
            val context = getApplication<Application>()
            DozeForegroundService.stopService(context)
            prefs.resetAllToDefaults()

            _toastMessage.value = "Safe Reset concluído! Sistema restaurado aos padrões."
            delay(200)
            updateServiceRunningState()
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val list = packages.mapNotNull { appInfo ->
                try {
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    WhitelistAppInfo(
                        packageName = appInfo.packageName,
                        appName = appName,
                        isWhitelisted = false,
                        isSystemApp = isSystem
                    )
                } catch (_: Exception) {
                    null
                }
            }.sortedWith(
                compareBy<WhitelistAppInfo> { it.isSystemApp }
                    .thenBy { it.appName.lowercase() }
            )
            _installedApps.value = list
        }
    }
}
