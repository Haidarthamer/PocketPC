package dev.pocketpc.app.ui.compositor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.pocketpc.app.ui.theme.Brand
import kotlin.math.abs
import kotlin.math.sin

/**
 * Live composition statistics sampled from the real frame clock.
 *
 * This measures the *Android desktop* pipeline: how long the compositor takes between
 * produced frames while windows are being drawn and moved. It is deliberately not called
 * "GPU acceleration" — whether these frames are served by hardware is answered only by
 * the EGL workload in GpuVerificationEngine, which reads pixels back from the driver.
 */
@Stable
class CompositionStats {
    var frames by mutableStateOf(0L)
        private set
    var avgFrameMs by mutableStateOf(0.0)
        private set
    var worstFrameMs by mutableStateOf(0.0)
        private set
    /** Rolling FPS over the last second of frames. */
    var fps by mutableStateOf(0.0)
        private set

    private var windowStartNanos = 0L
    private var framesInWindow = 0
    private var smoothedMs = 0.0

    internal fun onFrame(nanos: Long) {
        if (windowStartNanos == 0L) {
            windowStartNanos = nanos
            return
        }
        val deltaMs = (nanos - lastNanos) / 1_000_000.0
        lastNanos = nanos
        if (deltaMs <= 0.0) return
        frames++
        smoothedMs = if (smoothedMs == 0.0) deltaMs else smoothedMs * 0.9 + deltaMs * 0.1
        avgFrameMs = smoothedMs
        if (deltaMs > worstFrameMs) worstFrameMs = deltaMs
        framesInWindow++
        val elapsed = (nanos - windowStartNanos) / 1_000_000_000.0
        if (elapsed >= 1.0) {
            fps = framesInWindow / elapsed
            framesInWindow = 0
            windowStartNanos = nanos
        }
    }

    private var lastNanos = 0L

    internal fun reset() {
        frames = 0; avgFrameMs = 0.0; worstFrameMs = 0.0; fps = 0.0
        windowStartNanos = 0L; framesInWindow = 0; smoothedMs = 0.0; lastNanos = 0L
    }
}

/**
 * The desktop wallpaper layer.
 *
 * Drawn with Compose's Canvas, which the platform renders through HWUI onto the GPU.
 * The subtle animated glow is intentional but bounded: it stops when
 * [animate] is false so the compositor can idle and not burn battery.
 */
@Composable
fun Wallpaper(
    dark: Boolean,
    animate: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = Brand.Blue,
) {
    var phase by remember { mutableStateOf(0f) }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        while (true) {
            withFrameNanos { }
            phase = (phase + 0.004f) % 1f
        }
    }
    val top = if (dark) Brand.Navy else Color(0xFFDCE7F7)
    val bottom = if (dark) Brand.NavyDeep else Color(0xFFF4F7FC)
    Canvas(modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(top, bottom)))
        // Two soft radial glows give depth without a bitmap asset.
        drawGlow(
            center = Offset(size.width * 0.18f, size.height * (0.24f + 0.015f * sin(phase * 6.28f))),
            radius = size.minDimension * 0.62f,
            color = accent.copy(alpha = if (dark) 0.16f else 0.10f),
        )
        drawGlow(
            center = Offset(size.width * 0.86f, size.height * (0.78f + 0.02f * sin(phase * 4.2f + 1.3f))),
            radius = size.minDimension * 0.55f,
            color = Brand.Amber.copy(alpha = if (dark) 0.09f else 0.07f),
        )
        // Faint dot grid, a desktop-wallpaper convention that also helps judge motion.
        val step = 34f * density
        val dotColor = if (dark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)
        var y = step
        while (y < size.height) {
            var x = step
            while (x < size.width) {
                drawCircle(dotColor, radius = 1.1f * density, center = Offset(x, y))
                x += step
            }
            y += step
        }
    }
}

private fun DrawScope.drawGlow(center: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/** Samples the frame clock and feeds [CompositionStats]. Call once, high in the tree. */
@Composable
fun FrameInstrumentation(stats: CompositionStats, enabled: Boolean = true) {
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        while (true) {
            withFrameNanos { stats.onFrame(it) }
        }
    }
    DisposableEffect(Unit) {
        onDispose { stats.reset() }
    }
}

/** Utility used by window animation code to keep bouncy motion readable. */
internal fun easingProgress(t: Float): Float {
    val clamped = t.coerceIn(0f, 1f)
    return 1f - abs(1f - clamped) * abs(1f - clamped)
}

/** Empty box placeholder kept so callers can compose layers uniformly. */
@Composable
fun Layer(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) { content() }
}
