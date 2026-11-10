package dev.pocketpc.system.capability

/** Product features whose availability depends on the host device. */
public enum class Feature {
    DESKTOP_SHELL,
    WINDOW_MANAGER,
    TASKBAR,
    /** Rootless ARM64 Linux userspace via PRoot. Not virtualization. */
    LINUX_PROOT,
    /** Linux GUI apps through the display bridge. Requires LINUX_PROOT. */
    LINUX_GUI,
    /** Guest VMs via QEMU TCG (emulation: no KVM requirement, no hardware acceleration). */
    VM_QEMU_TCG,
    /** Guest VMs with genuine hardware virtualization through AVF/pKVM. */
    VM_AVF_HARDWARE,
    GPU_VULKAN,
    GPU_GLES3,
    /** The GPU-accelerated desktop compositor. VERIFIED only after real GPU frames. */
    GPU_COMPOSITOR,
    RESOURCE_MONITORING,
    ;
}

/** Snapshot of every capability, in display order. */
public data class SystemReport(
    public val snapshot: HardwareSnapshot,
    public val capabilities: List<CapabilityReport>,
    public val profile: PerformanceProfile,
) {
    public fun of(feature: Feature): CapabilityReport =
        capabilities.firstOrNull { it.feature == feature }
            ?: CapabilityReport(feature, CapabilityState.UNSUPPORTED, "not evaluated")
}
