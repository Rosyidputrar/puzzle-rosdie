package id.rosdie.puzzle.data.repository

import androidx.room.withTransaction
import id.rosdie.puzzle.data.local.GameResultEntity
import id.rosdie.puzzle.data.local.PuzzleDatabase
import id.rosdie.puzzle.data.local.SavedGameEntity
import id.rosdie.puzzle.domain.model.ChallengeType
import id.rosdie.puzzle.domain.model.GameMode
import id.rosdie.puzzle.domain.model.GameOutcome
import id.rosdie.puzzle.domain.model.GameResult
import id.rosdie.puzzle.domain.model.PuzzleState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class GameRepository(private val db: PuzzleDatabase) {
    private val results = db.results()
    private val saved = db.savedGame()

    val allResults: Flow<List<GameResult>> = results.observeAll().map { list -> list.map { it.toModel() } }

    val savedGame: Flow<SavedGame?> = saved.observe().map { it?.toModelOrNull() }

    suspend fun getAllResults(): List<GameResult> = results.getAll().map { it.toModel() }

    suspend fun loadSavedGame(): SavedGame? = saved.get()?.toModelOrNull()

    /**
     * Simpan permainan berjalan. Bila baris tersimpan milik sesi lain (pemain memulai puzzle
     * baru dan sudah melangkah), sesi lama dicatat sebagai ABANDONED.
     */
    suspend fun saveGame(game: SavedGame) {
        db.withTransaction {
            val existing = saved.get()?.toModelOrNull()
            if (existing != null && existing.sessionId != game.sessionId && existing.moves > 0) {
                results.insert(existing.toAbandonedResult(game.savedAt).toEntity())
            }
            saved.upsert(game.toEntity())
        }
    }

    suspend fun clearSavedGame(sessionId: String) = saved.clearSession(sessionId)

    suspend fun bestRecord(gridSize: Int): GridRecord? {
        val wins = results.eligibleWins(gridSize)
        if (wins.isEmpty()) return null
        return GridRecord(gridSize, wins.minOf { it.moves }, wins.minOf { it.timeSeconds })
    }

    /** Simpan hasil; untuk kemenangan juga kembalikan perbandingan dengan rekor sebelumnya. */
    suspend fun recordResult(result: GameResult): RecordComparison = db.withTransaction {
        val previous = bestRecord(result.gridSize)
        results.insert(result.toEntity())
        val eligible = result.eligibleForRecord
        RecordComparison(
            previous = previous,
            eligible = eligible,
            newMovesRecord = eligible && (previous == null || result.moves < previous.bestMoves),
            newTimeRecord = eligible && (previous == null || result.timeSeconds < previous.bestTimeSeconds),
        )
    }

    private fun SavedGame.toAbandonedResult(now: Long) = GameResult(
        gridSize = config.size,
        mode = config.mode,
        moves = moves,
        timeSeconds = elapsedMs / 1000,
        usedAi = usedAi,
        usedHint = usedHint,
        stars = 0,
        challengeType = config.challenge,
        dailyDate = null,
        outcome = GameOutcome.ABANDONED,
        playedAt = now,
        imageId = config.imageId,
    )

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun GameResult.toEntity() = GameResultEntity(
            id = id,
            gridSize = gridSize,
            mode = mode.name,
            moves = moves,
            timeSeconds = timeSeconds,
            usedAi = usedAi,
            usedHint = usedHint,
            stars = stars,
            challengeType = challengeType.name,
            dailyDate = dailyDate,
            outcome = outcome.name,
            playedAt = playedAt,
            imageId = imageId,
        )

        fun GameResultEntity.toModel() = GameResult(
            id = id,
            gridSize = gridSize,
            mode = enumValueOrNull<GameMode>(mode) ?: GameMode.NUMBERS,
            moves = moves,
            timeSeconds = timeSeconds,
            usedAi = usedAi,
            usedHint = usedHint,
            stars = stars,
            challengeType = enumValueOrNull<ChallengeType>(challengeType) ?: ChallengeType.NORMAL,
            dailyDate = dailyDate,
            outcome = enumValueOrNull<GameOutcome>(outcome) ?: GameOutcome.ABANDONED,
            playedAt = playedAt,
            imageId = imageId,
        )

        fun SavedGame.toEntity() = SavedGameEntity(
            sessionId = sessionId,
            grid = config.size,
            mode = config.mode.name,
            imageId = config.imageId,
            tiles = board.toList().joinToString(","),
            initialTiles = initial.toList().joinToString(","),
            moves = moves,
            elapsedMs = elapsedMs,
            undoStack = json.encodeToString(undo),
            paused = paused,
            challengeType = config.challenge.name,
            dailyDate = config.dailyDate,
            usedAi = usedAi,
            usedHint = usedHint,
            savedAt = savedAt,
        )

        /** Null bila data tersimpan rusak (misal tile tidak valid); permainan itu diabaikan. */
        fun SavedGameEntity.toModelOrNull(): SavedGame? = runCatching {
            val board = PuzzleState.of(grid, tiles.split(",").map { it.trim().toInt() })
            val initial = PuzzleState.of(grid, initialTiles.split(",").map { it.trim().toInt() })
            val undo = json.decodeFromString<List<UndoEntry>>(undoStack).filter { it.tiles.size == grid * grid }
            SavedGame(
                sessionId = sessionId,
                config = GameConfig(
                    size = grid,
                    mode = enumValueOrNull<GameMode>(mode) ?: GameMode.NUMBERS,
                    imageId = imageId,
                    challenge = enumValueOrNull<ChallengeType>(challengeType) ?: ChallengeType.NORMAL,
                    dailyDate = dailyDate,
                ),
                board = board,
                initial = initial,
                moves = moves,
                elapsedMs = elapsedMs,
                undo = undo,
                paused = paused,
                usedAi = usedAi,
                usedHint = usedHint,
                savedAt = savedAt,
            )
        }.getOrNull()

        private inline fun <reified E : Enum<E>> enumValueOrNull(name: String): E? =
            enumValues<E>().firstOrNull { it.name == name }
    }
}
