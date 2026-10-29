# Project: 3DRTCW

Unofficial Android port of the **iortcw** engine (single player tree) with stereoscopic 3D (side-by-side full/half,
red/cyan anaglyph, lens distortion for phone viewers), full gamepad support and a live "3D Depth" slider.
Not affiliated with id Software, Gray Matter, Activision or ZeniMax. Sister project: 3DsBsQ3 (Quake III Arena, same goals
and architecture; many decisions carry over).

## Hard rules
- **No game assets** in the repository or the APK: no `*.pk3`, maps, textures, sounds, videos, logos, demo data.
  Menus are built on the device from the user's own `ui/system.menu` (`PakCheckActivity`); never ship them.
- Do not use "Return to Castle Wolfenstein" or "Wolfenstein" in the app name or launcher; present it as an unofficial port.
  Do not use the game's logos for the icon.
- Keep licence headers (iortcw RTCW parts GPLv3, ioquake3 parts GPLv2, SDL2 zlib, gl4es MIT). New files get a GPLv3
  header. Keep `LICENSE` and `NOTICE` current. Do not relicense.
- If something is impossible (missing content, unsupported format), document it in `docs/`; do not work around it silently.
- Menus must not crash when content is missing.
- Record architectural decisions in `docs/DECISIONS.md`.

## Layout
```
app/              Android Gradle project (PakCheckActivity launcher, SDLActivity glue in org.libsdl.app, jni/CMakeLists.txt,
                  assets/stereo_tab.menu = our 3D/Pad menu tabs)
engine/patches/   numbered patches against iortcw (NNNN-*.patch) and gl4es (gl4es/*.patch)
engine/{iortcw,gl4es,SDL}   cloned by scripts/setup.sh at pinned commits, git-ignored
ui/pk3/           pad_defaults.cfg, packed into zz~stereo.pk3 and shipped in the APK
scripts/          setup.sh (fetch + patch), make_icon.py (launcher icon, own artwork)
docs/             BUILD, STEREO, INPUT, TOUCH, DECISIONS; docs/img = small illustration screenshots
release/          prebuilt APK (debug-signed)
```

## Build
JDK 17, Android SDK 34, NDK 28.2.13676358, CMake 3.22.1. `scripts/setup.sh` then `./gradlew assembleRelease`
(or `assembleDebug`). arm64-v8a only, `minSdk 24`, 16 KB page size. Package `io.github.noneawe.threedrtcw`, data dir
`Android/data/<pkg>/files/main/`.

## Changing engine code
Edit the working tree in `engine/iortcw`, then store the change as a numbered patch in `engine/patches/` (one concern per
patch, update the owning patch instead of stacking fixups). Verify: fresh clone + `scripts/setup.sh` + `diff -r` against the
working tree. After changing a header used by the game modules (`cgame`, `qagame`, `ui`), rebuild from clean.
After a scripted edit, grep to confirm it applied (silent no-ops happened before).

## Architecture notes
- Renderer: fixed-function GL1 renderer, linked statically into `libmain.so`, through gl4es to GLES 2.
- Game modules `libcgame.so` / `libqagame.so` / `libui.so` are built with our CMake and loaded with `dlopen` (original paks have no QVMs).
- Stereo: scene drawn twice per frame, eye = half the window reported as `vidWidth`; HUD/menus once per eye with no
  parallax; SBS goes offscreen and each eye is presented through a 32x32 lens mesh. `glconfig_t` is unchanged
  (game-module ABI): flags travel as cvars (`r_stereoMode`, `r_stereoSeparation`, `r_stereoConvergence`, `r_stereoLens`,
  `r_lensStrength`, `r_stereoShift`, `r_stereoMargin`, `r_renderScale`).
- Input: SDL_GameController (first real pad); in menus the pad becomes keys (A Enter, B/Start Esc, D-pad arrows,
  LB/RB tab switch). Aim assist `cg_aimAssist`, zoom `+padzoom`.
- Test hook: `startup.cfg` next to `pak0.pk3` is executed at start. Delete it afterwards.

## Testing tips
- `adb logcat -s ioq3`; `developer 1` logs pad and UI keys. Emulator: software GL, `adb input` keys work after one tap,
  pad events do not arrive. Test pad behaviour on a real phone and pad.
- Dirs created with `adb shell` are unreadable by the app; let the app create them.

## Status / open items
Working: SBS/half/anaglyph, 3D tab and Depth slider, lens/shift/margin, pad in game and menus, touch fallback, HUD aspect.
Open: real-device re-check of recent pad fixes, action-scene and scope checks in SBS, lens border stair-steps (coarse mesh),
Ogg music, MP, English launcher text, signed release.
