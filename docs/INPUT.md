# Gamepad input

Layer: iortcw `sdl_input.c` on `SDL_GameController` (Xbox/PS/generic, Bluetooth/USB). The first game controller is used (patch 0005): Android lists virtual devices first.

## Default bindings (`ui/pk3/pad_defaults.cfg`, in `zz~stereo.pk3`)
Applied once, when no `wolfconfig.cfg` exists yet; afterwards the user's saved binds win.
The pk3 is named `zz~stereo.pk3`: not `zz_...` (`FS_AddGameDirectory` renames `zz_*` to `sp_*`) and not `zzz...` (`FS_PathCmp` upper-cases letters, so `ZZZ` sorts before the `ZZ_` of the renamed sp_ paks and would lose to them). `~` sorts after everything, so our files override the game's.

| Pad | Action |
|---|---|
| Left stick | move / strafe (analog) |
| Right stick | look (analog) |
| RT | fire (`+attack`) |
| LT | zoom / binoculars (`+zoom`) |
| A / B | jump / crouch |
| X | reload |
| Y | use / activate |
| L1 / R1 | previous / next weapon |
| D-pad up | alternate fire mode (`weapalt`) |
| D-pad down | notebook |
| D-pad left / right | lean left / right |
| L3 / R3 | sprint / melee kick |
| Back | objectives / scores (`+scores`) |
| Start | menu |

## Menus (patch 0007)
While a menu is up the pad is turned into keys: A = Enter, B / Start = Esc, D-pad or left stick = arrows, L1/R1 = PgUp/PgDn. The key sent on press is remembered so the release matches. Analog translation is off in menus.

## Cvars
`joy_threshold` (dead zone, 0.15), `joy_curve` (response exponent, 1.5), `in_joystickUseAnalog 1`, `j_yaw`, `j_pitch`.

## Gamepad settings screen (patch 0011): Options -> System -> "Pad" tab
Built like the "3D" tab (menu script inserted at install time into the user's own `system.menu` / `ingame_system.menu`, see STEREO.md). Contents:
- **Look Speed** (`j_lookScale`, 0.2 - 3), **Deadzone** (`joy_threshold`), **Response Curve** (`joy_curve`), **Invert Y** (`j_invertY`). `j_lookScale` / `j_invertY` are new, applied in `CL_JoystickMove`.
- One list per action (Fire, Zoom / binoculars, Jump, Crouch, Reload, Use, Next / Previous weapon, Alt fire mode, Notebook, Lean left / right, Sprint, Melee kick, Objectives) choosing a pad button or None. Choosing a button another action uses takes it from that action (shown as None). Applies at once and is saved with the config.
- **Reset pad to defaults** re-executes `pad_defaults.cfg`.
- Implementation: `uiScript padLoad / padApply / padReset` in `ui_main.c` read and write the key bindings through the UI traps; one `ui_pad_<action>` cvar per action holds the button name. Remapping uses lists, not "press a button", because pad keys are turned into keyboard keys while a menu is open. Sticks (move/look) are not remappable here.
- Pitfall: the menu parser merges adjacent quoted strings, so `cvarStrList` entries need commas.
- Checked on the emulator (touch): the tab shows the current binds, a change takes effect (Jump A -> None), Reset restores it (the reload happens a few frames after the exec, because `exec` runs from the command buffer).

## Status
Code ported from 3DQuake (same SDL layer). **Not verified with a pad**: `adb input gamepad` reaches SDL as "Unknown keycode 96" on the emulator (not as a controller event), so only the keyboard/touch menu path was checked. Still to do: a real-pad test.

## Pad fixes after the first real-pad test (GameSir Nova Lite, Bluetooth)

- **LB / RB** switch the tabs (buttons named `ctr_*`) of the focused menu (System: 3D / Graphics / ... / Pad). The opened tab is not tracked: the first press starts from the leftmost tab.
- **A** confirms the "Continue" button of the pre-mission screen (`pregame`); `CL_KeyDownEvent` only let the mouse through there.
- **Any pad button** skips scripted camera cutscenes (`cl.cameraMode`) and engine cinematics / demos (both only accepted keyboard keys).
- If no pad button is bound at start (config from before the pad support), `pad_defaults.cfg` is executed again (`CL_Init`). A config that has just one wrong pad bind is not repaired: use Options -> System -> Pad -> Reset.
- Status: pad detected on the phone ("Is gamepad: Yes"), the fixes are **not** confirmed by a hands-on test yet.
- **Save** with an empty "Save As" field names the game `<map>_<n>` (no keyboard needed).

## Aim assist (single player)

`cg_aimAssist` (0 - 1, default 0.6, Options -> System -> Pad -> Aim Assist). `CG_AimAssist()` (cgame, every frame) looks for a live AI of another team than the player (`teamNum`, neutral 7 excluded) that is visible (solid trace) and within the angle of a ~22 unit radius around the crosshair. While one is, it sets `j_aimScale` and `CL_JoystickMove` multiplies the stick look speed by it: 1 - 0.65 * strength (0.6 -> 61%). Only slows down the stick; there is no pull towards the target. Not applied to the mouse or touch. Status: compiles and runs, `j_aimScale` stays 1 with no enemy; **not tested against a real enemy yet**.

## LT = one zoom button (`+padzoom`)

Stock `+zoom` is the binoculars only: it needs the binoculars item found in a level and is disabled while a scoped weapon is held. `+padzoom` (cgame, patch 0016) decides per weapon: Mauser / FG42 / Garand (or their scope mode) -> toggles the scope like `weapalt`; any other weapon -> holds the binoculars. An old bind `PAD0_LEFTTRIGGER +zoom` is rewritten to `+padzoom` at start. Not tested with a real sniper rifle yet.

## Controls screen: leaving with B

A key-bind row (Options -> Controls) swallowed every key while it had focus, so Esc (pad B) did nothing once the cursor sat on a row. `Item_Bind_HandleKey` now only takes Enter / the mouse click (start of a capture) and the keys of a running capture; the release of the starting key is ignored. With `developer 1` the engine logs every pad key and every key the UI gets (`pad key ...`, `ui key ...`).
