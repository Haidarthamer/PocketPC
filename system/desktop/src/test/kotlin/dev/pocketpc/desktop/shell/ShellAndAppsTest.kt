package dev.pocketpc.desktop.shell

import dev.pocketpc.desktop.apps.AppRegistry
import dev.pocketpc.desktop.apps.CalcResult
import dev.pocketpc.desktop.apps.Calculator
import dev.pocketpc.desktop.input.DesktopAction
import dev.pocketpc.desktop.input.DesktopKey
import dev.pocketpc.desktop.input.KeyBindings
import dev.pocketpc.desktop.input.KeyChord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRegistryTest {

    @Test
    fun `every app has a unique id and non-blank metadata`() {
        val ids = AppRegistry.apps.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        AppRegistry.apps.forEach {
            assertTrue(it.name.isNotBlank())
            assertTrue(it.iconKey.isNotBlank())
            assertTrue(it.description.isNotBlank())
            assertTrue(it.widthFraction in 0.2f..1f)
            assertTrue(it.heightFraction in 0.2f..1f)
        }
    }

    @Test
    fun `desktop shortcuts and pinned apps reference real apps`() {
        AppRegistry.defaultDesktopIcons.forEach { assertNotNull("missing app $it", AppRegistry.find(it)) }
        AppRegistry.defaultPinned.forEach { assertNotNull("missing app $it", AppRegistry.find(it)) }
    }

    @Test
    fun `search ranks name prefix matches first`() {
        val results = AppRegistry.search("te")
        assertEquals("terminal", results.first().id)
    }

    @Test
    fun `search is case insensitive and matches descriptions`() {
        assertTrue(AppRegistry.search("CALC").any { it.id == "calculator" })
        assertTrue(AppRegistry.search("thermal").any { it.id == "monitor" })
    }

    @Test
    fun `empty search returns everything`() {
        assertEquals(AppRegistry.apps.size, AppRegistry.search("  ").size)
    }

    @Test
    fun `unknown search returns empty`() {
        assertTrue(AppRegistry.search("zzzzz").isEmpty())
    }
}

class ShellModelTest {

    @Test
    fun `launching marks running, active and recent`() {
        val shell = ShellModel()
        shell.onAppLaunched("terminal")
        assertTrue(shell.isRunning("terminal"))
        assertTrue(shell.isActive("terminal"))
        assertEquals("terminal", shell.recentApps.first().id)
    }

    @Test
    fun `recent list de-duplicates and caps`() {
        val shell = ShellModel()
        listOf("files", "terminal", "editor", "calculator", "gpu", "settings", "about", "files")
            .forEach { shell.onAppLaunched(it) }
        assertEquals(ShellModel.MAX_RECENT, shell.recentApps.size)
        assertEquals("files", shell.recentApps.first().id)
        assertEquals(1, shell.recentApps.count { it.id == "files" })
    }

    @Test
    fun `closing the active app moves focus to another running app`() {
        val shell = ShellModel()
        shell.onAppLaunched("files")
        shell.onAppLaunched("terminal")
        shell.onAppClosed("terminal")
        assertFalse(shell.isRunning("terminal"))
        assertEquals("files", shell.activeApp?.id)
    }

    @Test
    fun `unknown apps are ignored`() {
        val shell = ShellModel()
        shell.onAppLaunched("nope")
        assertFalse(shell.isRunning("nope"))
        assertNull(shell.activeApp)
    }

    @Test
    fun `pinning toggles`() {
        val shell = ShellModel()
        assertTrue(shell.isPinned("terminal"))
        shell.togglePin("terminal")
        assertFalse(shell.isPinned("terminal"))
        shell.togglePin("terminal")
        assertTrue(shell.isPinned("terminal"))
    }

    @Test
    fun `start menu and quick settings are mutually exclusive`() {
        val shell = ShellModel()
        shell.toggleStartMenu()
        assertTrue(shell.isStartMenuOpen)
        shell.toggleQuickSettings()
        assertTrue(shell.isQuickSettingsOpen)
        assertFalse(shell.isStartMenuOpen)
        assertTrue(shell.dismissOverlays())
        assertFalse(shell.dismissOverlays())
    }

    @Test
    fun `launching an app closes overlays`() {
        val shell = ShellModel()
        shell.toggleStartMenu()
        shell.onAppLaunched("editor")
        assertFalse(shell.isStartMenuOpen)
    }

    @Test
    fun `alt-tab cycles through running apps`() {
        val shell = ShellModel()
        shell.onAppLaunched("files")
        shell.onAppLaunched("terminal")
        shell.onAppLaunched("editor")
        val next = shell.nextRunningApp()
        assertNotNull(next)
        assertTrue(next!!.id in setOf("files", "terminal"))
        val back = shell.nextRunningApp(backwards = true)
        assertEquals("editor", back?.id)
    }

    @Test
    fun `alt-tab does nothing with fewer than two apps`() {
        val shell = ShellModel()
        shell.onAppLaunched("files")
        assertNull(shell.nextRunningApp())
    }
}

class CalculatorTest {

    private fun value(expr: String): Double {
        val r = Calculator.evaluate(expr)
        assertTrue("expected value for '$expr', got $r", r is CalcResult.Value)
        return (r as CalcResult.Value).value
    }

    @Test
    fun `respects operator precedence`() {
        assertEquals(14.0, value("2+3*4"), 1e-9)
    }

    @Test
    fun `handles parentheses and unary minus`() {
        assertEquals(20.0, value("(2+3)*4"), 1e-9)
        assertEquals(-7.0, value("-7"), 1e-9)
        assertEquals(1.0, value("3--2-4"), 1e-9)
    }

    @Test
    fun `handles decimals and division`() {
        assertEquals(0.5, value("1/2"), 1e-9)
        assertEquals(2.5, value("5.0/2"), 1e-9)
    }

    @Test
    fun `percentage divides by one hundred`() {
        assertEquals(25.0, value("2500%"), 1e-9)
    }

    @Test
    fun `accepts the on-screen operator glyphs`() {
        assertEquals(6.0, value("2×3"), 1e-9)
        assertEquals(3.0, value("6÷2"), 1e-9)
    }

    @Test
    fun `division by zero is an error not a crash`() {
        assertTrue(Calculator.evaluate("1/0") is CalcResult.Error)
    }

    @Test
    fun `malformed input is an error`() {
        assertTrue(Calculator.evaluate("2+") is CalcResult.Error)
        assertTrue(Calculator.evaluate("(2+3") is CalcResult.Error)
        assertTrue(Calculator.evaluate("2++") is CalcResult.Error)
        assertTrue(Calculator.evaluate("abc") is CalcResult.Error)
    }

    @Test
    fun `empty expression is zero`() {
        assertEquals(0.0, value(""), 1e-9)
    }

    @Test
    fun `formatting drops trailing zeros`() {
        assertEquals("42", Calculator.format(42.0))
        assertEquals("0.5", Calculator.format(0.5))
        assertEquals("-3", Calculator.format(-3.0))
    }
}

class KeyBindingsTest {

    @Test
    fun `every binding resolves and has help text`() {
        KeyBindings.bindings.forEach { (chord, action) ->
            assertEquals(action, KeyBindings.resolve(chord))
            // shortcutFor returns the first chord for an action; some actions have two
            // (maximise is bound to both Ctrl+M and Meta+Up).
            val roundTrip = KeyBindings.shortcutFor(action)
            assertNotNull("no chord for $action", roundTrip)
            assertEquals(action, KeyBindings.resolve(roundTrip!!))
        }
        val help = KeyBindings.describeAll()
        assertEquals(KeyBindings.bindings.size, help.size)
        assertTrue(help.all { it.first.isNotBlank() && it.second.isNotBlank() })
    }

    @Test
    fun `alt-tab and alt-shift-tab differ`() {
        val fwd = KeyBindings.resolve(KeyChord(DesktopKey.TAB, alt = true))
        val back = KeyBindings.resolve(KeyChord(DesktopKey.TAB, alt = true, shift = true))
        assertEquals(DesktopAction.FocusNextWindow, fwd)
        assertEquals(DesktopAction.FocusPreviousWindow, back)
    }

    @Test
    fun `meta-left snaps left`() {
        assertEquals(DesktopAction.SnapWindow(dev.pocketpc.desktop.input.SnapSide.LEFT), KeyBindings.resolve(KeyChord(DesktopKey.LEFT, meta = true)))
    }

    @Test
    fun `workspace shortcuts map in order`() {
        assertEquals(DesktopAction.SwitchWorkspace(0), KeyBindings.resolve(KeyChord(DesktopKey.ONE, meta = true)))
        assertEquals(DesktopAction.SwitchWorkspace(2), KeyBindings.resolve(KeyChord(DesktopKey.THREE, meta = true)))
    }

    @Test
    fun `unbound chord returns null`() {
        assertNull(KeyBindings.resolve(KeyChord(DesktopKey.L, ctrl = true, shift = true)))
    }

    @Test
    fun `chord display name includes modifiers`() {
        assertEquals("Ctrl+Alt+G", KeyChord(DesktopKey.G, ctrl = true, alt = true).displayName)
    }
}
