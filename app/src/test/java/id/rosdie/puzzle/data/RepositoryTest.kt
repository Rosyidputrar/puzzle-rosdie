package id.rosdie.puzzle.data

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import androidx.core.graphics.createBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.rosdie.puzzle.FakeTimeSource
import id.rosdie.puzzle.data.images.BuiltInImage
import id.rosdie.puzzle.data.images.ImageError
import id.rosdie.puzzle.data.images.ImageIds
import id.rosdie.puzzle.data.images.ImageRepository
import id.rosdie.puzzle.data.images.ImageResult
import id.rosdie.puzzle.data.local.PuzzleDatabase
import id.rosdie.puzzle.data.repository.Achievement
import id.rosdie.puzzle.data.repository.AchievementRepository
import id.rosdie.puzzle.data.repository.GameConfig
import id.rosdie.puzzle.data.repository.GameRepository
import id.rosdie.puzzle.data.repository.SavedGame
import id.rosdie.puzzle.data.repository.UndoEntry
import id.rosdie.puzzle.domain.engine.PuzzleEngine
import id.rosdie.puzzle.domain.model.ChallengeType
import id.rosdie.puzzle.domain.model.GameMode
import id.rosdie.puzzle.domain.model.GameOutcome
import id.rosdie.puzzle.domain.model.GameResult
import id.rosdie.puzzle.inMemoryDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RepositoryTest {
    private lateinit var db: PuzzleDatabase
    private lateinit var games: GameRepository
    private val time = FakeTimeSource()
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        db = inMemoryDb(context)
        games = GameRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private fun win(moves: Int, time: Long, hint: Boolean = false, ai: Boolean = false) = GameResult(
        gridSize = 4, mode = GameMode.NUMBERS, moves = moves, timeSeconds = time, usedAi = ai, usedHint = hint,
        stars = 3, challengeType = ChallengeType.NORMAL, dailyDate = null,
        outcome = if (ai) GameOutcome.AI_SOLVED else GameOutcome.WON, playedAt = this.time.nowMs(),
    )

    @Test
    fun recordsOnlyCountPureWins() = runBlocking {
        val first = games.recordResult(win(150, 120))
        assertNull(first.previous)
        assertTrue(first.newMovesRecord && first.newTimeRecord)
        val hinted = games.recordResult(win(80, 60, hint = true))
        assertFalse(hinted.eligible)
        assertFalse(hinted.newMovesRecord)
        val second = games.recordResult(win(140, 130))
        assertEquals(150, second.previous!!.bestMoves)
        assertTrue(second.newMovesRecord)
        assertFalse(second.newTimeRecord)
        games.recordResult(win(10, 5, ai = true))
        val record = games.bestRecord(4)!!
        assertEquals(140, record.bestMoves)
        assertEquals(120L, record.bestTimeSeconds)
        assertEquals(4, games.getAllResults().size)
    }

    @Test
    fun savedGameRoundTripAndAbandonOnReplace() = runBlocking {
        val board = PuzzleEngine.shuffle(4, 1)
        val saved = SavedGame(
            sessionId = "A",
            config = GameConfig(4, GameMode.IMAGE, ImageIds.builtIn("sunset"), ChallengeType.TARGET_MOVES, null),
            board = board,
            initial = board,
            moves = 12,
            elapsedMs = 34_000,
            undo = List(12) { UndoEntry(board.toList(), 1) },
            paused = true,
            usedAi = false,
            usedHint = true,
            savedAt = 1,
        )
        games.saveGame(saved)
        val loaded = games.loadSavedGame()!!
        assertEquals(saved, loaded)
        // Sesi lain menyimpan: sesi lama tercatat ABANDONED.
        games.saveGame(saved.copy(sessionId = "B", moves = 1))
        val all = games.getAllResults()
        assertEquals(1, all.size)
        assertEquals(GameOutcome.ABANDONED, all[0].outcome)
        assertEquals(12, all[0].moves)
        games.clearSavedGame("A")
        assertNotNull(games.loadSavedGame())
        games.clearSavedGame("B")
        assertNull(games.loadSavedGame())
    }

    @Test
    fun achievementsUnlockOnceWithDate() = runBlocking {
        val repo = AchievementRepository(db.achievements(), time)
        val r = listOf(win(90, 60))
        val first = repo.evaluate(r, 0)
        assertEquals(listOf(Achievement.FAST_4X4, Achievement.PURE_WIN), first)
        assertTrue(repo.evaluate(r, 0).isEmpty())
        val unlocked = repo.unlocked.first()
        assertEquals(time.nowMs(), unlocked[Achievement.FAST_4X4])
    }

    @Test
    fun imageValidationAndImport() = runBlocking {
        val repo = ImageRepository(context, db.customImages(), time, Dispatchers.Unconfined)
        fun png(w: Int, h: Int): ByteArray {
            val b: Bitmap = createBitmap(w, h).apply { eraseColor(Color.RED) }
            return ByteArrayOutputStream().also { b.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        }
        assertEquals(ImageResult.Failure(ImageError.UNSUPPORTED), repo.decode("bukan gambar sama sekali".toByteArray()))
        assertEquals(ImageResult.Failure(ImageError.CORRUPT), repo.decode(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 1, 2, 3)))
        assertEquals(ImageResult.Failure(ImageError.TOO_SMALL), repo.decode(png(20, 20)))
        val ok = repo.decode(png(1600, 1200))
        assertTrue(ok is ImageResult.Success)
        val bmp = (ok as ImageResult.Success).value
        val imported = repo.importCropped(bmp, Rect(100, 0, 1300, 1200))
        assertTrue(imported is ImageResult.Success)
        val img = (imported as ImageResult.Success).value
        assertTrue(File(img.path).exists())
        assertEquals(img.sizeBytes, File(img.path).length())
        assertEquals(1, repo.customCount())
        val board = repo.loadBoardBitmap(ImageIds.custom(img.id))!!
        assertEquals(ImageRepository.STORED_SIZE_PX, board.width)
        assertEquals(ImageRepository.STORED_SIZE_PX, board.height)
        assertNotNull(repo.loadBoardBitmap(BuiltInImage.MOSAIC.imageId))
        repo.delete(img.id)
        assertFalse(File(img.path).exists())
        assertEquals(0, repo.customCount())
    }

    @Test
    fun builtInImagesRenderDistinctPixels() = runBlocking {
        val repo = ImageRepository(context, db.customImages(), time, Dispatchers.Unconfined)
        for (img in BuiltInImage.entries) {
            val b = repo.builtInThumbnail(img, 64)
            assertEquals(64, b.width)
        }
    }
}
