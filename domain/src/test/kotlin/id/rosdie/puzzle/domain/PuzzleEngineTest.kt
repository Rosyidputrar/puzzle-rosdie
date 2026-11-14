package id.rosdie.puzzle.domain

import id.rosdie.puzzle.domain.engine.PuzzleEngine
import id.rosdie.puzzle.domain.model.Direction
import id.rosdie.puzzle.domain.model.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleEngineTest {

    @Test
    fun solvedStateHasBlankBottomRight() {
        val s = PuzzleState.solved(4)
        assertEquals((1..15).toList() + 0, s.toList())
        assertEquals(15, s.blankIndex)
        assertTrue(s.isSolved)
        assertEquals((1..8).toList() + 0, PuzzleState.solved(3).toList())
        assertEquals((1..24).toList() + 0, PuzzleState.solved(5).toList())
    }

    @Test
    fun movesFollowTileDirectionSemantics() {
        val s = PuzzleState.solved(3)
        // Kotak kosong di pojok kanan bawah: hanya tile atas (bergeser DOWN) dan kiri (RIGHT).
        assertEquals(setOf(Direction.DOWN, Direction.RIGHT), PuzzleEngine.validMoves(s).toSet())
        assertNull(PuzzleEngine.move(s, Direction.UP))
        val moved = PuzzleEngine.move(s, Direction.RIGHT)!!
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 0, 8), moved.toList())
        assertEquals(Direction.LEFT, PuzzleEngine.directionForTile(moved, 8))
        assertNull(PuzzleEngine.directionForTile(moved, 0))
        // State asli tidak berubah (immutable).
        assertTrue(s.isSolved)
    }

    @Test
    fun lineMovesSlideWholeRowOrColumn() {
        val s = PuzzleState.solved(4)
        assertEquals(List(3) { Direction.RIGHT }, PuzzleEngine.lineMoves(s, 12))
        assertEquals(List(3) { Direction.DOWN }, PuzzleEngine.lineMoves(s, 3))
        assertTrue(PuzzleEngine.lineMoves(s, 0).isEmpty())
        assertTrue(PuzzleEngine.lineMoves(s, 15).isEmpty())
        val after = PuzzleEngine.applyAll(s, PuzzleEngine.lineMoves(s, 12))
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 0, 13, 14, 15), after.toList())
    }

    @Test
    fun solvabilityFollowsInversionRule() {
        val swapped4 = PuzzleState.of(4, (1..13).toList() + listOf(15, 14, 0))
        assertFalse(PuzzleEngine.isSolvable(swapped4))
        val swapped3 = PuzzleState.of(3, listOf(2, 1, 3, 4, 5, 6, 7, 8, 0))
        assertFalse(PuzzleEngine.isSolvable(swapped3))
        val swapped5 = PuzzleState.of(5, (1..22).toList() + listOf(24, 23, 0))
        assertFalse(PuzzleEngine.isSolvable(swapped5))
        for (n in 3..5) assertTrue(PuzzleEngine.isSolvable(PuzzleState.solved(n)))
        // Satu gerak vertikal pada grid genap tetap solvable.
        assertTrue(PuzzleEngine.isSolvable(PuzzleEngine.move(PuzzleState.solved(4), Direction.DOWN)!!))
    }

    @Test
    fun solvabilityMatchesReachabilityExhaustivelyFor3x3() {
        // 181.440 state terjangkau = tepat separuh dari 9! permutasi.
        assertEquals(181_440, Bfs3x3.distances.size)
        var solvable = 0
        permutations(9) { perm ->
            val s = PuzzleState.of(3, perm)
            if (PuzzleEngine.isSolvable(s)) solvable++
        }
        assertEquals(181_440, solvable)
    }

    @Test
    fun shuffleIsAlwaysSolvableAndNeverSolved() {
        for (size in 3..5) {
            repeat(300) { seed ->
                val s = PuzzleEngine.shuffle(size, seed.toLong())
                assertTrue("seed $seed tidak solvable", PuzzleEngine.isSolvable(s))
                assertFalse(s.isSolved)
            }
        }
    }

    @Test
    fun shuffleIsDeterministicPerSeed() {
        assertEquals(PuzzleEngine.shuffle(4, 7L), PuzzleEngine.shuffle(4, 7L))
        assertNotEquals(PuzzleEngine.shuffle(4, 7L), PuzzleEngine.shuffle(4, 8L))
    }

    @Test
    fun dailyPuzzleIsStableAcrossVersions() {
        val a = PuzzleEngine.dailyPuzzle(20261114)
        assertEquals(a, PuzzleEngine.dailyPuzzle(20261114))
        assertEquals(4, a.size)
        assertNotEquals(a, PuzzleEngine.dailyPuzzle(20261115))
        // Dipatok agar perubahan PRNG/acakan yang tidak disengaja langsung ketahuan.
        assertEquals(DAILY_20261114, a.toList())
    }

    private fun permutations(n: Int, block: (IntArray) -> Unit) {
        val a = IntArray(n) { it }
        fun rec(k: Int) {
            if (k == n) {
                block(a); return
            }
            for (i in k until n) {
                a[k] = a[i].also { a[i] = a[k] }
                rec(k + 1)
                a[k] = a[i].also { a[i] = a[k] }
            }
        }
        rec(0)
    }

    companion object {
        val DAILY_20261114 = listOf(15, 7, 13, 9, 10, 0, 2, 14, 12, 5, 1, 4, 6, 3, 8, 11)
    }
}
