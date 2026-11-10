package dev.pocketpc.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Pocket PC visual identity.
 *
 * Kept in one file so every surface (desktop, windows, taskbar, dialogs) shares one
 * palette and one type scale. Colours mirror res/values/colors.xml and the icon.
 */
object Brand {
    val Navy = Color(0xFF0E1A30)
    val NavyDeep = Color(0xFF0A101E)
    val Surface = Color(0xFF16233A)
    val SurfaceRaised = Color(0xFF1E2E4A)
    val Border = Color(0xFF2C3F60)
    val Blue = Color(0xFF6AC4FF)
    val BlueDeep = Color(0xFF409CE8)
    val Amber = Color(0xFFFFB054)
    val Green = Color(0xFF4ADE80)
    val Red = Color(0xFFF87171)
    val TextPrimary = Color(0xFFE8F0FC)
    val TextSecondary = Color(0xFF9FB2CC)
    val TextMuted = Color(0xFF6B7F9C)

    // Light theme equivalents, for "light mode".
    val LightSurface = Color(0xFFF4F7FC)
    val LightSurfaceRaised = Color(0xFFFFFFFF)
    val LightBorder = Color(0xFFD3DEEE)
    val LightTextPrimary = Color(0xFF152238)
    val LightTextSecondary = Color(0xFF54637A)
}

/** Status colours used consistently for capability states across every screen. */
object StatusColors {
    val verified = Brand.Green
    val tested = Color(0xFF60A5FA)
    val available = Brand.Blue
    val limited = Brand.Amber
    val failed = Brand.Red
    val unsupported = Brand.TextMuted
}

private val DesktopTypography = Typography(
    displaySmall = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall = TextStyle(fontSize = 11.5.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 10.5.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium),
)

private val DarkScheme = darkColorScheme(
    primary = Brand.Blue,
    onPrimary = Brand.NavyDeep,
    secondary = Brand.Amber,
    onSecondary = Brand.NavyDeep,
    background = Brand.NavyDeep,
    onBackground = Brand.TextPrimary,
    surface = Brand.Surface,
    onSurface = Brand.TextPrimary,
    surfaceVariant = Brand.SurfaceRaised,
    onSurfaceVariant = Brand.TextSecondary,
    outline = Brand.Border,
    error = Brand.Red,
)

private val LightScheme = lightColorScheme(
    primary = Brand.BlueDeep,
    onPrimary = Color.White,
    secondary = Color(0xFFB26A00),
    background = Brand.LightSurface,
    onBackground = Brand.LightTextPrimary,
    surface = Brand.LightSurfaceRaised,
    onSurface = Brand.LightTextPrimary,
    surfaceVariant = Color(0xFFE9EFF8),
    onSurfaceVariant = Brand.LightTextSecondary,
    outline = Brand.LightBorder,
    error = Color(0xFFB3261E),
)

@Composable
fun PocketPcTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = DesktopTypography,
        content = content,
    )
}

/** Monospace style for terminal, code and metric output. */
val MonoStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 12.sp,
    lineHeight = 17.sp,
)
