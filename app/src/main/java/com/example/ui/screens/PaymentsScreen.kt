package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.entity.AdvanceEntity
import com.example.data.entity.PaymentEntity
import com.example.data.entity.WorkerEntity
import com.example.domain.PaymentMethod
import com.example.domain.PaymentType
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.LocalHisabColors
import com.example.ui.viewmodel.HisabViewModel
import com.example.util.IndiaFormatters

@Composable
fun PaymentsScreen(
    workers: List<WorkerEntity>,
    payments: List<PaymentEntity>,
    advances: List<AdvanceEntity>,
    onAddPaymentClick: () -> Unit,
    onAddAdvanceClick: () -> Unit,
    onDeletePaymentConfirmed: (Long) -> Unit,
    onDeleteAdvanceConfirmed: (Long) -> Unit,
    onViewAdvanceReceipt: (AdvanceEntity) -> Unit,
    onOpenWorker: (Long) -> Unit
) {
    val strings = LocalStrings.current
    val extra = LocalHisabColors.current
    val workerMap = remember(workers) { workers.associateBy { it.id } }

    var activeTab by remember { mutableIntStateOf(0) }
    var selectedMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    var selectedWorkerId by remember { mutableStateOf<Long?>(null) }

    var pendingDeletePayment by remember { mutableStateOf<PaymentEntity?>(null) }
    var pendingDeleteAdvance by remember { mutableStateOf<AdvanceEntity?>(null) }

    val filteredPayments = remember(payments, selectedMethod, selectedWorkerId) {
        payments.filter { p ->
            (selectedMethod == null || p.paymentMethod == selectedMethod!!.code) &&
                (selectedWorkerId == null || p.workerId == selectedWorkerId)
        }
    }

    val filteredAdvances = remember(advances, selectedMethod, selectedWorkerId) {
        advances.filter { a ->
            (selectedMethod == null || a.paymentMethod == selectedMethod!!.code) &&
                (selectedWorkerId == null || a.workerId == selectedWorkerId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            SegmentedButton(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = { Icon(Icons.Default.Payments, contentDescription = null) },
                modifier = Modifier.testTag("seg_payments_tab")
            ) {
                Text(strings.tabPayments, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            SegmentedButton(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                modifier = Modifier.testTag("seg_advances_tab")
            ) {
                Text(strings.tabAdvances, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedMethod == null,
                    onClick = { selectedMethod = null },
                    label = { Text(strings.allCategories) }
                )
                PaymentMethod.entries.forEach { pm ->
                    FilterChip(
                        selected = selectedMethod == pm,
                        onClick = { selectedMethod = if (selectedMethod == pm) null else pm },
                        label = { Text(HisabViewModel.paymentMethodLabel(pm, strings)) }
                    )
                }
            }

            if (workers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedWorkerId == null,
                        onClick = { selectedWorkerId = null },
                        label = { Text("${strings.navWorkers}: ${strings.dateFilterAll}") }
                    )
                    workers.forEach { w ->
                        FilterChip(
                            selected = selectedWorkerId == w.id,
                            onClick = { selectedWorkerId = if (selectedWorkerId == w.id) null else w.id },
                            label = { Text(w.fullName) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (activeTab == 0) {
            val totalActualPaid = filteredPayments
                .filter { it.paymentType != PaymentType.ADVANCE_ADJUSTMENT.code }
                .sumOf { it.amountPaise }
            if (filteredPayments.isEmpty()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    EmptyStateCard(
                        icon = Icons.Default.Payments,
                        title = strings.noTransactionsYet,
                        subtitle = "",
                        actionLabel = if (workers.isNotEmpty()) strings.recordPayment else null,
                        onAction = if (workers.isNotEmpty()) onAddPaymentClick else null
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = extra.emeraldContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${filteredPayments.size} ${strings.entriesCount}", style = MaterialTheme.typography.titleSmall, color = extra.emerald)
                                Text(IndiaFormatters.formatInr(totalActualPaid), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = extra.emerald)
                            }
                        }
                    }
                    items(filteredPayments, key = { "pay_${it.id}" }) { payment ->
                        val wName = workerMap[payment.workerId]?.fullName ?: "Worker"
                        val pType = HisabViewModel.paymentTypeLabel(PaymentType.fromCode(payment.paymentType), strings)
                        val pMethod = HisabViewModel.paymentMethodLabel(PaymentMethod.fromCode(payment.paymentMethod), strings)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenWorker(payment.workerId) },
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(wName, style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        StatusPill(pType, extra.emeraldContainer, extra.emerald)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        buildString {
                                            append("${IndiaFormatters.formatDateKey(payment.dateKey)} • $pMethod")
                                            if (payment.transactionId.isNotBlank()) append(" • ${payment.transactionId}")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = extra.textMuted
                                    )
                                    if (payment.notes.isNotBlank()) {
                                        Text(payment.notes, style = MaterialTheme.typography.bodySmall, color = extra.textMuted)
                                    }
                                }
                                Text(
                                    IndiaFormatters.formatInr(payment.amountPaise),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = extra.emerald
                                )
                                IconButton(onClick = { pendingDeletePayment = payment }) {
                                    Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = extra.crimson)
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        } else {
            val totalAdvance = filteredAdvances.sumOf { it.amountPaise }
            if (filteredAdvances.isEmpty()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    EmptyStateCard(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = strings.noTransactionsYet,
                        subtitle = "",
                        actionLabel = if (workers.isNotEmpty()) strings.giveAdvance else null,
                        onAction = if (workers.isNotEmpty()) onAddAdvanceClick else null
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = extra.goldContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${filteredAdvances.size} ${strings.entriesCount}", style = MaterialTheme.typography.titleSmall, color = extra.gold)
                                Text(IndiaFormatters.formatInr(totalAdvance), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = extra.gold)
                            }
                        }
                    }
                    items(filteredAdvances, key = { "adv_${it.id}" }) { adv ->
                        val wName = workerMap[adv.workerId]?.fullName ?: "Worker"
                        val pMethod = HisabViewModel.paymentMethodLabel(PaymentMethod.fromCode(adv.paymentMethod), strings)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenWorker(adv.workerId) },
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(wName, style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        StatusPill(adv.receiptNumber, extra.goldContainer, extra.gold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "${IndiaFormatters.formatDateKey(adv.dateKey)} • $pMethod" +
                                            if (adv.referenceNumber.isNotBlank()) " • ${adv.referenceNumber}" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = extra.textMuted
                                    )
                                    if (adv.description.isNotBlank()) {
                                        Text(adv.description, style = MaterialTheme.typography.bodySmall, color = extra.textMuted)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        IndiaFormatters.formatInr(adv.amountPaise),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = extra.gold
                                    )
                                    Row {
                                        IconButton(onClick = { onViewAdvanceReceipt(adv) }) {
                                            Icon(Icons.Default.Receipt, contentDescription = strings.viewReceipt, tint = extra.gold)
                                        }
                                        IconButton(onClick = { pendingDeleteAdvance = adv }) {
                                            Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = extra.crimson)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    pendingDeletePayment?.let { target ->
        ConfirmActionDialog(
            title = strings.delete,
            message = strings.deletePaymentConfirm,
            confirmText = strings.delete,
            onConfirm = {
                onDeletePaymentConfirmed(target.id)
                pendingDeletePayment = null
            },
            onDismiss = { pendingDeletePayment = null }
        )
    }

    pendingDeleteAdvance?.let { target ->
        ConfirmActionDialog(
            title = strings.delete,
            message = strings.deleteAdvanceConfirm,
            confirmText = strings.delete,
            onConfirm = {
                onDeleteAdvanceConfirmed(target.id)
                pendingDeleteAdvance = null
            },
            onDismiss = { pendingDeleteAdvance = null }
        )
    }
}
