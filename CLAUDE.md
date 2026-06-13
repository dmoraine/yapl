# CLAUDE.md — working in the YAPL repo

YAPL (Yet Another Pilot Log) — a private, offline Android flight logbook.
Product name **YAPL**; applicationId **be.moraine.yapl** (`.debug` for debug builds);
internal Kotlin namespace **dev.pilotlog** (the `R` class and packages use `pilotlog`).
Published GPL-3.0 on GitHub (`dmoraine/yapl`) and F-Droid.

## Build / test / run

Requires **JDK 21** and the Android SDK (compile/target SDK 35).

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=$HOME/Android/Sdk

./gradlew assembleDebug          # debug APK  -> be.moraine.yapl.debug
./gradlew assembleRelease        # minified release APK (R8) -> be.moraine.yapl
./gradlew testDebugUnitTest      # JVM unit tests

# install on a device/emulator listed by `adb devices`
adb -s <device> install -r app/build/outputs/apk/debug/app-debug.apk
```

A release build is **signed only if `keystore.properties` exists** at the repo root
(gitignored); otherwise it builds unsigned (F-Droid signs its own). Inspecting a
device DB: `run-as be.moraine.yapl.debug cat databases/pilotlog.db` (+ checkpoint WAL).

## Architecture

Clean Architecture under `app/src/main/java/dev/pilotlog/`:
- **domain/** — models, repository interfaces, use cases (pure Kotlin, unit-tested)
- **data/** — Room (DAOs, entities, mappers), repository impls
- **ui/** — Jetpack Compose (Material 3), view models, navigation
Hilt for DI, Room for persistence, `kotlinx-datetime` for dates.

## Conventions & gotchas (read before committing)

- **Never commit personal data.** The maintainer's real flight history, backups,
  paper-logbook scans and one-off migration scripts live in `/legacy/` (gitignored),
  and exports match `*-flights.csv` / `yapl-logbook*.pdf`. **Run `git status` before
  every commit** and confirm no flight data is staged.
- **Anchor `.gitignore` patterns with a leading `/`.** A bare `legacy/` once also
  matched the source package `app/src/.../usecase/legacy/` and silently dropped
  `ImportLegacyFlightsUseCase.kt` from the repo, breaking F-Droid's clean build.
  Use `/legacy/` (repo-root only).
- **Stay permission-free and offline.** No Android permissions, no network code, no
  analytics/ads/trackers — this is required for F-Droid.
- **Database changes:** bump the Room `version`, add a `Migration`, register it in
  `DatabaseModule`, keep the exported schema in `app/schemas/`, then regenerate the
  bundled asset with `python3 scripts/build_airports_db.py` (its `user_version` must
  match the DB version). The asset ships **airports only — no aircraft seed** (empty
  hangar); never re-add personal aircraft to the seed.
- **New `.kt` files** start with `// SPDX-License-Identifier: GPL-3.0-only`.
- **Pure logic** (e.g. `domain/logbook/LogbookPaging.kt`) is unit-tested — add tests
  for new domain logic.

## Releasing a new version (F-Droid)

F-Droid uses **reproducible builds**: it rebuilds from source and verifies it matches
the developer-signed APK published on GitHub Releases. To cut a release, follow the
**`release-yapl` skill** (run `/release-yapl`) — it covers the version bump, tag,
signed APK, GitHub Release, and the fdroiddata recipe. Key invariants: the fdroiddata
`Builds.commit` must be a **full 40-char hash** (not a tag), the signing key never
changes (`AllowedAPKSigningKeys` is fixed), and `Binaries`/`AutoUpdateMode: Version`/
`UpdateCheckMode: Tags` let F-Droid auto-detect new tags.
