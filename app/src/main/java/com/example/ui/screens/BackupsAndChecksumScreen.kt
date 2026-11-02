package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BackupDumpEntity
import com.example.data.FirmwareEntity
import com.example.domain.ChecksumReport
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SynthAmberWarn
import com.example.ui.theme.SynthCyan
import com.example.ui.theme.SynthOrange
import com.example.ui.theme.SynthPhosphor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupsAndChecksumScreen(
    selectedFirmware: FirmwareEntity?,
    checksumReport: ChecksumReport?,
    customExpectedHash: String,
    backups: List<BackupDumpEntity>,
    isFlashing: Boolean,
    onUpdateExpectedHash: (String) -> Unit,
    onImportFileForVerify: () -> Unit,
    onCreateBackupNow: () -> Unit,
    onRestoreBackup: (BackupDumpEntity) -> Unit,
    onDeleteBackup: (Int) -> Unit,
    onExportBackup: (BackupDumpEntity) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("backups_checksum_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Cryptographic Checksum Verifier & Comparator (SHA-256 / CRC32 / MD5)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SynthCyan.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Контрольные суммы",
                                tint = SynthCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Верификатор контрольных сумм",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "Проверка целостности SHA-256, CRC32 и MD5 перед прошивкой",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onImportFileForVerify,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = "Открыть файл",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Проверить файл", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (checksumReport != null) {
                        val trimmedExpected = customExpectedHash.trim()
                        val matchesAny = trimmedExpected.isNotEmpty() && (
                            trimmedExpected.equals(checksumReport.sha256, ignoreCase = true) ||
                                trimmedExpected.equals(checksumReport.crc32, ignoreCase = true) ||
                                trimmedExpected.equals(checksumReport.md5, ignoreCase = true)
                            )

                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Активный образ: ${selectedFirmware?.name ?: "Бинарный пакет"} (${checksumReport.sizeBytes} байт)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SynthOrange
                                )
                                Text(
                                    text = "SHA-256: ${checksumReport.sha256}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "CRC32:   ${checksumReport.crc32}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SynthPhosphor
                                )
                                Text(
                                    text = "MD5:     ${checksumReport.md5}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SynthCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = customExpectedHash,
                            onValueChange = onUpdateExpectedHash,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_hash_input"),
                            label = { Text("Эталонный хэш для сверки (SHA-256, CRC32 или MD5)") },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            color = if (matchesAny) {
                                SynthPhosphor.copy(alpha = 0.16f)
                            } else {
                                SynthAmberWarn.copy(alpha = 0.16f)
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (matchesAny) SynthPhosphor else SynthAmberWarn
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (matchesAny) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = "Результат сверки",
                                    tint = if (matchesAny) SynthPhosphor else SynthAmberWarn,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (matchesAny) {
                                        "СОВПАДЕНИЕ ПОДТВЕРЖДЕНО: Контрольная сумма идентична эталону!"
                                    } else {
                                        "ВВЕДИТЕ ЭТАЛОННЫЙ SHA-256 / CRC32 / MD5 ИЛИ ПРОВЕРЬТЕ РАСХОЖДЕНИЕ"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (matchesAny) SynthPhosphor else SynthAmberWarn
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. EEPROM & Preset Backup Manager Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Резервные копии настроек, пресетов и загрузчика",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Сохраняйте полный дамп EEPROM (64 патча) и защищенный заголовок 0x0000–0x3FFF перед сменой прошивки или экспортируйте в облако/файл.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onCreateBackupNow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_backup_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SynthOrange,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Создать бэкап"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "СОЗДАТЬ СНИМОК EEPROM & BOOT HEADER СЕЙЧАС",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. List of Saved Backups
        items(backups, key = { it.id }) { backup ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("backup_card_${backup.id}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = backup.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = SynthPhosphor
                            )
                            Text(
                                text = "${backup.deviceName} • ${dateFormat.format(Date(backup.createdAt))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDeleteBackup(backup.id) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Удалить бэкап",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = backup.notes,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Область: ${backup.regionHex} (${backup.sizeBytes / 1024} КБ • ${backup.presetCount} пресетов)",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = SynthCyan
                        )
                        Text(
                            text = "CRC32: ${backup.crc32} | SHA-256: ${backup.sha256.take(28)}…",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = SynthPhosphor
                        )
                        Text(
                            text = "HEX: ${backup.rawHexPreview}",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onRestoreBackup(backup) },
                            enabled = !isFlashing,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SynthPhosphor,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = "Восстановить",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Восстановить", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onExportBackup(backup) },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, SynthCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Экспорт",
                                tint = SynthCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Экспорт дампа", color = SynthCyan)
                        }
                    }
                }
            }
        }
    }
}
