package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AdbInstructionsDialog(
    isShizukuAvailable: Boolean,
    isShizukuGranted: Boolean,
    hasDump: Boolean,
    hasWriteSecure: Boolean,
    onDismiss: () -> Unit,
    onRequestShizukuPermission: () -> Unit
) {
    val context = LocalContext.current
    val cmdDump = "adb shell pm grant com.aistudio.mododoze.dfntv android.permission.DUMP"
    val cmdSecure = "adb shell pm grant com.aistudio.mododoze.dfntv android.permission.WRITE_SECURE_SETTINGS"

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("adb_instructions_dialog"),
        shape = RoundedCornerShape(20.dp),
        containerColor = DarkSurfaceCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Privilégios Elevados (Shizuku / ADB)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Para forçar o Modo Doze Profundo e controlar rádios com consumo zero de bateria, o app necessita de privilégios de sistema via Shizuku (automático) ou comandos ADB únicos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Status Cards
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceElevated, RoundedCornerShape(12.dp))
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    PermissionStatusRow(
                        title = "Shizuku Serviço",
                        subtitle = if (isShizukuAvailable) {
                            if (isShizukuGranted) "Autorizado e Ativo" else "Em execução (necessita permissão)"
                        } else "Não detectado",
                        isGranted = isShizukuGranted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PermissionStatusRow(
                        title = "Permissão DUMP",
                        subtitle = if (hasDump) "Concedida via ADB/Root" else "Pendente",
                        isGranted = hasDump
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PermissionStatusRow(
                        title = "Permissão WRITE_SECURE_SETTINGS",
                        subtitle = if (hasWriteSecure) "Concedida via ADB/Root" else "Pendente",
                        isGranted = hasWriteSecure
                    )
                }

                if (isShizukuAvailable && !isShizukuGranted) {
                    Spacer(modifier = Modifier.height(14.dp))
                    FilledTonalButton(
                        onClick = onRequestShizukuPermission,
                        modifier = Modifier.fillMaxWidth().testTag("btn_request_shizuku"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = androidx.compose.ui.graphics.Color.Black
                        )
                    ) {
                        Text("Autorizar via Shizuku (1 Toque)")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Opção Manual via Computador (ADB):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Conecte o smartphone com Depuração USB ativada e execute os comandos abaixo no Prompt de Comando / Terminal:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))
                CommandSnippetBox(
                    command = cmdDump,
                    onCopy = {
                        copyToClipboard(context, cmdDump)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
                CommandSnippetBox(
                    command = cmdSecure,
                    onCopy = {
                        copyToClipboard(context, cmdSecure)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_close_button")
            ) {
                Text("Entendido", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun PermissionStatusRow(
    title: String,
    subtitle: String,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isGranted) EmeraldPrimary else AmberAlert,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun CommandSnippetBox(
    command: String,
    onCopy: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
            .clickable { onCopy() }
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = command,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = EmeraldPrimary,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(EmeraldGlow, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copiar Comando",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("ADB Command", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Comando copiado!", Toast.LENGTH_SHORT).show()
}
