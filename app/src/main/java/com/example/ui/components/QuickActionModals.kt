package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.AdvanceEntity
import com.example.data.entity.PaymentEntity
import com.example.data.entity.WorkEntryEntity
import com.example.data.entity.WorkerEntity
import com.example.domain.HisabCalculator
import com.example.domain.PaymentMethod
import com.example.domain.PaymentType
import com.example.domain.WorkType
import com.example.domain.WorkerLedgerSummary
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionChooserSheet(
    onDismiss: () -> Unit,
    onAddWork: () -> Unit,
    onMarkAttendance: () -> Unit,
    onGiveAdvance: () -> Unit,
    onRecordPayment: () -> Unit,
    onAddWorker: () -> Unit
) {
    val strings = LocalStrings.current
    val extra = LocalHisabColors.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = strings.quickActions,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            QuickSheetRow(
                icon = Icons.Default.WorkHistory,
                label = strings.addWork,
                tint = extra.emerald,
                container = extra.emeraldContainer,
                testTag = "quick_add_work",
                onClick = { onDismiss(); onAddWork() }
            )
            QuickSheetRow(
                icon = Icons.Default.EventAvailable,
                label = strings.markAttendance,
                tint = NavyPrimary,
                container = MaterialTheme.colorScheme.surfaceVariant,
                testTag = "quick_mark_attendance",
                onClick = { onDismiss(); onMarkAttendance() }
            )
            QuickSheetRow(
                icon = Icons.Default.AccountBalanceWallet,
                label = strings.giveAdvance,
                tint = extra.gold,
                container = extra.goldContainer,
                testTag = "quick_give_advance",
                onClick = { onDismiss(); onGiveAdvance() }
            )
            QuickSheetRow(
                icon = Icons.Default.Payments,
                label = strings.recordPayment,
                tint = extra.emerald,
                container = extra.emeraldContainer,
                testTag = "quick_record_payment",
                onClick = { onDismiss(); onRecordPayment() }
            )
            QuickSheetRow(
                icon = Icons.Default.PersonAdd,
                label = strings.addWorker,
                tint = extra.gold,
                container = extra.goldContainer,
                testTag = "quick_add_worker",
                onClick = { onDismiss(); onAddWorker() }
            )
        }
    }
}

@Composable
private fun QuickSheetRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    container: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(container),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditWorkerDialog(
    initialWorker: WorkerEntity? = null,
    onCopyPhoto: suspend (android.net.Uri, String) -> String?,
    onSave: (WorkerEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val extra = LocalHisabColors.current
    val scope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf(initialWorker?.fullName ?: "") }
    var mobile by remember { mutableStateOf(initialWorker?.mobileNumber ?: "") }
    var address by remember { mutableStateOf(initialWorker?.address ?: "") }
    var workerCode by remember { mutableStateOf(initialWorker?.workerCode ?: "") }
    var joiningDateText by remember {
        mutableStateOf(
            IndiaFormatters.formatDateKey(
                initialWorker?.joiningDateKey ?: IndiaFormatters.todayDateKey()
            )
        )
    }
    var category by remember { mutableStateOf(initialWorker?.workCategory ?: "Helper") }
    var dailyRate by remember { mutableStateOf(IndiaFormatters.paiseToEditableString(initialWorker?.dailyRatePaise ?: 0L)) }
    var halfTimeRate by remember { mutableStateOf(IndiaFormatters.paiseToEditableString(initialWorker?.halfTimeRatePaise ?: 0L)) }
    var fullTimeRate by remember { mutableStateOf(IndiaFormatters.paiseToEditableString(initialWorker?.fullTimeRatePaise ?: 0L)) }
    var hourlyRate by remember { mutableStateOf(IndiaFormatters.paiseToEditableString(initialWorker?.hourlyRatePaise ?: 0L)) }
    var photoPath by remember { mutableStateOf(initialWorker?.photoUri) }
    var notes by remember { mutableStateOf(initialWorker?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = onCopyPhoto(uri, "worker")
                if (saved != null) photoPath = saved
            }
        }
    }

    val presetCategories = listOf("Mason", "Helper", "Electrician", "Plumber", "Carpenter", "Driver", "Tailor")

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
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (initialWorker == null) strings.addWorker else strings.editWorker,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WorkerAvatar(name = fullName.ifBlank { "W" }, photoPath = photoPath, size = 56.dp)
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.pickPhoto)
                    }
                }

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; errorMsg = null },
                    label = { Text(strings.fullNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("worker_name_input")
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it; errorMsg = null },
                    label = { Text(strings.mobileLabel) },
                    placeholder = { Text("+91 9876543210") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("worker_mobile_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = workerCode,
                        onValueChange = { workerCode = it },
                        label = { Text(strings.workerIdLabel) },
                        placeholder = { Text("Auto (HB-001)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    DateInputRow(
                        label = strings.joiningDateLabel,
                        dateText = joiningDateText,
                        onDateTextChange = { joiningDateText = it; errorMsg = null },
                        modifier = Modifier.weight(1.3f)
                    )
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(strings.addressLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(strings.workCategoryLabel, style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presetCategories.forEach { cat ->
                        FilterChip(
                            selected = category.equals(cat, ignoreCase = true),
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(strings.workCategoryLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dailyRate,
                        onValueChange = { dailyRate = it },
                        label = { Text(strings.dailyRateLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("worker_daily_rate_input")
                    )
                    OutlinedTextField(
                        value = halfTimeRate,
                        onValueChange = { halfTimeRate = it },
                        label = { Text(strings.halfTimeRateLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("worker_halftime_rate_input")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fullTimeRate,
                        onValueChange = { fullTimeRate = it },
                        label = { Text(strings.fullTimeRateLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("worker_fulltime_rate_input")
                    )
                    OutlinedTextField(
                        value = hourlyRate,
                        onValueChange = { hourlyRate = it },
                        label = { Text(strings.hourlyRateLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("worker_hourly_rate_input")
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.notesLabel) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(errorMsg!!, color = extra.crimson, style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(strings.cancel) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val trimmedName = fullName.trim()
                            val joiningKey = IndiaFormatters.parseDdMmYyyyToDateKey(joiningDateText)
                            if (trimmedName.isEmpty()) {
                                errorMsg = strings.requiredFieldError
                                return@Button
                            }
                            if (!IndiaFormatters.isValidIndianMobile(mobile)) {
                                errorMsg = strings.invalidMobileError
                                return@Button
                            }
                            if (joiningKey == null) {
                                errorMsg = strings.invalidDateError
                                return@Button
                            }
                            val entity = WorkerEntity(
                                id = initialWorker?.id ?: 0L,
                                workerCode = workerCode.trim(),
                                fullName = trimmedName,
                                mobileNumber = IndiaFormatters.formatIndianMobile(mobile),
                                address = address.trim(),
                                joiningDateKey = joiningKey,
                                workCategory = category.trim().ifEmpty { "General" },
                                dailyRatePaise = IndiaFormatters.parseRupeesToPaise(dailyRate) ?: 0L,
                                halfTimeRatePaise = IndiaFormatters.parseRupeesToPaise(halfTimeRate) ?: 0L,
                                fullTimeRatePaise = IndiaFormatters.parseRupeesToPaise(fullTimeRate) ?: 0L,
                                hourlyRatePaise = IndiaFormatters.parseRupeesToPaise(hourlyRate) ?: 0L,
                                photoUri = photoPath,
                                notes = notes.trim(),
                                isActive = initialWorker?.isActive ?: true,
                                createdAt = initialWorker?.createdAt ?: System.currentTimeMillis()
                            )
                            onSave(entity)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier.testTag("save_worker_button")
                    ) {
                        Text(strings.save, color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditWorkDialog(
    workers: List<WorkerEntity>,
    preselectedWorkerId: Long? = null,
    initialEntry: WorkEntryEntity? = null,
    onSave: (WorkEntryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val extra = LocalHisabColors.current

    var selectedWorkerId by remember {
        mutableStateOf(initialEntry?.workerId ?: preselectedWorkerId ?: workers.firstOrNull()?.id)
    }
    var dateText by remember {
        mutableStateOf(
            IndiaFormatters.formatDateKey(initialEntry?.dateKey ?: IndiaFormatters.todayDateKey())
        )
    }
    var workType by remember {
        mutableStateOf(
            initialEntry?.let { WorkType.fromCode(it.workType) } ?: WorkType.FULL_TIME
        )
    }
    var startTime by remember { mutableStateOf(initialEntry?.startTime?.ifBlank { "09:00" } ?: "09:00") }
    var endTime by remember { mutableStateOf(initialEntry?.endTime?.ifBlank { "18:00" } ?: "18:00") }
    var hoursText by remember {
        mutableStateOf(
            if (initialEntry != null && initialEntry.totalHours > 0.0) {
                initialEntry.totalHours.toString()
            } else {
                "9.0"
            }
        )
    }

    val selectedWorker = workers.firstOrNull { it.id == selectedWorkerId }

    var rateText by remember {
        mutableStateOf(
            if (initialEntry != null) {
                IndiaFormatters.paiseToEditableString(initialEntry.ratePaise)
            } else {
                selectedWorker?.let {
                    IndiaFormatters.paiseToEditableString(
                        HisabCalculator.defaultRateForWorker(it, workType)
                    )
                } ?: ""
            }
        )
    }
    var manualCustomTotalText by remember {
        mutableStateOf(
            if (initialEntry != null) IndiaFormatters.paiseToEditableString(initialEntry.totalAmountPaise)
            else ""
        )
    }
    var description by remember { mutableStateOf(initialEntry?.description ?: "") }
    var notes by remember { mutableStateOf(initialEntry?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun applyDefaultRate(worker: WorkerEntity?, type: WorkType) {
        if (worker != null && type != WorkType.CUSTOM) {
            val def = HisabCalculator.defaultRateForWorker(worker, type)
            rateText = IndiaFormatters.paiseToEditableString(def)
        }
    }

    val computedHours = hoursText.toDoubleOrNull() ?: 0.0
    val parsedRate = IndiaFormatters.parseRupeesToPaise(rateText) ?: 0L
    val computedTotalPaise = if (workType == WorkType.CUSTOM && manualCustomTotalText.isNotBlank()) {
        IndiaFormatters.parseRupeesToPaise(manualCustomTotalText) ?: parsedRate
    } else {
        HisabCalculator.calculateWorkTotalPaise(workType, parsedRate, computedHours)
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
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (initialEntry == null) strings.addWork else strings.editWorkEntry,
                    style = MaterialTheme.typography.titleLarge
                )

                WorkerDropdownSelector(
                    workers = workers,
                    selectedWorkerId = selectedWorkerId,
                    label = strings.selectWorker,
                    onWorkerSelected = { w ->
                        selectedWorkerId = w.id
                        applyDefaultRate(w, workType)
                    }
                )

                DateInputRow(
                    label = strings.workDate,
                    dateText = dateText,
                    onDateTextChange = { dateText = it; errorMsg = null }
                )

                Text(strings.workTypeLabel, style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WorkType.entries.forEach { wt ->
                        FilterChip(
                            selected = workType == wt,
                            onClick = {
                                workType = wt
                                applyDefaultRate(selectedWorker, wt)
                            },
                            label = { Text(HisabViewModel.workTypeLabel(wt, strings)) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeInputField(
                        label = strings.startTime,
                        timeText = startTime,
                        onTimeChange = { newStart ->
                            startTime = newStart
                            IndiaFormatters.calculateHoursBetween(newStart, endTime)?.let {
                                hoursText = String.format(Locale.US, "%.2f", it).trimEnd('0').trimEnd('.')
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TimeInputField(
                        label = strings.endTime,
                        timeText = endTime,
                        onTimeChange = { newEnd ->
                            endTime = newEnd
                            IndiaFormatters.calculateHoursBetween(startTime, newEnd)?.let {
                                hoursText = String.format(Locale.US, "%.2f", it).trimEnd('0').trimEnd('.')
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { hoursText = it },
                        label = { Text(strings.totalHours) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it; errorMsg = null },
                        label = { Text(strings.workRate) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("work_rate_input")
                    )
                }

                if (workType == WorkType.CUSTOM) {
                    OutlinedTextField(
                        value = manualCustomTotalText,
                        onValueChange = { manualCustomTotalText = it },
                        label = { Text(strings.totalAmount) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Surface(
                    color = extra.emeraldContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.totalAmount, style = MaterialTheme.typography.titleSmall, color = extra.emerald)
                        Text(
                            IndiaFormatters.formatInr(computedTotalPaise),
                            style = MaterialTheme.typography.titleLarge,
                            color = extra.emerald
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(strings.descriptionLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.notesLabel) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(errorMsg!!, color = extra.crimson, style = MaterialTheme.typography.bodySmall)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(strings.cancel) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val wid = selectedWorkerId
                            val dateKey = IndiaFormatters.parseDdMmYyyyToDateKey(dateText)
                            if (wid == null) {
                                errorMsg = strings.selectWorker
                                return@Button
                            }
                            if (dateKey == null) {
                                errorMsg = strings.invalidDateError
                                return@Button
                            }
                            if (computedTotalPaise <= 0L) {
                                errorMsg = strings.invalidInputError
                                return@Button
                            }
                            val entry = WorkEntryEntity(
                                id = initialEntry?.id ?: 0L,
                                workerId = wid,
                                dateKey = dateKey,
                                workType = workType.code,
                                startTime = startTime.trim(),
                                endTime = endTime.trim(),
                                totalHours = computedHours,
                                ratePaise = if (parsedRate > 0L) parsedRate else computedTotalPaise,
                                totalAmountPaise = computedTotalPaise,
                                description = description.trim(),
                                notes = notes.trim(),
                                isApproved = true,
                                createdAt = initialEntry?.createdAt ?: System.currentTimeMillis()
                            )
                            onSave(entry)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier.testTag("save_work_button")
                    ) {
                        Text(strings.save, color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddAdvanceDialog(
    workers: List<WorkerEntity>,
    defaultMethod: PaymentMethod,
    preselectedWorkerId: Long? = null,
    onSave: (AdvanceEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val extra = LocalHisabColors.current

    var selectedWorkerId by remember { mutableStateOf(preselectedWorkerId ?: workers.firstOrNull()?.id) }
    var dateText by remember { mutableStateOf(IndiaFormatters.formatDateKey(IndiaFormatters.todayDateKey())) }
    var amountText by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(defaultMethod) }
    var refNo by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

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
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(strings.giveAdvance, style = MaterialTheme.typography.titleLarge)

                WorkerDropdownSelector(
                    workers = workers,
                    selectedWorkerId = selectedWorkerId,
                    label = strings.selectWorker,
                    onWorkerSelected = { selectedWorkerId = it.id }
                )

                DateInputRow(
                    label = strings.advanceDate,
                    dateText = dateText,
                    onDateTextChange = { dateText = it; errorMsg = null }
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; errorMsg = null },
                    label = { Text(strings.advanceAmount) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("advance_amount_input")
                )

                Text(strings.paymentMethod, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaymentMethod.entries.forEach { pm ->
                        FilterChip(
                            selected = method == pm,
                            onClick = { method = pm },
                            label = { Text(HisabViewModel.paymentMethodLabel(pm, strings)) }
                        )
                    }
                }

                OutlinedTextField(
                    value = refNo,
                    onValueChange = { refNo = it },
                    label = { Text(strings.referenceNumber) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(strings.descriptionLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.notesLabel) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(errorMsg!!, color = extra.crimson, style = MaterialTheme.typography.bodySmall)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(strings.cancel) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val wid = selectedWorkerId
                            val dateKey = IndiaFormatters.parseDdMmYyyyToDateKey(dateText)
                            val paise = IndiaFormatters.parseRupeesToPaise(amountText) ?: 0L
                            if (wid == null) {
                                errorMsg = strings.selectWorker
                                return@Button
                            }
                            if (dateKey == null) {
                                errorMsg = strings.invalidDateError
                                return@Button
                            }
                            if (paise <= 0L) {
                                errorMsg = strings.invalidInputError
                                return@Button
                            }
                            onSave(
                                AdvanceEntity(
                                    workerId = wid,
                                    dateKey = dateKey,
                                    amountPaise = paise,
                                    paymentMethod = method.code,
                                    referenceNumber = refNo.trim(),
                                    description = description.trim(),
                                    notes = notes.trim()
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier.testTag("save_advance_button")
                    ) {
                        Text(strings.save, color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddPaymentDialog(
    workers: List<WorkerEntity>,
    ledgers: Map<Long, WorkerLedgerSummary>,
    defaultMethod: PaymentMethod,
    preselectedWorkerId: Long? = null,
    onSave: (PaymentEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val extra = LocalHisabColors.current

    var selectedWorkerId by remember { mutableStateOf(preselectedWorkerId ?: workers.firstOrNull()?.id) }
    var dateText by remember { mutableStateOf(IndiaFormatters.formatDateKey(IndiaFormatters.todayDateKey())) }
    var paymentType by remember { mutableStateOf(PaymentType.PARTIAL_PAYMENT) }
    var amountText by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(defaultMethod) }
    var txId by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val ledger = selectedWorkerId?.let { ledgers[it] }

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
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(strings.recordPayment, style = MaterialTheme.typography.titleLarge)

                WorkerDropdownSelector(
                    workers = workers,
                    selectedWorkerId = selectedWorkerId,
                    label = strings.selectWorker,
                    onWorkerSelected = { selectedWorkerId = it.id }
                )

                if (ledger != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(strings.remainingBalanceLabel, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    IndiaFormatters.formatInr(ledger.remainingBalancePaise),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (ledger.remainingBalancePaise > 0L) extra.crimson else extra.emerald
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(strings.remainingAdvanceLabel, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    IndiaFormatters.formatInr(ledger.remainingAdvanceBalancePaise.coerceAtLeast(0L)),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = extra.gold
                                )
                            }
                        }
                    }
                }

                Text(strings.paymentTypeLabel, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaymentType.entries.forEach { pt ->
                        FilterChip(
                            selected = paymentType == pt,
                            onClick = {
                                paymentType = pt
                                if (ledger != null) {
                                    when (pt) {
                                        PaymentType.FULL_PAYMENT -> {
                                            if (ledger.remainingBalancePaise > 0L) {
                                                amountText = IndiaFormatters.paiseToEditableString(ledger.remainingBalancePaise)
                                            }
                                        }
                                        PaymentType.ADVANCE_ADJUSTMENT -> {
                                            if (ledger.remainingAdvanceBalancePaise > 0L) {
                                                amountText = IndiaFormatters.paiseToEditableString(ledger.remainingAdvanceBalancePaise)
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                            },
                            label = { Text(HisabViewModel.paymentTypeLabel(pt, strings)) }
                        )
                    }
                }

                DateInputRow(
                    label = strings.paymentDate,
                    dateText = dateText,
                    onDateTextChange = { dateText = it; errorMsg = null }
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; errorMsg = null },
                    label = { Text(strings.paymentAmount) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("payment_amount_input")
                )

                Text(strings.paymentMethod, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaymentMethod.entries.forEach { pm ->
                        FilterChip(
                            selected = method == pm,
                            onClick = { method = pm },
                            label = { Text(HisabViewModel.paymentMethodLabel(pm, strings)) }
                        )
                    }
                }

                OutlinedTextField(
                    value = txId,
                    onValueChange = { txId = it },
                    label = { Text(strings.transactionId) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.notesLabel) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(errorMsg!!, color = extra.crimson, style = MaterialTheme.typography.bodySmall)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(strings.cancel) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val wid = selectedWorkerId
                            val dateKey = IndiaFormatters.parseDdMmYyyyToDateKey(dateText)
                            val paise = IndiaFormatters.parseRupeesToPaise(amountText) ?: 0L
                            if (wid == null) {
                                errorMsg = strings.selectWorker
                                return@Button
                            }
                            if (dateKey == null) {
                                errorMsg = strings.invalidDateError
                                return@Button
                            }
                            if (paise <= 0L) {
                                errorMsg = strings.invalidInputError
                                return@Button
                            }
                            onSave(
                                PaymentEntity(
                                    workerId = wid,
                                    dateKey = dateKey,
                                    amountPaise = paise,
                                    paymentType = paymentType.code,
                                    paymentMethod = method.code,
                                    transactionId = txId.trim(),
                                    notes = notes.trim()
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier.testTag("save_payment_button")
                    ) {
                        Text(strings.save, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AdvanceReceiptDialog(
    advance: AdvanceEntity,
    workerName: String,
    businessName: String,
    ownerContact: String,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val extra = LocalHisabColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val methodLabel = HisabViewModel.paymentMethodLabel(PaymentMethod.fromCode(advance.paymentMethod), strings)
    val dateFormatted = IndiaFormatters.formatDateKey(advance.dateKey)
    val amountFormatted = IndiaFormatters.formatInr(advance.amountPaise)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, extra.gold)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavyDeep)
                        .padding(16.dp)
                ) {
                    Column {
                        Text(businessName, style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text(
                            "${strings.receiptLabel} • ${advance.receiptNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldBright
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReceiptRow(strings.selectWorker.removeSuffix(" *"), workerName)
                    ReceiptRow(strings.advanceDate.removeSuffix(" *"), dateFormatted)
                    ReceiptRow(strings.paymentMethod, methodLabel)
                    if (advance.referenceNumber.isNotBlank()) {
                        ReceiptRow(strings.referenceNumber, advance.referenceNumber)
                    }
                    if (advance.description.isNotBlank()) {
                        ReceiptRow(strings.descriptionLabel, advance.description)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.advanceAmount.removeSuffix(" *"), style = MaterialTheme.typography.titleMedium)
                        Text(
                            amountFormatted,
                            style = MaterialTheme.typography.headlineSmall,
                            color = extra.gold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val msg = buildString {
                                    appendLine("*$businessName - ${strings.receiptLabel}*")
                                    appendLine("Receipt: ${advance.receiptNumber}")
                                    appendLine("Worker: $workerName")
                                    appendLine("Date: $dateFormatted")
                                    appendLine("Amount: $amountFormatted")
                                    appendLine("Method: $methodLabel")
                                    if (advance.referenceNumber.isNotBlank()) appendLine("Ref: ${advance.referenceNumber}")
                                }
                                ExportManager.shareOnWhatsApp(context, msg)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp")
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    val table = ReportTable(
                                        title = "${strings.receiptLabel} (${advance.receiptNumber})",
                                        subtitle = "Date: $dateFormatted",
                                        businessName = businessName,
                                        ownerContact = ownerContact,
                                        headers = listOf("Field", "Details"),
                                        rows = listOfNotNull(
                                            listOf("Worker", workerName),
                                            listOf("Receipt No.", advance.receiptNumber),
                                            listOf("Date", dateFormatted),
                                            listOf("Method", methodLabel),
                                            advance.referenceNumber.takeIf { it.isNotBlank() }?.let { listOf("Ref No.", it) },
                                            advance.description.takeIf { it.isNotBlank() }?.let { listOf("Description", it) }
                                        ),
                                        summaryLines = listOf(
                                            strings.advanceAmount.removeSuffix(" *") to amountFormatted
                                        )
                                    )
                                    val pdf = ExportManager.generatePdf(context, table, "advance_receipt")
                                    ExportManager.shareFile(context, pdf, "application/pdf", strings.receiptLabel)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF")
                        }
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(strings.close)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = LocalHisabColors.current.textMuted)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
