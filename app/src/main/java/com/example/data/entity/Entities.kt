package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "workers")
data class WorkerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val workerCode: String,
    val fullName: String,
    val mobileNumber: String,
    val address: String = "",
    val joiningDateKey: Long,
    val workCategory: String,
    val dailyRatePaise: Long = 0L,
    val halfTimeRatePaise: Long = 0L,
    val fullTimeRatePaise: Long = 0L,
    val hourlyRatePaise: Long = 0L,
    val photoUri: String? = null,
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "work_entries",
    foreignKeys = [
        ForeignKey(
            entity = WorkerEntity::class,
            parentColumns = ["id"],
            childColumns = ["workerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workerId"), Index("dateKey")]
)
data class WorkEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val workerId: Long,
    val dateKey: Long,
    val workType: String,
    val startTime: String = "",
    val endTime: String = "",
    val totalHours: Double = 0.0,
    val ratePaise: Long,
    val totalAmountPaise: Long,
    val description: String = "",
    val notes: String = "",
    val isApproved: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = WorkerEntity::class,
            parentColumns = ["id"],
            childColumns = ["workerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("workerId"),
        Index("dateKey"),
        Index(value = ["workerId", "dateKey"], unique = true)
    ]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val workerId: Long,
    val dateKey: Long,
    val status: String,
    val hours: Double = 0.0,
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "advances",
    foreignKeys = [
        ForeignKey(
            entity = WorkerEntity::class,
            parentColumns = ["id"],
            childColumns = ["workerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workerId"), Index("dateKey")]
)
data class AdvanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val workerId: Long,
    val dateKey: Long,
    val amountPaise: Long,
    val paymentMethod: String,
    val referenceNumber: String = "",
    val receiptNumber: String = "",
    val description: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = WorkerEntity::class,
            parentColumns = ["id"],
            childColumns = ["workerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workerId"), Index("dateKey")]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val workerId: Long,
    val dateKey: Long,
    val amountPaise: Long,
    val paymentType: String,
    val paymentMethod: String,
    val transactionId: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reminders",
    indices = [Index("workerId"), Index("dueDateKey")]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val workerId: Long? = null,
    val title: String,
    val type: String,
    val dueDateKey: Long,
    val amountPaise: Long = 0L,
    val isCompleted: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
