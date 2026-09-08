package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.DozePreferences
import com.example.service.DozeForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val prefs = DozePreferences(context.applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                val config = prefs.configFlow.first()
                if (config.isServiceEnabled) {
                    DozeForegroundService.startService(context.applicationContext)
                    prefs.addLog(
                        tag = "BOOT",
                        message = "Sistema reiniciado. Serviço Doze reativado automaticamente."
                    )
                }
            }
        }
    }
}
