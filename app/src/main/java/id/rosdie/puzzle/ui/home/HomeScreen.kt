package id.rosdie.puzzle.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.rosdie.puzzle.AppContainer
import id.rosdie.puzzle.R
import id.rosdie.puzzle.data.images.BuiltInImage
import id.rosdie.puzzle.data.repository.Achievement
import id.rosdie.puzzle.data.repository.DateKeys
import id.rosdie.puzzle.data.repository.GameConfig
import id.rosdie.puzzle.data.repository.SavedGame
import id.rosdie.puzzle.data.repository.StatsSummary
import id.rosdie.puzzle.domain.model.ChallengeType
import id.rosdie.puzzle.domain.model.GameMode
import id.rosdie.puzzle.ui.components.AppIcons
import id.rosdie.puzzle.ui.components.IconBadge
import id.rosdie.puzzle.ui.components.PuzzleBoard
import id.rosdie.puzzle.ui.components.SectionCard
import id.rosdie.puzzle.ui.components.ThinBar
import id.rosdie.puzzle.ui.components.Tone
import id.rosdie.puzzle.ui.components.formatClock
import id.rosdie.puzzle.ui.components.formatSecondsClock
import id.rosdie.puzzle.ui.splash.LogoMark
import id.rosdie.puzzle.ui.theme.BrandBlue
import id.rosdie.puzzle.ui.theme.BrandBlueDark
import id.rosdie.puzzle.ui.theme.BrandBlueLight
import id.rosdie.puzzle.ui.theme.TabularNumbers
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.TimeZone

@Composable
fun HomeScreen(
    container: AppContainer,
    onPlay: (GameConfig) -> Unit,
    onResume: () -> Unit,
    onDaily: () -> Unit,
    onGallery: () -> Unit,
    onStats: () -> Unit,
    onHowTo: () -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showSetup by rememberSaveable { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val content = Modifier.widthIn(max = 640.dp).fillMaxWidth()
            Hero(state, content, onPlay = { showSetup = true })
            state.saved?.let { ContinueCard(it, onResume, content) }
            DailyCard(state.stats, onDaily, content)
            NavGrid(state, content, onGallery, onStats, onHowTo, onSettings)
            RecordsCard(state.stats, state.loading, content)
        }
    }

    if (showSetup) {
        GameSetupSheet(
            container = container,
            initial = GameConfig(state.settings.difficulty.size, GameMode.NUMBERS, null, ChallengeType.NORMAL, null),
            onStart = {
                showSetup = false
                onPlay(it)
            },
            onDismiss = { showSetup = false },
        )
    }
}

@Composable
private fun greeting(): String {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    return stringResource(
        when (hour) {
            in 4..10 -> R.string.greeting_morning
            in 11..14 -> R.string.greeting_afternoon
            in 15..17 -> R.string.greeting_evening
            else -> R.string.greeting_night
        },
    )
}

/** Hero: sapaan sesuai waktu, logo animasi, tombol Main, dan ringkasan cepat dari data nyata. */
@Composable
private fun Hero(state: HomeUiState, modifier: Modifier, onPlay: () -> Unit) {
    Box(
        modifier
            .shadow(10.dp, RoundedCornerShape(32.dp), spotColor = BrandBlueDark)
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(BrandBlueLight, BrandBlue, BrandBlueDark))),
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LogoMark(Modifier.size(56.dp), animate = true)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(greeting(), style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.9f))
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        modifier = Modifier.semantics { heading() },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.home_tagline), style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f))
            val stats = state.stats
            if (stats != null && stats.hasGames) {
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroChip(AppIcons.Trophy, stats.totalSolved.toString(), stringResource(R.string.hero_solved), Modifier.weight(1f))
                    HeroChip(AppIcons.Fire, stats.currentStreak.toString(), stringResource(R.string.hero_streak), Modifier.weight(1f))
                    HeroChip(AppIcons.Star, state.achievementsUnlocked.toString(), stringResource(R.string.home_achievements), Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(20.dp))
            Surface(
                onClick = onPlay,
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp),
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                    Icon(AppIcons.Play, null, Modifier.size(28.dp), tint = BrandBlueDark)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_play), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BrandBlueDark)
                }
            }
        }
    }
}

@Composable
private fun HeroChip(icon: ImageVector, value: String, label: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.16f)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp).semantics(mergeDescendants = true) {}) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(16.dp), tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text(value, style = MaterialTheme.typography.titleMedium.merge(TabularNumbers), color = Color.White)
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Lanjutkan: pratinjau papan tersimpan + progres tile yang sudah benar. */
@Composable
private fun ContinueCard(saved: SavedGame, onClick: () -> Unit, modifier: Modifier) {
    val board = saved.board
    val total = board.cellCount - 1
    val placed = (0 until total).count { board[it] == it + 1 }
    Surface(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(76.dp).clearAndSetSemantics {}) {
                PuzzleBoard(board = board, interactive = false, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_continue), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(
                        if (saved.config.isDaily) R.string.home_continue_daily else R.string.home_continue_body,
                        stringResource(R.string.grid_label, saved.config.size, saved.config.size),
                        pluralStringResource(R.plurals.moves_count, saved.moves, saved.moves),
                        formatClock(saved.elapsedMs),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                ThinBar(placed / total.toFloat(), MaterialTheme.colorScheme.primary, Modifier.fillMaxWidth(), height = 6.dp)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.home_continue_progress, "$placed/$total"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                Icon(AppIcons.Play, null, tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

/** Sisa waktu sampai tengah malam lokal (Puzzle Harian berganti), diperbarui tiap 30 detik. */
@Composable
private fun rememberMillisToMidnight(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    val tomorrow = DateKeys.epochDayOf(now) + 1
    return tomorrow * 86_400_000L - TimeZone.getDefault().getOffset(now) - now
}

@Composable
private fun DailyCard(stats: StatsSummary?, onClick: () -> Unit, modifier: Modifier) {
    val done = stats?.todayDailyDone == true
    val c = MaterialTheme.colorScheme
    val left = rememberMillisToMidnight()
    val duration = stringResource(R.string.duration_hm, (left / 3_600_000).toInt(), (left / 60_000 % 60).toInt())
    Surface(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.large, color = c.tertiaryContainer) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).background(c.surfaceContainerLowest.copy(alpha = 0.6f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Icon(AppIcons.Calendar, null, Modifier.size(28.dp), tint = c.onTertiaryContainer)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.home_daily), style = MaterialTheme.typography.titleMedium, color = c.onTertiaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = CircleShape, color = if (done) c.tertiary else c.surfaceContainerLowest.copy(alpha = 0.6f)) {
                        Text(
                            stringResource(if (done) R.string.status_done else R.string.status_open),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (done) c.onTertiary else c.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    stringResource(if (done) R.string.home_daily_done else R.string.home_daily_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onTertiaryContainer,
                )
                Text(
                    stringResource(if (done) R.string.home_daily_next else R.string.home_daily_deadline, duration),
                    style = MaterialTheme.typography.labelMedium,
                    color = c.onTertiaryContainer.copy(alpha = 0.8f),
                )
            }
            val streak = stats?.currentStreak ?: 0
            if (streak > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(AppIcons.Fire, null, tint = c.onTertiaryContainer)
                    Text(
                        stringResource(R.string.streak_days_short, streak),
                        style = MaterialTheme.typography.titleMedium.merge(TabularNumbers),
                        color = c.onTertiaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun NavGrid(
    state: HomeUiState,
    modifier: Modifier,
    onGallery: () -> Unit,
    onStats: () -> Unit,
    onHowTo: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NavTile(
                AppIcons.Image, Tone.PRIMARY, stringResource(R.string.nav_gallery),
                stringResource(R.string.nav_gallery_sub, BuiltInImage.entries.size, state.customImages),
                onGallery, Modifier.weight(1f),
            )
            NavTile(
                AppIcons.BarChart, Tone.TERTIARY, stringResource(R.string.nav_stats),
                stringResource(R.string.nav_stats_sub, state.achievementsUnlocked, Achievement.entries.size),
                onStats, Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NavTile(AppIcons.Help, Tone.SECONDARY, stringResource(R.string.nav_howto), stringResource(R.string.nav_howto_sub), onHowTo, Modifier.weight(1f))
            NavTile(AppIcons.Settings, Tone.NEUTRAL, stringResource(R.string.nav_settings), stringResource(R.string.nav_settings_sub), onSettings, Modifier.weight(1f))
        }
    }
}

@Composable
private fun NavTile(icon: ImageVector, tone: Tone, label: String, sub: String, onClick: () -> Unit, modifier: Modifier) {
    Surface(onClick = onClick, modifier = modifier.heightIn(min = 112.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(16.dp)) {
            IconBadge(icon, tone, size = 44.dp)
            Spacer(Modifier.height(12.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 2)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Rekor per ukuran grid sebagai tiga kartu kecil; tanpa data → "Belum ada permainan". */
@Composable
private fun RecordsCard(stats: StatsSummary?, loading: Boolean, modifier: Modifier) {
    SectionCard(stringResource(R.string.home_stats_title), modifier, icon = AppIcons.Trophy, tone = Tone.TERTIARY) {
        when {
            loading -> Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodyMedium)
            stats == null || !stats.hasGames -> Text(
                stringResource(R.string.no_games_yet),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> {
                Row(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.home_total_solved), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(stats.totalSolved.toString(), style = MaterialTheme.typography.bodyMedium.merge(TabularNumbers), fontWeight = FontWeight.SemiBold)
                }
                Row(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.home_streak), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        pluralStringResource(R.plurals.days_count, stats.currentStreak, stats.currentStreak),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.home_records), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.perGrid.forEach { g ->
                        val none = stringResource(R.string.value_none)
                        Surface(Modifier.weight(1f), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Column(Modifier.padding(10.dp).semantics(mergeDescendants = true) {}) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                                    Text(
                                        stringResource(R.string.grid_label, g.gridSize, g.gridSize),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(stringResource(R.string.home_best_moves), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(g.record?.bestMoves?.toString() ?: none, style = MaterialTheme.typography.titleMedium.merge(TabularNumbers))
                                Text(stringResource(R.string.home_best_time), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(g.record?.bestTimeSeconds?.let { formatSecondsClock(it) } ?: none, style = MaterialTheme.typography.titleMedium.merge(TabularNumbers))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.home_record_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

