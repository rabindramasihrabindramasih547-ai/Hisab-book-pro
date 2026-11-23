package com.example.domain

import com.example.data.entity.AdvanceEntity
import com.example.data.entity.PaymentEntity
import com.example.data.entity.WorkEntryEntity
import com.example.data.entity.WorkerEntity
import kotlin.math.roundToLong

object HisabCalculator {

    fun calculateWorkTotalPaise(
        workType: WorkType,
        ratePaise: Long,
        hours: Double
    ): Long {
        return when (workType) {
            WorkType.HOURLY -> (ratePaise.toDouble() * hours.coerceAtLeast(0.0)).roundToLong()
            WorkType.HALF_TIME, WorkType.FULL_TIME, WorkType.DAILY, WorkType.CUSTOM -> ratePaise
        }
    }

    fun defaultRateForWorker(worker: WorkerEntity, workType: WorkType): Long {
        return when (workType) {
            WorkType.HALF_TIME -> worker.halfTimeRatePaise
            WorkType.FULL_TIME -> worker.fullTimeRatePaise
            WorkType.HOURLY -> worker.hourlyRatePaise
            WorkType.DAILY -> worker.dailyRatePaise
            WorkType.CUSTOM -> 0L
        }
    }

    /**
     * Exact Hisab calculation avoiding any double-counting between Advances Given,
     * Advance Adjustments, and Actual Payments Made:
     *
     * - totalWorkEarningsPaise = sum of approved work entries
     * - totalAdvanceGivenPaise = sum of advance records
     * - totalAdvanceAdjustedPaise = sum of ADVANCE_ADJUSTMENT payment records
     * - remainingAdvanceBalancePaise = totalAdvanceGivenPaise - totalAdvanceAdjustedPaise
     * - effectiveAdvanceDeductedPaise = maxOf(totalAdvanceGivenPaise, totalAdvanceAdjustedPaise)
     * - netPayablePaise = totalWorkEarningsPaise - effectiveAdvanceDeductedPaise
     * - totalPaymentsMadePaise = sum of payments excluding ADVANCE_ADJUSTMENT
     * - remainingBalancePaise = netPayablePaise - totalPaymentsMadePaise
     */
    fun computeWorkerLedger(
        workerId: Long,
        workEntries: List<WorkEntryEntity>,
        advances: List<AdvanceEntity>,
        payments: List<PaymentEntity>
    ): WorkerLedgerSummary {
        val approvedWorks = workEntries.filter { it.workerId == workerId && it.isApproved }
        val workerAdvances = advances.filter { it.workerId == workerId }
        val workerPayments = payments.filter { it.workerId == workerId }

        val totalWorkEarnings = approvedWorks.sumOf { it.totalAmountPaise }
        val totalAdvanceGiven = workerAdvances.sumOf { it.amountPaise }
        val totalAdvanceAdjusted = workerPayments
            .filter { it.paymentType == PaymentType.ADVANCE_ADJUSTMENT.code }
            .sumOf { it.amountPaise }

        val remainingAdvance = totalAdvanceGiven - totalAdvanceAdjusted
        val effectiveAdvanceDeducted = maxOf(totalAdvanceGiven, totalAdvanceAdjusted)
        val netPayable = totalWorkEarnings - effectiveAdvanceDeducted

        val totalActualPayments = workerPayments
            .filter { it.paymentType != PaymentType.ADVANCE_ADJUSTMENT.code }
            .sumOf { it.amountPaise }

        val remainingBalance = netPayable - totalActualPayments

        return WorkerLedgerSummary(
            workerId = workerId,
            totalWorkCount = approvedWorks.size,
            totalWorkEarningsPaise = totalWorkEarnings,
            totalAdvanceGivenPaise = totalAdvanceGiven,
            totalAdvanceAdjustedPaise = totalAdvanceAdjusted,
            remainingAdvanceBalancePaise = remainingAdvance,
            netPayablePaise = netPayable,
            totalPaymentsMadePaise = totalActualPayments,
            remainingBalancePaise = remainingBalance
        )
    }
}
