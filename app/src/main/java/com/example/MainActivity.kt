package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.receiver.ScreenStateReceiver
import com.example.ui.DozeViewModel
import com.example.ui.components.AdbInstructionsDialog
import com.example.ui.components.AppWhitelistSection
import com.example.ui.components.EventTimelineView
import com.example.ui.components.HibernationChart
import com.example.ui.components.SafeResetDialog
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.CoralWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

class MainActivity : ComponentActivity() {

    private val viewModel: DozeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current

                // Request notification permission for session alerts on Android 13+
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Result handled */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                DozeAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
        viewModel.updateServiceRunningState()
    }
}

@Composable
fun DozeAppScreen(viewModel: DozeViewModel) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val isShizukuAvailable by viewModel.isShizukuAvailable.collectAsState()
    val isShizukuGranted by viewModel.isShizukuGranted.collectAsState()
    val hasDump by viewModel.hasDump.collectAsState()
    val hasWriteSecure by viewModel.hasWriteSecure.collectAsState()
    val isRunning by viewModel.isServiceRunning.collectAsState()
    val toastMsg by viewModel.toastMessage.collectAsState()

    var showAdbDialog by remember { mutableStateOf(false) }
    var showSafeResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(toastMsg) {
        toastMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearToast()
        }
    }

    val currentBattery = remember { ScreenStateReceiver.getBatteryPercentage(context) }
    val hasElevatedPrivileges = isShizukuGranted || (hasDump && hasWriteSecure)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack),
        containerColor = AmoledBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Bar
                item {
                    HeaderBar(
                        hasElevatedPrivileges = hasElevatedPrivileges,
                        onOpenPermissionsGuide = { showAdbDialog = true }
                    )
                }

                // Privilege Warning if not yet granted
                if (!hasElevatedPrivileges) {
                    item {
                        PrivilegeWarningCard(
                            isShizukuAvailable = isShizukuAvailable,
                            onOpenGuide = { showAdbDialog = true }
                        )
                    }
                }

                // Master Doze Switch Card
                item {
                    MasterDozeCard(
                        isEnabled = config.isServiceEnabled,
                        isRunning = isRunning,
                        batteryLevel = currentBattery,
                        onToggle = { viewModel.toggleService(it) }
                    )
                }

                // Módulo D: Hibernation History Chart
                item {
                    HibernationChart(sessions = sessions)
                }

                // Módulo B: Hardware Connectivity Toggles
                item {
                    HardwareControlsCard(
                        cutoffWifi = config.cutoffWifi,
                        cutoffBluetooth = config.cutoffBluetooth,
                        cutoffMobileData = config.cutoffMobileData,
                        cutoffAirplaneMode = config.cutoffAirplaneMode,
                        disconnectUnstable = config.disconnectUnstableNetwork,
                        onToggleWifi = { viewModel.setWifiCutoff(it) },
                        onToggleBluetooth = { viewModel.setBluetoothCutoff(it) },
                        onToggleMobileData = { viewModel.setMobileDataCutoff(it) },
                        onToggleAirplane = { viewModel.setAirplaneModeCutoff(it) },
                        onToggleUnstable = { viewModel.setDisconnectUnstable(it) }
                    )
                }

                // Módulo A: Cyclical Maintenance Window
                item {
                    MaintenanceWindowCard(
                        enabled = config.maintenanceWindowEnabled,
                        intervalMinutes = config.maintenanceIntervalMinutes,
                        onToggle = { viewModel.setMaintenanceEnabled(it) },
                        onSelectInterval = { viewModel.setMaintenanceInterval(it) }
                    )
                }

                // Módulo C: Dynamic Whitelist Section
                item {
                    AppWhitelistSection(
                        apps = installedApps,
                        whitelistedPackages = config.whitelistedPackages,
                        onTogglePackage = { viewModel.toggleWhitelistPackage(it) }
                    )
                }

                // Módulo D: Event Timeline
                item {
                    EventTimelineView(
                        logs = logs,
                        onClearLogs = { viewModel.clearLogs() }
                    )
                }

                // Quick Action & Emergency Reset Buttons
                item {
                    ActionButtonsSection(
                        onTestDoze = { viewModel.testForceDeepDozeNow() },
                        onOpenReset = { showSafeResetDialog = true }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showAdbDialog) {
        AdbInstructionsDialog(
            isShizukuAvailable = isShizukuAvailable,
            isShizukuGranted = isShizukuGranted,
            hasDump = hasDump,
            hasWriteSecure = hasWriteSecure,
            onDismiss = { showAdbDialog = false },
            onRequestShizukuPermission = {
                viewModel.requestShizukuPermission()
            }
        )
    }

    if (showSafeResetDialog) {
        SafeResetDialog(
            onDismiss = { showSafeResetDialog = false },
            onConfirmReset = {
                showSafeResetDialog = false
                viewModel.executeSafeReset()
            }
        )
    }
}

@Composable
private fun HeaderBar(
    hasElevatedPrivileges: Boolean,
    onOpenPermissionsGuide: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(EmeraldGlow, RoundedCornerShape(12.dp))
                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Modo Doze Definitivo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "Arquitetura Híbrida Shizuku & ADB",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        // Privilege Status Chip
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (hasElevatedPrivileges) DarkSurfaceElevated else AmberAlert.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (hasElevatedPrivileges) EmeraldPrimary.copy(alpha = 0.5f) else AmberAlert
            ),
            modifier = Modifier
                .clickable { onOpenPermissionsGuide() }
                .testTag("privilege_status_chip")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (hasElevatedPrivileges) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (hasElevatedPrivileges) EmeraldPrimary else AmberAlert,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (hasElevatedPrivileges) "Privilégios OK" else "Privilégios?",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasElevatedPrivileges) EmeraldPrimary else AmberAlert
                )
            }
        }
    }
}

@Composable
private fun PrivilegeWarningCard(
    isShizukuAvailable: Boolean,
    onOpenGuide: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AmberAlert.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .border(1.dp, AmberAlert.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable { onOpenGuide() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                tint = AmberAlert,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Aguardando Privilégios do Sistema",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (isShizukuAvailable)
                        "Shizuku detectado! Toque aqui para conceder acesso em 1 clique."
                    else
                        "Toque aqui para ver comandos ADB únicos para autorizar o Deep Doze.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun MasterDozeCard(
    isEnabled: Boolean,
    isRunning: Boolean,
    batteryLevel: Int,
    onToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .testTag("master_doze_card")
            .fillMaxWidth()
            .background(DarkSurfaceCard, RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = if (isEnabled) EmeraldPrimary.copy(alpha = 0.6f) else DarkSurfaceBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (isEnabled) EmeraldPrimary else TextTertiary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEnabled) "PROTEÇÃO DOZE ATIVA" else "MODO DOZE PAUSADO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) EmeraldPrimary else TextTertiary,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isEnabled) "Sono Profundo Imediato" else "Ativar Hibernação",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (isEnabled)
                        "Força Deep Doze no segundo em que a tela apaga"
                    else
                        "O sistema permanecerá em gerenciamento padrão",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("master_doze_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = EmeraldPrimary,
                    uncheckedThumbColor = TextTertiary,
                    uncheckedTrackColor = DarkSurfaceElevated
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceElevated, RoundedCornerShape(12.dp))
                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bateria Atual: $batteryLevel%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Text(
                text = if (isRunning) "Serviço Operando" else "Aguardando",
                fontSize = 11.sp,
                color = if (isRunning) EmeraldPrimary else TextSecondary
            )
        }
    }
}

@Composable
private fun HardwareControlsCard(
    cutoffWifi: Boolean,
    cutoffBluetooth: Boolean,
    cutoffMobileData: Boolean,
    cutoffAirplaneMode: Boolean,
    disconnectUnstable: Boolean,
    onToggleWifi: (Boolean) -> Unit,
    onToggleBluetooth: (Boolean) -> Unit,
    onToggleMobileData: (Boolean) -> Unit,
    onToggleAirplane: (Boolean) -> Unit,
    onToggleUnstable: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .testTag("hardware_controls_card")
            .fillMaxWidth()
            .background(DarkSurfaceCard, RoundedCornerShape(16.dp))
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Controle de Hardware ao Bloquear Tela",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
        Text(
            text = "Antenas desativadas com a tela apagada e religadas de forma escalonada",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        HardwareSwitchRow(
            icon = Icons.Default.Wifi,
            title = "Desligar Wi-Fi",
            subtitle = "cmd wifi set-wifi-enabled disabled",
            checked = cutoffWifi,
            onCheckedChange = onToggleWifi,
            testTag = "switch_cutoff_wifi"
        )

        Spacer(modifier = Modifier.height(8.dp))

        HardwareSwitchRow(
            icon = Icons.Default.Bluetooth,
            title = "Desligar Bluetooth",
            subtitle = "cmd bluetooth_manager disable",
            checked = cutoffBluetooth,
            onCheckedChange = onToggleBluetooth,
            testTag = "switch_cutoff_bt"
        )

        Spacer(modifier = Modifier.height(8.dp))

        HardwareSwitchRow(
            icon = Icons.Default.NetworkCell,
            title = "Desligar Dados Móveis",
            subtitle = "cmd phone data disable",
            checked = cutoffMobileData,
            onCheckedChange = onToggleMobileData,
            testTag = "switch_cutoff_data"
        )

        Spacer(modifier = Modifier.height(8.dp))

        HardwareSwitchRow(
            icon = Icons.Default.AirplanemodeActive,
            title = "Ativar Modo Avião",
            subtitle = "settings put global airplane_mode_on 1",
            checked = cutoffAirplaneMode,
            onCheckedChange = onToggleAirplane,
            testTag = "switch_cutoff_airplane"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Unstable Network cut-off
        HardwareSwitchRow(
            icon = Icons.Default.SignalCellularConnectedNoInternet0Bar,
            title = "Desconectar Redes Instáveis (<1 barra)",
            subtitle = "Desativa antena por 15 min se sinal estiver fraco para evitar superaquecimento",
            checked = disconnectUnstable,
            onCheckedChange = onToggleUnstable,
            testTag = "switch_unstable_net"
        )
    }
}

@Composable
private fun HardwareSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(if (checked) EmeraldGlow else DarkSurfaceCard, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) EmeraldPrimary else TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(text = subtitle, fontSize = 10.sp, color = TextTertiary, maxLines = 1)
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = EmeraldPrimary,
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = DarkSurfaceCard
            )
        )
    }
}

@Composable
private fun MaintenanceWindowCard(
    enabled: Boolean,
    intervalMinutes: Int,
    onToggle: (Boolean) -> Unit,
    onSelectInterval: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .testTag("maintenance_window_card")
            .fillMaxWidth()
            .background(DarkSurfaceCard, RoundedCornerShape(16.dp))
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(CyanAccent.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Janela de Manutenção Cíclica",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Abre o sistema por 1 min para puxar mensagens urgentes e fecha de novo",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("switch_maintenance_window"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = CyanAccent,
                    uncheckedThumbColor = TextTertiary,
                    uncheckedTrackColor = DarkSurfaceElevated
                )
            )
        }

        AnimatedVisibility(visible = enabled) {
            Column(modifier = Modifier.padding(top = 14.dp)) {
                Text(
                    text = "Intervalo da janela com tela apagada:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        30 to "A cada 30 min",
                        60 to "A cada 1 hora",
                        120 to "A cada 2 horas"
                    ).forEach { (mins, label) ->
                        val isSelected = intervalMinutes == mins
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectInterval(mins) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent.copy(alpha = 0.2f),
                                selectedLabelColor = CyanAccent,
                                containerColor = DarkSurfaceElevated,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) CyanAccent else DarkSurfaceBorder
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButtonsSection(
    onTestDoze: () -> Unit,
    onOpenReset: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Test Doze Button
        OutlinedButton(
            onClick = onTestDoze,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_test_deep_doze"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = DarkSurfaceElevated,
                contentColor = EmeraldPrimary
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Disparar Teste Manual (Deep Doze Imediato)")
        }

        // Emergency Safe Reset Button (Module E)
        Button(
            onClick = onOpenReset,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_safe_reset"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CoralWarning.copy(alpha = 0.18f),
                contentColor = CoralWarning
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, CoralWarning.copy(alpha = 0.6f))
        ) {
            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Restaurar Padrões / Safe Reset (Pré-Desinstalação)", fontWeight = FontWeight.Bold)
        }
    }
}
