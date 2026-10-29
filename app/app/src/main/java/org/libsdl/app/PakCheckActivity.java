// SPDX-License-Identifier: GPL-2.0-or-later
// Copyright (C) 2026 3DRTCW contributors

package org.libsdl.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** Launcher: starts the game when pak0.pk3 is present, otherwise says where to put it. */
public class PakCheckActivity extends Activity {
    private static final String[] GAME_DIRS = {"main"};

    private File filesDir() {
        File dir = getExternalFilesDir(null);
        return dir != null ? dir : getFilesDir();
    }

    /** pak0.pk3 files found but unusable (not a readable zip, or no maps/assault.bsp, e.g. a cut-short copy). */
    private final StringBuilder broken = new StringBuilder();

    private boolean hasPak0() {
        broken.setLength(0);
        boolean ok = false;
        for (String d : GAME_DIRS) {
            File pak = new File(filesDir(), d + "/pak0.pk3");
            if (!pak.canRead()) continue;
            // ponytail: structure check only, not a hash of the id data (no known-good hashes shipped);
            // the engine itself recognises the demo pak0 by its zip checksum
            try (ZipFile zip = new ZipFile(pak)) {
                if (zip.getEntry("maps/assault.bsp") != null) { ok = true; continue; }
            } catch (IOException e) {
                // fall through
            }
            broken.append(d).append("/pak0.pk3\n");
        }
        return ok;
    }

    /** Copies our own zz~stereo.pk3 (optional asset; not "zz_" (FS_AddGameDirectory renames zz_* to sp_*) and not "zzz" (FS_PathCmp upper-cases letters, so it would sort before the sp_ paks and lose to them): '~' sorts last) (pad defaults) from the APK next to pak0.pk3; always, so it matches this build. */
    private void installOwnPk3(File dir) {
        File target = new File(dir, "zz~stereo.pk3");
        target.delete();    // a copy put there by another user (adb, file manager) may not be writable
        try (InputStream in = getAssets().open("zz~stereo.pk3");
             OutputStream out = new FileOutputStream(target)) {
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } catch (IOException e) {
            // keep going: an older copy may already be there
        }
    }

    private static final String[] MENU_FILES = {"ui/system.menu", "ui/ingame_system.menu"};
    // later paks override earlier ones (sp_pakN sort after pak0)
    private static final String[] MENU_PAKS = {"sp_pak4.pk3", "sp_pak3.pk3", "sp_pak2.pk3", "sp_pak1.pk3", "pak0.pk3"};

    /**
     * Builds zz~menus.pk3 next to the user's paks: their System menus plus our "3D" tab. The menus are game
     * data, so they are derived on the device from the user's own files and never shipped in the APK.
     */
    private void installMenus(File dir) {
        File target = new File(dir, "zz~menus.pk3");
        target.delete();
        String tab;
        try (InputStream in = getAssets().open("stereo_tab.menu")) {
            tab = new String(readAll(in), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return;
        }
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(target))) {
            for (String name : MENU_FILES) {
                String text = readFromPaks(dir, name);
                if (text == null) continue;
                // insert before the two closing braces (menuDef and file block)
                int end = text.lastIndexOf('}');
                int menuEnd = end > 0 ? text.lastIndexOf('}', end - 1) : -1;
                if (menuEnd < 0) continue;
                out.putNextEntry(new ZipEntry(name));
                out.write((text.substring(0, menuEnd) + "\n" + tab + "\n" + text.substring(menuEnd))
                        .getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        } catch (IOException e) {
            target.delete();
        }
    }

    private static String readFromPaks(File dir, String name) {
        for (String pak : MENU_PAKS) {
            File f = new File(dir, pak);
            if (!f.canRead()) continue;
            try (ZipFile zip = new ZipFile(f)) {
                ZipEntry e = zip.getEntry(name);
                if (e == null) continue;
                try (InputStream in = zip.getInputStream(e)) {
                    return new String(readAll(in), StandardCharsets.UTF_8);
                }
            } catch (IOException e) {
                // try the next pak
            }
        }
        return null;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        return bo.toByteArray();
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (String d : GAME_DIRS) {
            File dir = new File(filesDir(), d);
            dir.mkdirs();
            installOwnPk3(dir);
            installMenus(dir);
        }

        if (hasPak0()) {
            startActivity(new Intent(this, SDLActivity.class));
            finish();
            return;
        }
        showMissing();
    }

    private void showMissing() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundColor(Color.BLACK);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad, pad, pad);

        TextView msg = new TextView(this);
        msg.setTextColor(Color.WHITE);
        msg.setTextSize(16);
        msg.setGravity(Gravity.CENTER);
        msg.setText((broken.length() > 0
                ? "Plik pak0.pk3 jest uszkodzony lub niekompletny (nieczytelny zip albo brak maps/assault.bsp):\n"
                  + broken + "Skopiuj go ponownie.\n\n"
                : "Brak pliku pak0.pk3.\n\n")
                + "Skopiuj pak0.pk3 (i sp_pak1.pk3...) z oryginalnej gry do:\n"
                + filesDir() + "/main/\n\n"
                + "Potem wróć tutaj.");
        msg.setTextIsSelectable(true);

        Button retry = new Button(this);
        retry.setText("Sprawdź ponownie");
        retry.setOnClickListener(v -> onResume());

        box.addView(msg, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(retry);
        setContentView(box);
    }
}
