package id.rosdie.puzzle.ui.game

import androidx.compose.runtime.Immutable
import id.rosdie.puzzle.data.repository.Achievement
import id.rosdie.puzzle.data.repository.GameConfig
import id.rosdie.puzzle.data.repository.GridRecord
import id.rosdie.puzzle.domain.model.ChallengeType
import id.rosdie.puzzle.domain.model.ChallengeRules
import id.rosdie.puzzle.domain.model.GameMode
import id.rosdie.puzzle.domain.model.PuzzleState
import id.rosdie.puzzle.domain.solver.ResumeToken
import id.rosdie.puzzle.domain.solver.SolveMode
import id.rosdie.puzzle.domain.solver.SolverProgress
import id.rosdie.puzzle.domain.solver.SolverStrategy

enum class GameStatus { READY, PLAYING, PAUSED, WON, LOST, AI_SOLVED }

enum class LossReason { OUT_OF_MOVES, OUT_OF_TIME }

@Immutable
sealed interface HintUi {
    data object None : HintUi
    data object Loading : HintUi
    data class Tile(val index: Int) : HintUi
}

@Immutable
sealed interface AiUi {
    data object Idle : AiUi

    /** Solver sedang berpikir. [progress] null sampai laporan pertama (~100 ms). */
    data class Thinking(val mode: SolveMode, val progress: SolverProgress?) : AiUi

    /** Anggaran waktu habis: tawarkan Lanjutkan, Batal, atau Solusi Cepat. */
    data class BudgetExceeded(val progress: SolverProgress, val token: ResumeToken) : AiUi

    /** Menjalankan solusi di papan. */
    data class Executing(
        val total: Int,
        val done: Int,
        val paused: Boolean,
        val optimal: Boolean,
        val strategy: SolverStrategy,
    ) : AiUi
}

@Immutable
data class WinSummary(
    val gridSize: Int,
    val moves: Int,
    val timeMs: Long,
    val stars: Int,
    val eligibleForRecord: Boolean,
    val previous: GridRecord?,
    val newMovesRecord: Boolean,
    val newTimeRecord: Boolean,
    val newAchievements: List<Achievement>,
    val isDaily: Boolean,
) {
    val movesPerMinute: Double? get() = if (timeMs >= 1000) moves / (timeMs / 60_000.0) else null
    val secondsPerMove: Double? get() = if (moves > 0) timeMs / 1000.0 / moves else null
}

@Immutable
data class AiSummary(val aiMoves: Int, val playerMoves: Int, val optimal: Boolean)

enum class GameMessage { UNSOLVABLE, HINT_FAILED, IMAGE_MISSING, SAVED_GAME_MISSING }

@Immutable
data class GameUiState(
    val loading: Boolean = true,
    val config: GameConfig = GameConfig(4, GameMode.NUMBERS, null, ChallengeType.NORMAL, null),
    val board: PuzzleState = PuzzleState.solved(4),
    val initial: PuzzleState = PuzzleState.solved(4),
    val moves: Int = 0,
    val elapsedMs: Long = 0,
    val status: GameStatus = GameStatus.READY,
    val usedAi: Boolean = false,
    val usedHint: Boolean = false,
    val canUndo: Boolean = false,
    val hint: HintUi = HintUi.None,
    val ai: AiUi = AiUi.Idle,
    val aiSpeedMs: Int = 300,
    val showReference: Boolean = true,
    val win: WinSummary? = null,
    val lossReason: LossReason? = null,
    val aiSummary: AiSummary? = null,
    val message: GameMessage? = null,
) {
    val size: Int get() = config.size

    val isFinished: Boolean get() = status == GameStatus.WON || status == GameStatus.LOST || status == GameStatus.AI_SOLVED

    val aiBusy: Boolean get() = ai !is AiUi.Idle

    /** Papan menerima input pemain. */
    val interactive: Boolean
        get() = !loading && !aiBusy && (status == GameStatus.READY || status == GameStatus.PLAYING)

    val remainingMoves: Int?
        get() = if (config.challenge == ChallengeType.TARGET_MOVES) {
            (ChallengeRules.targetMoves(size) - moves).coerceAtLeast(0)
        } else {
            null
        }

    val remainingTimeMs: Long?
        get() = if (config.challenge == ChallengeType.TIME_ATTACK) {
            (ChallengeRules.timeLimitSeconds(size) * 1000 - elapsedMs).coerceAtLeast(0)
        } else {
            null
        }
}
