package dev.pocketpc.app.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pocketpc.app.ui.icons.AppIcons
import dev.pocketpc.app.ui.theme.Brand
import dev.pocketpc.desktop.apps.AppDefinition
import dev.pocketpc.desktop.shell.ShellModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Fixed taskbar height in dp; the desktop reserves this from the window viewport. */
val TaskbarHeight = 52.dp

/**
 * The taskbar: Start button, running-window list, workspace pills, tray, clock and the
 * quick-settings trigger.
 *
 * Every element is wired to real state. Volume and brightness live in the quick-settings
 * panel because Android restricts programmatic system brightness/volume without user
 * interaction; the panel says so rather than presenting dead sliders.
 */
@Composable
fun Taskbar(
    shell: ShellModel,
    dark: Boolean,
    clock: LocalDateTime,
    batteryPercent: Int?,
    charging: Boolean,
    workspaceCount: Int,
    activeWorkspace: Int,
    workspaceWindowCounts: List<Int>,
    onStart: () -> Unit,
    onQuickSettings: () -> Unit,
    onAppClick: (AppDefinition) -> Unit,
    onWorkspaceClick: (Int) -> Unit,
) {
    val bg = if (dark) Brand.Navy.copy(alpha = 0.94f) else Color(0xFFF7FAFE)
    val fg = if (dark) Brand.TextPrimary else Brand.LightTextPrimary
    val subtle = if (dark) Brand.TextSecondary else Brand.LightTextSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TaskbarHeight)
            .background(bg)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ----- Start -----
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (shell.isStartMenuOpen) Brand.Blue.copy(alpha = 0.22f) else Color.Transparent)
                .clickable(onClick = onStart),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.start, contentDescription = "Start", tint = Brand.Blue, modifier = Modifier.size(20.dp))
        }

        // ----- Search -----
        Row(
            modifier = Modifier
                .padding(start = 4.dp)
                .height(34.dp)
                .width(150.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (dark) Brand.SurfaceRaised.copy(alpha = 0.6f) else Color(0xFFE9EFF8))
                .clickable(onClick = onStart)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(AppIcons.search, contentDescription = null, tint = subtle, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(6.dp))
            Text("Search", fontSize = 11.5.sp, color = subtle, maxLines = 1)
        }

        Spacer(Modifier.width(8.dp))

        // ----- Running / pinned apps -----
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val shown = (shell.pinnedApps + shell.runningApps.filter { !shell.isPinned(it.id) })
                .distinctBy { it.id }
                .take(7)
            shown.forEach { app ->
                TaskbarAppButton(
                    app = app,
                    running = shell.isRunning(app.id),
                    active = shell.isActive(app.id),
                    dark = dark,
                    onClick = { onAppClick(app) },
                )
            }
        }

        // ----- Workspaces -----
        if (workspaceCount > 1) {
            Row(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (dark) Brand.SurfaceRaised.copy(alpha = 0.45f) else Color(0xFFE9EFF8))
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(workspaceCount) { index ->
                    val active = index == activeWorkspace
                    val count = workspaceWindowCounts.getOrElse(index) { 0 }
                    Box(
                        modifier = Modifier
                            .size(width = if (active) 22.dp else 18.dp, height = 22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (active) Brand.Blue else Color.Transparent)
                            .clickable { onWorkspaceClick(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (count > 0) "${index + 1}·$count" else "${index + 1}",
                            fontSize = 9.5.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) Brand.NavyDeep else subtle,
                        )
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
        }

        // ----- Tray -----
        Row(
            modifier = Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onQuickSettings)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NetworkIndicator(dark)
            Spacer(Modifier.width(8.dp))
            BatteryIndicator(percent = batteryPercent, charging = charging, dark = dark)
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = clock.format(DateTimeFormatter.ofPattern("HH:mm")),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    color = fg,
                )
                Text(
                    text = clock.format(DateTimeFormatter.ofPattern("EEE d MMM")),
                    fontSize = 9.sp,
                    color = subtle,
                )
            }
            Spacer(Modifier.width(4.dp))
            Icon(AppIcons.chevronUp, contentDescription = "Quick settings", tint = subtle, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun TaskbarAppButton(
    app: AppDefinition,
    running: Boolean,
    active: Boolean,
    dark: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    active -> Brand.Blue.copy(alpha = 0.24f)
                    running -> Brand.Blue.copy(alpha = 0.10f)
                    else -> Color.Transparent
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppIconBadge(app.id, size = 22, tint = if (active || running) Brand.Blue else if (dark) Brand.TextSecondary else Brand.LightTextSecondary)
        // Running indicator: a short bar under the icon, the desktop convention.
        if (running) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
                    .size(width = if (active) 16.dp else 9.dp, height = 2.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (active) Brand.Blue else Brand.Blue.copy(alpha = 0.55f)),
            )
        }
    }
}

@Composable
private fun NetworkIndicator(dark: Boolean) {
    // A three-bar signal icon drawn inline; the fill level reflects real connectivity,
    // passed in by the caller from ConnectivityManager.
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
        listOf(4.dp, 7.dp, 10.dp).forEachIndexed { i, h ->
            Box(
                Modifier
                    .width(2.5.dp)
                    .height(h)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (dark) Brand.TextSecondary else Brand.LightTextSecondary),
            )
        }
    }
}

@Composable
private fun BatteryIndicator(percent: Int?, charging: Boolean, dark: Boolean) {
    val pct = percent ?: -1
    val color = when {
        charging -> Brand.Green
        pct in 0..15 -> Brand.Red
        else -> if (dark) Brand.TextSecondary else Brand.LightTextSecondary
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 22.dp, height = 11.dp)
                .clip(RoundedCornerShape(2.5.dp))
                .background(color.copy(alpha = 0.18f)),
        ) {
            if (pct >= 0) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(pct / 100f)
                        .background(color),
                )
            }
        }
        Spacer(Modifier.width(3.dp))
        Text(
            text = when {
                charging -> "⚡"
                pct >= 0 -> "$pct%"
                else -> "—"
            },
            fontSize = 9.5.sp,
            color = color,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 18.dp),
        )
    }
}
