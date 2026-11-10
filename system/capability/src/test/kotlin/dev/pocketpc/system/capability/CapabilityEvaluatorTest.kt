package dev.pocketpc.system.capability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the anti-overclaiming rules.
 *
 * Post-Phase-2 these encode the eight-state model: an API being present can never
 * produce VERIFIED, and measured rendering evidence is the only route to it.
 */
class CapabilityEvaluatorTest {

    /** Measured on the POCO F7 Pro (24117RK2CG), API 36, 2026-11-10. */
    private val targetDevice = HardwareSnapshot(
        abi = "arm64-v8a",
        supportedAbis = listOf("arm64-v8a", "armeabi-v7a"),
        cpuCores = 8,
        cpuMaxFreqKHz = 3_302_000,
        ramTotalMb = 11_100,
        storageFreeMb = 11_786,
        sdkInt = 36,
        deviceModel = "24117RK2CG",
        deviceManufacturer = "Xiaomi",
        gpuVendor = "Qualcomm",
        gpuRenderer = "Adreno (TM) 750",
        glEsVersion = "3.2",
        vulkanSupported = true,
        vulkanApiVersion = "1.3.0",
        avfDeclared = false,
        kvmPresent = false,
        thermalStatus = 0,
    )

    private fun eval(s: HardwareSnapshot) = CapabilityEvaluator.evaluate(s)

    // ---------- honesty of the state model ----------

    @Test
    fun `unprobed device never reports a usable capability`() {
        val report = eval(HardwareSnapshot.unprobed("probe crashed"))
        report.capabilities.forEach { cap ->
            assertTrue(
                "${cap.feature} claimed ${cap.state} without any probe",
                cap.state == CapabilityState.FAILED || cap.state == CapabilityState.UNSUPPORTED,
            )
        }
    }

    @Test
    fun `api availability never yields verified for any graphics capability`() {
        val report = eval(targetDevice)
        listOf(Feature.GPU_VULKAN, Feature.GPU_GLES3, Feature.GPU_COMPOSITOR).forEach { f ->
            val state = report.of(f).state
            assertNotEquals("$f must not be VERIFIED without a runtime test", CapabilityState.VERIFIED, state)
            assertNotEquals(CapabilityState.TESTED, state)
        }
    }

    @Test
    fun `vulkan detected is AVAILABLE and carries renderer identity`() {
        val vk = eval(targetDevice).of(Feature.GPU_VULKAN)
        assertEquals(CapabilityState.AVAILABLE, vk.state)
        assertEquals("Adreno (TM) 750", vk.evidence.rendererIdentity)
        assertEquals("Vulkan 1.3.0", vk.evidence.api)
    }

    @Test
    fun `compositor before any run says so explicitly`() {
        val c = eval(targetDevice).of(Feature.GPU_COMPOSITOR)
        assertEquals(CapabilityState.AVAILABLE, c.state)
        assertTrue(c.notes.any { it.contains("runtime test has not run") })
    }

    // ---------- compositor states from runtime evidence ----------

    @Test
    fun `compositor becomes VERIFIED only with hardware frames plus measurements`() {
        val perf = PerformanceSample.fromFrameTimes(
            List(60) { 16.6 },
            renderer = "Adreno (TM) 750",
            api = "OpenGL ES 3.2",
        )
        val report = CapabilityEvaluator.evaluateWithCompositor(
            targetDevice,
            CapabilityEvidence(
                detected = true,
                initialized = true,
                rendererImplemented = true,
                executed = true,
                frameProduced = true,
                hardwareBacked = true,
                rendererIdentity = "Adreno (TM) 750",
            ),
            perf,
        )
        val c = report.of(Feature.GPU_COMPOSITOR)
        assertEquals(CapabilityState.VERIFIED, c.state)
        assertEquals(60, c.performance?.frames)
        assertTrue(c.detail.contains("Adreno"))
    }

    @Test
    fun `compositor without measurements is TESTED not VERIFIED`() {
        val report = CapabilityEvaluator.evaluateWithCompositor(
            targetDevice,
            CapabilityEvidence(executed = true, frameProduced = true, hardwareBacked = true),
        )
        assertEquals(CapabilityState.TESTED, report.of(Feature.GPU_COMPOSITOR).state)
    }

    @Test
    fun `software rendering is LIMITED rather than verified`() {
        val report = CapabilityEvaluator.evaluateWithCompositor(
            targetDevice,
            CapabilityEvidence(executed = true, frameProduced = true, hardwareBacked = false),
        )
        val c = report.of(Feature.GPU_COMPOSITOR)
        assertEquals(CapabilityState.LIMITED, c.state)
        assertTrue(c.notes.any { it.contains("software") })
    }

    @Test
    fun `a failed gpu run is FAILED even if the API exists`() {
        val report = CapabilityEvaluator.evaluateWithCompositor(
            targetDevice,
            CapabilityEvidence(executed = true, frameProduced = false),
        )
        assertEquals(CapabilityState.FAILED, report.of(Feature.GPU_COMPOSITOR).state)
    }

    // ---------- device-specific expectations ----------

    @Test
    fun `target device reports AVF unsupported because it is not declared`() {
        val avf = eval(targetDevice).of(Feature.VM_AVF_HARDWARE)
        assertEquals(CapabilityState.UNSUPPORTED, avf.state)
        assertTrue(avf.detail.contains("virtualization_framework"))
    }

    @Test
    fun `target device reports QEMU as LIMITED software emulation`() {
        val q = eval(targetDevice).of(Feature.VM_QEMU_TCG)
        assertEquals(CapabilityState.LIMITED, q.state)
        assertTrue(q.notes.any { it.contains("software emulation") })
    }

    @Test
    fun `proot is AVAILABLE but documented as not virtualization`() {
        val p = eval(targetDevice).of(Feature.LINUX_PROOT)
        assertEquals(CapabilityState.AVAILABLE, p.state)
        assertTrue(p.notes.any { it.contains("not virtualization") })
    }

    @Test
    fun `linux gui is LIMITED with software rendering stated`() {
        val g = eval(targetDevice).of(Feature.LINUX_GUI)
        assertEquals(CapabilityState.LIMITED, g.state)
        assertTrue(g.notes.any { it.contains("software") })
    }

    @Test
    fun `desktop and window manager are at least TESTED`() {
        val report = eval(targetDevice)
        assertEquals(CapabilityState.TESTED, report.of(Feature.DESKTOP_SHELL).state)
        assertEquals(CapabilityState.TESTED, report.of(Feature.WINDOW_MANAGER).state)
        assertEquals(CapabilityState.TESTED, report.of(Feature.TASKBAR).state)
        assertTrue(report.of(Feature.WINDOW_MANAGER).state.isUsable)
    }

    @Test
    fun `target device profile allows guests and caps guest ram at a quarter`() {
        val profile = eval(targetDevice).profile
        assertEquals(ProfileId.PERFORMANCE, profile.id)
        assertTrue(profile.allowGuests)
        assertEquals(2_775L, profile.maxGuestRamMb)
    }

    // ---------- gating ----------

    @Test
    fun `low sdk is unsupported for the desktop`() {
        assertEquals(CapabilityState.UNSUPPORTED, eval(targetDevice.copy(sdkInt = 21)).of(Feature.DESKTOP_SHELL).state)
    }

    @Test
    fun `low storage blocks proot with the numbers in the message`() {
        val p = eval(targetDevice.copy(storageFreeMb = 900)).of(Feature.LINUX_PROOT)
        assertEquals(CapabilityState.UNSUPPORTED, p.state)
        assertTrue(p.detail.contains("900"))
    }

    @Test
    fun `gles2 only is unsupported for the gles3 feature`() {
        assertEquals(CapabilityState.UNSUPPORTED, eval(targetDevice.copy(glEsVersion = "2.0")).of(Feature.GPU_GLES3).state)
    }

    @Test
    fun `gles3_0 is limited not available`() {
        val g = eval(targetDevice.copy(glEsVersion = "3.0")).of(Feature.GPU_GLES3)
        assertEquals(CapabilityState.LIMITED, g.state)
    }

    @Test
    fun `missing vulkan probe is not reported as unsupported support`() {
        val vk = eval(targetDevice.copy(vulkanSupported = null)).of(Feature.GPU_VULKAN)
        assertEquals(CapabilityState.FAILED, vk.state)
        assertTrue(vk.detail.contains("inconclusive"))
    }

    @Test
    fun `low ram device loses guest capability`() {
        val profile = eval(targetDevice.copy(ramTotalMb = 3_000)).profile
        assertTrue(!profile.allowGuests)
        assertEquals(0L, profile.maxGuestRamMb)
    }

    @Test
    fun `thermal severe throttles heavy work`() {
        val profile = eval(targetDevice.copy(thermalStatus = CapabilityPolicy.THERMAL_SEVERE)).profile
        assertTrue(profile.thermalThrottled)
        assertTrue(!profile.allowGuests)
    }

    @Test
    fun `guest ram never exceeds a quarter of ram or four gibibytes`() {
        assertEquals(2_775L, PerformanceProfile.computeGuestRam(11_100))
        assertEquals(4_096L, PerformanceProfile.computeGuestRam(64_000))
        assertEquals(0L, PerformanceProfile.computeGuestRam(0))
    }

    @Test
    fun `user preference can only lower the profile`() {
        val derived = eval(targetDevice).profile
        val capped = PerformanceProfile.capTo(derived, ProfileId.BATTERY_SAVER)
        assertEquals(ProfileId.BATTERY_SAVER, capped.id)
        assertTrue(!capped.allowGuests)
        val cannotRaise = PerformanceProfile.capTo(
            eval(targetDevice.copy(ramTotalMb = 3_000)).profile,
            ProfileId.PERFORMANCE,
        )
        assertEquals(ProfileId.BALANCED, cannotRaise.id)
    }

    @Test
    fun `every feature is evaluated exactly once`() {
        val report = eval(targetDevice)
        assertEquals(Feature.entries.size, report.capabilities.size)
        assertEquals(Feature.entries.toSet(), report.capabilities.map { it.feature }.toSet())
    }

    // ---------- performance sample maths ----------

    @Test
    fun `performance sample computes fps and percentiles`() {
        val frames = listOf(10.0, 10.0, 10.0, 40.0, 20.0)
        val s = PerformanceSample.fromFrameTimes(frames, "Adreno", "OpenGL ES 3.2")
        assertEquals(5, s.frames)
        assertEquals(90.0, s.elapsedMs, 1e-9)
        assertEquals(5 * 1000.0 / 90.0, s.avgFps, 1e-9)
        assertEquals(40.0, s.maxFrameMs, 1e-9)
        assertEquals(18.0, s.avgFrameMs, 1e-9)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `performance sample rejects zero frames`() {
        PerformanceSample.fromFrameTimes(emptyList(), "x", "y")
    }
}
