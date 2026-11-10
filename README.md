# Pocket PC

A desktop-computing environment inside Android: a native desktop shell and window
manager, a real ARM64 Linux userspace (PRoot), a real virtual machine backend (QEMU,
plus AVF where firmware supports it), and an explicitly measured graphics path.

**This repository is at Phase 1.** Read `docs/FEASIBILITY.md` before believing any
capability claim: it records what has been proven, what is pending, and what is blocked.

## Honest status

| Area | State |
| --- | --- |
| Device capability detection | Implemented; 17/17 unit tests green in CI; **not yet run on the target device** |
| Android app (diagnostics shell) | Builds to a 9.5 MB debug APK; install/run on hardware **pending** |
| Desktop shell + window manager | **Not started** (Phase 2) |
| PRoot Linux environment | **Not started** (Phase 5) |
| QEMU / AVF virtualization | **Not started** (Phase 7) |
| Linux GPU acceleration | **Not started**; research conclusion: do not promise it |

Nothing above is a mock. Code that does not exist is listed as not existing.

## Build

The development sandbox for this project cannot reach Maven Central, so the
authoritative build is GitHub Actions (`.github/workflows/build.yml`). Locally, with a
normal Android toolchain:

```bash
./gradlew :system:capability:test      # pure-JVM capability logic, 17 tests
./gradlew :app:assembleDebug           # APK → app/build/outputs/apk/debug/
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

The diagnostics screen prints, per feature, whether it is `SUPPORTED`, `LIMITED`,
`UNSUPPORTED` or `UNKNOWN`, and an explicit `verified: false` line until a real test
backs it. Please send that screen's contents (§7 of the feasibility report lists the
five open hardware questions it answers).

## Layout

```
system/capability/          pure Kotlin: snapshot → availability policy (JVM-testable)
system/capability-android/  real probes: EGL, PackageManager, StatFs, thermal
app/                        Android app (Phase 1: diagnostics shell)
docs/                       feasibility report, ADRs
ci/                         committed CI evidence (auto-generated, do not hand-edit)
```

## Non-negotiables

Real functionality only. No feature is called working until a test proves it on the
device. PRoot is never described as virtualization. GPU acceleration is never claimed
because an API exists. Unsupported things are documented, not hidden.
