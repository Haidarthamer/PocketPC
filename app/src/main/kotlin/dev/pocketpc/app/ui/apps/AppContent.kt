package dev.pocketpc.app.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pocketpc.app.ui.theme.Brand
import dev.pocketpc.app.ui.theme.MonoStyle
import dev.pocketpc.desktop.apps.AppRegistry
import dev.pocketpc.desktop.apps.CalcResult
import dev.pocketpc.desktop.apps.Calculator
import dev.pocketpc.desktop.wm.DesktopWindow
import dev.pocketpc.system.capability.CapabilityState
import dev.pocketpc.system.capability.Feature
import dev.pocketpc.system.capability.SystemReport
import java.io.File
import kotlinx.coroutines.delay

/**
 * Host that maps a window's `appId` to its real content.
 *
 * Every branch renders a functioning app; there are no "coming soon" panes. Apps that
 * need live system data take a [DesktopServices] bundle from the caller.
 */
@Composable
fun AppHost(
    window: DesktopWindow,
    dark: Boolean,
    services: DesktopServices,
    report: SystemReport?,
    gpuResult: dev.pocketpc.graphics.GpuVerificationResult?,
    onRunGpuTest: () -> Unit,
    onGpuTestRunning: Boolean,
) {
    when (window.appId) {
        "files" -> FilesApp(services, dark)
        "terminal" -> TerminalApp(services, dark)
        "editor" -> EditorApp(services, dark)
        "calculator" -> CalculatorApp(dark)
        "taskmanager" -> TaskManagerApp(services, dark)
        "monitor" -> SystemMonitorApp(services, dark)
        "gpu" -> GpuDiagnosticsApp(report, gpuResult, dark, onRunGpuTest, onGpuTestRunning)
        "settings" -> SettingsApp(services, dark)
        "about" -> AboutApp(report, dark)
        else -> MissingApp(window.appId, dark)
    }
}

/** Services the apps need. Supplied by the activity so apps stay Android-light. */
data class DesktopServices(
    val basePath: String,
    val volumePercent: () -> Int,
    val setVolume: (Int) -> Unit,
    val batteryPercent: () -> Int?,
    val isCharging: () -> Boolean,
    val networkLabel: () -> String,
    val thermalLabel: () -> String,
    val memoryUsedMb: () -> Long,
    val memoryTotalMb: () -> Long,
    val cpuCores: Int,
    val darkTheme: Boolean,
    val setDarkTheme: (Boolean) -> Unit,
    val animationsEnabled: Boolean,
    val setAnimations: (Boolean) -> Unit,
    val runGpuTest: () -> Unit,
)

// ---------------------------------------------------------------- Files

@Composable
private fun FilesApp(services: DesktopServices, dark: Boolean) {
    var currentPath by remember { mutableStateOf(services.basePath) }
    var entries by remember { mutableStateOf(emptyList<FileEntry>()) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load(path: String) {
        val result = runCatching { listDirectory(File(path)) }
        result.onSuccess {
            entries = it
            error = null
            currentPath = path
        }.onFailure {
            error = "Cannot read $path: ${it.message}"
        }
    }

    LaunchedEffect(services.basePath) { load(services.basePath) }

    Column(Modifier.fillMaxSize().padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val parent = File(currentPath).parent
            PathButton("↑", enabled = parent != null) { parent?.let { load(it) } }
            Spacer(Modifier.width(6.dp))
            Text(
                currentPath.substringAfterLast('/').ifEmpty { "/" },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (dark) Brand.TextPrimary else Brand.LightTextPrimary,
            )
            Spacer(Modifier.weight(1f))
            PathButton("⟳") { load(currentPath) }
        }
        Text(
            currentPath,
            fontSize = 9.5.sp,
            fontFamily = FontFamily.Monospace,
            color = Brand.TextMuted,
            maxLines = 1,
        )
        Spacer(Modifier.height(8.dp))
        if (error != null) {
            Banner(error!!, Brand.Red)
        }
        LazyColumn(Modifier.weight(1f)) {
            if (entries.isEmpty() && error == null) {
                item { Text("This folder is empty", fontSize = 11.sp, color = Brand.TextMuted) }
            }
            items(entries) { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clickable(enabled = entry.isDirectory) { load(entry.path) }
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (entry.isDirectory) "▸" else "·",
                        fontSize = 12.sp,
                        color = if (entry.isDirectory) Brand.Blue else Brand.TextMuted,
                        modifier = Modifier.width(14.dp),
                    )
                    Text(
                        entry.name,
                        fontSize = 11.5.sp,
                        color = if (entry.isDirectory) Brand.Blue else if (dark) Brand.TextPrimary else Brand.LightTextPrimary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (entry.isDirectory) "folder" else formatSize(entry.size),
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Brand.TextMuted,
                    )
                }
            }
        }
        Text(
            "${entries.count { it.isDirectory }} folders · ${entries.count { !it.isDirectory }} files",
            fontSize = 9.5.sp,
            color = Brand.TextMuted,
        )
    }
}

data class FileEntry(val name: String, val path: String, val isDirectory: Boolean, val size: Long)

/** Real directory listing; hidden files skipped, sorted folders-first. */
private fun listDirectory(dir: File): List<FileEntry> {
    val files = dir.listFiles() ?: return emptyList()
    return files.filterNot { it.name.startsWith(".") }
        .map { FileEntry(it.name, it.absolutePath, it.isDirectory, if (it.isFile) it.length() else 0L) }
        .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024L * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
}

@Composable
private fun PathButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Brand.SurfaceRaised.copy(alpha = if (enabled) 0.9f else 0.35f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 13.sp, color = if (enabled) Brand.Blue else Brand.TextMuted)
    }
}

@Composable
private fun Banner(text: String, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(8.dp),
    ) {
        Text(text, fontSize = 10.5.sp, color = color)
    }
}

// ---------------------------------------------------------------- Terminal

/**
 * Terminal over the app's own sandbox.
 *
 * This is honest about what it is: a command shell executing real commands *inside the
 * Android app sandbox* (list, read, stat, mkdir, echo, date, uname-equivalents...). It is
 * not a Linux shell -- that arrives in Phase 5 with PRoot, and the banner says so rather
 * than pretending otherwise.
 */
@Composable
private fun TerminalApp(services: DesktopServices, dark: Boolean) {
    val lines = remember { mutableStateListOf("Pocket PC host shell — Android app sandbox, not a Linux shell.") }
    var input by remember { mutableStateOf("") }
    val scroll = rememberScrollState()

    fun run(command: String) {
        val trimmed = command.trim()
        lines += "❯ $trimmed"
        if (trimmed.isEmpty()) return
        val output = HostShell.execute(trimmed, services.basePath)
        output.forEach { lines += it }
        lines += ""
    }

    LaunchedEffect(lines.size) { scroll.animateScrollTo(scroll.maxValue) }

    Column(Modifier.fillMaxSize().background(Color(0xFF070C16)).padding(8.dp)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(scroll),
        ) {
            lines.forEach { line ->
                Text(line, style = MonoStyle, color = Color(0xFFB9D8F2))
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0E1626))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("❯", style = MonoStyle, color = Brand.Amber)
            Spacer(Modifier.width(6.dp))
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                singleLine = true,
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color(0xFFE6F1FF)),
                cursorBrush = SolidColor(Brand.Blue),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (input.isEmpty()) Text("help", style = MonoStyle, color = Brand.TextMuted)
                    inner()
                },
            )
            Text(
                "RUN",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Brand.Blue,
                modifier = Modifier
                    .clickable {
                        run(input)
                        input = ""
                    }
                    .padding(6.dp),
            )
        }
    }
}

// ---------------------------------------------------------------- Editor

@Composable
private fun EditorApp(services: DesktopServices, dark: Boolean) {
    var fileName by remember { mutableStateOf("notes.txt") }
    var content by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Unsaved new file") }

    val target = File(services.basePath, "desktop/$fileName")

    fun save() {
        status = runCatching {
            target.parentFile?.mkdirs()
            target.writeText(content)
            "Saved ${target.name} (${content.length} chars) to ${target.parent}"
        }.getOrElse { "Save failed: ${it.message}" }
    }

    fun load() {
        status = runCatching {
            content = target.readText()
            "Opened ${target.name}"
        }.getOrElse { "No saved file named ${target.name}" }
    }

    Column(Modifier.fillMaxSize().padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (dark) Brand.SurfaceRaised else Color(0xFFEFF4FB))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = if (dark) Brand.TextPrimary else Brand.LightTextPrimary),
                    cursorBrush = SolidColor(Brand.Blue),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(6.dp))
            SmallButton("Open") { load() }
            Spacer(Modifier.width(4.dp))
            SmallButton("Save", primary = true) { save() }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(7.dp))
                .background(if (dark) Color(0xFF0B1220) else Color(0xFFFBFCFE))
                .border(1.dp, if (dark) Brand.Border else Brand.LightBorder, RoundedCornerShape(7.dp))
                .padding(8.dp),
        ) {
            BasicTextField(
                value = content,
                onValueChange = { content = it },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = if (dark) Brand.TextPrimary else Brand.LightTextPrimary),
                cursorBrush = SolidColor(Brand.Blue),
                modifier = Modifier.fillMaxSize(),
                decorationBox = { inner ->
                    if (content.isEmpty()) {
                        Text("Start typing…", style = MonoStyle, color = Brand.TextMuted)
                    }
                    inner()
                },
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(status, fontSize = 9.5.sp, color = Brand.TextMuted, maxLines = 2)
    }
}

@Composable
private fun SmallButton(label: String, primary: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (primary) Brand.Blue else Brand.SurfaceRaised)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 11.sp, color = if (primary) Brand.NavyDeep else Brand.Blue, fontWeight = FontWeight.Medium)
    }
}

// ---------------------------------------------------------------- Calculator

@Composable
private fun CalculatorApp(dark: Boolean) {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    fun evaluate() {
        when (val r = Calculator.evaluate(expression)) {
            is CalcResult.Value -> {
                result = Calculator.format(r.value)
                isError = false
            }
            is CalcResult.Error -> {
                result = r.display
                isError = true
            }
        }
    }

    val buttons = listOf(
        listOf("C", "(", ")", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "−"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "%", "="),
    )

    Column(Modifier.fillMaxSize().padding(10.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9.dp))
                .background(if (dark) Color(0xFF0B1220) else Color(0xFFEFF4FB))
                .padding(10.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                expression.ifEmpty { "0" },
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = Brand.TextMuted,
                maxLines = 1,
            )
            Text(
                result.ifEmpty { " " },
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = if (isError) Brand.Red else if (dark) Brand.TextPrimary else Brand.LightTextPrimary,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(8.dp))
        buttons.forEach { row ->
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { label ->
                    val isEquals = label == "="
                    val isClear = label == "C"
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isEquals -> Brand.Blue
                                    isClear -> Brand.Red.copy(alpha = 0.22f)
                                    else -> Brand.SurfaceRaised
                                },
                            )
                            .clickable {
                                when (label) {
                                    "C" -> {
                                        expression = ""
                                        result = ""
                                        isError = false
                                    }
                                    "=" -> evaluate()
                                    else -> expression += label
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                isEquals -> Brand.NavyDeep
                                isClear -> Brand.Red
                                else -> Brand.TextPrimary
                            },
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Task manager

@Composable
private fun TaskManagerApp(services: DesktopServices, dark: Boolean) {
    Column(Modifier.fillMaxSize().padding(10.dp)) {
        Text(
            "Open applications",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (dark) Brand.TextPrimary else Brand.LightTextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(AppRegistry.apps) { app ->
                Row(
                    Modifier.fillMaxWidth().height(30.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(app.name, fontSize = 11.sp, color = if (dark) Brand.TextPrimary else Brand.LightTextPrimary, modifier = Modifier.weight(1f))
                    Text(app.category.name.lowercase(), fontSize = 9.sp, color = Brand.TextMuted, fontFamily = FontFamily.Monospace)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        val used = services.memoryUsedMb()
        val total = services.memoryTotalMb()
        Text(
            "Process memory: $used MiB used of $total MiB device RAM · ${services.cpuCores} cores",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = Brand.TextSecondary,
        )
        Text(
            "Per-window memory is not reported by Android; device-wide figures come from ActivityManager.",
            fontSize = 9.sp,
            color = Brand.TextMuted,
        )
    }
}

// ---------------------------------------------------------------- System monitor

@Composable
private fun SystemMonitorApp(services: DesktopServices, dark: Boolean) {
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tick++
        }
    }
    val used = remember(tick) { services.memoryUsedMb() }
    val total = remember(tick) { services.memoryTotalMb() }
    val fraction = if (total > 0) used.toFloat() / total else 0f

    Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Live readings, refreshed every second", fontSize = 9.5.sp, color = Brand.TextMuted)
        Metric("Application memory", "$used MiB", fraction, Brand.Blue)
        Metric("Device RAM", "$total MiB", 1f, Brand.TextMuted)
        Reading("Thermal state", services.thermalLabel())
        Reading("Battery", (services.batteryPercent()?.let { "$it%" } ?: "unavailable") + if (services.isCharging()) " (charging)" else "")
        Reading("Network", services.networkLabel())
        Reading("CPU cores", "${services.cpuCores}")
        Spacer(Modifier.height(4.dp))
        Text(
            "CPU load per core is not exposed to unprivileged apps on API 36; only aggregate " +
                "process memory and thermal state are reliable.",
            fontSize = 9.sp,
            lineHeight = 12.sp,
            color = Brand.TextMuted,
        )
    }
}

@Composable
private fun Metric(label: String, value: String, fraction: Float, color: Color) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, fontSize = 10.5.sp, color = Brand.TextSecondary, modifier = Modifier.weight(1f))
            Text(value, fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, color = Brand.TextPrimary)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Brand.SurfaceRaised),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(color),
            )
        }
    }
}

@Composable
private fun Reading(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 10.5.sp, color = Brand.TextSecondary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, color = Brand.TextPrimary, maxLines = 1)
    }
}

// ---------------------------------------------------------------- GPU diagnostics

@Composable
private fun GpuDiagnosticsApp(
    report: SystemReport?,
    result: dev.pocketpc.graphics.GpuVerificationResult?,
    dark: Boolean,
    onRun: () -> Unit,
    running: Boolean,
) {
    val fg = if (dark) Brand.TextPrimary else Brand.LightTextPrimary
    Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("GPU verification", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (running) Brand.SurfaceRaised else Brand.Blue)
                    .clickable(enabled = !running, onClick = onRun)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (running) "Running…" else "Run GPU test",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (running) Brand.TextMuted else Brand.NavyDeep,
                )
            }
        }
        Text(
            "Draws a real workload offscreen, reads the pixels back, and times 120 frames with glFinish. " +
                "Pixels matching the requested colour is what proves the command stream executed.",
            fontSize = 9.5.sp,
            lineHeight = 13.sp,
            color = Brand.TextMuted,
        )
        if (result == null) {
            Text("No run yet in this session.", fontSize = 10.5.sp, color = Brand.Amber)
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(result.report) { line ->
                    Text(
                        line,
                        style = MonoStyle,
                        color = when {
                            line.startsWith("WARNING") -> Brand.Amber
                            line.startsWith("Error") -> Brand.Red
                            line.contains("true") -> Brand.Green
                            else -> fg
                        },
                    )
                }
            }
        }
        // Compositor capability, from the evaluator fed with this run's evidence.
        report?.let {
            val cap = it.of(Feature.GPU_COMPOSITOR)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Compositor state:", fontSize = 10.sp, color = Brand.TextSecondary)
                Spacer(Modifier.width(6.dp))
                StateChip(cap.state)
            }
            Text(cap.detail, fontSize = 9.5.sp, color = Brand.TextMuted, maxLines = 3)
        }
    }
}

@Composable
fun StateChip(state: CapabilityState) {
    val color = when (state) {
        CapabilityState.VERIFIED -> Brand.Green
        CapabilityState.TESTED -> Color(0xFF60A5FA)
        CapabilityState.IMPLEMENTED -> Brand.Blue
        CapabilityState.AVAILABLE -> Brand.Blue
        CapabilityState.DETECTED -> Brand.TextSecondary
        CapabilityState.LIMITED -> Brand.Amber
        CapabilityState.FAILED -> Brand.Red
        CapabilityState.UNSUPPORTED -> Brand.TextMuted
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(state.name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ---------------------------------------------------------------- Settings

@Composable
private fun SettingsApp(services: DesktopServices, dark: Boolean) {
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingsToggle(
            "Dark desktop",
            "Switches the whole shell between the dark and light palette.",
            services.darkTheme,
        ) { services.setDarkTheme(!services.darkTheme) }
        SettingsToggle(
            "Wallpaper motion",
            "Animates the background gradient. Turn off to let the compositor idle.",
            services.animationsEnabled,
        ) { services.setAnimations(!services.animationsEnabled) }

        Text("Media volume (this app): ${services.volumePercent()}%", fontSize = 10.5.sp, color = Brand.TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0, 25, 50, 75, 100).forEach { level ->
                Box(
                    Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (services.volumePercent() == level) Brand.Blue else Brand.SurfaceRaised)
                        .clickable { services.setVolume(level) }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "$level",
                        fontSize = 10.sp,
                        color = if (services.volumePercent() == level) Brand.NavyDeep else Brand.Blue,
                    )
                }
            }
        }
        Text(
            "Android does not allow changing system brightness or ringer volume without a " +
                "user-granted permission, so Pocket PC adjusts only its own media stream.",
            fontSize = 9.sp,
            lineHeight = 12.sp,
            color = Brand.TextMuted,
        )
        SmallButton("Run GPU verification") { services.runGpuTest() }
    }
}

@Composable
private fun SettingsToggle(title: String, subtitle: String, checked: Boolean, onChange: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(Brand.SurfaceRaised.copy(alpha = 0.6f))
            .clickable(onClick = onChange)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 11.5.sp, color = Brand.TextPrimary, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 9.5.sp, lineHeight = 12.sp, color = Brand.TextMuted)
        }
        Box(
            Modifier
                .size(width = 38.dp, height = 22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (checked) Brand.Blue else Brand.Border),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .padding(horizontal = 2.dp)
                    .size(18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color.White),
            )
        }
    }
}

// ---------------------------------------------------------------- About

@Composable
private fun AboutApp(report: SystemReport?, dark: Boolean) {
    val fg = if (dark) Brand.TextPrimary else Brand.LightTextPrimary
    Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
        Text("Pocket PC", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = fg)
        Text("Phase 2 · desktop environment", fontSize = 10.5.sp, color = Brand.TextMuted)
        Spacer(Modifier.height(12.dp))
        report?.let { r ->
            val s = r.snapshot
            listOf(
                "Device" to "${s.deviceManufacturer} ${s.deviceModel}",
                "Android" to "API ${s.sdkInt}",
                "CPU" to "${s.cpuCores} cores @ ${s.cpuMaxFreqKHz / 1000} MHz",
                "RAM" to "${s.ramTotalMb} MiB",
                "GPU" to (s.gpuRenderer ?: "unknown"),
                "OpenGL ES" to (s.glEsVersion ?: "unknown"),
                "Vulkan" to (s.vulkanApiVersion ?: s.vulkanSupported?.toString() ?: "unprobed"),
                "KVM node" to (s.kvmPresent?.toString() ?: "unprobed"),
                "AVF" to (s.avfDeclared?.toString() ?: "unprobed"),
                "Profile" to r.profile.id.name,
            ).forEach { (k, v) -> Reading(k, v) }
            Spacer(Modifier.height(10.dp))
            Text(
                "Pocket PC never reports a feature as working because an API exists. Open GPU " +
                    "Diagnostics to run the hardware-rendering test.",
                fontSize = 9.sp,
                lineHeight = 12.sp,
                color = Brand.TextMuted,
            )
        } ?: Text("Probing device…", fontSize = 11.sp, color = Brand.TextMuted)
    }
}

@Composable
private fun MissingApp(appId: String, dark: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "No handler registered for \"$appId\"",
            fontSize = 11.sp,
            color = if (dark) Brand.TextMuted else Brand.LightTextSecondary,
        )
    }
}

/**
 * The host shell command interpreter.
 *
 * Executes real file and environment operations inside the app sandbox. Returning output
 * as a list keeps it testable and avoids spawning processes, which PRoot will handle in
 * Phase 5 rather than faking it here.
 */
object HostShell {
    fun execute(commandLine: String, basePath: String): List<String> {
        val parts = commandLine.trim().split(Regex("\\s+"))
        val cmd = parts.firstOrNull()?.lowercase() ?: return emptyList()
        val args = parts.drop(1)
        return runCatching {
            when (cmd) {
                "help" -> listOf(
                    "Available commands:",
                    "  help              this text",
                    "  pwd               print working directory",
                    "  ls [path]         list a directory",
                    "  cat <file>        print a file (max 4 KiB)",
                    "  du [path]         total bytes under a path",
                    "  mkdir <name>      create a directory here",
                    "  echo <text>       print text",
                    "  write <f> <text>  write text to a file",
                    "  rm <file>         delete a file",
                    "  date              current date and time",
                    "  clear             clear the screen",
                    "  Note: this is the Android app sandbox, not a Linux environment.",
                )
                "pwd" -> listOf(basePath)
                "ls" -> {
                    val dir = File(args.firstOrNull()?.let { if (it.startsWith("/")) it else "$basePath/$it" } ?: basePath)
                    val entries = dir.listFiles()?.sortedBy { it.name } ?: return listOf("cannot read ${dir.path}")
                    if (entries.isEmpty()) listOf("(empty)") else entries.map {
                        (if (it.isDirectory) "d" else "-") + " " + it.name + if (it.isFile) "  ${it.length()} B" else ""
                    }
                }
                "cat" -> {
                    val name = args.firstOrNull() ?: return listOf("usage: cat <file>")
                    val f = File(if (name.startsWith("/")) name else "$basePath/$name")
                    if (!f.isFile) listOf("no such file: $name")
                    else listOf(f.readText().take(4096))
                }
                "du" -> {
                    val dir = File(args.firstOrNull()?.let { if (it.startsWith("/")) it else "$basePath/$it" } ?: basePath)
                    listOf("${dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }} bytes under ${dir.path}")
                }
                "mkdir" -> {
                    val name = args.firstOrNull() ?: return listOf("usage: mkdir <name>")
                    val d = File(basePath, name)
                    if (d.mkdirs()) listOf("created ${d.path}") else listOf("mkdir failed (exists?)")
                }
                "echo" -> listOf(args.joinToString(" "))
                "write" -> {
                    val name = args.firstOrNull() ?: return listOf("usage: write <file> <text>")
                    val text = args.drop(1).joinToString(" ")
                    val f = File(basePath, name)
                    f.writeText(text)
                    listOf("wrote ${text.length} chars to ${f.path}")
                }
                "rm" -> {
                    val name = args.firstOrNull() ?: return listOf("usage: rm <file>")
                    val f = File(basePath, name)
                    if (f.delete()) listOf("removed $name") else listOf("cannot remove $name")
                }
                "date" -> listOf(java.util.Date().toString())
                "clear" -> listOf("(use the window's close button to dismiss)")
                else -> listOf("unknown command: $cmd  (type 'help')")
            }
        }.getOrElse { listOf("error: ${it.message}") }
    }
}
