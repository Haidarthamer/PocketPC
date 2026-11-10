package dev.pocketpc.desktop.wm

/** Unique identity of an open window instance. */
public data class WindowId(public val value: String) {
    init {
        require(value.isNotBlank()) { "window id must not be blank" }
    }
}

public enum class WindowState {
    /** Shown, focusable, may be raised. */
    NORMAL,
    MINIMIZED,
    MAXIMIZED,
    /** Filling the screen, with system chrome hidden. */
    FULLSCREEN,
}

/**
 * One open window. `restoreBounds` is the geometry to return to when un-maximising or
 * un-minimising, which is what makes restore behaviour feel correct.
 */
public data class DesktopWindow(
    public val id: WindowId,
    public val appId: String,
    public val title: String,
    public val bounds: WindowRect,
    public val restoreBounds: WindowRect = bounds,
    public val state: WindowState = WindowState.NORMAL,
    public val zIndex: Int = 0,
    /** Set when the window was snapped; cleared when the user moves it again. */
    public val snappedTo: SnapEdge = SnapEdge.NONE,
    public val resizable: Boolean = true,
) {
    public val isVisible: Boolean get() = state != WindowState.MINIMIZED
    public val keepsRestoreBounds: Boolean
        get() = state == WindowState.MAXIMIZED || state == WindowState.FULLSCREEN || snappedTo != SnapEdge.NONE
}
