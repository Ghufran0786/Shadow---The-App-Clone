# Shadow — The Clone App · PRD (v2.0 — MultiApp rebuild)

## 1. Summary
Shadow is a NATIVE ANDROID app that creates and runs UNLIMITED independent, isolated LOCAL clones
of installed (non-payment) apps, built on the WaxMoon/MultiApp virtualization engine — which, unlike
our previous engine, supports split-APK App Bundles. No cloud, no accounts, no server, no Shizuku,
no work profile, no root.

## 2. Why this rebuild
The previous attempt used NewBlackbox, which cannot install split-APK App Bundles (that capability is
paywalled in VirtualApp's commercial engine). Every modern app (SmartQ, JioCinema, Naukri…) ships as
base.apk + split_config.* , so cloning always failed at install. MultiApp is the free/open-source
engine that DOES support split-APK install (via a directory of APKs). Virtualization is already
proven to work on the target device (Parallel Space clones SmartQ there); the only barrier was the
engine's missing split support and the paywall on 3rd-party apps.

## 3. Goals
- Clone any user-selected NON-PAYMENT installed app N times, with NO artificial cap.
- Each clone = isolated local data / separate login.
- Correctly handle split-APK App Bundles (base + all splits).
- Simple, bright, white UI.
- Build entirely via GitHub Actions (no local Android Studio/emulator).

## 4. Non-Goals
- NO payment / banking / wallet apps.
- NO cloud, accounts, servers, analytics.
- NO device-ID / GPS spoofing (MultiApp may ship these — keep OFF/hidden).
- NO Shizuku, NO work profile, NO root.
- Not for Google Play; sideload only.

## 5. Primary validation target
SmartQ — package com.thesmartq.smartq (installed as base + split_config.arm64_v8a + split_config.xxhdpi).
Phase 1 is considered successful ONLY when SmartQ clones AND launches.

## 6. Architecture
- app module (Kotlin + Compose): white UI + clone manager. WE BUILD THIS.
- MultiApp OpenSDK / engine: virtualization core. INTEGRATE — do NOT rewrite.
- Cloning an installed app: gather base sourceDir + ALL splitSourceDirs, copy them into ONE temp
  directory, call MultiApp's split-aware install (e.g. installApkFiles(dir, userId, forceInstall=true)),
  then launch by userId.
- Unlimited clones: assign incrementing userId (0,1,2,3…), no cap.
- Storage: on-device app-private directories only.

## 7. Tech stack & constraints
- Kotlin, Jetpack Compose (Material 3 LIGHT theme).
- Gradle wrapper, JDK 17. compileSdk 34; targetSdk 28 if the engine needs it; minSdk per engine.
- Single ABI arm64-v8a for test builds (device is arm64-v8a / iQOO 7).
- CI: GitHub Actions on ubuntu-22.04; artifacts via actions/upload-artifact@v4.
- No local builds — CI only. Test by installing the artifact APK on the real phone.

## 8. Non-Functional
- Speed: cold clone launch < 3s target; lazy-init the engine.
- Security: minimal permissions in UI layer; UI layer requests NO INTERNET; R8 for release; all data local.
- Reliability: the app must NEVER hang on "installing" — 60s timeout + on-screen error + full logging
  (tag "ShadowClone"). Provide per-clone force-stop + safe restart.

## 9. Known engine risk (watch for this)
MultiApp's GitHub is unmaintained since 2023, so Android 13/14 quirks are possible. That is exactly
why Phase 1 validates cloning SmartQ BEFORE any UI is built. If MultiApp fails on-device, fallbacks:
(a) merge splits into a single universal APK (APKEditor) then install; (b) Shizuku multi-user.

## 10. UI Spec (white-forward)
Material 3 LIGHT only. Background #FFFFFF, surfaces #FAFAFA, text #1A1A1A, single accent #2F6FED,
dividers #ECECEC, generous whitespace. No dark mode in v1.
Screens: Home (clone cards + FAB) · Add Clone (searchable installed-app list) ·
Clone detail (launch/rename/force-stop/delete) · Settings (about, storage used, clear data).

## 11. Phases (CAPABILITY-FIRST — do not build UI before cloning works)
- Phase 1: CI green + CLONE & LAUNCH SmartQ from a minimal screen.  ← the whole risk lives here
- Phase 2: Clone ANY selected installed app (split-aware), launch it.
- Phase 3: Unlimited-clones manager (userIds, DataStore persistence, create/list/delete/force-stop).
- Phase 4: White UI polish (Home / Add-Clone / Clone-detail / Settings).
- Phase 5: Storage isolation + optional PIN/biometric lock + permission audit.
- Phase 6: Performance + signed release (tag-triggered, keystore via GitHub secret).
- Phase 7: Stabilization + smoke tests.

## 12. Lessons baked in (must not recur)
- Runner flakiness → pin ubuntu-22.04, add workflow_dispatch, re-run on transient GitHub errors.
- Missing JitPack → add maven("https://jitpack.io") in settings.gradle.
- abiFilters vs splits conflict → use exactly ONE ABI mechanism.
- NDK license not accepted → accept licenses + install NDK in CI before building.
- Split-APK App Bundles → install base + ALL splits (MultiApp directory install). Never base-only. Never abort on splits.
- Silent infinite "installing" → background thread + 60s timeout + error surfacing + ShadowClone logs.
- Validate the clone (SmartQ) BEFORE building the UI.

## 13. Licensing
MultiApp is open-source — keep its LICENSE and add a NOTICE crediting WaxMoon/MultiApp.