package id.rosdie.puzzle.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import id.rosdie.puzzle.data.images.BuiltInImage
import id.rosdie.puzzle.data.images.ImageRepository
import id.rosdie.puzzle.data.repository.CustomImage
import java.io.File

/** Thumbnail gambar bawaan (dirender sekali lalu di-cache). */
@Composable
fun BuiltInThumb(image: BuiltInImage, repo: ImageRepository, contentDescription: String?, modifier: Modifier = Modifier) {
    val bmp by produceState<ImageBitmap?>(null, image) { value = repo.builtInThumbnail(image).asImageBitmap() }
    val current = bmp
    if (current == null) {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    } else {
        Image(current, contentDescription, modifier, contentScale = ContentScale.Crop)
    }
}

/** Thumbnail gambar pengguna lewat Coil (file lokal). */
@Composable
fun CustomThumb(image: CustomImage, contentDescription: String?, modifier: Modifier = Modifier) {
    AsyncImage(
        model = File(image.path),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
    )
}
