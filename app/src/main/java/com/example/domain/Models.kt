package com.example.domain

enum class WorkType(val code: String) {
    HALF_TIME("HALF_TIME"),
    FULL_TIME("FULL_TIME"),
    HOURLY("HOURLY"),
    DAILY("DAILY"),
    CUSTOM("CUSTOM");

    companion object {
        fun fromCode(code: String): WorkType =
            entries.firstOrNull { it.code == code } ?: DAILY
    }
}

enum class AttendanceStatus(val code: String) {
    PRESENT("PRESENT"),
    ABSENT("ABSENT"),
    HALF_DAY("HALF_DAY"),
    FULL_DAY("FULL_DAY"),
    LEAVE("LEAVE"),
    CUSTOM_HOURS("CUSTOM_HOURS");

    companion object {
        fun fromCode(code: String): AttendanceStatus =
            entries.firstOrNull { it.code == code } ?: PRESENT
    }
}

enum class PaymentMethod(val code: String) {
    CASH("CASH"),
    UPI("UPI"),
    BANK_TRANSFER("BANK_TRANSFER"),
    OTHER("OTHER");

    companion object {
        fun fromCode(code: String): PaymentMethod =
            entries.firstOrNull { it.code == code } ?: CASH
    }
}

enum class PaymentType(val code: String) {
    FULL_PAYMENT("FULL_PAYMENT"),
    PARTIAL_PAYMENT("PARTIAL_PAYMENT"),
    ADVANCE_ADJUSTMENT("ADVANCE_ADJUSTMENT"),
    CUSTOM_PAYMENT("CUSTOM_PAYMENT");

    companion object {
        fun fromCode(code: String): PaymentType =
            entries.firstOrNull { it.code == code } ?: PARTIAL_PAYMENT
    }
}

enum class ReminderType(val code: String) {
    PENDING_PAYMENT("PENDING_PAYMENT"),
    PAYMENT_DUE("PAYMENT_DUE"),
    MONTHLY_REPORT("MONTHLY_REPORT"),
    BACKUP("BACKUP");

    companion object {
        fun fromCode(code: String): ReminderType =
            entries.firstOrNull { it.code == code } ?: PENDING_PAYMENT
    }
}

enum class LedgerDateFilter {
    TODAY, THIS_WEEK, THIS_MONTH, ALL_TIME, CUSTOM
}

enum class ReportType {
    DAILY, WEEKLY, MONTHLY, WORKER_WISE, ATTENDANCE, ADVANCE, PAYMENT, OUTSTANDING
}

data class WorkerLedgerSummary(
    val workerId: Long,
    val totalWorkCount: Int,
    val totalWorkEarningsPaise: Long,
    val totalAdvanceGivenPaise: Long,
    val totalAdvanceAdjustedPaise: Long,
    val remainingAdvanceBalancePaise: Long,
    val netPayablePaise: Long,
    val totalPaymentsMadePaise: Long,
    val remainingBalancePaise: Long
) {
    val isOverpaid: Boolean get() = remainingBalancePaise < 0L
}

data class DashboardSummary(
    val totalWorkers: Int,
    val activeWorkers: Int,
    val todayPresentWorkers: Int,
    val todayWorkCount: Int,
    val todayWorkAmountPaise: Long,
    val totalEarningsPaise: Long,
    val totalAdvanceGivenPaise: Long,
    val totalPaymentsMadePaise: Long,
    val totalOutstandingBalancePaise: Long,
    val monthlyExpensesPaise: Long,
    val monthlySeries: List<MonthlyChartPoint>
)

data class MonthlyChartPoint(
    val yearMonth: Long,
    val label: String,
    val earningsPaise: Long,
    val paidOutPaise: Long
)

data class UnifiedTransaction(
    val id: Long,
    val workerId: Long,
    val workerName: String,
    val dateKey: Long,
    val timestamp: Long,
    val category: TransactionCategory,
    val subtitle: String,
    val methodOrWorkType: String,
    val amountPaise: Long,
    val reference: String
)

enum class TransactionCategory {
    WORK, ADVANCE, PAYMENT, ADVANCE_ADJUSTMENT
}
