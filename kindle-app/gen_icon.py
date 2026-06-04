"""
Generate Tome app icon PNGs.
Design: open book seen face-on, glowing amber spine, deep dark background.
The spine + spread pages form a subtle T-for-Tome lettermark.
"""
import math, os
from PIL import Image, ImageDraw, ImageFilter

def make_icon(px, include_bg=True):
    """Draw at px×px. include_bg=False gives transparent bg (for adaptive foreground)."""
    img = Image.new('RGBA', (px, px), (0, 0, 0, 0))
    d   = ImageDraw.Draw(img)
    s   = px / 108          # viewport is 108 units
    cx  = cy = px / 2

    def p(x, y):   return (x * s, y * s)
    def sc(v):     return v * s

    # ── Background ────────────────────────────────────────────────────────────
    if include_bg:
        r = int(px * 0.22)
        d.rounded_rectangle([0, 0, px-1, px-1], radius=r, fill=(12, 5, 1, 255))

    # ── Ambient glow (warm amber behind spine) ────────────────────────────────
    glow = Image.new('RGBA', (px, px), (0, 0, 0, 0))
    gd   = ImageDraw.Draw(glow)
    for i in range(36, 0, -1):
        frac   = i / 36
        rad    = sc(46) * frac
        alpha  = int(88 * (1 - frac) ** 1.2)
        gd.ellipse([cx - rad, cy - rad, cx + rad, cy + rad],
                   fill=(200, 110, 8, alpha))
    img = Image.alpha_composite(img, glow)
    d   = ImageDraw.Draw(img)

    # ── Left page ─────────────────────────────────────────────────────────────
    # Trapezoid: wider at spine end, slightly narrower at outer edge
    lp = [p(19,34), p(51,29), p(51,81), p(19,76)]
    d.polygon(lp, fill=(248, 234, 196, 255))

    # Subtle gradient shading: darker strip at outer edge
    shade_l = [p(19,34), p(30,33), p(30,77), p(19,76)]
    d.polygon(shade_l, fill=(228, 210, 170, 255))

    # Thin inner shadow near spine (left page)
    d.line([p(50,29), p(50,81)], fill=(100, 80, 40, 60),
           width=max(1, int(sc(1.5))))

    # ── Right page ────────────────────────────────────────────────────────────
    rp = [p(57,29), p(89,34), p(89,76), p(57,81)]
    d.polygon(rp, fill=(242, 225, 182, 255))

    shade_r = [p(78,33), p(89,34), p(89,76), p(78,77)]
    d.polygon(shade_r, fill=(220, 202, 160, 255))

    d.line([p(58,29), p(58,81)], fill=(100, 80, 40, 60),
           width=max(1, int(sc(1.5))))

    # ── Page rules (text lines) ───────────────────────────────────────────────
    lw   = max(1, int(sc(0.9)))
    lcol = (148, 118, 68, 50)
    for y in [37, 44, 51, 58, 65, 72]:
        off = (y - 54) * 0.035          # slight perspective tilt
        # left page
        d.line([p(25, y - off), p(48, y + off)], fill=lcol, width=lw)
        # right page
        d.line([p(60, y + off), p(83, y - off)], fill=lcol, width=lw)

    # ── Bottom page edges (slight depth) ─────────────────────────────────────
    ew  = max(1, int(sc(1.2)))
    ec  = (170, 148, 100, 100)
    d.line([p(19,76), p(51,81)], fill=ec, width=ew)
    d.line([p(57,81), p(89,76)], fill=ec, width=ew)
    d.line([p(19,34), p(19,76)], fill=(160, 138, 90, 80), width=ew)
    d.line([p(89,34), p(89,76)], fill=(160, 138, 90, 80), width=ew)

    # ── Spine: glowing gold column ────────────────────────────────────────────
    sw = max(3, int(sc(5)))             # spine core width
    sy0, sy1 = sc(27), sc(83)

    # Outer halo layers
    for gw in range(sw + 18, sw - 1, -2):
        frac  = (gw - sw) / 18
        alpha = int(160 * (1 - frac) ** 1.8)
        col   = (210, 130, 8, alpha)
        d.rectangle([cx - gw // 2, sy0, cx + gw // 2, sy1], fill=col)

    # Core
    d.rectangle([cx - sw // 2, sy0, cx + sw // 2, sy1], fill=(255, 200, 40, 255))

    # Bright centre line
    hw = max(1, int(sc(1.4)))
    d.rectangle([cx - hw // 2, sy0 + sc(1), cx + hw // 2, sy1 - sc(1)],
                fill=(255, 248, 200, 230))

    # ── Spine-top arc (book curve at binding) ─────────────────────────────────
    arc_col = (255, 210, 60, 180)
    aw = max(1, int(sc(1.5)))
    d.line([p(51, 30), p(54, 27)], fill=arc_col, width=aw)
    d.line([p(54, 27), p(57, 30)], fill=arc_col, width=aw)

    # ── Soft inner vignette (polish) ──────────────────────────────────────────
    if include_bg:
        vig = Image.new('RGBA', (px, px), (0, 0, 0, 0))
        for i in range(1, 10):
            frac  = i / 10
            rad   = px * 0.5 * (0.65 + frac * 0.37)
            alpha = int(24 * frac)
            ImageDraw.Draw(vig).ellipse(
                [cx - rad, cy - rad, cx + rad, cy + rad],
                outline=(0, 0, 0, alpha), width=max(1, int(px * 0.045)))
        img = Image.alpha_composite(img, vig)

    return img


# ── Density table ─────────────────────────────────────────────────────────────
# (folder,  icon_px,  foreground_px)
#  foreground is 108dp at that density (safe zone = inner 72dp)
DENSITIES = [
    ('mipmap-mdpi',    48,   108),
    ('mipmap-hdpi',    72,   162),
    ('mipmap-xhdpi',   96,   216),
    ('mipmap-xxhdpi',  144,  324),
    ('mipmap-xxxhdpi', 192,  432),
]

BASE = 'android/app/src/main/res'

for folder, icon_px, fg_px in DENSITIES:
    icon = make_icon(icon_px, include_bg=True)
    icon.save(f'{BASE}/{folder}/ic_launcher.png')
    icon.save(f'{BASE}/{folder}/ic_launcher_round.png')

    fg = make_icon(fg_px, include_bg=False)
    fg.save(f'{BASE}/{folder}/ic_launcher_foreground.png')

    print(f'{folder}: icon {icon_px}px, fg {fg_px}px')

print('Icons generated.')
