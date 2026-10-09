package app.privatemoney.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Semantic colours for financial meaning. Colour is never the only signal; amounts carry signs. */
@Immutable
class FinanceColors(
    val positive: Color,
    val negative: Color,
    val warning: Color,
    val info: Color,
    val muted: Color,
    val raised: Color,
    val hairline: Color,
)

val LocalFinanceColors = staticCompositionLocalOf<FinanceColors> {
    error("FinanceColors not provided")
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFF2EFE9),
    onPrimary = Color(0xFF151618),
    background = Color(0xFF131416),
    onBackground = Color(0xFFF2EFE9),
    surface = Color(0xFF1A1B1E),
    onSurface = Color(0xFFF2EFE9),
    surfaceVariant = Color(0xFF232428),
    onSurfaceVariant = Color(0xFFA3A3AA),
    outline = Color(0xFF3A3B40),
    error = Color(0xFFE0736B),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF1C1D20),
    onPrimary = Color(0xFFF7F5F0),
    background = Color(0xFFF7F5F0),
    onBackground = Color(0xFF1C1D20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1D20),
    surfaceVariant = Color(0xFFECE9E2),
    onSurfaceVariant = Color(0xFF5F6066),
    outline = Color(0xFFCFCBC2),
    error = Color(0xFFB3463F),
)

private val DarkFinance = FinanceColors(
    positive = Color(0xFF7FD1AE),
    negative = Color(0xFFE0736B),
    warning = Color(0xFFD1A85A),
    info = Color(0xFF7FA6D9),
    muted = Color(0xFFA3A3AA),
    raised = Color(0xFF1F2023),
    hairline = Color(0xFF2B2C30),
)

private val LightFinance = FinanceColors(
    positive = Color(0xFF1F7A58),
    negative = Color(0xFFB3463F),
    warning = Color(0xFF8A6A1F),
    info = Color(0xFF3A6AA8),
    muted = Color(0xFF5F6066),
    raised = Color(0xFFFFFFFF),
    hairline = Color(0xFFE0DCD3),
)

/** Numeric styles use tabular figures so columns of amounts align. */
object MoneyStyles {
    val hero = TextStyle(fontSize = 40.sp, lineHeight = 46.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum", letterSpacing = (-0.5).sp)
    val large = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")
    val row = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")
    val input = TextStyle(fontSize = 48.sp, lineHeight = 56.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum", letterSpacing = (-1).sp)
}

private val BaseTypography = Typography()
private val PmosTypography = BaseTypography.copy(
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = BaseTypography.labelSmall.copy(letterSpacing = 1.2.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun PmosTheme(mode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalFinanceColors provides if (dark) DarkFinance else LightFinance) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = PmosTypography,
            content = content,
        )
    }
}
