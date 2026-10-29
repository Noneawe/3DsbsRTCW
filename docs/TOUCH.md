# Touch controls (emergency fallback)

`SP/code/client/cl_touch.c` (patch 0006), ported from 3DQuake. SDL touch-to-mouse is off; fingers are handled directly.

- **In a game:** overlay with FIRE, JUMP, DUCK, USE (`+activate`), RLD (`+reload`), `<` `>` (weapons), OBJ (`+scores`), MENU (Escape). Left 45 % = floating move stick, the rest = look. Up to 6 fingers.
- **In a menu:** a touch is a mouse click. The cursor is set in the UI's 640x480 space stretched over the whole window (RTCW menus are not letterboxed yet).
- `cl_touch 1` (default) = only while no gamepad is connected, `2` = always, `0` = off. The emulator reports a gamepad, so use `cl_touch 2` there to see the overlay.
- Verified on the emulator: tapping PLAY in the main menu opens the skill screen. Not verified: in-game overlay on RTCW, feel on a real phone.
