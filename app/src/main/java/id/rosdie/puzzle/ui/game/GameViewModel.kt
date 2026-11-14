package id.rosdie.puzzle.ui.game

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import id.rosdie.puzzle.PuzzleApplication
import id.rosdie.puzzle.data.datastore.AppSettings
import id.rosdie.puzzle.data.images.BuiltInImage
import id.rosdie.puzzle.data.images.ImageRepository
import id.rosdie.puzzle.data.repository.AchievementRepository
import id.rosdie.puzzle.data.repository.GameConfig
import id.rosdie.puzzle.data.repository.GameRepository
import id.rosdie.puzzle.data.repository.SavedGame
import id.rosdie.puzzle.data.repository.TimeSource
import id.rosdie.puzzle.data.repository.UndoEntry
import id.rosdie.puzzle.domain.engine.PuzzleEngine
import id.rosdie.puzzle.domain.model.ChallengeRules
import id.rosdie.puzzle.domain.model.ChallengeType
import id.rosdie.puzzle.domain.model.Difficulty
import id.rosdie.puzzle.domain.model.Direction
import id.rosdie.puzzle.domain.model.GameMode
import id.rosdie.puzzle.domain.model.GameOutcome
import id.rosdie.puzzle.domain.model.GameResult
import id.rosdie.puzzle.domain.model.PuzzleState
import id.rosdie.puzzle.domain.model.StarRating
import id.rosdie.puzzle.domain.solver.PuzzleSolver
import id.rosdie.puzzle.domain.solver.ResumeToken
import id.rosdie.puzzle.domain.solver.SolveMode
import id.rosdie.puzzle.domain.solver.SolveRequest
import id.rosdie.puzzle.domain.solver.SolverResult
import id.rosdie.puzzle.feedback.GameFeedback
import id.rosdie.puzzle.ui.navigation.GameRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.UUID

/**
 * ViewModel layar permainan. Sumber kebenaran untuk papan, langkah, timer, undo, hint, AI,
 * dan simpan otomatis. Undo MENAMBAH hitungan langkah (lihat README).
 */
class GameViewModel(
    private val route: GameRoute,
    private val games: GameRepository,
    private val achievements: AchievementRepository,
    private val images: ImageRepository,
    private val settings: StateFlow<AppSettings>,
    private val updateSettings: ((AppSettings) -> AppSettings) -> Unit,
    private val solver: PuzzleSolver,
    private val feedback: GameFeedback,
    private val time: TimeSource,
    private val persistScope: CoroutineScope,
    private val seeds: () -> Long = { secureRandom.nextLong() },
) : ViewModel() {

    private val _state = MutableStateFlow(
        GameUiState(showReference = settings.value.showReference, aiSpeedMs = settings.value.aiSpeedMs),
    )
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private val _boardImage = MutableStateFlow<ImageBitmap?>(null)
    val boardImage: StateFlow<ImageBitmap?> = _boardImage.asStateFlow()

    private var sessionId = UUID.randomUUID().toString()
    private val undoStack = ArrayDeque<UndoEntry>()
    private var accumulatedMs = 0L
    private var runningSinceMs: Long? = null
    private var statusBeforePause = GameStatus.READY

    private var tickerJob: Job? = null
    private var aiJob: Job? = null
    private var hintJob: Job? = null
    private var saveJob: Job? = null
    private var imageJob: Job? = null
    private val aiPaused = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            when {
                route.resume -> {
                    val saved = games.loadSavedGame()
                    if (saved != null) {
                        restore(saved)
                    } else {
                        startNew(route.toConfig())
                        _state.update { it.copy(message = GameMessage.SAVED_GAME_MISSING) }
                    }
                }
                route.daily -> {
                    val today = time.todayKey()
                    val saved = games.loadSavedGame()
                    if (saved != null && saved.config.dailyDate == today) restore(saved) else startDaily(today)
                }
                else -> startNew(route.toConfig())
            }
        }
    }

    // ---------- Siklus permainan ----------

    private fun startDaily(dateKey: Int) {
        val config = GameConfig(PuzzleEngine.DAILY_SIZE, GameMode.NUMBERS, null, ChallengeType.NORMAL, dateKey)
        startNew(config, PuzzleEngine.dailyPuzzle(dateKey))
    }

    private fun startNew(requested: GameConfig, board: PuzzleState? = null) {
        cancelBackgroundWork()
        val config = if (requested.mode == GameMode.IMAGE && requested.imageId == null) {
            requested.copy(imageId = BuiltInImage.SUNSET.imageId)
        } else {
            requested
        }
        sessionId = UUID.randomUUID().toString()
        val start = board ?: PuzzleEngine.shuffle(config.size, seeds())
        undoStack.clear()
        accumulatedMs = 0
        runningSinceMs = null
        _state.update {
            it.copy(
                loading = false,
                config = config,
                board = start,
                initial = start,
                moves = 0,
                elapsedMs = 0,
                status = GameStatus.READY,
                usedAi = false,
                usedHint = false,
                canUndo = false,
                hint = HintUi.None,
                ai = AiUi.Idle,
                win = null,
                lossReason = null,
                aiSummary = null,
            )
        }
        loadImage(config)
    }

    private fun restore(saved: SavedGame) {
        cancelBackgroundWork()
        sessionId = saved.sessionId
        undoStack.clear()
        saved.undo.takeLast(MAX_UNDO).forEach { undoStack.addLast(it) }
        accumulatedMs = saved.elapsedMs
        runningSinceMs = null
        val status = when {
            saved.moves == 0 -> GameStatus.READY
            saved.paused -> GameStatus.PAUSED
            else -> GameStatus.PLAYING
        }
        statusBeforePause = if (saved.moves > 0) GameStatus.PLAYING else GameStatus.READY
        _state.update {
            it.copy(
                loading = false,
                config = saved.config,
                board = saved.board,
                initial = saved.initial,
                moves = saved.moves,
                elapsedMs = saved.elapsedMs,
                status = status,
                usedAi = saved.usedAi,
                usedHint = saved.usedHint,
                canUndo = undoStack.isNotEmpty(),
                hint = HintUi.None,
                ai = AiUi.Idle,
                win = null,
                lossReason = null,
                aiSummary = null,
            )
        }
        if (status == GameStatus.PLAYING) startClock()
        loadImage(saved.config)
    }

    private fun loadImage(config: GameConfig) {
        imageJob?.cancel()
        if (config.mode != GameMode.IMAGE) {
            _boardImage.value = null
            return
        }
        val id = config.imageId ?: return
        imageJob = viewModelScope.launch {
            val bmp = images.loadBoardBitmap(id)
            if (bmp == null) {
                _boardImage.value = null
                _state.update {
                    it.copy(config = it.config.copy(mode = GameMode.NUMBERS, imageId = null), message = GameMessage.IMAGE_MISSING)
                }
            } else {
                _boardImage.value = bmp.asImageBitmap()
            }
        }
    }

    // ---------- Input pemain ----------

    /** Ketuk: tile sebaris/sekolom dengan kotak kosong menggeser seluruh barisan. */
    fun onTileTap(index: Int) {
        val s = _state.value
        if (!s.interactive) return
        val moves = PuzzleEngine.lineMoves(s.board, index)
        if (moves.isEmpty()) feedback.invalid() else applyPlayerMoves(moves)
    }

    /** Swipe di papan: tile di sisi berlawanan dari arah swipe masuk ke kotak kosong. */
    fun onSwipe(direction: Direction) {
        val s = _state.value
        if (!s.interactive) return
        if (PuzzleEngine.canMove(s.board, direction)) applyPlayerMoves(listOf(direction)) else feedback.invalid()
    }

    /** Seret: tile dilepas di kotak kosong. */
    fun onDragCommit(index: Int) {
        val s = _state.value
        if (!s.interactive) return
        val d = PuzzleEngine.directionForTile(s.board, index)
        if (d == null) feedback.invalid() else applyPlayerMoves(listOf(d))
    }

    fun onInvalidGesture() {
        if (_state.value.interactive) feedback.invalid()
    }

    private fun applyPlayerMoves(dirs: List<Direction>) {
        val before = _state.value.board
        val after = PuzzleEngine.applyAll(before, dirs)
        undoStack.addLast(UndoEntry(before.toList(), dirs.size))
        while (undoStack.size > MAX_UNDO) undoStack.removeFirst()
        hintJob?.cancel()
        _state.update {
            it.copy(
                board = after,
                moves = it.moves + dirs.size,
                status = GameStatus.PLAYING,
                canUndo = true,
                hint = HintUi.None,
            )
        }
        startClock()
        feedback.move()
        afterMove()
    }

    /** Undo: kembalikan papan sebelum aksi terakhir. Hitungan langkah tetap BERTAMBAH. */
    fun undo() {
        val s = _state.value
        if (!s.interactive || undoStack.isEmpty()) return
        val entry = undoStack.removeLast()
        hintJob?.cancel()
        _state.update {
            it.copy(
                board = PuzzleState.of(it.size, entry.tiles),
                moves = it.moves + entry.count,
                canUndo = undoStack.isNotEmpty(),
                hint = HintUi.None,
            )
        }
        feedback.move()
        afterMove()
    }

    private fun afterMove() {
        val s = _state.value
        when {
            s.board.isSolved -> onPlayerWin()
            s.config.challenge == ChallengeType.TARGET_MOVES && s.moves >= ChallengeRules.targetMoves(s.size) ->
                onLose(LossReason.OUT_OF_MOVES)
            else -> scheduleSave()
        }
    }

    // ---------- Kontrol ----------

    /** Reset = puzzle yang sama kembali ke posisi awal acakan. */
    fun reset() {
        cancelBackgroundWork()
        undoStack.clear()
        accumulatedMs = 0
        runningSinceMs = null
        _state.update {
            it.copy(
                board = it.initial,
                moves = 0,
                elapsedMs = 0,
                status = GameStatus.READY,
                canUndo = false,
                hint = HintUi.None,
                ai = AiUi.Idle,
                win = null,
                lossReason = null,
                aiSummary = null,
            )
        }
        persistScope.launch { games.clearSavedGame(sessionId) }
    }

    /** Acak = puzzle baru dengan konfigurasi yang sama. */
    fun shuffle() {
        val s = _state.value
        if (s.config.isDaily) return
        abandonIfNeeded()
        startNew(s.config)
    }

    /** Mengganti kesulitan langsung membuat puzzle baru. */
    fun changeDifficulty(size: Int) {
        val s = _state.value
        if (s.config.isDaily || s.size == size) return
        abandonIfNeeded()
        startNew(s.config.copy(size = size))
        updateSettings { it.copy(difficulty = Difficulty.fromSize(size)) }
    }

    fun newGame(config: GameConfig) {
        abandonIfNeeded()
        startNew(config.copy(dailyDate = null))
    }

    fun togglePause() {
        when (_state.value.status) {
            GameStatus.PLAYING, GameStatus.READY -> pause()
            GameStatus.PAUSED -> resume()
            else -> Unit
        }
    }

    fun pause() {
        val s = _state.value
        if (s.loading || s.aiBusy) return
        if (s.status != GameStatus.PLAYING && s.status != GameStatus.READY) return
        statusBeforePause = s.status
        stopClock()
        hintJob?.cancel()
        _state.update { it.copy(status = GameStatus.PAUSED, hint = if (it.hint is HintUi.Loading) HintUi.None else it.hint) }
        saveNow()
    }

    fun resume() {
        if (_state.value.status != GameStatus.PAUSED) return
        val next = if (_state.value.moves > 0) GameStatus.PLAYING else statusBeforePause
        _state.update { it.copy(status = next) }
        if (next == GameStatus.PLAYING) startClock()
    }

    fun toggleReference() = _state.update { it.copy(showReference = !it.showReference) }

    fun setAiSpeed(ms: Int) {
        val v = ms.coerceIn(AppSettings.MIN_AI_SPEED_MS, AppSettings.MAX_AI_SPEED_MS)
        _state.update { it.copy(aiSpeedMs = v) }
        updateSettings { it.copy(aiSpeedMs = v) }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    /** Dipanggil saat aplikasi ke latar belakang (ON_STOP): jeda dan simpan. */
    fun onAppBackgrounded() {
        if (_state.value.status == GameStatus.PLAYING && !_state.value.aiBusy) pause() else saveNow()
    }

    // ---------- Hint ----------

    fun requestHint() {
        val s = _state.value
        if (!s.interactive || s.hint is HintUi.Loading) return
        hintJob?.cancel()
        val board = s.board
        _state.update { it.copy(hint = HintUi.Loading) }
        hintJob = viewModelScope.launch {
            val dir = solver.hint(board)
            if (_state.value.board != board) return@launch
            if (dir == null) {
                _state.update { it.copy(hint = HintUi.None, message = GameMessage.HINT_FAILED) }
            } else {
                val idx = PuzzleEngine.tileIndexFor(board, dir)
                _state.update { it.copy(hint = HintUi.Tile(idx), usedHint = true) }
                scheduleSave()
            }
        }
    }

    // ---------- AI solver ----------

    fun solveWithAi() = startSolver(SolveMode.BEST, null)

    fun quickSolve() = startSolver(SolveMode.QUICK, null)

    fun continueAi() {
        val ai = _state.value.ai as? AiUi.BudgetExceeded ?: return
        startSolver(SolveMode.BEST, ai.token)
    }

    private fun startSolver(mode: SolveMode, resume: ResumeToken?) {
        val s = _state.value
        val canStart = !s.loading && (s.status == GameStatus.READY || s.status == GameStatus.PLAYING) &&
            (s.ai is AiUi.Idle || s.ai is AiUi.BudgetExceeded)
        if (!canStart) return
        hintJob?.cancel()
        aiJob?.cancel()
        stopClock()
        val board = s.board
        _state.update { it.copy(ai = AiUi.Thinking(mode, null), hint = HintUi.None) }
        aiJob = viewModelScope.launch {
            val request = SolveRequest(
                mode = mode,
                timeBudgetMs = if (resume != null) null else PuzzleSolver.DEFAULT_TIME_BUDGET_MS,
                resume = resume,
                onProgress = { p ->
                    _state.update { st -> if (st.ai is AiUi.Thinking) st.copy(ai = AiUi.Thinking(mode, p)) else st }
                },
            )
            when (val result = solver.solve(board, request)) {
                is SolverResult.Solved -> execute(result)
                is SolverResult.BudgetExceeded ->
                    _state.update { it.copy(ai = AiUi.BudgetExceeded(result.stats, result.resume)) }
                SolverResult.Unsolvable -> {
                    _state.update { it.copy(ai = AiUi.Idle, message = GameMessage.UNSOLVABLE) }
                    resumeClockIfPlaying()
                }
            }
        }
    }

    /** Batal: coroutine solver benar-benar dibatalkan; papan tetap seperti sebelum solver jalan. */
    fun cancelAi() {
        val ai = _state.value.ai
        if (ai !is AiUi.Thinking && ai !is AiUi.BudgetExceeded) return
        aiJob?.cancel()
        aiJob = null
        _state.update { it.copy(ai = AiUi.Idle) }
        resumeClockIfPlaying()
    }

    fun toggleAiExecutionPause() {
        val ai = _state.value.ai as? AiUi.Executing ?: return
        val paused = !ai.paused
        aiPaused.value = paused
        _state.update { it.copy(ai = ai.copy(paused = paused)) }
    }

    /** Hentikan eksekusi AI; pemain melanjutkan dari posisi sekarang (tetap ditandai memakai AI). */
    fun stopAiExecution() {
        if (_state.value.ai !is AiUi.Executing) return
        aiJob?.cancel()
        aiJob = null
        aiPaused.value = false
        _state.update {
            it.copy(ai = AiUi.Idle, status = if (it.moves > 0) GameStatus.PLAYING else GameStatus.READY)
        }
        resumeClockIfPlaying()
        scheduleSave()
    }

    private suspend fun execute(result: SolverResult.Solved) {
        undoStack.clear()
        aiPaused.value = false
        _state.update {
            it.copy(
                usedAi = true,
                canUndo = false,
                ai = AiUi.Executing(result.moves.size, 0, false, result.optimal, result.strategy),
            )
        }
        for ((i, d) in result.moves.withIndex()) {
            aiPaused.first { !it }
            val next = PuzzleEngine.move(_state.value.board, d) ?: break
            _state.update { st ->
                st.copy(board = next, ai = (st.ai as? AiUi.Executing)?.copy(done = i + 1) ?: st.ai)
            }
            feedback.aiStep()
            delay(_state.value.aiSpeedMs.toLong())
        }
        if (_state.value.board.isSolved) onAiSolved(result) else stopAiExecution()
    }

    private fun onAiSolved(result: SolverResult.Solved) {
        val s = _state.value
        stopClock()
        _state.update {
            it.copy(
                status = GameStatus.AI_SOLVED,
                ai = AiUi.Idle,
                aiSummary = AiSummary(result.moves.size, s.moves, result.optimal),
            )
        }
        val record = buildResult(GameOutcome.AI_SOLVED)
        val session = sessionId
        persistScope.launch {
            games.recordResult(record)
            games.clearSavedGame(session)
        }
    }

    // ---------- Akhir permainan ----------

    private fun onPlayerWin() {
        stopClock()
        hintJob?.cancel()
        val s = _state.value
        val result = buildResult(GameOutcome.WON)
        _state.update { it.copy(status = GameStatus.WON) }
        feedback.win()
        val session = sessionId
        persistScope.launch {
            val comparison = games.recordResult(result)
            games.clearSavedGame(session)
            val unlocked = achievements.evaluate(games.getAllResults(), images.customCount())
            _state.update {
                if (it.status != GameStatus.WON) {
                    it
                } else {
                    it.copy(
                        win = WinSummary(
                            gridSize = s.size,
                            moves = result.moves,
                            timeMs = s.elapsedMs,
                            stars = result.stars,
                            eligibleForRecord = comparison.eligible,
                            previous = comparison.previous,
                            newMovesRecord = comparison.newMovesRecord,
                            newTimeRecord = comparison.newTimeRecord,
                            newAchievements = unlocked,
                            isDaily = result.dailyDate != null,
                        ),
                    )
                }
            }
        }
    }

    private fun onLose(reason: LossReason) {
        stopClock()
        hintJob?.cancel()
        _state.update { it.copy(status = GameStatus.LOST, lossReason = reason, hint = HintUi.None) }
        feedback.lose()
        val result = buildResult(GameOutcome.LOST)
        val session = sessionId
        persistScope.launch {
            games.recordResult(result)
            games.clearSavedGame(session)
        }
    }

    private fun buildResult(outcome: GameOutcome): GameResult {
        val s = _state.value
        val elapsed = currentElapsed()
        val today = time.todayKey()
        return GameResult(
            gridSize = s.size,
            mode = s.config.mode,
            moves = s.moves,
            timeSeconds = elapsed / 1000,
            usedAi = s.usedAi,
            usedHint = s.usedHint,
            stars = if (outcome == GameOutcome.WON) StarRating.stars(s.size, s.moves) else 0,
            challengeType = s.config.challenge,
            // Puzzle Harian hanya dihitung bila diselesaikan pada hari yang sama.
            dailyDate = s.config.dailyDate?.takeIf { it == today && outcome == GameOutcome.WON },
            outcome = outcome,
            playedAt = time.nowMs(),
            imageId = s.config.imageId.takeIf { s.config.mode == GameMode.IMAGE },
        )
    }

    private fun abandonIfNeeded() {
        val s = _state.value
        if (s.moves == 0 || s.isFinished) return
        val result = buildResult(GameOutcome.ABANDONED)
        val session = sessionId
        persistScope.launch {
            games.recordResult(result)
            games.clearSavedGame(session)
        }
    }

    // ---------- Timer ----------

    private fun currentElapsed(): Long = accumulatedMs + (runningSinceMs?.let { time.elapsedMs() - it } ?: 0L)

    private fun startClock() {
        if (runningSinceMs != null) return
        runningSinceMs = time.elapsedMs()
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(TICK_MS)
                val elapsed = currentElapsed()
                _state.update { it.copy(elapsedMs = elapsed) }
                val s = _state.value
                if (s.config.challenge == ChallengeType.TIME_ATTACK && s.status == GameStatus.PLAYING &&
                    elapsed >= ChallengeRules.timeLimitSeconds(s.size) * 1000
                ) {
                    _state.update { it.copy(elapsedMs = ChallengeRules.timeLimitSeconds(s.size) * 1000) }
                    onLose(LossReason.OUT_OF_TIME)
                }
            }
        }
    }

    private fun stopClock() {
        runningSinceMs?.let { accumulatedMs += time.elapsedMs() - it }
        runningSinceMs = null
        tickerJob?.cancel()
        tickerJob = null
        _state.update { it.copy(elapsedMs = accumulatedMs) }
    }

    private fun resumeClockIfPlaying() {
        if (_state.value.status == GameStatus.PLAYING) startClock()
    }

    // ---------- Simpan otomatis ----------

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            saveNow()
        }
    }

    fun saveNow(forcePaused: Boolean = false) {
        saveJob?.cancel()
        val s = _state.value
        if (s.loading || s.isFinished || s.moves == 0) return
        val snapshot = SavedGame(
            sessionId = sessionId,
            config = s.config,
            board = s.board,
            initial = s.initial,
            moves = s.moves,
            elapsedMs = currentElapsed(),
            undo = undoStack.toList(),
            paused = forcePaused || s.status == GameStatus.PAUSED,
            usedAi = s.usedAi,
            usedHint = s.usedHint,
            savedAt = time.nowMs(),
        )
        persistScope.launch { withContext(NonCancellable) { games.saveGame(snapshot) } }
    }

    private fun cancelBackgroundWork() {
        aiJob?.cancel(); aiJob = null
        hintJob?.cancel(); hintJob = null
        tickerJob?.cancel(); tickerJob = null
        saveJob?.cancel(); saveJob = null
        aiPaused.value = false
    }

    override fun onCleared() {
        // Meninggalkan layar = permainan dijeda dan disimpan.
        saveNow(forcePaused = true)
    }

    companion object {
        const val MAX_UNDO = 50
        private const val TICK_MS = 200L
        private const val SAVE_DEBOUNCE_MS = 1_000L
        private val secureRandom = SecureRandom()

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PuzzleApplication
                val c = app.container
                val handle: SavedStateHandle = createSavedStateHandle()
                GameViewModel(
                    route = handle.toRoute<GameRoute>(),
                    games = c.games,
                    achievements = c.achievements,
                    images = c.images,
                    settings = c.settings,
                    updateSettings = c::updateSettings,
                    solver = c.solver,
                    feedback = c.feedback,
                    time = c.time,
                    persistScope = c.appScope,
                )
            }
        }
    }
}
