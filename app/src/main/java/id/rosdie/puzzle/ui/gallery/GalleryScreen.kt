package id.rosdie.puzzle.ui.gallery

import id.rosdie.puzzle.ui.components.AppCard
import id.rosdie.puzzle.ui.components.SecondaryButton
import id.rosdie.puzzle.ui.components.ConfirmDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import id.rosdie.puzzle.ui.components.PrimaryButton
import id.rosdie.puzzle.ui.components.ProgressRing
import kotlin.math.roundToInt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onUseImage: (GameConfig) -> Unit,
    viewModel: GalleryViewModel = viewModel(factory = GalleryViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val selected: Selected? = remember(selectedKey, state.images) {
        val key = selectedKey ?: return@remember null
        when {
            key.startsWith("b:") -> BuiltInImage.entries.firstOrNull { it.imageId == key.removePrefix("b:") }?.let { Selected.BuiltIn(it) }
            key.startsWith("c:") -> state.images.firstOrNull { it.id == key.removePrefix("c:") }?.let { Selected.Custom(it) }
            else -> null
        }
    }
    var confirmDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val confirmDelete: CustomImage? = remember(confirmDeleteId, state.images) {
        state.images.firstOrNull { it.id == confirmDeleteId }
    }
    val importer = rememberImageImport(container) { selectedKey = "c:${it.id}" }
    val sb = TopAppBarDefaults.pinnedScrollBehavior()

    fun use(imageId: String) {
        selectedKey = null
        onUseImage(GameConfig(state.difficultySize, GameMode.IMAGE, imageId, ChallengeType.NORMAL, null))
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.nestedScroll(sb.nestedScrollConnection),
        topBar = { BackTopBar(stringResource(R.string.nav_gallery), onBack, scrollBehavior = sb) }) { padding ->
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
                        SecondaryButton(stringResource(R.string.gallery_add_gallery), importer.pickFromGallery, Modifier.weight(1f), icon = AppIcons.AddPhoto)
                        SecondaryButton(stringResource(R.string.gallery_add_camera), importer.takePhoto, Modifier.weight(1f), icon = AppIcons.Camera)
                    }
                    Text(
                        stringResource(R.string.gallery_formats),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SectionTitle(stringResource(R.string.gallery_mine))
                    if (state.images.isEmpty()) {
                        // Empty state jujur: jelaskan kenapa kosong dan beri aksi langsung.
                        AppCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.gallery_empty_title), style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(4.dp))
                                Text(stringResource(R.string.gallery_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(12.dp))
                                PrimaryButton(stringResource(R.string.gallery_add_image), AppIcons.AddPhoto, importer.pickFromGallery, Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
            items(state.images, key = { it.id }) { img ->
                val label = stringResource(R.string.image_custom)
                GridThumb(label, onClick = { selectedKey = "c:${img.id}" }) {
                    CustomThumb(img, label, Modifier.fillMaxSize())
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle(stringResource(R.string.gallery_builtin), Modifier.padding(top = 12.dp))
            }
            items(BuiltInImage.entries, key = { it.key }) { img ->
                val title = stringResource(img.titleRes)
                GridThumb(title, onClick = { selectedKey = "b:${img.imageId}" }) {
                    BuiltInThumb(img, container.images, title, Modifier.fillMaxSize())
                }
            }
        }
    }

    selected?.let { sel ->
        PreviewDialog(
            selected = sel,
            repo = container.images,
            onDismiss = { selectedKey = null },
            onUse = {
                when (sel) {
                    is Selected.BuiltIn -> use(sel.image.imageId)
                    is Selected.Custom -> use(ImageIds.custom(sel.image.id))
                }
            },
            onDelete = (sel as? Selected.Custom)?.let { { confirmDeleteId = sel.image.id } },
        )
    }

    confirmDelete?.let { img ->
        ConfirmDialog(
            title = stringResource(R.string.gallery_delete_title),
            text = stringResource(R.string.gallery_delete_body),
            confirmText = stringResource(R.string.action_delete),
            onConfirm = {
                viewModel.delete(img.id)
                confirmDeleteId = null
                selectedKey = null
            },
            onDismiss = { confirmDeleteId = null },
            icon = AppIcons.Delete,
            dismissText = stringResource(R.string.action_cancel),
            destructive = true,
        )
    }
}

@Composable
private fun StorageCard(state: GalleryUiState) {
    val c = MaterialTheme.colorScheme
    AppCard {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                ProgressRing(state.usageFraction, Modifier.fillMaxSize(), color = if (state.nearlyFull) c.error else c.primary)
                Text("${(state.usageFraction * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.gallery_storage_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.gallery_storage, formatMegabytes(state.usageBytes), formatMegabytes(ImageRepository.MAX_TOTAL_BYTES)),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant,
                )
                if (state.nearlyFull) {
                    Text(stringResource(R.string.gallery_storage_warning), style = MaterialTheme.typography.bodySmall, color = c.error)
                }
            }
        }
    }
}

@Composable
private fun GridThumb(label: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick),
    ) {
        content()
        // Scrim gradient + label agar thumbnail punya identitas, bukan sekadar kotak gambar.
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                .padding(start = 10.dp, end = 10.dp, top = 18.dp, bottom = 8.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White, maxLines = 1)
        }
    }
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
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp).widthIn(max = 480.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
                PrimaryButton(stringResource(R.string.gallery_use), AppIcons.Play, onUse, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onDelete != null) {
                        SecondaryButton(stringResource(R.string.action_delete), onDelete, Modifier.weight(1f), icon = AppIcons.Delete, contentColor = MaterialTheme.colorScheme.error)
                    }
                    SecondaryButton(stringResource(R.string.action_close), onDismiss, Modifier.weight(1f), icon = AppIcons.Close)
                }
            }
        }
    }
}
