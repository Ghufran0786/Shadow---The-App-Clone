# Shadow — Agent Rules (READ BEFORE EVERY TASK)

## Project
Shadow = NATIVE ANDROID app (Kotlin + Jetpack Compose) that clones installed apps UNLIMITED times,
fully LOCAL, built on the WaxMoon/MultiApp virtualization engine already in this repo. MultiApp
supports split-APK App Bundles — the exact capability our previous engine lacked. Read PRD.md.

## Absolute rules
1. NEVER build or run Gradle locally. No Android Studio/emulator/SDK on this machine. ALL builds run
   on GitHub Actions. After ANY change, output the exact git commands to push and where to download the APK.
2. NO npm / JavaScript / React Native. Build system is Gradle only.
3. Engine = WaxMoon/MultiApp OpenSDK. DO NOT rewrite engine internals; only CALL its public API from
   the app module. If unsure of an API, READ the MultiApp README/opensdk/source FIRST — never invent method names.
4. CLONING IS SPLIT-APK AWARE. For an installed app: collect base sourceDir + ALL splitSourceDirs,
   copy them into ONE temp directory, and install via MultiApp's directory install
   (e.g. installApkFiles(dir, userId, forceInstall=true)). NEVER install base.apk alone. NEVER abort
   just because an app has split APKs.
5. Unlimited clones: assign incrementing userId (0,1,2,…) with NO maximum.
6. Storage is 100% LOCAL. No cloud/servers/accounts/analytics. The UI layer requests NO INTERNET permission.
7. NO Shizuku, NO work profile, NO root, NO device-spoofing / fake-GPS.
8. NEVER use payment/banking apps as samples or defaults. Validation target = SmartQ (com.thesmartq.smartq).
9. UI = Material 3 LIGHT only: #FFFFFF/#FAFAFA surfaces, #1A1A1A text, single accent #2F6FED, generous
   whitespace. NO dark mode.
10. Keep it FAST: single arm64-v8a ABI for test builds; lazy engine init; no needless services.

## Never hang, always log
- Log tag "ShadowClone" at every clone step: packageName, sourceDir, splitSourceDirs, install result
  code, launch result, and any exception via Log.e("ShadowClone", msg, e) with full stacktrace.
- Run install on Dispatchers.IO inside withTimeout(60_000). On timeout/exception/failed result: leave
  the Loading state and show the real error on screen. The app must NEVER spin forever on "installing".

## CI (baked-in fixes — do NOT regress)
- .github/workflows/build.yml: on push to main + workflow_dispatch; runs-on: ubuntu-22.04.
- actions/checkout@v4; actions/setup-java@v4 (temurin, java 17, cache gradle).
- Step: yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null 2>&1 || true
- chmod +x ./gradlew; ./gradlew assembleDebug --stacktrace; actions/upload-artifact@v4
  (name shadow-debug-apk, path app/build/outputs/apk/debug/*.apk).
- settings.gradle repositories: google(), mavenCentral(), maven("https://jitpack.io").
- Use exactly ONE ABI mechanism (abiFilters OR splits, never both); target arm64-v8a only.

## Workflow
- Work ONE PHASE AT A TIME (PRD §11). Phase 1 = clone + launch SmartQ. Do NOT build the full UI
  before SmartQ clones successfully. After each phase, STOP and wait for my CI + on-device confirmation.
- Prefer small, reviewable changes; state assumptions instead of asking trivial questions.
- Keep MultiApp's LICENSE; maintain a NOTICE crediting WaxMoon/MultiApp.