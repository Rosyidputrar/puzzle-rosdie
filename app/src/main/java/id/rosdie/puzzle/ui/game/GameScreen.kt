package id.rosdie.puzzle.ui.game

import androidx.compose.foundation.border
import id.rosdie.puzzle.ui.components.ThinBar
import id.rosdie.puzzle.ui.theme.TabularNumbers
import id.rosdie.puzzle.ui.theme.NumberFont
import id.rosdie.puzzle.ui.theme.ButtonShape
import id.rosdie.puzzle.ui.theme.tileBlue
import id.rosdie.puzzle.ui.components.Tag
import id.rosdie.puzzle.ui.components.AppCard
import id.rosdie.puzzle.ui.components.SecondaryButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import id.rosdie.puzzle.domain.model.ChallengeRules
import id.rosdie.puzzle.domain.solver.PuzzleSolver
import id.rosdie.puzzle.domain.solver.SolveMode
import id.rosdie.puzzle.ui.components.ControlButton
import id.rosdie.puzzle.ui.components.PrimaryButton
import id.rosdie.puzzle.ui.components.IconBadge
import id.rosdie.puzzle.ui.components.SegmentedSelector
import id.rosdie.puzzle.ui.components.ResultDialog
import id.rosdie.puzzle.ui.components.ResultStat
import id.rosdie.puzzle.ui.components.SectionCard
import id.rosdie.puzzle.ui.components.Tone
import id.rosdie.puzzle.ui.home.challengeName
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.rosdie.puzzle.AppContainer
import id.rosdie.puzzle.R
import id.rosdie.puzzle.data.images.ImageIds
import id.rosdie.puzzle.data.repository.DateKeys
import id.rosdie.puzzle.domain.model.Difficulty
import id.rosdie.puzzle.domain.model.GameMode
import id.rosdie.puzzle.domain.model.PuzzleState
import id.rosdie.puzzle.ui.components.AppIcons
import id.rosdie.puzzle.ui.components.PuzzleBoard
import id.rosdie.puzzle.ui.components.cappedFontSize
import id.rosdie.puzzle.ui.components.formatClock
import id.rosdie.puzzle.ui.components.rememberReducedMotion
import id.rosdie.puzzle.ui.gallery.rememberImageImport
import id.rosdie.puzzle.ui.home.GameSetupSheet
import id.rosdie.puzzle.ui.home.difficultyLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onHome: () -> Unit,
    viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val image by viewModel.boardImage.collectAsStateWithLifecycle()
    val settings by container.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val sb = TopAppBarDefaults.pinnedScrollBehavior()
    var showSetup by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onAppBackgrounded() }

    val importer = rememberImageImport(container) { img ->
        viewModel.newGame(state.config.copy(mode = GameMode.IMAGE, imageId = ImageIds.custom(img.id), dailyDate = null))
    }

    state.message?.let { msg ->
        val text = stringResource(
            when (msg) {
                GameMessage.UNSOLVABLE -> R.string.msg_unsolvable
                GameMessage.HINT_FAILED -> R.string.msg_hint_failed
                GameMessage.IMAGE_MISSING -> R.string.msg_image_missing
                GameMessage.SAVED_GAME_MISSING -> R.string.msg_saved_missing
            },
        )
        LaunchedEffect(msg) {
            snackbar.showSnackbar(text)
            viewModel.dismissMessage()
        }
    }

    val shareText = state.win?.let { w ->
        stringResource(
            R.string.share_text,
            stringResource(R.string.grid_label, w.gridSize, w.gridSize),
            pluralStringResource(R.plurals.moves_count, w.moves, w.moves),
            formatClock(w.timeMs),
        )
    }
    val shareTitle = stringResource(R.string.action_share)
    fun share() {
        val text = shareText ?: return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, shareTitle))
    }

    val title = state.config.dailyDate?.let { stringResource(R.string.game_title_daily, formatDateKey(it)) }
        ?: stringResource(R.string.game_title, state.size, state.size)
    val subtitle = listOf(
        stringResource(if (state.config.mode == GameMode.IMAGE) R.string.mode_image else R.string.mode_numbers),
        stringResource(challengeName(state.config.challenge)),
        stringResource(statusLabel(state)),
    ).joinToString(" · ")
    var peeking by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.nestedScroll(sb.nestedScrollConnection),
        
        topBar = {
            GameTopBar(
                title = title,
                scrollBehavior = sb,
                subtitle = subtitle,
                showReference = state.showReference,
                canEditSetup = !state.config.isDaily && !state.aiBusy,
                showSetupAction = !state.config.isDaily,
                onBack = onBack,
                onToggleReference = viewModel::toggleReference,
                onSetup = { showSetup = true },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val landscape = maxWidth > maxHeight && maxWidth > 560.dp
            val boardArea: @Composable (Modifier) -> Unit = { m ->
                BoardArea(state, image, settings.animationSpeed.stiffnessFactor, peeking, viewModel, onBack, m)
            }
            if (landscape) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Box(Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true), contentAlignment = Alignment.Center) {
                        boardArea(Modifier.fillMaxSize())
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Hud(state)
                        if (!state.config.isDaily) DifficultySelector(state, viewModel::changeDifficulty)
                        ControlDock(state, viewModel) { importer.pickFromGallery() }
                        if (state.showReference) ReferenceStrip(state, image) { peeking = it }
                        AiCard(state, viewModel)
                    }
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val content = Modifier.widthIn(max = 560.dp).fillMaxWidth()
                    Hud(state, content)
                    if (!state.config.isDaily) DifficultySelector(state, viewModel::changeDifficulty, content)
                    boardArea(content)
                    ControlDock(state, viewModel, content) { importer.pickFromGallery() }
                    if (state.showReference) ReferenceStrip(state, image, content) { peeking = it }
                    AiCard(state, viewModel, content)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (showSetup) {
        GameSetupSheet(
            container = container,
            initial = state.config,
            onStart = {
                showSetup = false
                viewModel.newGame(it)
            },
            onDismiss = { showSetup = false },
        )
    }

    state.win?.takeIf { state.status == GameStatus.WON }?.let { win ->
        WinDialog(
            win = win,
            celebrate = settings.winCelebration,
            onPlayAgain = { if (state.config.isDaily) viewModel.newGame(state.config.copy(dailyDate = null)) else viewModel.shuffle() },
            onRetry = viewModel::reset,
            onShare = ::share,
            onHome = onHome,
        )
    }

    if (state.status == GameStatus.LOST) {
        ResultDialog(
            icon = AppIcons.Timer,
            iconTint = MaterialTheme.colorScheme.onErrorContainer,
            iconBackground = MaterialTheme.colorScheme.errorContainer,
            title = stringResource(if (state.lossReason == LossReason.OUT_OF_TIME) R.string.lost_time_title else R.string.lost_moves_title),
            subtitle = stringResource(R.string.lost_body),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultStat(stringResource(R.string.stat_moves), state.moves.toString(), Modifier.weight(1f))
                ResultStat(stringResource(R.string.stat_time), formatClock(state.elapsedMs), Modifier.weight(1f))
                ResultStat(stringResource(R.string.stat_placed), placedLabel(state.board), Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(stringResource(R.string.action_retry), AppIcons.Reset, viewModel::reset, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(stringResource(R.string.action_play_again), viewModel::shuffle, Modifier.weight(1f), icon = AppIcons.Shuffle)
                SecondaryButton(stringResource(R.string.action_home), onHome, Modifier.weight(1f), icon = AppIcons.Home)
            }
        }
    }

    state.aiSummary?.takeIf { state.status == GameStatus.AI_SOLVED }?.let { s ->
        ResultDialog(
            icon = AppIcons.Grid,
            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
            iconBackground = MaterialTheme.colorScheme.primaryContainer,
            title = stringResource(R.string.ai_done_title),
            subtitle = stringResource(if (s.optimal) R.string.ai_done_optimal else R.string.ai_done_not_optimal),
        ) {
            Text(
                stringResource(
                    R.string.ai_done_compare,
                    pluralStringResource(R.plurals.moves_count, s.aiMoves, s.aiMoves),
                    pluralStringResource(R.plurals.moves_count, s.playerMoves, s.playerMoves),
                ),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultStat(stringResource(R.string.ai_label), s.aiMoves.toString(), Modifier.weight(1f))
                ResultStat(stringResource(R.string.you_label), s.playerMoves.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.ai_done_no_record), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                stringResource(if (state.config.isDaily) R.string.action_retry else R.string.action_play_again),
                if (state.config.isDaily) AppIcons.Reset else AppIcons.Shuffle,
                { if (state.config.isDaily) viewModel.reset() else viewModel.shuffle() },
                Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!state.config.isDaily) {
                    SecondaryButton(stringResource(R.string.action_retry), viewModel::reset, Modifier.weight(1f), icon = AppIcons.Reset)
                }
                SecondaryButton(stringResource(R.string.action_home), onHome, Modifier.weight(1f), icon = AppIcons.Home)
            }
        }
    }
}

@Composable
fun formatDateKey(key: Int): String {
    val months = androidx.compose.ui.res.stringArrayResource(R.array.months_short)
    return stringResource(R.string.date_format, DateKeys.day(key), months[DateKeys.month(key) - 1], DateKeys.year(key))
}

private fun statusLabel(state: GameUiState): Int = when {
    state.ai is AiUi.Thinking || state.ai is AiUi.BudgetExceeded -> R.string.status_ai_thinking
    state.ai is AiUi.Executing -> R.string.status_ai_playing
    else -> when (state.status) {
        GameStatus.READY -> R.string.status_ready
        GameStatus.PLAYING -> R.string.status_playing
        GameStatus.PAUSED -> R.string.status_paused
        GameStatus.WON -> R.string.status_won
        GameStatus.LOST -> R.string.status_lost
        GameStatus.AI_SOLVED -> R.string.status_ai_solved
    }
}

private fun placedCount(board: PuzzleState): Int = (0 until board.cellCount - 1).count { board[it] == it + 1 }

@Composable
private fun placedLabel(board: PuzzleState): String = "${placedCount(board)}/${board.cellCount - 1}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameTopBar(
    title: String,
    subtitle: String,
    showReference: Boolean,
    canEditSetup: Boolean,
    showSetupAction: Boolean,
    onBack: () -> Unit,
    onToggleReference: () -> Unit,
    onSetup: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = {
            Column {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(AppIcons.ArrowBack, stringResource(R.string.action_back)) }
        },
        actions = {
            IconButton(onClick = onToggleReference) {
                Icon(
                    if (showReference) AppIcons.Visibility else AppIcons.VisibilityOff,
                    contentDescription = stringResource(if (showReference) R.string.reference_hide else R.string.reference_show),
                )
            }
            if (showSetupAction) {
                IconButton(onClick = onSetup, enabled = canEditSetup) {
                    Icon(AppIcons.Tune, contentDescription = stringResource(R.string.setup_title))
                }
            }
        },        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer),
        scrollBehavior = scrollBehavior,
    )
}

/**
 * Statistik nyata dalam satu baris ringkas (bukan deretan kartu): langkah, waktu, dan tile
 * yang sudah benar. Di mode tantangan, langkah/waktu diganti sisa dan berubah merah saat menipis.
 */
@Composable
private fun Hud(state: GameUiState, modifier: Modifier = Modifier) {
    val total = state.board.cellCount - 1
    val placed = placedCount(state.board)
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val remainingMoves = state.remainingMoves
            if (remainingMoves != null) {
                val target = ChallengeRules.targetMoves(state.size)
                HudValue(stringResource(R.string.stat_moves_left), remainingMoves.toString(), warning = remainingMoves <= target / 10)
            } else {
                HudValue(stringResource(R.string.stat_moves), state.moves.toString())
            }
            val remainingTime = state.remainingTimeMs
            if (remainingTime != null) {
                HudValue(stringResource(R.string.stat_time_left), formatClock(remainingTime + 999), warning = remainingTime <= 15_000)
            } else {
                HudValue(stringResource(R.string.stat_time), formatClock(state.elapsedMs))
            }
            HudValue(stringResource(R.string.stat_placed), "$placed/$total", alignEnd = true)
        }
        Spacer(Modifier.height(8.dp))
        ThinBar(placed / total.toFloat(), MaterialTheme.colorScheme.primary, Modifier.fillMaxWidth())
    }
}

@Composable
private fun HudValue(label: String, value: String, warning: Boolean = false, alignEnd: Boolean = false) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall.merge(TabularNumbers), color = if (warning) c.error else c.onSurface)
    }
}

@Composable
private fun BoardArea(
    state: GameUiState,
    image: ImageBitmap?,
    stiffness: Float,
    peeking: Boolean,
    vm: GameViewModel,
    onExit: () -> Unit,
    modifier: Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        PuzzleBoard(
            board = state.board,
            image = if (state.config.mode == GameMode.IMAGE) image else null,
            interactive = state.interactive && !peeking,
            hidden = state.status == GameStatus.PAUSED,
            hintIndex = (state.hint as? HintUi.Tile)?.index,
            stiffnessFactor = stiffness,
            markCorrect = true,
            onTap = vm::onTileTap,
            onSwipe = vm::onSwipe,
            onDragCommit = vm::onDragCommit,
            modifier = Modifier.fillMaxWidth(),
        )
        // Intip acuan: susunan target ditumpuk di atas papan selama tombol ditahan.
        val reduceMotion = rememberReducedMotion()
        AnimatedVisibility(
            visible = peeking && state.status != GameStatus.PAUSED,
            enter = if (reduceMotion) fadeIn(tween(0)) else fadeIn(),
            exit = if (reduceMotion) fadeOut(tween(0)) else fadeOut(),
            modifier = Modifier.matchParentSize(),
        ) {
            TargetPreview(state.size, if (state.config.mode == GameMode.IMAGE) image else null, Modifier.fillMaxSize())
        }
        if (state.status == GameStatus.PAUSED) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconBadge(AppIcons.Pause, tone = Tone.PRIMARY, size = 64.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.paused_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                Text(
                    stringResource(R.string.paused_body),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                PrimaryButton(stringResource(R.string.action_resume), AppIcons.Play, vm::resume, Modifier.widthIn(min = 200.dp))
                Spacer(Modifier.height(10.dp))
                SecondaryButton(stringResource(R.string.action_exit), onExit, Modifier.widthIn(min = 200.dp))
            }
        }
        if (state.loading) CircularProgressIndicator()
    }
}

@Composable
private fun DifficultySelector(state: GameUiState, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    SegmentedSelector(
        options = Difficulty.entries.map { it.size },
        selected = state.size,
        onSelect = onSelect,
        enabled = !state.aiBusy,
        label = { difficultyLabel(it) },
        sublabel = { stringResource(R.string.grid_label, it, it) },
        modifier = modifier,
    )
}

/** Dock kontrol: semua aksi papan dalam satu permukaan membulat. */
@Composable
private fun ControlDock(state: GameUiState, vm: GameViewModel, modifier: Modifier = Modifier, onUpload: () -> Unit) {
    val paused = state.status == GameStatus.PAUSED
    val playing = state.status == GameStatus.PLAYING || state.status == GameStatus.READY
    Box(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            ControlButton(
                icon = if (paused) AppIcons.Play else AppIcons.Pause,
                label = stringResource(if (paused) R.string.action_play else R.string.action_pause),
                onClick = vm::togglePause,
                enabled = !state.aiBusy && (playing || paused),
                active = paused,
                toggle = true,
                modifier = Modifier.weight(1f),
            )
            ControlButton(AppIcons.Shuffle, stringResource(R.string.action_shuffle), vm::shuffle, Modifier.weight(1f), enabled = !state.aiBusy && !state.config.isDaily)
            ControlButton(AppIcons.Reset, stringResource(R.string.action_reset), vm::reset, Modifier.weight(1f), enabled = !state.aiBusy)
            ControlButton(AppIcons.Undo, stringResource(R.string.action_undo), vm::undo, Modifier.weight(1f), enabled = state.interactive && state.canUndo)
            ControlButton(
                AppIcons.Hint,
                stringResource(R.string.action_hint),
                vm::requestHint,
                Modifier.weight(1f),
                enabled = state.interactive && state.hint !is HintUi.Loading,
                active = state.hint is HintUi.Tile,
                loading = state.hint is HintUi.Loading,
                toggle = true,
            )
            ControlButton(AppIcons.AddPhoto, stringResource(R.string.action_upload), onUpload, Modifier.weight(1f), enabled = !state.aiBusy && !state.config.isDaily)
        }
    }
}

@Composable
private fun AiCard(state: GameUiState, vm: GameViewModel, modifier: Modifier = Modifier) {
    val strategy = PuzzleSolver.strategyFor(state.size, SolveMode.BEST)
    SectionCard(
        title = stringResource(R.string.ai_card_title),
        modifier = modifier.fillMaxWidth(),
        trailing = {
            Tag(stringResource(strategyLabel(strategy)))
        },
    ) {
        AiPanel(
            ai = state.ai,
            gridSize = state.size,
            onCancel = vm::cancelAi,
            onContinue = vm::continueAi,
            onQuick = vm::quickSolve,
            onTogglePause = vm::toggleAiExecutionPause,
            onStop = vm::stopAiExecution,
            onSolve = vm::solveWithAi,
            solveEnabled = !state.loading && (state.status == GameStatus.READY || state.status == GameStatus.PLAYING),
        )
        Spacer(Modifier.height(12.dp))
        AiSpeedSlider(state.aiSpeedMs, vm::setAiSpeed)
    }
}

/**
 * Acuan ringkas: thumbnail target + tombol "Intip". Tahan tombol untuk menumpuk susunan
 * target di atas papan; untuk TalkBack, ketuk dua kali untuk menyalakan/mematikan.
 */
@Composable
private fun ReferenceStrip(state: GameUiState, image: ImageBitmap?, modifier: Modifier = Modifier, onPeek: (Boolean) -> Unit) {
    var peekOn by remember { mutableStateOf(false) }
    val peekLabel = stringResource(R.string.reference_peek_a11y)
    AppCard(modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            TargetPreview(state.size, if (state.config.mode == GameMode.IMAGE) image else null, Modifier.size(64.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.reference_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(if (state.config.mode == GameMode.IMAGE) R.string.reference_body_image else R.string.reference_body_numbers),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .heightIn(min = 48.dp)
                    .clip(ButtonShape)
                    .background(if (peekOn) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .border(1.dp, if (peekOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, ButtonShape)
                    .semantics {
                        role = Role.Switch
                        toggleableState = ToggleableState(peekOn)
                        onClick(label = peekLabel) {
                            peekOn = !peekOn
                            onPeek(peekOn)
                            true
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            peekOn = true
                            onPeek(true)
                            tryAwaitRelease()
                            peekOn = false
                            onPeek(false)
                        })
                    },
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val tint = if (peekOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    Icon(AppIcons.Visibility, null, Modifier.size(18.dp), tint = tint)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.reference_peek), style = MaterialTheme.typography.labelLarge, color = tint)
                }
            }
        }
    }
}

/** Susunan target: gambar utuh atau grid angka 1..N. */
@Composable
fun TargetPreview(size: Int, image: ImageBitmap?, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.a11y_reference)
    if (image != null) {
        Image(
            image,
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(12.dp)),
        )
    } else {
        MiniNumberGrid(size, modifier.clearAndSetSemantics { contentDescription = label })
    }
}

/**
 * Grid angka target yang digambar langsung di Canvas: tetap terbaca walau hanya 64 dp
 * (PuzzleBoard penuh terlalu padat di ukuran sekecil itu).
 */
@Composable
private fun MiniNumberGrid(size: Int, modifier: Modifier) {
    val c = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val blue = tileBlue()
    Canvas(modifier.clip(RoundedCornerShape(12.dp)).background(c.surfaceContainerHighest).padding(3.dp)) {
        val cell = this.size.width / size
        val gap = cell * 0.08f
        val style = TextStyle(color = Color.White, fontSize = cappedFontSize(cell * 0.45f, this), fontFamily = NumberFont, fontWeight = FontWeight.SemiBold)
        for (i in 0 until size * size - 1) {
            val x = (i % size) * cell
            val y = (i / size) * cell
            drawRoundRect(blue, Offset(x + gap / 2, y + gap / 2), Size(cell - gap, cell - gap), CornerRadius(cell * 0.18f))
            val text = measurer.measure((i + 1).toString(), style)
            drawText(text, topLeft = Offset(x + (cell - text.size.width) / 2, y + (cell - text.size.height) / 2))
        }
    }
}
