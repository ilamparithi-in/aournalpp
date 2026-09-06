#!/usr/bin/env python3
"""
Dynamic Icon Generator for Aournal++
Dynamically generates all required Android launcher mipmaps (WebP) and store assets
from the source SVG logo (art/Aournal++_logo.svg).
"""

import os
import sys
import shutil
import tempfile
import subprocess
from pathlib import Path
from PIL import Image, ImageDraw

DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

def render_svg_to_png(svg_path: Path, output_png: Path, size: int):
    """Renders an SVG file to PNG at the target size using available system tools."""
    if shutil.which("rsvg-convert"):
        subprocess.run(
            ["rsvg-convert", "-w", str(size), "-h", str(size), str(svg_path), "-o", str(output_png)],
            check=True
        )
    elif shutil.which("inkscape"):
        subprocess.run(
            ["inkscape", str(svg_path), "-w", str(size), "-h", str(size), "-o", str(output_png)],
            check=True
        )
    else:
        try:
            import cairosvg
            cairosvg.svg2png(url=str(svg_path), write_to=str(output_png), output_width=size, output_height=size)
        except ImportError:
            raise RuntimeError("No SVG rasterizer found! Please install rsvg-convert, inkscape, or cairosvg.")

def create_adaptive_styled_icon(logo_img: Image.Image, target_size: int, is_round: bool = False) -> Image.Image:
    """Composites the logo onto a clean, styled adaptive background (round or rounded rect)."""
    canvas_size = 1024
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas)

    bg_color = (248, 249, 250, 255)  # Neutral elegant surface (#F8F9FA)

    if is_round:
        draw.ellipse([0, 0, canvas_size - 1, canvas_size - 1], fill=bg_color)
    else:
        # Standard squircle / rounded rect for legacy icon
        corner_radius = int(canvas_size * 0.22)
        draw.rounded_rectangle([0, 0, canvas_size - 1, canvas_size - 1], radius=corner_radius, fill=bg_color)

    # Scale logo to fit safely inside (~72% of canvas for circle safe zone)
    content_scale = 0.72 if is_round else 0.78
    content_size = int(canvas_size * content_scale)
    scaled_logo = logo_img.resize((content_size, content_size), Image.Resampling.LANCZOS)

    offset_x = (canvas_size - content_size) // 2
    offset_y = (canvas_size - content_size) // 2

    canvas.alpha_composite(scaled_logo, (offset_x, offset_y))

    # Downscale with high quality Lanczos to target_size
    return canvas.resize((target_size, target_size), Image.Resampling.LANCZOS)

def main():
    repo_root = Path(__file__).resolve().parent.parent
    svg_source = repo_root / "art" / "logo.svg"
    res_dir = repo_root / "app" / "src" / "main" / "res"

    if not svg_source.exists():
        print(f"Error: Source SVG not found at {svg_source}", file=sys.stderr)
        sys.exit(1)

    print(f"[*] Reading source SVG: {svg_source}")

    with tempfile.TemporaryDirectory() as tmp_dir:
        master_png = Path(tmp_dir) / "master_logo.png"
        render_svg_to_png(svg_source, master_png, 2048)
        logo_master = Image.open(master_png).convert("RGBA")

        for folder, size in DENSITIES.items():
            dest_dir = res_dir / folder
            dest_dir.mkdir(parents=True, exist_ok=True)

            # Generate standard icon
            std_icon = create_adaptive_styled_icon(logo_master, size, is_round=False)
            std_path = dest_dir / "ic_launcher.webp"
            std_icon.save(std_path, "WEBP", quality=100)

            # Generate round icon
            round_icon = create_adaptive_styled_icon(logo_master, size, is_round=True)
            round_path = dest_dir / "ic_launcher_round.webp"
            round_icon.save(round_path, "WEBP", quality=100)

            print(f"  -> Generated {folder}: ic_launcher.webp & ic_launcher_round.webp ({size}x{size})")

        # Also output 512x512 store icon in art/
        store_icon = create_adaptive_styled_icon(logo_master, 512, is_round=False)
        store_path = repo_root / "art" / "ic_launcher-playstore.png"
        store_icon.save(store_path, "PNG")
        print(f"  -> Generated store asset: {store_path} (512x512)")

    print("[✔] Successfully generated all app icons dynamically from SVG.")

if __name__ == "__main__":
    main()
