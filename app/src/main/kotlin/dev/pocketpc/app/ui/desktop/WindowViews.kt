package dev.pocketpc.app.ui.desktop

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pocketpc.app.ui.icons.AppIcons
import dev.pocketpc.app.ui.theme.Brand
import dev.pocketpc.desktop.apps.AppDefinition
import dev.pocketpc.desktop.wm.DesktopWindow
import dev.pocketpc.desktop.wm.OffsetDp
import dev.pocketpc.desktop.wm.WindowId
import dev.pocketpc.desktop.wm.WindowManager
import dev.pocketpc.desktop.wm.WindowRect
import dev.pocketpc.desktop.wm.WindowState
import kotlin.math.roundToInt

/**
 * Renders one real window: title bar with working controls, content area, drag, resize
 * and edge-to-edge focus handling.
 *
 * All geometry decisions live in [WindowManager]; this composable only forwards pointer
 * input and draws the result, so the tested behaviour and the rendered behaviour cannot
 * diverge.
 */
@Composable
fun DesktopWindowView(
    window: DesktopWindow,
    focused: Boolean,
    wm: WindowManager,
    dark: Boolean,
    onContent: @Composable (DesktopWindow) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val animatedElevation by animateFloatAsState(
        targetValue = if (focused) 18f else 6f,
        label = "window-elevation",
    )
    val surface = if (dark) Brand.Surface else Color.White
    val border = when {
        !focused -> if (dark) Brand.Border.copy(alpha = 0.5f) else Brand.LightBorder
        dark -> Brand.Blue.copy(alpha = 0.55f)
        else -> Brand.BlueDeep
    }

    Box(
        modifier = modifier
            .offset { IntOffset(window.bounds.x.dp.toPx().roundToInt(), window.bounds.y.dp.toPx().roundToInt()) }
            .size(window.bounds.width.dp, window.bounds.height.dp)
            .shadow(animatedElevation.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(surface)
            .border(BorderStroke(1.dp, border), RoundedCornerShape(10.dp))
            .pointerInput(window.id, window.bounds) {
                detectTapGestures(onPress = { wm.focus(window.id) })
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            TitleBar(window = window, focused = focused, wm = wm, dark = dark)
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(if (dark) Brand.NavyDeep.copy(alpha = 0.35f) else Color(0xFFFBFCFE)),
            ) {
                onContent(window)
            }
        }

        // Resize handle: a generous 24dp touch target in the bottom-right corner.
        if (window.resizable) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(24.dp)
                    .pointerInput(window.id) {
                        detectDragGestures(
                            onDragStart = { wm.beginResize(window.id) },
                            onDragEnd = { wm.endResize(window.id) },
                            onDragCancel = { wm.endResize(window.id) },
                        ) { change, dragAmount ->
                            change.consume()
                            val b = wm.window(window.id)?.bounds ?: return@detectDragGestures
                            wm.resize(
                                window.id,
                                OffsetDp(b.right + dragAmount.x / density.density, b.bottom + dragAmount.y / density.density),
                            )
                        }
                    },
            ) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .padding(2.dp)
                        .background(border.copy(alpha = 0.45f), RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

@Composable
private fun TitleBar(window: DesktopWindow, focused: Boolean, wm: WindowManager, dark: Boolean) {
    val density = LocalDensity.current
    val barColor = when {
        focused && dark -> Brand.SurfaceRaised
        focused -> Color(0xFFEAF1FB)
        dark -> Brand.Surface.copy(alpha = 0.75f)
        else -> Color(0xFFF0F3F8)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(barColor)
            .pointerInput(window.id) {
                detectDragGestures(
                    onDragStart = { offset ->
                        wm.beginDrag(window.id, OffsetDp(offset.x / density.density, offset.y / density.density))
                    },
                    onDragEnd = { wm.endDrag(window.id) },
                    onDragCancel = { wm.endDrag(window.id) },
                ) { change, dragAmount ->
                    change.consume()
                    val b = wm.window(window.id)?.bounds ?: return@detectDragGestures
                    wm.drag(
                        window.id,
                        OffsetDp(b.x + dragAmount.x / density.density, b.y + dragAmount.y / density.density),
                    )
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconBadge(window.appId, size = 18)
        Text(
            text = window.title,
            fontSize = 12.5.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
            color = if (dark) Brand.TextPrimary else Brand.LightTextPrimary,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, end = 6.dp),
        )
        WindowButton(AppIcons.minimize, "Minimise ${window.title}") { wm.minimize(window.id) }
        WindowButton(
            if (window.state == WindowState.MAXIMIZED) AppIcons.restore else AppIcons.maximize,
            if (window.state == WindowState.MAXIMIZED) "Restore ${window.title}" else "Maximise ${window.title}",
        ) { wm.toggleMaximize(window.id) }
        WindowButton(AppIcons.close, "Close ${window.title}", danger = true) { wm.close(window.id) }
    }
}

@Composable
private fun WindowButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clickable(onClick = onClick)
            .padding(11.dp),
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (danger) Brand.Red.copy(alpha = 0.9f) else Brand.TextSecondary,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Small rounded app tile used in title bars, the taskbar and the Start menu. */
@Composable
fun AppIconBadge(appId: String, size: Int = 22, tint: Color = Brand.Blue) {
    val app = dev.pocketpc.desktop.apps.AppRegistry.find(appId)
    val icon = AppIcons.forKey(app?.iconKey ?: "info")
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 5).dp))
            .background(tint.copy(alpha = 0.22f))
            .padding((size / 5).dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = app?.name ?: appId,
            tint = tint,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
