#!/usr/bin/env python3
"""Annote une capture (PNG) de flamegraph : rectangle + texte, pour le rapport
d'audit visuel. Usage:
    python3 tools/annotate_flamegraph.py in.png out.png "x,y,w,h,texte" ...
"""
import sys
from PIL import Image, ImageDraw, ImageFont

try:
    FONT = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 16)
except OSError:
    FONT = ImageFont.load_default()


def main():
    if len(sys.argv) < 4:
        print('Usage: annotate_flamegraph.py in.png out.png "x,y,w,h,texte" ...', file=sys.stderr)
        sys.exit(1)

    in_path, out_path, annotations = sys.argv[1], sys.argv[2], sys.argv[3:]
    img = Image.open(in_path).convert("RGB")
    draw = ImageDraw.Draw(img)

    for spec in annotations:
        x, y, w, h, text = spec.split(",", 4)
        x, y, w, h = int(x), int(y), int(w), int(h)
        draw.rectangle([x, y, x + w, y + h], outline=(255, 60, 60), width=3)
        text_y = max(0, y - 22)
        bbox = draw.textbbox((x, text_y), text, font=FONT)
        draw.rectangle(bbox, fill=(255, 60, 60))
        draw.text((x, text_y), text, fill=(255, 255, 255), font=FONT)

    img.save(out_path)
    print(f"-> {out_path}")


if __name__ == "__main__":
    main()
