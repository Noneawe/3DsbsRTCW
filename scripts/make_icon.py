#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
# Copyright (C) 2026 3DRTCW contributors
# Generates the launcher icons (own artwork: a castle silhouette and "3D" in red/cyan anaglyph offset, "SBS" below).
# Needs Pillow. Legacy square icons, plus the adaptive icon's foreground and monochrome layers.
import sys
from PIL import Image, ImageDraw, ImageFont

FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
S = 512
BG = (18, 18, 24, 255)


def text_layer(txt, size, color, dx, y):
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    f = ImageFont.truetype(FONT, size)
    w = d.textlength(txt, font=f)
    d.text(((S - w) / 2 + dx, y), txt, font=f, fill=color)
    return im


def castle(color, dx):
    """Castle silhouette (keep with two towers and crenellations) on the upper part of the canvas."""
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    x = dx
    top, base = 70, 250
    for x0, x1, t in ((90, 190, top), (322, 422, top), (190, 322, top + 60)):  # towers, curtain wall
        d.rectangle((x0 + x, t + 22, x1 + x, base), fill=color)
        w = (x1 - x0) // 5
        for i in range(0, 5, 2):  # merlons
            d.rectangle((x0 + i * w + x, t, x0 + (i + 1) * w + x, t + 22), fill=color)
    d.polygon([(256 + x - 28, base), (256 + x - 28, 190), (256 + x, 168), (256 + x + 28, 190), (256 + x + 28, base)], fill=(0, 0, 0, 0))  # gate arch
    return im


def art(mono=False):
    """The artwork on a transparent S x S canvas; mono = all white (themed icon)."""
    red, cyan, white = (255, 0, 0, 255), (0, 255, 255, 255), (255, 255, 255, 255)
    parts = [castle(white if mono else red, -10), castle(white if mono else cyan, 10)]
    r = text_layer("3D", 165, white if mono else red, -9, 225)
    c = text_layer("3D", 165, white if mono else cyan, 9, 225)
    out = Image.alpha_composite(Image.alpha_composite(parts[0], parts[1]), Image.alpha_composite(r, c))
    if not mono:  # red+cyan overlap shows white, like real anaglyph
        for a, b in ((parts[0], parts[1]), (r, c)):
            ov = Image.eval(Image.composite(a.split()[3], Image.new("L", (S, S), 0), b.split()[3]), lambda v: 255 if v > 128 else 0)
            out = Image.alpha_composite(out, Image.composite(Image.new("RGBA", (S, S), white), Image.new("RGBA", (S, S), (0, 0, 0, 0)), ov))
    return Image.alpha_composite(out, text_layer("SBS", 80, white if mono else (235, 235, 245, 255), 0, 405))


def legacy():
    bg = Image.new("RGBA", (S, S), BG)
    ImageDraw.Draw(bg).rounded_rectangle((0, 0, S - 1, S - 1), 96, outline=(70, 70, 90, 255), width=6)
    return Image.alpha_composite(bg, art())


def adaptive_layer(mono=False):
    """108dp canvas: only the centre 66dp is always visible, so the art is scaled to fit it with a margin."""
    box = art(mono).getbbox()
    a = art(mono).crop(box)
    k = 0.5 * S / max(a.size)
    a = a.resize((round(a.width * k), round(a.height * k)), Image.LANCZOS)
    out = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    out.paste(a, ((S - a.width) // 2, (S - a.height) // 2), a)
    return out


if __name__ == "__main__":
    res = sys.argv[1]
    leg, fg, mono = legacy(), adaptive_layer(), adaptive_layer(True)
    for name, px in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
        leg.resize((px, px), Image.LANCZOS).save(f"{res}/mipmap-{name}/ic_launcher.png")
        big = px * 108 // 48
        fg.resize((big, big), Image.LANCZOS).save(f"{res}/mipmap-{name}/ic_launcher_foreground.png")
        mono.resize((big, big), Image.LANCZOS).save(f"{res}/mipmap-{name}/ic_launcher_monochrome.png")
    prev = Image.new("RGBA", (S, S), BG)
    prev.alpha_composite(fg)
    prev.save("/tmp/icon_adaptive_preview.png")
