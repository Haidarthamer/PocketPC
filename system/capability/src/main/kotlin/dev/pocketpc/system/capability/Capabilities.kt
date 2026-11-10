package dev.pocketpc.system.capability

/** Product features whose availability depends on the host device. */
public enum class Feature {
    /** The desktop shell, taskbar and start menu. */
    DESKTOP_SHELL,

    /** The window manager (floating, resizable, z-ordered windows). */
    WINDOW_MANAGER,

    /** Rootless ARM64 Linux userspace via PRoot. Not virtualization. */
    LINUX_PROOT,

    /** Linux GUI apps through the display bridge. Requires LINUX_PROOT. */
    LINUX_GUI,

    /** Guest VMs via QEMU TCG (emulation: slow, no KVM requirement). */
    VM_QEMU_TCG,

    /** Guest VMs with genuine hardware virtualization through AVF/pKVM. */
    VM_AVF_HARDWARE,

    /** Vulkan rendering path for the compositor and applications. */
    GPU_VULKAN,

    /** OpenGL ES 3.x rendering path. */
    GPU_GLES3,

    /** GPU-accelerated desktop composition. Requires verified hardware rendering. */
    GPU_ACCELERATED_COMPOSITING,

    /** Live CPU / memory / thermal monitoring. */
    RESOURCE_MONITORING,
}

/**
 * How strong the evidence is that a feature works on this device.
 * Ordered from weakest to strongest. A feature may only be described as
 * "verified" when it reached [RUNTIME_VERIFIED] or [HARDWARE_TESTED].
 */
public enum class Evidence {
    /** The platform claims it is available; nothing has been exercised. */
    PLATFORM_DECLARATION,

    /** A probe ran and returned positive data (e.g. a Vulkan device was created). */
    RUNTIME_PROBE,

    /** A functional test executed successfully on this device. */
    RUNTIME_VERIFIED,

    /** Confirmed on real target hardware with recorded results. */
    HARDWARE_TESTED,
}

/** Result of evaluating one [Feature] against a [HardwareSnapshot]. */
public sealed class Availability {
    /** No decision possible: the probe was incomplete or inconclusive. */
    public data class Unknown(public val reason: String) : Availability()

    /** Technically possible on this device. [evidence] states how strongly this is known. */
    public data class Supported(
        public val evidence: Evidence,
        /** The unmet-or-met requirement this rests on, quoted in the UI. */
        public val basis: String,
        public val notes: List<String> = emptyList(),
    ) : Availability()

    /** Possible, but with a documented throughput or fidelity penalty. */
    public data class Degraded(
        public val evidence: Evidence,
        public val basis: String,
        public val limitation: String,
    ) : Availability()

    /** Not possible on this device, with a machine-checkable reason. */
    public data class Unsupported(public val reason: String) : Availability()

    public val isUsable: Boolean
        get() = this is Supported || this is Degraded

    /**
     * True only when a real test (not a platform claim) backed this feature.
     * Product code must use this before marketing a feature as working.
     */
    public val isVerified: Boolean
        get() = when (this) {
            is Supported -> evidence == Evidence.RUNTIME_VERIFIED || evidence == Evidence.HARDWARE_TESTED
            is Degraded -> evidence == Evidence.RUNTIME_VERIFIED || evidence == Evidence.HARDWARE_TESTED
            else -> false
        }
}

/** Explicit, documented resource policy. Thresholds are product policy, not device facts. */
public object CapabilityPolicy {
    /** Below this, the desktop is offered but guarded (fewer cached views, no VM). */
    public const val MIN_RAM_MB_FOR_DESKTOP: Long = 2_048L

    /** Comfortable threshold for multi-app desktop use. */
    public const val COMFORTABLE_RAM_MB: Long = 6_144L

    /** QEMU guests get RAM grants; require headroom before allowing one. */
    public const val MIN_RAM_MB_FOR_QEMU: Long = 4_096L

    /** PRoot needs a writable rootfs; refuse well before the disk is full. */
    public const val MIN_FREE_MB_FOR_ROOTFS: Long = 4_096L

    /** Android 8.0. Below this no supported target exists at all. */
    public const val MIN_SDK: Int = 26

    /** Android 14: AVF present in shipping devices and process-level limits are tighter. */
    public const val SDK_AVF_FLOOR: Int = 34

    /** Thermal status at which we stop starting new heavy work (PowerManager.THERMAL_STATUS_SEVERE). */
    public const val THERMAL_SEVERE: Int = 4
}

/** A full capability evaluation for one device. */
public data class CapabilityReport(
    public val snapshot: HardwareSnapshot,
    public val features: Map<Feature, Availability>,
    public val profile: PerformanceProfile,
)

/**
 * Pure decision logic: snapshot in, availability out.
 *
 * Rules enforced here (see docs/FEASIBILITY.md):
 *  - An unreadable probe yields [Availability.Unknown], never "supported".
 *  - "API exists" is never reported as "acceleration verified".
 *  - PRoot is never described as virtualization.
 */
public object CapabilityEvaluator {

    public fun evaluate(snapshot: HardwareSnapshot): CapabilityReport {
        val features = buildMap {
            put(Feature.DESKTOP_SHELL, desktopShell(snapshot))
            put(Feature.WINDOW_MANAGER, windowManager(snapshot))
            put(Feature.LINUX_PROOT, proot(snapshot))
            put(Feature.LINUX_GUI, linuxGui(snapshot))
            put(Feature.VM_QEMU_TCG, qemuTcg(snapshot))
            put(Feature.VM_AVF_HARDWARE, avfHardware(snapshot))
            put(Feature.GPU_VULKAN, vulkan(snapshot))
            put(Feature.GPU_GLES3, gles3(snapshot))
            put(Feature.GPU_ACCELERATED_COMPOSITING, acceleratedCompositing(snapshot))
            put(Feature.RESOURCE_MONITORING, resourceMonitoring(snapshot))
        }
        return CapabilityReport(
            snapshot = snapshot,
            features = features,
            profile = PerformanceProfile.derive(snapshot),
        )
    }

    private fun desktopShell(s: HardwareSnapshot): Availability {
        if (s.sdkInt <= 0) return Availability.Unknown("Android API level could not be read")
        if (s.sdkInt < CapabilityPolicy.MIN_SDK) {
            return Availability.Unsupported("Requires Android 8.0 (API ${CapabilityPolicy.MIN_SDK}); device reports API ${s.sdkInt}")
        }
        if (s.ramTotalMb <= 0L) {
            return Availability.Degraded(
                Evidence.PLATFORM_DECLARATION,
                "API ${s.sdkInt}",
                "Total RAM unreadable; shell runs without memory-based guards",
            )
        }
        if (s.ramTotalMb < CapabilityPolicy.MIN_RAM_MB_FOR_DESKTOP) {
            return Availability.Degraded(
                Evidence.PLATFORM_DECLARATION,
                "API ${s.sdkInt}",
                "Only ${s.ramTotalMb} MiB RAM: single-app desktop, no VMs",
            )
        }
        return Availability.Supported(
            Evidence.PLATFORM_DECLARATION,
            "API ${s.sdkInt}, ${s.ramTotalMb} MiB RAM",
        )
    }

    private fun windowManager(s: HardwareSnapshot): Availability = when (val shell = desktopShell(s)) {
        is Availability.Supported -> Availability.Supported(
            shell.evidence,
            shell.basis,
            listOf("Pointer capture and multi-window composition are exercised by instrumentation tests"),
        )
        is Availability.Degraded -> shell
        is Availability.Unsupported -> shell
        is Availability.Unknown -> shell
    }

    /**
     * PRoot needs executable native helpers for the device ABI plus a rootfs mount.
     * This evaluator can only establish preconditions; the environment is *working*
     * only after a distribution actually installs and runs `uname`/`apt` (Phase 5).
     */
    private fun proot(s: HardwareSnapshot): Availability {
        if (s.abi.isNullOrBlank()) return Availability.Unknown("Process ABI could not be read")
        if (s.sdkInt < 24) return Availability.Unsupported("PRoot requires API 24+ (exec support and SELinux policy)")
        val isArm = s.abi.startsWith("arm")
        val isX86 = s.abi.startsWith("x86")
        if (!isArm && !isX86) return Availability.Unsupported("Unsupported ABI for PRoot helpers: ${s.abi}")
        if (s.storageFreeMb <= 0L) return Availability.Unknown("Free storage could not be read; rootfs size unknown")
        if (s.storageFreeMb < CapabilityPolicy.MIN_FREE_MB_FOR_ROOTFS) {
            return Availability.Unsupported(
                "Needs at least ${CapabilityPolicy.MIN_FREE_MB_FOR_ROOTFS} MiB free for a rootfs; only ${s.storageFreeMb} MiB available",
            )
        }
        return Availability.Supported(
            Evidence.PLATFORM_DECLARATION,
            "ABI ${s.abi}, ${s.storageFreeMb} MiB free",
            listOf(
                "PRoot is a userspace emulation layer, not virtualization: the guest shares the Android kernel",
                "ptrace-based syscall interception has measurable overhead; no kernel modules, no real mount, limited /proc",
            ),
        )
    }

    private fun linuxGui(s: HardwareSnapshot): Availability = when (val p = proot(s)) {
        is Availability.Supported -> Availability.Degraded(
            Evidence.PLATFORM_DECLARATION,
            p.basis,
            "Graphical apps are displayed through a VNC/display bridge with software rendering by default; " +
                "guest GPU acceleration is not established by this check",
        )
        is Availability.Degraded -> p
        is Availability.Unsupported -> Availability.Unsupported("No Linux environment: ${p.reason}")
        is Availability.Unknown -> Availability.Unknown("Linux environment undetermined: ${p.reason}")
    }

    private fun qemuTcg(s: HardwareSnapshot): Availability {
        if (s.abi.isNullOrBlank()) return Availability.Unknown("Process ABI could not be read")
        if (s.ramTotalMb <= 0L) return Availability.Unknown("Total RAM unreadable; cannot size a guest")
        if (s.ramTotalMb < CapabilityPolicy.MIN_RAM_MB_FOR_QEMU) {
            return Availability.Unsupported(
                "Needs ${CapabilityPolicy.MIN_RAM_MB_FOR_QEMU} MiB RAM for a usable guest; device reports ${s.ramTotalMb} MiB",
            )
        }
        val host = if (s.abi.startsWith("arm") || s.abi.startsWith("x86")) "TCG" else "unsupported"
        if (host == "unsupported") return Availability.Unsupported("Unsupported host ABI ${s.abi}")
        return Availability.Degraded(
            Evidence.PLATFORM_DECLARATION,
            "QEMU user-mode/TCG, ${s.ramTotalMb} MiB RAM",
            "Without KVM every guest instruction is translated: expect seconds-per-boot-minute, not desktop speed",
        )
    }

    private fun avfHardware(s: HardwareSnapshot): Availability {
        if (s.avfDeclared == null) return Availability.Unknown("AVF declaration could not be read from PackageManager")
        if (s.avfDeclared != true) {
            return Availability.Unsupported(
                "Device does not declare android.software.virtualization_framework; " +
                    "AVF cannot be enabled by this app (it is a firmware/vendor property)",
            )
        }
        if (s.sdkInt < CapabilityPolicy.SDK_AVF_FLOOR) {
            return Availability.Degraded(
                Evidence.PLATFORM_DECLARATION,
                "AVF declared, API ${s.sdkInt}",
                "AVF is declared but the platform predates API ${CapabilityPolicy.SDK_AVF_FLOOR}; " +
                    "protected-VM APIs may be missing",
            )
        }
        if (s.kvmPresent == false) {
            return Availability.Degraded(
                Evidence.PLATFORM_DECLARATION,
                "AVF declared, /dev/kvm absent",
                "AVF is declared but /dev/kvm is not accessible to apps; guest acceleration depends on the vendor hypervisor",
            )
        }
        return Availability.Supported(
            Evidence.PLATFORM_DECLARATION,
            "android.software.virtualization_framework declared",
            listOf("A real guest boot must be demonstrated before AVF is reported as working"),
        )
    }

    private fun vulkan(s: HardwareSnapshot): Availability = when (s.vulkanSupported) {
        null -> Availability.Unknown("Vulkan probe did not complete")
        false -> Availability.Unsupported("No Vulkan device could be created (driver or loader absent)")
        true -> Availability.Supported(
            Evidence.RUNTIME_PROBE,
            "Vulkan instance/device created, API ${s.vulkanApiVersion ?: "unknown"}",
            listOf("Renderer identity must still be validated as hardware before claiming acceleration"),
        )
    }

    private fun gles3(s: HardwareSnapshot): Availability {
        val v = s.glEsVersion
        if (v.isNullOrBlank()) return Availability.Unknown("OpenGL ES version could not be read")
        val major = v.substringBefore('.').toIntOrNull()
            ?: return Availability.Unknown("Unparseable OpenGL ES version '$v'")
        val minor = v.substringAfter('.', "0").toIntOrNull() ?: 0
        if (major < 3) return Availability.Unsupported("OpenGL ES $v present; 3.x required")
        if (minor < 2 && major == 3) {
            return Availability.Degraded(
                Evidence.PLATFORM_DECLARATION,
                "OpenGL ES $v",
                "OpenGL ES $v lacks several 3.2 features; use the compatibility path",
            )
        }
        return Availability.Supported(Evidence.PLATFORM_DECLARATION, "OpenGL ES $v")
    }

    /**
     * Accelerated composition is claimed only when a rendering API exists *and* a
     * runtime test has confirmed the renderer is hardware-backed. This evaluator has
     * no runtime test, so the ceiling here is [Availability.Degraded]-grade evidence
     * tagged as unverified.
     */
    private fun acceleratedCompositing(s: HardwareSnapshot): Availability {
        val vulkan = s.vulkanSupported == true
        val gles = !s.glEsVersion.isNullOrBlank() &&
            (s.glEsVersion.substringBefore('.').toIntOrNull() ?: 0) >= 3
        if (!vulkan && !gles) {
            if (s.vulkanSupported == null && s.glEsVersion.isNullOrBlank()) {
                return Availability.Unknown("Neither Vulkan nor OpenGL ES could be probed")
            }
            return Availability.Unsupported("No Vulkan and no OpenGL ES 3.x: composition would be CPU-bound")
        }
        val api = when {
            vulkan -> "Vulkan ${s.vulkanApiVersion ?: ""}".trim()
            else -> "OpenGL ES ${s.glEsVersion}"
        }
        return Availability.Supported(
            Evidence.PLATFORM_DECLARATION,
            "$api available",
            listOf(
                "PENDING RUNTIME VERIFICATION: an API being present is not hardware acceleration. " +
                    "The compositor must pass a GPU frame test and report the real renderer before this is claimed.",
            ),
        )
    }

    private fun resourceMonitoring(s: HardwareSnapshot): Availability {
        if (s.sdkInt <= 0) return Availability.Unknown("Android API level could not be read")
        return Availability.Supported(
            Evidence.PLATFORM_DECLARATION,
            if (s.sdkInt >= 29) "ActivityManager + PowerManager thermal APIs" else "ActivityManager memory APIs",
        )
    }
}
