# Pocket PC

A desktop-computing environment inside Android: a native desktop shell and window
manager, a real ARM64 Linux userspace (PRoot), a real virtual machine backend (QEMU,
plus AVF where firmware supports it), and an explicitly measured graphics path.

**This repository is at Phase 2.** Read `docs/PHASE2.md` before believing any capability
claim: it separates *implemented*, *tested*, *verified* and *pending hardware* item by item.
`docs/FEASIBILITY.md` holds the Phase 0 research and risk register.

## Honest status

| Area | State |
| --- | --- |
| Desktop shell (wallpaper, icons, context menus) | Implemented + unit tested |
| Window manager (drag, resize, snap, z-order, restore, workspaces) | Implemented + 38 tests |
| Taskbar, Start menu, quick settings, system tray | Implemented + tested; **no notification centre** |
| 9 desktop applications | Implemented, functional |
| Capability reporting | 8-state model (DETECTED…VERIFIED), 26 tests |
| GPU verification engine | Implemented + verdict rules tested; **device run pending** |
| Application icon | Done — adaptive + monochrome + 5 densities |
| PRoot Linux environment | **Not started** (Phase 5) |
| QEMU / AVF virtualization | **Not started** (Phase 7). No KVM and no AVF on the target firmware |
| Linux GPU acceleration | **Not available** — documented reason in Phase 2 §7 |

Nothing above is a mock. Code that does not exist is listed as not existing, and no
performance figure is published unless it was measured on the device.

## Build

The development sandbox for this project cannot reach Maven Central, so the
authoritative build is GitHub Actions (`.github/workflows/build.yml`). Locally, with a
normal Android toolchain:

```bash
./gradlew :system:capability:test :system:desktop:test \
          :graphics:compositor:testDebugUnitTest   # 120 tests, pure JVM
./gradlew :app:assembleDebug                        # APK → app/build/outputs/apk/debug/
python3 scripts/generate_icons.py                   # regenerate icon assets
```

Every push runs both and commits the result to `ci/build-evidence.md`
(see `docs/ADR-0001-verification-loop.md`).

## Verify a capability claim

```bash
grep -n "result" ci/build-evidence.md     # and open the linked run
```

## Trying it on a device

```bash
./gradlew :app:installDebug
adb shell am start -n dev.pocketpc.app/.MainActivity
```

The app opens straight onto the desktop. Open **GPU Diagnostics** and press
**Run GPU test**: it draws a real workload, reads the pixels back, and reports the
renderer, API, hardware verdict and measured frame times. Those numbers are what Phase 2
is waiting on — see `docs/PHASE2.md` §4.2 and §8.

## Layout

```
system/capability/          pure Kotlin: snapshot → 8-state capability verdicts
system/capability-android/  real probes: EGL, PackageManager, StatFs, thermal
system/desktop/             pure Kotlin: window manager, shell, apps, calculator, keys
graphics/compositor/        GLES verification engine + GPU verdict rules
app/                        Compose UI: desktop, windows, taskbar, start menu, apps
scripts/                    icon generator, CI evidence helpers
docs/                       PHASE2 report, feasibility report, ADRs, icon previews
ci/                         committed CI evidence (auto-generated, do not hand-edit)
```

## Non-negotiables

Real functionality only. No feature is called working until a test proves it on the
device. PRoot is never described as virtualization. GPU acceleration is never claimed
because an API exists — only a verified frame readback from a real workload can do that.
Unsupported things are documented, not hidden.
