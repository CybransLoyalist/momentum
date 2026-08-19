package dev.slusa.momentum.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Todo::class, Habit::class, HabitCompletion::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = DropFirstTodayDate::class),
        AutoMigration(from = 2, to = 3),
    ],
)
@TypeConverters(Converters::class)
abstract class MomentumDatabase : RoomDatabase() {

    abstract fun todoDao(): TodoDao

    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile
        private var instance: MomentumDatabase? = null

        fun get(context: Context): MomentumDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MomentumDatabase::class.java,
                    "momentum.db",
                ).build().also { instance = it }
            }
    }
}
