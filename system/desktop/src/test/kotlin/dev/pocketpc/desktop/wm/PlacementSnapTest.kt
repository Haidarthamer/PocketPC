package dev.pocketpc.desktop.wm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Placement near an edge must not be mistaken for a snap gesture.
 *
 * Regression tests for a real bug found by CI: `open(bounds = ...)` at x = 0 was being
 * reported as snapped-to-left, which made tiled and workspace layouts look snapped. A
 * window only snaps when a *drag is released* near an edge.
 */
class PlacementSnapTest {

    private val viewport = Viewport(width = 412f, height = 915f, topInset = 24f, bottomInset = 56f)

    @Test
    fun `opening a window flush with the left edge is not snapped`() {
        val wm = WindowManager { viewport }
        val w = wm.open("files", "Files", bounds = WindowRect(0f, 100f, 300f, 300f))
        assertEquals(SnapEdge.NONE, w.snappedTo)
        assertEquals(0f, w.bounds.x, 0.01f)
    }

    @Test
    fun `applyBounds to a half-screen cell keeps exact geometry`() {
        val wm = WindowManager { viewport }
        val w = wm.open("files", "Files")
        wm.applyBounds(w.id, WindowRect(0f, viewport.usableTop, viewport.usableWidth / 2f, viewport.usableHeight))
        val placed = wm.window(w.id)!!
        assertEquals(viewport.usableWidth / 2f, placed.bounds.width, 0.01f)
        assertEquals(SnapEdge.NONE, placed.snappedTo)
    }

    @Test
    fun `a released drag at the edge does snap`() {
        val wm = WindowManager { viewport }
        val w = wm.open("files", "Files", bounds = WindowRect(120f, 200f, 260f, 300f))
        wm.beginDrag(w.id, OffsetDp(130f, 210f))
        wm.drag(w.id, OffsetDp(3f, 210f))
        wm.endDrag(w.id)
        assertEquals(SnapEdge.LEFT, wm.window(w.id)!!.snappedTo)
        assertEquals(viewport.usableWidth / 2f, wm.window(w.id)!!.bounds.width, 0.01f)
    }

    @Test
    fun `endDrag with allowSnap false leaves the drag position intact`() {
        val wm = WindowManager { viewport }
        val w = wm.open("files", "Files", bounds = WindowRect(120f, 200f, 260f, 300f))
        wm.beginDrag(w.id, OffsetDp(130f, 210f))
        wm.drag(w.id, OffsetDp(3f, 210f))
        wm.endDrag(w.id, allowSnap = false)
        val after = wm.window(w.id)!!
        assertEquals(0f, after.bounds.x, 0.01f)
        assertEquals(260f, after.bounds.width, 0.01f)
        assertEquals(SnapEdge.NONE, after.snappedTo)
    }

    @Test
    fun `a window in the middle of the screen does not snap`() {
        val wm = WindowManager { viewport }
        val w = wm.open("files", "Files", bounds = WindowRect(100f, 300f, 200f, 200f))
        wm.beginDrag(w.id, OffsetDp(110f, 310f))
        wm.drag(w.id, OffsetDp(200f, 400f))
        wm.endDrag(w.id)
        assertNotEquals(SnapEdge.LEFT, wm.window(w.id)!!.snappedTo)
        assertEquals(200f, wm.window(w.id)!!.bounds.width, 0.01f)
    }

    @Test
    fun `z indices are zero based for a single window`() {
        val wm = WindowManager { viewport }
        val w = wm.open("files", "Files")
        assertEquals(0, w.zIndex)
        assertEquals(0, wm.window(w.id)!!.zIndex)
    }
}
