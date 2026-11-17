"""Gera os arquivos do ícone "Camadas" (decisão 128) com as mesmas formas de docs/design/icone-camadas.svg.

Uso: python3 scripts/gerar_icone.py <pasta de saída>. Precisa do Pillow. Os tamanhos de 16 a 32 px levam um
contorno claro pra o quadro não sumir em fundo escuro.
"""
import sys
from PIL import Image, ImageDraw

NAVY, L1, L2, AMBER, NOZZLE, EDGE = "#0C2742", "#4C8FD0", "#7DB8F5", "#F2A541", "#EAF1F8", (125, 184, 245, 170)
SS = 8  # superamostragem

def draw(size: int, outline: bool) -> Image.Image:
    big = size * SS
    k = big / 120.0
    img = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, big - 1, big - 1], radius=27 * k, fill=NAVY)
    if outline:
        w = SS  # 1 px no tamanho final
        d.rounded_rectangle([w / 2, w / 2, big - 1 - w / 2, big - 1 - w / 2], radius=27 * k, outline=EDGE, width=w)
    for x, y, wd, color in ((22, 78, 76, L1), (22, 57, 76, L2), (22, 36, 46, AMBER)):
        d.rounded_rectangle([x * k, y * k, (x + wd) * k, (y + 16) * k], radius=8 * k, fill=color)
    d.polygon([(62 * k, 18 * k), (84 * k, 18 * k), (84 * k, 26 * k), (73 * k, 36 * k), (62 * k, 26 * k)], fill=NOZZLE)
    return img.resize((size, size), Image.LANCZOS)

def icon(size: int) -> Image.Image:
    return draw(size, outline=size <= 32)

out = sys.argv[1]
icon(512).save(f"{out}/icon.png")
ico_sizes = [16, 24, 32, 48, 64, 128, 256]
imgs = [icon(s) for s in ico_sizes]
imgs[-1].save(f"{out}/icon.ico", format="ICO", sizes=[(s, s) for s in ico_sizes], append_images=imgs[:-1])
icns_sizes = [16, 32, 64, 128, 256, 512, 1024]
big = icon(1024)
big.save(f"{out}/icon.icns", format="ICNS", append_images=[icon(s) for s in icns_sizes[:-1]])
for s in (16, 24, 32, 48, 128):
    icon(s).save(f"{out}/previa-{s}.png")
