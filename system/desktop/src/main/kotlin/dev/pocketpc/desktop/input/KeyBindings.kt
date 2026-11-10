package dev.pocketpc.desktop.input

/**
 * Physical-keyboard shortcuts for the desktop.
 *
 * Declared in the Android-free module so the mapping is unit tested, and interpreted by
 * the UI layer (which has the real KeyEvent). Hardware keyboards, USB and Bluetooth
 * keyboards all arrive through the same `onKeyEvent` path in Compose once the activity
 * requests pointer capture, so a single mapping covers them.
 */
public data class KeyChord(
    public val key: DesktopKey,
    public val ctrl: Boolean = false,
    public val alt: Boolean = false,
    public val shift: Boolean = false,
    public val meta: Boolean = false,
) {
    public val displayName: String
        get() = buildString {
            if (ctrl) append("Ctrl+")
            if (alt) append("Alt+")
            if (shift) append("Shift+")
            if (meta) append("Meta+")
            append(key.label)
        }
}

public enum class DesktopKey(public val label: String) {
    TAB("Tab"),
    ENTER("Enter"),
    ESCAPE("Esc"),
    SPACE("Space"),
    LEFT("Left"),
    RIGHT("Right"),
    UP("Up"),
    DOWN("Down"),
    W("W"),
    Q("Q"),
    M("M"),
    E("E"),
    F("F"),
    D("D"),
    L("L"),
    N("N"),
    H("H"),
    G("G"),
    ONE("1"),
    TWO("2"),
    THREE("3"),
}

/** What the desktop should do for a chord. */
public sealed class DesktopAction {
    /** Show or hide the Start menu. */
    public data object ToggleStartMenu : DesktopAction()
    /** Cycle to the next running window. */
    public data object FocusNextWindow : DesktopAction()
    public data object FocusPreviousWindow : DesktopAction()
    public data object CloseFocusedWindow : DesktopAction()
    public data object MinimizeFocusedWindow : DesktopAction()
    public data object ToggleMaximizeFocusedWindow : DesktopAction()
    public data object ToggleFullscreenFocusedWindow : DesktopAction()
    /** Snap the focused window to the left/right half. */
    public data class SnapWindow(public val side: SnapSide) : DesktopAction()
    public object TileAllWindows : DesktopAction()
    /** Switch to workspace 1..3. */
    public data class SwitchWorkspace(public val index: Int) : DesktopAction()
    public data object ToggleQuickSettings : DesktopAction()
    public data object OpenGpuDiagnostics : DesktopAction()
    public data object ShowShortcutHelp : DesktopAction()
}

public enum class SnapSide { LEFT, RIGHT }

public object KeyBindings {

    /** The complete, documented shortcut table (also shown in the help palette). */
    public val bindings: Map<KeyChord, DesktopAction> = mapOf(
        KeyChord(DesktopKey.TAB, alt = true) to DesktopAction.FocusNextWindow,
        KeyChord(DesktopKey.TAB, alt = true, shift = true) to DesktopAction.FocusPreviousWindow,
        KeyChord(DesktopKey.W, ctrl = true) to DesktopAction.CloseFocusedWindow,
        KeyChord(DesktopKey.M, ctrl = true) to DesktopAction.MinimizeFocusedWindow,
        KeyChord(DesktopKey.M, meta = true) to DesktopAction.ToggleMaximizeFocusedWindow,
        KeyChord(DesktopKey.F, meta = true) to DesktopAction.ToggleFullscreenFocusedWindow,
        KeyChord(DesktopKey.LEFT, meta = true) to DesktopAction.SnapWindow(SnapSide.LEFT),
        KeyChord(DesktopKey.RIGHT, meta = true) to DesktopAction.SnapWindow(SnapSide.RIGHT),
        KeyChord(DesktopKey.UP, meta = true) to DesktopAction.ToggleMaximizeFocusedWindow,
        KeyChord(DesktopKey.D, meta = true) to DesktopAction.ShowShortcutHelp,
        KeyChord(DesktopKey.Q, meta = true) to DesktopAction.ToggleQuickSettings,
        KeyChord(DesktopKey.G, ctrl = true, alt = true) to DesktopAction.OpenGpuDiagnostics,
        KeyChord(DesktopKey.ONE, meta = true) to DesktopAction.SwitchWorkspace(0),
        KeyChord(DesktopKey.TWO, meta = true) to DesktopAction.SwitchWorkspace(1),
        KeyChord(DesktopKey.THREE, meta = true) to DesktopAction.SwitchWorkspace(2),
        KeyChord(DesktopKey.ESCAPE) to DesktopAction.ToggleStartMenu,
    )

    /** Resolves a chord, or null when the desktop has no binding for it. */
    public fun resolve(chord: KeyChord): DesktopAction? = bindings[chord]

    public fun shortcutFor(action: DesktopAction): KeyChord? =
        bindings.entries.firstOrNull { it.value == action }?.key

    /** Human-readable list for the help overlay, sorted for stable display. */
    public fun describeAll(): List<Pair<String, String>> = bindings.entries
        .map { it.key.displayName to describe(it.value) }
        .sortedBy { it.first }

    private fun describe(action: DesktopAction): String = when (action) {
        DesktopAction.ToggleStartMenu -> "Open or close the Start menu"
        DesktopAction.FocusNextWindow -> "Switch to the next window"
        DesktopAction.FocusPreviousWindow -> "Switch to the previous window"
        DesktopAction.CloseFocusedWindow -> "Close the focused window"
        DesktopAction.MinimizeFocusedWindow -> "Minimise the focused window"
        DesktopAction.ToggleMaximizeFocusedWindow -> "Maximise or restore"
        DesktopAction.ToggleFullscreenFocusedWindow -> "Toggle fullscreen"
        is DesktopAction.SnapWindow -> "Snap window to the ${action.side.name.lowercase()} half"
        DesktopAction.TileAllWindows -> "Tile all windows"
        is DesktopAction.SwitchWorkspace -> "Switch to workspace ${action.index + 1}"
        DesktopAction.ToggleQuickSettings -> "Open or close quick settings"
        DesktopAction.OpenGpuDiagnostics -> "Open GPU diagnostics"
        DesktopAction.ShowShortcutHelp -> "Show keyboard shortcuts"
    }
}
