package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.Course
import com.example.data.model.PeriodSchedule

class Converters {
    @TypeConverter
    fun fromStatus(status: AttendanceStatus?): String {
        return status?.name ?: AttendanceStatus.PRESENT.name
    }

    @TypeConverter
    fun toStatus(value: String?): AttendanceStatus {
        return try {
            if (value != null) AttendanceStatus.valueOf(value) else AttendanceStatus.PRESENT
        } catch (e: Exception) {
            AttendanceStatus.PRESENT
        }
    }
}

@Database(
    entities = [Course::class, AttendanceRecord::class, PeriodSchedule::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attendtrack_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
