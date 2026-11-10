package dev.pocketpc.desktop.wm

/**
 * The window manager as a pure state machine.
 *
 * Deliberately free of Android types: every interaction the UI layer performs (drag,
 * resize, focus, minimise, restore, close) is a function here, and every rule is
 * covered by unit tests. The Compose layer only renders this state and forwards
 * pointer deltas, so the behaviour cannot drift from the tested behaviour.
 *
 * Invariants held after every operation:
 *  - windows never overlap the reserved chrome (top inset / taskbar),
 *  - at most one window is focused, and the focused window is always the topmost
 *    visible window,
 *  - z-indices are a dense 0..n-1 range with no duplicates,
 *  - a hidden (minimised) window is never focused.
 */
public class WindowManager(
    private val viewportProvider: () -> Viewport,
) {
    private val _windows = linkedMapOf<WindowId, DesktopWindow>()
    private var nextZ = 0
    private var focusedId: WindowId? = null

    public val viewport: Viewport get() = viewportProvider()

    /** Windows in z-order, bottom first. Includes minimised windows. */
    public val windows: List<DesktopWindow>
        get() = _windows.values.toList()

    /** Windows the compositor should draw, bottom first. */
    public val visibleWindows: List<DesktopWindow>
        get() = _windows.values.filter { it.isVisible }

    public val focused: DesktopWindow?
        get() = focusedId?.let { _windows[it] }

    public fun window(id: WindowId): DesktopWindow? = _windows[id]

    /**
     * Opens a window. If the same app is already open, the existing window is focused
     * instead of a duplicate being stacked on top of it.
     */
    public fun open(
        appId: String,
        title: String,
        bounds: WindowRect? = null,
        resizable: Boolean = true,
        singletonPerApp: Boolean = true,
    ): DesktopWindow {
        if (singletonPerApp) {
            _windows.values.firstOrNull { it.appId == appId }?.let { existing ->
                if (existing.state == WindowState.MINIMIZED) restore(existing.id)
                focus(existing.id)
                return _windows.getValue(existing.id)
            }
        }
        val id = WindowId("$appId#${_windows.size}${nextZ}${title.hashCode().toUInt()}")
        val rect = viewport.constrain(bounds ?: defaultBounds())
        val window = DesktopWindow(
            id = id,
            appId = appId,
            title = title,
            bounds = rect,
            restoreBounds = rect,
            zIndex = nextZ++,
            resizable = resizable,
        )
        _windows[id] = window
        focus(id)
        return window
    }

    /** A cascade-friendly default placement, offset per open window. */
    public fun defaultBounds(): WindowRect {
        val v = viewport
        val step = 28f
        val offset = (_windows.size % 6) * step
        val w = (v.usableWidth * 0.72f).coerceAtLeast(WindowRect.MIN_WIDTH)
        val h = (v.usableHeight * 0.66f).coerceAtLeast(WindowRect.MIN_HEIGHT)
        return WindowRect(x = 24f + offset, y = 24f + offset, width = w, height = h)
    }

    public fun focus(id: WindowId) {
        val target = _windows[id] ?: return
        if (target.state == WindowState.MINIMIZED) restore(id)
        val current = _windows.getValue(id)
        _windows[id] = current.copy(zIndex = nextZ++)
        normalizeZ()
        focusedId = id
    }

    public fun close(id: WindowId) {
        _windows.remove(id)
        if (focusedId == id) {
            focusedId = visibleWindows.lastOrNull()?.id
        }
        normalizeZ()
    }

    public fun minimize(id: WindowId) {
        val w = _windows[id] ?: return
        _windows[id] = w.copy(state = WindowState.MINIMIZED, snappedTo = SnapEdge.NONE)
        if (focusedId == id) focusedId = visibleWindows.lastOrNull()?.id
    }

    /** Returns a minimised window to its previous state, or activates it. */
    public fun restore(id: WindowId) {
        val w = _windows[id] ?: return
        _windows[id] = w.copy(state = WindowState.NORMAL, bounds = viewport.constrain(w.restoreBounds))
        focus(id)
    }

    public fun maximize(id: WindowId) {
        val w = _windows[id] ?: return
        if (w.state == WindowState.MAXIMIZED) {
            unmaximize(id)
            return
        }
        _windows[id] = w.copy(
            state = WindowState.MAXIMIZED,
            restoreBounds = if (w.keepsRestoreBounds) w.restoreBounds else w.bounds,
            bounds = Snapping.maximized(viewport),
            snappedTo = SnapEdge.NONE,
        )
        focus(id)
    }

    public fun unmaximize(id: WindowId) {
        val w = _windows[id] ?: return
        if (w.state != WindowState.MAXIMIZED && w.state != WindowState.FULLSCREEN) return
        _windows[id] = w.copy(
            state = WindowState.NORMAL,
            bounds = viewport.constrain(w.restoreBounds),
            snappedTo = SnapEdge.NONE,
        )
        focus(id)
    }

    public fun toggleMaximize(id: WindowId) = maximize(id)

    public fun setFullscreen(id: WindowId, fullscreen: Boolean) {
        val w = _windows[id] ?: return
        if (fullscreen) {
            _windows[id] = w.copy(
                state = WindowState.FULLSCREEN,
                restoreBounds = if (w.keepsRestoreBounds) w.restoreBounds else w.bounds,
                bounds = WindowRect(0f, 0f, viewport.width, viewport.height),
                snappedTo = SnapEdge.NONE,
            )
        } else {
            unmaximize(id)
            return
        }
        focus(id)
    }

    /** Starts a drag: remembers the grab offset and un-snaps if snapped. */
    public fun beginDrag(id: WindowId, pointer: OffsetDp) {
        val w = _windows[id] ?: return
        focus(id)
        drags[id] = DragSession(
            grabOffset = OffsetDp(pointer.x - w.bounds.x, pointer.y - w.bounds.y),
            startBounds = w.bounds,
        )
    }

    /**
     * Moves a dragged window. Snaps to a screen half when released near an edge; while
     * dragging, the window follows the finger without snapping so it does not jitter.
     */
    public fun drag(id: WindowId, pointer: OffsetDp) {
        val session = drags[id] ?: return
        val w = _windows[id] ?: return
        if (w.state == WindowState.MAXIMIZED || w.state == WindowState.FULLSCREEN) return
        val proposed = w.copy(
            bounds = viewport.constrain(
                WindowRect(
                    x = pointer.x - session.grabOffset.x,
                    y = pointer.y - session.grabOffset.y,
                    width = session.startBounds.width,
                    height = session.startBounds.height,
                ),
            ),
            snappedTo = SnapEdge.NONE,
        )
        _windows[id] = proposed
    }

    /** Ends a drag, applying edge snapping. */
    public fun endDrag(id: WindowId) {
        val session = drags.remove(id) ?: return
        val w = _windows[id] ?: return
        val edge = Snapping.edgeFor(w.bounds, viewport)
        val snapped = Snapping.snapped(w.bounds, viewport)
        if (edge != SnapEdge.TOP && edge != SnapEdge.BOTTOM && snapped != null) {
            _windows[id] = w.copy(
                bounds = snapped,
                restoreBounds = session.startBounds,
                snappedTo = edge,
            )
        }
    }

    public fun beginResize(id: WindowId) {
        val w = _windows[id] ?: return
        focus(id)
        resizes[id] = ResizeSession(startBounds = w.bounds)
    }

    /** Resizes from the bottom-right handle; clamps to the minimum size and viewport. */
    public fun resize(id: WindowId, pointer: OffsetDp) {
        val session = resizes[id] ?: return
        val w = _windows[id] ?: return
        if (!w.resizable) return
        val newRect = WindowRect(
            x = session.startBounds.x,
            y = session.startBounds.y,
            width = (pointer.x - session.startBounds.x).coerceAtLeast(WindowRect.MIN_WIDTH),
            height = (pointer.y - session.startBounds.y).coerceAtLeast(WindowRect.MIN_HEIGHT),
        )
        _windows[id] = w.copy(
            bounds = viewport.constrain(newRect),
            snappedTo = SnapEdge.NONE,
            state = if (w.state == WindowState.MAXIMIZED) WindowState.NORMAL else w.state,
        )
    }

    public fun endResize(id: WindowId) {
        resizes.remove(id)
        val w = _windows[id] ?: return
        _windows[id] = w.copy(restoreBounds = w.bounds)
    }

    /**
     * Applies an externally computed geometry (used by the tiling layout and keyboard
     * snapping). Bounds are still constrained to the viewport, so a caller cannot place a
     * window off-screen.
     */
    public fun applyBounds(id: WindowId, bounds: WindowRect) {
        val w = _windows[id] ?: return
        _windows[id] = w.copy(
            bounds = viewport.constrain(bounds),
            restoreBounds = if (w.keepsRestoreBounds) w.restoreBounds else w.bounds,
            state = if (w.state == WindowState.MINIMIZED) WindowState.NORMAL else w.state,
            snappedTo = SnapEdge.NONE,
        )
    }

    /** Called on rotation / window-size change: keeps windows inside the new viewport. */
    public fun onViewportChanged() {
        _windows.entries.toList().forEach { (id, w) ->
            val newBounds = if (w.state == WindowState.MAXIMIZED) {
                Snapping.maximized(viewport)
            } else {
                viewport.constrain(w.bounds)
            }
            _windows[id] = w.copy(bounds = newBounds)
        }
    }

    /** The window at a point, topmost first. Used for click-to-focus and hit testing. */
    public fun hitTest(pointer: OffsetDp): DesktopWindow? =
        visibleWindows.lastOrNull { it.bounds.contains(pointer.x, pointer.y) }

    /** Serializable state for process-death restore. */
    public fun snapshotState(): List<DesktopWindow> = windows

    /** Restores after process death, discarding ids whose app no longer exists. */
    public fun restoreState(saved: List<DesktopWindow>, validAppIds: Set<String>) {
        _windows.clear()
        drags.clear()
        resizes.clear()
        nextZ = 0
        saved.filter { it.appId in validAppIds }.forEach { w ->
            val rect = viewport.constrain(w.bounds)
            _windows[w.id] = w.copy(bounds = rect, zIndex = nextZ++)
        }
        focusedId = visibleWindows.lastOrNull()?.id
    }

    private var drags = mutableMapOf<WindowId, DragSession>()
    private var resizes = mutableMapOf<WindowId, ResizeSession>()

    private data class DragSession(val grabOffset: OffsetDp, val startBounds: WindowRect)
    private data class ResizeSession(val startBounds: WindowRect)

    /** Re-packs z-indices into 0..n-1 preserving order, so the list never drifts. */
    private fun normalizeZ() {
        val ordered = _windows.values.sortedBy { it.zIndex }
        ordered.forEachIndexed { index, w -> _windows[w.id] = w.copy(zIndex = index) }
        nextZ = ordered.size
    }
}

/** Pointer position in dp, kept independent of Android's Offset type. */
public data class OffsetDp(public val x: Float, public val y: Float)
