package dev.pocketpc.desktop.wm

/**
 * Multi-workspace support.
 *
 * Each workspace owns its own [WindowManager]; switching workspaces hides one set of
 * windows and shows another, which is what makes the feature real rather than cosmetic.
 * A single shared viewport provider keeps every workspace consistent with the screen.
 */
public class WorkspaceManager(
    public val count: Int = 3,
    private val viewportProvider: () -> Viewport,
) {
    init {
        require(count in 1..9) { "workspace count must be between 1 and 9" }
    }

    private val managers: List<WindowManager> = List(count) { WindowManager(viewportProvider) }

    public var activeIndex: Int = 0
        private set

    public val active: WindowManager get() = managers[activeIndex]

    public fun workspace(index: Int): WindowManager? = managers.getOrNull(index)

    /** Switches workspace; ignored when the index is out of range. Returns success. */
    public fun switchTo(index: Int): Boolean {
        if (index !in 0 until count) return false
        activeIndex = index
        return true
    }

    /** Cycles forward/backward through workspaces. */
    public fun cycle(backwards: Boolean = false): Int {
        activeIndex = Math.floorMod(activeIndex + if (backwards) -1 else 1, count)
        return activeIndex
    }

    /** Total open windows across all workspaces, for the task manager. */
    public fun totalWindowCount(): Int = managers.sumOf { it.windows.size }

    /** Propagates a viewport change (rotation) to every workspace. */
    public fun onViewportChanged() = managers.forEach { it.onViewportChanged() }

    /** Number of windows per workspace, for the workspace indicator badges. */
    public fun windowCounts(): List<Int> = managers.map { it.visibleWindows.size }
}
