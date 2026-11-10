package dev.pocketpc.app.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pocketpc.app.ui.theme.Brand
import dev.pocketpc.desktop.apps.AppDefinition
import dev.pocketpc.desktop.apps.AppRegistry
import dev.pocketpc.desktop.wm.OffsetDp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt

/**
 * Desktop icons with drag-to-reposition, double-tap/single-tap launch and a long-press
 * context menu.
 *
 * Positions are stored per app id in dp so they survive rotation; a layout pass clamps
 * them into the desktop area, which keeps icons from being stranded off-screen.
 */
class IconPositions {
    private val map = mutableMapOf<String, OffsetDp>()

    fun get(appId: String, default: OffsetDp): OffsetDp = map[appId] ?: default

    fun set(appId: String, position: OffsetDp) {
        map[appId] = position
    }

    fun all(): Map<String, OffsetDp> = map.toMap()

    /** Clamps every stored position into the visible desktop rectangle. */
    fun clampInto(width: Float, height: Float, iconSize: Float) {
        map.replaceAll { _, p ->
            OffsetDp(
                p.x.coerceIn(0f, (width - iconSize).coerceAtLeast(0f)),
                p.y.coerceIn(0f, (height - iconSize).coerceAtLeast(0f)),
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DesktopIcon(
    app: AppDefinition,
    position: OffsetDp,
    dark: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
    onLaunch: () -> Unit,
    onMove: (OffsetDp) -> Unit,
    onContextMenu: () -> Unit,
    onLongPress: () -> Unit,
) {
    val density = LocalDensity.current
    val bg = if (selected) Brand.Blue.copy(alpha = if (dark) 0.28f else 0.20f) else Color.Transparent
    val label = if (dark) Brand.TextPrimary else Brand.LightTextPrimary

    Column(
        modifier = Modifier
            .offset { IntOffsetPx(position.x, position.y, density.density) }
            .width(82.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .pointerInput(app.id) {
                detectTapGestures(
                    onTap = {
                        if (selected) onLaunch() else onSelect()
                    },
                    onLongPress = { onLongPress() },
                )
            }
            .pointerInput(app.id) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onMove(
                        OffsetDp(
                            position.x + dragAmount.x / density.density,
                            position.y + dragAmount.y / density.density,
                        ),
                    )
                }
            }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    Color.White.copy(alpha = if (dark) 0.09f else 0.9f),
                )
                .border(1.dp, Brand.Blue.copy(alpha = 0.20f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            AppIconBadge(app.id, size = 28, tint = if (dark) Brand.Blue else Brand.BlueDeep)
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = app.name,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            color = label,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/** Translates dp into a pixel IntOffset for `Modifier.offset`. */
private fun IntOffsetPx(xDp: Float, yDp: Float, density: Float) =
    androidx.compose.ui.unit.IntOffset((xDp * density).roundToInt(), (yDp * density).roundToInt())

/** A real context menu: actions act on the desktop, not on a decoration. */
@Composable
fun DesktopContextMenu(
    position: OffsetDp,
    dark: Boolean,
    canPaste: Boolean,
    onOpenTerminal: () -> Unit,
    onOpenFiles: () -> Unit,
    onTileWindows: () -> Unit,
    onToggleWallpaperAnimation: () -> Unit,
    onShowShortcuts: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val surface = if (dark) Brand.Surface else Color.White
    val border = if (dark) Brand.Border else Brand.LightBorder
    val fg = if (dark) Brand.TextPrimary else Brand.LightTextPrimary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
    ) {
        Column(
            modifier = Modifier
                .offset { IntOffsetPx(position.x, position.y, 1f) }
                .widthIn(min = 190.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(surface)
                .border(1.dp, border, RoundedCornerShape(10.dp))
                .padding(vertical = 5.dp)
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            MenuItem("Open Terminal", fg) { onOpenTerminal(); onDismiss() }
            MenuItem("Open Files", fg) { onOpenFiles(); onDismiss() }
            MenuDivider(border)
            MenuItem("Tile all windows", fg) { onTileWindows(); onDismiss() }
            MenuItem("Toggle wallpaper motion", fg) { onToggleWallpaperAnimation(); onDismiss() }
            MenuDivider(border)
            MenuItem("Keyboard shortcuts", fg) { onShowShortcuts(); onDismiss() }
            MenuItem("Desktop settings", fg) { onOpenSettings(); onDismiss() }
            if (canPaste) {
                MenuDivider(border)
                MenuItem("Paste", fg) { onDismiss() }
            }
        }
    }
}

@Composable
private fun MenuItem(label: String, color: Color, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 12.sp,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

@Composable
private fun MenuDivider(color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(1.dp)
            .background(color.copy(alpha = 0.5f)),
    )
}

/** The empty-desktop hint shown when no windows are open. */
@Composable
fun DesktopEmptyHint(dark: Boolean) {
    val fg = if (dark) Brand.TextSecondary else Brand.LightTextSecondary
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Pocket PC", fontSize = 26.sp, fontWeight = FontWeight.Light, color = fg.copy(alpha = 0.75f))
        Spacer(Modifier.height(6.dp))
        Text(
            "Tap Start or an icon to open an application",
            fontSize = 11.5.sp,
            color = fg.copy(alpha = 0.6f),
        )
    }
}
