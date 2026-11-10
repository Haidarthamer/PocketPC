package dev.pocketpc.desktop.apps

/**
 * Application identity for the desktop.
 *
 * Every desktop app is described by one of these so the Start menu, taskbar, desktop
 * icons and window manager all agree on identity, title and icon. `iconKey` maps to a
 * vector asset in the UI layer, keeping this module Android-free.
 */
public data class AppDefinition(
    public val id: String,
    public val name: String,
    public val iconKey: String,
    /** Short description shown in the Start menu. */
    public val description: String,
    public val category: AppCategory,
    /** Preferred initial window size as a fraction of the usable viewport. */
    public val widthFraction: Float = 0.72f,
    public val heightFraction: Float = 0.66f,
    public val resizable: Boolean = true,
    /** Apps that may only exist once at a time (Settings, Task Manager...). */
    public val singleton: Boolean = true,
)

public enum class AppCategory { SYSTEM, PRODUCTIVITY, MEDIA, SYSTEM_TOOLS, DEVELOPMENT, LINUX }

/**
 * The Phase 2 application catalogue.
 *
 * Only apps that actually exist in this build are listed. Each id is handled by a real
 * composable in the desktop's window host; nothing here is a dead entry.
 */
public object AppRegistry {
    public val apps: List<AppDefinition> = listOf(
        AppDefinition(
            id = "files",
            name = "Files",
            iconKey = "folder",
            description = "Browse device storage",
            category = AppCategory.SYSTEM_TOOLS,
        ),
        AppDefinition(
            id = "terminal",
            name = "Terminal",
            iconKey = "terminal",
            description = "Command line on the host environment",
            category = AppCategory.DEVELOPMENT,
        ),
        AppDefinition(
            id = "editor",
            name = "Text Editor",
            iconKey = "editor",
            description = "Edit plain text files",
            category = AppCategory.PRODUCTIVITY,
        ),
        AppDefinition(
            id = "calculator",
            name = "Calculator",
            iconKey = "calculator",
            description = "Arithmetic with a real expression parser",
            category = AppCategory.PRODUCTIVITY,
            widthFraction = 0.42f,
            heightFraction = 0.62f,
        ),
        AppDefinition(
            id = "taskmanager",
            name = "Task Manager",
            iconKey = "taskmanager",
            description = "Running windows and memory use",
            category = AppCategory.SYSTEM_TOOLS,
        ),
        AppDefinition(
            id = "monitor",
            name = "System Monitor",
            iconKey = "monitor",
            description = "CPU, memory and thermal state",
            category = AppCategory.SYSTEM_TOOLS,
        ),
        AppDefinition(
            id = "gpu",
            name = "GPU Diagnostics",
            iconKey = "gpu",
            description = "Verify hardware rendering and measure frame times",
            category = AppCategory.SYSTEM_TOOLS,
        ),
        AppDefinition(
            id = "settings",
            name = "Settings",
            iconKey = "settings",
            description = "Desktop, theme and performance",
            category = AppCategory.SYSTEM,
        ),
        AppDefinition(
            id = "about",
            name = "About Pocket PC",
            iconKey = "info",
            description = "Version, build and capability report",
            category = AppCategory.SYSTEM,
            widthFraction = 0.6f,
            heightFraction = 0.7f,
        ),
    )

    public val byId: Map<String, AppDefinition> = apps.associateBy { it.id }

    public fun find(id: String): AppDefinition? = byId[id]

    /** Desktop shortcuts shown on first run. */
    public val defaultDesktopIcons: List<String> =
        listOf("files", "terminal", "editor", "calculator", "gpu", "settings")

    /** Taskbar pinned apps. */
    public val defaultPinned: List<String> = listOf("files", "terminal", "gpu", "settings")

    /**
     * Start-menu search. Case-insensitive substring match over name, description and
     * category, returning name-prefix matches first so typing "te" puts Terminal on top.
     */
    public fun search(query: String): List<AppDefinition> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return apps
        val scored = apps.mapNotNull { app ->
            val name = app.name.lowercase()
            val inName = name.indexOf(q)
            val inDesc = app.description.lowercase().contains(q)
            val inCat = app.category.name.lowercase().contains(q)
            when {
                inName == 0 -> app to 0
                inName > 0 -> app to 1
                inDesc -> app to 2
                inCat -> app to 3
                else -> null
            }
        }
        return scored.sortedWith(compareBy({ it.second }, { it.first.name })).map { it.first }
    }
}
