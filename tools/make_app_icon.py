"""Gera o ícone adaptativo do Conectoma a partir da arte original.

Uso:
    python tools/make_app_icon.py "C:/caminho/icone_app_conectoma.png"

A arte é uma imagem quadrada com cantos arredondados e fundo transparente. O
launcher recorta o ícone adaptativo com a própria máscara (círculo, squircle),
então os cantos transparentes da arte apareceriam como falhas. Para evitar isso,
o script mede o gradiente da arte na moldura e preenche só os cantos com esse
gradiente — o resultado é uma camada de fundo de 108 dp inteira, sem emenda e
sem duplicar a rede de neurônios.

A rede desenhada ocupa a região central, dentro da zona segura de 72 dp, então
nenhum launcher a corta.
"""

from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

DENSITIES = {
    "mdpi": 1.0,
    "hdpi": 1.5,
    "xhdpi": 2.0,
    "xxhdpi": 3.0,
    "xxxhdpi": 4.0,
}
ADAPTIVE_DP = 108
RES = Path(__file__).resolve().parent.parent / "app/src/main/res"


def crop_art(image: Image.Image) -> Image.Image:
    """Recorta a margem branca/transparente em volta do ícone."""
    rgba = image.convert("RGBA")
    alpha = rgba.getchannel("A")
    if alpha.getextrema()[0] < 250:
        box = alpha.point(lambda a: 255 if a > 12 else 0).getbbox()
        if not box:
            raise SystemExit("não encontrei conteúdo na imagem")
        return rgba.crop(box)

    rgb = rgba.convert("RGB")
    mask = Image.new("L", rgb.size, 0)
    pixels = rgb.load()
    out = mask.load()
    width, height = rgb.size
    for y in range(height):
        for x in range(width):
            r, g, b = pixels[x, y]
            if r < 245 or g < 245 or b < 245:
                out[x, y] = 255
    box = mask.getbbox()
    if not box:
        raise SystemExit("não encontrei conteúdo na imagem")
    return rgba.crop(box)


def solve3(matrix: list[list[float]], vector: list[float]) -> list[float]:
    """Resolve um sistema 3x3 por eliminação de Gauss."""
    a = [row[:] + [vector[i]] for i, row in enumerate(matrix)]
    for col in range(3):
        pivot = max(range(col, 3), key=lambda r: abs(a[r][col]))
        if abs(a[pivot][col]) < 1e-12:
            return [0.0, 0.0, 0.0]
        a[col], a[pivot] = a[pivot], a[col]
        div = a[col][col]
        a[col] = [value / div for value in a[col]]
        for row in range(3):
            if row == col:
                continue
            factor = a[row][col]
            a[row] = [a[row][k] - factor * a[col][k] for k in range(4)]
    return [a[0][3], a[1][3], a[2][3]]


def fit_gradient(art: Image.Image) -> list[list[float]]:
    """Ajusta c = a + b*u + c*v (por canal) usando a moldura da arte."""
    rgb = art.convert("RGB")
    alpha = art.getchannel("A")
    width, height = rgb.size
    pixels = rgb.load()
    opaque = alpha.load()
    band = max(2, int(min(width, height) * 0.05))
    rows: list[list[float]] = []
    values: list[list[float]] = [[], [], []]
    for y in range(height):
        for x in range(width):
            on_frame = (
                x < band or y < band or x >= width - band or y >= height - band
            )
            if not on_frame or opaque[x, y] < 250:
                continue
            u = (x / (width - 1)) * 2 - 1
            v = (y / (height - 1)) * 2 - 1
            rows.append([1.0, u, v])
            r, g, b = pixels[x, y]
            values[0].append(float(r))
            values[1].append(float(g))
            values[2].append(float(b))

    normal = [[0.0] * 3 for _ in range(3)]
    for row in rows:
        for i in range(3):
            for j in range(3):
                normal[i][j] += row[i] * row[j]

    coefficients = []
    for channel in range(3):
        rhs = [0.0] * 3
        for row, value in zip(rows, values[channel]):
            for i in range(3):
                rhs[i] += row[i] * value
        coefficients.append(solve3(normal, rhs))
    return coefficients


def render_gradient(coefficients: list[list[float]], size: int) -> Image.Image:
    """Desenha o gradiente medido, para preencher os cantos da arte."""
    image = Image.new("RGBA", (size, size))
    pixels = image.load()
    for y in range(size):
        v = (y / (size - 1)) * 2 - 1
        for x in range(size):
            u = (x / (size - 1)) * 2 - 1
            channels = []
            for a, b, c in coefficients:
                channels.append(max(0, min(255, round(a + b * u + c * v))))
            pixels[x, y] = (channels[0], channels[1], channels[2], 255)
    return image


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print(__doc__)
        return 2

    art = crop_art(Image.open(Path(argv[1])))
    print(f"arte recortada: {art.size[0]}x{art.size[1]}")

    coefficients = fit_gradient(art)
    print("gradiente (a, b*u, c*v):", [[round(v, 1) for v in row] for row in coefficients])

    for density, scale in DENSITIES.items():
        size = round(ADAPTIVE_DP * scale)
        folder = RES / f"mipmap-{density}"
        folder.mkdir(parents=True, exist_ok=True)

        base = render_gradient(coefficients, size)
        # RGBa = alfa pré-multiplicado, para o reescalonamento não criar halo.
        scaled = art.convert("RGBa").resize((size, size), Image.LANCZOS).convert("RGBA")
        icon = Image.alpha_composite(base, scaled)
        icon.save(folder / "ic_launcher_bg.png", optimize=True)
        print(f"mipmap-{density}: {size}px")

    stale = RES / "mipmap-xxxhdpi" / "ic_launcher_fg.png"
    if stale.exists():
        for density in DENSITIES:
            old = RES / f"mipmap-{density}" / "ic_launcher_fg.png"
            if old.exists():
                old.unlink()
                print(f"removido {old.relative_to(RES.parent.parent)}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
