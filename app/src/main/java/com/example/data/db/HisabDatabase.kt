package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AdvanceDao
import com.example.data.dao.AttendanceDao
import com.example.data.dao.PaymentDao
import com.example.data.dao.ReminderDao
import com.example.data.dao.WorkEntryDao
import com.example.data.dao.WorkerDao
import com.example.data.entity.AdvanceEntity
import com.example.data.entity.AttendanceEntity
import com.example.data.entity.PaymentEntity
import com.example.data.entity.ReminderEntity
import com.example.data.entity.WorkEntryEntity
import com.example.data.entity.WorkerEntity

@Database(
    entities = [
        WorkerEntity::class,
        WorkEntryEntity::class,
        AttendanceEntity::class,
        AdvanceEntity::class,
        PaymentEntity::class,
        ReminderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HisabDatabase : RoomDatabase() {
    abstract fun workerDao(): WorkerDao
    abstract fun workEntryDao(): WorkEntryDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun advanceDao(): AdvanceDao
    abstract fun paymentDao(): PaymentDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: HisabDatabase? = null

        // Future-proof migration scaffolding when schema evolves past v1
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Reserved for future schema upgrades
            }
        }

        fun getInstance(context: Context): HisabDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HisabDatabase::class.java,
                    "hisabbook_pro.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
