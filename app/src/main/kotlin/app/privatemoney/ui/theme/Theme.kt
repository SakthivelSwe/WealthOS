package app.privatemoney.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Semantic colours for financial meaning and luxury FinTech styling. */
@Immutable
class FinanceColors(
    val positive: Color,
    val negative: Color,
    val warning: Color,
    val info: Color,
    val muted: Color,
    val raised: Color,
    val hairline: Color,
    val primaryGlow: Color,
    val cardGradientStart: Color,
    val cardGradientEnd: Color,
    val accentCyan: Color,
    val accentGold: Color,
    val accentPurple: Color,
    val glassBorder: Color,
)

val LocalFinanceColors = staticCompositionLocalOf<FinanceColors> {
    error("FinanceColors not provided")
}

/** Whether financial values should be masked with dots for privacy in public. */
val LocalPrivacyMask = compositionLocalOf { false }

enum class ThemeMode { SYSTEM, LIGHT, DARK, MIDNIGHT }

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF00E599),
    onPrimary = Color(0xFF06140D),
    primaryContainer = Color(0xFF003822),
    onPrimaryContainer = Color(0xFF66FFBA),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF042033),
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color(0xFF331C00),
    background = Color(0xFF0B0E14),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF121622),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF192030),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF26334B),
    outlineVariant = Color(0xFF1E283C),
    error = Color(0xFFF43F5E),
    onError = Color(0xFFFFFFFF),
)

private val MidnightScheme = darkColorScheme(
    primary = Color(0xFF00E599),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF002919),
    onPrimaryContainer = Color(0xFF66FFBA),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF000000),
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0A0C10),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF141720),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF1F2430),
    outlineVariant = Color(0xFF161A24),
    error = Color(0xFFF43F5E),
    onError = Color(0xFFFFFFFF),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = Color(0xFF0284C7),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFD97706),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFE11D48),
    onError = Color(0xFFFFFFFF),
)

private val DarkFinance = FinanceColors(
    positive = Color(0xFF10B981),
    negative = Color(0xFFF43F5E),
    warning = Color(0xFFF59E0B),
    info = Color(0xFF38BDF8),
    muted = Color(0xFF94A3B8),
    raised = Color(0xFF181F2E),
    hairline = Color(0xFF222B3D),
    primaryGlow = Color(0x3300E599),
    cardGradientStart = Color(0xFF161F33),
    cardGradientEnd = Color(0xFF0F1524),
    accentCyan = Color(0xFF38BDF8),
    accentGold = Color(0xFFF59E0B),
    accentPurple = Color(0xFFA855F7),
    glassBorder = Color(0x26FFFFFF),
)

private val MidnightFinance = FinanceColors(
    positive = Color(0xFF10B981),
    negative = Color(0xFFF43F5E),
    warning = Color(0xFFF59E0B),
    info = Color(0xFF38BDF8),
    muted = Color(0xFF8B949E),
    raised = Color(0xFF12141A),
    hairline = Color(0xFF1B1F28),
    primaryGlow = Color(0x4400E599),
    cardGradientStart = Color(0xFF12151D),
    cardGradientEnd = Color(0xFF08090D),
    accentCyan = Color(0xFF38BDF8),
    accentGold = Color(0xFFF59E0B),
    accentPurple = Color(0xFFA855F7),
    glassBorder = Color(0x1FFFFFFF),
)

private val LightFinance = FinanceColors(
    positive = Color(0xFF059669),
    negative = Color(0xFFE11D48),
    warning = Color(0xFFD97706),
    info = Color(0xFF0284C7),
    muted = Color(0xFF64748B),
    raised = Color(0xFFFFFFFF),
    hairline = Color(0xFFE2E8F0),
    primaryGlow = Color(0x20059669),
    cardGradientStart = Color(0xFFF8FAFC),
    cardGradientEnd = Color(0xFFEDF2F7),
    accentCyan = Color(0xFF0284C7),
    accentGold = Color(0xFFD97706),
    accentPurple = Color(0xFF9333EA),
    glassBorder = Color(0x1F000000),
)

/** Numeric styles use tabular figures so columns of amounts align. */
object MoneyStyles {
    val hero = TextStyle(fontSize = 38.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum", letterSpacing = (-1).sp)
    val large = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum", letterSpacing = (-0.5).sp)
    val row = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")
    val input = TextStyle(fontSize = 46.sp, lineHeight = 54.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum", letterSpacing = (-1.5).sp)
    val small = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")
}

private val BaseTypography = Typography()
private val PmosTypography = BaseTypography.copy(
    headlineLarge = BaseTypography.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = BaseTypography.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = BaseTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = BaseTypography.bodyLarge.copy(lineHeight = 24.sp),
    bodyMedium = BaseTypography.bodyMedium.copy(lineHeight = 20.sp),
    labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = BaseTypography.labelSmall.copy(letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold),
)

@Composable
fun PmosTheme(
    mode: ThemeMode = ThemeMode.DARK,
    isMasked: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val (scheme, finance) = when (mode) {
        ThemeMode.SYSTEM -> if (isSystemDark) DarkScheme to DarkFinance else LightScheme to LightFinance
        ThemeMode.LIGHT -> LightScheme to LightFinance
        ThemeMode.DARK -> DarkScheme to DarkFinance
        ThemeMode.MIDNIGHT -> MidnightScheme to MidnightFinance
    }

    CompositionLocalProvider(
        LocalFinanceColors provides finance,
        LocalPrivacyMask provides isMasked,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = PmosTypography,
            content = content,
        )
    }
}
