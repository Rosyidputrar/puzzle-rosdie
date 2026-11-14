package id.rosdie.puzzle.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.rosdie.puzzle.AppContainer
import id.rosdie.puzzle.R
import id.rosdie.puzzle.data.images.BuiltInImage
import id.rosdie.puzzle.data.images.ImageIds
import id.rosdie.puzzle.data.repository.GameConfig
import id.rosdie.puzzle.domain.model.ChallengeRules
import id.rosdie.puzzle.domain.model.ChallengeType
import id.rosdie.puzzle.domain.model.Difficulty
import id.rosdie.puzzle.domain.model.GameMode
import id.rosdie.puzzle.ui.components.AppIcons
import id.rosdie.puzzle.ui.components.BuiltInThumb
import id.rosdie.puzzle.ui.components.CustomThumb
import id.rosdie.puzzle.ui.components.formatSecondsClock
import id.rosdie.puzzle.ui.gallery.rememberImageImport

@Composable
fun difficultyLabel(size: Int): String = stringResource(
    when (size) {
        3 -> R.string.difficulty_easy
        4 -> R.string.difficulty_normal
        else -> R.string.difficulty_hard
    },
)

/** Pilih mode, gambar, kesulitan, dan jenis permainan sebelum mulai. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSetupSheet(
    container: AppContainer,
    initial: GameConfig,
    onStart: (GameConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mode by remember { mutableStateOf(initial.mode) }
    var imageId by remember { mutableStateOf(initial.imageId ?: BuiltInImage.SUNSET.imageId) }
    var size by remember { mutableIntStateOf(initial.size) }
    var challenge by remember { mutableStateOf(initial.challenge) }
    val custom by container.images.customImages.collectAsStateWithLifecycle(emptyList())
    val importer = rememberImageImport(container) { img ->
        imageId = ImageIds.custom(img.id)
        mode = GameMode.IMAGE
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            Label(stringResource(R.string.setup_mode))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                GameMode.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = mode == m,
                        onClick = { mode = m },
                        shape = SegmentedButtonDefaults.itemShape(i, GameMode.entries.size),
                        icon = { SegmentedButtonDefaults.Icon(mode == m) { Icon(if (m == GameMode.NUMBERS) AppIcons.Numbers else AppIcons.Image, null, Modifier.size(18.dp)) } },
                    ) { Text(stringResource(if (m == GameMode.NUMBERS) R.string.mode_numbers else R.string.mode_image)) }
                }
            }
            if (mode == GameMode.IMAGE) {
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(BuiltInImage.entries, key = { it.key }) { img ->
                        val title = stringResource(img.titleRes)
                        ThumbChoice(selected = imageId == img.imageId, label = title, onClick = { imageId = img.imageId }) {
                            BuiltInThumb(img, container.images, null, Modifier.size(76.dp))
                        }
                    }
                    items(custom, key = { it.id }) { img ->
                        val id = ImageIds.custom(img.id)
                        ThumbChoice(selected = imageId == id, label = stringResource(R.string.image_custom), onClick = { imageId = id }) {
                            CustomThumb(img, null, Modifier.size(76.dp))
                        }
                    }
                    item {
                        AddChoice(AppIcons.AddPhoto, stringResource(R.string.gallery_add_gallery), importer.pickFromGallery)
                    }
                    item {
                        AddChoice(AppIcons.Camera, stringResource(R.string.gallery_add_camera), importer.takePhoto)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Label(stringResource(R.string.setup_difficulty))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Difficulty.entries.forEachIndexed { i, d ->
                    SegmentedButton(
                        selected = size == d.size,
                        onClick = { size = d.size },
                        shape = SegmentedButtonDefaults.itemShape(i, Difficulty.entries.size),
                        icon = {},
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(difficultyLabel(d.size))
                            Text(stringResource(R.string.grid_label, d.size, d.size), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Label(stringResource(R.string.setup_challenge))
            Column(Modifier.selectableGroup()) {
                ChallengeType.entries.forEach { c ->
                    val desc = when (c) {
                        ChallengeType.NORMAL -> stringResource(R.string.challenge_normal_desc)
                        ChallengeType.TARGET_MOVES -> ChallengeRules.targetMoves(size).let {
                            stringResource(R.string.challenge_target_desc, pluralStringResource(R.plurals.moves_count, it, it))
                        }
                        ChallengeType.TIME_ATTACK -> stringResource(R.string.challenge_time_desc, formatSecondsClock(ChallengeRules.timeLimitSeconds(size)))
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(selected = challenge == c, onClick = { challenge = c }, role = Role.RadioButton)
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = challenge == c, onClick = null)
                        Spacer(Modifier.size(8.dp))
                        Column {
                            Text(stringResource(challengeName(c)), style = MaterialTheme.typography.bodyLarge)
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    onStart(GameConfig(size, mode, imageId.takeIf { mode == GameMode.IMAGE }, challenge, null))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(AppIcons.Play, null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.action_start), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

fun challengeName(c: ChallengeType): Int = when (c) {
    ChallengeType.NORMAL -> R.string.challenge_normal
    ChallengeType.TARGET_MOVES -> R.string.challenge_target
    ChallengeType.TIME_ATTACK -> R.string.challenge_time
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun ThumbChoice(selected: Boolean, label: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.size(width = 80.dp, height = 104.dp)) {
        Box(
            Modifier
                .clip(shape)
                .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, shape)
                .semantics { this.selected = selected }
                .clickable(onClickLabel = label, role = Role.RadioButton, onClick = onClick),
        ) { content() }
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun AddChoice(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.size(width = 80.dp, height = 104.dp)) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(76.dp),
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null) }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}
