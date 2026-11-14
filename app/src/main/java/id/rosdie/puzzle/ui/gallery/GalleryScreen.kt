package id.rosdie.puzzle.ui.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.rosdie.puzzle.AppContainer
import id.rosdie.puzzle.R
import id.rosdie.puzzle.data.images.BuiltInImage
import id.rosdie.puzzle.data.images.ImageIds
import id.rosdie.puzzle.data.images.ImageRepository
import id.rosdie.puzzle.data.repository.CustomImage
import id.rosdie.puzzle.data.repository.GameConfig
import id.rosdie.puzzle.domain.model.ChallengeType
import id.rosdie.puzzle.domain.model.GameMode
import id.rosdie.puzzle.ui.components.AppIcons
import id.rosdie.puzzle.ui.components.BackTopBar
import id.rosdie.puzzle.ui.components.BuiltInThumb
import id.rosdie.puzzle.ui.components.CustomThumb
import id.rosdie.puzzle.ui.components.SectionTitle
import id.rosdie.puzzle.ui.components.formatMegabytes

private sealed interface Selected {
    data class BuiltIn(val image: BuiltInImage) : Selected
    data class Custom(val image: CustomImage) : Selected
}

@Composable
fun GalleryScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onUseImage: (GameConfig) -> Unit,
    viewModel: GalleryViewModel = viewModel(factory = GalleryViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<Selected?>(null) }
    var confirmDelete by remember { mutableStateOf<CustomImage?>(null) }
    val importer = rememberImageImport(container) { selected = Selected.Custom(it) }

    fun use(imageId: String) {
        selected = null
        onUseImage(GameConfig(state.difficultySize, GameMode.IMAGE, imageId, ChallengeType.NORMAL, null))
    }

    Scaffold(topBar = { BackTopBar(stringResource(R.string.nav_gallery), onBack) }) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 104.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StorageCard(state)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = importer.pickFromGallery, modifier = Modifier.weight(1f)) {
                            Icon(AppIcons.AddPhoto, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.gallery_add_gallery))
                        }
                        FilledTonalButton(onClick = importer.takePhoto, modifier = Modifier.weight(1f)) {
                            Icon(AppIcons.Camera, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.gallery_add_camera))
                        }
                    }
                    Text(
                        stringResource(R.string.gallery_formats),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SectionTitle(stringResource(R.string.gallery_mine))
                    if (state.images.isEmpty()) {
                        Text(stringResource(R.string.gallery_empty), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            items(state.images, key = { it.id }) { img ->
                val label = stringResource(R.string.image_custom)
                GridThumb(label, onClick = { selected = Selected.Custom(img) }) {
                    CustomThumb(img, label, Modifier.fillMaxSize())
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle(stringResource(R.string.gallery_builtin), Modifier.padding(top = 12.dp))
            }
            items(BuiltInImage.entries, key = { it.key }) { img ->
                val title = stringResource(img.titleRes)
                GridThumb(title, onClick = { selected = Selected.BuiltIn(img) }) {
                    BuiltInThumb(img, container.images, title, Modifier.fillMaxSize())
                }
            }
        }
    }

    selected?.let { sel ->
        PreviewDialog(
            selected = sel,
            repo = container.images,
            onDismiss = { selected = null },
            onUse = {
                when (sel) {
                    is Selected.BuiltIn -> use(sel.image.imageId)
                    is Selected.Custom -> use(ImageIds.custom(sel.image.id))
                }
            },
            onDelete = (sel as? Selected.Custom)?.let { { confirmDelete = sel.image } },
        )
    }

    confirmDelete?.let { img ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            icon = { Icon(AppIcons.Delete, null) },
            title = { Text(stringResource(R.string.gallery_delete_title)) },
            text = { Text(stringResource(R.string.gallery_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(img.id)
                    confirmDelete = null
                    selected = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun StorageCard(state: GalleryUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Text(
                stringResource(
                    R.string.gallery_storage,
                    formatMegabytes(state.usageBytes),
                    formatMegabytes(ImageRepository.MAX_TOTAL_BYTES),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { state.usageFraction },
                modifier = Modifier.fillMaxWidth(),
                color = if (state.nearlyFull) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            if (state.nearlyFull) {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.gallery_storage_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun GridThumb(label: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick),
    ) { content() }
}

@Composable
private fun PreviewDialog(
    selected: Selected,
    repo: ImageRepository,
    onDismiss: () -> Unit,
    onUse: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val id = when (selected) {
        is Selected.BuiltIn -> selected.image.imageId
        is Selected.Custom -> ImageIds.custom(selected.image.id)
    }
    val bmp by produceState<ImageBitmap?>(null, id) { value = repo.loadBoardBitmap(id)?.asImageBitmap() }
    val title = when (selected) {
        is Selected.BuiltIn -> stringResource(selected.image.titleRes)
        is Selected.Custom -> stringResource(R.string.image_custom)
    }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(16.dp).widthIn(max = 480.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    val current = bmp
                    if (current == null) {
                        if (selected is Selected.Custom) CustomThumb(selected.image, title, Modifier.fillMaxSize()) else CircularProgressIndicator()
                    } else {
                        Image(current, title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onUse, modifier = Modifier.fillMaxWidth()) {
                    Icon(AppIcons.Play, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.gallery_use))
                }
                if (onDelete != null) {
                    OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                        Icon(AppIcons.Delete, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.action_delete))
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
            }
        }
    }
}
