package id.rosdie.puzzle.ui.home

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.rosdie.puzzle.AppContainer
import id.rosdie.puzzle.R
import id.rosdie.puzzle.data.repository.GameConfig
import id.rosdie.puzzle.data.repository.StatsSummary
import id.rosdie.puzzle.domain.model.ChallengeType
import id.rosdie.puzzle.domain.model.GameMode
import id.rosdie.puzzle.ui.components.AppIcons
import id.rosdie.puzzle.ui.components.SectionTitle
import id.rosdie.puzzle.ui.components.formatClock
import id.rosdie.puzzle.ui.components.formatSecondsClock
import id.rosdie.puzzle.ui.splash.LogoMark
import id.rosdie.puzzle.ui.theme.BrandBlue
import id.rosdie.puzzle.ui.theme.BrandBlueDark
import id.rosdie.puzzle.ui.theme.BrandBlueLight

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
            Hero(content, onPlay = { showSetup = true })
            state.saved?.let { saved ->
                ContinueCard(
                    size = saved.config.size,
                    moves = saved.moves,
                    elapsedMs = saved.elapsedMs,
                    daily = saved.config.isDaily,
                    onClick = onResume,
                    modifier = content,
                )
            }
            DailyCard(state.stats, onDaily, content)
            NavGrid(content, onGallery, onStats, onHowTo, onSettings)
            StatsCard(state.stats, state.loading, content)
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
private fun Hero(modifier: Modifier, onPlay: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(BrandBlueLight, BrandBlue, BrandBlueDark)))
            .padding(24.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            LogoMark(Modifier.size(88.dp), animate = true)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.home_tagline), style = MaterialTheme.typography.bodyLarge, color = Color.White)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BrandBlueDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp),
                shape = RoundedCornerShape(20.dp),
            ) {
                Icon(AppIcons.Play, null, Modifier.size(28.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.action_play), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ContinueCard(size: Int, moves: Int, elapsedMs: Long, daily: Boolean, onClick: () -> Unit, modifier: Modifier) {
    ElevatedCard(onClick = onClick, modifier = modifier) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(AppIcons.Play, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            }
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_continue), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(
                        if (daily) R.string.home_continue_daily else R.string.home_continue_body,
                        stringResource(R.string.grid_label, size, size),
                        pluralStringResource(R.plurals.moves_count, moves, moves),
                        formatClock(elapsedMs),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(AppIcons.ChevronRight, null)
        }
    }
}

@Composable
private fun DailyCard(stats: StatsSummary?, onClick: () -> Unit, modifier: Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Calendar, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_daily), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(if (stats?.todayDailyDone == true) R.string.home_daily_done else R.string.home_daily_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            val streak = stats?.currentStreak ?: 0
            if (streak > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.Fire, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(
                        stringResource(R.string.streak_days_short, streak),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun NavGrid(modifier: Modifier, onGallery: () -> Unit, onStats: () -> Unit, onHowTo: () -> Unit, onSettings: () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NavTile(AppIcons.Image, stringResource(R.string.nav_gallery), onGallery, Modifier.weight(1f))
            NavTile(AppIcons.BarChart, stringResource(R.string.nav_stats), onStats, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NavTile(AppIcons.Help, stringResource(R.string.nav_howto), onHowTo, Modifier.weight(1f))
            NavTile(AppIcons.Settings, stringResource(R.string.nav_settings), onSettings, Modifier.weight(1f))
        }
    }
}

@Composable
private fun NavTile(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 88.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            Text(label, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun StatsCard(stats: StatsSummary?, loading: Boolean, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle(stringResource(R.string.home_stats_title))
            Spacer(Modifier.height(8.dp))
            when {
                loading -> Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodyMedium)
                stats == null || !stats.hasGames -> Text(
                    stringResource(R.string.no_games_yet),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> {
                    Row(Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.home_total_solved), Modifier.weight(1f))
                        Text(stats.totalSolved.toString(), fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.home_streak), Modifier.weight(1f))
                        Text(pluralStringResource(R.plurals.days_count, stats.currentStreak, stats.currentStreak), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.stat_grid), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                        Text(stringResource(R.string.home_best_moves), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                        Text(stringResource(R.string.home_best_time), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                    }
                    stats.perGrid.forEach { g ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(stringResource(R.string.grid_label, g.gridSize, g.gridSize), Modifier.weight(1f))
                            val none = stringResource(R.string.value_none)
                            Text(g.record?.bestMoves?.toString() ?: none, Modifier.weight(1f))
                            Text(g.record?.bestTimeSeconds?.let { formatSecondsClock(it) } ?: none, Modifier.weight(1f))
                        }
                    }
                    Text(
                        stringResource(R.string.home_record_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

