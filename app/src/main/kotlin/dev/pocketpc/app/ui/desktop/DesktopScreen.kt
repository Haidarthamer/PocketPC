package dev.pocketpc.app.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pocketpc.app.ui.apps.AppHost
import dev.pocketpc.app.ui.apps.DesktopServices
import dev.pocketpc.app.ui.compositor.CompositionStats
import dev.pocketpc.app.ui.compositor.FrameInstrumentation
import dev.pocketpc.app.ui.compositor.Wallpaper
import dev.pocketpc.app.ui.theme.Brand
import dev.pocketpc.desktop.apps.AppDefinition
import dev.pocketpc.desktop.apps.AppRegistry
import dev.pocketpc.desktop.shell.ShellModel
import dev.pocketpc.desktop.wm.OffsetDp
import dev.pocketpc.desktop.wm.WindowState
import dev.pocketpc.desktop.wm.WorkspaceManager
import java.time.LocalDateTime

/**
 * The desktop: wallpaper, icons, windows, taskbar and overlays.
 *
 * Layering order (bottom to top): wallpaper → icons → windows → taskbar → overlays.
 * Windows are drawn in z-order from the workspace manager, so the compositor output
 * always matches the tested model.
 */
@Composable
fun DesktopScreen(
    workspaces: WorkspaceManager,
    shell: ShellModel,
    services: DesktopServices,
    report: dev.pocketpc.system.capability.SystemReport?,
    gpuResult: dev.pocketpc.graphics.GpuVerificationResult?,
    gpuRunning: Boolean,
    stats: CompositionStats,
    clock: LocalDateTime,
    batteryPercent: Int?,
    charging: Boolean,
    networkLabel: String,
    thermalLabel: String,
    iconPositions: IconPositions,
    onRunGpuTest: () -> Unit,
    onLaunch: (AppDefinition) -> Unit,
    onDismissOverlays: () -> Unit,
    onOpenGpu: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val density = LocalDensity.current
    val dark = services.darkTheme
    var contextMenuAt by remember { mutableStateOf<OffsetDp?>(null) }
    var selectedIcon by remember { mutableStateOf<String?>(null) }
    var showShortcuts by remember { mutableStateOf(false) }
    val wm = workspaces.active

    FrameInstrumentation(stats, enabled = services.animationsEnabled)

    Box(Modifier.fillMaxSize()) {
        Wallpaper(dark = dark, animate = services.animationsEnabled)

        // ---------- desktop icons ----------
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = 12.dp, start = 10.dp, bottom = TaskbarHeight),
        ) {
            val columns = 4
            AppRegistry.defaultDesktopIcons.forEachIndexed { index, appId ->
                val app = AppRegistry.find(appId) ?: return@forEachIndexed
                val col = index % columns
                val row = index / columns
                val default = OffsetDp(10f + col * 88f, 8f + row * 96f)
                DesktopIcon(
                    app = app,
                    position = iconPositions.get(appId, default),
                    dark = dark,
                    selected = selectedIcon == appId,
                    onSelect = { selectedIcon = appId },
                    onLaunch = {
                        selectedIcon = null
                        onLaunch(app)
                    },
                    onMove = { newPos -> iconPositions.set(appId, newPos) },
                    onContextMenu = { contextMenuAt = OffsetDp(default.x, default.y + 70f) },
                    onLongPress = { contextMenuAt = OffsetDp(90f, 140f) },
                )
            }

            if (wm.visibleWindows.isEmpty()) {
                DesktopEmptyHint(dark)
            }

            // Desktop background: long-press opens the context menu.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .then(desktopTapModifier { selectedIcon = null }),
            )

            LaunchedEffect(report) {
                // Keep icon positions valid when the screen size changes.
                iconPositions.clampInto(
                    width = 400f,
                    height = 700f,
                    iconSize = 82f,
                )
            }
        }

        // ---------- windows ----------
        wm.visibleWindows.forEach { window ->
            if (!window.isVisible) return@forEach
            DesktopWindowView(
                window = window,
                focused = wm.focused?.id == window.id,
                wm = wm,
                dark = dark,
                onContent = { w ->
                    AppHost(
                        window = w,
                        dark = dark,
                        services = services,
                        report = report,
                        gpuResult = gpuResult,
                        onRunGpuTest = onRunGpuTest,
                        onGpuTestRunning = gpuRunning,
                    )
                },
            )
        }

        // ---------- taskbar ----------
        Box(Modifier.align(Alignment.BottomCenter)) {
            Taskbar(
                shell = shell,
                dark = dark,
                clock = clock,
                batteryPercent = batteryPercent,
                charging = charging,
                workspaceCount = workspaces.count,
                activeWorkspace = workspaces.activeIndex,
                workspaceWindowCounts = workspaces.windowCounts(),
                onStart = {
                    shell.toggleStartMenu()
                    contextMenuAt = null
                },
                onQuickSettings = {
                    shell.toggleQuickSettings()
                    contextMenuAt = null
                },
                onAppClick = onLaunch,
                onWorkspaceClick = { workspaces.switchTo(it) },
            )
        }

        // ---------- overlays ----------
        AnimatedVisibility(
            visible = shell.isStartMenuOpen,
            enter = fadeIn() + scaleIn(initialScale = 0.96f),
            exit = fadeOut() + scaleOut(targetScale = 0.96f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = TaskbarHeight + 6.dp),
        ) {
            StartMenu(
                shell = shell,
                dark = dark,
                onLaunch = onLaunch,
                onSettings = onOpenSettings,
                onAbout = {
                    AppRegistry.find("about")?.let(onLaunch)
                },
                onDismiss = { shell.closeStartMenu() },
            )
        }

        AnimatedVisibility(
            visible = shell.isQuickSettingsOpen,
            enter = fadeIn() + scaleIn(initialScale = 0.96f),
            exit = fadeOut() + scaleOut(targetScale = 0.96f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = TaskbarHeight + 6.dp),
        ) {
            QuickSettingsPanel(
                dark = dark,
                animationsEnabled = services.animationsEnabled,
                onToggleAnimations = { services.setAnimations(!services.animationsEnabled) },
                volumePercent = services.volumePercent(),
                onVolumeChange = services.setVolume,
                desktopDimPercent = 0,
                onDimChange = { },
                batteryPercent = batteryPercent,
                charging = charging,
                thermal = thermalLabel,
                network = networkLabel,
                profile = report?.profile?.id?.name ?: "unknown",
                onOpenGpu = onOpenGpu,
                onOpenSettings = onOpenSettings,
            )
        }

        contextMenuAt?.let { at ->
            DesktopContextMenu(
                position = at,
                dark = dark,
                canPaste = false,
                onOpenTerminal = { AppRegistry.find("terminal")?.let(onLaunch) },
                onOpenFiles = { AppRegistry.find("files")?.let(onLaunch) },
                onTileWindows = { dev.pocketpc.desktop.wm.TileLayout.tile(wm) },
                onToggleWallpaperAnimation = { services.setAnimations(!services.animationsEnabled) },
                onShowShortcuts = { showShortcuts = true },
                onOpenSettings = onOpenSettings,
                onDismiss = { contextMenuAt = null },
            )
        }

        // ---------- frame statistics badge ----------
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Brand.NavyDeep.copy(alpha = 0.55f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                "%.0f fps · %.1f ms".format(stats.fps, stats.avgFrameMs),
                fontSize = 9.sp,
                color = Brand.TextSecondary,
            )
        }

        if (showShortcuts) {
            ShortcutSheet(dark = dark, onDismiss = { showShortcuts = false })
        }
    }
}

/**
 * Detects a tap on the desktop background. Kept as a small helper so the desktop file
 * reads declaratively.
 */
private fun desktopTapModifier(onTap: () -> Unit): Modifier =
    Modifier.pointerInput(Unit) {
        detectTapGestures(onTap = { onTap() })
    }

@Composable
private fun ShortcutSheet(dark: Boolean, onDismiss: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .then(desktopTapModifier(onDismiss)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (dark) Brand.Surface else Color.White)
                .padding(16.dp),
        ) {
            Text("Keyboard shortcuts", fontSize = 13.sp, color = if (dark) Brand.TextPrimary else Brand.LightTextPrimary)
            Text(
                "External, USB and Bluetooth keyboards all work; the same bindings also drive the on-screen controls.",
                fontSize = 9.5.sp,
                color = Brand.TextMuted,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            )
            dev.pocketpc.desktop.input.KeyBindings.describeAll().forEach { (chord, action) ->
                androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(
                        chord,
                        fontSize = 10.sp,
                        color = Brand.Blue,
                        modifier = Modifier.width(112.dp),
                    )
                    Text(action, fontSize = 10.sp, color = if (dark) Brand.TextSecondary else Brand.LightTextSecondary)
                }
            }
        }
    }
}
