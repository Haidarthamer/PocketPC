package dev.pocketpc.desktop.wm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Tiling layout and multi-workspace behaviour. */
class TileLayoutTest {

    private lateinit var wm: WindowManager
    private val viewport = Viewport(width = 412f, height = 915f, topInset = 0f, bottomInset = 52f)

    @Before
    fun setUp() {
        wm = WindowManager { viewport }
    }

    @Test
    fun `a single window fills the workspace`() {
        wm.open("files", "Files")
        TileLayout.tile(wm)
        val b = wm.focused!!.bounds
        assertEquals(viewport.usableWidth, b.width, 0.01f)
        assertEquals(viewport.usableHeight, b.height, 0.01f)
    }

    @Test
    fun `two windows sit side by side`() {
        wm.open("files", "Files")
        wm.open("terminal", "Terminal")
        TileLayout.tile(wm)
        val bounds = wm.windows.map { it.bounds }
        assertEquals(2, bounds.size)
        assertEquals(viewport.usableWidth / 2f, bounds[0].width, 0.01f)
        assertEquals(bounds[0].width, bounds[1].width, 0.01f)
        assertTrue(bounds[1].x >= bounds[0].right - 0.01f)
    }

    @Test
    fun `four windows form a two by two grid`() {
        listOf("files", "terminal", "editor", "calculator").forEach { wm.open(it, it) }
        TileLayout.tile(wm)
        val bounds = wm.windows.map { it.bounds }
        assertEquals(viewport.usableWidth / 2f, bounds[0].width, 0.01f)
        assertEquals(viewport.usableHeight / 2f, bounds[0].height, 0.01f)
        // Third window starts the second row.
        assertTrue(bounds[2].y >= bounds[0].bottom - 0.01f)
    }

    @Test
    fun `tiling never places a window outside the viewport`() {
        repeat(7) { wm.open("app$it", "App $it") }
        TileLayout.tile(wm)
        wm.windows.forEach { w ->
            assertTrue(w.bounds.x >= -0.01f)
            assertTrue(w.bounds.right <= viewport.usableWidth + 0.01f)
            assertTrue(w.bounds.bottom <= viewport.usableBottom + 0.01f)
        }
    }

    @Test
    fun `tiling an empty desktop is a no-op`() {
        TileLayout.tile(wm)
        assertTrue(wm.windows.isEmpty())
    }
}

class WorkspaceManagerTest {

    private val viewport = Viewport(width = 412f, height = 915f, bottomInset = 52f)

    @Test
    fun `each workspace keeps its own windows`() {
        val ws = WorkspaceManager(count = 3) { viewport }
        ws.workspace(0)!!.open("files", "Files")
        ws.workspace(1)!!.open("terminal", "Terminal")
        assertEquals(1, ws.workspace(0)!!.windows.size)
        assertEquals(1, ws.workspace(1)!!.windows.size)
        assertEquals("files", ws.workspace(0)!!.windows.single().appId)
    }

    @Test
    fun `switching changes the active workspace only`() {
        val ws = WorkspaceManager(count = 3) { viewport }
        ws.workspace(0)!!.open("files", "Files")
        assertTrue(ws.switchTo(2))
        assertEquals(2, ws.activeIndex)
        assertTrue(ws.active.windows.isEmpty())
        assertTrue(ws.workspace(0)!!.windows.isNotEmpty())
    }

    @Test
    fun `out of range workspace is rejected`() {
        val ws = WorkspaceManager(count = 3) { viewport }
        assertFalse(ws.switchTo(5))
        assertFalse(ws.switchTo(-1))
        assertEquals(0, ws.activeIndex)
        assertNull(ws.workspace(9))
    }

    @Test
    fun `cycling wraps around`() {
        val ws = WorkspaceManager(count = 3) { viewport }
        assertEquals(1, ws.cycle())
        assertEquals(2, ws.cycle())
        assertEquals(0, ws.cycle())
        assertEquals(2, ws.cycle(backwards = true))
    }

    @Test
    fun `counts aggregate across workspaces`() {
        val ws = WorkspaceManager(count = 2) { viewport }
        ws.workspace(0)!!.open("files", "Files")
        ws.workspace(0)!!.open("terminal", "Terminal")
        ws.workspace(1)!!.open("editor", "Editor")
        assertEquals(3, ws.totalWindowCount())
        assertEquals(listOf(2, 1), ws.windowCounts())
    }

    @Test
    fun `viewport change is propagated to every workspace`() {
        val ws = WorkspaceManager(count = 2) { Viewport(width = 412f, height = 915f, bottomInset = 52f) }
        ws.workspace(0)!!.open("files", "Files")
        ws.workspace(1)!!.open("terminal", "Terminal")
        ws.onViewportChanged()
        assertEquals(2, ws.totalWindowCount())
    }
}
