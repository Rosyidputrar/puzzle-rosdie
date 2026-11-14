package id.rosdie.puzzle.domain.solver

import id.rosdie.puzzle.domain.engine.PuzzleEngine
import id.rosdie.puzzle.domain.model.Direction
import id.rosdie.puzzle.domain.model.PuzzleState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext

/**
 * Fasad AI solver. Semua komputasi berjalan di [dispatcher] (default: Dispatchers.Default)
 * pada SALINAN state, sehingga papan asli tidak pernah tersentuh. Pembatalan coroutine
 * diperiksa berkala (ensureActive) di loop pencarian.
 *
 * Strategi:
 * - 3x3: A* optimal.
 * - 4x4: IDA* optimal dengan anggaran waktu; [SolveMode.QUICK] = weighted A* (tidak dijamin terpendek).
 * - 5x5: bertahap (baris/kolom lalu 3x3), tidak dijamin terpendek.
 */
class PuzzleSolver(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val nanoClock: () -> Long = System::nanoTime,
) {

    suspend fun solve(state: PuzzleState, request: SolveRequest = SolveRequest()): SolverResult =
        withContext(dispatcher) {
            if (!PuzzleEngine.isSolvable(state)) return@withContext SolverResult.Unsolvable
            val strategy = request.resume?.strategy ?: strategyFor(state.size, request.mode)
            val monitor = SearchMonitor(
                job = currentCoroutineContext().job,
                nanoClock = nanoClock,
                budgetMs = request.timeBudgetMs,
                intervalMs = request.progressIntervalMs,
                strategy = strategy,
                baseIterations = request.resume?.iterations ?: 0L,
                baseElapsedMs = request.resume?.elapsedMs ?: 0L,
                onProgress = request.onProgress,
            )
            val cells = state.toIntArray()
            if (state.isSolved) {
                return@withContext SolverResult.Solved(emptyList(), true, strategy, monitor.snapshot(0, null))
            }
            when (strategy) {
                SolverStrategy.ASTAR_OPTIMAL -> {
                    // 3x3 hanya punya 181.440 state terjangkau, jadi batas node tidak pernah tercapai.
                    val path = BestFirstSearch(PackedTileSpace(state.size), 1, 1, ASTAR_3X3_MAX_NODES, monitor)
                        .search(PackedTileSpace.pack(cells))!!
                    solved(path, strategy, monitor)
                }

                SolverStrategy.IDA_STAR_OPTIMAL -> {
                    val ida = IdaStarSolver(state.size, monitor)
                    try {
                        val path = ida.solve(cells, request.resume?.bound, request.resume?.iterationNodes.orEmpty())
                        solved(path, strategy, monitor)
                    } catch (_: BudgetExceededSignal) {
                        val stats = monitor.snapshot(0, ida.currentBound)
                        SolverResult.BudgetExceeded(
                            stats = stats,
                            resume = ResumeToken(
                                strategy = strategy,
                                bound = ida.currentBound,
                                iterations = monitor.totalIterations,
                                elapsedMs = monitor.totalElapsedMs,
                                iterationNodes = ida.iterationNodes.toList(),
                            ),
                        )
                    }
                }

                SolverStrategy.WEIGHTED_ASTAR -> quick(cells, state.size, request.maxNodes, monitor)

                SolverStrategy.STAGED -> {
                    val path = StagedSolver(state.size, monitor).solve(cells, request.maxNodes)
                    solved(path, strategy, monitor)
                }
            }
        }

    /**
     * Hint: langkah pertama dari solver dengan strategi yang sama. Untuk 4x4 dicoba IDA*
     * optimal dalam [budgetMs]; bila tidak cukup, pakai weighted A*. Null bila sudah selesai
     * atau tidak solvable.
     */
    suspend fun hint(state: PuzzleState, budgetMs: Long = DEFAULT_HINT_BUDGET_MS): Direction? {
        if (state.isSolved || !PuzzleEngine.isSolvable(state)) return null
        val first = solve(state, SolveRequest(mode = SolveMode.BEST, timeBudgetMs = budgetMs))
        val result = if (first is SolverResult.BudgetExceeded) {
            solve(state, SolveRequest(mode = SolveMode.QUICK, timeBudgetMs = null))
        } else {
            first
        }
        return (result as? SolverResult.Solved)?.moves?.firstOrNull()
    }

    private fun quick(cells: IntArray, size: Int, maxNodes: Int, monitor: SearchMonitor): SolverResult {
        if (size <= 4) {
            for ((num, den) in QUICK_WEIGHTS) {
                try {
                    val path = BestFirstSearch(PackedTileSpace(size), num, den, maxNodes, monitor)
                        .search(PackedTileSpace.pack(cells))!!
                    return solved(path, SolverStrategy.WEIGHTED_ASTAR, monitor)
                } catch (_: MemoryLimitSignal) {
                    // Coba bobot lebih besar (lebih rakus, lebih hemat memori).
                }
            }
        }
        monitor.strategy = SolverStrategy.STAGED
        val path = StagedSolver(size, monitor).solve(cells, maxNodes)
        return solved(path, SolverStrategy.STAGED, monitor)
    }

    private fun solved(path: IntArray, strategy: SolverStrategy, monitor: SearchMonitor): SolverResult.Solved =
        SolverResult.Solved(
            moves = path.map { Direction.entries[it] },
            optimal = strategy.optimal,
            strategy = strategy,
            stats = monitor.snapshot(0, null),
        )

    companion object {
        const val DEFAULT_TIME_BUDGET_MS = 20_000L
        const val DEFAULT_HINT_BUDGET_MS = 1_500L
        const val DEFAULT_MAX_NODES = 1_500_000
        private const val ASTAR_3X3_MAX_NODES = 1_000_000
        private val QUICK_WEIGHTS = listOf(3 to 2, 2 to 1, 3 to 1, 5 to 1)

        fun strategyFor(size: Int, mode: SolveMode): SolverStrategy = when {
            size <= 3 -> SolverStrategy.ASTAR_OPTIMAL
            size == 4 && mode == SolveMode.BEST -> SolverStrategy.IDA_STAR_OPTIMAL
            size == 4 -> SolverStrategy.WEIGHTED_ASTAR
            else -> SolverStrategy.STAGED
        }
    }
}
