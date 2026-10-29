# Stereo 3D

Renderer: `SP/code/renderer` (GL1, patch 0008, ported from 3DQuake). One frame = two passes (`SCR_DrawScreenField` per eye), one buffer swap.

## Modes (cvars)
- `r_stereoMode` (archived, latched: needs `vid_restart`): 0 off, 1 side-by-side, 2 anaglyph (red/cyan by default, `r_anaglyphMode` selects the variant).
- `r_stereoHalf` (latched): 0 = "full" SBS, one-eye aspect per half; 1 = half-SBS (full-width picture squeezed into each half, for 3D TVs/projectors).
- `r_stereoSeparation` (live, default 1): eye offset in world units. 0 = no parallax. (Stock iortcw default was 64 as a divisor; an old archived 64 would now be a huge offset.)
- `r_stereoConvergence` (live, default 64): distance of the zero-parallax plane. Parallax is an off-axis (shifted frustum) projection.
- `r_renderScale` (latched, 0.25 - 1): render into an FBO of that fraction of the window and stretch it.

## How
- The engine reports `vidWidth` = one eye (half the window) so cgame/UI lay out HUD and menus per eye. HUD, menus and console have zero parallax.
- `drawBufferCommand_t.stereoFrame` -> `backEnd.sbsOffsetX` is added to viewport and scissor of 3D views and 2D drawing.
- UI 3D scenes (`RDF_NOWORLDMODEL`, e.g. model previews) are drawn without parallax.
- `r_stereoMode` is read through the cvar in `SCR_UpdateScreen`; `glconfig_t` is not changed (game-module ABI).
- RTCW unit scale is close to Q3 (player ~ 66-72 units), so the 3DQuake defaults (1 / 64) were kept; to be re-tuned by eye.

## Verified (emulator, `escape1`, started through `startup.cfg`)
- SBS full: two half-width views, small parallax, HUD in both halves; with `r_stereoSeparation 0` both halves are identical.
- Half-SBS: squeezed picture in each half, HUD per half.
- Anaglyph: red/cyan fringes at depth edges, a single HUD.
- Seen once: in the intro camera of `escape1` the two eyes showed clearly different yaw (the camera moved between the two cgame passes). To check on cutscenes/spline cameras (open item 6/8 of the brief).

## Menu: Options -> System -> "3D" tab
RTCW menus are `.menu` scripts inside the game paks (game data: not shipped here). `PakCheckActivity.installMenus` builds `zz~menus.pk3` on the device at every start: the user's own `ui/system.menu` and `ui/ingame_system.menu` (newest pak wins) plus our `assets/stereo_tab.menu` inserted before the final two closing braces. The tab has **3D Depth** (`r_stereoSeparation`, slider 0 - 2, live), **Convergence** (16 - 256, live), **Stereo 3D** (Off / Side by Side / Anaglyph), **Half-width SBS**, **Render Scale**, and **Apply (restarts video)** (`exec "vid_restart"`). All cvars are archived. If the menu files are missing nothing is written and the game starts without the tab.
Checked on the emulator: the tab appears, the slider moves, Apply restarts video and the changed mode took effect. The in-game (`ingame_system`) copy is patched the same way but not opened yet. The sliders show no number (stock widget).

## HUD / menu aspect and FOV (patch 0009)
iortcw already has the machinery (`cg_fixedAspect`, `ui_fixedAspect` = the same cvar); we only change defaults and add one thing:
- `cg_fixedAspect 2` (latched, archived; first-run default, a saved config keeps its value): menus and HUD use one scale for both axes (a 4:3 block, centred); `2` also widens a few menu backdrops.
- `cg_fixedAspectFOV 0`: the stock `1` would force 90 degrees; with `0`, `cg_fov` is the horizontal angle for a 4:3 screen and widens on wide screens (Hor+, `CG_CalcFov`; zooms/binoculars go through the same conversion). In full SBS an eye is about square, so the angle narrows a little to keep the vertical angle.
- `cg_hudCenter 1` (new): HUD elements anchored left/right stick to the centred 4:3 block instead of the screen edges (in a viewer the edges are not seen).
- Touch in menus maps the finger position through the same centred block (`menuCursor`).
Checked on the emulator (2400x1080): main menu, skill menu and quit dialog are a centred 4:3 block; HUD (health/ammo) sits inside it; a tap on PLAY / NO hits the right item. Not checked: scoped views (sniper/snooper masks have their own aspect code), cutscene overlays, MP.

## Lens distortion, image shift, margin for viewers (patch 0010)
Ported from 3DQuake 0020 (+ 0013: `qglFlush` after the last 2D batch, needed under gl4es). Side by side is always rendered offscreen and each eye is presented through a 32x32 mesh into its half of the window:
- `r_stereoLens` (live, default 1): barrel pre-distortion `f(r) = 1 + k1 r^2 + k2 r^4`, `k1 = 0.022 * r_lensStrength`, `k2 = 0.024 * r_lensStrength`. `r_lensStrength` (0 - 20, default 8 = the 3DQuake values 0.176 / 0.192). The 3DQuake cvars `r_lensK1/K2` were replaced by this single strength. Corners that would sample outside the picture are black.
- `r_stereoShift` (-0.3 - 0.3, default 0.15): moves each eye's picture toward the screen centre (puts the picture on the lens axis; apps do this with the IPD).
- `r_stereoMargin` (0 - 0.5, default 0.26): shrinks each picture (aspect kept), black margins for viewers that show only part of the screen.
- Menu: the "3D" tab has Lens Distortion, Lens Strength, Image Shift, Image Margin (all live).
- Defaults are the values that worked on the 3DQuake author's phone and viewer; they apply only while Side by Side is on.
Checked on the emulator (full SBS, `escape1`): two shrunk, barrel-distorted pictures with black margins, HUD in both. Not checked: through a real viewer; touch input does not know about shift / margin / lens (use a pad with a viewer).

## Weapon model, crosshair and scoped views (patch 0012)
- **Weapon model:** it is a world entity with `RF_DEPTHHACK`; the renderer already draws it with `zProj = r_znear`, and our eye offset scales with `zProj / r_stereoConvergence`, so the weapon gets almost no parallax (sits on the screen plane) and is not clipped by the off-axis projection. Checked: Mauser model identical in both eyes, complete.
- **Crosshair and reticles:** stock cgame draws a *3D* crosshair (`CG_DrawCrosshair3D`) for stereo frames and no 2D crosshair, so the scope / snooper / binocular overlays (drawn from `CG_DrawCrosshair`) were missing in stereo. Now the 2D `CG_DrawCrosshair` runs for every eye (zero parallax, on the screen plane) and the 3D one is gone.
- **Scope side masks** use `PLACE_LEFT/RIGHT` as real screen edges; with `cg_hudCenter` they would have moved into the 4:3 block (visible as unmasked sides). `CG_SetEdgePlacement` switches `cg_hudCenter` off while the masks are drawn (sniper, snooper, FG42, binoculars).
- **Zoom:** `r_stereoZoom` (default 1): when the view angle is narrower than the 4:3 reference the eye offset is scaled by `tan(fovX/2) / (0.75 * aspect)`, so the parallax on the screen stays about the same instead of growing with the magnification.
- Checked on the emulator (`escape1`, `give all`, `+zoom` binoculars): mono shows the circular binocular mask over the full screen; SBS shows it in both eyes. The Mauser/snooper/sniper scopes themselves were not looked at (only the binocular overlay).

## Cutscenes (patch 0013)
- Full-screen engine cinematics (RoQ: intro, outro; `CL_handle`) were stretched over the whole window (640x480 virtual coordinates). With `cg_fixedAspect` they now keep 4:3, centred, on black; in SBS the same picture is drawn in both eyes on the screen plane (zero parallax, legible). Checked on the emulator with `cinematic wolfintro.RoQ` in SBS.
- In-game spline cameras (`camera`, `cg.cameraMode`) use the same cgame view path as normal play (`trap_getCameraInfo(..., cg.time, ...)`, deterministic per frame), 2D HUD is hidden there. The one-off case where the two eyes showed different yaw in the `escape1` start was not reproduced: 40 screenshots of the same start in SBS (lens/shift/margin off) have a constant left/right difference (mean 2.4 grey levels, normal parallax). A real camera scene was not triggered (`camera crypt01` did not start one).

## Not done yet
- Sniper / snooper / FG42 scopes in stereo; an in-game spline camera scene in stereo.
- Touch in SBS (3DQuake 0014): `cl_touch.c` already folds x for SBS, untested here.
- Real viewer, performance.
