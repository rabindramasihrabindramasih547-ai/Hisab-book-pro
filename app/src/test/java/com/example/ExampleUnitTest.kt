package com.example

import com.example.data.entity.AdvanceEntity
import com.example.data.entity.PaymentEntity
import com.example.data.entity.WorkEntryEntity
import com.example.domain.HisabCalculator
import com.example.domain.PaymentMethod
import com.example.domain.PaymentType
import com.example.domain.WorkType
import com.example.util.IndiaFormatters
import com.example.util.PinSecurity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `rahul hisab scenario computes exact remaining balance without double counting advance`() {
        val workerId = 1L
        val works = buildList {
            repeat(10) { idx ->
                add(
                    WorkEntryEntity(
                        id = idx + 1L,
                        workerId = workerId,
                        dateKey = 20261101L + idx,
                        workType = WorkType.FULL_TIME.code,
                        ratePaise = 600_00L,
                        totalAmountPaise = 600_00L
                    )
                )
            }
            repeat(5) { idx ->
                add(
                    WorkEntryEntity(
                        id = 20L + idx,
                        workerId = workerId,
                        dateKey = 20261115L + idx,
                        workType = WorkType.HALF_TIME.code,
                        ratePaise = 300_00L,
                        totalAmountPaise = 300_00L
                    )
                )
            }
        }
        val advances = listOf(
            AdvanceEntity(
                id = 1L,
                workerId = workerId,
                dateKey = 20261110L,
                amountPaise = 2_000_00L,
                paymentMethod = PaymentMethod.CASH.code
            )
        )
        val payments = listOf(
            PaymentEntity(
                id = 1L,
                workerId = workerId,
                dateKey = 20261120L,
                amountPaise = 2_000_00L,
                paymentType = PaymentType.PARTIAL_PAYMENT.code,
                paymentMethod = PaymentMethod.UPI.code
            )
        )

        val summaryBeforeAdjustment = HisabCalculator.computeWorkerLedger(workerId, works, advances, payments)
        assertEquals(7_500_00L, summaryBeforeAdjustment.totalWorkEarningsPaise)
        assertEquals(2_000_00L, summaryBeforeAdjustment.totalAdvanceGivenPaise)
        assertEquals(5_500_00L, summaryBeforeAdjustment.netPayablePaise)
        assertEquals(2_000_00L, summaryBeforeAdjustment.totalPaymentsMadePaise)
        assertEquals(3_500_00L, summaryBeforeAdjustment.remainingBalancePaise)

        // Even when advance is formally marked adjusted via ADVANCE_ADJUSTMENT, it must NEVER double count
        val paymentsWithAdjustment = payments + PaymentEntity(
            id = 2L,
            workerId = workerId,
            dateKey = 20261121L,
            amountPaise = 2_000_00L,
            paymentType = PaymentType.ADVANCE_ADJUSTMENT.code,
            paymentMethod = PaymentMethod.CASH.code
        )
        val summaryAfterAdjustment = HisabCalculator.computeWorkerLedger(workerId, works, advances, paymentsWithAdjustment)
        assertEquals(2_000_00L, summaryAfterAdjustment.totalAdvanceAdjustedPaise)
        assertEquals(0L, summaryAfterAdjustment.remainingAdvanceBalancePaise)
        assertEquals(3_500_00L, summaryAfterAdjustment.remainingBalancePaise)
    }

    @Test
    fun `overpayment produces accurate negative remaining balance`() {
        val workerId = 2L
        val works = listOf(
            WorkEntryEntity(
                id = 1L,
                workerId = workerId,
                dateKey = 20261105L,
                workType = WorkType.DAILY.code,
                ratePaise = 1_000_00L,
                totalAmountPaise = 1_000_00L
            )
        )
        val advances = listOf(
            AdvanceEntity(
                id = 1L,
                workerId = workerId,
                dateKey = 20261106L,
                amountPaise = 1_500_00L,
                paymentMethod = PaymentMethod.CASH.code
            )
        )
        val ledger = HisabCalculator.computeWorkerLedger(workerId, works, advances, emptyList())
        assertEquals(-500_00L, ledger.remainingBalancePaise)
        assertTrue(ledger.isOverpaid)
        assertEquals("-₹500.00", IndiaFormatters.formatInr(ledger.remainingBalancePaise))
    }

    @Test
    fun `hourly calculation and independent worker ledgers stay isolated`() {
        val hourlyTotal = HisabCalculator.calculateWorkTotalPaise(WorkType.HOURLY, 100_00L, 4.5)
        assertEquals(450_00L, hourlyTotal)

        val works = listOf(
            WorkEntryEntity(id = 1L, workerId = 10L, dateKey = 20261123L, workType = WorkType.HOURLY.code, ratePaise = 100_00L, totalAmountPaise = hourlyTotal),
            WorkEntryEntity(id = 2L, workerId = 20L, dateKey = 20261123L, workType = WorkType.FULL_TIME.code, ratePaise = 700_00L, totalAmountPaise = 700_00L)
        )
        val ledger10 = HisabCalculator.computeWorkerLedger(10L, works, emptyList(), emptyList())
        val ledger20 = HisabCalculator.computeWorkerLedger(20L, works, emptyList(), emptyList())
        assertEquals(450_00L, ledger10.remainingBalancePaise)
        assertEquals(700_00L, ledger20.remainingBalancePaise)
    }

    @Test
    fun `indian rupee formatting, hours calculation, and mobile validation follow india standards`() {
        assertEquals("₹1,00,000.00", IndiaFormatters.formatInr(1_00_000_00L))
        assertEquals("₹7,500.00", IndiaFormatters.formatInr(7_500_00L))
        assertEquals(123450L, IndiaFormatters.parseRupeesToPaise("1,234.50"))
        assertTrue(IndiaFormatters.isValidIndianMobile("9876543210"))
        assertTrue(IndiaFormatters.isValidIndianMobile("+91 9876543210"))
        assertFalse(IndiaFormatters.isValidIndianMobile("12345"))
        assertEquals(20261123L, IndiaFormatters.parseDdMmYyyyToDateKey("23/11/2026"))
        assertEquals("23/11/2026", IndiaFormatters.formatDateKey(20261123L))
        assertEquals(8.5, IndiaFormatters.calculateHoursBetween("09:00", "17:30") ?: 0.0, 0.01)
    }

    @Test
    fun `salted pin hashing verifies matching pin and rejects wrong pin`() {
        val salt = PinSecurity.generateSaltBase64()
        val hash = PinSecurity.hashPin("4829", salt)
        assertNotEquals("4829", hash)
        assertTrue(PinSecurity.verifyPin("4829", hash, salt))
        assertFalse(PinSecurity.verifyPin("1234", hash, salt))
    }
}
