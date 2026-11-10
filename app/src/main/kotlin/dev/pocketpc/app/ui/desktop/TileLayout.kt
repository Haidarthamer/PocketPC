package dev.pocketpc.app.ui.desktop

import dev.pocketpc.desktop.wm.WindowManager
import dev.pocketpc.desktop.wm.WindowRect

/**
 * Tiling layout for open windows.
 *
 * One window → full width, full height. Two → side by side. More → a grid in which the
 * windows are arranged left-to-right, top-to-bottom, with the last row sharing the
 * remaining height.
 *
 * Implemented against [WindowManager]'s public geometry so the result is the same
 * whether triggered from the context menu or a keyboard shortcut.
 */
object TileLayout {

    fun tile(wm: WindowManager) {
        val windows = wm.visibleWindows
        if (windows.isEmpty()) return
        val viewport = wm.viewport
        val count = windows.size

        val columns: Int
        val rows: Int
        when {
            count == 1 -> { columns = 1; rows = 1 }
            count == 2 -> { columns = 2; rows = 1 }
            count <= 4 -> { columns = 2; rows = 2 }
            else -> { columns = 3; rows = (count + 2) / 3 }
        }

        val cellW = viewport.usableWidth / columns
        val cellH = viewport.usableHeight / rows

        windows.forEachIndexed { index, window ->
            val col = index % columns
            val row = index / columns
            wm.applyBounds(
                window.id,
                WindowRect(
                    x = col * cellW,
                    y = viewport.usableTop + row * cellH,
                    width = cellW,
                    height = cellH,
                ),
            )
        }
    }
}
