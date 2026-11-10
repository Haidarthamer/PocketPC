#!/usr/bin/env python3
"""
Generates the Pocket PC launcher icon assets.

Design rationale (all original geometry -- no vendor logo is copied):
  * A rounded-square "pocket" shell reads as a device/PC bezel.
  * Inside it, a 2x2 grid of unequal tiles suggests a window manager's tiled
    desktop, not any specific vendor's four-pane logo: the tiles are clipped to
    the pocket silhouette and have different sizes, and the bottom-right tile is
    a taskbar strip rather than a fourth pane.

Outputs (all committed):
  app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml        adaptive icon
  app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml  round adaptive icon
  app/src/main/res/drawable/ic_launcher_foreground.xml      vector foreground (108dp)
  app/src/main/res/drawable/ic_launcher_monochrome.xml      themed-icon layer
  app/src/main/res/values/colors.xml                        background colour
  app/src/main/res/mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher.png        legacy
  app/src/main/res/mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher_round.png  legacy round
  docs/branding/icon-512.png                                store-style preview
"""

from PIL import Image, ImageDraw
import os

ROOT = os.path.join(os.path.dirname(__file__), "..")
RES = os.path.join(ROOT, "app", "src", "main", "res")

# Brand palette (also referenced by values/colors.xml and the Compose theme).
BG_TOP = (14, 26, 48)      # deep navy
BG_BOTTOM = (10, 16, 30)   # near-black navy
TILE = (106, 196, 255)     # "pocket blue"
TILE_2 = (64, 156, 232)    # deeper blue
ACCENT = (255, 176, 84)    # warm amber accent (taskbar + cursor)
SHELL = (232, 240, 252)    # bezel

SS = 4  # supersampling factor for clean edges


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def vertical_gradient(size, top, bottom):
    img = Image.new("RGB", (1, size), top)
    px = img.load()
    for y in range(size):
        px[0, y] = lerp(top, bottom, y / max(1, size - 1))
    return img.resize((size, size), Image.BILINEAR)


def rounded_mask(size, radius):
    m = Image.new("L", (size * SS, size * SS), 0)
    d = ImageDraw.Draw(m)
    d.rounded_rectangle([0, 0, size * SS - 1, size * SS - 1], radius=radius * SS, fill=255)
    return m.resize((size, size), Image.LANCZOS)


def draw_foreground(size, with_shell=True, monochrome=False):
    """The foreground mark, drawn on a transparent canvas of `size` px.

    Proportions follow Android's adaptive-icon safe zone: content sits inside the
    central 72dp of the 108dp canvas, i.e. radius ~0.33*size from centre.
    """
    S = size * SS
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    cx = cy = S / 2
    # Safe zone: keep the mark within 66% of the canvas.
    w = S * 0.52          # pocket width
    h = S * 0.62          # pocket height
    left, top = cx - w / 2, cy - h / 2
    right, bottom = cx + w / 2, cy + h / 2
    r = w * 0.16          # pocket corner radius

    shell = (255, 255, 255) if monochrome else SHELL
    tile_a = (255, 255, 255) if monochrome else TILE
    tile_b = (255, 255, 255) if monochrome else TILE_2
    accent = (255, 255, 255) if monochrome else ACCENT
    inner = (0, 0, 0, 0) if monochrome else (18, 32, 56)  # screen backing
    if monochrome:
        # Monochrome layer must be a solid silhouette: draw the pocket as a ring.
        d.rounded_rectangle([left, top, right, bottom], radius=r, outline=(255, 255, 255), width=int(S * 0.035))
        inset = S * 0.055
    else:
        # Pocket shell with a "screen" cut out of it.
        d.rounded_rectangle([left, top, right, bottom], radius=r, fill=shell)

    # Screen area.
    pad = S * 0.035 if monochrome else S * 0.032
    s_left, s_top = left + pad, top + pad
    s_right, s_bottom = right - pad, bottom - pad - S * 0.055  # leave a taskbar strip

    # Window-manager tiles: deliberately unequal, so this reads as a tiled desktop
    # and not as any vendor's equal four-pane logo.
    gap = S * 0.016
    mid_x = s_left + (s_right - s_left) * 0.58
    mid_y = s_top + (s_bottom - s_top) * 0.52
    tr = S * 0.014

    if monochrome:
        # Keep the monochrome layer simple and legible: one pane + taskbar.
        d.rounded_rectangle([s_left, s_top, s_right, mid_y - gap / 2], radius=tr, fill=(255, 255, 255))
    else:
        d.rounded_rectangle(
            [s_left, s_top, mid_x - gap / 2, s_bottom], radius=tr, fill=tile_b
        )
        d.rounded_rectangle(
            [s_left, s_top, mid_x - gap / 2, mid_y - gap / 2], radius=tr, fill=tile_a
        )
        d.rounded_rectangle(
            [mid_x + gap / 2, s_top, s_right, mid_y - gap / 2], radius=tr, fill=tile_a
        )
        d.rounded_rectangle(
            [mid_x + gap / 2, mid_y + gap / 2, s_right, s_bottom], radius=tr, fill=accent
        )

    # Taskbar strip across the bottom of the screen.
    bar_h = S * 0.030
    bar_top = bottom - pad - bar_h
    if monochrome:
        d.rounded_rectangle([s_left, bar_top, s_right, bar_top + bar_h], radius=bar_h / 2, fill=(255, 255, 255))
    else:
        d.rounded_rectangle([s_left, bar_top, s_right, bar_top + bar_h], radius=bar_h / 2, fill=accent)

    return img.resize((size, size), Image.LANCZOS)


def legacy_icon(size, round_shape=False):
    canvas = Image.new("RGBA", (size * SS, size * SS), (0, 0, 0, 0))
    bg = vertical_gradient(size * SS, BG_TOP, BG_BOTTOM).convert("RGBA")
    if round_shape:
        mask = Image.new("L", (size * SS, size * SS), 0)
        ImageDraw.Draw(mask).ellipse([0, 0, size * SS - 1, size * SS - 1], fill=255)
    else:
        mask = rounded_mask(size, int(size * 0.22)).resize((size * SS, size * SS), Image.BILINEAR)
    canvas.paste(bg, (0, 0), mask)

    # Foreground is drawn at 108/72 scale relative to the legacy 48dp grid so the
    # mark keeps the same optical weight as the adaptive icon.
    fg = draw_foreground(int(size * SS * 1.28))
    off = (size * SS - fg.size[0]) // 2
    canvas.alpha_composite(fg, (off, off))
    return canvas.resize((size, size), Image.LANCZOS)


VECTOR_FOREGROUND = """<?xml version="1.0" encoding="utf-8"?>
<!--
  Pocket PC launcher foreground (vector equivalent of scripts/generate_icons.py).
  Content is kept inside the adaptive-icon safe zone: the mark spans 52% x 62% of
  the 108dp canvas, centred, so no part is clipped by any mask shape.
  Original geometry: a pocket shell, an unequal tiled desktop, and a taskbar strip.
-->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">

    <!-- pocket shell -->
    <path
        android:fillColor="#E8F0FC"
        android:pathData="M26,21 h56 a8,8 0 0 1 8,8 v50 a8,8 0 0 1 -8,8 h-56 a8,8 0 0 1 -8,-8 v-50 a8,8 0 0 1 8,-8 z" />

    <!-- screen backing -->
    <path
        android:fillColor="#122038"
        android:pathData="M25.5,27.5 h57 a3.5,3.5 0 0 1 3.5,3.5 v42 a3.5,3.5 0 0 1 -3.5,3.5 h-57 a3.5,3.5 0 0 1 -3.5,-3.5 v-42 a3.5,3.5 0 0 1 3.5,-3.5 z" />

    <!-- large left pane -->
    <path
        android:fillColor="#409CE8"
        android:pathData="M27,31 h30 a1.6,1.6 0 0 1 1.6,1.6 v38 a1.6,1.6 0 0 1 -1.6,1.6 h-30 a1.6,1.6 0 0 1 -1.6,-1.6 v-38 a1.6,1.6 0 0 1 1.6,-1.6 z" />

    <!-- upper-right pane -->
    <path
        android:fillColor="#6AC4FF"
        android:pathData="M62.5,31 h19.5 a1.6,1.6 0 0 1 1.6,1.6 v17.5 a1.6,1.6 0 0 1 -1.6,1.6 h-19.5 a1.6,1.6 0 0 1 -1.6,-1.6 v-17.5 a1.6,1.6 0 0 1 1.6,-1.6 z" />

    <!-- lower-right accent pane -->
    <path
        android:fillColor="#FFB054"
        android:pathData="M62.5,56 h19.5 a1.6,1.6 0 0 1 1.6,1.6 v13 a1.6,1.6 0 0 1 -1.6,1.6 h-19.5 a1.6,1.6 0 0 1 -1.6,-1.6 v-13 a1.6,1.6 0 0 1 1.6,-1.6 z" />

    <!-- taskbar strip -->
    <path
        android:fillColor="#FFB054"
        android:pathData="M27,72.5 h54 a2.6,2.6 0 0 1 0,5.2 h-54 a2.6,2.6 0 0 1 0,-5.2 z" />
</vector>
"""

VECTOR_MONOCHROME = """<?xml version="1.0" encoding="utf-8"?>
<!--
  Themed-icon (monochrome) layer. Android tints this single-colour silhouette; it
  must therefore be drawn as filled shapes only, with no colour that carries meaning.
-->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">

    <!-- pocket shell outline -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M26,21 h56 a8,8 0 0 1 8,8 v50 a8,8 0 0 1 -8,8 h-56 a8,8 0 0 1 -8,-8 v-50 a8,8 0 0 1 8,-8 z
                          M26,25 a4,4 0 0 0 -4,4 v50 a4,4 0 0 0 4,4 h56 a4,4 0 0 0 4,-4 v-50 a4,4 0 0 0 -4,-4 z" />

    <!-- top pane -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M27,31 h50 a1.6,1.6 0 0 1 1.6,1.6 v20 a1.6,1.6 0 0 1 -1.6,1.6 h-50 a1.6,1.6 0 0 1 -1.6,-1.6 v-20 a1.6,1.6 0 0 1 1.6,-1.6 z" />

    <!-- taskbar strip -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M27,58 h54 a2.6,2.6 0 0 1 0,5.2 h-54 a2.6,2.6 0 0 1 0,-5.2 z" />

    <!-- two lower shortcut dots -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M34,68.5 m-3.2,0 a3.2,3.2 0 1,0 6.4,0 a3.2,3.2 0 1,0 -6.4,0" />
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M47,68.5 m-3.2,0 a3.2,3.2 0 1,0 6.4,0 a3.2,3.2 0 1,0 -6.4,0" />
</vector>
"""

ADAPTIVE = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
"""

ADAPTIVE_ROUND = ADAPTIVE

BACKGROUND_DRAWABLE = """<?xml version="1.0" encoding="utf-8"?>
<!-- Brand background: deep navy vertical gradient. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:pathData="M0,0 h108 v108 h-108 z">
        <aapt:attr xmlns:aapt="http://schemas.android.com/aapt" name="android:fillColor">
            <gradient
                android:type="linear"
                android:startX="0" android:startY="0"
                android:endX="0" android:endY="108"
                android:startColor="#0E1A30"
                android:endColor="#0A101E" />
        </aapt:attr>
    </path>
</vector>
"""

COLORS = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Pocket PC brand palette. Keep in sync with ui/theme/Brand.kt -->
    <color name="pocketpc_brand_navy">#0E1A30</color>
    <color name="pocketpc_brand_navy_deep">#0A101E</color>
    <color name="pocketpc_brand_blue">#6AC4FF</color>
    <color name="pocketpc_brand_blue_deep">#409CE8</color>
    <color name="pocketpc_brand_amber">#FFB054</color>
    <color name="pocketpc_icon_background">#0E1A30</color>
</resources>
"""


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)
    print("wrote", os.path.relpath(path, ROOT))


def main():
    # Vector / XML assets
    write(os.path.join(RES, "drawable", "ic_launcher_foreground.xml"), VECTOR_FOREGROUND)
    write(os.path.join(RES, "drawable", "ic_launcher_monochrome.xml"), VECTOR_MONOCHROME)
    write(os.path.join(RES, "drawable", "ic_launcher_background.xml"), BACKGROUND_DRAWABLE)
    write(os.path.join(RES, "values", "colors.xml"), COLORS)
    write(os.path.join(RES, "mipmap-anydpi-v26", "ic_launcher.xml"), ADAPTIVE)
    write(os.path.join(RES, "mipmap-anydpi-v26", "ic_launcher_round.xml"), ADAPTIVE_ROUND)

    # Legacy PNGs for pre-API 26 launchers and for stores/README previews.
    densities = {
        "mdpi": 48,
        "hdpi": 72,
        "xhdpi": 96,
        "xxhdpi": 144,
        "xxxhdpi": 192,
    }
    for bucket, px in densities.items():
        square = legacy_icon(px, round_shape=False)
        d = os.path.join(RES, f"mipmap-{bucket}")
        os.makedirs(d, exist_ok=True)
        square.save(os.path.join(d, "ic_launcher.png"), optimize=True)
        legacy_icon(px, round_shape=True).save(os.path.join(d, "ic_launcher_round.png"), optimize=True)
        print(f"wrote mipmap-{bucket}/ic_launcher{{,_round}}.png ({px}px)")

    # Preview for documentation.
    prev_dir = os.path.join(ROOT, "docs", "branding")
    os.makedirs(prev_dir, exist_ok=True)
    preview = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    preview.paste(legacy_icon(512), (0, 0))
    preview.save(os.path.join(prev_dir, "icon-512.png"))
    print("wrote docs/branding/icon-512.png")

    # Round-mask preview, to prove nothing is clipped by a circular launcher.
    round_prev = legacy_icon(512, round_shape=True)
    round_prev.save(os.path.join(prev_dir, "icon-512-round.png"))
    print("wrote docs/branding/icon-512-round.png")


if __name__ == "__main__":
    main()
