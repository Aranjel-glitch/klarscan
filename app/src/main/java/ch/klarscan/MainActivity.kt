@file:OptIn(ExperimentalMaterial3Api::class)

package ch.klarscan

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class MainActivity : FragmentActivity() {
    private lateinit var lock: AppLock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lock = AppLock(this)
        enableEdgeToEdge()
        setContent {
            KlarscanTheme {
                Box(Modifier.fillMaxSize()) {
                    // Die App bleibt im Hintergrund erhalten (offenes Dokument, laufender Scan),
                    // ist aber unsichtbar für Bildschirmleser und nicht bedienbar, solange gesperrt.
                    Box(if (lock.locked) Modifier.fillMaxSize().clearAndSetSemantics {} else Modifier.fillMaxSize()) {
                        App(this@MainActivity, lock)
                    }
                    if (lock.locked) LockScreen(lock)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lock.onStart()
    }

    override fun onStop() {
        super.onStop()
        lock.onStop()
    }
}

@Composable
private fun LockScreen(lock: AppLock) {
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { lock.unlock { error = it } }
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).clickable(enabled = true) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Klarscan ist gesperrt", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text("Deine Dokumente sind geschützt.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = { error = null; lock.unlock { error = it } }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Entsperren")
            }
        }
    }
}

@Composable
private fun LockSettingsDialog(lock: AppLock, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App-Sperre") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Mit Fingerabdruck schützen", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Switch(checked = lock.enabled, onCheckedChange = { on -> lock.setEnabled(on) { toast(ctx, it) } })
                }
                Text(
                    "Klarscan fragt beim Öffnen nach deinem Fingerabdruck. Klappt das nicht, geht auch die PIN deines Handys. " +
                        "Solange die Sperre an ist, zeigt die App-Übersicht keine Vorschau und Bildschirmfotos sind blockiert.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fertig") } },
    )
}

private fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

@Composable
fun App(activity: Activity, lock: AppLock) {
    val ctx = LocalContext.current
    val store = remember { DocumentStore(ctx.applicationContext) }
    val scope = rememberCoroutineScope()
    var docs by remember { mutableStateOf(store.list()) }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var addToId by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf<String?>(null) }

    fun refresh() { docs = store.list() }

    val scanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { res ->
        val target = addToId
        addToId = null
        if (res.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val result = GmsDocumentScanningResult.fromActivityResultIntent(res.data)
        val uris = result?.pages?.map { it.imageUri }.orEmpty()
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            busy = "Wird gespeichert"
            try {
                val id = withContext(Dispatchers.IO) {
                    val existing = target?.let { store.get(it) }
                    if (existing != null) store.addPages(existing, uris).id else store.create(uris).id
                }
                refresh()
                openId = id
            } catch (e: Exception) {
                toast(ctx, "Speichern hat nicht geklappt")
            } finally {
                busy = null
            }
        }
    }

    val scanner = remember {
        GmsDocumentScanning.getClient(
            GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setPageLimit(100)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build()
        )
    }

    fun startScan(intoDoc: String?) {
        addToId = intoDoc
        lock.expectExternal = true
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { scanLauncher.launch(IntentSenderRequest.Builder(it).build()) }
            .addOnFailureListener {
                lock.expectExternal = false
                toast(ctx, "Scanner nicht verfügbar. Bitte Google Play-Dienste aktualisieren.")
            }
    }

    val current = openId?.let { id -> docs.firstOrNull { it.id == id } }
    BackHandler(enabled = current != null) { openId = null }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (current == null) {
            LibraryScreen(docs, lock, onOpen = { openId = it }, onScan = { startScan(null) })
        } else {
            DocScreen(
                doc = current,
                store = store,
                onBack = { openId = null },
                onChanged = { refresh() },
                onAddPages = { startScan(current.id) },
                setBusy = { busy = it },
                beforeExternal = { lock.expectExternal = true },
            )
        }
        busy?.let { BusyOverlay(it) }
    }
}

@Composable
private fun BusyOverlay(text: String) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)).clickable(enabled = true) {},
        contentAlignment = Alignment.Center,
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                Spacer(Modifier.width(14.dp))
                Text(text, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private val dateFmt = SimpleDateFormat("d. MMM yyyy, HH:mm", Locale.GERMAN)

@Composable
private fun LibraryScreen(docs: List<Doc>, lock: AppLock, onOpen: (String) -> Unit, onScan: () -> Unit) {
    var q by rememberSaveable { mutableStateOf("") }
    var lockDialog by remember { mutableStateOf(false) }
    if (lockDialog) LockSettingsDialog(lock) { lockDialog = false }
    val shown = docs.filter { q.isBlank() || it.name.contains(q.trim(), ignoreCase = true) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onScan,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Scannen", fontWeight = FontWeight.Bold) },
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(horizontal = 16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.size(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Klarscan",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { lockDialog = true }) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "App-Sperre",
                        tint = if (lock.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                "Ohne Abo · Ohne Konto · Ohne Internet",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = q,
                onValueChange = { q = it },
                placeholder = { Text("Dokumente durchsuchen") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (shown.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth()
                        .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.surfaceVariant), RoundedCornerShape(14.dp))
                        .padding(28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (docs.isEmpty()) "Noch keine Dokumente.\nTippe auf Scannen und halte die Kamera über ein Blatt."
                        else "Kein Dokument passt zu dieser Suche.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    items(shown, key = { it.id }) { d -> DocCard(d) { onOpen(d.id) } }
                }
            }
        }
    }
}

@Composable
private fun DocCard(doc: Doc, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AsyncImage(
                model = doc.pages.firstOrNull(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().aspectRatio(210f / 297f)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(6.dp)),
            )
            Text(doc.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${doc.pages.size} ${if (doc.pages.size == 1) "Seite" else "Seiten"} · ${dateFmt.format(Date(doc.updated))}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DocScreen(
    doc: Doc,
    store: DocumentStore,
    onBack: () -> Unit,
    onChanged: () -> Unit,
    onAddPages: () -> Unit,
    setBusy: (String?) -> Unit,
    beforeExternal: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var sel by remember(doc.id) { mutableIntStateOf(0) }
    val idx = sel.coerceIn(0, max(0, doc.pages.lastIndex))
    var renaming by remember { mutableStateOf(false) }
    var confirmDoc by remember { mutableStateOf(false) }
    var confirmPage by remember { mutableStateOf(false) }
    var ocrText by remember { mutableStateOf<String?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            setBusy("PDF wird gespeichert")
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val pdf = store.ensurePdf(doc)
                    ctx.contentResolver.openOutputStream(uri)?.use { out -> pdf.inputStream().use { it.copyTo(out) } }
                }.isSuccess
            }
            setBusy(null)
            toast(ctx, if (ok) "PDF gespeichert" else "Speichern hat nicht geklappt")
        }
    }

    fun sharePdf() = scope.launch {
        setBusy("PDF wird erstellt")
        val file = withContext(Dispatchers.IO) { runCatching { store.ensurePdf(doc) }.getOrNull() }
        setBusy(null)
        if (file == null) { toast(ctx, "PDF konnte nicht erstellt werden"); return@launch }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        beforeExternal()
        ctx.startActivity(Intent.createChooser(send, doc.name))
    }

    fun recognize() = scope.launch {
        setBusy("Text wird erkannt")
        val text = runCatching { Ocr.recognize(ctx, doc.pages) }.getOrNull()
        setBusy(null)
        if (text == null) toast(ctx, "Texterkennung hat nicht geklappt")
        else ocrText = text.ifBlank { "Auf diesen Seiten wurde kein Text gefunden." }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(doc.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { renaming = true }) { Icon(Icons.Filled.Edit, contentDescription = "Umbenennen") }
                    IconButton(onClick = { confirmDoc = true }) { Icon(Icons.Filled.Delete, contentDescription = "Dokument löschen") }
                },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            AsyncImage(
                model = doc.pages.getOrNull(idx),
                contentDescription = "Seite ${idx + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Text(
                "Seite ${idx + 1} von ${doc.pages.size}",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(doc.pages, key = { _, f -> f.name }) { i, f ->
                    AsyncImage(
                        model = f,
                        contentDescription = "Seite ${i + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.width(64.dp).aspectRatio(210f / 297f)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(6.dp))
                            .border(
                                2.dp,
                                if (i == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(6.dp),
                            )
                            .clickable { sel = i },
                    )
                }
                item {
                    Box(
                        Modifier.width(64.dp).aspectRatio(210f / 297f)
                            .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                            .clickable(onClick = onAddPages),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Add, contentDescription = "Seiten hinzufügen") }
                }
            }
            Column(
                Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { sharePdf() }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("PDF teilen") }
                    OutlinedButton(
                        onClick = { beforeExternal(); saveLauncher.launch(DocumentStore.safeFileName(doc.name) + ".pdf") },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) { Text("PDF speichern") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { recognize() }, modifier = Modifier.weight(1f)) { Text("Text erkennen") }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) { store.movePage(doc, idx, idx - 1) }
                                sel = idx - 1; onChanged()
                            }
                        },
                        enabled = idx > 0,
                        modifier = Modifier.weight(1f),
                    ) { Text("Nach vorn") }
                    TextButton(onClick = { confirmPage = true }) {
                        Text("Seite löschen", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (renaming) {
        var name by remember { mutableStateOf(doc.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Dokument umbenennen") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    renaming = false
                    scope.launch { withContext(Dispatchers.IO) { store.rename(doc, name) }; onChanged() }
                }) { Text("Speichern") }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Abbrechen") } },
        )
    }

    if (confirmPage) {
        AlertDialog(
            onDismissRequest = { confirmPage = false },
            title = { Text("Seite ${idx + 1} löschen?") },
            text = {
                Text(if (doc.pages.size == 1) "Es ist die einzige Seite. Das ganze Dokument wird gelöscht." else "Das lässt sich nicht rückgängig machen.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmPage = false
                    scope.launch {
                        withContext(Dispatchers.IO) { store.deletePage(doc, idx) }
                        sel = max(0, idx - 1); onChanged()
                    }
                }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmPage = false }) { Text("Abbrechen") } },
        )
    }

    if (confirmDoc) {
        AlertDialog(
            onDismissRequest = { confirmDoc = false },
            title = { Text("Dokument löschen?") },
            text = { Text("„${doc.name}“ mit ${doc.pages.size} Seiten wird von diesem Gerät gelöscht.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDoc = false
                    scope.launch {
                        withContext(Dispatchers.IO) { store.delete(doc) }
                        onChanged(); onBack()
                    }
                }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDoc = false }) { Text("Abbrechen") } },
        )
    }

    ocrText?.let { text ->
        AlertDialog(
            onDismissRequest = { ocrText = null },
            title = { Text("Erkannter Text") },
            text = {
                SelectionContainer {
                    Text(
                        text,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(text)); toast(ctx, "Text kopiert")
                }) { Text("Kopieren") }
            },
            dismissButton = { TextButton(onClick = { ocrText = null }) { Text("Schliessen") } },
        )
    }
}
