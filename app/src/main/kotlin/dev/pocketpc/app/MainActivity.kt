package dev.pocketpc.app

import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import dev.pocketpc.app.ui.apps.DesktopServices
import dev.pocketpc.app.ui.compositor.CompositionStats
import dev.pocketpc.app.ui.desktop.DesktopScreen
import dev.pocketpc.app.ui.desktop.IconPositions
import dev.pocketpc.app.ui.desktop.TaskbarHeight
import dev.pocketpc.app.ui.theme.PocketPcTheme
import dev.pocketpc.desktop.apps.AppDefinition
import dev.pocketpc.desktop.input.DesktopAction
import dev.pocketpc.desktop.input.DesktopKey
import dev.pocketpc.desktop.input.KeyBindings
import dev.pocketpc.desktop.input.KeyChord
import dev.pocketpc.desktop.input.SnapSide
import dev.pocketpc.desktop.shell.ShellModel
import dev.pocketpc.desktop.wm.Snapping
import dev.pocketpc.desktop.wm.Viewport
import dev.pocketpc.desktop.wm.WorkspaceManager
import dev.pocketpc.graphics.GlesVerificationEngine
import dev.pocketpc.graphics.GpuVerificationResult
import dev.pocketpc.system.capability.CapabilityEvaluator
import dev.pocketpc.system.capability.PerformanceProfile
import dev.pocketpc.system.capability.ProfileId
import dev.pocketpc.system.capability.SystemReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import kotlin.concurrent.thread

/**
 * Single-activity host for the desktop.
 *
 * Owns the long-lived desktop state (window manager, shell, services) so window and
 * workspace state survive recomposition and configuration changes within the process.
 */
class MainActivity : ComponentActivity() {

    private lateinit var audio: AudioManager
    private lateinit var battery: BatteryManager
    private lateinit var connectivity: ConnectivityManager
    private lateinit var power: PowerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Keep the display awake while a desktop session is active; a PC does not lock
        // its screen every 30 seconds. Released automatically when the activity stops.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        battery = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        connectivity = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        power = getSystemService(Context.POWER_SERVICE) as PowerManager

        val hardwareSource = HardwareSourceFactory.create(this)

        setContent {
            var dark by remember { mutableStateOf(true) }
            var animations by remember { mutableStateOf(true) }
            var volume by remember { mutableStateOf(readVolumePercent()) }
            var report by remember { mutableStateOf<SystemReport?>(null) }
            var gpuResult by remember { mutableStateOf<GpuVerificationResult?>(null) }
            var gpuRunning by remember { mutableStateOf(false) }
            var gpuAttempted by remember { mutableStateOf(false) }
            var profileCap by remember { mutableStateOf<ProfileId?>(null) }

            val shell = remember { ShellModel() }
            val stats = remember { CompositionStats() }
            val iconPositions = remember { IconPositions() }
            val configuration = LocalConfiguration.current
            val density = LocalDensity.current

            // Screen geometry in dp, minus the status bar and our taskbar.
            val screenWidthDp = with(density) { configuration.screenWidthDp.dp }
            val screenHeightDp = with(density) { configuration.screenHeightDp.dp }
            val viewport = remember(configuration.screenWidthDp, configuration.screenHeightDp) {
                Viewport(
                    width = screenWidthDp.value,
                    height = screenHeightDp.value,
                    topInset = 0f,
                    bottomInset = TaskbarHeight.value,
                )
            }
            val workspaces = remember { WorkspaceManager(count = 3) { viewport } }

            // Battery / clock ticker.
            var clock by remember { mutableStateOf(LocalDateTime.now()) }
            var batteryPercent by remember { mutableStateOf(readBatteryPercent()) }
            var charging by remember { mutableStateOf(isCharging()) }
            var network by remember { mutableStateOf(networkLabel()) }
            var thermal by remember { mutableStateOf(thermalLabel()) }

            LaunchedEffect(Unit) {
                while (true) {
                    clock = LocalDateTime.now()
                    batteryPercent = readBatteryPercent()
                    charging = isCharging()
                    network = networkLabel()
                    thermal = thermalLabel()
                    delay(1_000)
                }
            }

            // Initial capability probe.
            LaunchedEffect(Unit) {
                report = withContext(Dispatchers.Default) {
                    CapabilityEvaluator.evaluate(hardwareSource.snapshot())
                }
            }

            val services = remember(dark, animations, volume) {
                DesktopServices(
                    basePath = filesDir.absolutePath,
                    volumePercent = { volume },
                    setVolume = { volume = it; applyVolume(it) },
                    batteryPercent = { batteryPercent },
                    isCharging = { charging },
                    networkLabel = { network },
                    thermalLabel = { thermal },
                    memoryUsedMb = { deviceUsedMemoryMb() },
                    memoryTotalMb = { deviceTotalMemoryMb() },
                    cpuCores = Runtime.getRuntime().availableProcessors(),
                    darkTheme = dark,
                    setDarkTheme = { dark = it },
                    animationsEnabled = animations,
                    setAnimations = { animations = it },
                    runGpuTest = {},
                )
            }

            fun launch(app: AppDefinition) {
                val wm = workspaces.active
                val base = wm.defaultBounds()
                wm.open(
                    appId = app.id,
                    title = app.name,
                    bounds = dev.pocketpc.desktop.wm.WindowRect(
                        x = base.x,
                        y = base.y,
                        width = viewport.usableWidth * app.widthFraction,
                        height = viewport.usableHeight * app.heightFraction,
                    ),
                    resizable = app.resizable,
                    singletonPerApp = app.singleton,
                )
                shell.onAppLaunched(app.id)
            }

            fun runGpu() {
                if (gpuRunning) return
                gpuRunning = true
                thread(name = "pocketpc-gpu-verify") {
                    val result = GlesVerificationEngine().run()
                    runOnUiThread {
                        gpuResult = result
                        gpuRunning = false
                        gpuAttempted = true
                        val snapshot = report?.snapshot
                        if (snapshot != null) {
                            report = CapabilityEvaluator.evaluateWithCompositor(
                                snapshot,
                                result.evidence,
                                result.performance,
                            )
                        }
                    }
                }
            }

            /** Applies a desktop action from either the keyboard or an on-screen control. */
            fun perform(action: DesktopAction) {
                val wm = workspaces.active
                val focused = wm.focused
                when (action) {
                    DesktopAction.ToggleStartMenu -> shell.toggleStartMenu()
                    DesktopAction.FocusNextWindow -> shell.nextRunningApp()?.let { app -> launch(app) }
                    DesktopAction.FocusPreviousWindow -> shell.nextRunningApp(backwards = true)?.let { app -> launch(app) }
                    DesktopAction.CloseFocusedWindow -> focused?.let {
                        wm.close(it.id)
                        shell.onAppClosed(it.appId)
                    }
                    DesktopAction.MinimizeFocusedWindow -> focused?.let {
                        wm.minimize(it.id)
                        shell.onAppMinimized(it.appId)
                    }
                    DesktopAction.ToggleMaximizeFocusedWindow -> focused?.let { wm.toggleMaximize(it.id) }
                    DesktopAction.ToggleFullscreenFocusedWindow -> focused?.let {
                        wm.setFullscreen(it.id, it.state != dev.pocketpc.desktop.wm.WindowState.FULLSCREEN)
                    }
                    is DesktopAction.SnapWindow -> focused?.let {
                        wm.applyBounds(
                            it.id,
                            when (action.side) {
                                SnapSide.LEFT -> dev.pocketpc.desktop.wm.WindowRect(
                                    0f, viewport.usableTop, viewport.usableWidth / 2f, viewport.usableHeight,
                                )
                                SnapSide.RIGHT -> dev.pocketpc.desktop.wm.WindowRect(
                                    viewport.usableWidth / 2f, viewport.usableTop,
                                    viewport.usableWidth / 2f, viewport.usableHeight,
                                )
                            },
                        )
                    }
                    DesktopAction.TileAllWindows -> dev.pocketpc.desktop.wm.TileLayout.tile(wm)
                    is DesktopAction.SwitchWorkspace -> workspaces.switchTo(action.index)
                    DesktopAction.ToggleQuickSettings -> shell.toggleQuickSettings()
                    DesktopAction.OpenGpuDiagnostics -> dev.pocketpc.desktop.apps.AppRegistry.find("gpu")?.let { launch(it) }
                    DesktopAction.ShowShortcutHelp -> Unit
                }
            }

            PocketPcTheme(dark = dark) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .onKeyEvent { event ->
                            // androidx.compose.ui.input.key.type is the extension property;
                            // KeyEvent.type would resolve to the platform field.
                            if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onKeyEvent false
                            val chord = event.nativeKeyEvent.toChord() ?: return@onKeyEvent false
                            val action = KeyBindings.resolve(chord) ?: return@onKeyEvent false
                            perform(action)
                            true
                        },
                ) {
                    DesktopScreen(
                        workspaces = workspaces,
                        shell = shell,
                        services = services.copy(runGpuTest = ::runGpu),
                        report = report,
                        gpuResult = gpuResult,
                        gpuRunning = gpuRunning,
                        stats = stats,
                        clock = clock,
                        batteryPercent = batteryPercent,
                        charging = charging,
                        networkLabel = network,
                        thermalLabel = thermal,
                        iconPositions = iconPositions,
                        onRunGpuTest = ::runGpu,
                        onLaunch = ::launch,
                        onDismissOverlays = { shell.dismissOverlays() },
                        onOpenGpu = { dev.pocketpc.desktop.apps.AppRegistry.find("gpu")?.let { launch(it) } },
                        onOpenSettings = { dev.pocketpc.desktop.apps.AppRegistry.find("settings")?.let { launch(it) } },
                    )
                }
            }
        }
    }

    // ---------------------------------------------------------------- Android services

    private fun readVolumePercent(): Int = runCatching {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        audio.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / max
    }.getOrDefault(50)

    private fun applyVolume(percent: Int) {
        runCatching {
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, (max * percent / 100).coerceIn(0, max), 0)
        }
    }

    private fun readBatteryPercent(): Int? = runCatching {
        battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf { it in 0..100 }
    }.getOrNull()

    private fun isCharging(): Boolean = runCatching {
        battery.isCharging
    }.getOrDefault(false)

    private fun networkLabel(): String = runCatching {
        val active = connectivity.activeNetwork ?: return "offline"
        val caps = connectivity.getNetworkCapabilities(active) ?: return "offline"
        when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "connected"
        }
    }.getOrDefault("unknown")

    private fun thermalLabel(): String = runCatching {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return "unavailable"
        @Suppress("DEPRECATION")
        when (power.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> "nominal"
            PowerManager.THERMAL_STATUS_LIGHT -> "light"
            PowerManager.THERMAL_STATUS_MODERATE -> "moderate"
            PowerManager.THERMAL_STATUS_SEVERE -> "severe"
            PowerManager.THERMAL_STATUS_CRITICAL -> "critical"
            PowerManager.THERMAL_STATUS_EMERGENCY -> "emergency"
            PowerManager.THERMAL_STATUS_SHUTDOWN -> "shutdown"
            else -> "unknown"
        }
    }.getOrDefault("unavailable")

    private fun deviceUsedMemoryMb(): Long = runCatching {
        val info = android.app.ActivityManager.MemoryInfo()
        (getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager).getMemoryInfo(info)
        (info.totalMem - info.availMem) / (1024 * 1024)
    }.getOrDefault(0L)

    private fun deviceTotalMemoryMb(): Long = runCatching {
        val info = android.app.ActivityManager.MemoryInfo()
        (getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager).getMemoryInfo(info)
        info.totalMem / (1024 * 1024)
    }.getOrDefault(0L)
}

/** Maps an Android KeyEvent to a desktop [KeyChord], or null when unmodified. */
private fun KeyEvent.toChord(): KeyChord? {
    val key = when (keyCode) {
        KeyEvent.KEYCODE_TAB -> DesktopKey.TAB
        KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> DesktopKey.ENTER
        KeyEvent.KEYCODE_ESCAPE -> DesktopKey.ESCAPE
        KeyEvent.KEYCODE_SPACE -> DesktopKey.SPACE
        KeyEvent.KEYCODE_DPAD_LEFT -> DesktopKey.LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> DesktopKey.RIGHT
        KeyEvent.KEYCODE_DPAD_UP -> DesktopKey.UP
        KeyEvent.KEYCODE_DPAD_DOWN -> DesktopKey.DOWN
        KeyEvent.KEYCODE_W -> DesktopKey.W
        KeyEvent.KEYCODE_Q -> DesktopKey.Q
        KeyEvent.KEYCODE_M -> DesktopKey.M
        KeyEvent.KEYCODE_E -> DesktopKey.E
        KeyEvent.KEYCODE_F -> DesktopKey.F
        KeyEvent.KEYCODE_D -> DesktopKey.D
        KeyEvent.KEYCODE_L -> DesktopKey.L
        KeyEvent.KEYCODE_N -> DesktopKey.N
        KeyEvent.KEYCODE_H -> DesktopKey.H
        KeyEvent.KEYCODE_G -> DesktopKey.G
        KeyEvent.KEYCODE_1 -> DesktopKey.ONE
        KeyEvent.KEYCODE_2 -> DesktopKey.TWO
        KeyEvent.KEYCODE_3 -> DesktopKey.THREE
        else -> null
    } ?: return null
    return KeyChord(
        key = key,
        ctrl = isCtrlPressed,
        alt = isAltPressed,
        shift = isShiftPressed,
        meta = isMetaPressed,
    )
}
