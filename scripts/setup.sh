#!/bin/sh
# SPDX-License-Identifier: GPL-3.0-or-later
# Copyright (C) 2026 3DRTCW contributors
# Fetches the pinned engine sources into engine/ and applies our patches (idempotent).
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
E=$ROOT/engine

fetch() { # url dir sha
    if [ ! -d "$E/$2/.git" ]; then
        git init -q "$E/$2"
        git -C "$E/$2" remote add origin "$1"
    fi
    if [ "$(git -C "$E/$2" rev-parse HEAD 2>/dev/null)" != "$3" ]; then
        git -C "$E/$2" fetch -q --depth 1 origin "$3"
        git -C "$E/$2" checkout -q -f FETCH_HEAD
    fi
}

apply() { # dir patch
    if git -C "$E/$1" apply --check "$2" 2>/dev/null; then
        git -C "$E/$1" apply "$2"
        echo "applied $(basename "$2")"
    elif git -C "$E/$1" apply -R --check "$2" 2>/dev/null; then
        echo "already applied $(basename "$2")"
    else
        echo "patch $(basename "$2") does not apply to engine/$1" >&2
        exit 1
    fi
}

fetch https://github.com/iortcw/iortcw iortcw 438e7d413b5f7277187c35b032eb0ef9093ae778
fetch https://github.com/ptitSeb/gl4es gl4es a025615bc79ac43ec911dced8fe5b8eb45a0b72d
fetch https://github.com/libsdl-org/SDL SDL 0aef2c3612294b8ed4fb9626b8426f4c9c835c6d

for p in "$E"/patches/*.patch; do [ -e "$p" ] || continue; apply iortcw "$p"; done
for p in "$E"/patches/gl4es/*.patch; do [ -e "$p" ] || continue; apply gl4es "$p"; done
