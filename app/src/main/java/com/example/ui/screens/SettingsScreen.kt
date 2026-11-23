package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.prefs.AppSettings
import com.example.domain.PaymentMethod
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.LocalHisabColors
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.HisabViewModel
import com.example.util.IndiaFormatters
import com.example.util.PinSecurity
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdateBusinessProfile: (String, String, String) -> Unit,
    onUpdateBusinessLogo: (android.net.Uri) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetDefaultPaymentMethod: (PaymentMethod) -> Unit,
    onSetPin: (String) -> Unit,
    onDisablePin: () -> Unit,
    onSetBiometric: (Boolean) -> Unit,
    onExportBackupJson: suspend () -> String,
    onRestoreBackupJson: suspend (String) -> Result<Int>,
    onClearAllData: () -> Unit
) {
    val strings = LocalStrings.current
    val extra = LocalHisabColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var businessName by remember(settings.businessName) { mutableStateOf(settings.businessName) }
    var ownerName by remember(settings.ownerName) { mutableStateOf(settings.ownerName) }
    var ownerMobile by remember(settings.ownerMobile) { mutableStateOf(settings.ownerMobile) }

    var showPinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    val biometricSupported = remember { PinSecurity.isBiometricHardwareSupported(context) }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onUpdateBusinessLogo(uri)
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = onExportBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, strings.savedSuccess, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (!content.isNullOrBlank()) {
                pendingRestoreJson = content
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Business Profile Card
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, extra.cardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.businessProfile, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (!settings.businessLogoPath.isNullOrBlank() && File(settings.businessLogoPath).exists()) {
                            AsyncImage(
                                model = File(settings.businessLogoPath),
                                contentDescription = strings.businessLogo,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(56.dp).clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(NavyPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("₹", style = MaterialTheme.typography.titleLarge, color = extra.gold)
                            }
                        }
                        OutlinedButton(onClick = {
                            logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.businessLogo)
                        }
                    }

                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text(strings.businessName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text(strings.ownerName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ownerMobile,
                        onValueChange = { ownerMobile = it },
                        label = { Text("${strings.mobileLabel.removeSuffix(" *")}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onUpdateBusinessProfile(
                                businessName,
                                ownerName,
                                IndiaFormatters.formatIndianMobile(ownerMobile)
                            )
                            Toast.makeText(context, strings.savedSuccess, Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(strings.save, color = Color.White)
                    }
                }
            }
        }

        // Language, Theme, Currency, Default Payment Method
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, extra.cardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.currencyLabel, style = MaterialTheme.typography.bodyMedium)
                        Text("INR (₹) • India", style = MaterialTheme.typography.titleSmall, color = extra.emerald, fontWeight = FontWeight.Bold)
                    }

                    Text(strings.languageLabel, style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.language == AppLanguage.HINDI,
                            onClick = { onSetLanguage(AppLanguage.HINDI) },
                            label = { Text("हिन्दी") },
                            modifier = Modifier.testTag("settings_lang_hindi")
                        )
                        FilterChip(
                            selected = settings.language == AppLanguage.ENGLISH,
                            onClick = { onSetLanguage(AppLanguage.ENGLISH) },
                            label = { Text("English") },
                            modifier = Modifier.testTag("settings_lang_english")
                        )
                    }

                    Text(strings.themeLabel, style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            ThemeMode.SYSTEM to strings.themeSystem,
                            ThemeMode.LIGHT to strings.themeLight,
                            ThemeMode.DARK to strings.themeDark
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { onSetThemeMode(mode) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Text(strings.defaultPaymentMethodLabel, style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PaymentMethod.entries.forEach { pm ->
                            FilterChip(
                                selected = settings.defaultPaymentMethod == pm,
                                onClick = { onSetDefaultPaymentMethod(pm) },
                                label = { Text(HisabViewModel.paymentMethodLabel(pm, strings)) }
                            )
                        }
                    }
                }
            }
        }

        // Security & PIN Lock
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, extra.cardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = extra.gold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.securitySection, style = MaterialTheme.typography.titleMedium)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                newPinInput = ""
                                showPinDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                        ) {
                            Text(if (settings.pinEnabled) strings.changePin else strings.enablePin, color = Color.White)
                        }
                        if (settings.pinEnabled) {
                            OutlinedButton(onClick = onDisablePin) {
                                Text(strings.disablePin)
                            }
                        }
                    }

                    if (settings.pinEnabled && biometricSupported) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.biometricUnlock, style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = settings.biometricEnabled,
                                onCheckedChange = onSetBiometric
                            )
                        }
                    }
                }
            }
        }

        // Backup & Restore + Data Management
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, extra.cardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.backupRestore, style = MaterialTheme.typography.titleMedium)
                    if (settings.lastBackupTimestamp > 0L) {
                        Text(
                            "Last backup: ${IndiaFormatters.formatDateMillis(settings.lastBackupTimestamp)} ${IndiaFormatters.formatTimeMillis(settings.lastBackupTimestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = extra.textMuted
                        )
                    }
                    Button(
                        onClick = {
                            val name = "HisabBookPro_Backup_${IndiaFormatters.todayDateKey()}.json"
                            createBackupLauncher.launch(name)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = extra.emerald),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.exportBackupJson, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = { restoreBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.restoreBackupJson)
                    }
                    OutlinedButton(
                        onClick = { showClearAllConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = extra.crimson),
                        border = BorderStroke(1.dp, extra.crimson.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.clearAllData)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(64.dp)) }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(strings.enablePin) },
            text = {
                OutlinedTextField(
                    value = newPinInput,
                    onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) newPinInput = it },
                    label = { Text(strings.enter4DigitPin) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    enabled = newPinInput.length == 4,
                    onClick = {
                        onSetPin(newPinInput)
                        showPinDialog = false
                        Toast.makeText(context, strings.savedSuccess, Toast.LENGTH_SHORT).show()
                    }
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    pendingRestoreJson?.let { jsonContent ->
        ConfirmActionDialog(
            title = strings.restoreBackupJson,
            message = strings.restoreConfirm,
            confirmText = strings.confirm,
            isDestructive = true,
            onConfirm = {
                pendingRestoreJson = null
                scope.launch {
                    val result = onRestoreBackupJson(jsonContent)
                    val msg = if (result.isSuccess) strings.savedSuccess else (result.exceptionOrNull()?.message ?: strings.invalidInputError)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { pendingRestoreJson = null }
        )
    }

    if (showClearAllConfirm) {
        ConfirmActionDialog(
            title = strings.clearAllData,
            message = strings.clearAllDataConfirm,
            confirmText = strings.delete,
            onConfirm = {
                onClearAllData()
                showClearAllConfirm = false
                Toast.makeText(context, strings.deletedSuccess, Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showClearAllConfirm = false }
        )
    }
}
