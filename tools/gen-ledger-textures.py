"""Generate the ledger and Villager Ward item icons and the ledger lectern block textures.

The lectern textures are VillagerBackport's (our own mod) recolored to dark spruce: every
wood-colored pixel is mapped through a spruce gradient by brightness, so the grain and
shading survive, while the colored book spines and the red cloth keep their colors.

The ledger icon is drawn from a character grid below - no vanilla texture is used.

    python tools/gen-ledger-textures.py
"""
import colorsys
import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VB = os.path.join(os.path.dirname(ROOT), "RLCraftVillagerTrading", "src", "main", "resources",
                  "assets", "villagerbackport", "textures", "blocks")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "rlcraftvillagertomes",
                   "textures")

# Dark spruce, darkest to lightest.
SPRUCE = [(0.00, (0x24, 0x18, 0x0E)),
          (0.35, (0x3E, 0x2A, 0x19)),
          (0.70, (0x5A, 0x3F, 0x26)),
          (1.00, (0x76, 0x56, 0x35))]


def gradient(t):
    t = max(0.0, min(1.0, t))
    for (a, ca), (b, cb) in zip(SPRUCE, SPRUCE[1:]):
        if t <= b:
            f = (t - a) / (b - a)
            return tuple(int(round(ca[i] + (cb[i] - ca[i]) * f)) for i in range(3))
    return SPRUCE[-1][1]


def is_wood(r, g, b):
    h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
    return 0.04 <= h <= 0.15 and s < 0.82


# Pixels never recolored, per texture: the row of book spines on the front, whose orange
# and brown books are close enough to oak that the color test alone would take them.
PROTECTED = {"lectern_front.png": lambda x, y: 8 <= y <= 11 and 1 <= x <= 6}


def recolor(name, out_name):
    keep = PROTECTED.get(name, lambda x, y: False)
    src = Image.open(os.path.join(VB, name)).convert("RGBA")
    px = src.load()
    lums = [0.299 * r + 0.587 * g + 0.114 * b
            for y in range(src.height) for x in range(src.width)
            for r, g, b, a in [px[x, y]] if a and is_wood(r, g, b)]
    lo, hi = min(lums), max(lums)
    out = src.copy()
    opx = out.load()
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = px[x, y]
            if a and is_wood(r, g, b) and not keep(x, y):
                lum = 0.299 * r + 0.587 * g + 0.114 * b
                opx[x, y] = gradient((lum - lo) / max(1.0, hi - lo)) + (a,)
    out.save(os.path.join(OUT, "blocks", out_name))


LEDGER = [
    "................",
    "..DDDDDDDDDDD...",
    "..DSGhhhhhhGDP..",
    "..DSCCCCCCCCDP..",
    "..DSCggggggCDp..",
    "..DSCgccccgCDP..",
    "..DSCgcEEcgCDp..",
    "..DSCgcEecgCDP..",
    "..DSCgceecgCDp..",
    "..DSCgccccgCDP..",
    "..DSCggggggCDp..",
    "..DSCCCCCCCCDP..",
    "..DSGCCCCCCGDP..",
    "..DDDDDDDDDDDP..",
    "...PpPRPpPpPPP..",
    "......R.........",
]
COLORS = {
    "D": (0x1E, 0x13, 0x0B), "S": (0x33, 0x21, 0x13), "C": (0x5B, 0x3A, 0x22),
    "c": (0x4A, 0x2F, 0x1C), "h": (0x74, 0x4E, 0x2E), "G": (0xE0, 0xB8, 0x4A),
    "g": (0xB0, 0x86, 0x30), "E": (0x4F, 0xD0, 0x6E), "e": (0x23, 0x86, 0x4A),
    "P": (0xEE, 0xE4, 0xC9), "p": (0xC9, 0xBA, 0x96), "R": (0xB3, 0x26, 0x2E),
}


WARD = [
    "................",
    "..OOOOOOOOOOOO..",
    "..OGGGGGGGGGGO..",
    "..OGhhEEEEEEGO..",
    "..OGhEEEyEEeGO..",
    "..OGEEEyyyEeGO..",
    "..OGEEEEyEEeGO..",
    "..OGEEEEyEEeGO..",
    "..OGEEEEEEeeGO..",
    "...OGEEEEEeGO...",
    "...OGeEEEeeGO...",
    "....OGeEEeGO....",
    ".....OGeeGO.....",
    "......OGGO......",
    ".......OO.......",
    "................",
]
WARD_COLORS = {
    "O": (0x3A, 0x26, 0x0E), "G": (0xE0, 0xB8, 0x4A), "y": (0xF7, 0xDC, 0x7A),
    "E": (0x3D, 0xC9, 0x66), "e": (0x1E, 0x86, 0x45), "h": (0xB4, 0xF5, 0xC8),
}


def icon(grid, colors, name):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(grid):
        assert len(row) == 16, (name, y, len(row))
        for x, ch in enumerate(row):
            if ch != ".":
                px[x, y] = colors[ch] + (255,)
    img.save(os.path.join(OUT, "items", name))


def main():
    os.makedirs(os.path.join(OUT, "blocks"), exist_ok=True)
    os.makedirs(os.path.join(OUT, "items"), exist_ok=True)
    for part in ("front", "sides", "top", "base"):
        recolor("lectern_%s.png" % part, "ledger_lectern_%s.png" % part)
    icon(LEDGER, COLORS, "ledger.png")
    icon(WARD, WARD_COLORS, "villager_ward.png")
    print("textures written to", OUT)


if __name__ == "__main__":
    main()
