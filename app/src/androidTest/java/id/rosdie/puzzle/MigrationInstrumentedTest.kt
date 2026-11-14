package id.rosdie.puzzle

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.rosdie.puzzle.data.local.PuzzleDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validasi skema Room yang diekspor (app/schemas). Saat versi skema naik, tambahkan
 * Migration ke PuzzleDatabase.MIGRATIONS dan uji di sini dengan runMigrationsAndValidate.
 */
@RunWith(AndroidJUnit4::class)
class MigrationInstrumentedTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), PuzzleDatabase::class.java)

    @Test
    fun allMigrationsReachCurrentVersion() {
        helper.createDatabase(DB, 1).close()
        helper.runMigrationsAndValidate(DB, PuzzleDatabase.VERSION, true, *PuzzleDatabase.MIGRATIONS)
    }

    private companion object {
        const val DB = "migration-test"
    }
}
