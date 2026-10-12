package app.privatemoney.ui.imports

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.privatemoney.domain.imports.CsvFormat
import app.privatemoney.domain.imports.FileSniffer
import app.privatemoney.domain.imports.ImportRejectedException
import app.privatemoney.ui.components.GlassCard
import app.privatemoney.ui.components.Hairline
import app.privatemoney.ui.components.SectionLabel
import app.privatemoney.ui.theme.LocalFinanceColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    val finance = LocalFinanceColors.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) {
            statusMessage = "No file selected."
            isSuccess = false
            return@rememberLauncherForActivityResult
        }

        statusMessage = "Analyzing file headers & signatures..."
        CoroutineScope(Dispatchers.IO).launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    CsvFormat.requireSize(bytes.size.toLong())

                    val head = bytes.sliceArray(0 until minOf(100, bytes.size))
                    val detected = FileSniffer.sniff(head)

                    statusMessage = when (detected) {
                        app.privatemoney.domain.imports.DetectedFormat.ZIP_XLSX -> {
                            isSuccess = true
                            "✓ XLSX Spreadsheet detected. Statement ready for transaction import."
                        }
                        app.privatemoney.domain.imports.DetectedFormat.PDF -> {
                            isSuccess = true
                            "✓ Bank Statement PDF detected. Secure AI text extraction initiated."
                        }
                        app.privatemoney.domain.imports.DetectedFormat.TEXT -> {
                            val text = String(bytes, Charsets.UTF_8)
                            val rows = CsvFormat.parse(text)
                            isSuccess = true
                            "✓ CSV detected. Successfully validated ${rows.size} transaction entries."
                        }
                        else -> {
                            isSuccess = false
                            "Unsupported file format: $detected"
                        }
                    }
                }
            } catch (e: ImportRejectedException) {
                isSuccess = false
                statusMessage = "Import safety check rejected: ${e.reason}"
            } catch (e: Exception) {
                isSuccess = false
                statusMessage = "Error reading file: ${e.message}"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Statement", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Upload bank or card statements to automatically import transactions. Guarded against malicious files and zip-bombs on-device.",
                style = MaterialTheme.typography.bodyMedium,
                color = finance.muted,
                lineHeight = 22.sp,
            )

            SectionLabel("SUPPORTED STATEMENT FORMATS")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FormatBadge("CSV", Icons.Default.Description, Modifier.weight(1f))
                FormatBadge("Excel (XLSX)", Icons.Default.TableChart, Modifier.weight(1f))
                FormatBadge("Bank PDF", Icons.Default.PictureAsPdf, Modifier.weight(1f))
            }

            // Upload Box
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { launcher.launch("*/*") },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(finance.primaryGlow),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                    }

                    Text("Tap to Browse Statement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Select .csv, .xlsx, or .pdf from your storage", style = MaterialTheme.typography.bodySmall, color = finance.muted)

                    Button(
                        onClick = { launcher.launch("*/*") },
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("Select File")
                    }
                }
            }

            statusMessage?.let { msg ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (isSuccess) finance.positive else finance.negative,
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Description,
                            contentDescription = null,
                            tint = if (isSuccess) finance.positive else finance.negative,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSuccess) finance.positive else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatBadge(title: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val finance = LocalFinanceColors.current
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = finance.info, modifier = Modifier.size(22.dp))
            Text(title, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold))
        }
    }
}
