package dev.pocketpc.system.capability

/**
 * Turns device observations into honest capability states.
 *
 * Two hard rules, both regression-tested:
 *  1. A probe that could not read a value yields [CapabilityState.UNSUPPORTED] /
 *     absent evidence -- never DETECTED, AVAILABLE or better.
 *  2. An API existing is never reported as verified hardware rendering. The maximum
 *     state this evaluator can reach for a rendering capability is AVAILABLE; only the
 *     runtime GPU test in the graphics layer may raise it to TESTED/VERIFIED/FAILED.
 */
public object CapabilityEvaluator {

    public fun evaluate(snapshot: HardwareSnapshot): SystemReport {
        val profile = PerformanceProfile.derive(snapshot)
        val caps = listOf(
            desktop(snapshot),
            windowManager(snapshot),
            taskbar(snapshot),
            proot(snapshot),
            linuxGui(snapshot),
            qemuTcg(snapshot),
            avf(snapshot),
            vulkan(snapshot),
            gles3(snapshot),
            compositor(snapshot),
            monitoring(snapshot),
        )
        return SystemReport(snapshot, caps, profile)
    }

    private fun desktop(s: HardwareSnapshot): CapabilityReport {
        if (s.sdkInt <= 0) return unsupported(Feature.DESKTOP_SHELL, "Android API level could not be read")
        if (s.sdkInt < CapabilityPolicy.MIN_SDK) {
            return unsupported(
                Feature.DESKTOP_SHELL,
                "Requires Android 8.0 (API ${CapabilityPolicy.MIN_SDK}); device reports API ${s.sdkInt}",
            )
        }
        // The desktop shell ships in this build and is covered by unit tests, so it can
        // honestly claim TESTED. It reaches VERIFIED only after a device run is recorded.
        return CapabilityReport(
            Feature.DESKTOP_SHELL,
            CapabilityState.TESTED,
            "Compose desktop shell, API ${s.sdkInt}",
            CapabilityEvidence(detected = true, initialized = true),
            notes = listOf("Desktop interaction is instrumented; on-device confirmation is recorded in docs/PHASE2.md"),
        )
    }

    private fun windowManager(s: HardwareSnapshot): CapabilityReport {
        val base = desktop(s)
        if (base.state == CapabilityState.UNSUPPORTED) {
            return unsupported(Feature.WINDOW_MANAGER, base.detail)
        }
        return CapabilityReport(
            Feature.WINDOW_MANAGER,
            CapabilityState.TESTED,
            "Floating windows with drag, resize, snap, z-order and restore",
            CapabilityEvidence(detected = true, initialized = true),
        )
    }

    private fun taskbar(s: HardwareSnapshot): CapabilityReport {
        val base = desktop(s)
        if (base.state == CapabilityState.UNSUPPORTED) {
            return unsupported(Feature.TASKBAR, base.detail)
        }
        val notes = buildList<String> {
            add("Volume is adjusted through the app's own media stream; changing system volume needs a user-granted permission")
            if (s.sdkInt < 23) add("Brightness control requires API 23+")
        }
        return CapabilityReport(
            Feature.TASKBAR,
            CapabilityState.TESTED,
            "Taskbar, start menu, tray and quick settings",
            CapabilityEvidence(detected = true, initialized = true),
            notes = notes,
        )
    }

    /**
     * PRoot needs an executable helper for the device ABI plus a rootfs. This evaluator
     * can only establish preconditions; the environment is working only once a
     * distribution installs and runs (Phase 5).
     */
    private fun proot(s: HardwareSnapshot): CapabilityReport {
        if (s.abi.isNullOrBlank()) return unknown(Feature.LINUX_PROOT, "Process ABI could not be read")
        if (s.sdkInt < 24) return unsupported(Feature.LINUX_PROOT, "PRoot requires API 24+")
        if (!s.abi.startsWith("arm") && !s.abi.startsWith("x86")) {
            return unsupported(Feature.LINUX_PROOT, "Unsupported ABI for PRoot helpers: ${s.abi}")
        }
        if (s.storageFreeMb <= 0L) return unknown(Feature.LINUX_PROOT, "Free storage unreadable; rootfs size unknown")
        if (s.storageFreeMb < CapabilityPolicy.MIN_FREE_MB_FOR_ROOTFS) {
            return unsupported(
                Feature.LINUX_PROOT,
                "Needs ${CapabilityPolicy.MIN_FREE_MB_FOR_ROOTFS} MiB free; only ${s.storageFreeMb} MiB available",
            )
        }
        return CapabilityReport(
            Feature.LINUX_PROOT,
            CapabilityState.AVAILABLE,
            "ABI ${s.abi}, ${s.storageFreeMb} MiB free",
            CapabilityEvidence(detected = true),
            notes = listOf(
                "PRoot is a userspace emulation layer, not virtualization: the guest shares the Android kernel",
                "ptrace interception has measurable overhead; no kernel modules, no real mount, limited /proc",
            ),
        )
    }

    private fun linuxGui(s: HardwareSnapshot): CapabilityReport {
        val p = proot(s)
        if (p.state == CapabilityState.UNSUPPORTED) {
            return unsupported(Feature.LINUX_GUI, "No Linux environment: ${p.detail}")
        }
        if (!p.state.isUsable) return unknown(Feature.LINUX_GUI, "Linux environment undetermined: ${p.detail}")
        return CapabilityReport(
            Feature.LINUX_GUI,
            CapabilityState.LIMITED,
            p.detail,
            CapabilityEvidence(detected = true),
            notes = listOf(
                "Linux GUI renders in software (LLVMpipe/Mesa) by default; guest GPU acceleration is not established",
                "Displayed through a VNC-class bridge; this is Android-hosted acceleration, not guest acceleration",
            ),
        )
    }

    private fun qemuTcg(s: HardwareSnapshot): CapabilityReport {
        if (s.abi.isNullOrBlank()) return unknown(Feature.VM_QEMU_TCG, "Process ABI could not be read")
        if (s.ramTotalMb <= 0L) return unknown(Feature.VM_QEMU_TCG, "Total RAM unreadable; cannot size a guest")
        if (s.ramTotalMb < CapabilityPolicy.MIN_RAM_MB_FOR_QEMU) {
            return unsupported(
                Feature.VM_QEMU_TCG,
                "Needs ${CapabilityPolicy.MIN_RAM_MB_FOR_QEMU} MiB RAM; device reports ${s.ramTotalMb} MiB",
            )
        }
        return CapabilityReport(
            Feature.VM_QEMU_TCG,
            CapabilityState.LIMITED,
            "QEMU TCG, ${s.ramTotalMb} MiB RAM",
            CapabilityEvidence(detected = true),
            notes = listOf(
                "TCG is software emulation: every guest instruction is translated on the CPU. No KVM, no hardware virtualization",
            ),
        )
    }

    private fun avf(s: HardwareSnapshot): CapabilityReport {
        if (s.avfDeclared == null) return unknown(Feature.VM_AVF_HARDWARE, "AVF declaration could not be read")
        if (s.avfDeclared != true) {
            return unsupported(
                Feature.VM_AVF_HARDWARE,
                "Device does not declare android.software.virtualization_framework. " +
                    "AVF is a firmware/vendor property and cannot be added by an app",
            )
        }
        if (s.sdkInt < CapabilityPolicy.SDK_AVF_FLOOR) {
            return CapabilityReport(
                Feature.VM_AVF_HARDWARE,
                CapabilityState.LIMITED,
                "AVF declared, API ${s.sdkInt}",
                CapabilityEvidence(detected = true),
                notes = listOf("Platform predates API ${CapabilityPolicy.SDK_AVF_FLOOR}; protected-VM APIs may be missing"),
            )
        }
        if (s.kvmPresent == false) {
            return CapabilityReport(
                Feature.VM_AVF_HARDWARE,
                CapabilityState.LIMITED,
                "AVF declared, /dev/kvm not accessible to apps",
                CapabilityEvidence(detected = true),
                notes = listOf("Acceleration depends on the vendor hypervisor; a real guest boot must be demonstrated"),
            )
        }
        return CapabilityReport(
            Feature.VM_AVF_HARDWARE,
            CapabilityState.AVAILABLE,
            "android.software.virtualization_framework declared",
            CapabilityEvidence(detected = true),
            notes = listOf("Not verified until a real guest boots under AVF"),
        )
    }

    private fun vulkan(s: HardwareSnapshot): CapabilityReport = when (s.vulkanSupported) {
        null -> unknown(Feature.GPU_VULKAN, "Vulkan probe did not complete")
        false -> unsupported(Feature.GPU_VULKAN, "No Vulkan device could be created (loader or driver absent)")
        true -> CapabilityReport(
            Feature.GPU_VULKAN,
            CapabilityState.AVAILABLE,
            "Vulkan device creatable, API ${s.vulkanApiVersion ?: "unknown"}",
            CapabilityEvidence(
                detected = true,
                initialized = true,
                rendererIdentity = s.gpuRenderer,
                api = s.vulkanApiVersion?.let { "Vulkan $it" },
            ),
            notes = listOf("Availability is not acceleration: only the GPU runtime test may raise this to VERIFIED"),
        )
    }

    private fun gles3(s: HardwareSnapshot): CapabilityReport {
        val v = s.glEsVersion
        if (v.isNullOrBlank()) return unknown(Feature.GPU_GLES3, "OpenGL ES version could not be read")
        val major = v.substringBefore('.').toIntOrNull()
            ?: return unknown(Feature.GPU_GLES3, "Unparseable OpenGL ES version '$v'")
        val minor = v.substringAfter('.', "0").toIntOrNull() ?: 0
        if (major < 3) return unsupported(Feature.GPU_GLES3, "OpenGL ES $v present; 3.x required")
        val ev = CapabilityEvidence(
            detected = true,
            initialized = s.gpuRenderer != null,
            rendererIdentity = s.gpuRenderer,
            api = "OpenGL ES $v",
        )
        if (major == 3 && minor < 2) {
            return CapabilityReport(
                Feature.GPU_GLES3,
                CapabilityState.LIMITED,
                "OpenGL ES $v",
                ev,
                notes = listOf("Lacks several 3.2 features; compositor uses the compatibility path"),
            )
        }
        return CapabilityReport(Feature.GPU_GLES3, CapabilityState.AVAILABLE, "OpenGL ES $v", ev)
    }

    /**
     * The compositor. Its state is decided by the *runtime* GPU test result, which is
     * supplied as [compositorEvidence]. Without that evidence this returns AVAILABLE at
     * most and says so explicitly.
     */
    private fun compositor(s: HardwareSnapshot, compositorEvidence: CapabilityEvidence? = null): CapabilityReport {
        val api = when {
            s.vulkanSupported == true -> "Vulkan ${s.vulkanApiVersion ?: ""}".trim()
            !s.glEsVersion.isNullOrBlank() -> "OpenGL ES ${s.glEsVersion}"
            else -> null
        }
        if (api == null) {
            if (s.vulkanSupported == null && s.glEsVersion.isNullOrBlank()) {
                return unknown(Feature.GPU_COMPOSITOR, "Neither Vulkan nor OpenGL ES could be probed")
            }
            return unsupported(Feature.GPU_COMPOSITOR, "No Vulkan and no OpenGL ES 3.x")
        }
        if (compositorEvidence == null) {
            return CapabilityReport(
                Feature.GPU_COMPOSITOR,
                CapabilityState.AVAILABLE,
                "$api available; GPU runtime test has not run in this session",
                CapabilityEvidence(detected = true, initialized = s.gpuRenderer != null, api = api,
                    rendererIdentity = s.gpuRenderer),
                notes = listOf(
                    "Run the GPU verification from Settings to reach TESTED/VERIFIED",
                    "An API being present is not hardware acceleration",
                ),
            )
        }
        return compositorFrom(api, s, compositorEvidence)
    }

    /** Raises the compositor capability using measured runtime evidence. */
    public fun compositorFrom(
        api: String,
        s: HardwareSnapshot,
        ev: CapabilityEvidence,
        perf: PerformanceSample? = null,
    ): CapabilityReport {
        val hardware = ev.hardwareBacked == true
        val drew = ev.executed == true && ev.frameProduced == true
        val state = when {
            !drew -> CapabilityState.FAILED
            hardware -> if (perf != null) CapabilityState.VERIFIED else CapabilityState.TESTED
            else -> CapabilityState.LIMITED
        }
        val detail = when (state) {
            CapabilityState.VERIFIED -> "Hardware rendering verified: ${ev.rendererIdentity ?: "GPU"} via $api"
            CapabilityState.TESTED -> "Frames rendered on ${ev.rendererIdentity ?: "GPU"} via $api (not yet measured)"
            CapabilityState.LIMITED -> "Rendering ran but was not served by hardware (software fallback detected)"
            else -> "GPU workload did not complete"
        }
        return CapabilityReport(
            Feature.GPU_COMPOSITOR,
            state,
            detail,
            ev.copy(api = api),
            perf,
            notes = if (state == CapabilityState.LIMITED) {
                listOf("The compositor fell back to software rendering; check the driver/renderer string")
            } else {
                emptyList()
            },
        )
    }

    private fun monitoring(s: HardwareSnapshot): CapabilityReport {
        if (s.sdkInt <= 0) return unknown(Feature.RESOURCE_MONITORING, "Android API level could not be read")
        return CapabilityReport(
            Feature.RESOURCE_MONITORING,
            CapabilityState.TESTED,
            if (s.sdkInt >= 29) "ActivityManager + PowerManager thermal APIs" else "ActivityManager memory APIs",
            CapabilityEvidence(detected = true, initialized = true),
        )
    }

    private fun unsupported(f: Feature, reason: String) =
        CapabilityReport(f, CapabilityState.UNSUPPORTED, reason)

    /**
     * Inconclusive probes are reported as FAILED with an explicit reason rather than
     * UNSUPPORTED: "we could not tell" must not read as "the device cannot do it".
     */
    private fun unknown(f: Feature, reason: String) =
        CapabilityReport(f, CapabilityState.FAILED, "Probe inconclusive: $reason")

    /** Exposed for tests: the compositor path with explicit runtime evidence. */
    public fun evaluateWithCompositor(
        snapshot: HardwareSnapshot,
        evidence: CapabilityEvidence,
        perf: PerformanceSample? = null,
    ): SystemReport {
        val base = evaluate(snapshot)
        val api = base.of(Feature.GPU_VULKAN).evidence.api
            ?: base.of(Feature.GPU_GLES3).evidence.api
            ?: "unknown API"
        val updated = base.capabilities.map {
            if (it.feature == Feature.GPU_COMPOSITOR) compositorFrom(api, snapshot, evidence, perf) else it
        }
        return base.copy(capabilities = updated)
    }
}
