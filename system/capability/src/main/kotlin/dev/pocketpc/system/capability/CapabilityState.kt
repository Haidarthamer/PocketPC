package dev.pocketpc.system.capability

/**
 * The lifecycle a capability moves through, from "the platform mentions it" to
 * "we have proven it renders on this hardware".
 *
 * This replaces the earlier binary `isVerified` flag, which allowed the UI to show
 * SUPPORTED while nothing had actually run. States are strictly ordered: a capability
 * may never report a later state than its evidence supports.
 */
public enum class CapabilityState {
    /** The platform or a probe says the API exists. Nothing of ours has run. */
    DETECTED,

    /** The API exists *and* our code can reach it (feature flag + probe both positive). */
    AVAILABLE,

    /** Pocket PC contains a real implementation of this capability. */
    IMPLEMENTED,

    /** Automated tests that exercise the implementation pass. */
    TESTED,

    /** A real workload executed on this device and produced correct, hardware-backed output. */
    VERIFIED,

    /** Works, but with a documented penalty or missing sub-feature. */
    LIMITED,

    /** Cannot work on this device; a reason is recorded. */
    UNSUPPORTED,

    /** Our implementation ran and failed on this device. */
    FAILED,
    ;

    /** Terminal states that describe a real outcome rather than a promise. */
    public val isOutcome: Boolean
        get() = this == VERIFIED || this == LIMITED || this == UNSUPPORTED || this == FAILED

    /** Whether the user can rely on the feature being present. */
    public val isUsable: Boolean
        get() = this == AVAILABLE || this == IMPLEMENTED || this == TESTED || this == VERIFIED || this == LIMITED
}

/**
 * Evidence attached to a capability. Each field is an independent fact, never derived
 * from another unless documented. Rendering capabilities fill the graphics fields;
 * non-graphics capabilities leave them null.
 */
public data class CapabilityEvidence(
    /** The platform declares / the probe observes the API. */
    val detected: Boolean = false,
    /** Initialisation succeeded (instance/context/feature created). */
    val initialized: Boolean? = null,
    /** Pocket PC ships a renderer or engine for this. */
    val rendererImplemented: Boolean? = null,
    /** Our commands actually executed on the device. */
    val executed: Boolean? = null,
    /** The executed workload produced a correct frame (not merely "no error"). */
    val frameProduced: Boolean? = null,
    /** The draw calls were served by hardware, i.e. not a software rasteriser. */
    val hardwareBacked: Boolean? = null,
    /** Renderer identity reported by the driver, e.g. "Adreno (TM) 750". */
    val rendererIdentity: String? = null,
    /** Graphics API, e.g. "Vulkan 1.3.0" or "OpenGL ES 3.2". */
    val api: String? = null,
    /** Driver/library version string when the API exposes one. */
    val driverVersion: String? = null,
)

/** One capability's honest state plus the measurements behind it. */
public data class CapabilityReport(
    public val feature: Feature,
    public val state: CapabilityState,
    /** Human-readable basis or reason. Shown verbatim in the UI. */
    public val detail: String,
    public val evidence: CapabilityEvidence = CapabilityEvidence(),
    /** Measured performance, when the capability was actually executed. */
    public val performance: PerformanceSample? = null,
    public val notes: List<String> = emptyList(),
)

/** Measurements from a real execution. Null fields mean "not measured". */
public data class PerformanceSample(
    val frames: Int,
    val elapsedMs: Double,
    val avgFps: Double,
    val avgFrameMs: Double,
    val p99FrameMs: Double,
    val maxFrameMs: Double,
    /** Renderer string observed during this run, for cross-checking. */
    val renderer: String,
    val api: String,
) {
    public companion object {
        /** Builds a sample from per-frame durations. Requires a real frame count. */
        public fun fromFrameTimes(frameMs: List<Double>, renderer: String, api: String): PerformanceSample {
            require(frameMs.isNotEmpty()) { "cannot compute performance from zero frames" }
            val sorted = frameMs.sorted()
            val total = frameMs.sum()
            return PerformanceSample(
                frames = frameMs.size,
                elapsedMs = total,
                avgFps = if (total > 0) frameMs.size * 1000.0 / total else 0.0,
                avgFrameMs = total / frameMs.size,
                p99FrameMs = sorted[((sorted.size - 1) * 0.99).toInt()],
                maxFrameMs = sorted.last(),
                renderer = renderer,
                api = api,
            )
        }
    }
}
