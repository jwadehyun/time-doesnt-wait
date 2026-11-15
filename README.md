# Time Doesn't Wait

Every hour on the hour your phone asks one question: *what did you just do?* Answer from the
notification, rate the day at bedtime. English + Korean, Halftone Cobalt 1-Bit design.

- Design doc: https://claude.ai/artifact/4iQ1qSwZ9uWnru9QsLywxA
- Stitch project: "Y2K Hourly Activity Journal"

## Layout

```
app/                    Kotlin Multiplatform (Android now, iOS later)
  shared/               domain rules, SQLDelight database, Compose UI, EN/KO strings, fonts
  androidApp/           alarms, notifications + inline reply, permissions, MainActivity
backend/                Java 21 + Spring Boot 4, Flyway, PostgreSQL (sign-in + sync arrive in M3)
docker-compose.yml      PostgreSQL 16 + API for local runs / the home server
.github/workflows/ci.yml  tests + APK + Docker image on every push
```

## Requirements

- JDK 21 (`brew install openjdk@21`)
- Android SDK with platform 37 (`brew install --cask android-commandlinetools`, then
  `sdkmanager "platforms;android-37.0" "platform-tools"`), and `app/local.properties` containing
  `sdk.dir=/opt/homebrew/share/android-commandlinetools`
- Docker Desktop (backend and backend tests)

## Run

```sh
# Android: tests, build, install on a connected phone (USB debugging on)
cd app
./gradlew :shared:jvmTest
./gradlew :androidApp:installDebug

# Backend
cd backend && ./gradlew test        # needs Docker for Testcontainers
docker compose up --build           # from the repo root
curl localhost:8080/api/v1/status
```

## Status

- **M0 Foundations**: done. Repo, KMP app skeleton, theme + pixel fonts, EN/KO strings,
  Spring Boot + PostgreSQL in Docker, CI.
- **M1 Core loop, offline**: built, needs the on-phone gate (a week of real check-ins).
  Local DB, Quick Log, Timeline with backfill, Check-in Hours, permissions setup, exact alarms,
  stacking check-in notifications with inline reply + one-tap buttons, category guessing.
- M2 Review/goals/stats, M3 accounts + sync, M4 polish: not started.

Fonts: Galmuri11 (SIL OFL, see `docs/FONT-LICENSE-Galmuri.txt`), Space Grotesk and Space Mono (SIL OFL).
