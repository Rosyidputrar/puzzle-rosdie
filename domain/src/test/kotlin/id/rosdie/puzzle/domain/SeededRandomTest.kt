package id.rosdie.puzzle.domain

import id.rosdie.puzzle.domain.engine.SeededRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeededRandomTest {
    @Test
    fun matchesReferenceSplitMix64Vectors() {
        // Vektor referensi SplitMix64 (seed 0) dari implementasi C asli Vigna.
        val r = SeededRandom(0)
        assertEquals(0xE220A8397B1DCDAFuL.toLong(), r.nextLong())
        assertEquals(0x6E789E6AA1B965F4uL.toLong(), r.nextLong())
        assertEquals(0x06C45D188009454FuL.toLong(), r.nextLong())
    }

    @Test
    fun sameSeedSameSequence() {
        val a = SeededRandom(20261114)
        val b = SeededRandom(20261114)
        repeat(1000) { assertEquals(a.nextInt(1000), b.nextInt(1000)) }
    }

    @Test
    fun nextIntStaysInRangeAndIsRoughlyUniform() {
        val r = SeededRandom(42)
        val counts = IntArray(7)
        repeat(70_000) {
            val v = r.nextInt(7)
            assertTrue(v in 0 until 7)
            counts[v]++
        }
        counts.forEach { assertTrue("distribusi miring: ${counts.toList()}", it in 9_400..10_600) }
        repeat(1000) { assertTrue(r.nextInt(1000, 2001) in 1000..2000) }
    }
}
