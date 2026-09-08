package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.DozePreferences
import com.example.executor.DozeExecutor
import com.example.receiver.ScreenStateReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DozeForegroundService : Service() {

    companion object {
        private const val TAG = "DozeForegroundService"
        const val ACTION_START = "com.example.action.START_DOZE"
        const val ACTION_STOP = "com.example.action.STOP_DOZE"

        private var instance: DozeForegroundService? = null
        private var maintenanceJob: Job? = null

        fun startService(context: Context) {
            val intent = Intent(context, DozeForegroundService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, DozeForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun isRunning(): Boolean = instance != null

        fun startMaintenanceCycle(context: Context) {
            instance?.launchMaintenanceLoop(context)
        }

        fun stopMaintenanceCycle() {
            maintenanceJob?.cancel()
            maintenanceJob = null
            Log.d(TAG, "Maintenance cycle stopped (screen on or service stopped).")
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var screenStateReceiver: ScreenStateReceiver? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.createNotificationChannels(this)
        registerScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.d(TAG, "Stopping DozeForegroundService")
                stopMaintenanceCycle()
                unregisterScreenReceiver()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                Log.d(TAG, "Starting DozeForegroundService foreground")
                val notification = NotificationHelper.buildForegroundNotification(
                    this,
                    "Hibernação inteligente ativa • Economia radical de bateria"
                )
                startForeground(NotificationHelper.NOTIFICATION_SERVICE_ID, notification)
            }
        }
        return START_STICKY
    }

    private fun registerScreenReceiver() {
        if (screenStateReceiver == null) {
            screenStateReceiver = ScreenStateReceiver()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            registerReceiver(screenStateReceiver, filter)
            Log.d(TAG, "ScreenStateReceiver registered dynamically.")
        }
    }

    private fun unregisterScreenReceiver() {
        screenStateReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering ScreenStateReceiver: ${e.message}")
            }
            screenStateReceiver = null
        }
    }

    fun launchMaintenanceLoop(context: Context) {
        maintenanceJob?.cancel()
        val prefs = DozePreferences(context.applicationContext)

        maintenanceJob = serviceScope.launch {
            val config = prefs.configFlow.first()
            if (!config.maintenanceWindowEnabled) return@launch

            val intervalMillis = config.maintenanceIntervalMinutes * 60 * 1000L
            Log.d(TAG, "Maintenance loop started: interval = ${config.maintenanceIntervalMinutes} minutes")

            while (isActive) {
                delay(intervalMillis)
                if (!isActive) break

                Log.d(TAG, "Executing 1-minute cyclical maintenance window...")
                prefs.addLog(
                    tag = "MAINTENANCE",
                    message = "Janela de Manutenção: Liberando sincronização por 1 minuto."
                )

                // 1. Awaken briefly for notifications
                DozeExecutor.stepIdle()
                DozeExecutor.setMasterSync(true)

                // 2. Wait 60 seconds for apps to receive push notifications
                delay(60 * 1000L)
                if (!isActive) break

                // 3. Freeze sync again and force Deep Doze
                DozeExecutor.setMasterSync(false)
                DozeExecutor.forceDeepDoze()
                prefs.addLog(
                    tag = "DEEP_DOZE",
                    message = "Janela de Manutenção concluída. Deep Doze reativado."
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        stopMaintenanceCycle()
        unregisterScreenReceiver()
        serviceScope.cancel()
        Log.d(TAG, "DozeForegroundService destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
