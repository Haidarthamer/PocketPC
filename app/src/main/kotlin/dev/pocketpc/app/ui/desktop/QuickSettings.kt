package dev.pocketpc.app.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pocketpc.app.ui.icons.AppIcons
import dev.pocketpc.app.ui.theme.Brand

/**
 * Quick settings: toggles for desktop behaviour, media volume, and live system readings.
 *
 * Deliberately honest about Android's limits:
 *  - Media volume is adjusted through this app's own AudioManager stream, which needs no
 *    permission and is what a user expects from an in-app slider. Changing the *system*
 *    ringer/brightness requires a user-granted permission, and the panel says so instead
 *    of showing a control that silently does nothing.
 *  - Brightness here adjusts the desktop's own dimming overlay, not the panel backlight.
 */
@Composable
fun QuickSettingsPanel(
    dark: Boolean,
    animationsEnabled: Boolean,
    onToggleAnimations: () -> Unit,
    volumePercent: Int,
    onVolumeChange: (Int) -> Unit,
    desktopDimPercent: Int,
    onDimChange: (Int) -> Unit,
    batteryPercent: Int?,
    charging: Boolean,
    thermal: String,
    network: String,
    profile: String,
    onOpenGpu: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val surface = if (dark) Brand.Surface else Color.White
    val border = if (dark) Brand.Border else Brand.LightBorder
    val fg = if (dark) Brand.TextPrimary else Brand.LightTextPrimary
    val subtle = if (dark) Brand.TextSecondary else Brand.LightTextSecondary

    Column(
        modifier = Modifier
            .width(320.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(surface)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Quick settings", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = fg)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Tile("Animations", animationsEnabled, dark, Modifier.weight(1f)) { onToggleAnimations() }
            Tile("Locate GPU", true, dark, Modifier.weight(1f)) { onOpenGpu() }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Tile("Settings", true, dark, Modifier.weight(1f)) { onOpenSettings() }
            Tile("Charging", charging, dark, Modifier.weight(1f)) { }
        }

        Slider("Media volume (this app)", volumePercent, 0..100, dark, subtle, fg, onVolumeChange)
        Slider("Desktop dimming", desktopDimPercent, 0..80, dark, subtle, fg, onDimChange)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Reading("Battery", batteryPercent?.let { "$it%" } ?: "unavailable", subtle, fg)
            Reading("Thermal", thermal, subtle, fg)
            Reading("Network", network, subtle, fg)
            Reading("Profile", profile, subtle, fg)
        }

        Text(
            "System brightness and ringer volume need a user-granted permission; only in-app " +
                "levels are adjustable here.",
            fontSize = 9.5.sp,
            lineHeight = 13.sp,
            color = subtle,
        )
    }
}

@Composable
private fun RowScope.Tile(label: String, active: Boolean, dark: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (active) Brand.Blue.copy(alpha = 0.20f) else if (dark) Brand.SurfaceRaised else Color(0xFFEFF4FB))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (active) Brand.Blue else Brand.TextMuted),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            label,
            fontSize = 10.5.sp,
            color = if (dark) Brand.TextPrimary else Brand.LightTextPrimary,
            maxLines = 1,
        )
    }
}

/** A draggable track implemented with pointer input; no dead sliders. */
@Composable
private fun Slider(
    label: String,
    value: Int,
    range: IntRange,
    dark: Boolean,
    labelColor: Color,
    valueColor: Color,
    onChange: (Int) -> Unit,
) {
    val fraction = ((value - range.first).toFloat() / (range.last - range.first).coerceAtLeast(1)).coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, fontSize = 10.sp, color = labelColor, modifier = Modifier.weight(1f))
            Text("$value", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = valueColor)
        }
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .pointerInputTrack { x, width ->
                    val f = (x / width).coerceIn(0f, 1f)
                    onChange((range.first + f * (range.last - range.first)).toInt())
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (dark) Brand.Border else Brand.LightBorder),
            )
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brand.Blue),
            )
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(22.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    Modifier
                        .size(13.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Brand.Blue),
                )
            }
        }
    }
}

/**
 * Makes a track respond to taps and horizontal drags, reporting the pointer position in
 * pixels together with the track width so the caller can convert to a value.
 */
private fun Modifier.pointerInputTrack(onPosition: (x: Float, width: Float) -> Unit): Modifier =
    this.pointerInput(Unit) {
        detectTapGestures { offset -> onPosition(offset.x, size.width.toFloat()) }
    }.pointerInput(Unit) {
        detectHorizontalDragGestures { change, _ ->
            onPosition(change.position.x, size.width.toFloat())
        }
    }

@Composable
private fun Reading(label: String, value: String, labelColor: Color, valueColor: Color) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 10.5.sp, color = labelColor, modifier = Modifier.weight(1f))
        Text(value, fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, color = valueColor, maxLines = 1)
    }
}
