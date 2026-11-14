package id.rosdie.puzzle.ui.gallery

import id.rosdie.puzzle.ui.components.AppIcons
import id.rosdie.puzzle.ui.components.ConfirmDialog
import id.rosdie.puzzle.ui.components.PrimaryButton
import id.rosdie.puzzle.ui.components.SecondaryButton
import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import id.rosdie.puzzle.AppContainer
import id.rosdie.puzzle.R
import id.rosdie.puzzle.data.images.ImageError
import id.rosdie.puzzle.data.images.ImageResult
import id.rosdie.puzzle.data.repository.CustomImage
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Alur tambah gambar: Photo Picker / kamera -> validasi -> crop persegi -> simpan. */
@Stable
class ImageImportHandle internal constructor(
    val pickFromGallery: () -> Unit,
    val takePhoto: () -> Unit,
    internal val state: ImportState,
)

@Stable
internal class ImportState {
    var source by mutableStateOf<Bitmap?>(null)
    var busy by mutableStateOf(false)
    var error by mutableStateOf<ImageError?>(null)
    var cameraUnavailable by mutableStateOf(false)
}

@Composable
fun rememberImageImport(container: AppContainer, onImported: (CustomImage) -> Unit): ImageImportHandle {
    val state = remember { ImportState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val onImportedState by rememberUpdatedState(onImported)

    fun load(uri: Uri) {
        state.busy = true
        scope.launch {
            when (val r = container.images.prepare(uri)) {
                is ImageResult.Success -> state.source = r.value
                is ImageResult.Failure -> state.error = r.error
            }
            state.busy = false
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) load(uri)
    }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingCameraUri
        if (ok && uri != null) load(uri)
    }
    val handle = remember {
        ImageImportHandle(
            pickFromGallery = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            takePhoto = {
                val file = container.images.cameraCaptureFile()
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                pendingCameraUri = uri
                try {
                    camera.launch(uri)
                } catch (_: ActivityNotFoundException) {
                    state.cameraUnavailable = true
                }
            },
            state = state,
        )
    }

    ImportHost(
        state = state,
        onCrop = { bitmap, rect ->
            state.busy = true
            scope.launch {
                when (val r = container.images.importCropped(bitmap, rect)) {
                    is ImageResult.Success -> {
                        state.source = null
                        container.achievements.evaluate(container.games.getAllResults(), container.images.customCount())
                        onImportedState(r.value)
                    }
                    is ImageResult.Failure -> state.error = r.error
                }
                state.busy = false
            }
        },
    )
    return handle
}

@Composable
private fun ImportHost(state: ImportState, onCrop: (Bitmap, Rect) -> Unit) {
    state.source?.let { bmp ->
        CropDialog(bitmap = bmp, busy = state.busy, onCancel = { state.source = null }, onConfirm = { onCrop(bmp, it) })
    }
    if (state.busy && state.source == null) {
        Dialog(onDismissRequest = {}) { CircularProgressIndicator() }
    }
    state.error?.let { err ->
        ConfirmDialog(
            title = stringResource(R.string.image_error_title),
            text = stringResource(imageErrorMessage(err)),
            confirmText = stringResource(R.string.action_ok),
            onConfirm = { state.error = null },
            onDismiss = { state.error = null },
            icon = AppIcons.Image,
        )
    }
    if (state.cameraUnavailable) {
        ConfirmDialog(
            title = stringResource(R.string.image_error_title),
            text = stringResource(R.string.image_error_no_camera),
            confirmText = stringResource(R.string.action_ok),
            onConfirm = { state.cameraUnavailable = false },
            onDismiss = { state.cameraUnavailable = false },
            icon = AppIcons.Camera,
        )
    }
}

fun imageErrorMessage(e: ImageError): Int = when (e) {
    ImageError.TOO_LARGE -> R.string.image_error_too_large
    ImageError.TOO_SMALL -> R.string.image_error_too_small
    ImageError.UNSUPPORTED -> R.string.image_error_unsupported
    ImageError.CORRUPT -> R.string.image_error_corrupt
    ImageError.STORAGE_FULL -> R.string.image_error_storage_full
    ImageError.WRITE_FAILED -> R.string.image_error_write_failed
    ImageError.NOT_FOUND -> R.string.image_error_not_found
}

/** Crop persegi: geser dan cubit gambar di bawah bingkai persegi. */
@Composable
fun CropDialog(bitmap: Bitmap, busy: Boolean, onCancel: () -> Unit, onConfirm: (Rect) -> Unit) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    var userScale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    // Geometri bingkai ditulis ke state lewat SideEffect (bukan dimutasi saat composition).
    var frameState by remember { mutableFloatStateOf(0f) }
    var baseState by remember { mutableFloatStateOf(1f) }
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .safeDrawingPadding()
                .padding(16.dp),
        ) {
            Text(
                stringResource(R.string.crop_title),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Text(
                stringResource(R.string.crop_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                val density = LocalDensity.current
                val w = with(density) { maxWidth.toPx() }
                val h = with(density) { maxHeight.toPx() }
                val frame = min(w, h) * 0.92f
                val base = frame / min(bitmap.width, bitmap.height)
                SideEffect {
                    frameState = frame
                    baseState = base
                }
                val cropDesc = stringResource(R.string.crop_a11y)
                fun clamp(o: Offset, scale: Float): Offset {
                    val ds = base * scale
                    val maxX = max(0f, (bitmap.width * ds - frame) / 2)
                    val maxY = max(0f, (bitmap.height * ds - frame) / 2)
                    return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
                }
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = cropDesc }
                        .pointerInput(bitmap) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                userScale = (userScale * zoom).coerceIn(1f, 5f)
                                offset = clamp(offset + pan, userScale)
                            }
                        },
                ) {
                    val ds = base * userScale
                    val center = Offset(size.width / 2, size.height / 2)
                    val imgW = bitmap.width * ds
                    val imgH = bitmap.height * ds
                    val topLeft = center + offset - Offset(imgW / 2, imgH / 2)
                    drawImage(
                        image,
                        dstOffset = IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()),
                        dstSize = IntSize(imgW.roundToInt(), imgH.roundToInt()),
                        filterQuality = FilterQuality.Medium,
                    )
                    val fr = ComposeRect(center - Offset(frame / 2, frame / 2), Size(frame, frame))
                    val dim = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(ComposeRect(Offset.Zero, size))
                        addRect(fr)
                    }
                    drawPath(dim, Color.Black.copy(alpha = 0.6f))
                    drawRect(Color.White, fr.topLeft, fr.size, style = Stroke(2.dp.toPx()))
                    for (i in 1..2) {
                        val x = fr.left + fr.width * i / 3
                        val y = fr.top + fr.height * i / 3
                        drawLine(Color.White.copy(alpha = 0.5f), Offset(x, fr.top), Offset(x, fr.bottom), 1.dp.toPx())
                        drawLine(Color.White.copy(alpha = 0.5f), Offset(fr.left, y), Offset(fr.right, y), 1.dp.toPx())
                    }
                }
                if (busy) CircularProgressIndicator()
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(stringResource(R.string.action_cancel), onCancel, Modifier.weight(1f), enabled = !busy, contentColor = Color.White)
                PrimaryButton(
                    stringResource(R.string.crop_confirm),
                    null,
                    { onConfirm(cropRect(frameState, baseState, bitmap.width, bitmap.height, userScale, offset)) },
                    Modifier.weight(1f),
                    enabled = !busy && frameState > 0f,
                )
            }
            Text(
                stringResource(R.string.crop_footer),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}


/** Hitung rect crop persegi dari geometri bingkai: fungsi murni, tanpa state. */
private fun cropRect(frame: Float, base: Float, bw: Int, bh: Int, scale: Float, offset: Offset): Rect {
    val ds = base * scale
    // Posisi bingkai relatif terhadap pojok kiri atas gambar, dalam piksel tampilan.
    val relLeft = (bw * ds - frame) / 2 - offset.x
    val relTop = (bh * ds - frame) / 2 - offset.y
    val left = (relLeft / ds).roundToInt().coerceIn(0, bw - 1)
    val top = (relTop / ds).roundToInt().coerceIn(0, bh - 1)
    val side = (frame / ds).roundToInt().coerceAtMost(min(bw - left, bh - top)).coerceAtLeast(1)
    return Rect(left, top, left + side, top + side)
}
