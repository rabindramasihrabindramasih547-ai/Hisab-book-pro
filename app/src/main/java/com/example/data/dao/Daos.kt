package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.AdvanceEntity
import com.example.data.entity.AttendanceEntity
import com.example.data.entity.PaymentEntity
import com.example.data.entity.ReminderEntity
import com.example.data.entity.WorkEntryEntity
import com.example.data.entity.WorkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers ORDER BY fullName COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers ORDER BY id ASC")
    suspend fun getAllOnce(): List<WorkerEntity>

    @Query("SELECT * FROM workers WHERE id = :id")
    fun observeById(id: Long): Flow<WorkerEntity?>

    @Query("SELECT * FROM workers WHERE id = :id")
    suspend fun getById(id: Long): WorkerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(worker: WorkerEntity): Long

    @Update
    suspend fun update(worker: WorkerEntity)

    @Delete
    suspend fun delete(worker: WorkerEntity)

    @Query("DELETE FROM workers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM workers")
    suspend fun deleteAll()
}

@Dao
interface WorkEntryDao {
    @Query("SELECT * FROM work_entries ORDER BY dateKey DESC, createdAt DESC")
    fun observeAll(): Flow<List<WorkEntryEntity>>

    @Query("SELECT * FROM work_entries ORDER BY id ASC")
    suspend fun getAllOnce(): List<WorkEntryEntity>

    @Query("SELECT * FROM work_entries WHERE workerId = :workerId ORDER BY dateKey DESC, createdAt DESC")
    fun observeByWorker(workerId: Long): Flow<List<WorkEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WorkEntryEntity): Long

    @Update
    suspend fun update(entry: WorkEntryEntity)

    @Query("DELETE FROM work_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM work_entries")
    suspend fun deleteAll()
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY dateKey DESC")
    fun observeAll(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records ORDER BY id ASC")
    suspend fun getAllOnce(): List<AttendanceEntity>

    @Query("SELECT * FROM attendance_records WHERE dateKey = :dateKey")
    fun observeByDate(dateKey: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records WHERE workerId = :workerId ORDER BY dateKey DESC")
    fun observeByWorker(workerId: Long): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: AttendanceEntity): Long

    @Query("DELETE FROM attendance_records WHERE workerId = :workerId AND dateKey = :dateKey")
    suspend fun deleteForWorkerAndDate(workerId: Long, dateKey: Long)

    @Query("DELETE FROM attendance_records")
    suspend fun deleteAll()
}

@Dao
interface AdvanceDao {
    @Query("SELECT * FROM advances ORDER BY dateKey DESC, createdAt DESC")
    fun observeAll(): Flow<List<AdvanceEntity>>

    @Query("SELECT * FROM advances ORDER BY id ASC")
    suspend fun getAllOnce(): List<AdvanceEntity>

    @Query("SELECT * FROM advances WHERE workerId = :workerId ORDER BY dateKey DESC, createdAt DESC")
    fun observeByWorker(workerId: Long): Flow<List<AdvanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(advance: AdvanceEntity): Long

    @Update
    suspend fun update(advance: AdvanceEntity)

    @Query("DELETE FROM advances WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM advances")
    suspend fun deleteAll()
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY dateKey DESC, createdAt DESC")
    fun observeAll(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY id ASC")
    suspend fun getAllOnce(): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE workerId = :workerId ORDER BY dateKey DESC, createdAt DESC")
    fun observeByWorker(workerId: Long): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentEntity): Long

    @Update
    suspend fun update(payment: PaymentEntity)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM payments")
    suspend fun deleteAll()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, dueDateKey ASC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders ORDER BY id ASC")
    suspend fun getAllOnce(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}
