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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.WorkHistory
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
import com.example.data.entity.AttendanceEntity
import com.example.data.entity.WorkEntryEntity
import com.example.data.entity.WorkerEntity
import com.example.domain.AttendanceStatus
import com.example.domain.LedgerDateFilter
import com.example.domain.WorkType
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.LocalHisabColors
import com.example.ui.viewmodel.HisabViewModel
import com.example.util.IndiaFormatters

@Composable
fun WorkRegisterScreen(
    workers: List<WorkerEntity>,
    workEntries: List<WorkEntryEntity>,
    attendanceRecords: List<AttendanceEntity>,
    initialSubTab: Int = 0,
    onAddWorkClick: () -> Unit,
    onEditWorkClick: (WorkEntryEntity) -> Unit,
    onDeleteWorkConfirmed: (Long) -> Unit,
    onMarkAttendance: (Long, Long, AttendanceStatus, Double, String) -> Unit,
    onClearAttendance: (Long, Long) -> Unit
) {
    val strings = LocalStrings.current
    var subTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }

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
                selected = subTab == 0,
                onClick = { subTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = { Icon(Icons.Default.WorkHistory, contentDescription = null) },
                modifier = Modifier.testTag("seg_work_register")
            ) {
                Text(strings.workRegisterTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            SegmentedButton(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = { Icon(Icons.Default.EventAvailable, contentDescription = null) },
                modifier = Modifier.testTag("seg_attendance")
            ) {
                Text(strings.attendanceTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        if (subTab == 0) {
            DailyWorkListContent(
                workers = workers,
                workEntries = workEntries,
                onAddWorkClick = onAddWorkClick,
                onEditWorkClick = onEditWorkClick,
                onDeleteWorkConfirmed = onDeleteWorkConfirmed
            )
        } else {
            AttendanceScreen(
                workers = workers,
                attendanceRecords = attendanceRecords,
                onMarkAttendance = onMarkAttendance,
                onClearAttendance = onClearAttendance
            )
        }
    }
}

@Composable
private fun DailyWorkListContent(
    workers: List<WorkerEntity>,
    workEntries: List<WorkEntryEntity>,
    onAddWorkClick: () -> Unit,
    onEditWorkClick: (WorkEntryEntity) -> Unit,
    onDeleteWorkConfirmed: (Long) -> Unit
) {
    val strings = LocalStrings.current
    val extra = LocalHisabColors.current
    val workerMap = remember(workers) { workers.associateBy { it.id } }

    var dateFilter by remember { mutableStateOf(LedgerDateFilter.THIS_MONTH) }
    var selectedType by remember { mutableStateOf<WorkType?>(null) }
    var selectedWorkerId by remember { mutableStateOf<Long?>(null) }
    var pendingDelete by remember { mutableStateOf<WorkEntryEntity?>(null) }

    val todayKey = IndiaFormatters.todayDateKey()
    val filteredEntries = remember(workEntries, dateFilter, selectedType, selectedWorkerId, todayKey) {
        val (startKey, endKey) = when (dateFilter) {
            LedgerDateFilter.TODAY -> todayKey to todayKey
            LedgerDateFilter.THIS_WEEK -> IndiaFormatters.startOfWeekDateKey(todayKey) to todayKey
            LedgerDateFilter.THIS_MONTH -> IndiaFormatters.startOfMonthDateKey(todayKey) to IndiaFormatters.endOfMonthDateKey(todayKey)
            else -> 19700101L to 29991231L
        }
        workEntries.filter { e ->
            e.dateKey in startKey..endKey &&
                (selectedType == null || e.workType == selectedType!!.code) &&
                (selectedWorkerId == null || e.workerId == selectedWorkerId)
        }
    }

    val totalAmount = remember(filteredEntries) { filteredEntries.sumOf { it.totalAmountPaise } }

    Column(modifier = Modifier.fillMaxSize()) {
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
                listOf(
                    LedgerDateFilter.TODAY to strings.dateFilterToday,
                    LedgerDateFilter.THIS_WEEK to strings.dateFilterWeek,
                    LedgerDateFilter.THIS_MONTH to strings.dateFilterMonth,
                    LedgerDateFilter.ALL_TIME to strings.dateFilterAll
                ).forEach { (f, label) ->
                    FilterChip(
                        selected = dateFilter == f,
                        onClick = { dateFilter = f },
                        label = { Text(label) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { selectedType = null },
                    label = { Text(strings.allCategories) }
                )
                WorkType.entries.forEach { wt ->
                    FilterChip(
                        selected = selectedType == wt,
                        onClick = { selectedType = if (selectedType == wt) null else wt },
                        label = { Text(HisabViewModel.workTypeLabel(wt, strings)) }
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

            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = extra.emeraldContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${filteredEntries.size} ${strings.entriesCount}",
                        style = MaterialTheme.typography.titleSmall,
                        color = extra.emerald
                    )
                    Text(
                        IndiaFormatters.formatInr(totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = extra.emerald
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredEntries.isEmpty()) {
            Column(modifier = Modifier.padding(16.dp)) {
                EmptyStateCard(
                    icon = Icons.Default.WorkHistory,
                    title = strings.noWorkEntries,
                    subtitle = "",
                    actionLabel = if (workers.isNotEmpty()) strings.addWork else null,
                    onAction = if (workers.isNotEmpty()) onAddWorkClick else null
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredEntries, key = { it.id }) { entry ->
                    val workerName = workerMap[entry.workerId]?.fullName ?: "Worker"
                    val typeLabel = HisabViewModel.workTypeLabel(WorkType.fromCode(entry.workType), strings)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditWorkClick(entry) },
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
                                    Text(workerName, style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    StatusPill(typeLabel, extra.emeraldContainer, extra.emerald)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    buildString {
                                        append(IndiaFormatters.formatDateKey(entry.dateKey))
                                        if (entry.startTime.isNotBlank() && entry.endTime.isNotBlank()) {
                                            append(" • ${entry.startTime}-${entry.endTime}")
                                        }
                                        if (entry.totalHours > 0) append(" (${entry.totalHours}h)")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extra.textMuted
                                )
                                if (entry.description.isNotBlank() || entry.notes.isNotBlank()) {
                                    Text(
                                        listOfNotNull(
                                            entry.description.takeIf { it.isNotBlank() },
                                            entry.notes.takeIf { it.isNotBlank() }
                                        ).joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = extra.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    IndiaFormatters.formatInr(entry.totalAmountPaise),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = extra.emerald
                                )
                                Row {
                                    IconButton(onClick = { onEditWorkClick(entry) }) {
                                        Icon(Icons.Default.Edit, contentDescription = strings.edit)
                                    }
                                    IconButton(onClick = { pendingDelete = entry }) {
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

    pendingDelete?.let { target ->
        ConfirmActionDialog(
            title = strings.delete,
            message = strings.deleteWorkConfirm,
            confirmText = strings.delete,
            onConfirm = {
                onDeleteWorkConfirmed(target.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}
