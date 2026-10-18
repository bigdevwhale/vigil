"""Builds README / Stillcraft screenshots from raw phone captures.

python tools/shots.py <raw_dir> <out_dir>

<raw_dir> holds full-screen captures (1080x2400) named <name>.png and <name>.ru.png, as tools/capture.py
writes them. Each one is cropped below the status bar, scaled to 540 px wide with rounded corners, and
four of them are laid out on parchment with a candle's glow as overview.png / overview.ru.png.
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

STATUS = 110  # status bar height at 1080 px wide
OVERVIEW = ["vows", "temptation", "vow", "chronicle"]
SINGLE = ["vows", "vow", "temptation", "ladder", "outcome", "catalog", "preset", "make", "chronicle", "page"]

PARCHMENT = (239, 227, 202)
UMBER = (214, 192, 150)


def card(src: Path) -> Image.Image:
    im = Image.open(src).convert("RGB")
    w, h = im.size
    im = im.crop((0, STATUS, w, STATUS + 2290))
    im = im.resize((540, 1145), Image.LANCZOS)
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.size[0] - 1, im.size[1] - 1), 34, fill=255)
    out = Image.new("RGBA", im.size)
    out.paste(im, (0, 0), mask)
    return out


def ground(w: int, h: int) -> Image.Image:
    g = Image.new("RGB", (w, h), PARCHMENT)
    d = ImageDraw.Draw(g)
    for y in range(h):
        t = (y / h) ** 1.5
        d.line([(0, y), (w, y)], fill=tuple(int(PARCHMENT[i] * (1 - t) + UMBER[i] * t) for i in range(3)))
    # Faint ruled lines, the way a scribe rules a page.
    lines = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ld = ImageDraw.Draw(lines)
    for y in range(60, h, 46):
        ld.line([(0, y), (w, y)], fill=(150, 120, 80, 22), width=2)
    g.paste(lines.filter(ImageFilter.GaussianBlur(1)), (0, 0), lines.filter(ImageFilter.GaussianBlur(1)))
    return g


def overview(raw: Path, suffix: str, out: Path):
    cards = [card(raw / f"{n}{suffix}.png") for n in OVERVIEW]
    gap, pad = 50, 80
    w = pad * 2 + 540 * len(cards) + gap * (len(cards) - 1)
    h = 1145 + pad * 2
    g = ground(w, h).convert("RGBA")
    for i, c in enumerate(cards):
        x = pad + i * (540 + gap)
        y = pad + (0 if i % 2 == 0 else 24) - 12
        sh = Image.new("RGBA", (c.width + 80, c.height + 80), (0, 0, 0, 0))
        ImageDraw.Draw(sh).rounded_rectangle((40, 50, c.width + 40, c.height + 40), 34, fill=(60, 35, 15, 90))
        g.alpha_composite(sh.filter(ImageFilter.GaussianBlur(18)), (x - 40, y - 40))
        g.alpha_composite(c, (x, y))
    g.convert("RGB").save(out / f"overview{suffix}.png", optimize=True)


def main():
    raw, out = Path(sys.argv[1]), Path(sys.argv[2])
    out.mkdir(parents=True, exist_ok=True)
    for suffix in ("", ".ru"):
        for n in SINGLE:
            src = raw / f"{n}{suffix}.png"
            if src.exists():
                card(src).save(out / f"{n}{suffix}.png", optimize=True)
        overview(raw, suffix, out)


if __name__ == "__main__":
    main()
