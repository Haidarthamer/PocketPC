package dev.pocketpc.desktop.wm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WindowManagerTest {

    private lateinit var viewport: Viewport
    private lateinit var wm: WindowManager

    @Before
    fun setUp() {
        // A landscape phone: 412 x 915 dp minus 24dp status bar and 56dp taskbar.
        viewport = Viewport(width = 412f, height = 915f, topInset = 24f, bottomInset = 56f)
        wm = WindowManager { viewport }
    }

    // ---------- creation ----------

    @Test
    fun `opens a window with valid geometry`() {
        val w = wm.open("files", "Files")
        assertEquals(1, wm.windows.size)
        assertTrue(w.bounds.width >= WindowRect.MIN_WIDTH)
        assertTrue(w.bounds.height >= WindowRect.MIN_HEIGHT)
        assertTrue(w.bounds.y >= viewport.usableTop)
        assertTrue(w.bounds.bottom <= viewport.usableBottom)
        assertEquals(w.id, wm.focused?.id)
    }

    @Test
    fun `opening the same app twice focuses instead of duplicating`() {
        val first = wm.open("files", "Files")
        val second = wm.open("files", "Files")
        assertEquals(1, wm.windows.size)
        assertEquals(first.id, second.id)
    }

    @Test
    fun `distinct apps produce distinct windows`() {
        val a = wm.open("files", "Files")
        val b = wm.open("terminal", "Terminal")
        assertNotEquals(a.id, b.id)
        assertEquals(2, wm.windows.size)
    }

    @Test
    fun `cascade offsets successive windows`() {
        val a = wm.open("files", "Files", bounds = null)
        val b = wm.open("terminal", "Terminal", bounds = null)
        assertNotEquals(a.bounds.x, b.bounds.x)
    }

    // ---------- movement ----------

    @Test
    fun `drag moves a window by the pointer delta`() {
        // Width 200 on a 412dp-wide viewport leaves 212dp of horizontal travel, so the
        // requested position is reachable. (A 300dp window clamps x at 112dp.)
        val w = wm.open("files", "Files", bounds = WindowRect(100f, 100f, 200f, 200f))
        wm.beginDrag(w.id, OffsetDp(120f, 110f))
        wm.drag(w.id, OffsetDp(170f, 160f))
        val moved = wm.window(w.id)!!
        assertEquals(150f, moved.bounds.x, 0.01f)
        assertEquals(150f, moved.bounds.y, 0.01f)
    }

    @Test
    fun `drag cannot push the title bar off screen`() {
        val w = wm.open("files", "Files", bounds = WindowRect(100f, 100f, 300f, 400f))
        wm.beginDrag(w.id, OffsetDp(110f, 110f))
        wm.drag(w.id, OffsetDp(-5000f, -5000f))
        val moved = wm.window(w.id)!!
        assertEquals(0f, moved.bounds.x, 0.01f)
        assertEquals(viewport.usableTop, moved.bounds.y, 0.01f)
        wm.endDrag(w.id)
        wm.beginDrag(w.id, OffsetDp(moved.bounds.x + 5, moved.bounds.y + 5))
        wm.drag(w.id, OffsetDp(9000f, 9000f))
        val clamped = wm.window(w.id)!!
        assertTrue(clamped.bounds.right <= viewport.usableWidth + 0.01f)
        assertTrue(clamped.bounds.bottom <= viewport.usableBottom + 0.01f)
    }

    @Test
    fun `dragging a maximized window is ignored until restored`() {
        val w = wm.open("files", "Files")
        wm.maximize(w.id)
        val before = wm.window(w.id)!!.bounds
        wm.beginDrag(w.id, OffsetDp(before.x + 10, before.y + 10))
        wm.drag(w.id, OffsetDp(before.x + 300, before.y + 300))
        assertEquals(before, wm.window(w.id)!!.bounds)
    }

    // ---------- resizing ----------

    @Test
    fun `resize honours the requested size`() {
        val w = wm.open("files", "Files", bounds = WindowRect(20f, 40f, 300f, 300f))
        wm.beginResize(w.id)
        wm.resize(w.id, OffsetDp(320f, 380f))
        wm.endResize(w.id)
        val r = wm.window(w.id)!!.bounds
        assertEquals(300f, r.width, 0.01f)
        assertEquals(340f, r.height, 0.01f)
    }

    @Test
    fun `resize clamps to the minimum window size`() {
        val w = wm.open("files", "Files", bounds = WindowRect(20f, 40f, 300f, 300f))
        wm.beginResize(w.id)
        wm.resize(w.id, OffsetDp(25f, 45f))
        wm.endResize(w.id)
        val r = wm.window(w.id)!!.bounds
        assertTrue(r.width >= WindowRect.MIN_WIDTH)
        assertTrue(r.height >= WindowRect.MIN_HEIGHT)
    }

    @Test
    fun `non-resizable windows ignore resize`() {
        val w = wm.open("terminal", "Terminal", bounds = WindowRect(20f, 40f, 300f, 300f), resizable = false)
        wm.beginResize(w.id)
        wm.resize(w.id, OffsetDp(600f, 700f))
        wm.endResize(w.id)
        assertEquals(300f, wm.window(w.id)!!.bounds.width, 0.01f)
    }

    // ---------- minimize / maximize / restore / close ----------

    @Test
    fun `minimize hides the window and drops focus`() {
        val a = wm.open("files", "Files")
        val b = wm.open("terminal", "Terminal")
        wm.minimize(b.id)
        assertFalse(wm.window(b.id)!!.isVisible)
        assertEquals(1, wm.visibleWindows.size)
        assertEquals(a.id, wm.focused?.id)
    }

    @Test
    fun `restore brings a minimized window back at its old geometry`() {
        val w = wm.open("files", "Files", bounds = WindowRect(60f, 80f, 280f, 320f))
        val before = wm.window(w.id)!!.bounds
        wm.minimize(w.id)
        wm.restore(w.id)
        val after = wm.window(w.id)!!
        assertTrue(after.isVisible)
        assertEquals(before, after.bounds)
        assertEquals(w.id, wm.focused?.id)
    }

    @Test
    fun `maximize fills the usable area and restore returns the old size`() {
        val w = wm.open("files", "Files", bounds = WindowRect(60f, 80f, 280f, 320f))
        val original = wm.window(w.id)!!.bounds
        wm.maximize(w.id)
        val maxed = wm.window(w.id)!!
        assertEquals(WindowState.MAXIMIZED, maxed.state)
        assertEquals(viewport.usableWidth, maxed.bounds.width, 0.01f)
        assertEquals(viewport.usableTop, maxed.bounds.y, 0.01f)
        wm.unmaximize(w.id)
        val restored = wm.window(w.id)!!
        assertEquals(WindowState.NORMAL, restored.state)
        assertEquals(original, restored.bounds)
    }

    @Test
    fun `maximize is a toggle`() {
        val w = wm.open("files", "Files")
        wm.toggleMaximize(w.id)
        assertEquals(WindowState.MAXIMIZED, wm.window(w.id)!!.state)
        wm.toggleMaximize(w.id)
        assertEquals(WindowState.NORMAL, wm.window(w.id)!!.state)
    }

    @Test
    fun `close removes the window and refocuses the next one`() {
        val a = wm.open("files", "Files")
        val b = wm.open("terminal", "Terminal")
        wm.close(b.id)
        assertNull(wm.window(b.id))
        assertEquals(1, wm.windows.size)
        assertEquals(a.id, wm.focused?.id)
    }

    @Test
    fun `closing the last window leaves no focus`() {
        val a = wm.open("files", "Files")
        wm.close(a.id)
        assertNull(wm.focused)
        assertTrue(wm.visibleWindows.isEmpty())
    }

    // ---------- focus and z-order ----------

    @Test
    fun `focus raises the window to the top`() {
        val a = wm.open("files", "Files")
        wm.open("terminal", "Terminal")
        wm.focus(a.id)
        assertEquals(a.id, wm.windows.last().id)
        assertEquals(a.id, wm.focused?.id)
    }

    @Test
    fun `z indices stay dense and unique`() {
        wm.open("files", "Files")
        wm.open("terminal", "Terminal")
        wm.open("editor", "Editor")
        wm.focus(wm.windows.first().id)
        wm.close(wm.windows.first().id)
        val zs = wm.windows.map { it.zIndex }
        assertEquals((0 until wm.windows.size).toList(), zs)
    }

    @Test
    fun `hit test returns the topmost window at a point`() {
        val a = wm.open("files", "Files", bounds = WindowRect(0f, 40f, 400f, 400f))
        val b = wm.open("terminal", "Terminal", bounds = WindowRect(0f, 40f, 400f, 400f))
        assertEquals(b.id, wm.hitTest(OffsetDp(10f, 60f))?.id)
        wm.focus(a.id)
        assertEquals(a.id, wm.hitTest(OffsetDp(10f, 60f))?.id)
    }

    @Test
    fun `minimized windows are not hit-testable`() {
        val a = wm.open("files", "Files", bounds = WindowRect(0f, 40f, 400f, 400f))
        wm.minimize(a.id)
        assertNull(wm.hitTest(OffsetDp(10f, 60f)))
    }

    // ---------- snapping ----------

    @Test
    fun `dragging to the left edge snaps to the left half`() {
        val w = wm.open("files", "Files", bounds = WindowRect(150f, 100f, 300f, 400f))
        wm.beginDrag(w.id, OffsetDp(160f, 110f))
        wm.drag(w.id, OffsetDp(5f, 110f))
        wm.endDrag(w.id)
        val snapped = wm.window(w.id)!!
        assertEquals(0f, snapped.bounds.x, 0.01f)
        assertEquals(viewport.usableWidth / 2f, snapped.bounds.width, 0.01f)
        assertEquals(SnapEdge.LEFT, snapped.snappedTo)
    }

    @Test
    fun `dragging to the right edge snaps to the right half`() {
        val w = wm.open("files", "Files", bounds = WindowRect(50f, 100f, 300f, 400f))
        wm.beginDrag(w.id, OffsetDp(60f, 110f))
        wm.drag(w.id, OffsetDp(400f, 110f))
        wm.endDrag(w.id)
        val snapped = wm.window(w.id)!!
        assertEquals(SnapEdge.RIGHT, snapped.snappedTo)
        assertEquals(viewport.usableWidth, snapped.bounds.right, 0.01f)
    }

    @Test
    fun `a snapped window restores to its pre-snap size when unmaximized`() {
        val original = WindowRect(120f, 120f, 260f, 300f)
        val w = wm.open("files", "Files", bounds = original)
        wm.beginDrag(w.id, OffsetDp(130f, 130f))
        wm.drag(w.id, OffsetDp(2f, 130f))
        wm.endDrag(w.id)
        wm.maximize(w.id)
        wm.unmaximize(w.id)
        assertEquals(original, wm.window(w.id)!!.bounds)
    }

    @Test
    fun `snap edge detection ignores the middle of the screen`() {
        val r = WindowRect(150f, 300f, 100f, 200f)
        assertEquals(SnapEdge.NONE, Snapping.edgeFor(r, viewport))
    }

    // ---------- viewport / lifecycle ----------

    @Test
    fun `rotation keeps minimised state but refits geometry`() {
        val w = wm.open("files", "Files", bounds = WindowRect(20f, 100f, 380f, 700f))
        viewport = Viewport(width = 240f, height = 500f, topInset = 24f, bottomInset = 56f)
        wm.onViewportChanged()
        val r = wm.window(w.id)!!
        assertTrue(r.bounds.width <= viewport.usableWidth + 0.01f)
        assertTrue(r.bounds.bottom <= viewport.usableBottom + 0.01f)
    }

    @Test
    fun `maximized window follows a viewport change`() {
        val w = wm.open("files", "Files")
        wm.maximize(w.id)
        viewport = Viewport(width = 240f, height = 500f, topInset = 24f, bottomInset = 56f)
        wm.onViewportChanged()
        assertEquals(viewport.usableWidth, wm.window(w.id)!!.bounds.width, 0.01f)
    }

    @Test
    fun `state survives a simulated process recreation`() {
        val a = wm.open("files", "Files", bounds = WindowRect(40f, 90f, 300f, 340f))
        val b = wm.open("terminal", "Terminal", bounds = WindowRect(80f, 130f, 300f, 340f))
        wm.minimize(a.id)
        val saved = wm.snapshotState()

        val revived = WindowManager { viewport }
        revived.restoreState(saved, setOf("files", "terminal"))
        assertEquals(2, revived.windows.size)
        assertEquals(WindowState.MINIMIZED, revived.window(a.id)!!.state)
        assertEquals(b.id, revived.focused?.id)
        assertEquals(1, revived.visibleWindows.size)
    }

    @Test
    fun `restore drops windows whose app no longer exists`() {
        val a = wm.open("files", "Files")
        wm.open("ghostapp", "Ghost")
        val saved = wm.snapshotState()
        val revived = WindowManager { viewport }
        revived.restoreState(saved, setOf("files"))
        assertEquals(1, revived.windows.size)
        assertEquals(a.appId, revived.windows.single().appId)
    }

    @Test
    fun `many windows stay within the viewport`() {
        repeat(12) { i -> wm.open("app$i", "App $i") }
        assertEquals(12, wm.windows.size)
        wm.windows.forEach { w ->
            assertTrue("${w.title} left of screen", w.bounds.x >= 0f)
            assertTrue("${w.title} right of screen", w.bounds.right <= viewport.usableWidth + 0.01f)
            assertTrue("${w.title} under taskbar", w.bounds.bottom <= viewport.usableBottom + 0.01f)
        }
    }
}
