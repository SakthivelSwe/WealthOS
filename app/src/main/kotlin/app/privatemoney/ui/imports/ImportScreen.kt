package app.privatemoney.ui.imports

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.privatemoney.domain.imports.Likelihood
import app.privatemoney.domain.ledger.TransactionType
import app.privatemoney.domain.model.Currency
import app.privatemoney.ui.MoneyFormat
import app.privatemoney.ui.components.GlassCard
import app.privatemoney.ui.components.Hairline
import app.privatemoney.ui.components.MoneyText
import app.privatemoney.ui.components.MoneyTint
import app.privatemoney.ui.components.SectionLabel
import app.privatemoney.ui.theme.LocalFinanceColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    currency: Currency,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val finance = LocalFinanceColors.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            viewModel.analyzeFile(uri)
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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Target Account Selector
            SectionLabel("DESTINATION ACCOUNT")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.accounts.forEach { acct ->
                    val selected = acct.id == state.selectedAccountId
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.selectAccount(acct.id) },
                        label = { Text(acct.name, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }

            // If an import summary is ready, display completion view
            state.importSummary?.let { summary ->
                GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = finance.positive) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = finance.positive, modifier = Modifier.size(24.dp))
                            Text("Import Completed Successfully", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = finance.positive)
                        }
                        Text("• Valid transactions extracted: ${summary.totalFound}", style = MaterialTheme.typography.bodyMedium)
                        Text("• Safely imported to ledger: ${summary.importedCount}", style = MaterialTheme.typography.bodyMedium, color = finance.positive, fontWeight = FontWeight.SemiBold)
                        if (summary.skippedDuplicates > 0) {
                            Text("• Skipped duplicate transactions: ${summary.skippedDuplicates}", style = MaterialTheme.typography.bodyMedium, color = finance.muted)
                        }

                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text("View Transactions in Ledger", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                return@Column
            }

            // If no preview rows, display file upload picker
            if (state.previewRows.isEmpty()) {
                SectionLabel("SUPPORTED STATEMENT FORMATS")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FormatBadge("CSV", Icons.Default.Description, Modifier.weight(1f))
                    FormatBadge("Excel (XLSX)", Icons.Default.TableChart, Modifier.weight(1f))
                    FormatBadge("Bank PDF", Icons.Default.PictureAsPdf, Modifier.weight(1f))
                }

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { launcher.launch("*/*") },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
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

                        Text("Tap to Select Statement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Select .csv or bank statement from your device", style = MaterialTheme.typography.bodySmall, color = finance.muted)

                        if (state.isAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            Text("Parsing and scoring against ledger...", style = MaterialTheme.typography.bodySmall, color = finance.muted)
                        } else {
                            Button(
                                onClick = { launcher.launch("*/*") },
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("Choose File")
                            }
                        }
                    }
                }

                state.statusMessage?.let { msg ->
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = if (state.isSuccess) finance.positive else MaterialTheme.colorScheme.error)
                }
            } else {
                // Preview & Review Step
                val selectedCount = state.previewRows.count { it.isSelected }
                val duplicateCount = state.previewRows.count { it.duplicateLikelihood == Likelihood.LIKELY }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "$selectedCount of ${state.previewRows.size} Selected",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        if (duplicateCount > 0) {
                            Text(
                                "⚠️ $duplicateCount suspected duplicates auto-unselected",
                                style = TextStyle(fontSize = 11.sp, color = Color(0xFFFBBF24), fontWeight = FontWeight.SemiBold),
                            )
                        }
                    }

                    Row {
                        OutlinedButton(
                            onClick = { viewModel.selectAll(selectedCount < state.previewRows.size) },
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(if (selectedCount < state.previewRows.size) "Select All" else "Deselect", fontSize = 12.sp)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.previewRows, key = { it.row.id }) { item ->
                        val row = item.row
                        val isDuplicate = item.duplicateLikelihood == Likelihood.LIKELY
                        val isPossible = item.duplicateLikelihood == Likelihood.POSSIBLE

                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = if (isDuplicate) Color(0xFFFBBF24).copy(alpha = 0.6f) else finance.glassBorder,
                            onClick = { viewModel.toggleRowSelection(row.id) },
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = item.isSelected,
                                    onCheckedChange = { viewModel.toggleRowSelection(row.id) },
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        row.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(row.date.toString(), style = TextStyle(fontSize = 11.sp, color = finance.muted))
                                        if (isDuplicate) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0x33FBBF24))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                            ) {
                                                Text("Likely Duplicate", style = TextStyle(fontSize = 9.sp, color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold))
                                            }
                                        } else if (isPossible) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0x2238BDF8))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                            ) {
                                                Text("Possible Match", style = TextStyle(fontSize = 9.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold))
                                            }
                                        }
                                    }
                                }

                                MoneyText(
                                    minor = row.amountMinor,
                                    currency = currency,
                                    tint = if (row.type == TransactionType.INCOME) MoneyTint.POSITIVE else MoneyTint.NEGATIVE,
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { viewModel.executeImport() },
                    enabled = selectedCount > 0 && !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Importing Transactions...")
                    } else {
                        Text("Confirm Import ($selectedCount Transactions)", fontWeight = FontWeight.Bold)
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
