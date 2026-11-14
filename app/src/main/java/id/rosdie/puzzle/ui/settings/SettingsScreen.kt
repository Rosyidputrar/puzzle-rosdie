package id.rosdie.puzzle.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import id.rosdie.puzzle.ui.components.AppSlider
import id.rosdie.puzzle.ui.components.PillSelector
import id.rosdie.puzzle.ui.components.SectionCard
import id.rosdie.puzzle.ui.components.Tone
import id.rosdie.puzzle.ui.components.accentBrush
import id.rosdie.puzzle.ui.splash.LogoMark
import id.rosdie.puzzle.ui.theme.DarkColors
import id.rosdie.puzzle.ui.theme.LightColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.rosdie.puzzle.AppContainer
import id.rosdie.puzzle.BuildConfig
import id.rosdie.puzzle.R
import id.rosdie.puzzle.data.datastore.AnimationSpeed
import id.rosdie.puzzle.data.datastore.ClickSound
import id.rosdie.puzzle.data.datastore.ThemeMode
import id.rosdie.puzzle.domain.model.Difficulty
import id.rosdie.puzzle.ui.components.AppIcons
import id.rosdie.puzzle.ui.components.BackTopBar
import id.rosdie.puzzle.ui.home.difficultyLabel
import id.rosdie.puzzle.ui.theme.supportsDynamicColor
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    val clearing by viewModel.clearing.collectAsStateWithLifecycle()
    val cleared by viewModel.cleared.collectAsStateWithLifecycle()
    var deleteStep by remember { mutableIntStateOf(0) }
    var showLicenses by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val clearedText = stringResource(R.string.settings_data_cleared)
    LaunchedEffect(cleared) {
        if (cleared) {
            snackbar.showSnackbar(clearedText)
            viewModel.consumeCleared()
        }
    }

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.nav_settings), onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
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

            SectionCard(stringResource(R.string.settings_game), m, icon = AppIcons.Grid, tone = Tone.PRIMARY) {
                Label(stringResource(R.string.settings_difficulty))
                PillSelector(
                    options = Difficulty.entries.toList(),
                    selected = s.difficulty,
                    onSelect = { d -> viewModel.update { it.copy(difficulty = d) } },
                    label = { difficultyLabel(it.size) },
                    sublabel = { stringResource(R.string.grid_label, it.size, it.size) },
                    height = 52.dp,
                )
                Spacer(Modifier.height(8.dp))
                SwitchRow(AppIcons.Visibility, stringResource(R.string.settings_reference), stringResource(R.string.settings_reference_desc), s.showReference) { v ->
                    viewModel.update { it.copy(showReference = v) }
                }
                Label(stringResource(R.string.settings_animation))
                PillSelector(
                    options = AnimationSpeed.entries.toList(),
                    selected = s.animationSpeed,
                    onSelect = { a -> viewModel.update { it.copy(animationSpeed = a) } },
                    label = {
                        stringResource(
                            when (it) {
                                AnimationSpeed.SLOW -> R.string.anim_slow
                                AnimationSpeed.NORMAL -> R.string.anim_normal
                                AnimationSpeed.FAST -> R.string.anim_fast
                            },
                        )
                    },
                )
                Text(
                    stringResource(R.string.settings_animation_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            SectionCard(stringResource(R.string.settings_appearance), m, icon = AppIcons.Palette, tone = Tone.TERTIARY) {
                Label(stringResource(R.string.settings_theme))
                ThemePicker(s.themeMode) { t -> viewModel.update { it.copy(themeMode = t) } }
                SwitchRow(
                    AppIcons.Palette,
                    stringResource(R.string.settings_dynamic),
                    stringResource(if (supportsDynamicColor) R.string.settings_dynamic_desc else R.string.settings_dynamic_unsupported),
                    s.dynamicColor && supportsDynamicColor,
                    enabled = supportsDynamicColor,
                ) { v -> viewModel.update { it.copy(dynamicColor = v) } }
                Label(stringResource(R.string.settings_language))
                LanguagePicker()
            }

            SectionCard(stringResource(R.string.settings_sound_haptics), m, icon = AppIcons.Volume, tone = Tone.SECONDARY) {
                SwitchRow(AppIcons.Volume, stringResource(R.string.settings_sound), null, s.soundEnabled) { v ->
                    viewModel.update { it.copy(soundEnabled = v) }
                }
                if (s.soundEnabled) {
                    SliderRow(stringResource(R.string.settings_volume), s.soundVolume) { v ->
                        viewModel.update { it.copy(soundVolume = v) }
                        container.sound?.click()
                    }
                    Label(stringResource(R.string.settings_click_sound))
                    PillSelector(
                        options = ClickSound.entries.toList(),
                        selected = s.clickSound,
                        onSelect = { c ->
                            viewModel.update { it.copy(clickSound = c) }
                            container.sound?.preview(c)
                        },
                        label = {
                            stringResource(
                                when (it) {
                                    ClickSound.SOFT -> R.string.click_soft
                                    ClickSound.WOOD -> R.string.click_wood
                                    ClickSound.POP -> R.string.click_pop
                                },
                            )
                        },
                    )
                }
                SwitchRow(AppIcons.Vibration, stringResource(R.string.settings_haptics), null, s.hapticsEnabled) { v ->
                    viewModel.update { it.copy(hapticsEnabled = v) }
                }
                if (s.hapticsEnabled) {
                    SliderRow(stringResource(R.string.settings_haptic_strength), s.hapticStrength) { v ->
                        viewModel.update { it.copy(hapticStrength = v) }
                        container.haptics?.preview()
                    }
                }
                SwitchRow(
                    AppIcons.Trophy,
                    stringResource(R.string.settings_celebration),
                    stringResource(R.string.settings_celebration_desc),
                    s.winCelebration,
                ) { v -> viewModel.update { it.copy(winCelebration = v) } }
            }

            SectionCard(stringResource(R.string.settings_data), m, icon = AppIcons.Lock, tone = Tone.ERROR) {
                Text(stringResource(R.string.settings_data_desc), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { deleteStep = 1 },
                    enabled = !clearing,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    if (clearing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(AppIcons.Delete, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.settings_delete_all))
                }
            }

            SectionCard(stringResource(R.string.settings_about), m, icon = AppIcons.Info, tone = Tone.NEUTRAL) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LogoMark(
                        Modifier
                            .size(56.dp)
                            .background(accentBrush(), RoundedCornerShape(16.dp))
                            .padding(10.dp),
                    )
                    Spacer(Modifier.size(14.dp))
                    Column {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            stringResource(R.string.settings_author) + ": " + stringResource(R.string.settings_author_value),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.settings_privacy), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { showLicenses = true }) { Text(stringResource(R.string.settings_licenses)) }
            }
        }
    }

    if (deleteStep == 1) {
        AlertDialog(
            onDismissRequest = { deleteStep = 0 },
            icon = { Icon(AppIcons.Delete, null) },
            title = { Text(stringResource(R.string.delete_step1_title)) },
            text = { Text(stringResource(R.string.delete_step1_body)) },
            confirmButton = { TextButton(onClick = { deleteStep = 2 }) { Text(stringResource(R.string.action_continue)) } },
            dismissButton = { TextButton(onClick = { deleteStep = 0 }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (deleteStep == 2) {
        AlertDialog(
            onDismissRequest = { deleteStep = 0 },
            title = { Text(stringResource(R.string.delete_step2_title)) },
            text = { Text(stringResource(R.string.delete_step2_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        deleteStep = 0
                        viewModel.clearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                ) { Text(stringResource(R.string.delete_step2_confirm)) }
            },
            dismissButton = { TextButton(onClick = { deleteStep = 0 }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            title = { Text(stringResource(R.string.settings_licenses)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.licenses_body), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { showLicenses = false }) { Text(stringResource(R.string.action_close)) } },
        )
    }
}

@Composable
private fun LanguagePicker() {
    var current by remember { mutableStateOf(AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')) }
    val options = listOf("", "id", "en")
    PillSelector(
        options = options,
        selected = if (current in options) current else "",
        onSelect = { tag ->
            current = tag
            AppCompatDelegate.setApplicationLocales(
                if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
            )
        },
        label = {
            stringResource(
                when (it) {
                    "id" -> R.string.language_id_short
                    "en" -> R.string.language_en
                    else -> R.string.language_system_short
                },
            )
        },
    )
}

/** Pemilih tema visual: tiap opsi menampilkan pratinjau mini layar terang/gelap. */
@Composable
private fun ThemePicker(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = mode == selected
            val label = stringResource(
                when (mode) {
                    ThemeMode.LIGHT -> R.string.theme_light
                    ThemeMode.DARK -> R.string.theme_dark
                    ThemeMode.SYSTEM -> R.string.theme_system
                },
            )
            Column(
                Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .selectable(isSelected, role = Role.RadioButton) { onSelect(mode) }
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val border = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.8f)
                        .border(if (isSelected) 3.dp else 1.dp, border, MaterialTheme.shapes.medium)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(12.dp)),
                ) {
                    when (mode) {
                        ThemeMode.LIGHT -> MiniScreen(LightColors.background, LightColors.primary, LightColors.surfaceContainerHigh, Modifier.fillMaxSize())
                        ThemeMode.DARK -> MiniScreen(DarkColors.background, DarkColors.primary, DarkColors.surfaceContainerHigh, Modifier.fillMaxSize())
                        ThemeMode.SYSTEM -> Row(Modifier.fillMaxSize()) {
                            MiniScreen(LightColors.background, LightColors.primary, LightColors.surfaceContainerHigh, Modifier.weight(1f).fillMaxHeight())
                            MiniScreen(DarkColors.background, DarkColors.primary, DarkColors.surfaceContainerHigh, Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

/** Sketsa layar kecil: latar, bilah judul, dan grid tile 3x3 berwarna primary. */
@Composable
private fun MiniScreen(background: Color, accent: Color, card: Color, modifier: Modifier) {
    Canvas(modifier.background(background)) {
        val pad = size.width * 0.12f
        drawRoundRect(card, Offset(pad, pad), Size(size.width - 2 * pad, size.height * 0.12f), CornerRadius(6f))
        val gridTop = size.height * 0.34f
        val cell = (size.width - 2 * pad) / 3f
        for (i in 0 until 8) {
            val x = pad + (i % 3) * cell
            val y = gridTop + (i / 3) * cell
            drawRoundRect(accent, Offset(x + cell * 0.08f, y + cell * 0.08f), Size(cell * 0.84f, cell * 0.84f), CornerRadius(cell * 0.2f))
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun RadioRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SliderRow(title: String, value: Float, onFinished: (Float) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value) }
    val percent = "${(local * 100).roundToInt()}%"
    Column {
        Row {
            Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(percent, style = MaterialTheme.typography.bodyMedium)
        }
        AppSlider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onFinished(local) },
            modifier = Modifier.semantics { contentDescription = title },
        )
    }
}
