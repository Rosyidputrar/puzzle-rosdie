package id.rosdie.puzzle.ui

import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.rosdie.puzzle.FakeTimeSource
import id.rosdie.puzzle.data.datastore.AppSettings
import id.rosdie.puzzle.data.images.ImageRepository
import id.rosdie.puzzle.data.local.PuzzleDatabase
import id.rosdie.puzzle.data.repository.AchievementRepository
import id.rosdie.puzzle.data.repository.GameRepository
import id.rosdie.puzzle.domain.engine.PuzzleEngine
import id.rosdie.puzzle.domain.model.ChallengeRules
import id.rosdie.puzzle.domain.model.ChallengeType
import id.rosdie.puzzle.domain.model.GameMode
import id.rosdie.puzzle.domain.model.GameOutcome
import id.rosdie.puzzle.domain.solver.PuzzleSolver
import id.rosdie.puzzle.domain.solver.SolverResult
import id.rosdie.puzzle.feedback.GameFeedback
import id.rosdie.puzzle.inMemoryDb
import id.rosdie.puzzle.ui.game.AiUi
import id.rosdie.puzzle.ui.game.GameStatus
import id.rosdie.puzzle.ui.game.GameViewModel
import id.rosdie.puzzle.ui.game.HintUi
import id.rosdie.puzzle.ui.game.LossReason
import id.rosdie.puzzle.ui.navigation.GameRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class GameViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private lateinit var db: PuzzleDatabase
    private lateinit var games: GameRepository
    private val time = FakeTimeSource()
    private val settings = MutableStateFlow(AppSettings())
    private val store = ViewModelStore()
    private var invalids = 0
    private var wins = 0
    private val feedback = object : GameFeedback {
        override fun move() = Unit
        override fun invalid() { invalids++ }
        override fun aiStep() = Unit
        override fun win() { wins++ }
        override fun lose() = Unit
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        db = inMemoryDb(ApplicationProvider.getApplicationContext(), dispatcher)
        games = GameRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun vm(route: GameRoute, seed: Long = 42, solver: PuzzleSolver = PuzzleSolver(dispatcher, nanoClock = { time.elapsed * 1_000_000 })): GameViewModel {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return GameViewModel(
            route = route,
            games = games,
            achievements = AchievementRepository(db.achievements(), time),
            images = ImageRepository(context, db.customImages(), time, dispatcher),
            settings = settings,
            updateSettings = { t -> settings.value = t(settings.value) },
            solver = solver,
            feedback = feedback,
            time = time,
            persistScope = CoroutineScope(dispatcher + SupervisorJob()),
            seeds = { seed },
        ).also {
            store.put("vm${store.keys().size}", it)
            scope.runCurrent()
        }
    }

    /** runTest yang membersihkan ViewModel di akhir (ticker timer berhenti). */
    private fun test(block: suspend TestScope.() -> Unit): TestResult = scope.runTest {
        try {
            block()
        } finally {
            store.clear()
        }
    }

    private fun GameViewModel.solveFully() {
        val r = runBlocking { PuzzleSolver(Dispatchers.Default).solve(state.value.board) } as SolverResult.Solved
        r.moves.forEach { onSwipe(it) }
    }

    @Test
    fun newGameUsesSeededShuffleAndStartsReady() = test {
        val m = vm(GameRoute(size = 3))
        val s = m.state.value
        assertFalse(s.loading)
        assertEquals(PuzzleEngine.shuffle(3, 42), s.board)
        assertEquals(GameStatus.READY, s.status)
        assertEquals(0, s.moves)
    }

    @Test
    fun movesTimerUndoAndReset() = test {
        val m = vm(GameRoute(size = 4))
        val start = m.state.value.board
        val dir = PuzzleEngine.validMoves(start).first()
        m.onSwipe(dir)
        assertEquals(GameStatus.PLAYING, m.state.value.status)
        assertEquals(1, m.state.value.moves)
        time.advance(5_000)
        advanceTimeBy(400)
        assertEquals(5_000L, m.state.value.elapsedMs)
        // Undo memulihkan papan tapi langkah BERTAMBAH.
        m.undo()
        assertEquals(start, m.state.value.board)
        assertEquals(2, m.state.value.moves)
        assertFalse(m.state.value.canUndo)
        // Ketuk tile yang tidak sejajar dengan kotak kosong: tidak valid.
        val blocked = (0 until 16).first { PuzzleEngine.lineMoves(start, it).isEmpty() && start[it] != 0 }
        m.onTileTap(blocked)
        assertEquals(1, invalids)
        // Ketuk barisan: beberapa tile bergeser, setiap tile = satu langkah.
        val line = (0 until 16).first { PuzzleEngine.lineMoves(start, it).size >= 2 }
        val count = PuzzleEngine.lineMoves(start, line).size
        m.onTileTap(line)
        assertEquals(2 + count, m.state.value.moves)
        m.reset()
        assertEquals(start, m.state.value.board)
        assertEquals(0, m.state.value.moves)
        assertEquals(0L, m.state.value.elapsedMs)
        assertEquals(GameStatus.READY, m.state.value.status)
    }

    @Test
    fun pauseStopsTimerAndHidesBoard() = test {
        val m = vm(GameRoute(size = 3))
        m.onSwipe(PuzzleEngine.validMoves(m.state.value.board).first())
        time.advance(2_000)
        m.togglePause()
        assertEquals(GameStatus.PAUSED, m.state.value.status)
        assertFalse(m.state.value.interactive)
        time.advance(60_000)
        advanceTimeBy(1_000)
        assertEquals(2_000L, m.state.value.elapsedMs)
        val before = m.state.value.board
        m.onSwipe(PuzzleEngine.validMoves(before).first())
        assertEquals(before, m.state.value.board) // papan terkunci saat jeda
        m.togglePause()
        assertEquals(GameStatus.PLAYING, m.state.value.status)
    }

    @Test
    fun playerWinRecordsResultClearsSaveAndShowsSummary() = test {
        val m = vm(GameRoute(size = 3))
        m.onSwipe(PuzzleEngine.validMoves(m.state.value.board).first())
        advanceTimeBy(2_000) // simpan otomatis (debounce)
        runCurrent()
        assertNotNull(games.loadSavedGame())
        m.solveFully()
        advanceUntilIdle()
        val s = m.state.value
        assertEquals(GameStatus.WON, s.status)
        val win = s.win!!
        assertTrue(win.eligibleForRecord)
        assertTrue(win.newMovesRecord)
        assertEquals(s.moves, win.moves)
        assertEquals(1, wins)
        val results = games.getAllResults()
        assertEquals(1, results.size)
        assertEquals(GameOutcome.WON, results[0].outcome)
        assertNull(games.loadSavedGame())
        assertTrue(win.newAchievements.isNotEmpty())
    }

    @Test
    fun targetMovesChallengeIsLostWhenMovesRunOut() = test {
        val m = vm(GameRoute(size = 3, challenge = ChallengeType.TARGET_MOVES.name))
        val target = ChallengeRules.targetMoves(3)
        var guard = 0
        while (m.state.value.status != GameStatus.LOST && guard++ < 200) {
            val b = m.state.value.board
            // Bolak-balik dua langkah yang tidak pernah menyelesaikan puzzle.
            val d = PuzzleEngine.validMoves(b).first { !PuzzleEngine.move(b, it)!!.isSolved }
            m.onSwipe(d)
        }
        assertEquals(GameStatus.LOST, m.state.value.status)
        assertEquals(LossReason.OUT_OF_MOVES, m.state.value.lossReason)
        assertEquals(target, m.state.value.moves)
        advanceUntilIdle()
        assertEquals(GameOutcome.LOST, games.getAllResults().single().outcome)
    }

    @Test
    fun timeAttackIsLostWhenTimeRunsOut() = test {
        val m = vm(GameRoute(size = 3, challenge = ChallengeType.TIME_ATTACK.name))
        m.onSwipe(PuzzleEngine.validMoves(m.state.value.board).first())
        time.advance(ChallengeRules.timeLimitSeconds(3) * 1000)
        advanceTimeBy(500)
        assertEquals(GameStatus.LOST, m.state.value.status)
        assertEquals(LossReason.OUT_OF_TIME, m.state.value.lossReason)
        assertEquals(0L, m.state.value.remainingTimeMs)
    }

    @Test
    fun aiSolvesAndReportsComparison() = test {
        val m = vm(GameRoute(size = 3))
        m.onSwipe(PuzzleEngine.validMoves(m.state.value.board).first())
        m.solveWithAi()
        runCurrent()
        assertTrue(m.state.value.ai is AiUi.Executing)
        assertFalse(m.state.value.canUndo)
        advanceUntilIdle()
        val s = m.state.value
        assertEquals(GameStatus.AI_SOLVED, s.status)
        assertTrue(s.board.isSolved)
        val summary = s.aiSummary!!
        assertTrue(summary.optimal)
        assertEquals(1, summary.playerMoves)
        assertEquals(GameOutcome.AI_SOLVED, games.getAllResults().single().outcome)
        assertTrue(s.usedAi)
    }

    @Test
    fun hintHighlightsAMovableTileAndMarksGame() = test {
        val m = vm(GameRoute(size = 3))
        m.requestHint()
        advanceUntilIdle()
        val hint = m.state.value.hint as HintUi.Tile
        assertNotNull(PuzzleEngine.directionForTile(m.state.value.board, hint.index))
        assertTrue(m.state.value.usedHint)
        m.onTileTap(hint.index)
        assertEquals(HintUi.None, m.state.value.hint)
    }

    @Test
    fun saveAndResumeRestoresPositionUndoAndPausedState() = test {
        val m = vm(GameRoute(size = 4, mode = GameMode.NUMBERS.name))
        repeat(5) {
            val b = m.state.value.board
            m.onSwipe(PuzzleEngine.validMoves(b).first { d -> PuzzleEngine.move(b, d) != b })
        }
        time.advance(7_000)
        m.onAppBackgrounded()
        advanceUntilIdle()
        val expected = m.state.value
        val resumed = vm(GameRoute(resume = true))
        advanceUntilIdle()
        val r = resumed.state.value
        assertEquals(expected.board, r.board)
        assertEquals(expected.initial, r.initial)
        assertEquals(5, r.moves)
        assertEquals(7_000L, r.elapsedMs)
        assertEquals(GameStatus.PAUSED, r.status)
        assertTrue(r.canUndo)
    }

    @Test
    fun dailyPuzzleIsFixed4x4ForTheDate() = test {
        time.setDay(20261114)
        val m = vm(GameRoute(daily = true))
        assertEquals(PuzzleEngine.dailyPuzzle(20261114), m.state.value.board)
        assertEquals(20261114, m.state.value.config.dailyDate)
        val before = m.state.value.board
        m.shuffle()
        assertEquals(before, m.state.value.board) // Acak tidak berlaku untuk Puzzle Harian
        m.solveFully()
        advanceUntilIdle()
        assertEquals(20261114, games.getAllResults().single().dailyDate)
    }

    @Test
    fun changingDifficultyCreatesNewPuzzleAndAbandonsOld() = test {
        val m = vm(GameRoute(size = 4))
        m.onSwipe(PuzzleEngine.validMoves(m.state.value.board).first())
        m.changeDifficulty(5)
        advanceUntilIdle()
        assertEquals(5, m.state.value.size)
        assertEquals(0, m.state.value.moves)
        assertEquals(GameOutcome.ABANDONED, games.getAllResults().single().outcome)
        assertEquals(5, settings.value.difficulty.size)
    }

    @Test
    fun cancellingAiLeavesBoardUntouched() {
        // Solver nyata di Dispatchers.Default. Seed 14 menghasilkan 4x4 yang butuh > 3 detik.
        val main = UnconfinedTestDispatcher()
        Dispatchers.setMain(main)
        val m = GameViewModel(
            route = GameRoute(size = 4),
            games = games,
            achievements = AchievementRepository(db.achievements(), time),
            images = ImageRepository(ApplicationProvider.getApplicationContext(), db.customImages(), time, main),
            settings = settings,
            updateSettings = {},
            solver = PuzzleSolver(Dispatchers.Default),
            feedback = feedback,
            time = time,
            persistScope = CoroutineScope(main + SupervisorJob()),
            seeds = { 14 },
        )
        val before = m.state.value.board
        m.solveWithAi()
        Thread.sleep(400)
        val thinking = m.state.value.ai
        assertTrue("AI seharusnya masih berpikir: $thinking", thinking is AiUi.Thinking)
        assertNotNull("progres seharusnya sudah dilaporkan", (thinking as AiUi.Thinking).progress)
        m.cancelAi()
        assertEquals(AiUi.Idle, m.state.value.ai)
        Thread.sleep(200)
        assertEquals(before, m.state.value.board)
        assertEquals(AiUi.Idle, m.state.value.ai)
        assertFalse(m.state.value.usedAi)
    }
}
