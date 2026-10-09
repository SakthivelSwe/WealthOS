package app.privatemoney.ui.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, targetMinor: Long, currentMinor: Long, monthlyMinor: Long, currency: String, date: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var current by remember { mutableStateOf("") }
    var monthly by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") } // simplified, real app might use DatePicker

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Add Goal", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Goal Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Target Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = current, onValueChange = { current = it }, label = { Text("Current Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = monthly, onValueChange = { monthly = it }, label = { Text("Planned Monthly") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Target Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = {
                        val t = (target.toDoubleOrNull() ?: 0.0) * 100
                        val c = (current.toDoubleOrNull() ?: 0.0) * 100
                        val m = (monthly.toDoubleOrNull() ?: 0.0) * 100
                        if (name.isNotBlank() && t > 0) {
                            onSave(name, t.toLong(), c.toLong(), m.toLong(), "USD", date) // Hardcoding USD for demo
                            onDismiss()
                        }
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun AddDebtDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, principalMinor: Long, balanceMinor: Long, apr: String, expectedMonthlyMinor: Long, currency: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var principal by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var apr by remember { mutableStateOf("") }
    var monthly by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Add Debt", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Debt Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = principal, onValueChange = { principal = it }, label = { Text("Principal Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = balance, onValueChange = { balance = it }, label = { Text("Current Balance") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = apr, onValueChange = { apr = it }, label = { Text("APR %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = monthly, onValueChange = { monthly = it }, label = { Text("Expected Monthly") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = {
                        val p = (principal.toDoubleOrNull() ?: 0.0) * 100
                        val b = (balance.toDoubleOrNull() ?: 0.0) * 100
                        val m = (monthly.toDoubleOrNull() ?: 0.0) * 100
                        if (name.isNotBlank() && p > 0) {
                            onSave(name, p.toLong(), b.toLong(), apr, m.toLong(), "USD")
                            onDismiss()
                        }
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun AddChitFundDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, months: Int, startMonth: String, monthlyMinor: Long, currency: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var totalMonths by remember { mutableStateOf("") }
    var startMonth by remember { mutableStateOf("") }
    var monthly by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Add Chit Fund", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Chit Fund Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = totalMonths, onValueChange = { totalMonths = it }, label = { Text("Total Months") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = startMonth, onValueChange = { startMonth = it }, label = { Text("Start Month (YYYY-MM)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = monthly, onValueChange = { monthly = it }, label = { Text("Monthly Contribution") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = {
                        val mths = totalMonths.toIntOrNull() ?: 0
                        val m = (monthly.toDoubleOrNull() ?: 0.0) * 100
                        if (name.isNotBlank() && mths > 0) {
                            onSave(name, mths, startMonth, m.toLong(), "USD")
                            onDismiss()
                        }
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun AddRecurringTxnDialog(
    onDismiss: () -> Unit,
    onSave: (type: String, amountMinor: Long, currency: String, accountId: String, categoryId: String?, desc: String?, freq: String, nextRun: String) -> Unit
) {
    var type by remember { mutableStateOf("EXPENSE") }
    var amount by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf("test_account_id") } // Simplified, normally dropdown
    var desc by remember { mutableStateOf("") }
    var freq by remember { mutableStateOf("MONTHLY") }
    var nextRun by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Add Recurring Transaction", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type (INCOME/EXPENSE)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = freq, onValueChange = { freq = it }, label = { Text("Frequency (e.g. MONTHLY)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nextRun, onValueChange = { nextRun = it }, label = { Text("Next Run Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = {
                        val amt = (amount.toDoubleOrNull() ?: 0.0) * 100
                        if (type.isNotBlank() && amt > 0) {
                            onSave(type, amt.toLong(), "USD", accountId, null, desc, freq, nextRun)
                            onDismiss()
                        }
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
