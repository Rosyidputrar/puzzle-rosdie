package id.rosdie.puzzle.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import id.rosdie.puzzle.domain.model.PuzzleState
import java.io.File
import java.io.FileOutputStream

/** Simpan tangkapan layar Robolectric (native graphics) ke app/build/screenshots untuk ditinjau. */
fun SemanticsNodeInteractionsProvider.screenshot(name: String) {
    val bmp = onRoot().captureToImage().asAndroidBitmap()
    val dir = File("build/screenshots").apply { mkdirs() }
    FileOutputStream(File(dir, "$name.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
}

private val TILE = Regex("""\D*(\d+)\D+(\d+)\D+(\d+)""")

/**
 * Bangun ulang papan dari content description tile ("Tile 5, baris 2 kolom 3"), persis seperti
 * yang dibacakan TalkBack.
 */
fun SemanticsNodeInteractionsProvider.readBoard(size: Int, tilePrefix: String): PuzzleState {
    val cells = IntArray(size * size)
    val nodes = onAllNodes(androidx.compose.ui.test.SemanticsMatcher("tile") { node ->
        node.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()?.startsWith(tilePrefix) == true
    }).fetchSemanticsNodes()
    for (n in nodes) {
        val desc = n.config[SemanticsProperties.ContentDescription].first()
        val m = TILE.find(desc) ?: continue
        val (v, r, c) = m.destructured
        cells[(r.toInt() - 1) * size + (c.toInt() - 1)] = v.toInt()
    }
    return PuzzleState.of(size, cells)
}
