package id.rosdie.puzzle.domain.solver

import id.rosdie.puzzle.domain.model.Direction

/** Strategi solver per ukuran grid (lihat README untuk benchmark). */
enum class SolverStrategy(val optimal: Boolean) {
    /** A* + MD/LC. Dipakai untuk 3x3. Terbukti optimal. */
    ASTAR_OPTIMAL(true),

    /** IDA* + MD/LC. Dipakai untuk 4x4. Optimal, memori O(kedalaman), dengan anggaran waktu. */
    IDA_STAR_OPTIMAL(true),

    /** Weighted A* ("Solusi Cepat"). Tidak dijamin terpendek. */
    WEIGHTED_ASTAR(false),

    /** Bertahap: baris & kolom luar dulu, sisa 3x3 dengan A*. Tidak dijamin terpendek. */
    STAGED(false),
}

enum class SolveMode {
    /** Strategi terbaik untuk ukuran grid: optimal bila realistis. */
    BEST,

    /** Solusi cepat (weighted A* / bertahap), tidak dijamin terpendek. */
    QUICK,
}

/** Fase berpikir, ditentukan dari waktu atau jumlah iterasi (mana yang lebih jauh). */
enum class ThinkingPhase {
    ANALYZING,
    SEARCHING,
    DEEP_EXPLORATION,
    DEEP_THINKING;

    companion object {
        fun of(elapsedMs: Long, iterations: Long): ThinkingPhase {
            val byTime = when {
                elapsedMs < 1_000 -> 0
                elapsedMs < 5_000 -> 1
                elapsedMs < 15_000 -> 2
                else -> 3
            }
            val byIterations = when {
                iterations < 100_000 -> 0
                iterations < 5_000_000 -> 1
                iterations < 50_000_000 -> 2
                else -> 3
            }
            return entries[maxOf(byTime, byIterations)]
        }
    }
}

/**
 * Snapshot progres solver.
 * @property activeStates A*: ukuran open set. IDA*: panjang jalur yang sedang ditelusuri
 *   (IDA* hanya menyimpan satu jalur di memori).
 * @property etaMs perkiraan sisa waktu, null bila belum bisa diperkirakan secara jujur.
 * @property bound batas f iterasi IDA* yang sedang berjalan, null untuk strategi lain.
 */
data class SolverProgress(
    val strategy: SolverStrategy,
    val iterations: Long,
    val elapsedMs: Long,
    val activeStates: Long,
    val iterationsPerSecond: Long,
    val etaMs: Long?,
    val phase: ThinkingPhase,
    val bound: Int?,
)

/** Token untuk melanjutkan pencarian IDA* yang melewati anggaran waktu. */
class ResumeToken internal constructor(
    val strategy: SolverStrategy,
    internal val bound: Int,
    val iterations: Long,
    val elapsedMs: Long,
    internal val iterationNodes: List<Long>,
)

sealed interface SolverResult {
    data class Solved(
        val moves: List<Direction>,
        val optimal: Boolean,
        val strategy: SolverStrategy,
        val stats: SolverProgress,
    ) : SolverResult

    /** Anggaran waktu habis sebelum solusi optimal ditemukan. Papan tidak berubah. */
    data class BudgetExceeded(val stats: SolverProgress, val resume: ResumeToken) : SolverResult

    data object Unsolvable : SolverResult
}

data class SolveRequest(
    val mode: SolveMode = SolveMode.BEST,
    /** Anggaran waktu; null = tanpa batas (sampai selesai atau dibatalkan). */
    val timeBudgetMs: Long? = PuzzleSolver.DEFAULT_TIME_BUDGET_MS,
    val resume: ResumeToken? = null,
    /** Batas jumlah node A* (dan weighted A*) untuk membatasi memori. */
    val maxNodes: Int = PuzzleSolver.DEFAULT_MAX_NODES,
    val progressIntervalMs: Long = 100,
    val onProgress: (SolverProgress) -> Unit = {},
)
