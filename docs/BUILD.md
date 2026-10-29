# Build

## Requirements
JDK 17, Android SDK platform 34, `ndk;28.2.13676358`, `cmake;3.22.1`, host CMake >= 3.25, C compiler, `git`.
Set `sdk.dir` in `app/local.properties` (git-ignored) or `ANDROID_HOME`.

## Steps
```
scripts/setup.sh           # idempotent: clones engine/iortcw, engine/gl4es, engine/SDL at pinned commits, applies engine/patches
./gradlew assembleRelease  # APK: app/app/build/outputs/apk/release/app-release.apk
./gradlew assembleDebug    # faster, debug APK
```
`engine/iortcw`, `engine/gl4es`, `engine/SDL` are git-ignored; the repo only holds our patches
(`engine/patches/NNNN-*.patch`, `engine/patches/gl4es/*.patch`). To change the engine: edit the working tree under
`engine/iortcw`, then store the diff as a new numbered patch and verify with a clean clone + `scripts/setup.sh` + `diff -r`.
After changing a header used by the game modules, rebuild from clean.

## Signing
The release build uses the debug key so it installs for testing. For distribution create your own keystore and change
`signingConfig` in `app/app/build.gradle`.

## Icon
`python3 scripts/make_icon.py app/app/src/main/res` (needs Pillow) regenerates the launcher icons (own artwork).

## Debugging
- `adb logcat -s ioq3` for the engine log; `developer 1` also logs pad keys and UI key events.
- An optional `startup.cfg` next to `pak0.pk3` is executed at start (e.g. `devmap escape1`); delete it afterwards.
- `adb input` keys reach the game only after the window got a tap. Directories created by `adb shell` are not readable by
  the app: let the app create them.
