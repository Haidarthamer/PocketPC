# ADR-0001 — CI publishes its own build evidence into the repository

* **Status:** Accepted
* **Date:** 2026-11-10
* **Context:** Phase 0

## Context

The development sandbox has no Android SDK, no Gradle distribution and no route to
Maven Central or Google Maven (see `docs/FEASIBILITY.md` §2). It cannot compile this
project. GitHub Actions can — a probe run confirmed it — but **both the workflow logs
(`results-receiver.actions.githubusercontent.com`) and the artifact store
(`*.blob.core.windows.net`) are unreachable from the sandbox.**

Only `github.com`, `api.github.com`, `codeload.github.com`, PyPI and npm are reachable.
`git push` is therefore the only channel by which build results can travel back to a
place the agent and the user can both read.

## Decision

`:build` workflow is the authoritative compiler and test runner. On every push it:

1. runs the unit tests and `assembleDebug`;
2. tees all Gradle output to `ci/build.log`;
3. writes a summary to `ci/build-evidence.md` (commit, run link, test XML, APK metadata);
4. commits both files back to the same branch.

`docs/` must reference the run ID for any claim of "verified".

## Consequences

**Good**
* Failure diagnostics are available even though the log hosts are blocked. The first
  three runs of this workflow each found a real defect (§9 of the feasibility report).
* The claim "it builds" is falsifiable by anyone: clone, read `ci/build-evidence.md`,
  open the linked run.
* Being unable to build locally is *documented* instead of hidden behind "should work".

**Bad**
* Every change costs one push-and-wait cycle (2–3 minutes here).
* Evidence commits pollute the branch history; they are clearly prefixed `ci(evidence):`.
* CI writes to the branch, so local work must `git pull --rebase` before pushing — this
  happened twice during Phase 1 and was resolved by rebasing, never by force-pushing.

**Rejected alternatives**
* *Read the logs with a different token/path* — the block is at the network layer, not auth.
* *Ask the user to paste logs* — works, but makes verification non-reproducible.
* *Ship untested code and "fix later"* — forbidden by the constitution.
