package id.rosdie.puzzle.ui.stats

import androidx.compose.runtime.getValue
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import id.rosdie.puzzle.ui.components.SectionCard
import id.rosdie.puzzle.ui.components.Tone
import id.rosdie.puzzle.ui.components.toneContainer
import id.rosdie.puzzle.ui.components.toneContent
import id.rosdie.puzzle.ui.theme.TabularNumbers
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.rosdie.puzzle.R
import id.rosdie.puzzle.data.repository.Achievement
import id.rosdie.puzzle.data.repository.DateKeys
import id.rosdie.puzzle.data.repository.DayActivity
import id.rosdie.puzzle.data.repository.StatsSummary
import id.rosdie.puzzle.ui.components.AppIcons
import id.rosdie.puzzle.ui.components.BackTopBar
import id.rosdie.puzzle.ui.components.formatDecimal
import id.rosdie.puzzle.ui.components.formatSecondsClock
import id.rosdie.puzzle.ui.game.formatDateKey
import kotlin.math.roundToInt

@Composable
fun StatsScreen(onBack: () -> Unit, viewModel: StatsViewModel = viewModel(factory = StatsViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { BackTopBar(stringResource(R.string.nav_stats), onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val m = Modifier.widthIn(max = 640.dp).fillMaxWidth()
            val stats = state.stats
            if (stats != null) {
                SummaryCard(stats, m)
                // Tanpa data cukup satu empty state; kartu detail muncul setelah ada permainan.
                if (stats.hasGames) {
                    PerGridCard(stats, m)
                    WeekChartCard(stats.last7Days, m)
                }
                CalendarCard(state, viewModel::shiftMonth, m)
            }
            AchievementsCard(state.unlocked, m)
        }
    }
}

@Composable
private fun StatCardShell(title: String, modifier: Modifier, icon: ImageVector, tone: Tone, content: @Composable () -> Unit) {
    SectionCard(title, modifier, icon = icon, tone = tone) { content() }
}

@Composable
private fun SummaryCard(stats: StatsSummary, modifier: Modifier) {
    StatCardShell(stringResource(R.string.stats_summary), modifier, AppIcons.BarChart, Tone.PRIMARY) {
        if (!stats.hasGames) {
            Text(stringResource(R.string.no_games_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@StatCardShell
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigNumber(stats.totalGames.toString(), stringResource(R.string.stats_total_games), Tone.SECONDARY, Modifier.weight(1f))
            BigNumber(stats.totalWins.toString(), stringResource(R.string.stats_total_wins), Tone.PRIMARY, Modifier.weight(1f))
            BigNumber(
                stats.winRate?.let { "${(it * 100).roundToInt()}%" } ?: stringResource(R.string.value_none),
                stringResource(R.string.stats_win_rate),
                Tone.TERTIARY,
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BigNumber(value: String, label: String, tone: Tone, modifier: Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = toneContainer(tone)) {
        Column(
            Modifier.padding(vertical = 14.dp, horizontal = 8.dp).semantics(mergeDescendants = true) {},
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium.merge(TabularNumbers), color = toneContent(tone))
            Text(label, style = MaterialTheme.typography.labelMedium, color = toneContent(tone), textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
private fun PerGridCard(stats: StatsSummary, modifier: Modifier) {
    StatCardShell(stringResource(R.string.stats_per_grid), modifier, AppIcons.Grid, Tone.SECONDARY) {
        val none = stringResource(R.string.value_none)
        if (stats.perGrid.all { it.games == 0 }) {
            Text(stringResource(R.string.no_games_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@StatCardShell
        }
        stats.perGrid.forEachIndexed { i, g ->
            if (i > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Text(
                        stringResource(R.string.grid_label, g.gridSize, g.gridSize),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
                Spacer(Modifier.size(10.dp))
                if (g.games == 0) {
                    Text(stringResource(R.string.no_games_yet), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (g.games > 0) {
                Spacer(Modifier.height(6.dp))
                KeyValue(stringResource(R.string.stats_games_wins), stringResource(R.string.stats_games_wins_value, g.games, g.wins))
                KeyValue(stringResource(R.string.stats_avg_moves), g.averageMoves?.let { formatDecimal(it) } ?: none)
                KeyValue(stringResource(R.string.stats_avg_time), g.averageTimeSeconds?.let { formatSecondsClock(it.roundToInt().toLong()) } ?: none)
                KeyValue(
                    stringResource(R.string.stats_record),
                    g.record?.let {
                        stringResource(
                            R.string.stats_record_value,
                            pluralStringResource(R.plurals.moves_count, it.bestMoves, it.bestMoves),
                            formatSecondsClock(it.bestTimeSeconds),
                        )
                    } ?: none,
                )
            }
        }
    }
}

@Composable
private fun KeyValue(key: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(key, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

/** Grafik batang sederhana 7 hari terakhir, digambar dengan Canvas. */
@Composable
private fun WeekChartCard(days: List<DayActivity>, modifier: Modifier) {
    val labels = stringArrayResource(R.array.days_short)
    val totalGames = days.sumOf { it.games }
    val totalWins = days.sumOf { it.wins }
    val summary = stringResource(
        R.string.a11y_week_chart,
        pluralStringResource(R.plurals.games_count, totalGames, totalGames),
        pluralStringResource(R.plurals.wins_count, totalWins, totalWins),
    )
    StatCardShell(stringResource(R.string.stats_last7), modifier, AppIcons.BarChart, Tone.PRIMARY) {
        if (totalGames == 0) {
            Text(stringResource(R.string.stats_no_week), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            return@StatCardShell
        }
        val primary = MaterialTheme.colorScheme.primary
        val today = MaterialTheme.colorScheme.tertiary
        val track = MaterialTheme.colorScheme.primaryContainer
        val maxValue = (days.maxOfOrNull { it.games } ?: 0).coerceAtLeast(1)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .semantics { contentDescription = summary },
        ) {
            val slot = size.width / days.size
            val barW = slot * 0.5f
            days.forEachIndexed { i, d ->
                val x = i * slot + (slot - barW) / 2
                val gamesH = size.height * d.games / maxValue
                val winsH = size.height * d.wins / maxValue
                drawRoundRect(track, Offset(x, size.height - gamesH), Size(barW, gamesH), CornerRadius(6.dp.toPx()))
                drawRoundRect(if (i == days.lastIndex) today else primary, Offset(x, size.height - winsH), Size(barW, winsH), CornerRadius(6.dp.toPx()))
                if (d.games == 0) drawLine(track, Offset(x, size.height - 1), Offset(x + barW, size.height - 1), 2.dp.toPx())
            }
        }
        Row(Modifier.fillMaxWidth()) {
            days.forEach { d ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(labels[DateKeys.dayOfWeekMondayFirst(d.epochDay)], style = MaterialTheme.typography.labelSmall)
                    Text("${d.wins}/${d.games}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(primary, CircleShape))
            Text(" " + stringResource(R.string.stats_legend_wins), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.size(12.dp))
            Box(Modifier.size(10.dp).background(track, CircleShape))
            Text(" " + stringResource(R.string.stats_legend_games), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CalendarCard(state: StatsUiState, onShift: (Int) -> Unit, modifier: Modifier) {
    val stats = state.stats ?: return
    val months = stringArrayResource(R.array.months_long)
    val dayLabels = stringArrayResource(R.array.days_short)
    StatCardShell(stringResource(R.string.stats_daily_calendar), modifier, AppIcons.Calendar, Tone.TERTIARY) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onShift(-1) }) { Icon(AppIcons.ChevronLeft, stringResource(R.string.calendar_prev)) }
            Text(
                "${months[state.calendarMonth - 1]} ${state.calendarYear}",
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onShift(1) }) { Icon(AppIcons.ChevronRight, stringResource(R.string.calendar_next)) }
        }
        Row(Modifier.fillMaxWidth()) {
            dayLabels.forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
        }
        val first = DateKeys.daysFromCivil(state.calendarYear, state.calendarMonth, 1)
        val lead = DateKeys.dayOfWeekMondayFirst(first)
        val days = DateKeys.daysInMonth(state.calendarYear, state.calendarMonth)
        val cells = lead + days
        val rows = (cells + 6) / 7
        val doneLabel = stringResource(R.string.calendar_done)
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val dayNum = r * 7 + c - lead + 1
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (dayNum in 1..days) {
                            val key = state.calendarYear * 10_000 + state.calendarMonth * 100 + dayNum
                            val done = key in stats.dailyCompleted
                            val today = key == state.todayKey
                            val bg = if (done) MaterialTheme.colorScheme.primary else Color.Transparent
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(bg, CircleShape)
                                    .then(if (today) Modifier.border(2.dp, MaterialTheme.colorScheme.tertiary, CircleShape) else Modifier)
                                    .semantics { if (done) contentDescription = "$dayNum, $doneLabel" },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    dayNum.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Fire, null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.size(6.dp))
            Text(
                stringResource(
                    R.string.stats_streak_summary,
                    pluralStringResource(R.plurals.days_count, stats.currentStreak, stats.currentStreak),
                    pluralStringResource(R.plurals.days_count, stats.bestStreak, stats.bestStreak),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun AchievementsCard(unlocked: Map<Achievement, Long>, modifier: Modifier) {
    StatCardShell(stringResource(R.string.stats_achievements, unlocked.size, Achievement.entries.size), modifier, AppIcons.Trophy, Tone.TERTIARY) {
        Achievement.entries.forEachIndexed { i, a ->
            if (i > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp))
            val ui = AchievementUi.of(a)
            val at = unlocked[a]
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics(mergeDescendants = true) {}) {
                val c = MaterialTheme.colorScheme
                Box(
                    Modifier
                        .size(44.dp)
                        .background(if (at != null) c.tertiaryContainer else c.surfaceContainerHighest, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (at != null) ui.icon else AppIcons.Lock,
                        null,
                        tint = if (at != null) c.onTertiaryContainer else c.outline,
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(ui.title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(ui.description), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                    Text(
                        if (at != null) stringResource(R.string.achievement_unlocked_on, formatDateKey(DateKeys.keyOf(at)))
                        else stringResource(R.string.achievement_locked),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (at != null) c.primary else c.outline,
                    )
                }
            }
        }
    }
}
