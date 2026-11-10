package dev.pocketpc.desktop.wm

/** Screen-space geometry in dp. Immutable value type so window state is snapshot-able. */
public data class WindowRect(
    public val x: Float,
    public val y: Float,
    public val width: Float,
    public val height: Float,
) {
    public val right: Float get() = x + width
    public val bottom: Float get() = y + height

    public fun contains(px: Float, py: Float): Boolean =
        px >= x && px <= right && py >= y && py <= bottom

    public fun movedBy(dx: Float, dy: Float): WindowRect = copy(x = x + dx, y = y + dy)

    public companion object {
        /** Smallest window a user may resize to; below this controls stop being tappable. */
        public const val MIN_WIDTH: Float = 220f
        public const val MIN_HEIGHT: Float = 160f
    }
}

/** The area windows may occupy: the screen minus reserved system chrome. */
public data class Viewport(
    public val width: Float,
    public val height: Float,
    /** Height reserved at the top (status bar). */
    public val topInset: Float = 0f,
    /** Height reserved at the bottom (taskbar). */
    public val bottomInset: Float = 0f,
) {
    /** Top edge available to windows (including their title bars). */
    public val usableTop: Float get() = topInset

    /** Bottom edge available to windows. */
    public val usableBottom: Float get() = (height - bottomInset).coerceAtLeast(topInset + 1f)

    public val usableHeight: Float get() = (usableBottom - usableTop).coerceAtLeast(1f)

    public val usableWidth: Float get() = width.coerceAtLeast(1f)

    /**
     * Keeps a window fully reachable: its title bar can never be dragged off-screen,
     * which would otherwise strand the window with no way to grab it.
     */
    public fun constrain(r: WindowRect): WindowRect {
        val w = r.width.coerceIn(WindowRect.MIN_WIDTH, usableWidth)
        val h = r.height.coerceIn(WindowRect.MIN_HEIGHT, usableHeight)
        val maxX = (usableWidth - w).coerceAtLeast(0f)
        val maxY = (usableBottom - h).coerceAtLeast(usableTop)
        return r.copy(
            x = r.x.coerceIn(0f, maxX),
            y = r.y.coerceIn(usableTop, maxY),
            width = w,
            height = h,
        )
    }
}

/** Screen edges a drag can snap to. */
public enum class SnapEdge { NONE, LEFT, RIGHT, TOP, BOTTOM }

/**
 * Edge-snapping geometry.
 *
 * Thresholds are in dp and expressed for touch: 24dp is roughly a finger's slop, so a
 * window snaps only when the user clearly means it.
 */
public object Snapping {
    public const val THRESHOLD_DP: Float = 24f

    /** Which edge (if any) the window's rectangle is close enough to snap against. */
    public fun edgeFor(r: WindowRect, viewport: Viewport): SnapEdge = when {
        r.x <= THRESHOLD_DP -> SnapEdge.LEFT
        r.right >= viewport.usableWidth - THRESHOLD_DP -> SnapEdge.RIGHT
        r.y <= viewport.usableTop + THRESHOLD_DP -> SnapEdge.TOP
        r.bottom >= viewport.usableBottom - THRESHOLD_DP -> SnapEdge.BOTTOM
        else -> SnapEdge.NONE
    }

    /**
     * Half-screen snap: LEFT/RIGHT fill half the usable width full-height; TOP maximises.
     * Returns null when the window is not near an edge.
     */
    public fun snapped(r: WindowRect, viewport: Viewport): WindowRect? {
        val halfW = viewport.usableWidth / 2f
        return when (edgeFor(r, viewport)) {
            SnapEdge.LEFT -> WindowRect(0f, viewport.usableTop, halfW, viewport.usableHeight)
            SnapEdge.RIGHT -> WindowRect(
                halfW,
                viewport.usableTop,
                viewport.usableWidth - halfW,
                viewport.usableHeight,
            )
            SnapEdge.TOP, SnapEdge.BOTTOM -> null
            SnapEdge.NONE -> null
        }
    }

    /** What a maximise action should produce. */
    public fun maximized(viewport: Viewport): WindowRect =
        WindowRect(0f, viewport.usableTop, viewport.usableWidth, viewport.usableHeight)
}
