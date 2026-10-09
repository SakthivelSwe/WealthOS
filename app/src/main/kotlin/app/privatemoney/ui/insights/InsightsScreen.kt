package app.privatemoney.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.privatemoney.ui.MoneyFormat
import app.privatemoney.ui.components.Hairline
import app.privatemoney.ui.components.MoneyText
import app.privatemoney.ui.components.MoneyTint
import app.privatemoney.ui.components.SectionLabel
import app.privatemoney.ui.theme.LocalFinanceColors
import app.privatemoney.ui.theme.MoneyStyles

@Composable
fun InsightsRoute(viewModel: InsightsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (state.topCategories.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionLabel("Top expenses this month")
                state.topCategories.forEach { cat ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(12.dp)
                                .background(Color(cat.color))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        MoneyText(
                            minor = cat.amountMinor,
                            currency = state.currency,
                            style = MoneyStyles.row,
                            tint = MoneyTint.NEUTRAL
                        )
                    }
                }
            }
            Hairline()
        }

        if (state.trend.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionLabel("6-Month Trend")
                state.trend.forEach { trend ->
                    Column {
                        Text(
                            text = trend.month,
                            style = MaterialTheme.typography.labelMedium,
                            color = LocalFinanceColors.current.muted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "In",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalFinanceColors.current.muted
                                )
                                MoneyText(
                                    minor = trend.income,
                                    currency = state.currency,
                                    style = MoneyStyles.row,
                                    tint = MoneyTint.NEUTRAL
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Out",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LocalFinanceColors.current.muted
                                )
                                MoneyText(
                                    minor = trend.expense,
                                    currency = state.currency,
                                    style = MoneyStyles.row,
                                    tint = MoneyTint.NEUTRAL
                                )
                            }
                        }
                    }
                }
            }
        }

        if (state.topCategories.isEmpty() && state.trend.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No insights available yet.",
                    color = LocalFinanceColors.current.muted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
