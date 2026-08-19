package dev.slusa.momentum.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Todo::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = DropFirstTodayDate::class),
    ],
)
@TypeConverters(Converters::class)
abstract class MomentumDatabase : RoomDatabase() {

    abstract fun todoDao(): TodoDao

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
