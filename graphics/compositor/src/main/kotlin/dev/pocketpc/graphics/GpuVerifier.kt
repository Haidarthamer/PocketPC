package dev.pocketpc.graphics

import dev.pocketpc.system.capability.CapabilityEvidence
import dev.pocketpc.system.capability.PerformanceSample

/**
 * Result of a GPU verification run.
 *
 * [evidence] is what the capability layer consumes; nothing here is inferred from the
 * mere existence of an API. In particular [hardwareBacked] is computed from the renderer
 * identity reported by the driver *and* the fact that real commands were executed.
 */
public data class GpuVerificationResult(
    public val evidence: CapabilityEvidence,
    public val performance: PerformanceSample?,
    /** Human-readable summary lines, shown verbatim in the UI. */
    public val report: List<String>,
    /** Frames whose centre pixel did not match the expected colour. */
    public val verificationFailures: Int,
    public val error: String? = null,
) {
    public val succeeded: Boolean get() = error == null && verificationFailures == 0
}

/**
 * Renderer families that are software rasterisers even though they expose OpenGL ES.
 * Matching one of these means the "GPU" path is running on the CPU.
 */
public object SoftwareRendererDetector {
    private val knownSoftware = listOf(
        "swiftshader",
        "llvmpipe",
        "softpipe",
        "angle (software",
        "google swiftshader",
        "mesa offscreen",
        "software rasterizer",
    )

    /** Vendor strings that indicate a CPU-backed implementation. */
    private val softwareVendors = listOf("google inc. (google)", "mesa project")

    /**
     * True when the reported renderer is a software rasteriser.
     *
     * Deliberately conservative: an unknown renderer is *not* labelled software (that
     * would be a false negative claim), but it is also not treated as proof of hardware.
     * Callers must combine this with [isPlausiblyHardware].
     */
    public fun isSoftware(renderer: String?, vendor: String?): Boolean {
        val r = renderer?.lowercase() ?: return false
        val v = vendor?.lowercase().orEmpty()
        if (knownSoftware.any { r.contains(it) }) return true
        // "Mesa" alone is not proof of software (Mesa also drives real GPUs), but Mesa
        // reporting llvmpipe/softpipe already matched above.
        return softwareVendors.any { v == it && r.contains("swiftshader") }
    }

    /**
     * Whether the renderer string looks like a real mobile GPU vendor. Used only to
     * strengthen the verdict, never to replace the executed-workload check.
     */
    public fun isPlausiblyHardware(renderer: String?, vendor: String?): Boolean {
        if (renderer.isNullOrBlank()) return false
        if (isSoftware(renderer, vendor)) return false
        val r = renderer.lowercase()
        val v = vendor?.lowercase().orEmpty()
        val vendorHints = listOf("adreno", "mali", "powervr", "xclipse", "immortalis", "apple", "nvidia", "intel", "radeon", "geforce")
        return vendorHints.any { r.contains(it) || v.contains(it) } ||
            listOf("qualcomm", "arm", "imagination", "samsung", "broadcom").any { v.contains(it) }
    }
}

/**
 * Pure verification-verdict logic, separated from EGL so it can be unit tested on a JVM
 * without a GPU. The Android side supplies the raw observations.
 */
public object GpuVerifier {

    /**
     * Builds the evidence record from raw run data.
     *
     * @param contextCreated EGL display + config + context all succeeded.
     * @param commandsExecuted draw calls were issued without a GL error.
     * @param frameProduced glReadPixels returned the expected pixel colour.
     * @param frames number of frames rendered (0 means nothing was measured).
     */
    public fun buildEvidence(
        api: String,
        renderer: String?,
        vendor: String?,
        contextCreated: Boolean,
        commandsExecuted: Boolean,
        frameProduced: Boolean,
        frames: Int,
        glError: Int?,
    ): CapabilityEvidence {
        val software = SoftwareRendererDetector.isSoftware(renderer, vendor)
        val plausibleHw = SoftwareRendererDetector.isPlausiblyHardware(renderer, vendor)
        return CapabilityEvidence(
            detected = true,
            initialized = contextCreated,
            rendererImplemented = true,
            executed = commandsExecuted && frames > 0,
            frameProduced = frameProduced,
            // Hardware-backed requires: a real context, executed work, a correct frame,
            // a plausibly hardware renderer, and no software rasteriser in the string.
            hardwareBacked = contextCreated && commandsExecuted && frameProduced && plausibleHw && !software,
            rendererIdentity = renderer,
            api = api,
            driverVersion = glError?.takeIf { it != 0 }?.let { "GL error 0x${Integer.toHexString(it)}" },
        )
    }

    public fun summarize(ev: CapabilityEvidence, perf: PerformanceSample?): List<String> = buildList {
        add("API: ${ev.api ?: "unknown"}")
        add("Renderer: ${ev.rendererIdentity ?: "unknown"}")
        add("Context created: ${ev.initialized}")
        add("Commands executed: ${ev.executed}")
        add("Frame verified by pixel readback: ${ev.frameProduced}")
        add("Hardware-backed: ${ev.hardwareBacked}")
        if (perf != null) {
            add(
                "Measured %.1f fps over %d frames (avg %.2f ms, p99 %.2f ms, max %.2f ms)"
                    .format(perf.avgFps, perf.frames, perf.avgFrameMs, perf.p99FrameMs, perf.maxFrameMs),
            )
        } else {
            add("No frame timing recorded")
        }
    }
}
