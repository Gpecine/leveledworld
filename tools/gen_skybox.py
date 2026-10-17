"""Gera os fundos de espaço da tela de constelações (um por skill), de forma procedural e original.

Uso: python3 tools/gen_skybox.py <pasta-saida>
Cada imagem é uma cena: pilares de poeira, galáxia espiral, nebulosa azul, nebulosa de emissão,
nuvem dourada, nebulosa planetária e remanescente de supernova.
"""
import sys
import zlib
import struct
import numpy as np

W, H = 960, 540


# ---------- ruído ----------

def value_noise(h, w, cell, rng):
    gh, gw = h // cell + 3, w // cell + 3
    grid = rng.random((gh, gw)).astype(np.float32)
    ys = np.arange(h, dtype=np.float32) / cell
    xs = np.arange(w, dtype=np.float32) / cell
    y0 = ys.astype(int)
    x0 = xs.astype(int)
    fy = ys - y0
    fx = xs - x0
    fy = fy * fy * (3 - 2 * fy)
    fx = fx * fx * (3 - 2 * fx)
    a = grid[y0][:, x0]
    b = grid[y0][:, x0 + 1]
    c = grid[y0 + 1][:, x0]
    d = grid[y0 + 1][:, x0 + 1]
    top = a + (b - a) * fx[None, :]
    bot = c + (d - c) * fx[None, :]
    return top + (bot - top) * fy[:, None]


def fbm(h, w, base, octaves, rng, gain=0.5):
    out = np.zeros((h, w), np.float32)
    amp, total, cell = 1.0, 0.0, base
    for _ in range(octaves):
        out += amp * value_noise(h, w, max(1, int(cell)), rng)
        total += amp
        amp *= gain
        cell /= 2
    return out / total


def warp_sample(field, dx, dy):
    h, w = field.shape
    yy, xx = np.mgrid[0:h, 0:w]
    sx = np.clip((xx + dx).astype(int), 0, w - 1)
    sy = np.clip((yy + dy).astype(int), 0, h - 1)
    return field[sy, sx]


def coords():
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    return xx / W, yy / H


def colorize(t, stops):
    """Mapeia t (0..1) num gradiente de cores [(pos, (r,g,b)), ...]."""
    t = np.clip(t, 0, 1)
    out = np.zeros(t.shape + (3,), np.float32)
    for (p0, c0), (p1, c1) in zip(stops, stops[1:]):
        m = (t >= p0) & (t <= p1)
        k = ((t - p0) / max(p1 - p0, 1e-6))[m][:, None]
        out[m] = np.array(c0) * (1 - k) + np.array(c1) * k
    return out


def glow(cx, cy, radius, power=2.0):
    x, y = coords()
    d = np.sqrt(((x - cx) * W / H) ** 2 + (y - cy) ** 2) / radius
    return np.exp(-d ** power)


# ---------- estrelas ----------

def stars(img, rng, count=900, bright=10, tint=(1, 1, 1)):
    for _ in range(count):
        x, y = rng.integers(0, W), rng.integers(0, H)
        v = rng.random() ** 3 * 0.9 + 0.1
        c = np.array(tint) * v * rng.uniform(0.8, 1.0, 3)
        img[y, x] = np.maximum(img[y, x], c)
    # estrelas brilhantes com raios de difração (6 pontas)
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    for _ in range(bright):
        x, y = rng.integers(40, W - 40), rng.integers(30, H - 60)
        size = rng.uniform(0.6, 1.4)
        col = np.array(rng.choice([(1.0, 0.95, 0.85), (0.8, 0.9, 1.0), (1.0, 0.85, 0.7)]))
        dx, dy = xx - x, yy - y
        r = np.sqrt(dx * dx + dy * dy) + 1e-3
        core = np.exp(-(r / (1.8 * size)) ** 2)
        halo = np.exp(-r / (9 * size)) * 0.25
        spikes = np.zeros_like(r)
        for ang in (np.pi / 2, np.pi / 6, -np.pi / 6):
            along = np.abs(dx * np.cos(ang) + dy * np.sin(ang))
            across = np.abs(-dx * np.sin(ang) + dy * np.cos(ang))
            spikes += np.exp(-across / 0.6) * np.exp(-along / (18 * size)) * 0.45
        img += (core + halo + spikes)[..., None] * col


def vignette(img, strength=0.55, bottom=0.35):
    x, y = coords()
    v = 1 - strength * (((x - 0.5) * 1.6) ** 2 + ((y - 0.45) * 1.3) ** 2)
    v -= bottom * np.clip((y - 0.72) / 0.28, 0, 1)  # escurece embaixo (painel)
    return img * np.clip(v, 0.15, 1)[..., None]


# ---------- cenas ----------

def blur(a, r):
    """Desfoque de caixa separável (3 passadas ~ gaussiano)."""
    for _ in range(3):
        for axis in (0, 1):
            c = np.cumsum(np.pad(a, [(r + 1, r) if i == axis else (0, 0) for i in range(a.ndim)], mode="edge"), axis=axis)
            a = (np.take(c, range(2 * r + 1, c.shape[axis]), axis=axis) - np.take(c, range(0, c.shape[axis] - 2 * r - 1), axis=axis)) / (2 * r + 1)
    return a


def scene_pillars(rng):
    """Pilares de poeira: densidade suave, afinando para cima, bordas erodidas, iluminados pela esquerda-topo."""
    x, y = coords()
    gas = fbm(H, W, 170, 6, rng)
    warp = fbm(H, W, 90, 4, rng)
    gas = warp_sample(gas, (warp - 0.5) * 140, (warp - 0.5) * 90)
    light = glow(0.35, 0.05, 0.9, 1.2)
    img = colorize(gas * (0.45 + 0.7 * light), [(0, (0.01, 0.02, 0.04)), (0.35, (0.04, 0.16, 0.2)),
                                                (0.6, (0.22, 0.45, 0.42)), (0.8, (0.7, 0.62, 0.4)), (1, (1, 0.85, 0.6))])
    erosion = fbm(H, W, 22, 5, rng)
    coarse = fbm(H, W, 70, 3, rng)
    density = np.zeros((H, W), np.float32)
    tips = []
    # (x base, x topo, y topo, largura base, largura topo, curvatura)
    for bx, tx, top, wb, wt, bend in ((0.27, 0.33, 0.12, 0.13, 0.035, 0.03),
                                       (0.53, 0.49, 0.36, 0.09, 0.03, -0.02),
                                       (0.76, 0.72, 0.50, 0.075, 0.025, 0.015)):
        k = np.clip((y - top) / (1 - top), 0, 1)
        cx = tx + (bx - tx) * k + bend * np.sin(k * np.pi)
        wdt = wt + (wb - wt) * k ** 0.8
        d = np.abs(x - cx) / wdt
        cap = np.clip((y - top) / 0.035, 0, 1)  # topo arredondado
        body = np.clip(1.15 - d + (erosion - 0.5) * 1.1 + (coarse - 0.5) * 0.6, 0, 1) * cap
        density = np.maximum(density, body)
        tips.append((tx, top))
    density = blur(np.clip(density * 1.6, 0, 1), 2)
    # iluminação: lado voltado para a estrela (esquerda-topo) mais claro
    gy, gx = np.gradient(blur(density, 5))
    lit = np.clip((gx * 0.8 + gy * 0.6) * 45, 0, 1)
    inner = fbm(H, W, 12, 4, rng)
    dust = colorize(0.25 + inner * 0.35 + lit * 0.6, [(0, (0.03, 0.015, 0.01)), (0.35, (0.16, 0.08, 0.04)),
                                                      (0.65, (0.45, 0.24, 0.1)), (1, (0.92, 0.55, 0.26))])
    a = (density ** 0.8)[..., None]
    img = img * (1 - a) + dust * a
    rim = np.clip(np.sqrt(gx ** 2 + gy ** 2) * 25, 0, 1) * (0.3 + lit)
    img += blur(rim, 2)[..., None] * np.array([1.0, 0.65, 0.35]) * 0.35
    for tx, top in tips:  # gás evaporando nas pontas
        img += (glow(tx, top - 0.01, 0.07, 1.6) * 0.5)[..., None] * np.array([1.0, 0.8, 0.55])
    stars(img, rng, 650, 5)
    img *= (1 - 0.6 * density[..., None] * (1 - lit[..., None]))  # estrelas atrás da poeira somem
    return img


def scene_spiral(rng):
    x, y = coords()
    cx, cy = 0.5, 0.42
    dx = (x - cx) * W / H
    dy = (y - cy) / 0.55  # galáxia inclinada
    ang = 0.35
    rx = dx * np.cos(ang) - dy * np.sin(ang)
    ry = dx * np.sin(ang) + dy * np.cos(ang)
    r = np.sqrt(rx ** 2 + ry ** 2) + 1e-4
    th = np.arctan2(ry, rx)
    n = fbm(H, W, 30, 5, rng)
    arms = (1 + np.cos(2 * (th - 3.2 * np.log(r)))) / 2
    arms = arms ** 2.5 * np.exp(-r / 0.28) * (0.6 + 0.8 * n)
    core = np.exp(-(r / 0.06) ** 1.5)
    img = arms[..., None] * np.array([0.45, 0.6, 1.0]) * 1.6
    img += core[..., None] * np.array([1.0, 0.85, 0.6]) * 1.6
    knots = blur(((fbm(H, W, 6, 2, rng) > 0.8) * arms * 3).astype(np.float32), 1)
    img += knots[..., None] * np.array([1.0, 0.35, 0.6])
    dust = np.clip((1 + np.cos(2 * (th - 3.2 * np.log(r)) + 0.9)) / 2, 0, 1) ** 4 * np.exp(-r / 0.25)
    img *= (1 - 0.6 * dust)[..., None]
    bg = fbm(H, W, 200, 4, rng)
    img += colorize(bg, [(0, (0, 0, 0)), (0.6, (0.02, 0.02, 0.06)), (1, (0.08, 0.05, 0.15))])
    stars(img, rng, 1100, 9)
    return img


def scene_blue(rng):
    """Nebulosa de reflexão azul com aglomerado de estrelas brilhantes."""
    gas = fbm(H, W, 140, 6, rng)
    wisp = fbm(H, W, 60, 5, rng)
    gas = warp_sample(gas, (wisp - 0.5) * 160, (wisp - 0.5) * 60)
    light = glow(0.55, 0.38, 0.45, 1.5)
    img = colorize(gas * (0.4 + light), [(0, (0, 0.01, 0.03)), (0.4, (0.03, 0.08, 0.22)),
                                         (0.7, (0.2, 0.4, 0.85)), (1, (0.75, 0.88, 1.0))])
    stars(img, rng, 900, 4)
    sub = np.random.default_rng(rng.integers(1 << 30))
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    for _ in range(7):  # aglomerado
        sx, sy = 0.55 * W + sub.normal(0, 70), 0.38 * H + sub.normal(0, 40)
        r = np.sqrt((xx - sx) ** 2 + (yy - sy) ** 2)
        img += (np.exp(-(r / 2.2) ** 2) + np.exp(-r / 14) * 0.3)[..., None] * np.array([0.85, 0.92, 1.0])
    return img


def scene_emission(rng):
    gas = fbm(H, W, 150, 7, rng)
    warp = fbm(H, W, 70, 4, rng)
    gas = warp_sample(gas, (warp - 0.5) * 140, (warp - 0.5) * 140)
    light = glow(0.45, 0.4, 0.5, 1.3)
    img = colorize(gas * (0.35 + light * 0.9), [(0, (0.01, 0, 0.02)), (0.35, (0.2, 0.02, 0.08)),
                                                (0.6, (0.7, 0.12, 0.25)), (0.85, (1.0, 0.55, 0.55)), (1, (1, 0.9, 0.85))])
    lanes = fbm(H, W, 50, 5, rng)
    img *= (1 - 0.75 * np.clip((lanes - 0.55) * 4, 0, 1))[..., None]
    stars(img, rng, 900, 6)
    return img


def scene_gold(rng):
    """Nuvem de poeira dourada com uma crista escura em primeiro plano."""
    x, y = coords()
    gas = fbm(H, W, 130, 6, rng)
    warp = fbm(H, W, 80, 4, rng)
    gas = warp_sample(gas, (warp - 0.5) * 100, (warp - 0.5) * 50)
    img = colorize(gas * (0.5 + glow(0.6, 0.3, 0.5)), [(0, (0.02, 0.01, 0)), (0.4, (0.25, 0.12, 0.03)),
                                                      (0.7, (0.85, 0.55, 0.18)), (1, (1, 0.9, 0.6))])
    ridge_n = fbm(1, W, 60, 5, rng)[0]
    ridge = 0.62 + (ridge_n - 0.5) * 0.25 + 0.08 * np.sin(x[0] * 9)
    dark = np.clip((y - ridge[None, :]) * 30, 0, 1)
    img = img * (1 - dark[..., None] * 0.92)
    rim = np.exp(-np.abs(y - ridge[None, :]) * 120)
    img += rim[..., None] * np.array([1.0, 0.7, 0.35]) * 0.5
    stars(img, rng, 700, 5, (1, 0.95, 0.85))
    return img


def scene_ring(rng):
    """Nebulosa planetária verde-azulada em forma de anel ("olho")."""
    x, y = coords()
    dx = (x - 0.5) * W / H
    dy = (y - 0.42) / 0.8
    r = np.sqrt(dx ** 2 + dy ** 2)
    n = fbm(H, W, 25, 5, rng)
    ring = np.exp(-((r - 0.24) / (0.06 + 0.03 * n)) ** 2) * (0.6 + 0.8 * n)
    inner = np.exp(-(r / 0.2) ** 2) * 0.45
    outer = np.exp(-((r - 0.33) / 0.05) ** 2) * n * 0.6
    img = ring[..., None] * np.array([0.25, 0.9, 0.6]) + inner[..., None] * np.array([0.3, 0.75, 0.9])
    img += outer[..., None] * np.array([0.9, 0.35, 0.25])
    bg = fbm(H, W, 180, 4, rng)
    img += colorize(bg, [(0, (0, 0, 0)), (0.6, (0.0, 0.03, 0.03)), (1, (0.04, 0.12, 0.08))])
    stars(img, rng, 1000, 6)
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    rc = np.sqrt((xx - 0.5 * W) ** 2 + (yy - 0.42 * H) ** 2)
    img += np.exp(-(rc / 2.5) ** 2)[..., None] * np.array([0.9, 0.95, 1.0])
    return img


def scene_supernova(rng):
    """Remanescente de supernova: filamentos alaranjados ao redor de um núcleo azul."""
    x, y = coords()
    dx = (x - 0.5) * W / H
    dy = (y - 0.42) / 0.75
    r = np.sqrt(dx ** 2 + dy ** 2)
    th = np.arctan2(dy, dx)
    n = fbm(H, W, 18, 6, rng)
    fil = blur((np.abs(np.sin(th * 31 + n * 6)) ** 14 * np.exp(-((r - 0.25) / 0.12) ** 2)).astype(np.float32), 1)
    shell = np.exp(-((r - 0.27) / 0.05) ** 2) * n
    core = np.exp(-(r / 0.16) ** 2) * (0.5 + n)
    img = core[..., None] * np.array([0.35, 0.55, 1.0]) * 0.9
    img += (fil * 1.6 + shell)[..., None] * np.array([1.0, 0.45, 0.12])
    img += (fil * shell * 3)[..., None] * np.array([1.0, 0.9, 0.5])
    bg = fbm(H, W, 200, 4, rng)
    img += colorize(bg, [(0, (0, 0, 0)), (0.6, (0.03, 0.01, 0.01)), (1, (0.12, 0.04, 0.02))])
    stars(img, rng, 900, 6)
    return img


SCENES = {
    "one_handed": scene_pillars,
    "archery": scene_spiral,
    "block": scene_blue,
    "armor": scene_emission,
    "mining": scene_gold,
    "woodcutting": scene_ring,
    "smithing": scene_supernova,
}


def tonemap(img):
    img = 1 - np.exp(-img * 1.4)  # suaviza os brilhos
    return np.clip(img ** 0.95, 0, 1)


def write_png(path, rgb):
    data = (np.clip(rgb, 0, 1) * 255).astype(np.uint8)
    raw = b"".join(b"\x00" + data[i].tobytes() for i in range(data.shape[0]))
    def chunk(t, b):
        return struct.pack(">I", len(b)) + t + b + struct.pack(">I", zlib.crc32(t + b) & 0xFFFFFFFF)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", data.shape[1], data.shape[0], 8, 2, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    open(path, "wb").write(png)


if __name__ == "__main__":
    out = sys.argv[1]
    for i, (name, fn) in enumerate(SCENES.items()):
        rng = np.random.default_rng(1000 + i)
        img = vignette(tonemap(fn(rng)))
        write_png(f"{out}/{name}.png", img)
        print(name)
