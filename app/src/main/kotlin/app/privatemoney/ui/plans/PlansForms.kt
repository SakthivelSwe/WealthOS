package app.privatemoney.ui.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.privatemoney.domain.model.Currency
import app.privatemoney.ui.MoneyFormat
import java.time.LocalDate

@Composable
fun AddGoalDialog(
    currency: Currency = Currency.INR,
    onDismiss: () -> Unit,
    onSave: (name: String, targetMinor: Long, currentMinor: Long, monthlyMinor: Long, currency: String, date: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var current by remember { mutableStateOf("0") }
    var monthly by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().plusMonths(6).toString()) }
    val sym = MoneyFormat.symbol(currency)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("New Savings Goal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Set a target and track milestone progress", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name (e.g. First SIP / Emergency Fund)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Target Amount ($sym)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it },
                    label = { Text("Initial Amount ($sym)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = monthly,
                    onValueChange = { monthly = it },
                    label = { Text("Planned Monthly Saving ($sym)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Target Date (YYYY-MM-DD)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val t = (target.toDoubleOrNull() ?: 0.0) * 100
                            val c = (current.toDoubleOrNull() ?: 0.0) * 100
                            val m = (monthly.toDoubleOrNull() ?: 0.0) * 100
                            if (name.isNotBlank() && t > 0) {
                                onSave(name, t.toLong(), c.toLong(), m.toLong(), currency.code, date)
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Create Goal")
                    }
                }
            }
        }
    }
}

@Composable
fun AddDebtDialog(
    currency: Currency = Currency.INR,
    onDismiss: () -> Unit,
    onSave: (name: String, principalMinor: Long, balanceMinor: Long, apr: String, expectedMonthlyMinor: Long, currency: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var principal by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var apr by remember { mutableStateOf("12.0") }
    var monthly by remember { mutableStateOf("") }
    val sym = MoneyFormat.symbol(currency)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Add Debt or Loan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Debt Name") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = principal, onValueChange = { principal = it }, label = { Text("Principal Amount ($sym)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = balance, onValueChange = { balance = it }, label = { Text("Current Balance ($sym)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = apr, onValueChange = { apr = it }, label = { Text("APR %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = monthly, onValueChange = { monthly = it }, label = { Text("Expected Monthly EMI ($sym)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val p = (principal.toDoubleOrNull() ?: 0.0) * 100
                        val b = (balance.toDoubleOrNull() ?: 0.0) * 100
                        val m = (monthly.toDoubleOrNull() ?: 0.0) * 100
                        if (name.isNotBlank() && p > 0) {
                            onSave(name, p.toLong(), b.toLong(), apr, m.toLong(), currency.code)
                            onDismiss()
                        }
                    }, shape = RoundedCornerShape(10.dp)) {
                        Text("Save Debt")
                    }
                }
            }
        }
    }
}

/** Form matching Screenshot 5 for Chit Funds & Cheetu schemes. */
@Composable
fun AddChitFundDialog(
    currency: Currency = Currency.INR,
    onDismiss: () -> Unit,
    onSave: (name: String, months: Int, startMonth: String, monthlyMinor: Long, currency: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var totalMonths by remember { mutableStateOf("20") }
    var monthly by remember { mutableStateOf("5000") }
    var startDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var schemeType by remember { mutableStateOf("Regular Chit (Cash)") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val sym = MoneyFormat.symbol(currency)

    val monthsInt = totalMonths.toIntOrNull() ?: 0
    val monthlyDouble = monthly.toDoubleOrNull() ?: 0.0
    val totalPrizePoolMinor = (monthsInt * monthlyDouble * 100).toLong()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Savings, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(22.dp))
                    Text("Start Chit Fund", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Tamil traditional rotating savings scheme with monthly auctions or gold jewel redemption.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Chit Name (e.g. 1 Lakh Chit)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Chit Company / Organizer (e.g. Shriram / Kapil)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = monthly,
                        onValueChange = { monthly = it },
                        label = { Text("Monthly ($sym)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = totalMonths,
                        onValueChange = { totalMonths = it },
                        label = { Text("Months / Members") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                // Scheme Type Selector (Cash vs Jewel Chit)
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(12.dp))
                            .clickable { dropdownExpanded = true }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (schemeType.contains("Jewel")) "💎 $schemeType" else "💰 $schemeType",
                                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White),
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                        }
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("💰 Regular Chit (Cash)") },
                            onClick = {
                                schemeType = "Regular Chit (Cash)"
                                dropdownExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("💎 Jewel Chit (Gold grams)") },
                            onClick = {
                                schemeType = "Jewel Chit (Gold)"
                                dropdownExpanded = false
                            },
                        )
                    }
                }

                // Calculated Prize Pool Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x22F59E0B))
                        .padding(12.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "ESTIMATED TOTAL PRIZE POOL",
                            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), letterSpacing = 1.sp),
                        )
                        Text(
                            text = MoneyFormat.format(totalPrizePoolMinor, currency),
                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White),
                        )
                        Text(
                            text = "$monthsInt members × ${MoneyFormat.format((monthlyDouble * 100).toLong(), currency)} monthly",
                            style = TextStyle(fontSize = 10.sp, color = Color.Gray),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && monthsInt > 0) {
                                val fullName = buildString {
                                    if (schemeType.contains("Jewel")) append("[Jewel] ")
                                    append(name)
                                    if (company.isNotBlank()) append(" ($company)")
                                }
                                val m = (monthlyDouble * 100).toLong()
                                onSave(fullName, monthsInt, startDate.take(7), m, currency.code)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("START CHIT FUND", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddRecurringTxnDialog(
    currency: Currency = Currency.INR,
    onDismiss: () -> Unit,
    onSave: (type: String, amountMinor: Long, currency: String, accountId: String, categoryId: String?, desc: String?, freq: String, nextRun: String, autoPost: Boolean) -> Unit,
) {
    var type by remember { mutableStateOf("EXPENSE") }
    var amount by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf("default_account") }
    var desc by remember { mutableStateOf("") }
    var freq by remember { mutableStateOf("MONTHLY") }
    var nextRun by remember { mutableStateOf(LocalDate.now().plusMonths(1).toString()) }
    var autoPost by remember { mutableStateOf(false) }
    val sym = MoneyFormat.symbol(currency)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Add Recurring Transaction", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description (e.g. Netflix, Rent)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount ($sym)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = freq, onValueChange = { freq = it }, label = { Text("Frequency (DAILY / WEEKLY / MONTHLY / YEARLY)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nextRun, onValueChange = { nextRun = it }, label = { Text("Next Run Date (YYYY-MM-DD)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-post to Ledger", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (autoPost) "Transactions will be booked automatically. (Local bookkeeping only)" else "Reminder only. WealthOS will send a notification on due date.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = autoPost,
                        onCheckedChange = { autoPost = it },
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val amt = runCatching { app.privatemoney.domain.model.Money.parse(amount, currency).amountMinor }.getOrDefault(0L)
                        if (amt > 0) {
                            onSave(type, amt, currency.code, accountId, null, desc, freq, nextRun, autoPost)
                            onDismiss()
                        }
                    }, shape = RoundedCornerShape(10.dp)) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
