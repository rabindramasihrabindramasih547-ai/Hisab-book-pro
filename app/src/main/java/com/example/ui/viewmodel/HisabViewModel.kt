package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HisabBookApp
import com.example.data.entity.AdvanceEntity
import com.example.data.entity.AttendanceEntity
import com.example.data.entity.PaymentEntity
import com.example.data.entity.ReminderEntity
import com.example.data.entity.WorkEntryEntity
import com.example.data.entity.WorkerEntity
import com.example.data.prefs.AppSettings
import com.example.domain.AttendanceStatus
import com.example.domain.DashboardSummary
import com.example.domain.HisabCalculator
import com.example.domain.MonthlyChartPoint
import com.example.domain.PaymentMethod
import com.example.domain.PaymentType
import com.example.domain.ReportType
import com.example.domain.TransactionCategory
import com.example.domain.UnifiedTransaction
import com.example.domain.WorkType
import com.example.domain.WorkerLedgerSummary
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.stringsFor
import com.example.ui.theme.ThemeMode
import com.example.util.IndiaFormatters
import com.example.util.PinSecurity
import com.example.util.ReportTable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HisabViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as HisabBookApp
    private val repo = app.repository
    private val settingsStore = app.settingsStore

    val settings: StateFlow<AppSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val workers: StateFlow<List<WorkerEntity>> = repo.workersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workEntries: StateFlow<List<WorkEntryEntity>> = repo.workEntriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendanceRecords: StateFlow<List<AttendanceEntity>> = repo.attendanceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val advances: StateFlow<List<AdvanceEntity>> = repo.advancesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<PaymentEntity>> = repo.paymentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = repo.remindersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked

    private val _pinInitialized = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val initialSettings = settingsStore.settingsFlow.first()
            if (initialSettings.pinEnabled && initialSettings.pinHash.isNotEmpty()) {
                _isLocked.value = true
            }
            _pinInitialized.value = true
        }
    }

    val workerLedgers: StateFlow<Map<Long, WorkerLedgerSummary>> = combine(
        workers,
        workEntries,
        advances,
        payments
    ) { workerList, workList, advanceList, paymentList ->
        workerList.associate { worker ->
            worker.id to HisabCalculator.computeWorkerLedger(
                workerId = worker.id,
                workEntries = workList,
                advances = advanceList,
                payments = paymentList
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        workers,
        workEntries,
        attendanceRecords,
        advances,
        payments,
        workerLedgers,
        settings
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val workerList = values[0] as List<WorkerEntity>
        @Suppress("UNCHECKED_CAST")
        val workList = values[1] as List<WorkEntryEntity>
        @Suppress("UNCHECKED_CAST")
        val attendanceList = values[2] as List<AttendanceEntity>
        @Suppress("UNCHECKED_CAST")
        val advanceList = values[3] as List<AdvanceEntity>
        @Suppress("UNCHECKED_CAST")
        val paymentList = values[4] as List<PaymentEntity>
        @Suppress("UNCHECKED_CAST")
        val ledgerMap = values[5] as Map<Long, WorkerLedgerSummary>
        val currentSettings = values[6] as AppSettings

        val todayKey = IndiaFormatters.todayDateKey()
        val monthStart = IndiaFormatters.startOfMonthDateKey(todayKey)
        val monthEnd = IndiaFormatters.endOfMonthDateKey(todayKey)
        val isHindi = currentSettings.language == AppLanguage.HINDI

        val presentToday = attendanceList.count {
            it.dateKey == todayKey && it.status in setOf(
                AttendanceStatus.PRESENT.code,
                AttendanceStatus.HALF_DAY.code,
                AttendanceStatus.FULL_DAY.code,
                AttendanceStatus.CUSTOM_HOURS.code
            )
        }
        val todayWorks = workList.filter { it.dateKey == todayKey }
        val totalEarnings = workList.filter { it.isApproved }.sumOf { it.totalAmountPaise }
        val totalAdvances = advanceList.sumOf { it.amountPaise }
        val totalActualPayments = paymentList
            .filter { it.paymentType != PaymentType.ADVANCE_ADJUSTMENT.code }
            .sumOf { it.amountPaise }
        val totalOutstanding = ledgerMap.values.sumOf { it.remainingBalancePaise }

        val monthlyWorkValue = workList
            .filter { it.isApproved && it.dateKey in monthStart..monthEnd }
            .sumOf { it.totalAmountPaise }

        val series = (5 downTo 0).map { offset ->
            val (start, end) = IndiaFormatters.monthRangeForOffset(offset, todayKey)
            val ym = IndiaFormatters.yearMonthKey(start)
            val monthEarn = workList
                .filter { it.isApproved && it.dateKey in start..end }
                .sumOf { it.totalAmountPaise }
            val monthPaid = advanceList
                .filter { it.dateKey in start..end }
                .sumOf { it.amountPaise } +
                paymentList
                    .filter { it.paymentType != PaymentType.ADVANCE_ADJUSTMENT.code && it.dateKey in start..end }
                    .sumOf { it.amountPaise }
            MonthlyChartPoint(
                yearMonth = ym,
                label = IndiaFormatters.monthShortLabel(ym, isHindi),
                earningsPaise = monthEarn,
                paidOutPaise = monthPaid
            )
        }

        DashboardSummary(
            totalWorkers = workerList.size,
            activeWorkers = workerList.count { it.isActive },
            todayPresentWorkers = presentToday,
            todayWorkCount = todayWorks.size,
            todayWorkAmountPaise = todayWorks.sumOf { it.totalAmountPaise },
            totalEarningsPaise = totalEarnings,
            totalAdvanceGivenPaise = totalAdvances,
            totalPaymentsMadePaise = totalActualPayments,
            totalOutstandingBalancePaise = totalOutstanding,
            monthlyExpensesPaise = monthlyWorkValue,
            monthlySeries = series
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardSummary(0, 0, 0, 0, 0L, 0L, 0L, 0L, 0L, 0L, emptyList())
    )

    val recentTransactions: StateFlow<List<UnifiedTransaction>> = combine(
        workers,
        workEntries,
        advances,
        payments
    ) { workerList, workList, advanceList, paymentList ->
        val nameMap = workerList.associate { it.id to it.fullName }
        val list = mutableListOf<UnifiedTransaction>()

        workList.forEach { w ->
            list += UnifiedTransaction(
                id = w.id,
                workerId = w.workerId,
                workerName = nameMap[w.workerId] ?: "Worker #${w.workerId}",
                dateKey = w.dateKey,
                timestamp = w.createdAt,
                category = TransactionCategory.WORK,
                subtitle = w.description.ifBlank { w.workType },
                methodOrWorkType = w.workType,
                amountPaise = w.totalAmountPaise,
                reference = "${w.totalHours}h"
            )
        }
        advanceList.forEach { a ->
            list += UnifiedTransaction(
                id = a.id,
                workerId = a.workerId,
                workerName = nameMap[a.workerId] ?: "Worker #${a.workerId}",
                dateKey = a.dateKey,
                timestamp = a.createdAt,
                category = TransactionCategory.ADVANCE,
                subtitle = a.description.ifBlank { a.receiptNumber },
                methodOrWorkType = a.paymentMethod,
                amountPaise = a.amountPaise,
                reference = a.receiptNumber
            )
        }
        paymentList.forEach { p ->
            val cat = if (p.paymentType == PaymentType.ADVANCE_ADJUSTMENT.code) {
                TransactionCategory.ADVANCE_ADJUSTMENT
            } else {
                TransactionCategory.PAYMENT
            }
            list += UnifiedTransaction(
                id = p.id,
                workerId = p.workerId,
                workerName = nameMap[p.workerId] ?: "Worker #${p.workerId}",
                dateKey = p.dateKey,
                timestamp = p.createdAt,
                category = cat,
                subtitle = p.notes.ifBlank { p.paymentType },
                methodOrWorkType = p.paymentMethod,
                amountPaise = p.amountPaise,
                reference = p.transactionId
            )
        }
        list.sortedWith(compareByDescending<UnifiedTransaction> { it.dateKey }.thenByDescending { it.timestamp })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun unlockWithPin(pin: String): Boolean {
        val current = settings.value
        val valid = PinSecurity.verifyPin(pin, current.pinHash, current.pinSalt)
        if (valid) _isLocked.value = false
        return valid
    }

    fun unlockWithBiometricSuccess() {
        _isLocked.value = false
    }

    fun lockNow() {
        if (settings.value.pinEnabled) _isLocked.value = true
    }

    fun saveWorker(worker: WorkerEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repo.saveWorker(worker)
            onSaved(id)
        }
    }

    fun deleteWorker(workerId: Long) {
        viewModelScope.launch { repo.deleteWorker(workerId) }
    }

    fun saveWorkEntry(entry: WorkEntryEntity, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repo.saveWorkEntry(entry)
            onSaved()
        }
    }

    fun deleteWorkEntry(id: Long) {
        viewModelScope.launch { repo.deleteWorkEntry(id) }
    }

    fun markAttendance(
        workerId: Long,
        dateKey: Long,
        status: AttendanceStatus,
        hours: Double = 0.0,
        notes: String = ""
    ) {
        viewModelScope.launch {
            repo.markAttendance(
                AttendanceEntity(
                    workerId = workerId,
                    dateKey = dateKey,
                    status = status.code,
                    hours = hours,
                    notes = notes
                )
            )
        }
    }

    fun clearAttendance(workerId: Long, dateKey: Long) {
        viewModelScope.launch { repo.clearAttendance(workerId, dateKey) }
    }

    fun saveAdvance(advance: AdvanceEntity, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repo.saveAdvance(advance)
            onSaved()
        }
    }

    fun deleteAdvance(id: Long) {
        viewModelScope.launch { repo.deleteAdvance(id) }
    }

    fun savePayment(payment: PaymentEntity, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repo.savePayment(payment)
            onSaved()
        }
    }

    fun deletePayment(id: Long) {
        viewModelScope.launch { repo.deletePayment(id) }
    }

    fun saveReminder(reminder: ReminderEntity) {
        viewModelScope.launch { repo.saveReminder(reminder) }
    }

    fun toggleReminderCompleted(reminder: ReminderEntity) {
        viewModelScope.launch {
            repo.saveReminder(reminder.copy(isCompleted = !reminder.isCompleted))
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch { repo.deleteReminder(id) }
    }

    suspend fun copyPhotoToInternal(uri: Uri, prefix: String): String? =
        repo.copyPickerImageToInternal(uri, prefix)

    fun updateBusinessProfile(name: String, owner: String, mobile: String) {
        viewModelScope.launch { settingsStore.updateBusinessProfile(name, owner, mobile) }
    }

    fun updateBusinessLogo(uri: Uri) {
        viewModelScope.launch {
            val path = repo.copyPickerImageToInternal(uri, "business_logo")
            if (path != null) settingsStore.updateBusinessLogo(path)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsStore.setLanguage(language) }
    }

    fun toggleLanguage() {
        val next = if (settings.value.language == AppLanguage.HINDI) AppLanguage.ENGLISH else AppLanguage.HINDI
        setLanguage(next)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    fun setDefaultPaymentMethod(method: PaymentMethod) {
        viewModelScope.launch { settingsStore.setDefaultPaymentMethod(method) }
    }

    fun setPin(pin: String) {
        viewModelScope.launch { settingsStore.setPin(pin) }
    }

    fun disablePin() {
        viewModelScope.launch { settingsStore.disablePin() }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setBiometricEnabled(enabled) }
    }

    fun setReminderPreferences(pendingAlerts: Boolean, backupReminder: Boolean) {
        viewModelScope.launch { settingsStore.setReminderPreferences(pendingAlerts, backupReminder) }
    }

    suspend fun createBackupJson(): String {
        val json = repo.exportBackupJson()
        settingsStore.markBackupCompleted()
        return json
    }

    suspend fun restoreBackupJson(json: String): Result<Int> = repo.restoreFromBackupJson(json)

    fun clearAllData() {
        viewModelScope.launch { repo.clearAllData() }
    }

    fun buildReportTable(
        reportType: ReportType,
        startDateKey: Long,
        endDateKey: Long,
        selectedWorkerId: Long? = null,
        strings: AppStrings = stringsFor(settings.value.language)
    ): ReportTable {
        val s = settings.value
        val workerList = workers.value
        val workerMap = workerList.associateBy { it.id }
        val workList = workEntries.value
        val attList = attendanceRecords.value
        val advList = advances.value
        val payList = payments.value
        val ledgers = workerLedgers.value

        val ownerContact = listOfNotNull(
            s.ownerName.takeIf { it.isNotBlank() },
            s.ownerMobile.takeIf { it.isNotBlank() }
        ).joinToString(" • ")

        val rangeSubtitle = "${IndiaFormatters.formatDateKey(startDateKey)} - ${IndiaFormatters.formatDateKey(endDateKey)}"

        return when (reportType) {
            ReportType.DAILY, ReportType.WEEKLY, ReportType.MONTHLY -> {
                val filteredWorks = workList.filter { it.dateKey in startDateKey..endDateKey }
                val filteredAdv = advList.filter { it.dateKey in startDateKey..endDateKey }
                val filteredPay = payList.filter { it.dateKey in startDateKey..endDateKey }
                val title = when (reportType) {
                    ReportType.DAILY -> strings.reportDaily
                    ReportType.WEEKLY -> strings.reportWeekly
                    else -> strings.reportMonthly
                }
                val rows = mutableListOf<List<String>>()
                filteredWorks.forEach { w ->
                    rows += listOf(
                        IndiaFormatters.formatDateKey(w.dateKey),
                        workerMap[w.workerId]?.fullName ?: "-",
                        strings.addWork,
                        workTypeLabel(WorkType.fromCode(w.workType), strings),
                        IndiaFormatters.formatInr(w.totalAmountPaise)
                    )
                }
                filteredAdv.forEach { a ->
                    rows += listOf(
                        IndiaFormatters.formatDateKey(a.dateKey),
                        workerMap[a.workerId]?.fullName ?: "-",
                        strings.tabAdvances,
                        paymentMethodLabel(PaymentMethod.fromCode(a.paymentMethod), strings),
                        IndiaFormatters.formatInr(a.amountPaise)
                    )
                }
                filteredPay.forEach { p ->
                    rows += listOf(
                        IndiaFormatters.formatDateKey(p.dateKey),
                        workerMap[p.workerId]?.fullName ?: "-",
                        paymentTypeLabel(PaymentType.fromCode(p.paymentType), strings),
                        paymentMethodLabel(PaymentMethod.fromCode(p.paymentMethod), strings),
                        IndiaFormatters.formatInr(p.amountPaise)
                    )
                }
                val workTotal = filteredWorks.sumOf { it.totalAmountPaise }
                val advTotal = filteredAdv.sumOf { it.amountPaise }
                val payTotal = filteredPay
                    .filter { it.paymentType != PaymentType.ADVANCE_ADJUSTMENT.code }
                    .sumOf { it.amountPaise }
                ReportTable(
                    title = title,
                    subtitle = rangeSubtitle,
                    businessName = s.businessName,
                    ownerContact = ownerContact,
                    headers = listOf("Date", "Worker", "Type", "Details", "Amount (INR)"),
                    rows = rows,
                    summaryLines = listOf(
                        strings.totalEarnings to IndiaFormatters.formatInr(workTotal),
                        strings.totalAdvanceGiven to IndiaFormatters.formatInr(advTotal),
                        strings.totalPaymentsMade to IndiaFormatters.formatInr(payTotal)
                    )
                )
            }

            ReportType.WORKER_WISE -> {
                val targetWorker = selectedWorkerId?.let { workerMap[it] }
                if (targetWorker != null) {
                    buildWorkerLedgerReportTable(targetWorker, startDateKey, endDateKey, strings)
                } else {
                    val rows = workerList.map { w ->
                        val l = ledgers[w.id]
                        listOf(
                            w.workerCode,
                            w.fullName,
                            IndiaFormatters.formatInr(l?.totalWorkEarningsPaise ?: 0L),
                            IndiaFormatters.formatInr(l?.totalAdvanceGivenPaise ?: 0L),
                            IndiaFormatters.formatInr(l?.totalPaymentsMadePaise ?: 0L),
                            IndiaFormatters.formatInr(l?.remainingBalancePaise ?: 0L)
                        )
                    }
                    ReportTable(
                        title = strings.reportWorkerWise,
                        subtitle = rangeSubtitle,
                        businessName = s.businessName,
                        ownerContact = ownerContact,
                        headers = listOf("ID", "Worker", "Earnings", "Advance", "Paid", "Balance"),
                        rows = rows,
                        summaryLines = listOf(
                            strings.totalEarnings to IndiaFormatters.formatInr(ledgers.values.sumOf { it.totalWorkEarningsPaise }),
                            strings.totalOutstandingBalance to IndiaFormatters.formatInr(ledgers.values.sumOf { it.remainingBalancePaise })
                        )
                    )
                }
            }

            ReportType.ATTENDANCE -> {
                val filteredAtt = attList.filter { it.dateKey in startDateKey..endDateKey }
                val rows = filteredAtt.map { a ->
                    listOf(
                        IndiaFormatters.formatDateKey(a.dateKey),
                        workerMap[a.workerId]?.fullName ?: "-",
                        attendanceLabel(AttendanceStatus.fromCode(a.status), strings),
                        if (a.hours > 0) "${a.hours}h" else "-",
                        a.notes.ifBlank { "-" }
                    )
                }
                ReportTable(
                    title = strings.reportAttendance,
                    subtitle = rangeSubtitle,
                    businessName = s.businessName,
                    ownerContact = ownerContact,
                    headers = listOf("Date", "Worker", "Status", "Hours", "Notes"),
                    rows = rows,
                    summaryLines = listOf(
                        strings.entriesCount to filteredAtt.size.toString()
                    )
                )
            }

            ReportType.ADVANCE -> {
                val filtered = advList.filter { it.dateKey in startDateKey..endDateKey }
                val rows = filtered.map { a ->
                    listOf(
                        IndiaFormatters.formatDateKey(a.dateKey),
                        workerMap[a.workerId]?.fullName ?: "-",
                        a.receiptNumber,
                        paymentMethodLabel(PaymentMethod.fromCode(a.paymentMethod), strings),
                        IndiaFormatters.formatInr(a.amountPaise)
                    )
                }
                ReportTable(
                    title = strings.reportAdvance,
                    subtitle = rangeSubtitle,
                    businessName = s.businessName,
                    ownerContact = ownerContact,
                    headers = listOf("Date", "Worker", "Receipt", "Method", "Amount"),
                    rows = rows,
                    summaryLines = listOf(
                        strings.totalAdvanceGiven to IndiaFormatters.formatInr(filtered.sumOf { it.amountPaise })
                    )
                )
            }

            ReportType.PAYMENT -> {
                val filtered = payList.filter { it.dateKey in startDateKey..endDateKey }
                val rows = filtered.map { p ->
                    listOf(
                        IndiaFormatters.formatDateKey(p.dateKey),
                        workerMap[p.workerId]?.fullName ?: "-",
                        paymentTypeLabel(PaymentType.fromCode(p.paymentType), strings),
                        paymentMethodLabel(PaymentMethod.fromCode(p.paymentMethod), strings),
                        IndiaFormatters.formatInr(p.amountPaise)
                    )
                }
                val totalPaid = filtered
                    .filter { it.paymentType != PaymentType.ADVANCE_ADJUSTMENT.code }
                    .sumOf { it.amountPaise }
                ReportTable(
                    title = strings.reportPayment,
                    subtitle = rangeSubtitle,
                    businessName = s.businessName,
                    ownerContact = ownerContact,
                    headers = listOf("Date", "Worker", "Type", "Method", "Amount"),
                    rows = rows,
                    summaryLines = listOf(
                        strings.totalPaymentsMade to IndiaFormatters.formatInr(totalPaid)
                    )
                )
            }

            ReportType.OUTSTANDING -> {
                val rows = workerList.mapNotNull { w ->
                    val l = ledgers[w.id] ?: return@mapNotNull null
                    if (l.remainingBalancePaise == 0L) return@mapNotNull null
                    listOf(
                        w.workerCode,
                        w.fullName,
                        w.mobileNumber,
                        IndiaFormatters.formatInr(l.totalWorkEarningsPaise),
                        IndiaFormatters.formatInr(l.totalAdvanceGivenPaise + l.totalPaymentsMadePaise),
                        IndiaFormatters.formatInr(l.remainingBalancePaise)
                    )
                }
                ReportTable(
                    title = strings.reportOutstanding,
                    subtitle = rangeSubtitle,
                    businessName = s.businessName,
                    ownerContact = ownerContact,
                    headers = listOf("ID", "Worker", "Mobile", "Earned", "Advance+Paid", "Balance"),
                    rows = rows,
                    summaryLines = listOf(
                        strings.totalOutstandingBalance to IndiaFormatters.formatInr(
                            ledgers.values.sumOf { it.remainingBalancePaise }
                        )
                    )
                )
            }
        }
    }

    fun buildWorkerLedgerReportTable(
        worker: WorkerEntity,
        startDateKey: Long,
        endDateKey: Long,
        strings: AppStrings = stringsFor(settings.value.language)
    ): ReportTable {
        val s = settings.value
        val ownerContact = listOfNotNull(
            s.ownerName.takeIf { it.isNotBlank() },
            s.ownerMobile.takeIf { it.isNotBlank() }
        ).joinToString(" • ")

        val wWorks = workEntries.value.filter { it.workerId == worker.id && it.dateKey in startDateKey..endDateKey }
        val wAdvs = advances.value.filter { it.workerId == worker.id && it.dateKey in startDateKey..endDateKey }
        val wPays = payments.value.filter { it.workerId == worker.id && it.dateKey in startDateKey..endDateKey }
        val ledger = HisabCalculator.computeWorkerLedger(worker.id, wWorks, wAdvs, wPays)

        val rows = mutableListOf<List<String>>()
        wWorks.forEach { w ->
            rows += listOf(
                IndiaFormatters.formatDateKey(w.dateKey),
                strings.addWork,
                workTypeLabel(WorkType.fromCode(w.workType), strings) +
                    if (w.description.isNotBlank()) " (${w.description})" else "",
                "+ ${IndiaFormatters.formatInr(w.totalAmountPaise)}"
            )
        }
        wAdvs.forEach { a ->
            rows += listOf(
                IndiaFormatters.formatDateKey(a.dateKey),
                strings.tabAdvances,
                "${a.receiptNumber} (${paymentMethodLabel(PaymentMethod.fromCode(a.paymentMethod), strings)})",
                "- ${IndiaFormatters.formatInr(a.amountPaise)}"
            )
        }
        wPays.forEach { p ->
            rows += listOf(
                IndiaFormatters.formatDateKey(p.dateKey),
                paymentTypeLabel(PaymentType.fromCode(p.paymentType), strings),
                paymentMethodLabel(PaymentMethod.fromCode(p.paymentMethod), strings),
                "- ${IndiaFormatters.formatInr(p.amountPaise)}"
            )
        }

        return ReportTable(
            title = "${worker.fullName} (${worker.workerCode}) - ${strings.workerLedger}",
            subtitle = "${worker.workCategory} • ${worker.mobileNumber} • ${IndiaFormatters.formatDateKey(startDateKey)} - ${IndiaFormatters.formatDateKey(endDateKey)}",
            businessName = s.businessName,
            ownerContact = ownerContact,
            headers = listOf("Date", "Category", "Description", "Amount (INR)"),
            rows = rows.sortedByDescending { it.firstOrNull() ?: "" },
            summaryLines = listOf(
                strings.totalWorkLabel to IndiaFormatters.formatInr(ledger.totalWorkEarningsPaise),
                strings.totalAdvanceGiven to IndiaFormatters.formatInr(ledger.totalAdvanceGivenPaise),
                strings.advanceAdjustedLabel to IndiaFormatters.formatInr(ledger.totalAdvanceAdjustedPaise),
                strings.netPayableLabel to IndiaFormatters.formatInr(ledger.netPayablePaise),
                strings.totalPaymentsMade to IndiaFormatters.formatInr(ledger.totalPaymentsMadePaise),
                strings.remainingBalanceLabel to IndiaFormatters.formatInr(ledger.remainingBalancePaise)
            )
        )
    }

    companion object {
        fun workTypeLabel(type: WorkType, strings: AppStrings): String = when (type) {
            WorkType.HALF_TIME -> strings.workTypeHalfTime
            WorkType.FULL_TIME -> strings.workTypeFullTime
            WorkType.HOURLY -> strings.workTypeHourly
            WorkType.DAILY -> strings.workTypeDaily
            WorkType.CUSTOM -> strings.workTypeCustom
        }

        fun attendanceLabel(status: AttendanceStatus, strings: AppStrings): String = when (status) {
            AttendanceStatus.PRESENT -> strings.attendancePresent
            AttendanceStatus.ABSENT -> strings.attendanceAbsent
            AttendanceStatus.HALF_DAY -> strings.attendanceHalfDay
            AttendanceStatus.FULL_DAY -> strings.attendanceFullDay
            AttendanceStatus.LEAVE -> strings.attendanceLeave
            AttendanceStatus.CUSTOM_HOURS -> strings.attendanceCustomHours
        }

        fun paymentMethodLabel(method: PaymentMethod, strings: AppStrings): String = when (method) {
            PaymentMethod.CASH -> strings.methodCash
            PaymentMethod.UPI -> strings.methodUpi
            PaymentMethod.BANK_TRANSFER -> strings.methodBank
            PaymentMethod.OTHER -> strings.methodOther
        }

        fun paymentTypeLabel(type: PaymentType, strings: AppStrings): String = when (type) {
            PaymentType.FULL_PAYMENT -> strings.typeFullPayment
            PaymentType.PARTIAL_PAYMENT -> strings.typePartialPayment
            PaymentType.ADVANCE_ADJUSTMENT -> strings.typeAdvanceAdjustment
            PaymentType.CUSTOM_PAYMENT -> strings.typeCustomPayment
        }
    }
}
