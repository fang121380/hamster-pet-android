from pathlib import Path
import math
import random
import struct
import wave

ROOT = Path(__file__).resolve().parents[1]
RAW = ROOT / "app" / "src" / "main" / "res" / "raw"
RATE = 44100


def save(name, duration, sample):
    count = int(duration * RATE)
    values = []
    for index in range(count):
        value = max(-1.0, min(1.0, sample(index / RATE)))
        values.append(struct.pack("<h", int(value * 32767)))
    with wave.open(str(RAW / name), "wb") as target:
        target.setnchannels(1)
        target.setsampwidth(2)
        target.setframerate(RATE)
        target.writeframes(b"".join(values))


def pulse(t, start, length):
    if t < start or t > start + length:
        return 0.0
    return math.exp(-7 * (t - start) / length)


def main():
    RAW.mkdir(parents=True, exist_ok=True)
    rng = random.Random(240816)
    eat_noise = [rng.uniform(-1, 1) for _ in range(int(.75 * RATE))]
    save("sfx_eat.wav", .75, lambda t: sum(
        pulse(t, start, .14) * (eat_noise[min(int(t * RATE), len(eat_noise) - 1)] * .34 + math.sin(2 * math.pi * 118 * t) * .16)
        for start in (.02, .24, .47)
    ))
    save("sfx_play.wav", .55, lambda t: sum(
        pulse(t, start, .16) * math.sin(2 * math.pi * freq * (t - start)) * .28
        for start, freq in ((0.0, 523), (.14, 659), (.28, 784))
    ))
    save("sfx_pat.wav", .32, lambda t: math.sin(2 * math.pi * (420 + 180 * t) * t) * math.exp(-8 * t) * .42)
    save("sfx_sleep.wav", .65, lambda t: (math.sin(2 * math.pi * 196 * t) + .45 * math.sin(2 * math.pi * 247 * t)) * math.exp(-5 * t) * .20)
    build_noise = [rng.uniform(-1, 1) for _ in range(int(.55 * RATE))]
    save("sfx_build.wav", .55, lambda t: sum(
        pulse(t, start, .10) * (build_noise[min(int(t * RATE), len(build_noise) - 1)] * .25 + math.sin(2 * math.pi * 155 * t) * .20)
        for start in (.02, .19, .36)
    ))


if __name__ == "__main__":
    main()
