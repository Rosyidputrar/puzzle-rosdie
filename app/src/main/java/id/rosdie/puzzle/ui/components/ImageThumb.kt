package id.rosdie.puzzle.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import id.rosdie.puzzle.data.images.BuiltInImage
import id.rosdie.puzzle.data.images.ImageRepository
import id.rosdie.puzzle.data.repository.CustomImage
import java.io.File

/** Placeholder saat thumbnail gagal dimuat. */
@Composable
private fun ThumbPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Icon(AppIcons.Image, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Thumbnail gambar bawaan (dirender sekali lalu di-cache). */
@Composable
fun BuiltInThumb(image: BuiltInImage, repo: ImageRepository, contentDescription: String?, modifier: Modifier = Modifier) {
    val bmp by produceState<ImageBitmap?>(null, image) {
        value = runCatching { repo.builtInThumbnail(image).asImageBitmap() }.getOrNull()
    }
    val current = bmp
    if (current == null) {
        ThumbPlaceholder(modifier)
    } else {
        Image(current, contentDescription, modifier, contentScale = ContentScale.Crop)
    }
}

/** Thumbnail gambar pengguna lewat Coil (file lokal). */
@Composable
fun CustomThumb(image: CustomImage, contentDescription: String?, modifier: Modifier = Modifier) {
    var error by remember { mutableStateOf(false) }
    if (error) {
        ThumbPlaceholder(modifier)
    } else {
        AsyncImage(
            model = File(image.path),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            onError = { error = true },
        )
    }
}
