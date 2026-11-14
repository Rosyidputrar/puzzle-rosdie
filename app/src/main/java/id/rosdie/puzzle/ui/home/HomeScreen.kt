package id.rosdie.puzzle.ui.home

import id.rosdie.puzzle.ui.theme.Success
import id.rosdie.puzzle.ui.components.PrimaryButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import id.rosdie.puzzle.ui.components.AppCard
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Scaffold(containerColor = Color.Transparent) { padding ->
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
            NavList(state, content, onGallery, onStats, onHowTo, onSettings)
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

/** Kepala halaman: logo, sapaan, judul, dan satu tombol utama. */
@Composable
private fun Hero(state: HomeUiState, modifier: Modifier, onPlay: () -> Unit) {
    Column(modifier.padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)).padding(10.dp)) {
                LogoMark(Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(greeting(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.home_tagline), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        PrimaryButton(stringResource(R.string.action_play), AppIcons.Play, onPlay, Modifier.fillMaxWidth().heightIn(min = 56.dp))
    }
}

/** Lanjutkan: pratinjau papan tersimpan + progres tile yang sudah benar. */
@Composable
private fun ContinueCard(saved: SavedGame, onClick: () -> Unit, modifier: Modifier) {
    val board = saved.board
    val total = board.cellCount - 1
    val placed = (0 until total).count { board[it] == it + 1 }
    AppCard(modifier, onClick = onClick) {
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
            Icon(AppIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Sisa waktu sampai tengah malam lokal (Puzzle Harian berganti), diperbarui tiap menit. */
@Composable
private fun rememberMillisToMidnight(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
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
    AppCard(modifier, onClick = onClick) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(AppIcons.Calendar, tone = Tone.PRIMARY, size = 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.home_daily), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(if (done) R.string.status_done else R.string.status_open),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (done) Success else c.onSurfaceVariant,
                    )
                }
                Text(
                    stringResource(if (done) R.string.home_daily_done else R.string.home_daily_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant,
                )
                Text(
                    stringResource(if (done) R.string.home_daily_next else R.string.home_daily_deadline, duration),
                    style = MaterialTheme.typography.labelMedium,
                    color = c.onSurfaceVariant,
                )
            }
            val streak = stats?.currentStreak ?: 0
            if (streak > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(AppIcons.Fire, null, tint = c.primary)
                    Text(stringResource(R.string.streak_days_short, streak), style = MaterialTheme.typography.titleMedium.merge(TabularNumbers))
                }
            }
        }
    }
}

/** Menu sebagai daftar sederhana dalam satu kartu (bukan grid kartu ala dashboard). */
@Composable
private fun NavList(
    state: HomeUiState,
    modifier: Modifier,
    onGallery: () -> Unit,
    onStats: () -> Unit,
    onHowTo: () -> Unit,
    onSettings: () -> Unit,
) {
    AppCard(modifier) {
        Column {
            NavRow(AppIcons.Image, stringResource(R.string.nav_gallery), stringResource(R.string.nav_gallery_sub, BuiltInImage.entries.size, state.customImages), onGallery)
            HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
            NavRow(
                AppIcons.BarChart, stringResource(R.string.nav_stats),
                stringResource(R.string.nav_stats_sub, state.achievementsUnlocked, Achievement.entries.size), onStats,
            )
            HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
            NavRow(AppIcons.Help, stringResource(R.string.nav_howto), stringResource(R.string.nav_howto_sub), onHowTo)
            HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
            NavRow(AppIcons.Settings, stringResource(R.string.nav_settings), stringResource(R.string.nav_settings_sub), onSettings)
        }
    }
}

@Composable
private fun NavRow(icon: ImageVector, label: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, tone = Tone.NEUTRAL, size = 40.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(AppIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Rekor per ukuran grid sebagai tiga kartu kecil; tanpa data → "Belum ada permainan". */
@Composable
private fun RecordsCard(stats: StatsSummary?, loading: Boolean, modifier: Modifier) {
    SectionCard(stringResource(R.string.home_stats_title), modifier, icon = AppIcons.Trophy) {
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
                Spacer(Modifier.height(4.dp))
                val none = stringResource(R.string.value_none)
                stats.perGrid.forEach { g ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.grid_label, g.gridSize, g.gridSize), style = MaterialTheme.typography.titleSmall.merge(TabularNumbers), modifier = Modifier.width(56.dp))
                        Text(
                            g.record?.let { pluralStringResource(R.plurals.moves_count, it.bestMoves, it.bestMoves) } ?: none,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(g.record?.bestTimeSeconds?.let { formatSecondsClock(it) } ?: none, style = MaterialTheme.typography.bodyMedium.merge(TabularNumbers))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.home_record_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

