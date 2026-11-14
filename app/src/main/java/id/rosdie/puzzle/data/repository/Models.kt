package id.rosdie.puzzle.data.repository

import id.rosdie.puzzle.domain.model.ChallengeType
import id.rosdie.puzzle.domain.model.GameMode
import id.rosdie.puzzle.domain.model.PuzzleState
import kotlinx.serialization.Serializable

/** Konfigurasi satu permainan. */
data class GameConfig(
    val size: Int,
    val mode: GameMode,
    val imageId: String?,
    val challenge: ChallengeType,
    /** yyyyMMdd untuk Puzzle Harian. */
    val dailyDate: Int?,
) {
    val isDaily: Boolean get() = dailyDate != null
}

@Serializable
data class UndoEntry(val tiles: List<Int>, val count: Int)

data class SavedGame(
    val sessionId: String,
    val config: GameConfig,
    val board: PuzzleState,
    val initial: PuzzleState,
    val moves: Int,
    val elapsedMs: Long,
    val undo: List<UndoEntry>,
    val paused: Boolean,
    val usedAi: Boolean,
    val usedHint: Boolean,
    val savedAt: Long,
)

data class CustomImage(val id: String, val path: String, val sizeBytes: Long, val createdAt: Long)

/** Rekor lokal satu ukuran grid (hanya kemenangan tanpa AI dan hint). */
data class GridRecord(val gridSize: Int, val bestMoves: Int, val bestTimeSeconds: Long)

/** Perbandingan hasil baru dengan rekor sebelumnya. */
data class RecordComparison(
    val previous: GridRecord?,
    val eligible: Boolean,
    val newMovesRecord: Boolean,
    val newTimeRecord: Boolean,
)
