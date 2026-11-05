"""Generate the ledger screen's book texture, in the same hand as the bounty scroll.

    python tools/gen-ledger-gui.py

textures/gui/ledger_book.png, 512x512:
    (0, 0)    344 x 264   cover: dark spruce-brown leather, stitched border, brass corners
    (0, 264)  330 x 222   page: parchment, matching the bounty scroll's paper

Sized for the largest ledger screen (340 x 260 panel); smaller screens draw it scaled.
Fixed seed so reruns give identical files. Needs Pillow.
"""
import math
import os
import random

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "rlcraftvillagertomes",
                   "textures", "gui", "ledger_book.png")

COVER_W, COVER_H = 344, 264
PAGE_W, PAGE_H = 330, 222
PAGE_Y = 264


def clamp(v):
    return max(0, min(255, int(round(v))))


def cover(px, rnd):
    w, h = COVER_W, COVER_H
    corner = 3  # rounded-off corner pixels
    for y in range(h):
        for x in range(w):
            # Rounded outer corners.
            dx = min(x, w - 1 - x)
            dy = min(y, h - 1 - y)
            if dx + dy < corner - 1:
                continue
            # Leather: base with fine grain and a few larger soft mottles.
            n = rnd.uniform(-6, 6)
            grain = 6 if rnd.random() < 0.05 else 0
            r, g, b = 78 + n - grain, 54 + n - grain, 36 + n * 0.8 - grain
            # Bevel: light top/left edge, dark bottom/right.
            if dx == 0 or dy == 0 or dx + dy == corner - 1:
                r, g, b = 30, 20, 12
            elif y == 1 or x == 1:
                r, g, b = r + 26, g + 20, b + 14
            elif y == h - 2 or x == w - 2:
                r, g, b = r - 22, g - 16, b - 10
            px[x, y] = (clamp(r), clamp(g), clamp(b), 255)

    # Stitching: dashes 4 px in from the edge.
    inset = 4
    stitch = (176, 146, 98, 255)
    shadow = (44, 30, 20, 255)
    for x in range(inset + 3, w - inset - 3):
        if (x // 3) % 2 == 0:
            for y in (inset, h - 1 - inset):
                px[x, y] = stitch
                px[x, y + 1] = shadow
    for y in range(inset + 3, h - inset - 3):
        if (y // 3) % 2 == 0:
            for x in (inset, w - 1 - inset):
                px[x, y] = stitch
                px[x + 1, y] = shadow

    # Brass corner caps: a 9 px triangle in each corner, shaded.
    size = 9
    for cx, cy, sx, sy in ((0, 0, 1, 1), (w - 1, 0, -1, 1), (0, h - 1, 1, -1),
                           (w - 1, h - 1, -1, -1)):
        for i in range(size):
            for j in range(size - i):
                x, y = cx + sx * i, cy + sy * j
                if i + j < corner - 1:
                    continue
                edge = (i + j == size - 1)
                light = 1.0 - (i + j) / (size * 2.2)
                if edge:
                    c = (92, 66, 26)
                else:
                    c = (196 * light + 30, 156 * light + 20, 70 * light + 10)
                    if i + j <= 3:
                        c = (c[0] + 30, c[1] + 26, c[2] + 14)
                px[x, y] = (clamp(c[0]), clamp(c[1]), clamp(c[2]), 255)
        # Rivet.
        rx, ry = cx + sx * 3, cy + sy * 3
        px[rx, ry] = (250, 226, 150, 255)


def page(px, rnd):
    w, h = PAGE_W, PAGE_H
    blotches = [(rnd.uniform(0, w), rnd.uniform(0, h), rnd.uniform(24, 60),
                 rnd.uniform(5, 12)) for _ in range(12)]
    for y in range(h):
        for x in range(w):
            n = rnd.uniform(-5, 5)
            shade = 0.0
            for bx, by, rad, depth in blotches:
                d = math.hypot(x - bx, (y - by) * 0.8)
                if d < rad:
                    shade += depth * (1 - d / rad) ** 2
            edge = min(x, w - 1 - x, y, h - 1 - y)
            if edge < 7:
                shade += (7 - edge) * 2.6
            if rnd.random() < 0.012:
                shade += 10
            c = (236 - shade * 0.9 + n, 222 - shade * 1.05 + n, 187 - shade * 1.35 + n)
            px[x, PAGE_Y + y] = (clamp(c[0]), clamp(c[1]), clamp(c[2]), 255)


def main():
    rnd = random.Random(2207)
    img = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    px = img.load()
    cover(px, rnd)
    page(px, rnd)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    img.save(OUT)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
