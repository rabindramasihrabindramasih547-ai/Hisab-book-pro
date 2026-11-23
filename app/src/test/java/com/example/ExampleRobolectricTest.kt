package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.HisabDatabase
import com.example.data.entity.AdvanceEntity
import com.example.data.entity.AttendanceEntity
import com.example.data.entity.PaymentEntity
import com.example.data.entity.WorkEntryEntity
import com.example.data.entity.WorkerEntity
import com.example.data.repo.HisabRepository
import com.example.domain.AttendanceStatus
import com.example.domain.HisabCalculator
import com.example.domain.PaymentMethod
import com.example.domain.PaymentType
import com.example.domain.WorkType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: HisabDatabase
    private lateinit var repo: HisabRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, HisabDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = HisabRepository(context, db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `app name matches HisabBook Pro`() {
        assertEquals("HisabBook Pro", context.getString(R.string.app_name))
    }

    @Test
    fun `room persistence and backup restore roundtrip preserve worker ledger`() = runBlocking {
        val workerId = repo.saveWorker(
            WorkerEntity(
                workerCode = "HB-001",
                fullName = "Rahul Sharma",
                mobileNumber = "+91 9876543210",
                joiningDateKey = 20261101L,
                workCategory = "Mason",
                fullTimeRatePaise = 600_00L,
                halfTimeRatePaise = 300_00L,
                hourlyRatePaise = 100_00L
            )
        )
        repo.saveWorkEntry(
            WorkEntryEntity(
                workerId = workerId,
                dateKey = 20261123L,
                workType = WorkType.FULL_TIME.code,
                ratePaise = 600_00L,
                totalAmountPaise = 600_00L
            )
        )
        repo.markAttendance(
            AttendanceEntity(
                workerId = workerId,
                dateKey = 20261123L,
                status = AttendanceStatus.FULL_DAY.code,
                hours = 8.0
            )
        )
        repo.saveAdvance(
            AdvanceEntity(
                workerId = workerId,
                dateKey = 20261123L,
                amountPaise = 200_00L,
                paymentMethod = PaymentMethod.CASH.code
            )
        )
        repo.savePayment(
            PaymentEntity(
                workerId = workerId,
                dateKey = 20261123L,
                amountPaise = 100_00L,
                paymentType = PaymentType.PARTIAL_PAYMENT.code,
                paymentMethod = PaymentMethod.UPI.code
            )
        )

        val backupJson = repo.exportBackupJson()
        repo.clearAllData()
        assertEquals(0, repo.workersFlow.first().size)

        val restoreResult = repo.restoreFromBackupJson(backupJson)
        assertTrue(restoreResult.isSuccess)
        val restoredWorkers = repo.workersFlow.first()
        assertEquals(1, restoredWorkers.size)
        val newId = restoredWorkers.first().id
        val ledger = HisabCalculator.computeWorkerLedger(
            workerId = newId,
            workEntries = repo.workEntriesFlow.first(),
            advances = repo.advancesFlow.first(),
            payments = repo.paymentsFlow.first()
        )
        assertEquals(600_00L, ledger.totalWorkEarningsPaise)
        assertEquals(200_00L, ledger.totalAdvanceGivenPaise)
        assertEquals(100_00L, ledger.totalPaymentsMadePaise)
        assertEquals(300_00L, ledger.remainingBalancePaise)
    }
}
