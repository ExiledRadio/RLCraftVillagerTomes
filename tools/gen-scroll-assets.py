"""Generate the bounty scroll's texture and its unroll / roll-up sounds.

    python tools/gen-scroll-assets.py

Texture (textures/gui/bounty_scroll.png, 256x256):
    (0, 0)    200 x 240   parchment body, drawn cropped to the scroll's height
    (0, 240)  224 x 16    rolled end with wooden knobs, used for the top and bottom

Sounds (sounds/scroll/*.ogg): synthesised paper. A crinkle is a dense spray of very short
broadband clicks with random loudness, band-passed to where paper lives (about 1-9 kHz);
under it a soft low "slide" of filtered noise. Unrolling swells then tails off; rolling up is
shorter and quieter. Fixed seeds so reruns give identical files.

Needs numpy, scipy, Pillow and soundfile (pip install soundfile - its wheel ships
libsndfile with Vorbis).
"""
import math
import os
import random

import numpy as np
from PIL import Image
from scipy.signal import butter, sosfilt
import soundfile as sf

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "rlcraftvillagertomes")

PAPER_W, PAPER_H = 200, 240
ROLL_W, ROLL_H = 224, 16


def clamp(v):
    return max(0, min(255, int(round(v))))


def texture():
    rnd = random.Random(1408)
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    px = img.load()

    # Low-frequency ageing: a few soft blotches.
    blotches = [(rnd.uniform(0, PAPER_W), rnd.uniform(0, PAPER_H), rnd.uniform(18, 46),
                 rnd.uniform(6, 14)) for _ in range(9)]
    # Deckled side edges: each row starts/ends a pixel or two in, in short runs.
    left, right = [], []
    l = r = 0
    for y in range(PAPER_H):
        if rnd.random() < 0.25:
            l = rnd.choice([0, 0, 1, 1, 2])
        if rnd.random() < 0.25:
            r = rnd.choice([0, 0, 1, 1, 2])
        left.append(l)
        right.append(r)

    for y in range(PAPER_H):
        for x in range(PAPER_W):
            if x < left[y] or x >= PAPER_W - right[y]:
                continue
            base = [236, 222, 187]
            n = rnd.uniform(-5, 5)
            shade = 0.0
            for bx, by, rad, depth in blotches:
                d = math.hypot(x - bx, (y - by) * 0.8)
                if d < rad:
                    shade += depth * (1 - d / rad) ** 2
            # Darker toward the side edges, as old paper browns from the edge in.
            edge = min(x - left[y], PAPER_W - right[y] - 1 - x)
            if edge < 6:
                shade += (6 - edge) * 3.2
            # Faint fibres.
            if rnd.random() < 0.012:
                shade += 10
            c = [base[0] - shade * 0.9 + n, base[1] - shade * 1.05 + n,
                 base[2] - shade * 1.35 + n]
            px[x, y] = (clamp(c[0]), clamp(c[1]), clamp(c[2]), 255)

    # Rolled end: a parchment cylinder with knobs either side.
    oy = 240
    knob = 12
    for y in range(ROLL_H):
        t = (y + 0.5) / ROLL_H
        light = 0.55 + 0.45 * math.sin(math.pi * t)
        # Spiral line where the outer turn of paper overlaps the next.
        if y in (4, 11):
            light *= 0.82
        for x in range(knob, ROLL_W - knob):
            n = rnd.uniform(-4, 4)
            c = (226 * light + n, 208 * light + n, 168 * light + n)
            px[x, oy + y] = (clamp(c[0]), clamp(c[1]), clamp(c[2]), 255)
        # Dark rim where the roll meets the knob.
        for x in (knob, ROLL_W - knob - 1):
            px[x, oy + y] = (clamp(120 * light), clamp(96 * light), clamp(64 * light), 255)
    # Knobs: rounded dark wood, a little taller than the roll's middle.
    for side in (0, ROLL_W - knob):
        for y in range(ROLL_H):
            for x in range(knob):
                cx = side + knob / 2 - 0.5
                dx = (side + x - cx) / (knob / 2)
                dy = (y - (ROLL_H / 2 - 0.5)) / (ROLL_H / 2)
                if dx * dx * 0.7 + dy * dy > 1.0:
                    continue
                light = 0.6 + 0.4 * math.cos(dy * math.pi / 2) - 0.15 * dx
                if dy < -0.45 and abs(dx) < 0.5:
                    light += 0.25  # highlight
                n = rnd.uniform(-5, 5)
                px[side + x, oy + y] = (clamp(104 * light + n), clamp(66 * light + n),
                                        clamp(38 * light + n), 255)

    out = os.path.join(ASSETS, "textures", "gui", "bounty_scroll.png")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    img.save(out)
    print("wrote", out)


SR = 44100


def crinkle(seed, length, density, peak_at, loudness):
    """Paper: bursts of tiny clicks under a soft slide, swelling then tailing off."""
    rng = np.random.default_rng(seed)
    n = int(SR * length)
    t = np.arange(n) / SR
    # Density envelope: fast rise to peak_at, then exponential tail.
    env = np.where(t < peak_at, (t / peak_at) ** 1.5,
                   np.exp(-(t - peak_at) / (length * 0.32)))

    out = np.zeros(n)
    # Clicks: each a few-hundred-microsecond decaying noise grain.
    count = int(density * length)
    for _ in range(count):
        at = rng.uniform(0, length)
        if rng.random() > env[min(n - 1, int(at * SR))]:
            continue
        g = int(SR * rng.uniform(0.0004, 0.0035))
        start = int(at * SR)
        if start + g >= n:
            continue
        grain = rng.normal(0, 1, g) * np.exp(-np.linspace(0, rng.uniform(3, 7), g))
        out[start:start + g] += grain * rng.uniform(0.15, 1.0) ** 2
    out = sosfilt(butter(4, [1100, 9000], btype="band", fs=SR, output="sos"), out)

    # The slide: the sheet dragging against itself.
    slide = rng.normal(0, 1, n)
    slide = sosfilt(butter(2, [250, 2200], btype="band", fs=SR, output="sos"), slide)
    slide *= env * 0.35

    mix = out + slide * (np.max(np.abs(out)) or 1.0)
    # Short fades so nothing clicks at the edges.
    fade = int(SR * 0.006)
    mix[:fade] *= np.linspace(0, 1, fade)
    mix[-fade:] *= np.linspace(1, 0, fade)
    mix /= np.max(np.abs(mix))
    return (mix * loudness).astype(np.float32)


def sounds():
    folder = os.path.join(ASSETS, "sounds", "scroll")
    os.makedirs(folder, exist_ok=True)
    for i in range(3):
        data = crinkle(700 + i, 0.55, 2600, 0.16, 0.7)
        path = os.path.join(folder, "unroll%d.ogg" % (i + 1))
        sf.write(path, data, SR, format="OGG", subtype="VORBIS")
        print("wrote", path)
    for i in range(2):
        data = crinkle(900 + i, 0.3, 2200, 0.06, 0.5)
        path = os.path.join(folder, "roll%d.ogg" % (i + 1))
        sf.write(path, data, SR, format="OGG", subtype="VORBIS")
        print("wrote", path)


if __name__ == "__main__":
    texture()
    sounds()
