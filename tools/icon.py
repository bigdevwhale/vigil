"""Paints Vigil's icon: a red wax seal with a candle flame pressed into it, on parchment.

Writes the adaptive-icon foreground, the legacy icons and docs/logo.png.
Run from the repo root: python tools/icon.py
"""
import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter

RES = "app/src/main/res"
DENS = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
WAX = (168, 46, 38)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def seal(size, wax=WAX, seed=7):
    """The seal alone on transparency, `size` px across (drawn at 4x and scaled down)."""
    s = size * 4
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    c = s / 2
    r = s * 0.40
    rnd = random.Random(seed)
    ph = [rnd.random() * 6.28 for _ in range(3)]
    drips = [rnd.random() * 6.28 for _ in range(3)]
    pts = []
    for i in range(240):
        a = i / 240 * 2 * math.pi
        k = 1 + 0.03 * math.sin(3 * a + ph[0]) + 0.02 * math.sin(5 * a + ph[1]) + 0.012 * math.sin(9 * a + ph[2])
        for d in drips:
            diff = abs((a - d + 3 * math.pi) % (2 * math.pi) - math.pi)
            if diff < 0.22:
                k += 0.06 * math.cos(diff / 0.22 * math.pi / 2)
        pts.append((c + math.cos(a) * r * k, c + math.sin(a) * r * k))
    # Shadow.
    sh = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    ImageDraw.Draw(sh).polygon([(x + s * 0.012, y + s * 0.02) for x, y in pts], fill=(40, 20, 10, 110))
    sh = sh.filter(ImageFilter.GaussianBlur(s * 0.015))
    img.alpha_composite(sh)
    # Wax with a radial light from the upper left.
    mask = Image.new("L", (s, s), 0)
    ImageDraw.Draw(mask).polygon(pts, fill=255)
    grad = Image.new("RGBA", (s, s))
    gp = grad.load()
    lx, ly = c - r * 0.35, c - r * 0.4
    for y in range(0, s):
        for x in range(0, s):
            d = math.hypot(x - lx, y - ly) / (r * 1.6)
            if d < 0.5:
                col = lerp(lerp(wax, (255, 255, 255), 0.22), wax, d / 0.5)
            else:
                col = lerp(wax, lerp(wax, (0, 0, 0), 0.4), min(1, (d - 0.5) / 0.6))
            gp[x, y] = col + (255,)
    img.paste(grad, (0, 0), mask)
    d = ImageDraw.Draw(img)
    dark = lerp(wax, (0, 0, 0), 0.32)
    light = lerp(wax, (255, 255, 255), 0.3)
    # The pressed ring and the pearls.
    rr = r * 0.74
    d.ellipse([c - rr, c - rr, c + rr, c + rr], outline=dark, width=int(r * 0.05))
    d.ellipse([c - rr * 0.92, c - rr * 0.92, c + rr * 0.92, c + rr * 0.92], fill=lerp(wax, (0, 0, 0), 0.16))
    for i in range(20):
        a = i / 20 * 2 * math.pi
        px, py = c + math.cos(a) * r * 0.85, c + math.sin(a) * r * 0.85
        pr = r * 0.032
        d.ellipse([px - pr, py - pr, px + pr, py + pr], fill=dark)
    # A candle flame pressed in: light edge below-right, dark face on top.
    def flame(dx, dy, col):
        top = (c + dx, c - r * 0.5 + dy)
        base = (c + dx, c + r * 0.12 + dy)
        w = r * 0.24
        poly = []
        for i in range(60):
            t = i / 59
            # Teardrop: narrow at the top, round at the base.
            y = top[1] + (base[1] - top[1]) * t
            half = w * math.sin(math.pi * min(1, t * 0.85 + 0.0)) * (t ** 0.6)
            poly.append((top[0] + half, y))
        for i in range(60):
            t = 1 - i / 59
            y = top[1] + (base[1] - top[1]) * t
            half = w * math.sin(math.pi * min(1, t * 0.85)) * (t ** 0.6)
            poly.append((top[0] - half, y))
        d.polygon(poly, fill=col)
        # The candle below the flame.
        bw = r * 0.15
        d.rectangle([c - bw + dx, c + r * 0.2 + dy, c + bw + dx, c + r * 0.52 + dy], fill=col)
        d.rectangle([c - r * 0.012 + dx, c + r * 0.1 + dy, c + r * 0.012 + dx, c + r * 0.22 + dy], fill=col)
    flame(r * 0.025, r * 0.035, light)
    flame(0, 0, lerp(wax, (0, 0, 0), 0.48))
    # Shine, blended on its own layer.
    shine = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    ImageDraw.Draw(shine).arc([c - r * 0.9, c - r * 0.9, c + r * 0.9, c + r * 0.9], 205, 250, fill=(255, 240, 220, 60), width=int(r * 0.045))
    img.alpha_composite(shine.filter(ImageFilter.GaussianBlur(s * 0.004)))
    return img.resize((size, size), Image.LANCZOS)


def parchment(size, round_=False):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    bg = Image.new("RGBA", (size, size))
    p = bg.load()
    for y in range(size):
        for x in range(size):
            dd = math.hypot(x - size * 0.5, y - size * 0.45) / (size * 0.75)
            p[x, y] = lerp((246, 236, 214), (214, 192, 150), min(1, dd)) + (255,)
    mask = Image.new("L", (size, size), 0)
    if round_:
        ImageDraw.Draw(mask).ellipse([0, 0, size - 1, size - 1], fill=255)
    else:
        ImageDraw.Draw(mask).rounded_rectangle([0, 0, size - 1, size - 1], radius=size * 0.2, fill=255)
    img.paste(bg, (0, 0), mask)
    return img


def main():
    for name, k in DENS.items():
        folder = os.path.join(RES, "mipmap-" + name)
        os.makedirs(folder, exist_ok=True)
        fg_size = int(108 * k)
        fg = Image.new("RGBA", (fg_size, fg_size), (0, 0, 0, 0))
        sz = int(fg_size * 0.72)
        fg.alpha_composite(seal(sz), ((fg_size - sz) // 2, (fg_size - sz) // 2))
        fg.save(os.path.join(folder, "ic_launcher_foreground.png"))
        legacy = int(48 * k)
        for round_, fname in ((False, "ic_launcher.png"), (True, "ic_launcher_round.png")):
            im = parchment(legacy, round_)
            s2 = int(legacy * 0.86)
            im.alpha_composite(seal(s2), ((legacy - s2) // 2, (legacy - s2) // 2))
            im.save(os.path.join(folder, fname))
    os.makedirs("docs", exist_ok=True)
    logo = parchment(512)
    logo.alpha_composite(seal(440), (36, 36))
    logo.save("docs/logo.png")


if __name__ == "__main__":
    main()
