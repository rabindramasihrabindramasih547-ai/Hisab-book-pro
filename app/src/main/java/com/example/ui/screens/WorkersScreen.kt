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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.entity.WorkerEntity
import com.example.domain.WorkerLedgerSummary
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.WorkerAvatar
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.LocalHisabColors
import com.example.util.IndiaFormatters

@Composable
fun WorkersScreen(
    workers: List<WorkerEntity>,
    ledgers: Map<Long, WorkerLedgerSummary>,
    onWorkerClick: (Long) -> Unit,
    onAddWorkerClick: () -> Unit,
    onEditWorkerClick: (WorkerEntity) -> Unit,
    onDeleteWorkerConfirmed: (Long) -> Unit
) {
    val strings = LocalStrings.current
    val extra = LocalHisabColors.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var onlyBalanceDue by remember { mutableStateOf(false) }
    var workerPendingDelete by remember { mutableStateOf<WorkerEntity?>(null) }

    val categories = remember(workers) {
        workers.map { it.workCategory }.distinct().sorted()
    }

    val filteredWorkers = remember(workers, searchQuery, selectedCategory, onlyBalanceDue, ledgers) {
        val q = searchQuery.trim()
        workers.filter { w ->
            val matchesQuery = q.isEmpty() ||
                w.fullName.contains(q, ignoreCase = true) ||
                w.mobileNumber.contains(q, ignoreCase = true) ||
                w.workerCode.contains(q, ignoreCase = true)
            val matchesCat = selectedCategory == null || w.workCategory.equals(selectedCategory, ignoreCase = true)
            val bal = ledgers[w.id]?.remainingBalancePaise ?: 0L
            val matchesDue = !onlyBalanceDue || bal > 0L
            matchesQuery && matchesCat && matchesDue
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchWorkersHint) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("workers_search_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null && !onlyBalanceDue,
                    onClick = { selectedCategory = null; onlyBalanceDue = false },
                    label = { Text(strings.allCategories) }
                )
                FilterChip(
                    selected = onlyBalanceDue,
                    onClick = { onlyBalanceDue = !onlyBalanceDue },
                    label = { Text(strings.filterBalanceDue) }
                )
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat) }
                    )
                }
            }
        }

        if (filteredWorkers.isEmpty()) {
            Column(modifier = Modifier.padding(16.dp)) {
                EmptyStateCard(
                    icon = Icons.Default.Groups,
                    title = strings.noWorkersTitle,
                    subtitle = strings.noWorkersSubtitle,
                    actionLabel = strings.addWorker,
                    onAction = onAddWorkerClick
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredWorkers, key = { it.id }) { worker ->
                    val ledger = ledgers[worker.id]
                    val balance = ledger?.remainingBalancePaise ?: 0L
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onWorkerClick(worker.id) }
                            .testTag("worker_card_${worker.id}"),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, extra.cardBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WorkerAvatar(name = worker.fullName, photoPath = worker.photoUri, size = 50.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = worker.fullName,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        StatusPill(
                                            text = worker.workerCode,
                                            containerColor = extra.goldContainer,
                                            contentColor = extra.gold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Phone,
                                            contentDescription = null,
                                            tint = extra.textMuted,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${worker.mobileNumber} • ${worker.workCategory}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = extra.textMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(onClick = { onEditWorkerClick(worker) }) {
                                    Icon(Icons.Default.Edit, contentDescription = strings.editWorker)
                                }
                                IconButton(onClick = { workerPendingDelete = worker }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = strings.deleteWorker,
                                        tint = extra.crimson
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = buildString {
                                        if (worker.fullTimeRatePaise > 0) append("Full: ${IndiaFormatters.formatInr(worker.fullTimeRatePaise, false)}  ")
                                        if (worker.halfTimeRatePaise > 0) append("Half: ${IndiaFormatters.formatInr(worker.halfTimeRatePaise, false)}  ")
                                        if (worker.hourlyRatePaise > 0) append("Hr: ${IndiaFormatters.formatInr(worker.hourlyRatePaise, false)}")
                                    }.ifBlank { "Daily: ${IndiaFormatters.formatInr(worker.dailyRatePaise, false)}" },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = extra.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (balance < 0L) strings.overpaidLabel else strings.remainingBalanceLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = extra.textMuted
                                    )
                                    Text(
                                        text = IndiaFormatters.formatInr(balance),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            balance > 0L -> extra.crimson
                                            balance < 0L -> extra.gold
                                            else -> extra.emerald
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    workerPendingDelete?.let { target ->
        ConfirmActionDialog(
            title = "${strings.deleteWorker}: ${target.fullName}",
            message = strings.deleteWorkerConfirm,
            confirmText = strings.delete,
            onConfirm = {
                onDeleteWorkerConfirmed(target.id)
                workerPendingDelete = null
            },
            onDismiss = { workerPendingDelete = null }
        )
    }
}
