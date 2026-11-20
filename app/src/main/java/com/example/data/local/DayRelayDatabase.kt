package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.AlertMode
import com.example.data.model.BridgeProviderType
import com.example.data.model.DailyNoteEntity
import com.example.data.model.DeliveryLogEntity
import com.example.data.model.DeliveryStatus
import com.example.data.model.HabitCompletionEntity
import com.example.data.model.PlannerItemEntity
import com.example.data.model.PlannerItemType
import com.example.data.model.PriorityLevel

class PlannerConverters {
    @TypeConverter
    fun fromItemType(value: PlannerItemType): String = value.name

    @TypeConverter
    fun toItemType(value: String): PlannerItemType =
        runCatching { PlannerItemType.valueOf(value) }.getOrDefault(PlannerItemType.TASK)

    @TypeConverter
    fun fromAlertMode(value: AlertMode): String = value.name

    @TypeConverter
    fun toAlertMode(value: String): AlertMode =
        runCatching { AlertMode.valueOf(value) }.getOrDefault(AlertMode.SNOOZE)

    @TypeConverter
    fun fromPriority(value: PriorityLevel): String = value.name

    @TypeConverter
    fun toPriority(value: String): PriorityLevel =
        runCatching { PriorityLevel.valueOf(value) }.getOrDefault(PriorityLevel.MEDIUM)

    @TypeConverter
    fun fromProvider(value: BridgeProviderType): String = value.name

    @TypeConverter
    fun toProvider(value: String): BridgeProviderType =
        runCatching { BridgeProviderType.valueOf(value) }.getOrDefault(BridgeProviderType.NTFY)

    @TypeConverter
    fun fromDeliveryStatus(value: DeliveryStatus): String = value.name

    @TypeConverter
    fun toDeliveryStatus(value: String): DeliveryStatus =
        runCatching { DeliveryStatus.valueOf(value) }.getOrDefault(DeliveryStatus.FAILED)
}

@Database(
    entities = [
        PlannerItemEntity::class,
        HabitCompletionEntity::class,
        DailyNoteEntity::class,
        DeliveryLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(PlannerConverters::class)
abstract class DayRelayDatabase : RoomDatabase() {
    abstract fun plannerDao(): PlannerDao

    companion object {
        @Volatile
        private var INSTANCE: DayRelayDatabase? = null

        fun getInstance(context: Context): DayRelayDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DayRelayDatabase::class.java,
                    "dayrelay_database"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
