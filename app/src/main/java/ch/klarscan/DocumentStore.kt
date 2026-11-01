package ch.klarscan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/** Ein gescanntes Dokument. Seiten sind JPEG-Dateien im Ordner des Dokuments. */
data class Doc(
    val id: String,
    val name: String,
    val created: Long,
    val updated: Long,
    val dir: File,
    val pages: List<File>,
)

/**
 * Speichert alles im privaten App-Speicher (filesDir/docs/<id>/).
 * Kein Konto, keine Cloud, kein Netzwerk.
 */
class DocumentStore(private val context: Context) {

    private val root = File(context.filesDir, "docs").apply { mkdirs() }

    fun list(): List<Doc> =
        root.listFiles()?.mapNotNull { load(it) }?.sortedByDescending { it.updated } ?: emptyList()

    fun get(id: String): Doc? = load(File(root, id))

    private fun load(dir: File): Doc? = runCatching {
        val j = JSONObject(File(dir, META).readText())
        val arr = j.getJSONArray("pages")
        val pages = (0 until arr.length()).map { File(dir, arr.getString(it)) }.filter { it.exists() }
        Doc(dir.name, j.getString("name"), j.getLong("created"), j.getLong("updated"), dir, pages)
    }.getOrNull()

    private fun write(doc: Doc): Doc {
        val d = doc.copy(updated = System.currentTimeMillis())
        val j = JSONObject()
            .put("name", d.name)
            .put("created", d.created)
            .put("updated", d.updated)
            .put("pages", JSONArray(d.pages.map { it.name }))
        File(d.dir, META).writeText(j.toString())
        // Das PDF wird beim nächsten Teilen/Speichern neu erzeugt
        d.dir.listFiles { f -> f.extension == "pdf" }?.forEach { it.delete() }
        return d
    }

    private fun copyIn(dir: File, uri: Uri): File {
        val f = File(dir, "p_${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            f.outputStream().use { input.copyTo(it) }
        } ?: error("Seite konnte nicht gelesen werden")
        return f
    }

    fun create(uris: List<Uri>): Doc {
        val id = UUID.randomUUID().toString()
        val dir = File(root, id).apply { mkdirs() }
        val pages = uris.map { copyIn(dir, it) }
        val now = System.currentTimeMillis()
        val name = "Scan " + SimpleDateFormat("d. MMM yyyy, HH:mm", Locale.GERMAN).format(Date(now))
        return write(Doc(id, name, now, now, dir, pages))
    }

    fun addPages(doc: Doc, uris: List<Uri>): Doc =
        write(doc.copy(pages = doc.pages + uris.map { copyIn(doc.dir, it) }))

    fun rename(doc: Doc, name: String): Doc =
        write(doc.copy(name = name.trim().ifEmpty { doc.name }))

    fun movePage(doc: Doc, from: Int, to: Int): Doc {
        if (from !in doc.pages.indices || to !in doc.pages.indices) return doc
        val p = doc.pages.toMutableList()
        p.add(to, p.removeAt(from))
        return write(doc.copy(pages = p))
    }

    /** Gibt null zurück, wenn die letzte Seite gelöscht wurde (dann ist das Dokument weg). */
    fun deletePage(doc: Doc, index: Int): Doc? {
        if (index !in doc.pages.indices) return doc
        doc.pages[index].delete()
        val rest = doc.pages.filterIndexed { i, _ -> i != index }
        if (rest.isEmpty()) {
            delete(doc); return null
        }
        return write(doc.copy(pages = rest))
    }

    fun delete(doc: Doc) {
        doc.dir.deleteRecursively()
    }

    /** Liefert das PDF des Dokuments, erzeugt es bei Bedarf (A4, Bilder eingepasst). */
    fun ensurePdf(doc: Doc): File {
        val out = File(doc.dir, safeFileName(doc.name) + ".pdf")
        if (out.exists()) return out // jede Änderung löscht das alte PDF (siehe write)
        doc.dir.listFiles { f -> f.extension == "pdf" }?.forEach { it.delete() }
        val pdf = PdfDocument()
        try {
            doc.pages.forEachIndexed { i, f ->
                val bmp = decodeSampled(f, 1800) ?: return@forEachIndexed
                val landscape = bmp.width > bmp.height
                val w = if (landscape) A4_LONG else A4_SHORT
                val h = if (landscape) A4_SHORT else A4_LONG
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(w, h, i + 1).create())
                val s = min(w.toFloat() / bmp.width, h.toFloat() / bmp.height)
                val dw = bmp.width * s
                val dh = bmp.height * s
                val dst = RectF((w - dw) / 2f, (h - dh) / 2f, (w + dw) / 2f, (h + dh) / 2f)
                page.canvas.drawBitmap(bmp, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
                pdf.finishPage(page)
                bmp.recycle()
            }
            out.outputStream().use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
        return out
    }

    companion object {
        private const val META = "meta.json"
        private const val A4_SHORT = 595 // 210 mm in PDF-Punkten
        private const val A4_LONG = 842  // 297 mm

        fun safeFileName(name: String): String =
            name.replace(Regex("[\\\\/:*?\"<>|]+"), "-").replace(Regex("\\s+"), " ").trim().take(80).ifEmpty { "Scan" }

        fun decodeSampled(file: File, maxDim: Int): Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            if (bounds.outWidth <= 0) return null
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) sample *= 2
            return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }
}
