package ch.klarscan

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.File

/** Texterkennung komplett auf dem Gerät (lateinische Schrift: Deutsch, Französisch, Italienisch, Englisch …). */
object Ocr {
    suspend fun recognize(context: Context, pages: List<File>): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val parts = mutableListOf<String>()
            for ((i, f) in pages.withIndex()) {
                val image = InputImage.fromFilePath(context, Uri.fromFile(f))
                val text = recognizer.process(image).await().text.trim()
                parts += if (pages.size > 1) "--- Seite ${i + 1} ---\n$text" else text
            }
            return parts.joinToString("\n\n")
        } finally {
            recognizer.close()
        }
    }
}
