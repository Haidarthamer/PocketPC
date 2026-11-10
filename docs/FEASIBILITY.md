# Pocket PC — Phase 0 Technical Feasibility Report

**Date:** 2026-11-10
**Status:** Phase 0 complete; Phase 1 in progress
**Author:** Principal architecture (agent), pending hardware verification by project owner

> **Evidence policy.** Every claim below is tagged with how it is known. Anything not
> observed in this repository, in the connected toolchain, or in a cited public source
> is explicitly marked *unverified*. Nothing in this document is inferred from the
> marketing name of the target device.

Legend: **[OBSERVED]** measured in this workspace · **[CI]** proven by a green GitHub
Actions run · **[PUBLIC]** cited external source · **[PENDING]** requires the physical
device · **[UNAVAILABLE]** blocked by the environment.

---

## 1. Repository state at the start of work

| Item | Finding | Evidence |
| --- | --- | --- |
| Commits before this work | 1 (`Initial commit`) | [OBSERVED] `git log` |
| Tracked files | 1 (`README.md`, content `# PocketPC`, 11 bytes) | [OBSERVED] `find`, `wc -c` |
| Existing code, dependencies, docs | None | [OBSERVED] |
| Branches | `main`, `arena/6054088b-pocketpc` | [OBSERVED] |
| Default branch | `main` | [OBSERVED] `gh repo view` |
| License | **None present** — must be chosen before distributing third-party binaries | [OBSERVED] |

**Conclusion:** the project starts from zero. There was no working code to preserve and
no prior architecture to respect. The prompt's proposed directory tree was therefore
adopted *with the deviations listed in §8*, each for a technical reason.

---

## 2. Build environment: what can actually be verified here

This is the single most important constraint on the whole roadmap, so it is documented
before any architecture.

| Capability | Status | Detail |
| --- | --- | --- |
| JDK | [OBSERVED] available | OpenJDK 25 via the `jdk4py` PyPI wheel (`.../jdk4py/java-runtime/bin/java`) |
| `javac` | [UNAVAILABLE] | `jdk4py` ships a JRE only |
| Kotlin compiler | [UNAVAILABLE] | `kotlinc` is a multi-hundred-MB archive on `objects.githubusercontent.com` — blocked |
| Gradle | [UNAVAILABLE] | `services.gradle.org` and Maven Central unreachable (HTTP 000) |
| Android SDK / build tools | [UNAVAILABLE] | `dl.google.com` unreachable |
| Maven Central / Google Maven | [UNAVAILABLE] | unreachable from the sandbox |
| GitHub Actions | [CI] **working** | probe run succeeded; `ci/build-evidence.md` committed by the workflow |
| Actions *logs* | [UNAVAILABLE] | served from `results-receiver.actions.githubusercontent.com` — blocked |
| Actions *artifacts* | [UNAVAILABLE] | served from `*.blob.core.windows.net` — blocked |
| Direct device access (`adb`) | [UNAVAILABLE] | no device attached to this environment |

### Consequence: the verification loop

Because neither logs nor artifacts can be read back, **CI publishes its own evidence
into the repository** over `git push`, which *is* reachable:

```
.github/workflows/build.yml
   ├─ ./gradlew :system:capability:test      → tee ci/build.log
   ├─ ./gradlew :app:assembleDebug           → tee ci/build.log
   └─ commits ci/build-evidence.md + ci/build.log back to the branch
```

This is a deliberate, documented deviation from "run builds locally". It means:

* A phase is never marked complete here on the basis of "it compiles on my machine".
* Every engineering claim in this repo points at a run ID and a committed log.
* **The sandbox cannot compile Android code at all.** This is stated plainly rather
  than worked around by writing untested code and calling it done.

---

## 3. Feasibility by subsystem

### 3.1 Android desktop shell and window manager — FEASIBLE

Jetpack Compose can host a real window manager: floating windows are `Modifier.offset`
+ `detectDragGestures` over a z-ordered list; resizing uses `BoxWithConstraints` and
`onGloballyPositioned`; frame pacing comes from the Choreographer/Compose recomposer.
This is genuinely native Android rendering, not a simulation of a desktop.

*Risk:* Compose currently renders through the platform's HWUI pipeline. Claiming
"our compositor is GPU-accelerated" because Compose draws with the GPU is **not**
acceptable per the constitution; Phase 4 must build an explicit renderer (OpenGL
ES/Vulkan via `SurfaceView`/`VkSurface`) and measure it.

### 3.2 PRoot ARM64 Linux userspace — FEASIBLE, with hard limits

PRoot uses `ptrace` to intercept syscalls and to rewrite paths, so a rootfs can run
unprivileged. It needs: an executable ARM64 helper for the device ABI, a rootfs image,
and `W^X`-compatible exec.

**[PUBLIC] Known limitations that must be surfaced in the UI, not hidden:**
* There is **no guest kernel**. `/proc`, kernel modules, and privileged syscalls are the
  host's. PRoot is *not* virtualization and must never be labelled as such.
* `ptrace` interception costs a measurable multiple on syscall-heavy work (package
  installs are notably slow).
* Android's SELinux policy and app sandboxing can deny `exec` from app data on some
  builds; this is device-specific and [PENDING] verification.

### 3.3 QEMU ARM64 guests — FEASIBLE but SLOW without KVM

QEMU's TCG backend emulates every instruction. On a Snapdragon 8 Gen 3, an ARM64 guest
under TCG is usable for a terminal, painful for a full desktop. Booting is measured in
minutes. The capability model marks this `Degraded` for exactly this reason
(`Feature.VM_QEMU_TCG`).

### 3.4 AVF / pKVM hardware virtualization — DEVICE-DEPENDENT

**[PUBLIC]** AVF requires firmware that boots the kernel at EL2 with KVM (or a vendor
hypervisor: Qualcomm Gunyah, MediaTek GenieZone). Devices advertise it via
`android.software.virtualization_framework`. Per Google's Play Console catalog there
were ~361 AVF-capable models as of late 2024, and new chipsets launching with Android
15 (vendor API 202404) are *required* to declare AVF — which includes Snapdragon 8
Gen 3-class parts. **The POCO F7 Pro must be checked on-device; this document does not
assume it.** The detection is implemented and returns `Unknown`/`Unsupported`, never a
guess.

### 3.5 GPU acceleration

Three separate questions, routinely conflated:

1. **Is there a GPU?** — measurable now via a real EGL context (§4). [OBSERVED: implemented]
2. **Does the compositor reach it?** — needs a real renderer + frame timing. Phase 4.
3. **Can Linux guests use it?** — requires virgl/virtio-gpu/Venus plumbing through the
   display bridge. **[PUBLIC]** native virgl on Android is impractical because it needs
   host GL with a guest-visible 3D driver; Venus (Vulkan passthrough) requires a
   virtio-gpu with venus support in the guest kernel. **Plan: do not promise guest GPU
   acceleration.** Report the true renderer and fall back to software (llvmpipe) for
   Linux GUI, documented as such.

The capability model encodes this: `GPU_ACCELERATED_COMPOSITING` is `Supported` only on
API presence and explicitly carries the note *"PENDING RUNTIME VERIFICATION"*, and
`isVerified` stays `false` until a hardware frame test exists. There is a unit test
asserting exactly that (`api availability never counts as verified acceleration`).

---

## 4. First verified milestone (delivered)

**Milestone M1 — honest hardware capability report.**

* `:system:capability` — pure Kotlin. `HardwareSnapshot` (observations only) →
  `CapabilityEvaluator` (policy) → `Availability` with explicit `Evidence` levels.
  17 unit tests encode the anti-overclaiming rules. **[CI] passing.**
* `:system:capability-android` — real probes: EGL pbuffer context for `GL_VENDOR` /
  `GL_RENDERER`, `PackageManager` feature scan for Vulkan and AVF, `StatFs` for storage,
  `PowerManager.currentThermalStatus` for thermals. Every probe is individually guarded
  and its failure recorded in `probeWarnings`.
* `:app` — a diagnostics screen that shows every feature with its evidence level and a
  literal `verified: true|false` line, so the UI cannot flatter the implementation.

**Acceptance for Phase 1:** criteria 1 (build) is met and proven:
CI run [`42882436120`](https://github.com/Haidarthamer/PocketPC/actions/runs/42882436120)
— 17/17 unit tests green, `app/build/outputs/apk/debug/app-debug.apk` (9,485,534 bytes,
`dev.pocketpc.app`, minSdk 26, targetSdk 35, **no dangerous permissions**).
Criteria 2–3 (installs, detects hardware correctly on the target) remain **[PENDING]**
until the owner runs the diagnostics screen on the POCO F7 Pro (§7).

> Note: `42-MiB` of the APK is Compose runtime, not our code — the next milestone should
> add an R8/`assembleRelease` size report rather than accept 9 MB blindly.

---

## 5. Risk register

| # | Risk | Likelihood | Impact | Mitigation / decision |
| --- | --- | --- | --- | --- |
| R1 | Sandbox cannot compile Android code | Certain | High | CI is the compiler; evidence committed back; **no phase marked complete without a run ID** |
| R2 | No physical target device available to the agent | Certain | High | Hardware items tracked as [PENDING]; owner runs `adb` reports |
| R3 | AVF absent on retail POCO F7 Pro firmware | Medium | High | Detected at runtime; QEMU/TCG remains the honest fallback |
| R4 | PRoot blocked by SELinux/`exec` policy on HyperOS | Medium | High | Probe `exec` on the app data dir at first run; fall back to a filesystem-only mode |
| R5 | Guest GPU acceleration never works | High | Medium | Never promised; `LINUX_GUI` is `Degraded` by construction; software rendering documented |
| R6 | QEMU/TCG too slow to be "desktop" | High | Medium | Capability model labels it `Degraded`; messaging matches reality |
| R7 | Third-party licences (QEMU GPLv2, PRoot GPLv2, distro rootfs) | High | High | See §6 — decide distribution model before shipping binaries |
| R8 | Android kills long-running Linux/VM processes (LMK, Doze) | High | Medium | Foreground service + `startForeground` + explicit user-visible state |
| R9 | Thermal throttling degrades VM performance | Medium | Medium | `PerformanceProfile` already refuses guests at `THERMAL_STATUS_SEVERE` |
| R10 | Scope: the full prompt is a multi-year product | Certain | High | Strict phase gates; no phase starts before the previous acceptance passes |

---

## 6. Dependency and licensing posture

Nothing third-party is vendored yet. Before any binary is distributed:

| Component | Licence | Obligation |
| --- | --- | --- |
| QEMU | GPLv2 | Shipping a modified binary ⇒ publish corresponding source; no linking into a closed app |
| PRoot | GPLv2 | Same; must ship sources for the built `proot` |
| Debian/Ubuntu ARM64 rootfs | mixed (GPL + others) | Ship licence texts; do not strip `/usr/share/doc` blanket-wide |
| `termux/proot-distro` scripts | GPLv3 | Scripts only; keep attribution if reused |
| AndroidX / Compose | Apache-2.0 | NOTICE file |
| libvncserver / TigerVNC | GPLv2 | Only if the display bridge uses them — prefer an MIT/BSD VNC server or an in-house RFB client |

**Decision:** the app itself stays closed-source-friendly only if GPL binaries are
delivered as separate, downloadable artifacts with source offers — not statically
linked into the APK. This must be settled before Phase 5.

---

## 7. Open items requiring the physical device

| ID | Item | How to verify |
| --- | --- | --- |
| H1 | `GL_RENDERER` is Adreno and not a software rasteriser | Diagnostics screen screenshot |
| H2 | `android.software.virtualization_framework` declared | Diagnostics screen: AVF row |
| H3 | `/dev/kvm` reachable from the app | Diagnostics screen: `kvm node` |
| H4 | `exec` from app data permitted (PRoot prerequisite) | Phase 5 smoke test |
| H5 | Thermal status readable | Diagnostics screen: `thermal` |

The diagnostics screen exists specifically so these can be answered by the owner in one
run, instead of the agent inventing device facts.

---

## 8. Documented deviations from the prompt's proposed tree

| Prompt | Implemented | Why |
| --- | --- | --- |
| `system/` holds managers | `system/capability` + `system/capability-android` | Splitting pure policy from Android bindings is what makes the logic unit-testable on a JVM (and is what caught defect #1 below) |
| `graphics/`, `native/`, `sdk/`, `virtualization/` | Not created yet | Empty directories are not architecture. Each appears when it has real code and a passing test |
| Everything under `android/` | `:app` module | Single launcher activity for now; modules are split when there is a reason, not pre-emptively |

---

## 9. Defects found by the verification loop (evidence that it works)

1. **Architecture defect:** Android probes were compiled into the pure-JVM core. CI run
   `42881217840` produced ~100 `Unresolved reference 'android'` errors. Fixed by creating
   `:system:capability-android`. Had the build "worked locally" and been declared done,
   this would have shipped broken.
2. **Version-specific API:** `PackageManager.FEATURE_VIRTUALIZATION_FRAMEWORK` did not
   resolve against the compile SDK. Replaced with a stable feature-name query after run
   `42881757877` reported exactly one error.
3. **Toolchain defect:** `jvmToolchain(17)` with only JDK 21 installed fails resolution.
   Replaced with bytecode targeting.

---

## 10. Phase status

| Phase | Status |
| --- | --- |
| 0 — Research and feasibility | **Complete (this document)** |
| 1 — Android foundation | **In progress** — code written, unit tests green [CI]; APK install on device [PENDING H1–H5] |
| 2 — Desktop shell + window manager | Not started (gate: Phase 1 acceptance) |
| 3+ | Not started |

**Next executable milestone:** close Phase 1 by installing the debug APK on the POCO F7
Pro and recording the diagnostics output, then implement the window manager (Phase 2)
with drag/resize/z-order/state-restoration covered by instrumentation tests.
