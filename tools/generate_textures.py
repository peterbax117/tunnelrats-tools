"""Generates every texture of TunnelRat's Tools. All art is original and drawn here pixel by pixel.

Run from the repository root:  python tools/generate_textures.py
Needs Pillow (pip install pillow). Output is deterministic, so rerunning only changes files whose drawing changed.
"""
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "tunnelrats_tools" / "textures"

# Palette
SLATE = (64, 70, 84)
SLATE_LIGHT = (86, 94, 110)
SLATE_DARK = (42, 46, 56)
COPPER = (192, 112, 72)
COPPER_LIGHT = (232, 152, 104)
COPPER_DARK = (128, 70, 48)
GOLD = (240, 196, 60)
GOLD_DARK = (176, 128, 32)
SKY = (120, 190, 240)
SKY_LIGHT = (176, 224, 252)
NIGHT = (28, 36, 84)
STAR = (230, 236, 255)
STONE = (160, 160, 160)
STONE_LIGHT = (190, 190, 190)
STONE_DARK = (110, 110, 110)
GLASS = (44, 56, 78)
GLASS_LIGHT = (90, 110, 140)
RAIN = (96, 150, 240)
RAIN_LIGHT = (160, 200, 255)
CLOUD = (200, 204, 214)
CLOUD_DARK = (140, 146, 160)


def jitter(color, rng, amount):
    d = rng.randint(-amount, amount)
    return tuple(max(0, min(255, c + d)) for c in color)


def noisy_fill(img, color, amount, seed, box=None):
    rng = random.Random(seed)
    x0, y0, x1, y1 = box or (0, 0, img.width, img.height)
    for y in range(y0, y1):
        for x in range(x0, x1):
            img.putpixel((x, y), jitter(color, rng, amount) + (255,))


def bevel(img, light, dark, box=None):
    x0, y0, x1, y1 = box or (0, 0, img.width, img.height)
    for x in range(x0, x1):
        img.putpixel((x, y0), light + (255,))
        img.putpixel((x, y1 - 1), dark + (255,))
    for y in range(y0, y1):
        img.putpixel((x0, y), light + (255,))
        img.putpixel((x1 - 1, y), dark + (255,))


def px(img, x, y, color):
    img.putpixel((x, y), color + (255,))


def save(img, relative):
    path = ASSETS / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    print("wrote", path.relative_to(ROOT))


def new(size=16):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


# Weather Vane

def weather_vane_bottom():
    img = new()
    noisy_fill(img, SLATE, 6, 1)
    bevel(img, SLATE_LIGHT, SLATE_DARK)
    save(img, "block/weather_vane_bottom.png")


def weather_vane_side():
    img = new()
    noisy_fill(img, SLATE, 6, 2)
    # Copper frame.
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            px(img, x, y, COPPER)
    for i in range(1, 15):
        px(img, i, 1, COPPER_LIGHT)
        px(img, 1, i, COPPER_LIGHT)
        px(img, i, 14, COPPER_DARK)
        px(img, 14, i, COPPER_DARK)
    # Window: day sky on the left fading to night on the right.
    for y in range(4, 10):
        for x in range(3, 13):
            t = (x - 3) / 9
            c = tuple(int(SKY[k] * (1 - t) + NIGHT[k] * t) for k in range(3))
            px(img, x, y, c)
    bevel(img, SLATE_DARK, SLATE_LIGHT, (2, 3, 14, 11))
    px(img, 5, 6, GOLD)  # sun
    px(img, 4, 6, GOLD_DARK)
    px(img, 5, 5, GOLD_DARK)
    px(img, 6, 6, GOLD_DARK)
    px(img, 5, 7, GOLD_DARK)
    px(img, 10, 5, STAR)  # moon and a star
    px(img, 11, 5, STAR)
    px(img, 10, 6, STAR)
    px(img, 8, 8, STAR)
    # Dial ticks.
    for x in (4, 6, 8, 10, 12):
        px(img, x - 1, 12, GOLD)
    save(img, "block/weather_vane_side.png")


def weather_vane_top():
    img = new()
    noisy_fill(img, SLATE, 6, 3)
    bevel(img, SLATE_LIGHT, SLATE_DARK)
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if 6.0 <= d < 7.2:
                px(img, x, y, COPPER_LIGHT if (x + y) < 15 else COPPER_DARK)
            elif d < 6.0:
                px(img, x, y, SKY if y < 8 else NIGHT)
    # Sun in the day half, stars in the night half.
    for (x, y) in ((4, 4), (5, 4), (4, 5), (5, 5)):
        px(img, x, y, GOLD)
    for (x, y) in ((10, 10), (5, 11), (11, 12), (8, 12)):
        px(img, x, y, STAR)
    # The vane: an arrow pointing east, with a tail at the west.
    for x in range(3, 13):
        px(img, x, 7, GOLD)
        px(img, x, 8, GOLD_DARK)
    for (x, y) in ((11, 6), (12, 6), (11, 9), (12, 9), (13, 7), (13, 8), (10, 5), (10, 10)):
        px(img, x, y, GOLD)
    for (x, y) in ((2, 6), (3, 5), (2, 9), (3, 10), (2, 7), (2, 8)):
        px(img, x, y, COPPER_LIGHT)
    px(img, 7, 7, COPPER_DARK)  # pivot
    px(img, 8, 7, COPPER_DARK)
    save(img, "block/weather_vane_top.png")


def sky_shard():
    img = new()
    # A long crystal from the lower left to the upper right, dark at the base and light at the tip.
    for i in range(10):
        t = i / 9
        x, y = 3 + i, 12 - i
        core = tuple(int(NIGHT[k] * (1 - t) + SKY_LIGHT[k] * t) for k in range(3))
        edge = tuple(int(c * 0.6) for c in core)
        px(img, x, y, core)
        if i < 9:
            px(img, x + 1, y, tuple(min(255, int(c * 1.15)) for c in core))
            px(img, x, y - 1, edge)
            px(img, x + 1, y + 1, edge)
        if 2 <= i <= 7:
            px(img, x - 1, y, edge)
            px(img, x, y + 1, core)
    px(img, 13, 2, STAR)
    px(img, 12, 3, SKY_LIGHT)
    # Gold flecks.
    for (x, y) in ((6, 9), (9, 6), (11, 4)):
        px(img, x, y, GOLD)
    px(img, 2, 13, GOLD_DARK)
    px(img, 3, 13, GOLD_DARK)
    px(img, 2, 12, GOLD_DARK)
    save(img, "item/sky_shard.png")


# Rain Detector

def rain_detector_side():
    img = new()
    noisy_fill(img, STONE, 5, 10)
    # Only rows 10 to 15 show on the 6 pixel high block.
    for x in range(16):
        px(img, x, 10, STONE_LIGHT)
        px(img, x, 12, RAIN if x % 3 else RAIN_LIGHT)
        px(img, x, 15, STONE_DARK)
    px(img, 0, 11, STONE_LIGHT)
    px(img, 15, 11, STONE_DARK)
    save(img, "block/rain_detector_side.png")


def rain_detector_bottom():
    img = new()
    noisy_fill(img, STONE, 5, 11)
    bevel(img, STONE_LIGHT, STONE_DARK)
    save(img, "block/rain_detector_bottom.png")


def rain_detector_top(mode):
    img = new()
    noisy_fill(img, STONE, 5, 12)
    bevel(img, STONE_LIGHT, STONE_DARK)
    noisy_fill(img, GLASS, 3, 13, (2, 2, 14, 14))
    bevel(img, STONE_DARK, STONE_LIGHT, (2, 2, 14, 14))
    px(img, 3, 3, GLASS_LIGHT)
    px(img, 4, 3, GLASS_LIGHT)
    px(img, 3, 4, GLASS_LIGHT)
    if mode in ("rain", "thunder"):
        # Cloud.
        for (x, y) in [(x, 5) for x in range(6, 11)] + [(x, 6) for x in range(4, 13)] + [(x, 4) for x in range(7, 10)]:
            px(img, x, y, CLOUD if mode == "rain" else CLOUD_DARK)
        for x in range(5, 12):
            px(img, x, 7, CLOUD_DARK)
    if mode == "rain":
        for (x, y) in ((5, 9), (5, 10), (8, 10), (8, 11), (11, 9), (11, 10), (6, 12), (10, 12)):
            px(img, x, y, RAIN_LIGHT if y % 2 else RAIN)
    elif mode == "thunder":
        for (x, y) in ((8, 8), (7, 9), (8, 9), (9, 9), (8, 10), (7, 11), (6, 12)):
            px(img, x, y, GOLD)
        px(img, 9, 8, GOLD_DARK)
    elif mode == "clear":
        for (x, y) in [(x, y) for x in range(7, 10) for y in range(7, 10)]:
            px(img, x, y, GOLD)
        for (x, y) in ((8, 4), (8, 5), (8, 11), (8, 12), (4, 8), (5, 8), (11, 8), (12, 8), (5, 5), (11, 5), (5, 11), (11, 11)):
            px(img, x, y, GOLD_DARK if (x + y) % 2 else GOLD)
    save(img, f"block/rain_detector_top_{mode}.png")


# GUI, in the vanilla container style (light gray panel, inset slots)

GUI_BG = (198, 198, 198)
GUI_WHITE = (255, 255, 255)
GUI_SHADOW = (85, 85, 85)
SLOT_BG = (139, 139, 139)
SLOT_DARK = (55, 55, 55)


def gui_panel(img, w, h):
    for y in range(h):
        for x in range(w):
            img.putpixel((x, y), GUI_BG + (255,))
    for x in range(2, w - 2):
        px(img, x, 0, (0, 0, 0))
        px(img, x, h - 1, (0, 0, 0))
    for y in range(2, h - 2):
        px(img, 0, y, (0, 0, 0))
        px(img, w - 1, y, (0, 0, 0))
    for (x, y) in ((1, 1), (w - 2, h - 2), (w - 2, 1), (1, h - 2)):
        px(img, x, y, (0, 0, 0))
    for (x, y) in ((0, 0), (1, 0), (0, 1), (w - 1, 0), (w - 2, 0), (w - 1, 1),
                   (0, h - 1), (1, h - 1), (0, h - 2), (w - 1, h - 1), (w - 2, h - 1), (w - 1, h - 2)):
        img.putpixel((x, y), (0, 0, 0, 0))
    for x in range(2, w - 3):
        px(img, x, 1, GUI_WHITE)
        px(img, x, 2, GUI_WHITE)
    for y in range(2, h - 3):
        px(img, 1, y, GUI_WHITE)
        px(img, 2, y, GUI_WHITE)
    for x in range(3, w - 2):
        px(img, x, h - 2, GUI_SHADOW)
        px(img, x, h - 3, GUI_SHADOW)
    for y in range(3, h - 2):
        px(img, w - 2, y, GUI_SHADOW)
        px(img, w - 3, y, GUI_SHADOW)
    px(img, 2, 2, GUI_WHITE)
    px(img, w - 3, h - 3, GUI_SHADOW)


def inset(img, x, y, w, h, fill=SLOT_BG):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            px(img, xx, yy, fill)
    for xx in range(x, x + w - 1):
        px(img, xx, y, SLOT_DARK)
    for yy in range(y, y + h - 1):
        px(img, x, yy, SLOT_DARK)
    for xx in range(x + 1, x + w):
        px(img, xx, y + h - 1, GUI_WHITE)
    for yy in range(y + 1, y + h):
        px(img, x + w - 1, yy, GUI_WHITE)


def weather_vane_gui():
    # Coordinates match WeatherVaneScreen and WeatherVaneMenu.
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    gui_panel(img, 176, 196)
    inset(img, 7, 58, 18, 18)  # Sky Shard slot at 8, 59
    inset(img, 29, 59, 96, 16, SLOT_DARK)  # energy bar, inner area 30, 60, 94 x 14
    # A faint shard outline in the empty slot.
    for i in range(8):
        px(img, 12 + i, 71 - i, (120, 120, 132))
    for row in range(3):
        for col in range(9):
            inset(img, 7 + col * 18, 113 + row * 18, 18, 18)
    for col in range(9):
        inset(img, 7 + col * 18, 171, 18, 18)
    path = ASSETS / "gui" / "container" / "weather_vane.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
    weather_vane_bottom()
    weather_vane_side()
    weather_vane_top()
    sky_shard()
    rain_detector_side()
    rain_detector_bottom()
    for m in ("rain", "thunder", "clear"):
        rain_detector_top(m)
    weather_vane_gui()
