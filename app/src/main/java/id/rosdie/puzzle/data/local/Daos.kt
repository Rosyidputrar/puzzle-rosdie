package id.rosdie.puzzle.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GameResultDao {
    @Insert
    suspend fun insert(result: GameResultEntity): Long

    @Query("SELECT * FROM game_results ORDER BY playedAt DESC")
    fun observeAll(): Flow<List<GameResultEntity>>

    @Query("SELECT * FROM game_results ORDER BY playedAt DESC")
    suspend fun getAll(): List<GameResultEntity>

    @Query(
        "SELECT * FROM game_results WHERE gridSize = :grid AND outcome = 'WON' AND usedAi = 0 AND usedHint = 0",
    )
    suspend fun eligibleWins(grid: Int): List<GameResultEntity>
}

@Dao
interface SavedGameDao {
    @Query("SELECT * FROM saved_game WHERE id = 0")
    fun observe(): Flow<SavedGameEntity?>

    @Query("SELECT * FROM saved_game WHERE id = 0")
    suspend fun get(): SavedGameEntity?

    @Upsert
    suspend fun upsert(game: SavedGameEntity)

    @Query("DELETE FROM saved_game")
    suspend fun clear()

    @Query("DELETE FROM saved_game WHERE sessionId = :sessionId")
    suspend fun clearSession(sessionId: String)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun observeAll(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun getAll(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: AchievementEntity): Long
}

@Dao
interface CustomImageDao {
    @Query("SELECT * FROM custom_images ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CustomImageEntity>>

    @Query("SELECT * FROM custom_images WHERE id = :id")
    suspend fun get(id: String): CustomImageEntity?

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM custom_images")
    suspend fun totalBytes(): Long

    @Query("SELECT COUNT(*) FROM custom_images")
    suspend fun count(): Int

    @Insert
    suspend fun insert(entity: CustomImageEntity)

    @Query("DELETE FROM custom_images WHERE id = :id")
    suspend fun delete(id: String)
}
