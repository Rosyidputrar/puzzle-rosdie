package id.rosdie.puzzle.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [GameResultEntity::class, SavedGameEntity::class, AchievementEntity::class, CustomImageEntity::class],
    version = PuzzleDatabase.VERSION,
    exportSchema = true,
)
abstract class PuzzleDatabase : RoomDatabase() {
    abstract fun results(): GameResultDao
    abstract fun savedGame(): SavedGameDao
    abstract fun achievements(): AchievementDao
    abstract fun customImages(): CustomImageDao

    companion object {
        const val VERSION = 1
        const val NAME = "puzzle_rosdie.db"

        /**
         * Migrasi eksplisit untuk setiap kenaikan versi skema. Skema diekspor ke app/schemas
         * dan diuji dengan MigrationTestHelper. Tidak ada fallbackToDestructiveMigration:
         * data pemain tidak boleh hilang saat update.
         */
        val MIGRATIONS: Array<Migration> = emptyArray()

        fun create(context: Context): PuzzleDatabase =
            Room.databaseBuilder(context.applicationContext, PuzzleDatabase::class.java, NAME)
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
