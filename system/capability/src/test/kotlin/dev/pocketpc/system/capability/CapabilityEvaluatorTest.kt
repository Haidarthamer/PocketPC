package dev.pocketpc.system.capability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the anti-overclaiming rules. These encode the project constitution:
 * absent evidence must never be rendered as support.
 */
class CapabilityEvaluatorTest {

    private val targetDevice = HardwareSnapshot(
        abi = "arm64-v8a",
        supportedAbis = listOf("arm64-v8a", "armeabi-v7a"),
        cpuCores = 8,
        cpuMaxFreqKHz = 3_300_000,
        ramTotalMb = 11_600,
        storageFreeMb = 180_000,
        sdkInt = 35,
        deviceModel = "POCO F7 Pro",
        deviceManufacturer = "Xiaomi",
        gpuVendor = "Qualcomm",
        gpuRenderer = "Adreno (TM) 750",
        glEsVersion = "3.2",
        vulkanSupported = true,
        vulkanApiVersion = "1.3.0",
        avfDeclared = true,
        kvmPresent = true,
        thermalStatus = 0,
    )

    private fun eval(s: HardwareSnapshot) = CapabilityEvaluator.evaluate(s)

    @Test
    fun `unprobed snapshot yields unknowns not support`() {
        val report = eval(HardwareSnapshot.unprobed("probe crashed"))
        assertEquals(Availability.Unknown::class.java, report.features[Feature.GPU_VULKAN]!!.javaClass)
        assertFalse(report.features[Feature.GPU_VULKAN]!!.isUsable)
        assertFalse(report.features[Feature.VM_AVF_HARDWARE]!!.isUsable)
        assertFalse(report.features[Feature.LINUX_PROOT]!!.isUsable)
    }

    @Test
    fun `missing vulkan probe is unknown while explicit false is unsupported`() {
        val unknown = eval(targetDevice.copy(vulkanSupported = null)).features[Feature.GPU_VULKAN]!!
        val missing = eval(targetDevice.copy(vulkanSupported = false)).features[Feature.GPU_VULKAN]!!
        assertTrue(unknown is Availability.Unknown)
        assertTrue(missing is Availability.Unsupported)
    }

    @Test
    fun `api availability never counts as verified acceleration`() {
        val gpu = eval(targetDevice).features[Feature.GPU_ACCELERATED_COMPOSITING]!!
        assertTrue(gpu.isUsable)
        assertFalse(
            "An available graphics API must not be reported as verified GPU acceleration",
            gpu.isVerified,
        )
    }

    @Test
    fun `vulkan probe is evidence but not verification`() {
        val vk = eval(targetDevice).features[Feature.GPU_VULKAN]!!
        assertTrue(vk is Availability.Supported)
        assertEquals(Evidence.RUNTIME_PROBE, (vk as Availability.Supported).evidence)
        assertFalse(vk.isVerified)
    }

    @Test
    fun `low sdk is unsupported`() {
        val report = eval(targetDevice.copy(sdkInt = 21))
        assertTrue(report.features[Feature.DESKTOP_SHELL] is Availability.Unsupported)
    }

    @Test
    fun `low storage blocks proot`() {
        val report = eval(targetDevice.copy(storageFreeMb = 900))
        assertTrue(report.features[Feature.LINUX_PROOT] is Availability.Unsupported)
    }

    @Test
    fun `proot is documented as userspace not virtualization`() {
        val proot = eval(targetDevice).features[Feature.LINUX_PROOT]!!
        assertTrue(proot is Availability.Supported)
        val notes = (proot as Availability.Supported).notes.joinToString(" ")
        assertTrue(notes.contains("not virtualization", ignoreCase = true))
    }

    @Test
    fun `linux gui is degraded because guest gpu is unproven`() {
        val gui = eval(targetDevice).features[Feature.LINUX_GUI]!!
        assertTrue(gui is Availability.Degraded)
        assertFalse(gui.isVerified)
    }

    @Test
    fun `qemu tcg is degraded not fast`() {
        val qemu = eval(targetDevice).features[Feature.VM_QEMU_TCG]!!
        assertTrue(qemu is Availability.Degraded)
        assertTrue((qemu as Availability.Degraded).limitation.contains("Without KVM"))
    }

    @Test
    fun `avf unsupported when not declared and unknown when unread`() {
        assertTrue(
            eval(targetDevice.copy(avfDeclared = false)).features[Feature.VM_AVF_HARDWARE] is Availability.Unsupported,
        )
        assertTrue(
            eval(targetDevice.copy(avfDeclared = null)).features[Feature.VM_AVF_HARDWARE] is Availability.Unknown,
        )
    }

    @Test
    fun `avf declared but kvm absent is degraded not supported`() {
        val avf = eval(targetDevice.copy(kvmPresent = false)).features[Feature.VM_AVF_HARDWARE]!!
        assertTrue(avf is Availability.Degraded)
    }

    @Test
    fun `gles2 only is unsupported for gles3 feature`() {
        val g = eval(targetDevice.copy(glEsVersion = "2.0")).features[Feature.GPU_GLES3]!!
        assertTrue(g is Availability.Unsupported)
    }

    @Test
    fun `low ram device loses guest capability`() {
        val profile = eval(targetDevice.copy(ramTotalMb = 3_000)).profile
        assertFalse(profile.allowGuests)
        assertEquals(0L, profile.maxGuestRamMb)
    }

    @Test
    fun `thermal severe throttles heavy work`() {
        val profile = eval(targetDevice.copy(thermalStatus = CapabilityPolicy.THERMAL_SEVERE)).profile
        assertTrue(profile.thermalThrottled)
        assertFalse(profile.allowGuests)
    }

    @Test
    fun `guest ram never exceeds a quarter of ram or four gibibytes`() {
        assertEquals(2_900L, PerformanceProfile.computeGuestRam(11_600))
        assertEquals(4_096L, PerformanceProfile.computeGuestRam(64_000))
        assertEquals(0L, PerformanceProfile.computeGuestRam(0))
    }

    @Test
    fun `user preference can only lower the profile`() {
        val derived = eval(targetDevice).profile
        val capped = PerformanceProfile.capTo(derived, ProfileId.BATTERY_SAVER)
        assertEquals(ProfileId.BATTERY_SAVER, capped.id)
        assertFalse(capped.allowGuests)

        val cannotRaise = PerformanceProfile.capTo(
            eval(targetDevice.copy(ramTotalMb = 3_000)).profile,
            ProfileId.PERFORMANCE,
        )
        assertEquals(ProfileId.BALANCED, cannotRaise.id)
    }

    @Test
    fun `target device report is internally consistent`() {
        val report = eval(targetDevice)
        assertTrue(report.features.values.any { it is Availability.Unknown }.not())
        assertTrue(report.snapshot.gaps().isEmpty())
    }
}
