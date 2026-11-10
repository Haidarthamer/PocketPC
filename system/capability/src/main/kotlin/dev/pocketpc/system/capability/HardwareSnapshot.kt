package dev.pocketpc.system.capability

/**
 * A raw, un-interpreted snapshot of what the device reports about itself.
 *
 * This type deliberately holds *observations only*. It contains no policy and no
 * feature decisions; those live in [CapabilityEvaluator]. Keeping the two apart is
 * what makes the decision logic unit-testable on a plain JVM and prevents device
 * capabilities from being hardcoded into the product.
 *
 * Nullable / zero fields mean "the probe could not observe this", never "the device
 * does not have it". Every such gap must surface in [probeWarnings] and must degrade
 * the affected features to [Availability.Unknown], never to a silent "supported".
 */
public data class HardwareSnapshot(
    /** Primary ABI of this process, e.g. "arm64-v8a". */
    val abi: String? = null,
    /** All ABIs the platform supports, most preferred first. */
    val supportedAbis: List<String> = emptyList(),
    val cpuCores: Int = 0,
    /** Maximum hardware-core clock in kHz, 0 when unreadable. */
    val cpuMaxFreqKHz: Long = 0L,
    /** Total physical RAM in MiB as reported to applications, 0 when unreadable. */
    val ramTotalMb: Long = 0L,
    /** Free bytes on the app-private data volume, 0 when unreadable. */
    val storageFreeMb: Long = 0L,
    /** Build.VERSION.SDK_INT. */
    val sdkInt: Int = 0,
    val deviceModel: String = "",
    val deviceManufacturer: String = "",
    /** GL_VENDOR, e.g. "Qualcomm". Null when no GL context could be created. */
    val gpuVendor: String? = null,
    /** GL_RENDERER, e.g. "Adreno (TM) 750". Null when no GL context could be created. */
    val gpuRenderer: String? = null,
    /** OpenGL ES version string as reported by ActivityManager, e.g. "3.2". */
    val glEsVersion: String? = null,
    /** True when a Vulkan instance/device could be created. Null when the probe failed. */
    val vulkanSupported: Boolean? = null,
    /** Highest Vulkan API version the loader reports, e.g. "1.3.0". */
    val vulkanApiVersion: String? = null,
    /** True when the platform declares android.software.virtualization_framework. */
    val avfDeclared: Boolean? = null,
    /** True when /dev/kvm is present and openable. Null when unchecked. */
    val kvmPresent: Boolean? = null,
    /**
     * PowerManager thermal status (0 NONE .. 6 SHUTDOWN) at probe time, or null.
     * This is a point-in-time reading, not a capability.
     */
    val thermalStatus: Int? = null,
    /** Non-fatal probe failures. Never swallow a failed probe instead of recording it. */
    val probeWarnings: List<String> = emptyList(),
    val probedAtEpochMs: Long = 0L,
) {
    /** Unreadable probes that must not be silently treated as "absent". */
    public fun gaps(): List<String> {
        val out = mutableListOf<String>()
        if (abi.isNullOrBlank()) out += "abi"
        if (cpuCores <= 0) out += "cpuCores"
        if (ramTotalMb <= 0L) out += "ramTotalMb"
        if (storageFreeMb <= 0L) out += "storageFreeMb"
        if (sdkInt <= 0) out += "sdkInt"
        if (gpuRenderer.isNullOrBlank()) out += "gpuRenderer"
        if (glEsVersion.isNullOrBlank()) out += "glEsVersion"
        if (vulkanSupported == null) out += "vulkanSupported"
        if (avfDeclared == null) out += "avfDeclared"
        return out
    }

    public companion object {
        /** Snapshot used when a probe fails entirely: every field unknown. */
        public fun unprobed(reason: String): HardwareSnapshot =
            HardwareSnapshot(probeWarnings = listOf(reason))
    }
}

/** Source of [HardwareSnapshot] values. Production: Android APIs. Tests: fakes. */
public fun interface HardwareSource {
    public fun snapshot(): HardwareSnapshot
}
