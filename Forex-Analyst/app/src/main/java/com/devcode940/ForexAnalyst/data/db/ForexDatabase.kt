package com.devcode940.ForexAnalyst.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.devcode940.ForexAnalyst.data.local.HistoricalForexPriceEntity
import com.devcode940.ForexAnalyst.data.local.HistoricalPriceDao

@Database(
    entities = [
        SignalEntity::class,
        AlertEntity::class,
        WatchlistEntity::class,
        CandleEntity::class,
        HistoricalForexPriceEntity::class,
        UserProfileEntity::class,
        UserSettingsEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class ForexDatabase : RoomDatabase() {
    abstract fun forexDao(): ForexDao
    abstract fun historicalPriceDao(): HistoricalPriceDao

    companion object {
        @Volatile
        private var INSTANCE: ForexDatabase? = null

        fun getDatabase(context: Context): ForexDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ForexDatabase::class.java,
                    "forex_analyzer_db"
                ).addMigrations(MIGRATION_5_6).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE saved_signals ADD COLUMN predictionOutcome TEXT NOT NULL DEFAULT 'PENDING'")
                db.execSQL("ALTER TABLE saved_signals ADD COLUMN outcomeAt INTEGER")
                db.execSQL("ALTER TABLE saved_signals ADD COLUMN outcomePrice REAL")
            }
        }
    }
}
