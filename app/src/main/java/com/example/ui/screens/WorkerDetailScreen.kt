package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.AdvanceEntity
import com.example.data.entity.AttendanceEntity
import com.example.data.entity.PaymentEntity
import com.example.data.entity.WorkEntryEntity
import com.example.data.entity.WorkerEntity
import com.example.domain.AttendanceStatus
import com.example.domain.HisabCalculator
import com.example.domain.LedgerDateFilter
import com.example.domain.PaymentMethod
import com.example.domain.PaymentType
import com.example.domain.WorkType
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.DateInputRow
import com.example.ui.components.StatusPill
import com.example.ui.components.WorkerAvatar
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.GoldBright
import com.example.ui.theme.LocalHisabColors
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.HisabViewModel
import com.example.util.ExportManager
import com.example.util.IndiaFormatters
import com.example.util.ReportTable
import kotlinx.coroutines.launch

@Composable
fun WorkerDetailScreen(
    worker: WorkerEntity,
    allWorkEntries: List<WorkEntryEntity>,
    allAttendance: List<AttendanceEntity>,
    allAdvances: List<AdvanceEntity>,
    allPayments: List<PaymentEntity>,
    onEditWorker: () -> Unit,
    onDeleteWorker: () -> Unit,
    onAddWork: () -> Unit,
    onEditWork: (WorkEntryEntity) -> Unit,
    onDeleteWork: (Long) -> Unit,
    onGiveAdvance: () -> Unit,
    onDeleteAdvance: (Long) -> Unit,
    onRecordPayment: () -> Unit,
    onDeletePayment: (Long) -> Unit,
    onShowAdvanceReceipt: (AdvanceEntity) -> Unit,
    onBuildLedgerPdfTable: (Long, Long) -> ReportTable
) {
    val strings = LocalStrings.current
    val extra = LocalHisabColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var dateFilter by remember { mutableStateOf(LedgerDateFilter.ALL_TIME) }
    var customStartText by remember { mutableStateOf(IndiaFormatters.formatDateKey(IndiaFormatters.startOfMonthDateKey())) }
    var customEndText by remember { mutableStateOf(IndiaFormatters.formatDateKey(IndiaFormatters.todayDateKey())) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDeleteWorkerConfirm by remember { mutableStateOf(false) }

    val todayKey = IndiaFormatters.todayDateKey()
    val (startKey, endKey) = remember(dateFilter, customStartText, customEndText, todayKey) {
        when (dateFilter) {
            LedgerDateFilter.TODAY -> todayKey to todayKey
            LedgerDateFilter.THIS_WEEK -> IndiaFormatters.startOfWeekDateKey(todayKey) to todayKey
            LedgerDateFilter.THIS_MONTH -> IndiaFormatters.startOfMonthDateKey(todayKey) to IndiaFormatters.endOfMonthDateKey(todayKey)
            LedgerDateFilter.ALL_TIME -> 19700101L to 29991231L
            LedgerDateFilter.CUSTOM -> {
                val s = IndiaFormatters.parseDdMmYyyyToDateKey(customStartText) ?: 19700101L
                val e = IndiaFormatters.parseDdMmYyyyToDateKey(customEndText) ?: 29991231L
                minOf(s, e) to maxOf(s, e)
            }
        }
    }

    val filteredWorks = remember(allWorkEntries, worker.id, startKey, endKey) {
        allWorkEntries.filter { it.workerId == worker.id && it.dateKey in startKey..endKey }
    }
    val filteredAdvances = remember(allAdvances, worker.id, startKey, endKey) {
        allAdvances.filter { it.workerId == worker.id && it.dateKey in startKey..endKey }
    }
    val filteredPayments = remember(allPayments, worker.id, startKey, endKey) {
        allPayments.filter { it.workerId == worker.id && it.dateKey in startKey..endKey }
    }
    val filteredAttendance = remember(allAttendance, worker.id, startKey, endKey) {
        allAttendance.filter { it.workerId == worker.id && it.dateKey in startKey..endKey }
    }

    val ledger = remember(worker.id, filteredWorks, filteredAdvances, filteredPayments) {
        HisabCalculator.computeWorkerLedger(worker.id, filteredWorks, filteredAdvances, filteredPayments)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Worker Profile Navy Header
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = NavyDeep)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WorkerAvatar(name = worker.fullName, photoPath = worker.photoUri, size = 60.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(worker.fullName, style = MaterialTheme.typography.titleLarge, color = Color.White)
                            Text(
                                "${worker.workerCode} • ${worker.workCategory}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldBright
                            )
                            Text(
                                "${worker.mobileNumber} • Joined ${IndiaFormatters.formatDateKey(worker.joiningDateKey)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        IconButton(onClick = onEditWorker) {
                            Icon(Icons.Default.Edit, contentDescription = strings.editWorker, tint = Color.White)
                        }
                        IconButton(onClick = { showDeleteWorkerConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = strings.deleteWorker, tint = extra.crimson)
                        }
                    }

                    if (worker.address.isNotBlank() || worker.notes.isNotBlank()) {
                        Text(
                            text = listOfNotNull(
                                worker.address.takeIf { it.isNotBlank() },
                                worker.notes.takeIf { it.isNotBlank() }
                            ).joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RateBadge("Daily", worker.dailyRatePaise)
                        RateBadge("Full", worker.fullTimeRatePaise)
                        RateBadge("Half", worker.halfTimeRatePaise)
                        RateBadge("Hour", worker.hourlyRatePaise)
                    }
                }
            }
        }

        // Quick Actions for this Worker
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddWork,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    modifier = Modifier.testTag("worker_add_work_btn")
                ) {
                    Icon(Icons.Default.WorkHistory, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.addWork, color = Color.White)
                }
                Button(
                    onClick = onGiveAdvance,
                    colors = ButtonDefaults.buttonColors(containerColor = extra.gold),
                    modifier = Modifier.testTag("worker_give_advance_btn")
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.giveAdvance, color = Color.White)
                }
                Button(
                    onClick = onRecordPayment,
                    colors = ButtonDefaults.buttonColors(containerColor = extra.emerald),
                    modifier = Modifier.testTag("worker_pay_btn")
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.recordPayment, color = Color.White)
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val table = onBuildLedgerPdfTable(startKey, endKey)
                            val pdf = ExportManager.generatePdf(context, table, "ledger_${worker.workerCode}")
                            ExportManager.shareFile(context, pdf, "application/pdf", table.title)
                        }
                    }
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF")
                }
                OutlinedButton(
                    onClick = {
                        val summaryMsg = buildString {
                            appendLine("*${worker.fullName} (${worker.workerCode}) - Hisab*")
                            appendLine("${strings.totalWorkLabel}: ${IndiaFormatters.formatInr(ledger.totalWorkEarningsPaise)}")
                            appendLine("${strings.totalAdvanceGiven}: ${IndiaFormatters.formatInr(ledger.totalAdvanceGivenPaise)}")
                            appendLine("${strings.totalPaymentsMade}: ${IndiaFormatters.formatInr(ledger.totalPaymentsMadePaise)}")
                            appendLine("${strings.remainingBalanceLabel}: ${IndiaFormatters.formatInr(ledger.remainingBalancePaise)}")
                        }
                        ExportManager.shareOnWhatsApp(context, summaryMsg)
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp")
                }
            }
        }

        // Date Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        LedgerDateFilter.ALL_TIME to strings.dateFilterAll,
                        LedgerDateFilter.TODAY to strings.dateFilterToday,
                        LedgerDateFilter.THIS_WEEK to strings.dateFilterWeek,
                        LedgerDateFilter.THIS_MONTH to strings.dateFilterMonth,
                        LedgerDateFilter.CUSTOM to strings.dateFilterCustom
                    ).forEach { (filter, label) ->
                        FilterChip(
                            selected = dateFilter == filter,
                            onClick = { dateFilter = filter },
                            label = { Text(label) }
                        )
                    }
                }
                if (dateFilter == LedgerDateFilter.CUSTOM) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateInputRow(
                            label = strings.fromDate,
                            dateText = customStartText,
                            onDateTextChange = { customStartText = it },
                            modifier = Modifier.weight(1f)
                        )
                        DateInputRow(
                            label = strings.toDate,
                            dateText = customEndText,
                            onDateTextChange = { customEndText = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Individual Worker Ledger Summary Card
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, extra.cardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.workerLedger, style = MaterialTheme.typography.titleMedium)
                    LedgerStatLine(
                        "${strings.totalWorkLabel} (${ledger.totalWorkCount})",
                        IndiaFormatters.formatInr(ledger.totalWorkEarningsPaise),
                        extra.emerald
                    )
                    LedgerStatLine(
                        strings.totalAdvanceGiven,
                        IndiaFormatters.formatInr(ledger.totalAdvanceGivenPaise),
                        extra.gold
                    )
                    LedgerStatLine(
                        strings.advanceAdjustedLabel,
                        IndiaFormatters.formatInr(ledger.totalAdvanceAdjustedPaise),
                        extra.textMuted
                    )
                    LedgerStatLine(
                        strings.remainingAdvanceLabel,
                        IndiaFormatters.formatInr(ledger.remainingAdvanceBalancePaise),
                        extra.gold
                    )
                    LedgerStatLine(
                        strings.netPayableLabel,
                        IndiaFormatters.formatInr(ledger.netPayablePaise),
                        MaterialTheme.colorScheme.onSurface
                    )
                    LedgerStatLine(
                        strings.totalPaymentsMade,
                        IndiaFormatters.formatInr(ledger.totalPaymentsMadePaise),
                        extra.emerald
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (ledger.isOverpaid) strings.overpaidLabel else strings.remainingBalanceLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = IndiaFormatters.formatInr(ledger.remainingBalancePaise),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                ledger.remainingBalancePaise > 0L -> extra.crimson
                                ledger.remainingBalancePaise < 0L -> extra.gold
                                else -> extra.emerald
                            },
                            modifier = Modifier.testTag("worker_remaining_balance")
                        )
                    }
                }
            }
        }

        // Tabs for Work History / Advances & Payments / Attendance
        item {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("${strings.workHistory} (${filteredWorks.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("${strings.transactionHistory} (${filteredAdvances.size + filteredPayments.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("${strings.attendanceTitle.substringBefore(" ")} (${filteredAttendance.size})") }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                items(filteredWorks, key = { "wd_work_${it.id}" }) { w ->
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${IndiaFormatters.formatDateKey(w.dateKey)} • ${HisabViewModel.workTypeLabel(WorkType.fromCode(w.workType), strings)}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    buildString {
                                        if (w.totalHours > 0) append("${w.totalHours}h ")
                                        if (w.description.isNotBlank()) append("• ${w.description}")
                                    }.ifBlank { IndiaFormatters.formatInr(w.ratePaise) },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extra.textMuted
                                )
                            }
                            Text(
                                IndiaFormatters.formatInr(w.totalAmountPaise),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = extra.emerald
                            )
                            IconButton(onClick = { onEditWork(w) }) {
                                Icon(Icons.Default.Edit, contentDescription = strings.edit)
                            }
                            IconButton(onClick = { onDeleteWork(w.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = extra.crimson)
                            }
                        }
                    }
                }
            }
            1 -> {
                items(filteredAdvances, key = { "wd_adv_${it.id}" }) { adv ->
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
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusPill(strings.tabAdvances, extra.goldContainer, extra.gold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(IndiaFormatters.formatDateKey(adv.dateKey), style = MaterialTheme.typography.titleSmall)
                                }
                                Text(
                                    "${adv.receiptNumber} • ${HisabViewModel.paymentMethodLabel(PaymentMethod.fromCode(adv.paymentMethod), strings)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extra.textMuted
                                )
                            }
                            Text(
                                IndiaFormatters.formatInr(adv.amountPaise),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = extra.gold
                            )
                            IconButton(onClick = { onShowAdvanceReceipt(adv) }) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = strings.viewReceipt)
                            }
                            IconButton(onClick = { onDeleteAdvance(adv.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = extra.crimson)
                            }
                        }
                    }
                }
                items(filteredPayments, key = { "wd_pay_${it.id}" }) { pay ->
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
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusPill(
                                        HisabViewModel.paymentTypeLabel(PaymentType.fromCode(pay.paymentType), strings),
                                        extra.emeraldContainer,
                                        extra.emerald
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(IndiaFormatters.formatDateKey(pay.dateKey), style = MaterialTheme.typography.titleSmall)
                                }
                                Text(
                                    "${HisabViewModel.paymentMethodLabel(PaymentMethod.fromCode(pay.paymentMethod), strings)} ${if (pay.transactionId.isNotBlank()) "• ${pay.transactionId}" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extra.textMuted
                                )
                            }
                            Text(
                                IndiaFormatters.formatInr(pay.amountPaise),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = extra.emerald
                            )
                            IconButton(onClick = { onDeletePayment(pay.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = extra.crimson)
                            }
                        }
                    }
                }
            }
            else -> {
                items(filteredAttendance, key = { "wd_att_${it.id}" }) { att ->
                    val status = AttendanceStatus.fromCode(att.status)
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, extra.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(IndiaFormatters.formatDateKey(att.dateKey), style = MaterialTheme.typography.titleSmall)
                                if (att.hours > 0.0 || att.notes.isNotBlank()) {
                                    Text(
                                        listOfNotNull(
                                            att.hours.takeIf { it > 0.0 }?.let { "${it}h" },
                                            att.notes.takeIf { it.isNotBlank() }
                                        ).joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = extra.textMuted
                                    )
                                }
                            }
                            StatusPill(
                                text = HisabViewModel.attendanceLabel(status, strings),
                                containerColor = when (status) {
                                    AttendanceStatus.ABSENT -> extra.crimsonContainer
                                    AttendanceStatus.LEAVE -> extra.goldContainer
                                    else -> extra.emeraldContainer
                                },
                                contentColor = when (status) {
                                    AttendanceStatus.ABSENT -> extra.crimson
                                    AttendanceStatus.LEAVE -> extra.gold
                                    else -> extra.emerald
                                }
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(64.dp)) }
    }

    if (showDeleteWorkerConfirm) {
        ConfirmActionDialog(
            title = "${strings.deleteWorker}: ${worker.fullName}",
            message = strings.deleteWorkerConfirm,
            confirmText = strings.delete,
            onConfirm = {
                showDeleteWorkerConfirm = false
                onDeleteWorker()
            },
            onDismiss = { showDeleteWorkerConfirm = false }
        )
    }
}

@Composable
private fun RateBadge(label: String, paise: Long) {
    Surface(
        color = Color.White.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
            Text(
                IndiaFormatters.formatInr(paise, showDecimals = false),
                style = MaterialTheme.typography.labelLarge,
                color = GoldBright
            )
        }
    }
}

@Composable
private fun LedgerStatLine(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = LocalHisabColors.current.textMuted)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
