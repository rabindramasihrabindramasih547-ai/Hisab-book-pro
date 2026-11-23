package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.entity.ReminderEntity
import com.example.data.entity.WorkerEntity
import com.example.domain.ReminderType
import com.example.domain.WorkerLedgerSummary
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.DateInputRow
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.WorkerDropdownSelector
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.LocalHisabColors
import com.example.ui.theme.NavyPrimary
import com.example.util.ExportManager
import com.example.util.IndiaFormatters
import com.example.util.NotificationHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemindersScreen(
    businessName: String,
    workers: List<WorkerEntity>,
    ledgers: Map<Long, WorkerLedgerSummary>,
    reminders: List<ReminderEntity>,
    onSaveReminder: (ReminderEntity) -> Unit,
    onToggleCompleted: (ReminderEntity) -> Unit,
    onDeleteReminder: (Long) -> Unit
) {
    val strings = LocalStrings.current
    val extra = LocalHisabColors.current
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var whatsappConfirmTarget by remember { mutableStateOf<Pair<WorkerEntity, Long>?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    val pendingWorkers = remember(workers, ledgers) {
        workers.mapNotNull { w ->
            val bal = ledgers[w.id]?.remainingBalancePaise ?: 0L
            if (bal > 0L) w to bal else null
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.remindersTitle, style = MaterialTheme.typography.titleLarge)
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.addReminder, color = Color.White)
                }
            }
        }

        if (pendingWorkers.isNotEmpty()) {
            item {
                Text(
                    strings.pendingPaymentsSuggested,
                    style = MaterialTheme.typography.titleMedium,
                    color = extra.gold
                )
            }
            items(pendingWorkers, key = { "pend_${it.first.id}" }) { (worker, balance) ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, extra.cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(worker.fullName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${strings.remainingBalanceLabel}: ${IndiaFormatters.formatInr(balance)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = extra.crimson,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !NotificationHelper.hasNotificationPermission(context)
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                NotificationHelper.postReminderNotification(
                                    context,
                                    worker.id.toInt(),
                                    "${strings.reminderPendingPayment}: ${worker.fullName}",
                                    "${strings.remainingBalanceLabel}: ${IndiaFormatters.formatInr(balance)}"
                                )
                            }
                        }) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = strings.remindersTitle, tint = extra.gold)
                        }
                        OutlinedButton(onClick = { whatsappConfirmTarget = worker to balance }) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp")
                        }
                    }
                }
            }
        }

        if (reminders.isEmpty() && pendingWorkers.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.NotificationsActive,
                    title = strings.remindersTitle,
                    subtitle = "",
                    actionLabel = strings.addReminder,
                    onAction = { showAddDialog = true }
                )
            }
        } else {
            items(reminders, key = { "rem_${it.id}" }) { rem ->
                val typeLabel = when (ReminderType.fromCode(rem.type)) {
                    ReminderType.PENDING_PAYMENT -> strings.reminderPendingPayment
                    ReminderType.PAYMENT_DUE -> strings.reminderPaymentDue
                    ReminderType.MONTHLY_REPORT -> strings.reminderMonthlyReport
                    ReminderType.BACKUP -> strings.reminderBackup
                }
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, extra.cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onToggleCompleted(rem) }) {
                            Icon(
                                imageVector = if (rem.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = strings.confirm,
                                tint = if (rem.isCompleted) extra.emerald else extra.textMuted
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(rem.title, style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.width(6.dp))
                                StatusPill(typeLabel, extra.goldContainer, extra.gold)
                            }
                            Text(
                                buildString {
                                    append(IndiaFormatters.formatDateKey(rem.dueDateKey))
                                    if (rem.amountPaise > 0L) append(" • ${IndiaFormatters.formatInr(rem.amountPaise)}")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = extra.textMuted
                            )
                        }
                        IconButton(onClick = { onDeleteReminder(rem.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = extra.crimson)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(64.dp)) }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var type by remember { mutableStateOf(ReminderType.PAYMENT_DUE) }
        var dueDateText by remember { mutableStateOf(IndiaFormatters.formatDateKey(IndiaFormatters.todayDateKey())) }
        var amountText by remember { mutableStateOf("") }
        var selectedWorkerId by remember { mutableStateOf<Long?>(workers.firstOrNull()?.id) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(strings.addReminder) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            ReminderType.PAYMENT_DUE to strings.reminderPaymentDue,
                            ReminderType.PENDING_PAYMENT to strings.reminderPendingPayment,
                            ReminderType.MONTHLY_REPORT to strings.reminderMonthlyReport,
                            ReminderType.BACKUP to strings.reminderBackup
                        ).forEach { (rt, label) ->
                            FilterChip(
                                selected = type == rt,
                                onClick = {
                                    type = rt
                                    if (title.isBlank()) title = label
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    if (workers.isNotEmpty() && (type == ReminderType.PAYMENT_DUE || type == ReminderType.PENDING_PAYMENT)) {
                        WorkerDropdownSelector(
                            workers = workers,
                            selectedWorkerId = selectedWorkerId,
                            label = strings.selectWorker.removeSuffix(" *"),
                            onWorkerSelected = { selectedWorkerId = it.id }
                        )
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(strings.descriptionLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    DateInputRow(
                        label = strings.workDate.removeSuffix(" *"),
                        dateText = dueDateText,
                        onDateTextChange = { dueDateText = it }
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text(strings.paymentAmount.removeSuffix(" *")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val key = IndiaFormatters.parseDdMmYyyyToDateKey(dueDateText) ?: IndiaFormatters.todayDateKey()
                    val finalTitle = title.trim().ifBlank { strings.reminderPaymentDue }
                    onSaveReminder(
                        ReminderEntity(
                            workerId = selectedWorkerId,
                            title = finalTitle,
                            type = type.code,
                            dueDateKey = key,
                            amountPaise = IndiaFormatters.parseRupeesToPaise(amountText) ?: 0L
                        )
                    )
                    showAddDialog = false
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    whatsappConfirmTarget?.let { (worker, balance) ->
        ConfirmActionDialog(
            title = strings.sendWhatsappReminder,
            message = strings.confirmSendWhatsapp,
            confirmText = strings.confirm,
            isDestructive = false,
            onConfirm = {
                val msg = "*$businessName*\n${worker.fullName} (${worker.workerCode})\n${strings.remainingBalanceLabel}: ${IndiaFormatters.formatInr(balance)}"
                ExportManager.shareOnWhatsApp(context, msg)
                whatsappConfirmTarget = null
            },
            onDismiss = { whatsappConfirmTarget = null }
        )
    }
}
