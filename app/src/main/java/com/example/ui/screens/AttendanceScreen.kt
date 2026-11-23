package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.entity.AttendanceEntity
import com.example.data.entity.WorkerEntity
import com.example.domain.AttendanceStatus
import com.example.ui.components.DateInputRow
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.WorkerAvatar
import com.example.ui.components.WorkerDropdownSelector
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.LocalHisabColors
import com.example.ui.theme.NavyPrimary
import com.example.ui.viewmodel.HisabViewModel
import com.example.util.IndiaFormatters
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttendanceScreen(
    workers: List<WorkerEntity>,
    attendanceRecords: List<AttendanceEntity>,
    onMarkAttendance: (Long, Long, AttendanceStatus, Double, String) -> Unit,
    onClearAttendance: (Long, Long) -> Unit
) {
    val strings = LocalStrings.current
    val extra = LocalHisabColors.current
    var subTab by remember { mutableIntStateOf(0) }

    var selectedDateText by remember { mutableStateOf(IndiaFormatters.formatDateKey(IndiaFormatters.todayDateKey())) }
    val selectedDateKey = IndiaFormatters.parseDdMmYyyyToDateKey(selectedDateText) ?: IndiaFormatters.todayDateKey()

    var customHoursTargetWorker by remember { mutableStateOf<WorkerEntity?>(null) }
    var customHoursInput by remember { mutableStateOf("4.0") }
    var customNotesInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(selectedTabIndex = subTab) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = { Text(strings.dailyAttendance) }
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = { Text(strings.monthlyCalendar) }
            )
        }

        if (workers.isEmpty()) {
            Column(modifier = Modifier.padding(16.dp)) {
                EmptyStateCard(
                    icon = Icons.Default.EventAvailable,
                    title = strings.noWorkersTitle,
                    subtitle = strings.noWorkersSubtitle
                )
            }
            return
        }

        if (subTab == 0) {
            val dateRecords = remember(attendanceRecords, selectedDateKey) {
                attendanceRecords.filter { it.dateKey == selectedDateKey }.associateBy { it.workerId }
            }
            val presentCount = dateRecords.values.count {
                it.status in setOf(
                    AttendanceStatus.PRESENT.code,
                    AttendanceStatus.FULL_DAY.code,
                    AttendanceStatus.HALF_DAY.code,
                    AttendanceStatus.CUSTOM_HOURS.code
                )
            }
            val absentCount = dateRecords.values.count { it.status == AttendanceStatus.ABSENT.code }
            val leaveCount = dateRecords.values.count { it.status == AttendanceStatus.LEAVE.code }
            val unmarkedCount = (workers.size - dateRecords.size).coerceAtLeast(0)

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    DateInputRow(
                        label = strings.workDate.removeSuffix(" *"),
                        dateText = selectedDateText,
                        onDateTextChange = { selectedDateText = it }
                    )
                }

                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, extra.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SummaryPillCount(strings.attendancePresent, presentCount, extra.emerald)
                            SummaryPillCount(strings.attendanceAbsent, absentCount, extra.crimson)
                            SummaryPillCount(strings.attendanceLeave, leaveCount, extra.gold)
                            SummaryPillCount(strings.attendanceUnmarked, unmarkedCount, extra.textMuted)
                        }
                    }
                }

                items(workers, key = { "att_daily_${it.id}" }) { worker ->
                    val currentRecord = dateRecords[worker.id]
                    val currentStatus = currentRecord?.let { AttendanceStatus.fromCode(it.status) }

                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, extra.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    WorkerAvatar(name = worker.fullName, photoPath = worker.photoUri, size = 40.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(worker.fullName, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${worker.workerCode} • ${worker.workCategory}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = extra.textMuted
                                        )
                                    }
                                }
                                StatusPill(
                                    text = if (currentStatus == null) {
                                        strings.attendanceUnmarked
                                    } else {
                                        HisabViewModel.attendanceLabel(currentStatus, strings) +
                                            if (currentRecord.hours > 0) " (${currentRecord.hours}h)" else ""
                                    },
                                    containerColor = when (currentStatus) {
                                        AttendanceStatus.ABSENT -> extra.crimsonContainer
                                        AttendanceStatus.LEAVE, AttendanceStatus.HALF_DAY -> extra.goldContainer
                                        null -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> extra.emeraldContainer
                                    },
                                    contentColor = when (currentStatus) {
                                        AttendanceStatus.ABSENT -> extra.crimson
                                        AttendanceStatus.LEAVE, AttendanceStatus.HALF_DAY -> extra.gold
                                        null -> extra.textMuted
                                        else -> extra.emerald
                                    }
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AttendanceStatus.entries.forEach { st ->
                                    val isSelected = currentStatus == st
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (isSelected) {
                                                onClearAttendance(worker.id, selectedDateKey)
                                            } else if (st == AttendanceStatus.CUSTOM_HOURS) {
                                                customHoursTargetWorker = worker
                                                customHoursInput = currentRecord?.hours?.takeIf { it > 0 }?.toString() ?: "4.0"
                                                customNotesInput = currentRecord?.notes ?: ""
                                            } else {
                                                val defaultHrs = when (st) {
                                                    AttendanceStatus.HALF_DAY -> 4.0
                                                    AttendanceStatus.FULL_DAY, AttendanceStatus.PRESENT -> 8.0
                                                    else -> 0.0
                                                }
                                                onMarkAttendance(worker.id, selectedDateKey, st, defaultHrs, "")
                                            }
                                        },
                                        label = { Text(HisabViewModel.attendanceLabel(st, strings)) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = when (st) {
                                                AttendanceStatus.ABSENT -> extra.crimsonContainer
                                                AttendanceStatus.LEAVE, AttendanceStatus.HALF_DAY -> extra.goldContainer
                                                else -> extra.emeraldContainer
                                            }
                                        ),
                                        modifier = Modifier.testTag("att_${worker.id}_${st.code}")
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        } else {
            MonthlyAttendanceCalendarView(
                workers = workers,
                attendanceRecords = attendanceRecords,
                onMarkAttendance = onMarkAttendance
            )
        }
    }

    customHoursTargetWorker?.let { target ->
        AlertDialog(
            onDismissRequest = { customHoursTargetWorker = null },
            title = { Text("${target.fullName} • ${strings.attendanceCustomHours}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customHoursInput,
                        onValueChange = { customHoursInput = it },
                        label = { Text(strings.totalHours) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customNotesInput,
                        onValueChange = { customNotesInput = it },
                        label = { Text(strings.notesLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hrs = customHoursInput.toDoubleOrNull()?.coerceAtLeast(0.5) ?: 4.0
                        onMarkAttendance(
                            target.id,
                            selectedDateKey,
                            AttendanceStatus.CUSTOM_HOURS,
                            hrs,
                            customNotesInput.trim()
                        )
                        customHoursTargetWorker = null
                    }
                ) { Text(strings.save) }
            },
            dismissButton = {
                OutlinedButton(onClick = { customHoursTargetWorker = null }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun MonthlyAttendanceCalendarView(
    workers: List<WorkerEntity>,
    attendanceRecords: List<AttendanceEntity>,
    onMarkAttendance: (Long, Long, AttendanceStatus, Double, String) -> Unit
) {
    val strings = LocalStrings.current
    val extra = LocalHisabColors.current
    var selectedWorkerId by remember(workers) { mutableStateOf(workers.firstOrNull()?.id) }
    var monthOffset by remember { mutableIntStateOf(0) }

    val (monthStartKey, monthEndKey) = remember(monthOffset) {
        IndiaFormatters.monthRangeForOffset(monthOffset)
    }
    val ym = IndiaFormatters.yearMonthKey(monthStartKey)
    val startCal = remember(monthStartKey) { IndiaFormatters.dateKeyToCalendar(monthStartKey) }
    val daysInMonth = remember(monthStartKey) { startCal.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val firstDayOffset = remember(monthStartKey) {
        val dow = startCal.get(Calendar.DAY_OF_WEEK) // Sun=1..Sat=7
        if (dow == Calendar.SUNDAY) 6 else dow - 2 // Mon=0..Sun=6
    }

    val workerRecords = remember(attendanceRecords, selectedWorkerId, monthStartKey, monthEndKey) {
        attendanceRecords
            .filter { it.workerId == selectedWorkerId && it.dateKey in monthStartKey..monthEndKey }
            .associateBy { it.dateKey }
    }

    val presentDays = workerRecords.values.count {
        it.status == AttendanceStatus.PRESENT.code || it.status == AttendanceStatus.FULL_DAY.code
    }
    val halfDays = workerRecords.values.count { it.status == AttendanceStatus.HALF_DAY.code }
    val absentDays = workerRecords.values.count { it.status == AttendanceStatus.ABSENT.code }
    val leaveDays = workerRecords.values.count { it.status == AttendanceStatus.LEAVE.code }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            WorkerDropdownSelector(
                workers = workers,
                selectedWorkerId = selectedWorkerId,
                label = strings.selectWorker.removeSuffix(" *"),
                onWorkerSelected = { selectedWorkerId = it.id }
            )
        }

        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, extra.cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { monthOffset++ }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
                    }
                    Text(
                        text = IndiaFormatters.monthShortLabel(ym, strings.language == AppLanguage.HINDI),
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = { if (monthOffset > 0) monthOffset-- }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
                    }
                }
            }
        }

        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, extra.cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryPillCount(strings.attendancePresent, presentDays, extra.emerald)
                    SummaryPillCount(strings.attendanceHalfDay, halfDays, extra.gold)
                    SummaryPillCount(strings.attendanceAbsent, absentDays, extra.crimson)
                    SummaryPillCount(strings.attendanceLeave, leaveDays, NavyPrimary)
                }
            }
        }

        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, extra.cardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val dayHeaders = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        dayHeaders.forEach { d ->
                            Text(
                                text = d,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = extra.textMuted
                            )
                        }
                    }

                    val totalCells = firstDayOffset + daysInMonth
                    val rows = (totalCells + 6) / 7
                    for (r in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (c in 0 until 7) {
                                val cellIndex = r * 7 + c
                                val dayNum = cellIndex - firstDayOffset + 1
                                if (dayNum in 1..daysInMonth) {
                                    val dayKey = ym * 100L + dayNum
                                    val rec = workerRecords[dayKey]
                                    val st = rec?.let { AttendanceStatus.fromCode(it.status) }
                                    val (bg, fg) = when (st) {
                                        AttendanceStatus.PRESENT, AttendanceStatus.FULL_DAY -> extra.emeraldContainer to extra.emerald
                                        AttendanceStatus.HALF_DAY, AttendanceStatus.CUSTOM_HOURS -> extra.goldContainer to extra.gold
                                        AttendanceStatus.ABSENT -> extra.crimsonContainer to extra.crimson
                                        AttendanceStatus.LEAVE -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.primary
                                        null -> Color.Transparent to MaterialTheme.colorScheme.onSurface
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(bg)
                                            .border(
                                                width = 1.dp,
                                                color = if (st == null) extra.cardBorder else fg.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                val wid = selectedWorkerId ?: return@clickable
                                                val nextStatus = when (st) {
                                                    null -> AttendanceStatus.PRESENT
                                                    AttendanceStatus.PRESENT -> AttendanceStatus.HALF_DAY
                                                    AttendanceStatus.HALF_DAY -> AttendanceStatus.ABSENT
                                                    AttendanceStatus.ABSENT -> AttendanceStatus.LEAVE
                                                    else -> AttendanceStatus.PRESENT
                                                }
                                                val hrs = if (nextStatus == AttendanceStatus.HALF_DAY) 4.0 else if (nextStatus == AttendanceStatus.PRESENT) 8.0 else 0.0
                                                onMarkAttendance(wid, dayKey, nextStatus, hrs, "")
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = fg,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun SummaryPillCount(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = LocalHisabColors.current.textMuted)
    }
}
