package id.rosdie.puzzle.ui.settings

import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import id.rosdie.puzzle.ui.components.SectionTitle
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

            Section(stringResource(R.string.settings_game), m) {
                Label(stringResource(R.string.settings_difficulty))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Difficulty.entries.forEachIndexed { i, d ->
                        SegmentedButton(
                            selected = s.difficulty == d,
                            onClick = { viewModel.update { it.copy(difficulty = d) } },
                            shape = SegmentedButtonDefaults.itemShape(i, Difficulty.entries.size),
                        ) { Text(difficultyLabel(d.size)) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                SwitchRow(AppIcons.Visibility, stringResource(R.string.settings_reference), stringResource(R.string.settings_reference_desc), s.showReference) { v ->
                    viewModel.update { it.copy(showReference = v) }
                }
                Label(stringResource(R.string.settings_animation))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    AnimationSpeed.entries.forEachIndexed { i, a ->
                        SegmentedButton(
                            selected = s.animationSpeed == a,
                            onClick = { viewModel.update { it.copy(animationSpeed = a) } },
                            shape = SegmentedButtonDefaults.itemShape(i, AnimationSpeed.entries.size),
                        ) {
                            Text(stringResource(when (a) {
                                AnimationSpeed.SLOW -> R.string.anim_slow
                                AnimationSpeed.NORMAL -> R.string.anim_normal
                                AnimationSpeed.FAST -> R.string.anim_fast
                            }))
                        }
                    }
                }
                Text(
                    stringResource(R.string.settings_animation_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Section(stringResource(R.string.settings_appearance), m) {
                Label(stringResource(R.string.settings_theme))
                Column(Modifier.selectableGroup()) {
                    ThemeMode.entries.forEach { t ->
                        RadioRow(
                            text = stringResource(when (t) {
                                ThemeMode.LIGHT -> R.string.theme_light
                                ThemeMode.DARK -> R.string.theme_dark
                                ThemeMode.SYSTEM -> R.string.theme_system
                            }),
                            selected = s.themeMode == t,
                        ) { viewModel.update { it.copy(themeMode = t) } }
                    }
                }
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

            Section(stringResource(R.string.settings_sound_haptics), m) {
                SwitchRow(AppIcons.Volume, stringResource(R.string.settings_sound), null, s.soundEnabled) { v ->
                    viewModel.update { it.copy(soundEnabled = v) }
                }
                if (s.soundEnabled) {
                    SliderRow(stringResource(R.string.settings_volume), s.soundVolume) { v ->
                        viewModel.update { it.copy(soundVolume = v) }
                        container.sound?.click()
                    }
                    Label(stringResource(R.string.settings_click_sound))
                    Column(Modifier.selectableGroup()) {
                        ClickSound.entries.forEach { c ->
                            RadioRow(
                                text = stringResource(when (c) {
                                    ClickSound.SOFT -> R.string.click_soft
                                    ClickSound.WOOD -> R.string.click_wood
                                    ClickSound.POP -> R.string.click_pop
                                }),
                                selected = s.clickSound == c,
                            ) {
                                viewModel.update { it.copy(clickSound = c) }
                                container.sound?.preview(c)
                            }
                        }
                    }
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

            Section(stringResource(R.string.settings_data), m) {
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

            Section(stringResource(R.string.settings_about), m) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.app_name)) },
                    supportingContent = { Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)) },
                    leadingContent = { Icon(AppIcons.Info, null) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_author)) },
                    supportingContent = { Text(stringResource(R.string.settings_author_value)) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                )
                Text(stringResource(R.string.settings_privacy), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))
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
    var current by remember { mutableStateOf(AppCompatDelegate.getApplicationLocales().toLanguageTags()) }
    val options = listOf(
        "" to stringResource(R.string.language_system),
        "id" to stringResource(R.string.language_id),
        "en" to stringResource(R.string.language_en),
    )
    Column(Modifier.selectableGroup()) {
        options.forEach { (tag, label) ->
            RadioRow(label, selected = current.substringBefore('-') == tag || (tag.isEmpty() && current.isEmpty())) {
                current = tag
                AppCompatDelegate.setApplicationLocales(
                    if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, modifier: Modifier, content: @Composable () -> Unit) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle(title)
            Spacer(Modifier.height(8.dp))
            content()
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
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onFinished(local) },
            modifier = Modifier.semantics { contentDescription = title },
        )
    }
}
