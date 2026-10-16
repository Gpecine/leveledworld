"""Sintetiza os sons originais do mod (level up e perk) e grava WAVs mono 44.1 kHz.

Uso: python3 tools/gen_sounds.py <pasta-saida>; depois converta para OGG, por exemplo:
  ffmpeg -i level_up.wav -af "atrim=0:4.8,afade=t=out:st=4.0:d=0.8" -c:a libvorbis -q:a 5 level_up.ogg
  ffmpeg -i perk_unlock.wav -af "atrim=0:2.6,afade=t=out:st=2.0:d=0.6" -c:a libvorbis -q:a 5 perk_unlock.ogg
"""
import sys
import wave
import numpy as np

SR = 44100
rng = np.random.default_rng(7)


def t_axis(dur):
    return np.arange(int(SR * dur)) / SR


def env_adsr(n, a, d, s, r, sustain_level=0.7):
    a, d, r = int(a * SR), int(d * SR), int(r * SR)
    s = max(0, n - a - d - r)
    return np.concatenate([
        np.linspace(0, 1, a, endpoint=False),
        np.linspace(1, sustain_level, d, endpoint=False),
        np.full(s, sustain_level),
        np.linspace(sustain_level, 0, r),
    ])[:n]


def place(buf, sig, start):
    i = int(start * SR)
    end = min(len(buf), i + len(sig))
    buf[i:end] += sig[: end - i]


def horn(freq, dur, vib=4.5):
    """Trompa: harmônicos com queda suave, vibrato leve e brilho que abre com o tempo."""
    t = t_axis(dur)
    phase = 2 * np.pi * freq * t + 0.004 * freq / vib * np.sin(2 * np.pi * vib * t) * (t > 0.5)
    bright = np.clip(t / 0.8, 0, 1)
    sig = np.zeros_like(t)
    for h in range(1, 9):
        amp = (1.0 / h ** 1.25) * (1 if h < 3 else bright)
        sig += amp * np.sin(h * phase)
    return sig * env_adsr(len(t), 0.35, 0.3, 0, 0.9, 0.8)


def choir(freq, dur):
    """Coro "aah": dente-de-serra filtrado por formantes, com cópias desafinadas (chorus)."""
    t = t_axis(dur)
    out = np.zeros_like(t)
    formants = [(730, 90), (1090, 110), (2440, 160)]
    weights = [1.0, 0.7, 0.18]
    for detune in (-0.006, 0.0, 0.007):
        f = freq * (1 + detune)
        sig = np.zeros_like(t)
        for h in range(1, 40):
            hf = h * f
            if hf > 5000:
                break
            gain = sum(w * np.exp(-((hf - fc) ** 2) / (2 * bw ** 2)) for (fc, bw), w in zip(formants, weights)) + 0.15 / h
            sig += gain * np.sin(2 * np.pi * hf * t + rng.uniform(0, 6.28))
        out += sig
    return out * env_adsr(len(t), 0.7, 0.4, 0, 1.2, 0.85)


def drum(dur=1.2):
    t = t_axis(dur)
    pitch = 52 + 40 * np.exp(-t * 18)
    body = np.sin(2 * np.pi * np.cumsum(pitch) / SR) * np.exp(-t * 4.5)
    noise = rng.normal(0, 1, len(t)) * np.exp(-t * 30)
    noise = np.convolve(noise, np.ones(40) / 40, mode="same")  # passa-baixa simples
    return body * 1.0 + noise * 0.6


def bell(freq, dur):
    t = t_axis(dur)
    partials = [(1, 1.0), (2.76, 0.45), (5.4, 0.25), (8.93, 0.12)]
    sig = sum(a * np.sin(2 * np.pi * freq * p * t) * np.exp(-t * (2.5 + p)) for p, a in partials)
    return sig * np.minimum(1, t / 0.004)


def reverb(sig, seconds=2.2, mix=0.35):
    n = int(SR * seconds)
    ir = rng.normal(0, 1, n) * np.exp(-np.linspace(0, 7, n))
    ir[: int(0.02 * SR)] *= np.linspace(0, 1, int(0.02 * SR))
    size = len(sig) + n
    wet = np.fft.irfft(np.fft.rfft(sig, size) * np.fft.rfft(ir, size), size)[: len(sig) + n]
    wet /= np.max(np.abs(wet)) + 1e-9
    dry = np.concatenate([sig, np.zeros(n)])
    dry /= np.max(np.abs(dry)) + 1e-9
    return dry * (1 - mix) + wet * mix


def finish(sig, fade=0.6):
    f = int(fade * SR)
    sig[-f:] *= np.linspace(1, 0, f)
    sig /= np.max(np.abs(sig)) + 1e-9
    return (sig * 0.89).astype(np.float32)


def write(path, sig):
    data = (sig * 32767).astype(np.int16)
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(data.tobytes())


def level_up():
    buf = np.zeros(int(SR * 3.6))
    place(buf, drum() * 0.9, 0.0)
    place(buf, drum(0.8) * 0.45, 0.42)
    # Acorde menor (Ré) que resolve para maior: Fá -> Fá#
    d2, a2, d3, f3, fs3, a3 = 73.42, 110.0, 146.83, 174.61, 185.0, 220.0
    for f, amp in ((d2, 0.5), (a2, 0.45), (d3, 0.4), (a3, 0.3)):
        place(buf, horn(f, 3.2) * amp, 0.05)
    place(buf, horn(f3, 1.25) * 0.35, 0.05)
    place(buf, horn(fs3, 2.0) * 0.38, 1.25)
    for f, amp in ((293.66, 0.16), (440.0, 0.12), (369.99, 0.10)):
        place(buf, choir(f, 2.6) * amp / 3, 0.7)
    for i, f in enumerate((1174.7, 1479.98, 1760.0, 2349.3)):
        place(buf, bell(f, 1.6) * 0.07, 1.35 + i * 0.09)
    return finish(reverb(buf, 2.4, 0.32))


def perk_unlock():
    buf = np.zeros(int(SR * 1.6))
    for i, f in enumerate((587.33, 880.0, 1174.66, 1479.98)):
        place(buf, bell(f, 1.3) * (0.5 - i * 0.06), i * 0.075)
    place(buf, choir(587.33, 1.3) * 0.04, 0.05)
    return finish(reverb(buf, 1.8, 0.4), 0.4)


if __name__ == "__main__":
    out = sys.argv[1]
    write(f"{out}/level_up.wav", level_up())
    write(f"{out}/perk_unlock.wav", perk_unlock())
    print("ok")
