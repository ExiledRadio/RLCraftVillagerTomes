"""
Project avatar for RLCraft Villager Tomes.

Third in the set, and deliberately built from the same parts as the other two:
512px squircle, near-black radial background, one centred flat-vector subject with
soft gradients and no outlines, ringed by lavender four-point sparkles. The helpers
below are carried over unchanged from the Death Overhaul script on purpose - the
family resemblance is meant to be literal rather than approximate.

Subject is an open book with an emerald rising off the pages: the book you hand over,
and the emeralds you get back. The maroon cover is the same one the Enchantment
Recipes avatar uses, which ties the two together, while the open pose and the green
keep them apart at thumbnail size - a closed book and an open book read differently
even at 64px, and neither of the others has any green in it.

Run with `python tools/make-avatar.py` (needs Pillow). Writes project-avatar.png.
"""
import math
import os
from PIL import Image, ImageDraw, ImageFilter

S = 512          # final size
SS = 4           # supersample factor
N = S * SS

LAV = (0xC2, 0xB2, 0xF2)
EMERALD_GLOW = (0x2A, 0xC8, 0x78)


# ---------------------------------------------------------------- helpers
# Unchanged from tools/make-avatar.py in RLCraftDeathOverhaul.

def radial_bg(size, c_in, c_out, cx, cy, radius, falloff=1.15):
    """Smooth radial gradient, built small and upscaled so it stays banding-free."""
    small = 96
    img = Image.new("RGB", (small, small))
    px = img.load()
    for y in range(small):
        for x in range(small):
            dx = (x + 0.5) / small - cx
            dy = (y + 0.5) / small - cy
            d = min(1.0, math.hypot(dx, dy) / radius) ** falloff
            px[x, y] = tuple(int(c_in[i] + (c_out[i] - c_in[i]) * d) for i in range(3))
    return img.resize((size, size), Image.BICUBIC)


def vertical_gradient(size, c_top, c_bot):
    w, h = size
    strip = Image.new("RGB", (1, h))
    for y in range(h):
        f = y / max(1, h - 1)
        strip.putpixel((0, y), tuple(int(c_top[i] + (c_bot[i] - c_top[i]) * f) for i in range(3)))
    return strip.resize((w, h), Image.BICUBIC)


def squircle_mask(size, radius):
    m = Image.new("L", (size, size), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill=255)
    return m


def sparkle_points(cx, cy, r, n=4.2, stretch=1.10):
    """
    Four-point sparkle: a generalised astroid (x = cos^n t, y = sin^n t), which gives
    cusped tips on the axes joined by concave sides. A plain |cos 2t|^p curve does NOT
    work here: it pinches to zero at the diagonals and renders as four detached petals
    instead of one solid star.
    """
    pts = []
    for i in range(360):
        t = i * (2 * math.pi / 360)
        ct, st = math.cos(t), math.sin(t)
        x = r * math.copysign(abs(ct) ** n, ct)
        y = r * stretch * math.copysign(abs(st) ** n, st)
        pts.append((cx + x, cy - y))
    return pts


def tinted(mask, colour, alpha=255):
    layer = Image.new("RGBA", mask.size, colour + (0,))
    a = mask if alpha == 255 else mask.point(lambda v: v * alpha // 255)
    layer.putalpha(a)
    return layer


def poly_mask(points):
    m = Image.new("L", (N, N), 0)
    ImageDraw.Draw(m).polygon(points, fill=255)
    return m


def filled(points, gradient):
    layer = gradient.copy()
    layer.putalpha(poly_mask(points))
    return layer


# ---------------------------------------------------------------- canvas

bg = radial_bg(N, (0x1B, 0x19, 0x28), (0x05, 0x05, 0x0A), 0.5, 0.42, 0.80)
canvas = bg.convert("RGBA")

# ---------------------------------------------------------------- book

# Half-width and half-height of the open book, and where its spine sits.
BW, BH = N * 0.348, N * 0.163
BCX, BCY = N * 0.5, N * 0.648


def b(u, v):
    """Book-local coordinates: u across (-1 left, 1 right), v down."""
    return (BCX + u * BW, BCY + v * BH)


# The cover sits a little lower and wider than the pages, so it shows as a rim along
# the bottom and outer edges rather than as a separate shape.
COVER = [b(-1.00, -0.62), b(-0.52, -0.86), b(0.00, -0.50), b(0.52, -0.86),
         b(1.00, -0.62), b(1.06, 0.90), b(0.52, 0.52), b(0.00, 0.86),
         b(-0.52, 0.52), b(-1.06, 0.90)]

# Pages, drawn as two mirrored leaves meeting at the spine. The outer top corner sits
# higher than the spine, which is what makes it read as an open book rather than a
# folded sheet - a symmetric trapezoid looks like a tent.
PAGE_L = [b(-0.02, -0.46), b(-0.50, -0.80), b(-0.96, -0.56), b(-0.96, 0.62),
          b(-0.50, 0.40), b(-0.02, 0.74)]
PAGE_R = [(2 * BCX - x, y) for x, y in PAGE_L]

cover_grad = vertical_gradient((N, N), (0x84, 0x2B, 0x38), (0x46, 0x13, 0x1E)).convert("RGBA")
page_grad = vertical_gradient((N, N), (0xF4, 0xE7, 0xC9), (0xC9, 0xB0, 0x84)).convert("RGBA")

# Warm glow behind the whole book, matching how the other two seat their subject.
book_glow = poly_mask(COVER).filter(ImageFilter.GaussianBlur(N * 0.040))
canvas.alpha_composite(tinted(book_glow.point(lambda v: int(v * 0.42)), (0x9A, 0x2E, 0x40)))

canvas.alpha_composite(filled(COVER, cover_grad))
canvas.alpha_composite(filled(PAGE_L, page_grad))
canvas.alpha_composite(filled(PAGE_R, page_grad))

# Spine shadow: a soft dark wedge down the centre gutter, so the two leaves read as
# one book rather than two loose sheets.
gutter = poly_mask([b(-0.10, -0.48), b(0.10, -0.48), b(0.10, 0.76), b(-0.10, 0.76)])
gutter = gutter.filter(ImageFilter.GaussianBlur(N * 0.016))
canvas.alpha_composite(tinted(gutter, (0x3A, 0x0E, 0x18), alpha=150))

# Two short ruled lines per leaf. Enough to say "pages"; any more turns to mush at
# thumbnail size, which is the size that matters most here.
for side in (-1, 1):
    for vy, half in ((-0.16, 0.30), (0.10, 0.26)):
        x0 = BCX + side * (0.20 * BW)
        x1 = BCX + side * ((0.20 + half * 2) * BW)
        y = BCY + vy * BH + side * 0  # leaves are mirrored, lines stay level
        line = poly_mask([(min(x0, x1), y - N * 0.007), (max(x0, x1), y - N * 0.007),
                          (max(x0, x1), y + N * 0.007), (min(x0, x1), y + N * 0.007)])
        canvas.alpha_composite(tinted(line, (0x8A, 0x74, 0x4E), alpha=95))

# ---------------------------------------------------------------- emerald

EW, EH = N * 0.107, N * 0.126
ECX, ECY = N * 0.5, N * 0.330


def e(u, v):
    return (ECX + u * EW, ECY + v * EH)


# Five-point cut: flat table, angled shoulders, pointed culet. Reads as a gem at any
# size, unlike a faceted octagon which just becomes a blob.
GEM = [e(-0.58, -0.66), e(0.58, -0.66), e(1.00, -0.04), e(0.00, 0.98), e(-1.00, -0.04)]
TABLE = [e(-0.58, -0.66), e(0.58, -0.66), e(0.32, -0.18), e(-0.32, -0.18)]
FACET_R = [e(0.32, -0.18), e(1.00, -0.04), e(0.00, 0.98)]

gem_grad = vertical_gradient((N, N), (0x5D, 0xE8, 0xA4), (0x0F, 0x8B, 0x4E)).convert("RGBA")

gem_glow = poly_mask(GEM).filter(ImageFilter.GaussianBlur(N * 0.045))
canvas.alpha_composite(tinted(gem_glow.point(lambda v: int(v * 0.62)), EMERALD_GLOW))

canvas.alpha_composite(filled(GEM, gem_grad))
canvas.alpha_composite(tinted(poly_mask(TABLE), (0xAF, 0xF8, 0xD0), alpha=120))
canvas.alpha_composite(tinted(poly_mask(FACET_R), (0x04, 0x5E, 0x33), alpha=95))

# ---------------------------------------------------------------- sparkles

SPARKS = [  # (u, v, radius as fraction of N) - u,v measured from canvas centre
    (-0.360, -0.290, 0.060), (0.345, -0.300, 0.052), (0.400, -0.070, 0.034),
    (-0.395, -0.055, 0.036), (0.395, 0.180, 0.032), (-0.390, 0.185, 0.030),
    (-0.165, -0.395, 0.030), (0.180, -0.400, 0.026), (0.000, 0.415, 0.028),
]
for u, v, r in SPARKS:
    cx, cy = N * 0.5 + u * N, N * 0.5 + v * N
    rad = r * N
    m = poly_mask(sparkle_points(cx, cy, rad))
    canvas.alpha_composite(tinted(m.filter(ImageFilter.GaussianBlur(rad * 0.28)), LAV, alpha=135))
    canvas.alpha_composite(tinted(m, (0xCE, 0xBE, 0xF6)))

# Two small green motes between the pages and the gem, so the emerald reads as having
# come off the book rather than floating unrelated above it.
for u, v, r in [(-0.088, -0.048, 0.019), (0.092, -0.086, 0.015), (0.020, -0.010, 0.011)]:
    cx, cy = N * 0.5 + u * N, N * 0.5 + v * N
    rad = r * N
    m = poly_mask(sparkle_points(cx, cy, rad))
    canvas.alpha_composite(tinted(m.filter(ImageFilter.GaussianBlur(rad * 0.45)), EMERALD_GLOW, alpha=185))
    canvas.alpha_composite(tinted(m, (0xD6, 0xFB, 0xE6)))

# ---------------------------------------------------------------- finish

canvas.putalpha(squircle_mask(N, int(N * 0.22)))
out = canvas.resize((S, S), Image.LANCZOS)

dest = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "project-avatar.png")
out.save(dest)
print("wrote", dest)
