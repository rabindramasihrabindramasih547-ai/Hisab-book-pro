package com.example.data.repo

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.data.db.HisabDatabase
import com.example.data.entity.AdvanceEntity
import com.example.data.entity.AttendanceEntity
import com.example.data.entity.PaymentEntity
import com.example.data.entity.ReminderEntity
import com.example.data.entity.WorkEntryEntity
import com.example.data.entity.WorkerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class HisabRepository(
    private val context: Context,
    private val db: HisabDatabase
) {
    private val workerDao = db.workerDao()
    private val workEntryDao = db.workEntryDao()
    private val attendanceDao = db.attendanceDao()
    private val advanceDao = db.advanceDao()
    private val paymentDao = db.paymentDao()
    private val reminderDao = db.reminderDao()

    val workersFlow: Flow<List<WorkerEntity>> = workerDao.observeAll()
    val workEntriesFlow: Flow<List<WorkEntryEntity>> = workEntryDao.observeAll()
    val attendanceFlow: Flow<List<AttendanceEntity>> = attendanceDao.observeAll()
    val advancesFlow: Flow<List<AdvanceEntity>> = advanceDao.observeAll()
    val paymentsFlow: Flow<List<PaymentEntity>> = paymentDao.observeAll()
    val remindersFlow: Flow<List<ReminderEntity>> = reminderDao.observeAll()

    fun observeWorker(workerId: Long): Flow<WorkerEntity?> = workerDao.observeById(workerId)

    suspend fun saveWorker(worker: WorkerEntity): Long {
        return if (worker.id == 0L) {
            val code = worker.workerCode.ifBlank { generateNextWorkerCode() }
            workerDao.insert(worker.copy(workerCode = code))
        } else {
            workerDao.update(worker)
            worker.id
        }
    }

    private suspend fun generateNextWorkerCode(): String {
        val existing = workerDao.getAllOnce()
        val nextNum = (existing.maxOfOrNull { it.id } ?: 0L) + 1L
        return "HB-${nextNum.toString().padStart(3, '0')}"
    }

    suspend fun deleteWorker(workerId: Long) {
        workerDao.deleteById(workerId)
    }

    suspend fun saveWorkEntry(entry: WorkEntryEntity): Long {
        return if (entry.id == 0L) {
            workEntryDao.insert(entry)
        } else {
            workEntryDao.update(entry.copy(updatedAt = System.currentTimeMillis()))
            entry.id
        }
    }

    suspend fun deleteWorkEntry(id: Long) {
        workEntryDao.deleteById(id)
    }

    suspend fun markAttendance(record: AttendanceEntity) {
        attendanceDao.upsert(record.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun clearAttendance(workerId: Long, dateKey: Long) {
        attendanceDao.deleteForWorkerAndDate(workerId, dateKey)
    }

    suspend fun saveAdvance(advance: AdvanceEntity): Long {
        return if (advance.id == 0L) {
            val receipt = advance.receiptNumber.ifBlank {
                "ADV-${advance.dateKey}-${(System.currentTimeMillis() % 10000).toString().padStart(4, '0')}"
            }
            advanceDao.insert(advance.copy(receiptNumber = receipt))
        } else {
            advanceDao.update(advance)
            advance.id
        }
    }

    suspend fun deleteAdvance(id: Long) {
        advanceDao.deleteById(id)
    }

    suspend fun savePayment(payment: PaymentEntity): Long {
        return if (payment.id == 0L) {
            paymentDao.insert(payment)
        } else {
            paymentDao.update(payment)
            payment.id
        }
    }

    suspend fun deletePayment(id: Long) {
        paymentDao.deleteById(id)
    }

    suspend fun saveReminder(reminder: ReminderEntity): Long {
        return if (reminder.id == 0L) reminderDao.insert(reminder)
        else {
            reminderDao.update(reminder)
            reminder.id
        }
    }

    suspend fun deleteReminder(id: Long) {
        reminderDao.deleteById(id)
    }

    suspend fun clearAllData() {
        db.withTransaction {
            reminderDao.deleteAll()
            paymentDao.deleteAll()
            advanceDao.deleteAll()
            attendanceDao.deleteAll()
            workEntryDao.deleteAll()
            workerDao.deleteAll()
        }
    }

    suspend fun copyPickerImageToInternal(uri: Uri, prefix: String): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "photos").apply { mkdirs() }
            val dest = File(dir, "${prefix}_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            dest.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject().apply {
            put("app", "HisabBookPro")
            put("schemaVersion", 1)
            put("exportedAt", System.currentTimeMillis())

            put("workers", JSONArray().apply {
                workerDao.getAllOnce().forEach { w ->
                    put(JSONObject().apply {
                        put("id", w.id)
                        put("workerCode", w.workerCode)
                        put("fullName", w.fullName)
                        put("mobileNumber", w.mobileNumber)
                        put("address", w.address)
                        put("joiningDateKey", w.joiningDateKey)
                        put("workCategory", w.workCategory)
                        put("dailyRatePaise", w.dailyRatePaise)
                        put("halfTimeRatePaise", w.halfTimeRatePaise)
                        put("fullTimeRatePaise", w.fullTimeRatePaise)
                        put("hourlyRatePaise", w.hourlyRatePaise)
                        put("notes", w.notes)
                        put("isActive", w.isActive)
                        put("createdAt", w.createdAt)
                    })
                }
            })

            put("workEntries", JSONArray().apply {
                workEntryDao.getAllOnce().forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("workerId", e.workerId)
                        put("dateKey", e.dateKey)
                        put("workType", e.workType)
                        put("startTime", e.startTime)
                        put("endTime", e.endTime)
                        put("totalHours", e.totalHours)
                        put("ratePaise", e.ratePaise)
                        put("totalAmountPaise", e.totalAmountPaise)
                        put("description", e.description)
                        put("notes", e.notes)
                        put("isApproved", e.isApproved)
                        put("createdAt", e.createdAt)
                        put("updatedAt", e.updatedAt)
                    })
                }
            })

            put("attendance", JSONArray().apply {
                attendanceDao.getAllOnce().forEach { a ->
                    put(JSONObject().apply {
                        put("id", a.id)
                        put("workerId", a.workerId)
                        put("dateKey", a.dateKey)
                        put("status", a.status)
                        put("hours", a.hours)
                        put("notes", a.notes)
                        put("updatedAt", a.updatedAt)
                    })
                }
            })

            put("advances", JSONArray().apply {
                advanceDao.getAllOnce().forEach { adv ->
                    put(JSONObject().apply {
                        put("id", adv.id)
                        put("workerId", adv.workerId)
                        put("dateKey", adv.dateKey)
                        put("amountPaise", adv.amountPaise)
                        put("paymentMethod", adv.paymentMethod)
                        put("referenceNumber", adv.referenceNumber)
                        put("receiptNumber", adv.receiptNumber)
                        put("description", adv.description)
                        put("notes", adv.notes)
                        put("createdAt", adv.createdAt)
                    })
                }
            })

            put("payments", JSONArray().apply {
                paymentDao.getAllOnce().forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("workerId", p.workerId)
                        put("dateKey", p.dateKey)
                        put("amountPaise", p.amountPaise)
                        put("paymentType", p.paymentType)
                        put("paymentMethod", p.paymentMethod)
                        put("transactionId", p.transactionId)
                        put("notes", p.notes)
                        put("createdAt", p.createdAt)
                    })
                }
            })

            put("reminders", JSONArray().apply {
                reminderDao.getAllOnce().forEach { r ->
                    put(JSONObject().apply {
                        put("id", r.id)
                        if (r.workerId != null) put("workerId", r.workerId)
                        put("title", r.title)
                        put("type", r.type)
                        put("dueDateKey", r.dueDateKey)
                        put("amountPaise", r.amountPaise)
                        put("isCompleted", r.isCompleted)
                        put("notes", r.notes)
                        put("createdAt", r.createdAt)
                    })
                }
            })
        }
        root.toString(2)
    }

    suspend fun restoreFromBackupJson(jsonText: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonText)
            if (root.optString("app") != "HisabBookPro") {
                return@withContext Result.failure(IllegalArgumentException("Invalid backup file format"))
            }

            val workersArr = root.optJSONArray("workers") ?: JSONArray()
            val workArr = root.optJSONArray("workEntries") ?: JSONArray()
            val attArr = root.optJSONArray("attendance") ?: JSONArray()
            val advArr = root.optJSONArray("advances") ?: JSONArray()
            val payArr = root.optJSONArray("payments") ?: JSONArray()
            val remArr = root.optJSONArray("reminders") ?: JSONArray()

            db.withTransaction {
                reminderDao.deleteAll()
                paymentDao.deleteAll()
                advanceDao.deleteAll()
                attendanceDao.deleteAll()
                workEntryDao.deleteAll()
                workerDao.deleteAll()

                val workerIdMap = mutableMapOf<Long, Long>()
                for (i in 0 until workersArr.length()) {
                    val o = workersArr.getJSONObject(i)
                    val oldId = o.getLong("id")
                    val insertedId = workerDao.insert(
                        WorkerEntity(
                            id = 0L,
                            workerCode = o.optString("workerCode", "HB-${i + 1}"),
                            fullName = o.getString("fullName"),
                            mobileNumber = o.optString("mobileNumber", ""),
                            address = o.optString("address", ""),
                            joiningDateKey = o.getLong("joiningDateKey"),
                            workCategory = o.optString("workCategory", "General"),
                            dailyRatePaise = o.optLong("dailyRatePaise", 0L),
                            halfTimeRatePaise = o.optLong("halfTimeRatePaise", 0L),
                            fullTimeRatePaise = o.optLong("fullTimeRatePaise", 0L),
                            hourlyRatePaise = o.optLong("hourlyRatePaise", 0L),
                            photoUri = null,
                            notes = o.optString("notes", ""),
                            isActive = o.optBoolean("isActive", true),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                    workerIdMap[oldId] = insertedId
                }

                for (i in 0 until workArr.length()) {
                    val o = workArr.getJSONObject(i)
                    val newWorkerId = workerIdMap[o.getLong("workerId")] ?: continue
                    workEntryDao.insert(
                        WorkEntryEntity(
                            id = 0L,
                            workerId = newWorkerId,
                            dateKey = o.getLong("dateKey"),
                            workType = o.getString("workType"),
                            startTime = o.optString("startTime", ""),
                            endTime = o.optString("endTime", ""),
                            totalHours = o.optDouble("totalHours", 0.0),
                            ratePaise = o.getLong("ratePaise"),
                            totalAmountPaise = o.getLong("totalAmountPaise"),
                            description = o.optString("description", ""),
                            notes = o.optString("notes", ""),
                            isApproved = o.optBoolean("isApproved", true),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }

                for (i in 0 until attArr.length()) {
                    val o = attArr.getJSONObject(i)
                    val newWorkerId = workerIdMap[o.getLong("workerId")] ?: continue
                    attendanceDao.upsert(
                        AttendanceEntity(
                            id = 0L,
                            workerId = newWorkerId,
                            dateKey = o.getLong("dateKey"),
                            status = o.getString("status"),
                            hours = o.optDouble("hours", 0.0),
                            notes = o.optString("notes", ""),
                            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }

                for (i in 0 until advArr.length()) {
                    val o = advArr.getJSONObject(i)
                    val newWorkerId = workerIdMap[o.getLong("workerId")] ?: continue
                    advanceDao.insert(
                        AdvanceEntity(
                            id = 0L,
                            workerId = newWorkerId,
                            dateKey = o.getLong("dateKey"),
                            amountPaise = o.getLong("amountPaise"),
                            paymentMethod = o.optString("paymentMethod", "CASH"),
                            referenceNumber = o.optString("referenceNumber", ""),
                            receiptNumber = o.optString("receiptNumber", ""),
                            description = o.optString("description", ""),
                            notes = o.optString("notes", ""),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }

                for (i in 0 until payArr.length()) {
                    val o = payArr.getJSONObject(i)
                    val newWorkerId = workerIdMap[o.getLong("workerId")] ?: continue
                    paymentDao.insert(
                        PaymentEntity(
                            id = 0L,
                            workerId = newWorkerId,
                            dateKey = o.getLong("dateKey"),
                            amountPaise = o.getLong("amountPaise"),
                            paymentType = o.optString("paymentType", "PARTIAL_PAYMENT"),
                            paymentMethod = o.optString("paymentMethod", "CASH"),
                            transactionId = o.optString("transactionId", ""),
                            notes = o.optString("notes", ""),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }

                for (i in 0 until remArr.length()) {
                    val o = remArr.getJSONObject(i)
                    val oldWid = if (o.has("workerId")) o.optLong("workerId") else null
                    val mappedWid = oldWid?.let { workerIdMap[it] }
                    reminderDao.insert(
                        ReminderEntity(
                            id = 0L,
                            workerId = mappedWid,
                            title = o.getString("title"),
                            type = o.optString("type", "PENDING_PAYMENT"),
                            dueDateKey = o.getLong("dueDateKey"),
                            amountPaise = o.optLong("amountPaise", 0L),
                            isCompleted = o.optBoolean("isCompleted", false),
                            notes = o.optString("notes", ""),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
            Result.success(workersArr.length())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
