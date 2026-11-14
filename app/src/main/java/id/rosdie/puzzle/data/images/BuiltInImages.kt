package id.rosdie.puzzle.data.images

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.annotation.StringRes
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withRotation
import id.rosdie.puzzle.R
import id.rosdie.puzzle.domain.engine.SeededRandom
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Gambar bawaan yang digambar sendiri lewat kode (Canvas), bebas hak cipta dan tidak
 * menambah ukuran APK. Semua digambar di ruang koordinat 1000x1000 lalu diskalakan.
 */
enum class BuiltInImage(val key: String, @param:StringRes val titleRes: Int) {
    SUNSET("sunset", R.string.builtin_sunset),
    OCEAN("ocean", R.string.builtin_ocean),
    MOSAIC("mosaic", R.string.builtin_mosaic),
    NIGHT("night", R.string.builtin_night),
    BLOOM("bloom", R.string.builtin_bloom);

    val imageId: String get() = ImageIds.builtIn(key)

    companion object {
        fun byKey(key: String): BuiltInImage? = entries.firstOrNull { it.key == key }
    }
}

object ImageIds {
    private const val BUILTIN = "builtin:"
    private const val CUSTOM = "custom:"

    fun builtIn(key: String) = BUILTIN + key
    fun custom(id: String) = CUSTOM + id
    fun builtInKey(imageId: String): String? = imageId.removePrefix(BUILTIN).takeIf { imageId.startsWith(BUILTIN) }
    fun customKey(imageId: String): String? = imageId.removePrefix(CUSTOM).takeIf { imageId.startsWith(CUSTOM) }
}

object BuiltInRenderer {
    fun render(image: BuiltInImage, sizePx: Int): Bitmap {
        val bmp = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bmp)
        canvas.scale(sizePx / 1000f, sizePx / 1000f)
        when (image) {
            BuiltInImage.SUNSET -> sunset(canvas)
            BuiltInImage.OCEAN -> ocean(canvas)
            BuiltInImage.MOSAIC -> mosaic(canvas)
            BuiltInImage.NIGHT -> night(canvas)
            BuiltInImage.BLOOM -> bloom(canvas)
        }
        return bmp
    }

    private fun paint() = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun vertical(c: Canvas, top: Float, bottom: Float, colors: IntArray, stops: FloatArray? = null) {
        val p = paint()
        p.shader = LinearGradient(0f, top, 0f, bottom, colors, stops, Shader.TileMode.CLAMP)
        c.drawRect(0f, top, 1000f, bottom, p)
    }

    private fun ridge(c: Canvas, seed: Long, base: Float, amp: Float, color: Int, bottom: Float = 1000f) {
        val rnd = SeededRandom(seed)
        val path = Path()
        path.moveTo(0f, bottom)
        var x = 0f
        var y = base - rnd.nextInt(amp.toInt())
        path.lineTo(0f, y)
        while (x < 1000f) {
            x += 60f + rnd.nextInt(90)
            y = base - rnd.nextInt(amp.toInt())
            path.lineTo(x, y)
        }
        path.lineTo(1000f, bottom)
        path.close()
        c.drawPath(path, paint().apply { this.color = color })
    }

    private fun sunset(c: Canvas) {
        vertical(
            c, 0f, 640f,
            intArrayOf(0xFF1E3A8A.toInt(), 0xFF7C3AED.toInt(), 0xFFF97316.toInt(), 0xFFFDE68A.toInt()),
            floatArrayOf(0f, 0.45f, 0.82f, 1f),
        )
        val glow = paint()
        glow.shader = RadialGradient(500f, 560f, 330f, 0x88FFF1C2.toInt(), 0x00FFF1C2, Shader.TileMode.CLAMP)
        c.drawCircle(500f, 560f, 330f, glow)
        val sun = paint()
        sun.shader = RadialGradient(500f, 560f, 120f, 0xFFFFFBEB.toInt(), 0xFFFDBA74.toInt(), Shader.TileMode.CLAMP)
        c.drawCircle(500f, 560f, 120f, sun)
        ridge(c, 11, 560f, 180f, 0xFF8B5CF6.toInt())
        ridge(c, 23, 620f, 160f, 0xFF5B21B6.toInt())
        ridge(c, 37, 690f, 120f, 0xFF2E1065.toInt())
        vertical(c, 720f, 1000f, intArrayOf(0xFF3730A3.toInt(), 0xFF0F172A.toInt()))
        val streak = paint().apply { color = 0x66FDE68A }
        val rnd = SeededRandom(5)
        for (i in 0 until 14) {
            val y = 740f + i * 18f
            val w = 260f - i * 14f + rnd.nextInt(40)
            c.drawRoundRect(RectF(500f - w / 2, y, 500f + w / 2, y + 6f), 3f, 3f, streak)
        }
    }

    private fun ocean(c: Canvas) {
        vertical(c, 0f, 520f, intArrayOf(0xFF7DD3FC.toInt(), 0xFFE0F2FE.toInt()))
        c.drawCircle(760f, 180f, 90f, paint().apply { color = 0xFFFFF7AE.toInt() })
        c.drawCircle(760f, 180f, 130f, paint().apply { color = 0x33FFF7AE })
        val cloud = paint().apply { color = 0xEEFFFFFF.toInt() }
        for ((cx, cy) in listOf(220f to 160f, 420f to 260f)) {
            c.drawCircle(cx, cy, 50f, cloud)
            c.drawCircle(cx + 55f, cy - 20f, 60f, cloud)
            c.drawCircle(cx + 115f, cy, 45f, cloud)
            c.drawRoundRect(RectF(cx - 40f, cy, cx + 150f, cy + 45f), 22f, 22f, cloud)
        }
        val blues = intArrayOf(
            0xFF38BDF8.toInt(), 0xFF0EA5E9.toInt(), 0xFF0284C7.toInt(),
            0xFF0369A1.toInt(), 0xFF075985.toInt(), 0xFF0C4A6E.toInt(),
        )
        for ((i, col) in blues.withIndex()) {
            val baseY = 500f + i * 85f
            val path = Path()
            path.moveTo(0f, 1000f)
            var x = 0f
            while (x <= 1000f) {
                val y = baseY + 18f * sin((x / 1000f) * 2f * PI.toFloat() * (2 + i % 3) + i)
                if (x == 0f) path.lineTo(0f, y) else path.lineTo(x, y)
                x += 10f
            }
            path.lineTo(1000f, 1000f)
            path.close()
            c.drawPath(path, paint().apply { color = col })
        }
        // Perahu layar
        val hull = Path().apply {
            moveTo(300f, 590f); lineTo(470f, 590f); lineTo(440f, 625f); lineTo(330f, 625f); close()
        }
        c.drawPath(hull, paint().apply { color = 0xFF7C2D12.toInt() })
        val sail = Path().apply { moveTo(385f, 440f); lineTo(385f, 580f); lineTo(470f, 580f); close() }
        c.drawPath(sail, paint().apply { color = 0xFFFFFFFF.toInt() })
        val sail2 = Path().apply { moveTo(378f, 470f); lineTo(378f, 580f); lineTo(315f, 580f); close() }
        c.drawPath(sail2, paint().apply { color = 0xFFFDE68A.toInt() })
        val bird = paint().apply {
            color = 0xFF1E293B.toInt(); style = Paint.Style.STROKE; strokeWidth = 6f; strokeCap = Paint.Cap.ROUND
        }
        for ((bx, by) in listOf(560f to 330f, 610f to 300f, 650f to 350f)) {
            val p = Path().apply {
                moveTo(bx - 22f, by); quadTo(bx - 10f, by - 14f, bx, by); quadTo(bx + 10f, by - 14f, bx + 22f, by)
            }
            c.drawPath(p, bird)
        }
    }

    private fun mosaic(c: Canvas) {
        val cells = 8
        val s = 1000f / cells
        val hsv = FloatArray(3)
        val rnd = SeededRandom(99)
        for (r in 0 until cells) for (col in 0 until cells) {
            val x = col * s
            val y = r * s
            val hueBase = 200f + (r + col) * 11f
            for (tri in 0 until 2) {
                hsv[0] = (hueBase + tri * 18f + rnd.nextInt(14)) % 360f
                hsv[1] = 0.55f + rnd.nextInt(30) / 100f
                hsv[2] = 0.65f + rnd.nextInt(30) / 100f
                val p = Path()
                if ((r + col) % 2 == 0) {
                    if (tri == 0) { p.moveTo(x, y); p.lineTo(x + s, y); p.lineTo(x, y + s) }
                    else { p.moveTo(x + s, y); p.lineTo(x + s, y + s); p.lineTo(x, y + s) }
                } else {
                    if (tri == 0) { p.moveTo(x, y); p.lineTo(x + s, y); p.lineTo(x + s, y + s) }
                    else { p.moveTo(x, y); p.lineTo(x + s, y + s); p.lineTo(x, y + s) }
                }
                p.close()
                c.drawPath(p, paint().apply { color = Color.HSVToColor(hsv) })
            }
        }
        val ring = paint().apply { style = Paint.Style.STROKE; strokeWidth = 34f; color = 0xCCFFFFFF.toInt() }
        c.drawCircle(500f, 500f, 300f, ring)
        ring.strokeWidth = 14f
        ring.color = 0x99FFFFFF.toInt()
        c.drawCircle(500f, 500f, 190f, ring)
        val star = Path()
        for (i in 0 until 10) {
            val rad = if (i % 2 == 0) 120f else 50f
            val a = -PI / 2 + i * PI / 5
            val px = 500f + rad * cos(a).toFloat()
            val py = 500f + rad * sin(a).toFloat()
            if (i == 0) star.moveTo(px, py) else star.lineTo(px, py)
        }
        star.close()
        c.drawPath(star, paint().apply { color = 0xFFFFFFFF.toInt() })
    }

    private fun night(c: Canvas) {
        vertical(c, 0f, 1000f, intArrayOf(0xFF020617.toInt(), 0xFF1E1B4B.toInt(), 0xFF1D4ED8.toInt()), floatArrayOf(0f, 0.55f, 1f))
        val rnd = SeededRandom(2026)
        val star = paint()
        repeat(140) {
            val x = rnd.nextInt(1000).toFloat()
            val y = rnd.nextInt(650).toFloat()
            star.color = Color.argb(120 + rnd.nextInt(135), 255, 255, 230)
            c.drawCircle(x, y, 1.5f + rnd.nextInt(30) / 10f, star)
        }
        val moonGlow = paint()
        moonGlow.shader = RadialGradient(260f, 230f, 200f, 0x55FEF3C7, 0x00FEF3C7, Shader.TileMode.CLAMP)
        c.drawCircle(260f, 230f, 200f, moonGlow)
        c.drawCircle(260f, 230f, 95f, paint().apply { color = 0xFFFEF3C7.toInt() })
        c.drawCircle(305f, 200f, 85f, paint().apply { color = 0xFF0F1235.toInt() })
        // Siluet kota dengan jendela menyala
        var x = 0f
        val building = paint().apply { color = 0xFF0B1022.toInt() }
        val window = paint().apply { color = 0xFFFACC15.toInt() }
        while (x < 1000f) {
            val w = 70f + rnd.nextInt(70)
            val h = 180f + rnd.nextInt(260)
            c.drawRect(x, 1000f - h, x + w - 6f, 1000f, building)
            var wy = 1000f - h + 22f
            while (wy < 980f) {
                var wx = x + 12f
                while (wx < x + w - 24f) {
                    if (rnd.nextInt(10) < 4) c.drawRect(wx, wy, wx + 12f, wy + 16f, window)
                    wx += 24f
                }
                wy += 34f
            }
            x += w
        }
    }

    private fun bloom(c: Canvas) {
        vertical(c, 0f, 1000f, intArrayOf(0xFFD9F99D.toInt(), 0xFF4ADE80.toInt(), 0xFF166534.toInt()), floatArrayOf(0f, 0.5f, 1f))
        val rnd = SeededRandom(7)
        val petalColors = intArrayOf(
            0xFFF43F5E.toInt(), 0xFFF59E0B.toInt(), 0xFFA855F7.toInt(),
            0xFF3B82F6.toInt(), 0xFFEC4899.toInt(), 0xFFFFFFFF.toInt(),
        )
        val stem = paint().apply { color = 0xFF14532D.toInt(); strokeWidth = 10f; strokeCap = Paint.Cap.ROUND }
        val positions = listOf(
            150f to 220f, 420f to 160f, 760f to 230f, 260f to 480f, 600f to 430f,
            880f to 520f, 120f to 760f, 430f to 720f, 720f to 760f, 560f to 920f, 900f to 880f, 260f to 930f,
        )
        for ((i, pos) in positions.withIndex()) {
            val (cx, cy) = pos
            val radius = 70f + rnd.nextInt(45)
            c.drawLine(cx, cy, cx + rnd.nextInt(40) - 20f, cy + radius * 2.2f, stem)
            val petal = paint().apply { color = petalColors[i % petalColors.size] }
            val petals = 6 + i % 3
            for (k in 0 until petals) {
                c.withRotation(k * 360f / petals + i * 13f, cx, cy) {
                    drawOval(RectF(cx - radius * 0.32f, cy - radius, cx + radius * 0.32f, cy), petal)
                }
            }
            c.drawCircle(cx, cy, radius * 0.3f, paint().apply { color = 0xFFFDE047.toInt() })
            c.drawCircle(cx, cy, radius * 0.14f, paint().apply { color = 0xFFB45309.toInt() })
        }
    }
}
