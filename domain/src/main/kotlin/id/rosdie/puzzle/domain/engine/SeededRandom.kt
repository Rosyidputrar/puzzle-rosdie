package id.rosdie.puzzle.domain.engine

/**
 * PRNG SplitMix64 buatan sendiri. Algoritmanya tetap, sehingga seed yang sama menghasilkan
 * urutan angka yang identik di semua perangkat dan semua versi aplikasi (tidak bergantung
 * pada implementasi kotlin.random).
 */
class SeededRandom(seed: Long) {
    private var state: Long = seed

    fun nextLong(): Long {
        state += GOLDEN_GAMMA
        var z = state
        z = (z xor (z ushr 30)) * MIX_1
        z = (z xor (z ushr 27)) * MIX_2
        return z xor (z ushr 31)
    }

    /** Bilangan bulat seragam di [0, bound) tanpa bias (rejection sampling). */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound harus positif" }
        val b = bound.toLong()
        while (true) {
            val r = nextLong() ushr 1 // 63 bit non-negatif
            val v = r % b
            if (r - v + (b - 1) >= 0) return v.toInt()
        }
    }

    /** Bilangan bulat seragam di [from, untilExclusive). */
    fun nextInt(from: Int, untilExclusive: Int): Int {
        require(untilExclusive > from) { "rentang kosong" }
        return from + nextInt(untilExclusive - from)
    }

    private companion object {
        val GOLDEN_GAMMA = 0x9E3779B97F4A7C15uL.toLong()
        val MIX_1 = 0xBF58476D1CE4E5B9uL.toLong()
        val MIX_2 = 0x94D049BB133111EBuL.toLong()
    }
}
