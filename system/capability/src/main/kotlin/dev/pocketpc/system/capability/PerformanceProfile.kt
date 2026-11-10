package dev.pocketpc.system.capability

/**
 * Performance profiles. The profile is *derived* from the device, not chosen by the
 * user alone: a device that cannot sustain the work must not be offered it. User intent
 * may only move the profile downward, never above what hardware evidence supports.
 */
public enum class ProfileId {
    /** Minimum background work, no guests, reduced animation, aggressive idle. */
    BATTERY_SAVER,

    /** Default desktop experience with guarding. */
    BALANCED,

    /** Maximum responsiveness; only offered with real headroom. */
    PERFORMANCE,
    ;

    public val rank: Int get() = ordinal
}

public data class PerformanceProfile(
    public val id: ProfileId,
    public val reasons: List<String>,
    public val maxGuestRamMb: Long,
    public val allowGuests: Boolean,
    /** True when heavy work should be refused until the device cools. */
    public val thermalThrottled: Boolean,
) {
    public companion object {
        public fun derive(s: HardwareSnapshot): PerformanceProfile {
            val reasons = mutableListOf<String>()
            val thermal = s.thermalStatus != null && s.thermalStatus >= CapabilityPolicy.THERMAL_SEVERE
            if (thermal) reasons += "Thermal status ${s.thermalStatus}: heavy work throttled"

            val ram = s.ramTotalMb
            if (ram in 1..(CapabilityPolicy.MIN_RAM_MB_FOR_DESKTOP - 1)) {
                reasons += "Low RAM (${ram} MiB)"
                return PerformanceProfile(ProfileId.BATTERY_SAVER, reasons, 0, false, thermal)
            }
            if (ram < CapabilityPolicy.MIN_RAM_MB_FOR_QEMU) {
                reasons += "RAM (${ram} MiB) supports the desktop but not guests"
                return PerformanceProfile(ProfileId.BALANCED, reasons, 0, false, thermal)
            }
            if (ram < CapabilityPolicy.COMFORTABLE_RAM_MB) {
                reasons += "RAM ($ram MiB) fits one modest guest"
                return PerformanceProfile(ProfileId.BALANCED, reasons, computeGuestRam(ram), !thermal, thermal)
            }
            reasons += "RAM ($ram MiB) provides headroom"
            return PerformanceProfile(
                if (thermal) ProfileId.BALANCED else ProfileId.PERFORMANCE,
                reasons,
                computeGuestRam(ram),
                !thermal,
                thermal,
            )
        }

        /**
         * Never hand a guest more than a quarter of physical RAM, and never more than
         * 4 GiB: mobile guests contend with the framework for the rest.
         */
        public fun computeGuestRam(totalMb: Long): Long {
            if (totalMb <= 0L) return 0L
            val quarter = totalMb / 4
            return quarter.coerceIn(0L, 4_096L)
        }

        /** Apply an explicit user ceiling without exceeding the hardware-derived profile. */
        public fun capTo(profile: PerformanceProfile, requested: ProfileId): PerformanceProfile {
            if (requested.rank >= profile.id.rank) return profile
            val allowGuests = profile.allowGuests && requested != ProfileId.BATTERY_SAVER
            return profile.copy(
                id = requested,
                allowGuests = allowGuests,
                maxGuestRamMb = if (allowGuests) profile.maxGuestRamMb else 0L,
                reasons = profile.reasons + "Capped by user preference (${requested.name})",
            )
        }
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
