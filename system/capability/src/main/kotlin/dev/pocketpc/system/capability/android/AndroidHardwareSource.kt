package dev.pocketpc.system.capability.android

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.StatFs
import dev.pocketpc.system.capability.HardwareSnapshot
import dev.pocketpc.system.capability.HardwareSource
import java.io.File

/**
 * Reads the real device through Android APIs.
 *
 * Every read is individually guarded: a failing probe is recorded in
 * [HardwareSnapshot.probeWarnings] and its field left unknown. Nothing is inferred
 * from the marketing name of the device.
 */
class AndroidHardwareSource(private val context: Context) : HardwareSource {

    override fun snapshot(): HardwareSnapshot {
        val warnings = mutableListOf<String>()

        val pm = context.packageManager
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        if (am == null) warnings += "ActivityManager unavailable"

        val memoryInfo = ActivityManager.MemoryInfo()
        runCatching { am?.getMemoryInfo(memoryInfo) }
            .onFailure { warnings += "getMemoryInfo failed: ${it.message}" }

        val glEsVersion = runCatching { am?.deviceConfigurationInfo?.glEsVersion }
            .onFailure { warnings += "glEsVersion unavailable: ${it.message}" }
            .getOrNull()

        val gl = EglGraphicsProbe.probe()
        gl.error?.let { warnings += "GL: $it" }

        val vulkan = readVulkan(pm, warnings)

        val avf = runCatching {
            pm.hasSystemFeature(PackageManager.FEATURE_VIRTUALIZATION_FRAMEWORK)
        }.onFailure { warnings += "AVF feature check failed: ${it.message}" }.getOrNull()

        return HardwareSnapshot(
            abi = Build.SUPPORTED_ABIS.firstOrNull() ?: Build.CPU_ABI,
            supportedAbis = Build.SUPPORTED_ABIS?.toList().orEmpty(),
            cpuCores = Runtime.getRuntime().availableProcessors(),
            cpuMaxFreqKHz = readCpuMaxFreqKhz(warnings),
            ramTotalMb = if (memoryInfo.totalMem > 0) memoryInfo.totalMem / (1024 * 1024) else 0L,
            storageFreeMb = readFreeStorageMb(warnings),
            sdkInt = Build.VERSION.SDK_INT,
            deviceModel = Build.MODEL.orEmpty(),
            deviceManufacturer = Build.MANUFACTURER.orEmpty(),
            gpuVendor = gl.vendor,
            gpuRenderer = gl.renderer,
            glEsVersion = glEsVersion,
            vulkanSupported = vulkan.supported,
            vulkanApiVersion = vulkan.apiVersion,
            avfDeclared = avf,
            kvmPresent = readKvm(warnings),
            thermalStatus = readThermalStatus(warnings),
            probeWarnings = warnings,
            probedAtEpochMs = System.currentTimeMillis(),
        )
    }

    private data class VulkanInfo(val supported: Boolean?, val apiVersion: String?)

    /**
     * Uses the platform feature flags rather than opening a Vulkan device: this is a
     * declaration-level signal. A real device+swapchain probe belongs to Phase 4.
     */
    private fun readVulkan(pm: PackageManager, warnings: MutableList<String>): VulkanInfo {
        return try {
            val features = pm.systemAvailableFeatures
            val hwVersion = features.firstOrNull {
                it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION
            }
            val hasLoader = pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL) ||
                hwVersion != null
            val versionString = hwVersion?.version?.let { v ->
                // The platform encodes the Vulkan version as major<<22 | minor<<12 | patch.
                val major = v shr 22
                val minor = (v shr 12) and 0x3FF
                val patch = v and 0xFFF
                "$major.$minor.$patch"
            }
            VulkanInfo(hasLoader, versionString)
        } catch (t: Throwable) {
            warnings += "Vulkan feature query failed: ${t.message}"
            VulkanInfo(null, null)
        }
    }

    private fun readCpuMaxFreqKhz(warnings: MutableList<String>): Long {
        // cpu0 may be a little core; take the maximum across the clusters we can read.
        var best = 0L
        val cores = Runtime.getRuntime().availableProcessors()
        for (i in 0 until cores) {
            val f = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
            val v = runCatching { f.readText().trim().toLong() }.getOrNull() ?: continue
            if (v > best) best = v
        }
        if (best == 0L) warnings += "cpuinfo_max_freq unreadable on all cores"
        return best
    }

    private fun readFreeStorageMb(warnings: MutableList<String>): Long = try {
        val stat = StatFs(context.filesDir.absolutePath)
        stat.availableBytes / (1024 * 1024)
    } catch (t: Throwable) {
        warnings += "StatFs failed: ${t.message}"
        0L
    }

    /**
     * /dev/kvm is normally not world-readable on Android; an app-visible "false" here
     * means "not accessible to this app", which is exactly the honest claim to make.
     */
    private fun readKvm(warnings: MutableList<String>): Boolean? = try {
        val kvm = File("/dev/kvm")
        kvm.exists() && kvm.canRead()
    } catch (t: Throwable) {
        warnings += "kvm probe failed: ${t.message}"
        null
    }

    private fun readThermalStatus(warnings: MutableList<String>): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                ?: return null
            @Suppress("DEPRECATION")
            pm.currentThermalStatus
        } catch (t: Throwable) {
            warnings += "thermal status unavailable: ${t.message}"
            null
        }
    }
}
