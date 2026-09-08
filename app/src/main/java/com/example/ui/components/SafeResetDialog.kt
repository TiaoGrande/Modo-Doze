package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralWarning
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SafeResetDialog(
    onDismiss: () -> Unit,
    onConfirmReset: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("safe_reset_dialog"),
        shape = RoundedCornerShape(20.dp),
        containerColor = DarkSurfaceCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(CoralWarning.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = CoralWarning,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Restaurar Padrões / Safe Reset",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Deseja restaurar todas as configurações e desfazer as alterações do sistema?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(text = "O aplicativo irá reverter imediatamente:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "• Reativação de Wi-Fi, Bluetooth e Dados Móveis", fontSize = 11.sp, color = TextPrimary)
                    Text(text = "• Desativação forçada do Modo Avião", fontSize = 11.sp, color = TextPrimary)
                    Text(text = "• Reativação da Sincronização Nativa (Master Sync)", fontSize = 11.sp, color = TextPrimary)
                    Text(text = "• Limpeza de todas as regras da Whitelist", fontSize = 11.sp, color = TextPrimary)
                    Text(text = "• Execução de 'dumpsys deviceidle reset'", fontSize = 11.sp, color = CoralWarning)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Recomendado antes de desinstalar o utilitário para garantir que o gerenciador de energia volte ao estado original.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmReset,
                modifier = Modifier.testTag("btn_confirm_safe_reset"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoralWarning,
                    contentColor = Color.White
                )
            ) {
                Text("Executar Safe Reset", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_safe_reset")
            ) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}
