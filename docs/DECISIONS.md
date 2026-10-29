# Decisions

## D1: Engine, scope, renderer, game code (2026-10-26)

Engine: **iortcw** (`github.com/iortcw/iortcw`, pinned `438e7d41`) + **SDL2** + **gl4es**, same stack as 3DQuake.

Scope options: (1) SP only, (2) MP only, (3) both. Choice: **SP first** (the main game, `SP/` tree).
MP (`MP/` tree, same layout) can be added later as a second set of modules; not needed for the acceptance criteria.

Renderer options: `SP/code/renderer` (fixed-function GL1) vs `rend2` (GL 3.x shaders). Choice: **GL1 renderer**,
linked statically into `libmain.so` (no `USE_RENDERER_DLOPEN`), through gl4es as in 3DQuake.
Note: the SP GL1 renderer also has its own `USE_OPENGLES` (GLES 1.1) path; not used, gl4es keeps the 3DQuake patches reusable.

Game code (spike item 1): the original paks contain **no QVMs**; the GOG install ships only x86 DLLs
(`qagamex86.dll`, `cgamex86.dll`, `uix86.dll`, `*_mp_x86.dll`). iortcw builds cgame/qagame/ui as native shared
libraries (`BUILD_GAME_SO`; `vm_cgame` etc. default 0 = native). Choice: build them with our CMake as
`libcgame.so`, `libqagame.so`, `libui.so` (packed in the APK's lib dir) and load them by name with `dlopen`.
Patch: `VM_Create` on Android calls `Sys_LoadGameDll("lib<module>.so")` directly and skips the pak/fs search
(a `.so` in `files/` could not be dlopen-ed anyway: external storage is mounted noexec).
Consequence: pure-server checks of the original DLLs do not apply; `sv_pure` is irrelevant for SP.

Other build choices (carried over from 3DQuake D2): own CMake, arm64-v8a, no OpenAL (SDL audio), no curl,
no Ogg/Opus yet (`snd_codec_ogg` not built; check music formats in the next stage), `NO_VM_COMPILED`.
SP engine needs C++ for `splines/*.cpp` (camera splines). Internal zlib 1.2.11 (unzip) and jpeg-8c.

Data: GOG install provides `Main/pak0.pk3`, `sp_pak1-4.pk3`, `mp_pak*.pk3`. No demo is used. Game data dir is
`Android/data/io.github.noneawe.threedrtcw/files/main/`. Launcher check: `main/pak0.pk3` contains `maps/assault.bsp`.

Stage 0 status: APK builds (`./gradlew assembleDebug`). Verified on the x86_64 emulator (arm64 translation) with the GOG data: FS loads 6384 files, gl4es context up, SDL audio up, native `libui.so` loaded, intro RoQ cutscene plays. Main menu renders (menu is stretched to the wide screen: HUD aspect fix pending); `devmap escape1` via `startup.cfg` loads and runs with HUD. Menu taps do not work yet (touch glue pending). Findings: `GL_ARB_multitexture` not found (disabled, as in 3DQuake); `vid_restart` after `highVidhighCPU.cfg` needed a fix (display index) to keep mode -2; Ogg not built yet.

## D-release: public repository (2026-10-29)
The public repo is a fresh, single-commit copy of the tracked files with a new launcher icon (castle silhouette, red/cyan
"3D", "SBS"; own artwork, no game logos) and `release/3DRTCW-0.1.apk`, built from that repo with `scripts/setup.sh` +
`./gradlew assembleRelease`. The APK holds only our code and libraries (no pk3, maps, videos, logos); the menu pak is built
on the device from the user's own files.
