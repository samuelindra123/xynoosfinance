package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.AppUpdateInfo
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RefinedRed
import com.example.ui.theme.SapphireSecondary
import com.example.ui.theme.WarmAmber

/**
 * Mandatory ("Wajib Update") or Optional Update prompt triggered by GitHub Releases metadata.
 * When `updateInfo.isMandatory` is true, back navigation and outside dismiss are locked so users
 * proceed directly to updating with one tap.
 */
@Composable
fun AppUpdatePromptDialog(
    updateInfo: AppUpdateInfo,
    currentVersionLabel: String,
    isChecking: Boolean,
    onUpdateNow: () -> Unit,
    onRecheck: () -> Unit,
    onRemindLater: () -> Unit,
    onSkipVersion: () -> Unit
) {
    val accent = if (updateInfo.isMandatory) RefinedRed else EmeraldPrimary
    val badgeBg = if (updateInfo.isMandatory) RefinedRed.copy(alpha = 0.16f) else EmeraldPrimary.copy(alpha = 0.16f)
    val badgeText = if (updateInfo.isMandatory) "PEMBARUAN WAJIB" else "VERSI BARU TERSEDIA"

    Dialog(
        onDismissRequest = {
            if (!updateInfo.isMandatory) {
                onRemindLater()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.isMandatory,
            dismissOnClickOutside = !updateInfo.isMandatory
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_update_prompt_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(badgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (updateInfo.isMandatory) Icons.Default.SystemUpdate else Icons.Default.NewReleases,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = badgeBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (updateInfo.isMandatory) {
                                "Wajib Update ke ${updateInfo.tagName}"
                            } else {
                                "Tersedia Rilis ${updateInfo.tagName}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Terpasang", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currentVersionLabel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        Text("→", style = MaterialTheme.typography.titleMedium, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Versi Terbaru", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${updateInfo.tagName} (Build ${updateInfo.remoteVersionCode})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }

                Text(
                    text = "Catatan Pembaruan:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SapphireSecondary
                )

                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                ) {
                    Text(
                        text = updateInfo.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    )
                }

                Text(
                    text = "Menekan tombol di bawah langsung mengunduh file APK resmi dari GitHub Releases. Setelah selesai, ketuk file untuk memperbarui aplikasi tanpa menghapus data.",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmAmber
                )

                Button(
                    onClick = onUpdateNow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = if (updateInfo.isMandatory) Color.White else Color(0xFF081018)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("update_now_button")
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Update Sekarang (${updateInfo.tagName})", fontWeight = FontWeight.Bold)
                }

                if (updateInfo.isMandatory) {
                    OutlinedButton(
                        onClick = onRecheck,
                        enabled = !isChecking,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isChecking) "Memeriksa Rilis GitHub..." else "Periksa Ulang Versi Terbaru")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onSkipVersion) {
                            Text("Lewati Versi Ini", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = onRemindLater) {
                            Text("Nanti Saja")
                        }
                    }
                }
            }
        }
    }
}
