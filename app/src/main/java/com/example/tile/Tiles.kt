package com.example.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.data.DozePreferences
import com.example.service.DozeForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DozeTileService : TileService() {
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val prefs = DozePreferences(applicationContext)
        scope.launch(Dispatchers.IO) {
            val config = prefs.configFlow.first()
            val newState = !config.isServiceEnabled
            prefs.setServiceEnabled(newState)
            if (newState) {
                DozeForegroundService.startService(applicationContext)
            } else {
                DozeForegroundService.stopService(applicationContext)
            }
            updateTileState()
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isRunning = DozeForegroundService.isRunning()
        tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isRunning) "Doze: Ativo" else "Doze: Inativo"
        tile.updateTile()
    }
}

class WifiTileService : TileService() {
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val prefs = DozePreferences(applicationContext)
        scope.launch(Dispatchers.IO) {
            val config = prefs.configFlow.first()
            val newState = !config.cutoffWifi
            prefs.setWifiCutoff(newState)
            updateTileState()
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val prefs = DozePreferences(applicationContext)
        scope.launch(Dispatchers.IO) {
            val config = prefs.configFlow.first()
            scope.launch(Dispatchers.Main) {
                tile.state = if (config.cutoffWifi) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                tile.label = if (config.cutoffWifi) "Wi-Fi Cut: ON" else "Wi-Fi Cut: OFF"
                tile.updateTile()
            }
        }
    }
}

class BluetoothTileService : TileService() {
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val prefs = DozePreferences(applicationContext)
        scope.launch(Dispatchers.IO) {
            val config = prefs.configFlow.first()
            val newState = !config.cutoffBluetooth
            prefs.setBluetoothCutoff(newState)
            updateTileState()
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val prefs = DozePreferences(applicationContext)
        scope.launch(Dispatchers.IO) {
            val config = prefs.configFlow.first()
            scope.launch(Dispatchers.Main) {
                tile.state = if (config.cutoffBluetooth) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                tile.label = if (config.cutoffBluetooth) "BT Cut: ON" else "BT Cut: OFF"
                tile.updateTile()
            }
        }
    }
}

class AirplaneTileService : TileService() {
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val prefs = DozePreferences(applicationContext)
        scope.launch(Dispatchers.IO) {
            val config = prefs.configFlow.first()
            val newState = !config.cutoffAirplaneMode
            prefs.setAirplaneModeCutoff(newState)
            updateTileState()
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val prefs = DozePreferences(applicationContext)
        scope.launch(Dispatchers.IO) {
            val config = prefs.configFlow.first()
            scope.launch(Dispatchers.Main) {
                tile.state = if (config.cutoffAirplaneMode) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                tile.label = if (config.cutoffAirplaneMode) "Avião Cut: ON" else "Avião Cut: OFF"
                tile.updateTile()
            }
        }
    }
}
