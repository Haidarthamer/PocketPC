package dev.pocketpc.desktop.shell

import dev.pocketpc.desktop.apps.AppDefinition
import dev.pocketpc.desktop.apps.AppRegistry

/**
 * Taskbar and Start-menu state: what is pinned, what is running, what is active, and how
 * launch history feeds the Start menu's "recent" section.
 *
 * Pure state, no Android types; the UI renders it and the unit tests pin it down.
 */
public class ShellModel(
    private val registry: AppRegistry = AppRegistry,
) {
    private val pinned = LinkedHashSet(registry.defaultPinned)
    private val running = LinkedHashSet<String>()
    private val recent = ArrayDeque<String>()
    private var activeAppId: String? = null
    private var startMenuOpen = false
    private var quickSettingsOpen = false

    public companion object {
        /** How many recent apps the Start menu remembers. */
        public const val MAX_RECENT: Int = 6
    }

    public val pinnedApps: List<AppDefinition>
        get() = pinned.mapNotNull { registry.find(it) }

    public val runningApps: List<AppDefinition>
        get() = running.mapNotNull { registry.find(it) }

    public val activeApp: AppDefinition?
        get() = activeAppId?.let { registry.find(it) }

    public val isStartMenuOpen: Boolean get() = startMenuOpen
    public val isQuickSettingsOpen: Boolean get() = quickSettingsOpen

    /** Recently launched apps, most recent first. */
    public val recentApps: List<AppDefinition>
        get() = recent.toList().mapNotNull { registry.find(it) }

    public fun isRunning(appId: String): Boolean = appId in running
    public fun isPinned(appId: String): Boolean = appId in pinned
    public fun isActive(appId: String): Boolean = activeAppId == appId

    /** Records that an app was launched or brought to the front. */
    public fun onAppLaunched(appId: String) {
        if (registry.find(appId) == null) return
        running += appId
        activeAppId = appId
        recent.remove(appId)
        recent.addFirst(appId)
        while (recent.size > MAX_RECENT) recent.removeLast()
        startMenuOpen = false
        quickSettingsOpen = false
    }

    /** Records that every window of an app was closed. */
    public fun onAppClosed(appId: String) {
        running -= appId
        if (activeAppId == appId) activeAppId = running.lastOrNull()
    }

    public fun onAppMinimized(appId: String) {
        if (activeAppId == appId) {
            // Focus falls to another running app, if any.
            activeAppId = running.lastOrNull { it != appId }
        }
    }

    public fun togglePin(appId: String) {
        if (registry.find(appId) == null) return
        if (!pinned.remove(appId)) pinned += appId
    }

    public fun toggleStartMenu() {
        startMenuOpen = !startMenuOpen
        if (startMenuOpen) quickSettingsOpen = false
    }

    public fun closeStartMenu() {
        startMenuOpen = false
    }

    public fun toggleQuickSettings() {
        quickSettingsOpen = !quickSettingsOpen
        if (quickSettingsOpen) startMenuOpen = false
    }

    public fun closeQuickSettings() {
        quickSettingsOpen = false
    }

    /** Closes whichever overlay is open. Returns true if something was closed. */
    public fun dismissOverlays(): Boolean {
        val had = startMenuOpen || quickSettingsOpen
        startMenuOpen = false
        quickSettingsOpen = false
        return had
    }

    /**
     * Cycles focus through running apps, used by Alt+Tab / taskbar long-press.
     * Returns the app to activate, or null when fewer than two apps are running.
     */
    public fun nextRunningApp(backwards: Boolean = false): AppDefinition? {
        val order = running.mapNotNull { registry.find(it) }
        if (order.size < 2) return null
        val currentIndex = order.indexOfFirst { it.id == activeAppId }
        val delta = if (backwards) -1 else 1
        val nextIndex = Math.floorMod(currentIndex + delta, order.size)
        val next = order[nextIndex]
        activeAppId = next.id
        return next
    }
}
