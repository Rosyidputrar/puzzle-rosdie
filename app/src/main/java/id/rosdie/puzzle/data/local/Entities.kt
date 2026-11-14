package id.rosdie.puzzle.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "game_results", indices = [Index("gridSize"), Index("playedAt"), Index("dailyDate")])
data class GameResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gridSize: Int,
    val mode: String,
    val moves: Int,
    val timeSeconds: Long,
    val usedAi: Boolean,
    val usedHint: Boolean,
    val stars: Int,
    val challengeType: String,
    val dailyDate: Int?,
    val outcome: String,
    val playedAt: Long,
    val imageId: String?,
)

/** Satu baris: permainan yang sedang berjalan untuk kartu "Lanjutkan". */
@Entity(tableName = "saved_game")
data class SavedGameEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val sessionId: String,
    val grid: Int,
    val mode: String,
    val imageId: String?,
    /** Tile dipisah koma, baris demi baris. */
    val tiles: String,
    val initialTiles: String,
    val moves: Int,
    val elapsedMs: Long,
    /** JSON daftar entri undo. */
    val undoStack: String,
    val paused: Boolean,
    val challengeType: String,
    val dailyDate: Int?,
    val usedAi: Boolean,
    val usedHint: Boolean,
    val savedAt: Long,
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val unlockedAt: Long,
)

@Entity(tableName = "custom_images")
data class CustomImageEntity(
    @PrimaryKey val id: String,
    val path: String,
    val sizeBytes: Long,
    val createdAt: Long,
)
