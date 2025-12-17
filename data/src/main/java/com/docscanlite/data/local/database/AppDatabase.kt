package com.docscanlite.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.docscanlite.data.local.converter.FloatListConverter
import com.docscanlite.data.local.converter.StringListConverter
import com.docscanlite.data.local.dao.DocumentDao
import com.docscanlite.data.local.entity.DocumentEntity

/**
 * Main Room database for DocScan Lite
 */
@Database(
    entities = [DocumentEntity::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(StringListConverter::class, FloatListConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao

    companion object {
        const val DATABASE_NAME = "docscan_lite.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE documents ADD COLUMN bounds TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns for editing settings
                db.execSQL("ALTER TABLE documents ADD COLUMN filterName TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE documents ADD COLUMN brightness REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE documents ADD COLUMN contrast REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE documents ADD COLUMN saturation REAL NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE documents ADD COLUMN cropBounds TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE documents ADD COLUMN rotationAngle REAL NOT NULL DEFAULT 0")
            }
        }
    }
}
