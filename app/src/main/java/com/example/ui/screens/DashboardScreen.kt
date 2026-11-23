package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.entity.WorkEntryEntity
import com.example.data.entity.WorkerEntity
import com.example.data.prefs.AppSettings
import com.example.domain.DashboardSummary
import com.example.domain.TransactionCategory
import com.example.domain.UnifiedTransaction
import com.example.domain.WorkType
import com.example.domain.WorkerLedgerSummary
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MonthlyEarningsBarChart
import com.example.ui.components.PaymentOverviewDonutChart
import com.example.ui.components.StatusPill
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.GoldBright
import com.example.ui.theme.LocalHisabColors
import com.example.ui.theme.NavyContainer
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.HisabViewModel
import com.example.util.IndiaFormatters
import java.io.File

@Composable
fun DashboardScreen(
    settings: AppSettings,
    summary: DashboardSummary,
    workers: List<WorkerEntity>,
    todayWorkEntries: List<WorkEntryEntity>,
    recentTransactions: List<UnifiedTransaction>,
    onQuickAttendance: () -> Unit,
    onQuickAddWork: () -> Unit,
    onQuickGiveAdvance: () -> Unit,
    onQuickRecordPayment: () -> Unit,
    onQuickAddWorker: () -> Unit,
    onOpenWorker: (Long) -> Unit,
    onOpenWorkTab: () -> Unit,
    onOpenPaymentsTab: () -> Unit
) {
    val strings = LocalStrings.current
    val extra = LocalHisabColors.current
    val workerNameMap = remember(workers) { workers.associate { it.id to it.fullName } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Navy Balance Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = NavyDeep)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(NavyDeep, NavyPrimary, NavyContainer)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.totalOutstandingBalance,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = GoldBright
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = IndiaFormatters.formatInr(summary.totalOutstandingBalancePaise),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White,
                                    modifier = Modifier.testTag("dashboard_outstanding_balance")
                                )
                            }
                            if (!settings.businessLogoPath.isNullOrBlank() && File(settings.businessLogoPath).exists()) {
                                AsyncImage(
                                    model = File(settings.businessLogoPath),
                                    contentDescription = settings.businessName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(GoldBright.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "₹",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = GoldBright,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HeroMiniStat(
                                label = strings.totalWorkers,
                                value = summary.totalWorkers.toString(),
                                icon = Icons.Default.Groups,
                                modifier = Modifier.weight(1f)
                            )
                            HeroMiniStat(
                                label = strings.todayPresent,
                                value = "${summary.todayPresentWorkers}/${summary.activeWorkers}",
                                icon = Icons.Default.EventAvailable,
                                modifier = Modifier.weight(1f)
                            )
                            HeroMiniStat(
                                label = strings.todayWork,
                                value = IndiaFormatters.formatInrCompact(summary.todayWorkAmountPaise),
                                icon = Icons.Default.WorkHistory,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Quick Action Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.quickActions,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        icon = Icons.Default.EventAvailable,
                        label = strings.markAttendance,
                        tint = NavyPrimary,
                        bg = MaterialTheme.colorScheme.surfaceVariant,
                        tag = "dash_qa_attendance",
                        onClick = onQuickAttendance
                    )
                    QuickActionTile(
                        icon = Icons.Default.WorkHistory,
                        label = strings.addWork,
                        tint = extra.emerald,
                        bg = extra.emeraldContainer,
                        tag = "dash_qa_work",
                        onClick = onQuickAddWork
                    )
                    QuickActionTile(
                        icon = Icons.Default.AccountBalanceWallet,
                        label = strings.giveAdvance,
                        tint = extra.gold,
                        bg = extra.goldContainer,
                        tag = "dash_qa_advance",
                        onClick = onQuickGiveAdvance
                    )
                    QuickActionTile(
                        icon = Icons.Default.Payments,
                        label = strings.recordPayment,
                        tint = extra.emerald,
                        bg = extra.emeraldContainer,
                        tag = "dash_qa_payment",
                        onClick = onQuickRecordPayment
                    )
                    QuickActionTile(
                        icon = Icons.Default.PersonAdd,
                        label = strings.addWorker,
                        tint = extra.gold,
                        bg = extra.goldContainer,
                        tag = "dash_qa_worker",
                        onClick = onQuickAddWorker
                    )
                }
            }
        }

        // 4 Financial Metric Cards (2x2 Grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FinancialKpiCard(
                        title = strings.totalEarnings,
                        value = IndiaFormatters.formatInr(summary.totalEarningsPaise, showDecimals = false),
                        icon = Icons.Default.TrendingUp,
                        accent = extra.emerald,
                        container = extra.emeraldContainer,
                        modifier = Modifier.weight(1f)
                    )
                    FinancialKpiCard(
                        title = strings.totalAdvanceGiven,
                        value = IndiaFormatters.formatInr(summary.totalAdvanceGivenPaise, showDecimals = false),
                        icon = Icons.Default.AccountBalanceWallet,
                        accent = extra.gold,
                        container = extra.goldContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FinancialKpiCard(
                        title = strings.totalPaymentsMade,
                        value = IndiaFormatters.formatInr(summary.totalPaymentsMadePaise, showDecimals = false),
                        icon = Icons.Default.Payments,
                        accent = extra.emerald,
                        container = extra.emeraldContainer,
                        modifier = Modifier.weight(1f)
                    )
                    FinancialKpiCard(
                        title = strings.monthlyExpenses,
                        value = IndiaFormatters.formatInr(summary.monthlyExpensesPaise, showDecimals = false),
                        icon = Icons.Default.CalendarMonth,
                        accent = NavyPrimary,
                        container = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Monthly Earnings Graph
        item {
            MonthlyEarningsBarChart(points = summary.monthlySeries)
        }

        // Payment Overview Donut Chart
        item {
            PaymentOverviewDonutChart(
                totalEarningsPaise = summary.totalEarningsPaise,
                totalAdvancePaise = summary.totalAdvanceGivenPaise,
                totalPaidPaise = summary.totalPaymentsMadePaise,
                totalBalancePaise = summary.totalOutstandingBalancePaise
            )
        }

        // Today's Work Summary
        item {
            SectionHeaderRow(
                title = "${strings.todaysWorkSummary} (${summary.todayWorkCount})",
                actionLabel = strings.viewAll,
                onAction = onOpenWorkTab
            )
        }

        if (todayWorkEntries.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.WorkHistory,
                    title = strings.noWorkToday,
                    subtitle = "",
                    actionLabel = if (workers.isNotEmpty()) strings.addWork else strings.addWorker,
                    onAction = if (workers.isNotEmpty()) onQuickAddWork else onQuickAddWorker
                )
            }
        } else {
            items(todayWorkEntries.take(4), key = { "today_work_${it.id}" }) { work ->
                val wName = workerNameMap[work.workerId] ?: "Worker"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenWorker(work.workerId) },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, extra.cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(wName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = buildString {
                                    append(HisabViewModel.workTypeLabel(WorkType.fromCode(work.workType), strings))
                                    if (work.totalHours > 0) append(" • ${work.totalHours}h")
                                    if (work.description.isNotBlank()) append(" • ${work.description}")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = extra.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = IndiaFormatters.formatInr(work.totalAmountPaise),
                            style = MaterialTheme.typography.titleMedium,
                            color = extra.emerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Recent Transactions
        item {
            SectionHeaderRow(
                title = strings.recentTransactions,
                actionLabel = strings.viewAll,
                onAction = onOpenPaymentsTab
            )
        }

        if (recentTransactions.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.ReceiptLong,
                    title = strings.noTransactionsYet,
                    subtitle = ""
                )
            }
        } else {
            items(recentTransactions.take(6), key = { "${it.category.name}_${it.id}" }) { tx ->
                TransactionRowCard(tx = tx, onClick = { onOpenWorker(tx.workerId) })
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun HeroMiniStat(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.09f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    tint: Color,
    bg: Color,
    tag: String,
    onClick: () -> Unit
) {
    val extra = LocalHisabColors.current
    Surface(
        modifier = Modifier
            .width(96.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extra.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FinancialKpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    container: Color,
    modifier: Modifier = Modifier
) {
    val extra = LocalHisabColors.current
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extra.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = extra.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(container),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionHeaderRow(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        TextButton(onClick = onAction) {
            Text(actionLabel)
        }
    }
}

@Composable
fun TransactionRowCard(tx: UnifiedTransaction, onClick: () -> Unit) {
    val strings = LocalStrings.current
    val extra = LocalHisabColors.current
    val (badgeLabel, badgeBg, badgeFg, icon) = when (tx.category) {
        TransactionCategory.WORK -> Quad(
            strings.addWork,
            extra.emeraldContainer,
            extra.emerald,
            Icons.Default.ArrowUpward
        )
        TransactionCategory.ADVANCE -> Quad(
            strings.tabAdvances,
            extra.goldContainer,
            extra.gold,
            Icons.Default.ArrowDownward
        )
        TransactionCategory.PAYMENT -> Quad(
            strings.tabPayments,
            extra.emeraldContainer,
            extra.emerald,
            Icons.Default.Payments
        )
        TransactionCategory.ADVANCE_ADJUSTMENT -> Quad(
            strings.typeAdvanceAdjustment,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.primary,
            Icons.Default.AccountBalanceWallet
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = badgeFg, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.workerName, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${IndiaFormatters.formatDateKey(tx.dateKey)} • ${tx.subtitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = extra.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = IndiaFormatters.formatInr(tx.amountPaise),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeFg
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusPill(text = badgeLabel, containerColor = badgeBg, contentColor = badgeFg)
            }
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
fun GlobalSearchDialog(
    workers: List<WorkerEntity>,
    ledgers: Map<Long, WorkerLedgerSummary>,
    transactions: List<UnifiedTransaction>,
    onSelectWorker: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val extra = LocalHisabColors.current
    var query by remember { mutableStateOf("") }

    val matchedWorkers = remember(query, workers) {
        val q = query.trim()
        if (q.isEmpty()) workers.take(5)
        else workers.filter {
            it.fullName.contains(q, ignoreCase = true) ||
                it.mobileNumber.contains(q, ignoreCase = true) ||
                it.workerCode.contains(q, ignoreCase = true) ||
                it.workCategory.contains(q, ignoreCase = true)
        }
    }
    val matchedTx = remember(query, transactions) {
        val q = query.trim()
        if (q.isEmpty()) emptyList()
        else transactions.filter {
            it.workerName.contains(q, ignoreCase = true) ||
                it.subtitle.contains(q, ignoreCase = true) ||
                it.reference.contains(q, ignoreCase = true)
        }.take(8)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.search, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = strings.close)
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(strings.searchWorkersHint) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("global_search_input")
                )
                LazyColumn(
                    modifier = Modifier.height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(matchedWorkers, key = { "gs_w_${it.id}" }) { w ->
                        val bal = ledgers[w.id]?.remainingBalancePaise ?: 0L
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable {
                                    onDismiss()
                                    onSelectWorker(w.id)
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${w.fullName} (${w.workerCode})", style = MaterialTheme.typography.titleSmall)
                                    Text("${w.mobileNumber} • ${w.workCategory}", style = MaterialTheme.typography.bodySmall, color = extra.textMuted)
                                }
                                Text(
                                    IndiaFormatters.formatInr(bal),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (bal > 0L) extra.crimson else extra.emerald
                                )
                            }
                        }
                    }
                    items(matchedTx, key = { "gs_tx_${it.category.name}_${it.id}" }) { tx ->
                        TransactionRowCard(tx = tx, onClick = {
                            onDismiss()
                            onSelectWorker(tx.workerId)
                        })
                    }
                }
            }
        }
    }
}
