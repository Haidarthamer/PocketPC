package dev.pocketpc.graphics

import dev.pocketpc.system.capability.CapabilityEvaluator
import dev.pocketpc.system.capability.CapabilityState
import dev.pocketpc.system.capability.Feature
import dev.pocketpc.system.capability.HardwareSnapshot
import dev.pocketpc.system.capability.PerformanceSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the GPU verdict logic.
 *
 * These run on a plain JVM: they do not need a GPU because they assert the *rules* that
 * decide what a set of raw observations means. The observations themselves come from the
 * on-device EGL workload in [GlesVerificationEngine].
 */
class SoftwareRendererDetectorTest {

    @Test
    fun `known software rasterisers are detected`() {
        listOf(
            "SwiftShader Device (Subzero)",
            "Android Emulator OpenGL ES Translator (Google SwiftShader)",
            "llvmpipe (LLVM 15.0.7, 256 bits)",
            "softpipe",
        ).forEach { renderer ->
            assertTrue("$renderer should be software", SoftwareRendererDetector.isSoftware(renderer, "Google"))
        }
    }

    @Test
    fun `real mobile gpus are not labelled software`() {
        listOf(
            "Adreno (TM) 750" to "Qualcomm",
            "Mali-G715" to "ARM",
            "PowerVR Rogue GE8320" to "Imagination Technologies",
            "Xclipse 940" to "Samsung",
        ).forEach { (renderer, vendor) ->
            assertFalse("$renderer misdetected as software", SoftwareRendererDetector.isSoftware(renderer, vendor))
            assertTrue("$renderer should be plausible hardware", SoftwareRendererDetector.isPlausiblyHardware(renderer, vendor))
        }
    }

    @Test
    fun `null renderer is not hardware and not claimed as software`() {
        assertFalse(SoftwareRendererDetector.isPlausiblyHardware(null, null))
        // An unknown renderer must not be called software: that would be an unverified claim.
        assertFalse(SoftwareRendererDetector.isSoftware(null, null))
    }

    @Test
    fun `unknown renderer strings are not accepted as hardware evidence`() {
        assertFalse(SoftwareRendererDetector.isPlausiblyHardware("Some Random Renderer 1.0", "Unknown Vendor"))
    }
}

class GpuVerifierTest {

    private fun evidence(
        renderer: String? = "Adreno (TM) 750",
        vendor: String? = "Qualcomm",
        context: Boolean = true,
        executed: Boolean = true,
        frame: Boolean = true,
        frames: Int = 120,
        glError: Int? = 0,
    ) = GpuVerifier.buildEvidence(
        api = "OpenGL ES 3.2",
        renderer = renderer,
        vendor = vendor,
        contextCreated = context,
        commandsExecuted = executed,
        frameProduced = frame,
        frames = frames,
        glError = glError,
    )

    @Test
    fun `full successful run is hardware backed`() {
        val ev = evidence()
        assertEquals(true, ev.initialized)
        assertEquals(true, ev.executed)
        assertEquals(true, ev.frameProduced)
        assertEquals(true, ev.hardwareBacked)
        assertEquals("Adreno (TM) 750", ev.rendererIdentity)
    }

    @Test
    fun `software renderer is never hardware backed even when it draws correctly`() {
        val ev = evidence(renderer = "SwiftShader Device (Subzero)", vendor = "Google")
        assertEquals(true, ev.frameProduced)
        assertEquals("a software rasteriser reported a correct frame", false, ev.hardwareBacked)
    }

    @Test
    fun `zero frames means nothing was executed`() {
        val ev = evidence(frames = 0)
        assertEquals(false, ev.executed)
        assertEquals(false, ev.hardwareBacked)
    }

    @Test
    fun `failed context cannot be hardware backed`() {
        val ev = evidence(context = false, executed = false, frame = false, frames = 0)
        assertEquals(false, ev.initialized)
        assertEquals(false, ev.hardwareBacked)
    }

    @Test
    fun `a gl error is recorded on the evidence`() {
        val ev = evidence(glError = 0x0502)
        assertEquals("GL error 0x502", ev.driverVersion)
    }

    @Test
    fun `summary states every step explicitly`() {
        val lines = GpuVerifier.summarize(evidence(), null)
        assertTrue(lines.any { it.startsWith("API:") })
        assertTrue(lines.any { it.contains("Renderer: Adreno") })
        assertTrue(lines.any { it.contains("Hardware-backed: true") })
        assertTrue(lines.any { it.contains("No frame timing") })
    }

    @Test
    fun `summary includes measurement when present`() {
        val perf = PerformanceSample.fromFrameTimes(List(60) { 16.0 }, "Adreno (TM) 750", "OpenGL ES 3.2")
        val lines = GpuVerifier.summarize(evidence(), perf)
        assertTrue(lines.any { it.contains("fps") && it.contains("60 frames") })
    }
}

/**
 * End-to-end capability reporting: raw GPU observations must map onto the right
 * eight-state verdict, and nothing may reach VERIFIED without measurements.
 */
class GpuCapabilityIntegrationTest {

    private val device = HardwareSnapshot(
        abi = "arm64-v8a",
        cpuCores = 8,
        ramTotalMb = 11_100,
        storageFreeMb = 11_786,
        sdkInt = 36,
        gpuRenderer = "Adreno (TM) 750",
        gpuVendor = "Qualcomm",
        glEsVersion = "3.2",
        vulkanSupported = true,
        vulkanApiVersion = "1.3.0",
        avfDeclared = false,
        kvmPresent = false,
    )

    @Test
    fun `before any run the compositor is AVAILABLE and claims no acceleration`() {
        val cap = CapabilityEvaluator.evaluate(device).of(Feature.GPU_COMPOSITOR)
        assertEquals(CapabilityState.AVAILABLE, cap.state)
        assertNotEquals(CapabilityState.VERIFIED, cap.state)
    }

    @Test
    fun `measured hardware run reaches VERIFIED with real numbers`() {
        val ev = GpuVerifier.buildEvidence(
            api = "OpenGL ES 3.2",
            renderer = "Adreno (TM) 750",
            vendor = "Qualcomm",
            contextCreated = true,
            commandsExecuted = true,
            frameProduced = true,
            frames = 120,
            glError = 0,
        )
        val perf = PerformanceSample.fromFrameTimes(List(120) { 8.3 }, "Adreno (TM) 750", "OpenGL ES 3.2")
        val cap = CapabilityEvaluator.evaluateWithCompositor(device, ev, perf).of(Feature.GPU_COMPOSITOR)
        assertEquals(CapabilityState.VERIFIED, cap.state)
        assertEquals(120, cap.performance?.frames)
        assertTrue((cap.performance?.avgFps ?: 0.0) > 100.0)
    }

    @Test
    fun `software emulator run is LIMITED and says so in the notes`() {
        val ev = GpuVerifier.buildEvidence(
            api = "OpenGL ES 3.0",
            renderer = "SwiftShader Device (Subzero)",
            vendor = "Google",
            contextCreated = true,
            commandsExecuted = true,
            frameProduced = true,
            frames = 120,
            glError = 0,
        )
        val perf = PerformanceSample.fromFrameTimes(List(120) { 40.0 }, "SwiftShader", "OpenGL ES 3.0")
        val cap = CapabilityEvaluator.evaluateWithCompositor(device, ev, perf).of(Feature.GPU_COMPOSITOR)
        assertEquals(CapabilityState.LIMITED, cap.state)
        assertTrue(cap.notes.any { it.contains("software") })
    }

    @Test
    fun `a run that produced no frame is FAILED`() {
        val ev = GpuVerifier.buildEvidence(
            api = "OpenGL ES 3.2",
            renderer = null,
            vendor = null,
            contextCreated = false,
            commandsExecuted = false,
            frameProduced = false,
            frames = 0,
            glError = 0x3003,
        )
        val cap = CapabilityEvaluator.evaluateWithCompositor(device, ev).of(Feature.GPU_COMPOSITOR)
        assertEquals(CapabilityState.FAILED, cap.state)
    }
}
