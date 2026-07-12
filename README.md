# Shadow — The Clone App

Shadow is a native Android app (Kotlin + Jetpack Compose) that creates and runs
**local, isolated clones of installed apps** — entirely on-device.

- 100% local: no cloud, no accounts, no servers, no analytics.
- No root, no Shizuku, no work profile, no device/GPS spoofing.
- Split-APK (App Bundle) aware: clones apps shipped as `base.apk` + `split_config.*`.
- Unlimited clones via incrementing user IDs.

## Status

Early development. **Phase 1** validates the core capability: clone and launch a
single split-APK app on-device. UI polish and the multi-clone manager come later.

## Architecture

- **`app/`** — Kotlin + Compose UI (Material 3, light theme). This is the part we build.
- **`opensdk/`** — the WaxMoon/MultiApp virtualization engine. Integrated as-is; not rewritten.

Cloning gathers an installed app's base APK + all split APKs into one directory and
installs them through the engine's split-aware directory install, then launches the
clone by user ID.

## Building

Builds run on **GitHub Actions** (see `.github/workflows/build.yml`), not locally.
The debug APK is published as the `shadow-debug-apk` artifact at
`app/build/outputs/apk/debug/`.

## Credits & License

This project embeds the **WaxMoon/MultiApp** virtualization engine
([MultiApp](https://github.com/WaxMoon/MultiApp),
[opensdk](https://github.com/WaxMoon/opensdk)), which is licensed under **AGPL-3.0**.
Because Shadow links that engine, this project is distributed under **AGPL-3.0** as well.
See `LICENSE` and `NOTICE`.

Shadow is an independent front-end. It is not affiliated with or endorsed by WaxMoon.
