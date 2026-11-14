package id.rosdie.puzzle.domain.model

enum class GameMode { NUMBERS, IMAGE }

enum class ChallengeType { NORMAL, TARGET_MOVES, TIME_ATTACK }

/**
 * Hasil akhir satu permainan.
 * - [WON]: pemain sendiri mencapai posisi selesai (hint boleh, tapi tidak masuk rekor).
 * - [AI_SOLVED]: posisi selesai dicapai oleh AI. Tidak dihitung sebagai kemenangan.
 * - [LOST]: gagal di mode tantangan (langkah/waktu habis).
 * - [ABANDONED]: puzzle diganti (acak/kesulitan baru) setelah ada langkah.
 */
enum class GameOutcome { WON, LOST, ABANDONED, AI_SOLVED }

data class GameResult(
    val id: Long = 0,
    val gridSize: Int,
    val mode: GameMode,
    val moves: Int,
    val timeSeconds: Long,
    val usedAi: Boolean,
    val usedHint: Boolean,
    val stars: Int,
    val challengeType: ChallengeType,
    /** yyyyMMdd untuk Puzzle Harian, null untuk permainan biasa. */
    val dailyDate: Int?,
    val outcome: GameOutcome,
    val playedAt: Long,
    val imageId: String? = null,
) {
    /** Hanya kemenangan murni (tanpa AI dan hint) yang masuk rekor. */
    val eligibleForRecord: Boolean get() = outcome == GameOutcome.WON && !usedAi && !usedHint
}

/** Rating bintang berdasarkan jumlah langkah saja. */
object StarRating {
    private val thresholds = mapOf(
        3 to (20 to 40),
        4 to (200 to 400),
        5 to (500 to 800),
    )

    fun thresholdsFor(gridSize: Int): Pair<Int, Int> =
        thresholds[gridSize] ?: throw IllegalArgumentException("Ukuran grid tidak didukung: $gridSize")

    fun stars(gridSize: Int, moves: Int): Int {
        val (three, two) = thresholdsFor(gridSize)
        return when {
            moves <= three -> 3
            moves <= two -> 2
            else -> 1
        }
    }
}

/** Aturan mode tantangan per ukuran grid (dicatat di README). */
object ChallengeRules {
    /** Target Langkah = batas 2 bintang, jadi lolos tantangan selalu bernilai minimal 2 bintang. */
    fun targetMoves(gridSize: Int): Int = StarRating.thresholdsFor(gridSize).second

    fun timeLimitSeconds(gridSize: Int): Long = when (gridSize) {
        3 -> 90L
        4 -> 6 * 60L
        5 -> 15 * 60L
        else -> throw IllegalArgumentException("Ukuran grid tidak didukung: $gridSize")
    }
}
