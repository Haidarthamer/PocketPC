# Pocket PC — Phase 2 Report

**Version:** 0.2.0-phase2 (versionCode 2)
**Date:** 2026-11-10
**Target hardware:** POCO F7 Pro (24117RK2CG), API 36, Adreno 750, 11,100 MiB RAM

> **How to read this document.** Every claim is tagged. Nothing here is a projection.
>
> * **[CI]** — proven by a green GitHub Actions run with committed evidence.
> * **[TESTED]** — covered by an automated unit test that passes [CI].
> * **[PENDING]** — implemented but needs a run on the physical device; not claimed working.
> * **[NOT DONE]** — absent, and said so.
> * **[BLOCKED]** — technically impossible on this platform; reason given.

---

## 1. Status summary

| # | Acceptance criterion | State | Evidence |
| --- | --- | --- | --- |
| 1 | Functional desktop shell | **Implemented + [TESTED]** | `DesktopScreen`, wallpaper/icons/taskbar; 120 tests |
| 2 | Working window manager | **Implemented + [TESTED]** | 29 WM tests + snap/z-order regressions |
| 3 | Multiple functional windows | **Implemented + [TESTED]** | concurrent windows, cascade, tiling |
| 4 | Working taskbar | **Implemented + [TESTED]** | running/active indicators, workspaces, tray |
| 5 | Working Start menu | **Implemented + [TESTED]** | ranked search tests, recents, pinning |
| 6 | System tray and notifications | **Partial** | battery/network/clock/thermal live; **no notification centre** |
| 7 | Touchscreen support | **Implemented [PENDING]** | drag/resize with 24dp+ targets; needs device run |
| 8 | External mouse + keyboard | **Implemented [PENDING]** | 16 mapped chords, scroll/pointer via Compose; needs device run |
| 9 | Actual GPU compositor | **Implemented [PENDING]** | EGL workload + pixel readback; needs device run |
| 10 | Runtime GPU verification | **Implemented + [TESTED]** | verdict rules unit tested; engine needs device |
| 11 | Adreno 750 hardware-rendering evidence | **[PENDING]** | **must be produced by running the app**; not fabricated here |
| 12 | Separate Linux graphics evaluation | **Documented (§7)** | research conclusion: software rendering only |
| 13 | Accurate capability reporting | **Implemented + [TESTED]** | 8-state model, 26 tests |
| 14 | Original Pocket PC icon | **Done** | `scripts/generate_icons.py`, previews in `docs/branding/` |
| 15 | Integrated adaptive launcher icon | **Done + [CI]** | foreground/background/monochrome, 5 densities, round |
| 16 | Automated tests | **Done, 120 tests** | `ci/build-evidence.md` |
| 17 | Updated documentation | **This document** | |
| 18 | Successful Android build | **Done** | run 42905125390, APK 9,851,652 bytes |
| 19 | Uploaded + verified APK artifact | **Done** | `PocketPC-debug-apk`, verified via Actions API |

**Phase 2 is not complete.** Criteria 7, 8, 9, 11 require on-device execution that this
environment cannot perform; criterion 6 is genuinely incomplete. §8 lists exactly what is
missing.

---

## 2. Architecture

```
system/capability/          pure Kotlin — snapshot → 8-state verdicts (26 tests)
system/capability-android/  Android probes (EGL, PackageManager, StatFs, thermal)
system/desktop/             pure Kotlin — window manager, shell, apps registry,
                            calculator, key bindings, tiling (65 tests)
graphics/compositor/        GLES verification engine + verdict rules (15 tests)
app/                        Compose UI: desktop, windows, taskbar, start menu, apps
```

**Design rule enforced throughout:** state machines and decision logic live in Android-free
modules and are unit tested; the Compose layer only renders that state and forwards
pointer/key events. This is why the window manager's behaviour is verifiable without a
device — and it is how three real bugs were caught (§6).

---

## 3. Eight-state capability model

The old binary `isVerified` flag allowed the UI to show SUPPORTED while nothing had run.
Replaced by a lifecycle:

`DETECTED → AVAILABLE → IMPLEMENTED → TESTED → VERIFIED`, plus `LIMITED`, `UNSUPPORTED`,
`FAILED`.

Rules, all regression-tested:

* An unreadable probe yields **FAILED with "Probe inconclusive"**, never UNSUPPORTED —
  "we could not tell" must not read as "the device cannot do it".
* `CapabilityEvaluator` can reach at most **AVAILABLE** for any graphics capability.
  Only `evaluateWithCompositor(...)`, fed by measured runtime evidence, can produce
  VERIFIED / TESTED / LIMITED / FAILED.
* `hardwareBacked` requires: context created **and** work actually executed (frames > 0)
  **and** a verified frame **and** a plausibly-hardware renderer string **and** no known
  software rasteriser.
* PRoot is reported AVAILABLE with a note stating it is **not virtualization**.
* AVF on this device reports **UNSUPPORTED** because the firmware does not declare
  `android.software.virtualization_framework` (confirmed by the owner's diagnostics).
  KVM is absent. **No KVM or AVF acceleration is claimed anywhere.**

---

## 4. GPU: what was built and what it proves

### 4.1 The verification engine (`graphics/compositor`)

`GlesVerificationEngine` does, in order:

1. Creates an EGL display/config/context (ES3, falling back to ES2) on a 512×512 pbuffer.
2. Compiles a real vertex+fragment shader program and creates a VBO with two triangles.
3. Clears to `rgb(0, 163, 255)` and draws the quad, `glFinish()` per frame.
4. **Reads the centre pixel back with `glReadPixels`** and compares it to the requested
   colour. This is the step that proves the command stream executed; without it, "we called
   draw" would be an unverified claim.
5. Times 120 frames with `SystemClock.elapsedRealtimeNanos`, computing avg/p99/max frame
   time and FPS.
6. Classifies the renderer string against known software rasterisers (SwiftShader,
   llvmpipe, softpipe…) and vendor patterns (Adreno, Mali, PowerVR, Xclipse…).

### 4.2 What is verified *now*, and what is not

| Claim | State |
| --- | --- |
| The engine is implemented and compiles | **Verified** [CI] |
| The verdict rules are correct | **Verified** [TESTED] — 15 tests incl. software detection |
| The frame readback path produces VERIFIED only with measurements | **Verified** [TESTED] |
| Adreno 750 renders this workload on the device | **[PENDING]** — requires running the app |

**No FPS or frame-time figure is reported in this document.** Publishing numbers that were
never measured is exactly the fabrication this project forbids. Tap **GPU Diagnostics →
Run GPU test** on the device; the window prints renderer, API, readback result, hardware
verdict and the measured fps/avg/p99/max, which can then be recorded here.

### 4.3 The desktop compositor itself

The desktop is drawn with Compose, i.e. through Android's HWUI pipeline onto the GPU. That
is genuine hardware-accelerated rendering **by the platform**, and this document does not
claim more than that. The frame-time badge on the desktop shows live composition cost
(`CompositionStats`, sampled from the real frame clock).

Why not a custom Vulkan compositor in this phase: the window manager's content is Compose
UI (text, vector icons, text fields). Reproducing that surface in a hand-written
Vulkan/GLES layer would mean re-implementing text shaping and input handling and would
*downgrade* the product. The GPU verification engine exists precisely so the hardware claim
is measured rather than asserted. A custom renderer for specific high-load surfaces (e.g.
the Linux display bridge) remains the right place for Phase 6.

---

## 5. Desktop, window manager, taskbar, input

### Window manager (`system/desktop/wm/WindowManager.kt`) — 38 tests

Drag (grab-offset preserving), resize from a 24dp handle with minimum-size clamping,
minimise, maximise/restore with saved bounds, close, focus-to-top, dense z-order, edge
snapping (12dp threshold, half-screen left/right) with pre-snap restore, viewport
re-constraint on rotation, and process-death snapshot/restore that drops windows whose app
no longer exists.

Snapping is an **explicit release-time decision** (`endDrag(allowSnap)`), so tiled and
programmatic placements keep their exact geometry instead of being rewritten as "snapped".

### Shell — 30 tests

* `AppRegistry`: 9 real apps, ranked search (name-prefix > name-substring > description >
  category), defaults for desktop icons and taskbar pins.
* `ShellModel`: running/active/recent state (capped at 6, de-duplicated), overlay
  exclusivity, Alt-Tab cycling.
* `WorkspaceManager`: 3 workspaces, each with its own window manager; per-workspace window
  counts shown on the taskbar pills.
* `KeyBindings`: 16 chords with generated help text, including Meta+←/→ snapping,
  Meta+1..3 workspaces, Ctrl+W close, Meta+↑ maximise.

### Applications (all functional)

| App | What it actually does |
| --- | --- |
| Files | real directory listing of app storage, sizes, folder navigation |
| Terminal | real command interpreter over the app sandbox (`ls`, `cat`, `du`, `mkdir`, `write`, `rm`, `date`) — **explicitly labelled "not a Linux shell"** |
| Text Editor | opens and saves real files under `filesDir/desktop/` |
| Calculator | recursive-descent parser: precedence, parentheses, unary minus, postfix `%` (9 tests) |
| Task Manager | app catalogue + real device memory from ActivityManager |
| System Monitor | live memory/thermal/battery/network, 1 s refresh |
| GPU Diagnostics | runs the verification engine and shows the verdict |
| Settings | theme, wallpaper motion, this app's media volume, GPU test trigger |
| About | live device/capability summary |

---

## 6. Defects the verification loop caught this phase

Recorded because they are the strongest evidence the loop is honest.

| # | Defect | How caught | Fix |
| --- | --- | --- | --- |
| 1 | Android probes compiled inside the pure-JVM module | CI compile errors | new `:system:capability-android` |
| 2 | `hardwareBacked` could be true with **zero frames rendered** | new `GpuVerifierTest` | rule requires executed work |
| 3 | `windows` documented as z-ordered, returned insertion order → focus/hit-test disagreed | `WindowManagerTest` | sorted by `zIndex` |
| 4 | **`MIN_WIDTH` 220dp > half of a 412dp display → left/right snapping impossible on the target device** | `TileLayoutTest` + `SnapWidthRegressionTest` | minimums 160×120dp |
| 5 | Calculator `%` did modulo instead of postfix percent | `CalculatorTest` | `/100` |
| 6 | Placement at x=0 reported as snapped, corrupting tiled restore bounds | `PlacementSnapTest` | explicit snap-on-release |
| 7 | z-index was 1-based | `WindowManagerTest` | dense 0-based |
| 8 | **CI only ran `:system:capability:test`** — 94 desktop/GPU tests existed but never executed | noticed while verifying test counts | workflow runs all three test tasks |
| 9 | Workflow YAML broke (inline python in a block scalar) → 0-second runs | Actions API showed no jobs | scripts moved to `scripts/ci/`, YAML validated in CI |
| 10 | `ci/build.log` accumulated across runs, embedding a stale artifact URL | reading committed evidence | log reset per run |

---

## 7. Linux graphics evaluation (requirement 4E)

Distinguishing the three layers, as demanded:

| Layer | Status on Adreno 750 / POCO F7 Pro |
| --- | --- |
| Android desktop acceleration | Measured by §4. **[PENDING device run]** |
| Linux GUI acceleration | **Not available on this path.** |
| QEMU guest acceleration | **BLOCKED** — no KVM, no AVF on this firmware. QEMU would run pure TCG. |

Why Linux GUI acceleration is not available through PRoot:

* PRoot shares the **Android kernel**; it does not provide a Linux DRM/KMS device, so Mesa
  has no GPU node to talk to.
* The practical route is Android-side GL (EGL) exposed to the guest via a socket —
  Termux:X11 style — which renders **on the host GPU** but requires a compatible Mesa build
  and `libepoxy`/X11 plumbing inside the rootfs. That is a Phase 6 experiment, and it must
  be verified, not assumed.
* VirGL/Venus need `virtio-gpu` with a 3D backend, i.e. a *real* VM. With no KVM, any such
  VM runs under TCG, where a 3D guest is unusably slow on this hardware.

**Honest conclusion for Phase 2:** `LINUX_GUI` is reported **LIMITED** with the note that
Linux GUI renders in software by default. No guest GPU acceleration is claimed. Phase 6
will attempt the host-GL bridge and report measured results.

---

## 8. Remaining limitations (nothing here is hidden)

1. **No on-device verification of criteria 7–11.** Clicks, drags, keyboard chords, and the
   GPU verdict need the physical POCO F7 Pro. The engine prints everything needed.
2. **No notification centre.** The tray shows live battery/network/clock/thermal; there is
   no notification list. Listed as Partial, not done.
3. **System brightness/ringer volume are not adjustable.** Android requires a
   user-granted permission; only the app's own media stream is controlled, and the UI says
   so.
4. **No custom Vulkan/GLES compositing of window content** (§4.3 explains the trade-off).
5. **Per-window memory is not measurable** by unprivileged apps on API 36; the Task Manager
   says this rather than inventing per-app figures.
6. **CI cannot read its own logs** (blocked Azure hosts), hence the committed evidence.
7. **Desktop icon layout resets** are clamped against a fixed 400×700dp assumption rather
   than the measured desktop box; a small correctness gap to fix in Phase 3.
8. **No VM/Linux backend in this phase** — Phase 5/7 work, deliberately not started.

---

## 9. Build and test results

| Item | Value |
| --- | --- |
| CI run | [`42905125390`](https://github.com/Haidarthamer/PocketPC/actions/runs/42905125390) — success |
| Test suites | 13 |
| Tests | **120, 0 failures, 0 errors** |
| APK | `app-debug.apk`, 9,851,652 bytes, sha256 `f3991e9…e254` |
| Version | `0.2.0-phase2` (versionCode 2) |
| minSdk / targetSdk | 26 / 35 |
| Permissions | none dangerous |
| Artifact | `PocketPC-debug-apk`, verified via Actions API |

Reproduce locally with a normal Android toolchain:

```bash
./gradlew :system:capability:test :system:desktop:test :graphics:compositor:testDebugUnitTest
./gradlew :app:assembleDebug
python3 scripts/generate_icons.py     # regenerates every icon asset
```

---

## 10. What Phase 3 must not start before

Phase 2's hardware criteria (7–11) are open. The next action is a device run of the
diagnostics and GPU test; those results belong in §4.2 before any Phase 3 work begins.
