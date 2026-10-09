package app.privatemoney.ui.imports

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import app.privatemoney.domain.imports.FileSniffer
import app.privatemoney.domain.imports.ImportRejectedException
import app.privatemoney.domain.imports.CsvFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ImportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var statusMessage by remember { mutableStateOf("Select a file to import.") }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) {
            statusMessage = "No file selected."
            return@rememberLauncherForActivityResult
        }
        
        statusMessage = "Analyzing file..."
        CoroutineScope(Dispatchers.IO).launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    CsvFormat.requireSize(bytes.size.toLong())
                    
                    // Take first 100 bytes for sniffing
                    val head = bytes.sliceArray(0 until minOf(100, bytes.size))
                    val detected = FileSniffer.sniff(head)
                    
                    // Simulated logic
                    statusMessage = when (detected) {
                        app.privatemoney.domain.imports.DetectedFormat.ZIP_XLSX -> "XLSX detected. Ready to process."
                        app.privatemoney.domain.imports.DetectedFormat.PDF -> {
                            val text = "Extracting text from PDF (simulated for now)..."
                            val result = app.privatemoney.cloud.GeminiAIService().extractTransactions(text)
                            "AI Parsing Complete: $result"
                        }
                        app.privatemoney.domain.imports.DetectedFormat.TEXT -> {
                            val text = String(bytes, Charsets.UTF_8)
                            val rows = CsvFormat.parse(text)
                            "CSV detected. Successfully parsed ${rows.size} rows."
                        }
                        else -> "Unsupported format: $detected"
                    }
                }
            } catch (e: ImportRejectedException) {
                statusMessage = "Import rejected: ${e.reason}"
            } catch (e: Exception) {
                statusMessage = "Error reading file: ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(onClick = onBack) {
            Text("Back")
        }
        
        Text("Import Statement", style = MaterialTheme.typography.headlineMedium)
        Text("Supported formats: CSV, XLSX, PDF", style = MaterialTheme.typography.bodyMedium)
        
        Button(
            onClick = { launcher.launch("*/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select File")
        }
        
        Text(statusMessage, style = MaterialTheme.typography.bodyLarge)
    }
}
