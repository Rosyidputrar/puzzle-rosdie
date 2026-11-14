package id.rosdie.puzzle.ui.game

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import id.rosdie.puzzle.ui.components.BackTopBar
import id.rosdie.puzzle.ui.components.PuzzleBoard
import id.rosdie.puzzle.ui.components.StatValue
import id.rosdie.puzzle.ui.components.formatClock
import id.rosdie.puzzle.ui.gallery.rememberImageImport
import id.rosdie.puzzle.ui.home.GameSetupSheet
import id.rosdie.puzzle.ui.home.difficultyLabel

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

    Scaffold(
        topBar = {
            BackTopBar(
                title = title,
                onBack = onBack,
                actions = {
                    IconButton(onClick = viewModel::toggleReference) {
                        Icon(
                            if (state.showReference) AppIcons.Visibility else AppIcons.VisibilityOff,
                            contentDescription = stringResource(if (state.showReference) R.string.reference_hide else R.string.reference_show),
                        )
                    }
                    if (!state.config.isDaily) {
                        IconButton(onClick = { showSetup = true }, enabled = !state.aiBusy) {
                            Icon(AppIcons.Tune, contentDescription = stringResource(R.string.setup_title))
                        }
                    }
                },
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
                BoardArea(state, image, settings.animationSpeed.stiffnessFactor, viewModel, onBack, m)
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
                        SidePanel(state, image, viewModel) { importer.pickFromGallery() }
                    }
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatsCard(state)
                    if (!state.config.isDaily) DifficultySelector(state, viewModel::changeDifficulty)
                    boardArea(Modifier.widthIn(max = 520.dp).fillMaxWidth())
                    Controls(state, viewModel, onUpload = { importer.pickFromGallery() })
                    AiSection(state, viewModel)
                    ReferencePanel(state, image, viewModel::toggleReference)
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
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(AppIcons.Timer, null) },
            title = {
                Text(stringResource(if (state.lossReason == LossReason.OUT_OF_TIME) R.string.lost_time_title else R.string.lost_moves_title))
            },
            text = { Text(stringResource(R.string.lost_body)) },
            confirmButton = { Button(onClick = viewModel::reset) { Text(stringResource(R.string.action_retry)) } },
            dismissButton = {
                Row {
                    TextButton(onClick = onHome) { Text(stringResource(R.string.action_home)) }
                    TextButton(onClick = viewModel::shuffle) { Text(stringResource(R.string.action_play_again)) }
                }
            },
        )
    }

    state.aiSummary?.takeIf { state.status == GameStatus.AI_SOLVED }?.let { s ->
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(AppIcons.AutoAwesome, null) },
            title = { Text(stringResource(R.string.ai_done_title)) },
            text = {
                Column {
                    Text(
                        stringResource(
                            R.string.ai_done_compare,
                            pluralStringResource(R.plurals.moves_count, s.aiMoves, s.aiMoves),
                            pluralStringResource(R.plurals.moves_count, s.playerMoves, s.playerMoves),
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(if (s.optimal) R.string.ai_done_optimal else R.string.ai_done_not_optimal))
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.ai_done_no_record), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = { if (state.config.isDaily) viewModel.reset() else viewModel.shuffle() }) {
                    Text(stringResource(if (state.config.isDaily) R.string.action_retry else R.string.action_play_again))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onHome) { Text(stringResource(R.string.action_home)) }
                    if (!state.config.isDaily) TextButton(onClick = viewModel::reset) { Text(stringResource(R.string.action_retry)) }
                }
            },
        )
    }
}

@Composable
fun formatDateKey(key: Int): String {
    val months = androidx.compose.ui.res.stringArrayResource(R.array.months_short)
    return stringResource(R.string.date_format, DateKeys.day(key), months[DateKeys.month(key) - 1], DateKeys.year(key))
}

@Composable
private fun BoardArea(
    state: GameUiState,
    image: ImageBitmap?,
    stiffness: Float,
    vm: GameViewModel,
    onExit: () -> Unit,
    modifier: Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        PuzzleBoard(
            board = state.board,
            image = if (state.config.mode == GameMode.IMAGE) image else null,
            interactive = state.interactive,
            hidden = state.status == GameStatus.PAUSED,
            hintIndex = (state.hint as? HintUi.Tile)?.index,
            stiffnessFactor = stiffness,
            onTap = vm::onTileTap,
            onSwipe = vm::onSwipe,
            onDragCommit = vm::onDragCommit,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.status == GameStatus.PAUSED) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(AppIcons.Pause, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
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
                Spacer(Modifier.height(16.dp))
                Button(onClick = vm::resume, modifier = Modifier.widthIn(min = 180.dp)) {
                    Icon(AppIcons.Play, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.action_resume))
                }
                OutlinedButton(onClick = onExit, modifier = Modifier.widthIn(min = 180.dp)) {
                    Text(stringResource(R.string.action_exit))
                }
            }
        }
        if (state.loading) CircularProgressIndicator()
    }
}

@Composable
private fun SidePanel(state: GameUiState, image: ImageBitmap?, vm: GameViewModel, onUpload: () -> Unit) {
    StatsCard(state)
    if (!state.config.isDaily) DifficultySelector(state, vm::changeDifficulty)
    Controls(state, vm, onUpload)
    AiSection(state, vm)
    ReferencePanel(state, image, vm::toggleReference)
}

@Composable
private fun StatsCard(state: GameUiState) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            val remainingMoves = state.remainingMoves
            if (remainingMoves != null) {
                StatValue(stringResource(R.string.stat_moves_left), remainingMoves.toString(), Modifier.weight(1f))
            } else {
                StatValue(stringResource(R.string.stat_moves), state.moves.toString(), Modifier.weight(1f))
            }
            val remainingTime = state.remainingTimeMs
            if (remainingTime != null) {
                StatValue(stringResource(R.string.stat_time_left), formatClock(remainingTime + 999), Modifier.weight(1f), AppIcons.Timer)
            } else {
                StatValue(stringResource(R.string.stat_time), formatClock(state.elapsedMs), Modifier.weight(1f))
            }
            StatValue(stringResource(R.string.stat_status), stringResource(statusLabel(state)), Modifier.weight(1.2f))
            StatValue(stringResource(R.string.stat_grid), stringResource(R.string.grid_label, state.size, state.size), Modifier.weight(1f))
        }
    }
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

@Composable
private fun DifficultySelector(state: GameUiState, onSelect: (Int) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().widthIn(max = 520.dp)) {
        Difficulty.entries.forEachIndexed { i, d ->
            SegmentedButton(
                selected = state.size == d.size,
                onClick = { onSelect(d.size) },
                enabled = !state.aiBusy,
                shape = SegmentedButtonDefaults.itemShape(i, Difficulty.entries.size),
                icon = {},
            ) {
                Text("${difficultyLabel(d.size)} ${stringResource(R.string.grid_label, d.size, d.size)}", maxLines = 1, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun Controls(state: GameUiState, vm: GameViewModel, onUpload: () -> Unit) {
    val playing = state.status == GameStatus.PLAYING || state.status == GameStatus.READY
    Row(Modifier.fillMaxWidth().widthIn(max = 520.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        ControlButton(
            icon = if (state.status == GameStatus.PAUSED) AppIcons.Play else AppIcons.Pause,
            label = stringResource(if (state.status == GameStatus.PAUSED) R.string.action_play else R.string.action_pause),
            enabled = !state.aiBusy && (playing || state.status == GameStatus.PAUSED),
            onClick = vm::togglePause,
        )
        ControlButton(
            AppIcons.Shuffle,
            stringResource(R.string.action_shuffle),
            enabled = !state.aiBusy && !state.config.isDaily,
            onClick = vm::shuffle,
        )
        ControlButton(AppIcons.Reset, stringResource(R.string.action_reset), enabled = !state.aiBusy, onClick = vm::reset)
        ControlButton(AppIcons.Undo, stringResource(R.string.action_undo), enabled = state.interactive && state.canUndo, onClick = vm::undo)
        ControlButton(
            AppIcons.Hint,
            stringResource(R.string.action_hint),
            enabled = state.interactive && state.hint !is HintUi.Loading,
            loading = state.hint is HintUi.Loading,
            onClick = vm::requestHint,
        )
        ControlButton(
            AppIcons.AddPhoto,
            stringResource(R.string.action_upload),
            enabled = !state.aiBusy && !state.config.isDaily,
            onClick = onUpload,
        )
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    loading: Boolean = false,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(56.dp)) {
        FilledTonalIconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(icon, contentDescription = label)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AiSection(state: GameUiState, vm: GameViewModel) {
    Column(Modifier.fillMaxWidth().widthIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AnimatedVisibility(visible = state.ai is AiUi.Idle) {
            Button(
                onClick = vm::solveWithAi,
                enabled = !state.loading && (state.status == GameStatus.READY || state.status == GameStatus.PLAYING),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(AppIcons.AutoAwesome, null)
                Spacer(Modifier.size(10.dp))
                Text(stringResource(R.string.action_solve_ai), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        AiPanel(
            ai = state.ai,
            gridSize = state.size,
            onCancel = vm::cancelAi,
            onContinue = vm::continueAi,
            onQuick = vm::quickSolve,
            onTogglePause = vm::toggleAiExecutionPause,
            onStop = vm::stopAiExecution,
        )
        AiSpeedSlider(state.aiSpeedMs, vm::setAiSpeed)
        if (state.size == 5 && state.ai is AiUi.Idle) {
            Text(
                stringResource(R.string.ai_5x5_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReferencePanel(state: GameUiState, image: ImageBitmap?, onToggle: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().widthIn(max = 520.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.reference_title), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onToggle) {
                    Icon(
                        if (state.showReference) AppIcons.VisibilityOff else AppIcons.Visibility,
                        contentDescription = stringResource(if (state.showReference) R.string.reference_hide else R.string.reference_show),
                    )
                }
            }
            AnimatedVisibility(state.showReference) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TargetPreview(state.size, if (state.config.mode == GameMode.IMAGE) image else null, Modifier.size(120.dp))
                    Spacer(Modifier.size(16.dp))
                    Text(
                        stringResource(if (state.config.mode == GameMode.IMAGE) R.string.reference_body_image else R.string.reference_body_numbers),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = 12.dp),
                    )
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
        Box(modifier.clearAndSetSemantics { contentDescription = label }) {
            PuzzleBoard(board = PuzzleState.solved(size), interactive = false, modifier = Modifier.fillMaxSize())
        }
    }
}

