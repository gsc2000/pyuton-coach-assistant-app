package com.example.swimminganalysisapplication.data.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Application Room Database for storing projects.
 */
@Database(entities = [ProjectEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 1 to 2: Add alignment point and offset columns
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE projects ADD COLUMN alignmentPoint1Ms INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE projects ADD COLUMN alignmentPoint2Ms INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE projects ADD COLUMN offset1Ms INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE projects ADD COLUMN offset2Ms INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Migration from version 2 to 3: Consolidate offset1Ms and offset2Ms into offsetMs
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new table with the updated schema
                database.execSQL("""
                    CREATE TABLE projects_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        videoUri1 TEXT,
                        videoUri2 TEXT,
                        startPosition1Ms INTEGER NOT NULL,
                        startPosition2Ms INTEGER NOT NULL,
                        alignmentPoint1Ms INTEGER NOT NULL DEFAULT 0,
                        alignmentPoint2Ms INTEGER NOT NULL DEFAULT 0,
                        offsetMs INTEGER NOT NULL DEFAULT 0,
                        drawingsJson TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Copy data from old table to new table
                // For old projects (where alignmentPoint1Ms is still 0), keep the original startPosition
                // For new projects (where alignmentPoint1Ms is set), recalculate startPosition from alignmentPoint + offset
                database.execSQL("""
                    INSERT INTO projects_new (id, name, videoUri1, videoUri2, startPosition1Ms, startPosition2Ms, 
                        alignmentPoint1Ms, alignmentPoint2Ms, offsetMs, drawingsJson, createdAt, updatedAt)
                    SELECT id, name, videoUri1, videoUri2, 
                        CASE 
                            WHEN alignmentPoint1Ms > 0 THEN alignmentPoint1Ms + offset1Ms
                            ELSE startPosition1Ms
                        END,
                        CASE 
                            WHEN alignmentPoint2Ms > 0 THEN alignmentPoint2Ms + offset1Ms
                            ELSE startPosition2Ms
                        END,
                        alignmentPoint1Ms, alignmentPoint2Ms, offset1Ms, drawingsJson, createdAt, updatedAt
                    FROM projects
                """.trimIndent())
                
                // Drop old table
                database.execSQL("DROP TABLE projects")
                
                // Rename new table to original name
                database.execSQL("ALTER TABLE projects_new RENAME TO projects")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
