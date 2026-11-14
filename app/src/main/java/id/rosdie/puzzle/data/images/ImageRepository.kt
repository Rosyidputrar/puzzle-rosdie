package id.rosdie.puzzle.data.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.provider.OpenableColumns
import android.util.LruCache
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import id.rosdie.puzzle.data.local.CustomImageDao
import id.rosdie.puzzle.data.local.CustomImageEntity
import id.rosdie.puzzle.data.repository.CustomImage
import id.rosdie.puzzle.data.repository.TimeSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID

enum class ImageError { TOO_LARGE, TOO_SMALL, UNSUPPORTED, CORRUPT, STORAGE_FULL, WRITE_FAILED, NOT_FOUND }

sealed interface ImageResult<out T> {
    data class Success<T>(val value: T) : ImageResult<T>
    data class Failure(val error: ImageError) : ImageResult<Nothing>
}

class ImageRepository(
    context: Context,
    private val dao: CustomImageDao,
    private val time: TimeSource,
    private val io: CoroutineDispatcher,
) {
    private val appContext = context.applicationContext
    private val dir = File(appContext.filesDir, "images")
    private val boardCache = LruCache<String, Bitmap>(3)
    private val thumbCache = LruCache<String, Bitmap>(16)

    val customImages: Flow<List<CustomImage>> = dao.observeAll().map { list ->
        list.map { CustomImage(it.id, it.path, it.sizeBytes, it.createdAt) }
    }

    val usageBytes: Flow<Long> = customImages.map { list -> list.sumOf { it.sizeBytes } }

    /**
     * Validasi dan decode gambar dari Photo Picker / kamera. Hasilnya bitmap dengan orientasi
     * EXIF sudah diterapkan (sisi terpanjang maks 4096 px) untuk layar crop.
     */
    suspend fun prepare(uri: Uri): ImageResult<Bitmap> = withContext(io) {
        val resolver = appContext.contentResolver
        val declaredSize = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
            }
        }.getOrNull()
        if (declaredSize != null && declaredSize > MAX_SOURCE_BYTES) return@withContext ImageResult.Failure(ImageError.TOO_LARGE)
        val stream = try {
            resolver.openInputStream(uri)
        } catch (_: SecurityException) {
            null
        } catch (_: IOException) {
            null
        } ?: return@withContext ImageResult.Failure(ImageError.NOT_FOUND)
        val bytes = try {
            stream.use { readLimited(it) }
        } catch (_: IOException) {
            return@withContext ImageResult.Failure(ImageError.CORRUPT)
        } ?: return@withContext ImageResult.Failure(ImageError.TOO_LARGE)
        decode(bytes)
    }

    internal fun decode(bytes: ByteArray): ImageResult<Bitmap> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val mime = bounds.outMimeType
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || mime == null) {
            return ImageResult.Failure(if (looksLikeImage(bytes)) ImageError.CORRUPT else ImageError.UNSUPPORTED)
        }
        if (mime.lowercase() !in SUPPORTED_MIME) return ImageResult.Failure(ImageError.UNSUPPORTED)
        if (minOf(bounds.outWidth, bounds.outHeight) < MIN_SIDE_PX) return ImageResult.Failure(ImageError.TOO_SMALL)

        var sample = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= STORED_SIZE_PX) sample *= 2
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_DECODE_SIDE) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        } catch (_: OutOfMemoryError) {
            null
        } ?: return ImageResult.Failure(ImageError.CORRUPT)
        return ImageResult.Success(applyExifOrientation(decoded, bytes))
    }

    /** Potong persegi [crop] (koordinat piksel [source]), ubah ke 1024 px, kompres JPEG, simpan. */
    suspend fun importCropped(source: Bitmap, crop: Rect): ImageResult<CustomImage> = withContext(io) {
        val side = minOf(crop.width(), crop.height())
        if (side <= 0) return@withContext ImageResult.Failure(ImageError.CORRUPT)
        val target = minOf(STORED_SIZE_PX, side)
        val out = createBitmap(target, target)
        val canvas = Canvas(out)
        canvas.drawColor(android.graphics.Color.WHITE) // PNG/WebP transparan -> latar putih
        canvas.drawBitmap(
            source,
            Rect(crop.left, crop.top, crop.left + side, crop.top + side),
            Rect(0, 0, target, target),
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG),
        )
        val bytes = ByteArrayOutputStream().use { stream ->
            out.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            stream.toByteArray()
        }
        if (dao.totalBytes() + bytes.size > MAX_TOTAL_BYTES) return@withContext ImageResult.Failure(ImageError.STORAGE_FULL)
        val id = UUID.randomUUID().toString()
        val file = File(dir, "$id.jpg")
        try {
            if (!dir.exists() && !dir.mkdirs()) throw IOException("mkdirs gagal")
            file.writeBytes(bytes)
        } catch (_: IOException) {
            file.delete()
            return@withContext ImageResult.Failure(ImageError.WRITE_FAILED)
        }
        val entity = CustomImageEntity(id, file.absolutePath, bytes.size.toLong(), time.nowMs())
        dao.insert(entity)
        boardCache.put(ImageIds.custom(id), out)
        ImageResult.Success(CustomImage(id, entity.path, entity.sizeBytes, entity.createdAt))
    }

    suspend fun delete(id: String) = withContext(io) {
        dao.get(id)?.let { File(it.path).delete() }
        dao.delete(id)
        boardCache.remove(ImageIds.custom(id))
    }

    suspend fun customCount(): Int = dao.count()

    /** Bitmap papan (1024 px) untuk mode gambar. Null bila gambar sudah dihapus atau rusak. */
    suspend fun loadBoardBitmap(imageId: String): Bitmap? = withContext(io) {
        boardCache.get(imageId)?.let { return@withContext it }
        val bmp = ImageIds.builtInKey(imageId)?.let { key ->
            BuiltInImage.byKey(key)?.let { BuiltInRenderer.render(it, STORED_SIZE_PX) }
        } ?: ImageIds.customKey(imageId)?.let { id ->
            dao.get(id)?.let { BitmapFactory.decodeFile(it.path) }
        }
        bmp?.also { boardCache.put(imageId, it) }
    }

    suspend fun builtInThumbnail(image: BuiltInImage, sizePx: Int = THUMB_SIZE_PX): Bitmap = withContext(io) {
        val key = "${image.key}@$sizePx"
        thumbCache.get(key) ?: BuiltInRenderer.render(image, sizePx).also { thumbCache.put(key, it) }
    }

    fun cameraCaptureFile(): File = File(File(appContext.cacheDir, "camera").apply { mkdirs() }, "capture.jpg")

    suspend fun deleteAllFiles() = withContext(io) {
        dir.deleteRecursively()
        boardCache.evictAll()
    }

    private fun readLimited(input: InputStream): ByteArray? {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            total += n
            if (total > MAX_SOURCE_BYTES) return null
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    private fun looksLikeImage(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF
        return (b0 == 0xFF && b1 == 0xD8) || // JPEG
            (b0 == 0x89 && b1 == 0x50) || // PNG
            (b0 == 0x47 && b1 == 0x49) || // GIF
            (b0 == 0x52 && b1 == 0x49) // RIFF (WebP)
    }

    private fun applyExifOrientation(bitmap: Bitmap, bytes: ByteArray): Bitmap {
        val orientation = runCatching {
            ExifInterface(ByteArrayInputStream(bytes))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }

    companion object {
        const val MAX_SOURCE_BYTES = 10L * 1024 * 1024
        const val MAX_TOTAL_BYTES = 50L * 1024 * 1024
        const val WARN_FRACTION = 0.8
        const val STORED_SIZE_PX = 1024
        const val THUMB_SIZE_PX = 256
        const val MIN_SIDE_PX = 64
        private const val MAX_DECODE_SIDE = 4096
        private const val JPEG_QUALITY = 88
        private val SUPPORTED_MIME = setOf("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp")
    }
}
