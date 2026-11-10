package dev.pocketpc.desktop.wm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for two defects CI exposed on the target hardware geometry
 * (POCO F7 Pro reports a 412dp-wide display).
 */
class SnapWidthRegressionTest {

    private val phoneLandscape = Viewport(width = 412f, height = 915f, topInset = 24f, bottomInset = 56f)

    @Test
    fun `left and right snapping produces real half-screen windows on a 412dp display`() {
        val half = phoneLandscape.usableWidth / 2f
        assertTrue(
            "MIN_WIDTH (${WindowRect.MIN_WIDTH}) must be below half the viewport ($half) " +
                "or edge snapping silently clamps back to the minimum",
            WindowRect.MIN_WIDTH < half,
        )
        val wm = WindowManager { phoneLandscape }
        val w = wm.open("files", "Files", bounds = WindowRect(100f, 200f, 260f, 300f))
        wm.applyBounds(w.id, WindowRect(0f, phoneLandscape.usableTop, half, phoneLandscape.usableHeight))
        assertEquals(half, wm.window(w.id)!!.bounds.width, 0.01f)
    }

    @Test
    fun `windows are reported in z-order after a refocus`() {
        val wm = WindowManager { phoneLandscape }
        val a = wm.open("files", "Files")
        wm.open("terminal", "Terminal")
        wm.focus(a.id)
        assertEquals(a.id, wm.windows.last().id)
        assertEquals(a.id, wm.visibleWindows.last().id)
        // z-indices ascend along the list.
        val z = wm.windows.map { it.zIndex }
        assertEquals(z.sorted(), z)
    }

    @Test
    fun `hit testing follows the most recently focused window`() {
        val wm = WindowManager { phoneLandscape }
        val a = wm.open("files", "Files", bounds = WindowRect(0f, 100f, 300f, 300f))
        wm.open("terminal", "Terminal", bounds = WindowRect(0f, 100f, 300f, 300f))
        wm.focus(a.id)
        assertEquals(a.id, wm.hitTest(OffsetDp(20f, 120f))?.id)
    }
}
