package id.rosdie.puzzle.domain

import id.rosdie.puzzle.domain.engine.PuzzleEngine
import id.rosdie.puzzle.domain.model.PuzzleState
import id.rosdie.puzzle.domain.solver.Heuristics
import id.rosdie.puzzle.domain.solver.PuzzleSolver
import id.rosdie.puzzle.domain.solver.SolveMode
import id.rosdie.puzzle.domain.solver.SolveRequest
import id.rosdie.puzzle.domain.solver.SolverProgress
import id.rosdie.puzzle.domain.solver.SolverResult
import id.rosdie.puzzle.domain.solver.SolverStrategy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.Collections

class PuzzleSolverTest {
    private val solver = PuzzleSolver(Dispatchers.Default)

    private fun solve(s: PuzzleState, request: SolveRequest = SolveRequest()): SolverResult.Solved = runBlocking {
        solver.solve(s, request) as SolverResult.Solved
    }

    private fun assertSolves(start: PuzzleState, result: SolverResult.Solved) {
        assertTrue("solusi tidak menyelesaikan puzzle", PuzzleEngine.applyAll(start, result.moves).isSolved)
    }

    @Test
    fun aStar3x3IsOptimalAgainstExhaustiveBfs() {
        for (seed in 0L until 300L) {
            val s = PuzzleEngine.shuffle(3, seed)
            val r = solve(s)
            assertEquals(SolverStrategy.ASTAR_OPTIMAL, r.strategy)
            assertTrue(r.optimal)
            assertSolves(s, r)
            assertEquals("seed $seed", Bfs3x3.distance(s), r.moves.size)
        }
    }

    @Test
    fun aStar3x3HardestKnownInstancesNeed31Moves() {
        // Dua konfigurasi 8-puzzle terjauh (31 langkah) yang terdokumentasi.
        val a = PuzzleState.of(3, listOf(8, 6, 7, 2, 5, 4, 3, 0, 1))
        val b = PuzzleState.of(3, listOf(6, 4, 7, 8, 5, 0, 3, 2, 1))
        for (s in listOf(a, b)) {
            assertEquals(31, Bfs3x3.distance(s))
            val r = solve(s)
            assertEquals(31, r.moves.size)
            assertSolves(s, r)
        }
    }

    @Test
    fun idaStar4x4IsOptimalOnBfsVerifiedInstances() {
        for (seed in 0L until 40L) {
            val s = walk(4, 18, seed)
            val exact = bfsDistance(s, 18)!!
            val r = solve(s)
            assertEquals(SolverStrategy.IDA_STAR_OPTIMAL, r.strategy)
            assertSolves(s, r)
            assertEquals("seed $seed", exact, r.moves.size)
        }
    }

    @Test
    fun idaStar4x4KnownInstance() {
        // Instans Korf #1 (1985), panjang optimal yang dipublikasikan: 57. Korf memakai kotak kosong
        // di kiri atas; di sini diputar 180 derajat dan tile t diganti 16-t (panjang optimal sama).
        val korf1 = PuzzleState.of(4, listOf(13, 6, 8, 12, 15, 14, 0, 10, 11, 7, 4, 5, 9, 1, 3, 2))
        val r = solve(korf1, SolveRequest(timeBudgetMs = null))
        assertSolves(korf1, r)
        assertEquals(57, r.moves.size)
    }

    @Test
    fun quickSolve4x4IsValidAndHonestlyLabelled() {
        for (seed in 0L until 20L) {
            val s = PuzzleEngine.shuffle(4, seed)
            val r = solve(s, SolveRequest(mode = SolveMode.QUICK))
            assertFalse(r.optimal)
            assertSolves(s, r)
            assertTrue(r.moves.size >= Heuristics.estimate(s))
        }
    }

    @Test
    fun staged5x5AlwaysSolvesAndIsNeverLabelledOptimal() {
        for (seed in 0L until 40L) {
            val s = PuzzleEngine.shuffle(5, seed)
            val r = solve(s)
            assertEquals(SolverStrategy.STAGED, r.strategy)
            assertFalse(r.optimal)
            assertSolves(s, r)
        }
    }

    @Test
    fun unsolvableIsReportedClearly() = runBlocking {
        val bad = PuzzleState.of(4, (1..13).toList() + listOf(15, 14, 0))
        assertEquals(SolverResult.Unsolvable, solver.solve(bad))
        assertEquals(null, solver.hint(bad))
    }

    @Test
    fun solvedPuzzleNeedsNoMoves() {
        val r = solve(PuzzleState.solved(4))
        assertTrue(r.moves.isEmpty())
    }

    @Test
    fun hintIsFirstMoveOfAnOptimalSolution() = runBlocking {
        for (seed in 0L until 50L) {
            val s = PuzzleEngine.shuffle(3, seed)
            val hint = solver.hint(s)!!
            val after = PuzzleEngine.move(s, hint)!!
            assertEquals(Bfs3x3.distance(s) - 1, Bfs3x3.distance(after))
        }
        assertNotNull(solver.hint(PuzzleEngine.shuffle(4, 3)))
        assertNotNull(solver.hint(PuzzleEngine.shuffle(5, 3)))
    }

    @Test
    fun cancellationStopsSearchQuicklyAndLeavesBoardUntouched() = runBlocking {
        // Instans 4x4 sulit tanpa anggaran: hanya berhenti karena dibatalkan.
        val hard = PuzzleState.of(4, listOf(0, 12, 9, 13, 15, 11, 10, 14, 3, 7, 2, 5, 4, 8, 6, 1))
        val before = hard.toList()
        val job = async(Dispatchers.Default) { solver.solve(hard, SolveRequest(timeBudgetMs = null)) }
        delay(300)
        val t0 = System.nanoTime()
        job.cancel()
        try {
            withTimeout(2_000) { job.await() }
            fail("seharusnya dibatalkan")
        } catch (_: CancellationException) {
            // diharapkan
        }
        val stopMs = (System.nanoTime() - t0) / 1_000_000
        assertTrue("pembatalan terlalu lambat: $stopMs ms", stopMs < 500)
        assertEquals(before, hard.toList())
    }

    @Test
    fun budgetExceededCanBeResumedToTheSameOptimalAnswer() = runBlocking {
        val korf1 = PuzzleState.of(4, listOf(13, 6, 8, 12, 15, 14, 0, 10, 11, 7, 4, 5, 9, 1, 3, 2))
        val first = solver.solve(korf1, SolveRequest(timeBudgetMs = 50))
        assertTrue(first is SolverResult.BudgetExceeded)
        first as SolverResult.BudgetExceeded
        assertTrue(first.stats.iterations > 0)
        val resumed = solver.solve(korf1, SolveRequest(timeBudgetMs = null, resume = first.resume))
        resumed as SolverResult.Solved
        assertEquals(57, resumed.moves.size)
        assertTrue(resumed.optimal)
        assertTrue(resumed.stats.iterations >= first.stats.iterations)
    }

    @Test
    fun progressIsThrottledToAboutTenPerSecond() = runBlocking {
        val events = Collections.synchronizedList(ArrayList<SolverProgress>())
        val hard = PuzzleState.of(4, listOf(0, 12, 9, 13, 15, 11, 10, 14, 3, 7, 2, 5, 4, 8, 6, 1))
        val r = solver.solve(hard, SolveRequest(timeBudgetMs = 1_000, onProgress = { events += it }))
        assertTrue(r is SolverResult.BudgetExceeded || r is SolverResult.Solved)
        assertTrue("terlalu banyak event: ${events.size}", events.size in 3..12)
        val its = events.map { it.iterations }
        assertEquals(its.sorted(), its)
        events.forEach { assertTrue(it.iterationsPerSecond > 0) }
    }
}
