package id.rosdie.puzzle.domain

import id.rosdie.puzzle.domain.engine.PuzzleEngine
import id.rosdie.puzzle.domain.model.PuzzleState
import id.rosdie.puzzle.domain.solver.Heuristics
import id.rosdie.puzzle.domain.solver.PackedTileSpace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class HeuristicsTest {

    @Test
    fun knownValues() {
        assertEquals(0, Heuristics.estimate(PuzzleState.solved(4)))
        // 2 1 3 / 4 5 6 / 7 8 _ : MD = 2, konflik baris 1-2 = +2.
        val s = PuzzleState.of(3, listOf(2, 1, 3, 4, 5, 6, 7, 8, 0))
        assertEquals(2, Heuristics.manhattan(s))
        assertEquals(2, Heuristics.linearConflict(s))
        // 3 2 1 di baris pertama: LIS = 1, dua tile harus keluar -> +4.
        val t = PuzzleState.of(3, listOf(3, 2, 1, 4, 5, 6, 7, 8, 0))
        assertEquals(4, Heuristics.linearConflict(t))
    }

    @Test
    fun admissibleAndConsistentForEvery3x3State() {
        val cells = IntArray(9)
        var exact = 0
        for ((key, dist) in Bfs3x3.distances) {
            PackedTileSpace.unpack(key, cells)
            val s = PuzzleState.of(3, cells)
            val h = Heuristics.estimate(s)
            assertTrue("h=$h > d=$dist pada $s", h <= dist)
            assertEquals("paritas h harus sama dengan paritas jarak", dist % 2, h % 2)
            if (h == dist) exact++
            for (m in PuzzleEngine.validMoves(s)) {
                val hn = Heuristics.estimate(PuzzleEngine.move(s, m)!!)
                assertTrue("tidak konsisten", abs(h - hn) <= 1)
            }
        }
        assertTrue(exact > 0)
    }

    @Test
    fun linearConflictNeverOverestimatesOn4x4ShallowInstances() {
        for (seed in 0L until 60L) {
            val s = walk(4, 14, seed)
            val d = bfsDistance(s, 14)!!
            assertTrue(Heuristics.estimate(s) <= d)
        }
    }
}
