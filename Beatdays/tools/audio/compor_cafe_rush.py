"""Composição original, partitura exportada do jogo e foley de resposta ao toque."""
from pathlib import Path
import csv, json, wave, hashlib
import numpy as np
W = Path(__file__).resolve().parents[2]
STAGE = W / 'app/src/main/res/raw'
STAGE.mkdir(parents=True, exist_ok=True)
(W / 'dist/audio').mkdir(parents=True, exist_ok=True)
SR = 44100
N = SR * 80
rng = np.random.default_rng(2026101302)
events = []
with Path(__file__).with_name('partitura_cafe_rush.csv').open(encoding='utf-8-sig') as f:
    for row in csv.reader(f):
        i, p, q, kind, a, c, l, d = row
        events.append(dict(id=int(i), pedido=int(p), quantidade=int(q), tipo=kind, aviso=int(a), captura=int(c), lancamento=int(l), demonstracao=d == 'true'))
assert len(events) == 40 and sum((e['captura'] >= 20000 for e in events)) == 31

def hz(midi):
    return 440 * 2 ** ((midi - 69) / 12)

def time(duration):
    return np.arange(round(SR * duration), dtype=np.float64) / SR

def env(t, d, attack=0.003, release=0.025):
    return np.minimum(1, t / attack) * np.minimum(1, np.maximum(0, d - t) / release)

def pluck(midi, d=0.23, amp=0.05, kind='wood'):
    t = time(d)
    f = hz(midi)
    if kind == 'bass':
        s = np.sin(2 * np.pi * f * t) + 0.45 * np.sin(2 * np.pi * f * 2 * t) + 0.22 * np.sin(2 * np.pi * f * 3 * t) + 0.1 * np.sin(2 * np.pi * f * 4 * t)
        s *= np.exp(-6 * t)
    elif kind == 'wood':
        s = np.sin(2 * np.pi * f * t) * np.exp(-12 * t) + 0.26 * np.sin(2 * np.pi * f * 3.99 * t) * np.exp(-28 * t)
    elif kind == 'guitar':
        s = sum((np.sin(2 * np.pi * f * k * t + 0.07 * k) * np.exp(-(7 + k * 2) * t) / k ** 1.8 for k in range(1, 7)))
    else:
        s = np.sin(2 * np.pi * f * t) * np.exp(-11 * t) + 0.16 * np.sin(2 * np.pi * f * 2.76 * t) * np.exp(-19 * t)
    return s * env(t, d) * amp

def noise(d, lo, hi, decay, amp):
    t = time(d)
    s = rng.normal(0, 1, len(t))
    frequencies = np.fft.rfftfreq(len(s), 1 / SR)
    spectrum = np.fft.rfft(s)
    shape = 1 / (1 + (lo / np.maximum(frequencies, 0.01)) ** 4) / (1 + (frequencies / hi) ** 4)
    s = np.fft.irfft(spectrum * shape, n=len(s))
    return s * np.exp(-decay * t) * env(t, d, 0.0008, 0.012) * amp

def kick():
    t = time(0.18)
    phase = 2 * np.pi * (48 * t + 52 * (1 - np.exp(-35 * t)) / 35)
    return np.sin(phase) * np.exp(-25 * t) * env(t, 0.18, 0.002, 0.025) * 0.085

def rim():
    t = time(0.08)
    return (np.sin(2 * np.pi * 1640 * t) * 0.012 + np.sin(2 * np.pi * 580 * t) * 0.038) * np.exp(-72 * t) * env(t, 0.08, 0.0007, 0.018) + noise(0.08, 900, 6500, 90, 0.014)

def put(bus, ms, sample, pan=0, gain=1):
    start = round(ms * SR / 1000)
    end = min(len(bus), start + len(sample))
    if end <= start or start < 0:
        return
    sample = sample[:end - start] * gain
    if bus.ndim == 1:
        bus[start:end] += sample
    else:
        bus[start:end, 0] += sample * np.sqrt((1 - pan) / 2)
        bus[start:end, 1] += sample * np.sqrt((1 + pan) / 2)
rhythm = np.zeros((N, 2), dtype=np.float32)
harmony = np.zeros_like(rhythm)
melody = np.zeros_like(rhythm)
cue_bus = np.zeros_like(rhythm)
sections = [(0, 4000, 'abertura', 0.64), (4000, 12000, 'demonstração', 0.7), (12000, 20000, 'treino', 0.78), (20000, 35000, 'tema A', 1), (35000, 49500, 'tema B', 1.05), (49500, 52000, 'respiro', 0.42), (52000, 64000, 'retomada', 1), (64000, 69500, 'trios rápidos', 1.17), (69500, 71500, 'suspensão', 0.52), (71500, 75000, 'último pedido', 0.92), (75000, 80000, 'fechamento', 0.68)]

def arrangement(ms):
    return next(((name, gain) for a, b, name, gain in sections if a <= ms < b))
roots = [36, 33, 38, 31]
chords = [[60, 64, 69, 74], [57, 60, 64, 67], [57, 60, 64, 65], [59, 62, 64, 69]]
for beat in range(150):
    ms = beat * 500
    section, gain = arrangement(ms)
    group = beat // 8 % 4
    rest = section in ('respiro', 'suspensão')
    if not rest or beat % 4 == 0:
        put(rhythm, ms, kick(), gain=gain * 0.86)
        bass_midi = roots[group] + (7 if beat % 8 in (3, 7) else 12 if beat % 8 == 6 else 0)
        put(harmony, ms, pluck(bass_midi, 0.31, 0.062, 'bass'), -0.08, gain)
    if not rest:
        if beat % 2:
            put(rhythm, ms, rim(), 0.13, gain)
        if section not in ('abertura', 'demonstração'):
            put(rhythm, ms + 250, noise(0.045, 4800, 14000, 105, 0.012), 0.22, gain * 0.8)
        if beat % 4 in (0, 2):
            for j, note in enumerate(chords[group]):
                put(harmony, ms + 250 + j * 4, pluck(note, 0.19, 0.021, 'guitar'), -0.22, gain)
        if beat % 8 in (2, 5) and ms >= 20000:
            put(harmony, ms + 375, pluck(roots[group] + 12, 0.1, 0.028, 'bass'), -0.08, gain)
    if rest and beat % 4 == 0:
        for j, note in enumerate(chords[group][:3]):
            put(harmony, ms + j * 12, pluck(note, 0.65, 0.018, 'bell'), -0.12, gain)
motifs = [[(0, 72), (1.5, 76), (3, 79), (4.5, 76), (6, 74), (7.25, 72)], [(0, 69), (1.5, 72), (2.75, 76), (4, 79), (5.5, 76), (7, 72)], [(0, 74), (1.25, 77), (3, 81), (4.5, 79), (6, 77), (7.25, 76)], [(0, 71), (1.5, 74), (3, 79), (4, 81), (5.5, 79), (7, 74)]]

def near_gameplay(ms):
    return any((e['aviso'] - 90 <= ms < e['aviso'] + 230 or e['captura'] - 140 <= ms < e['captura'] + 160 for e in events))
for bar in range(19):
    start = bar * 4000
    section, gain = arrangement(start)
    if section in ('respiro', 'suspensão'):
        continue
    pattern = motifs[bar % 4]
    if section in ('abertura', 'demonstração', 'treino'):
        pattern = pattern[:3]
    for offset, note in pattern:
        ms = start + offset * 500
        if ms >= 75000 or near_gameplay(ms):
            continue
        note += 12 if section == 'trios rápidos' and offset >= 4 else 0
        put(melody, ms, pluck(note, 0.27, 0.041, 'wood'), 0.12, gain)
        put(melody, ms + 145, pluck(note, 0.18, 0.007, 'wood'), -0.18, gain)
    fill = start + 3500
    if start >= 20000 and section not in ('demonstração', 'treino') and (not near_gameplay(fill)):
        for j in range(3):
            ms = fill + j * 125
            if not near_gameplay(ms):
                put(rhythm, ms, rim(), (-1 if j % 2 else 1) * 0.18, 0.44)
t = time(0.21)
fundamental = 225 + 30 * np.sin(np.pi * t / 0.21) + 6 * np.sin(2 * np.pi * 8 * t)
phase = 2 * np.pi * np.cumsum(fundamental) / SR
call = np.zeros(len(t))
vowel = np.minimum(1, t / 0.11)
for k in range(1, 15):
    f = k * 230
    a = np.exp(-((f - 540) / 240) ** 2) + 0.7 * np.exp(-((f - 1550) / 390) ** 2)
    b = np.exp(-((f - 330) / 190) ** 2) + 0.4 * np.exp(-((f - 2200) / 400) ** 2)
    call += ((1 - vowel) * a + vowel * b) * np.sin(k * phase) / k
call /= max(abs(call).max(), 1e-08)
call = call * 0.34 * env(t, 0.21, 0.009, 0.04)
call[:round(SR * 0.03)] += noise(0.03, 800, 5000, 70, 0.032)
t = time(0.14)
clock = (np.sin(2 * np.pi * 880 * t) * 0.25 * np.exp(-27 * t) + np.sin(2 * np.pi * 2190 * t) * 0.12 * np.exp(-45 * t)) * env(t, 0.14, 0.0006, 0.016)
clock += noise(0.14, 900, 9000, 100, 0.044)
t = time(0.105)
timer = (np.sin(2 * np.pi * 1174.66 * t) * 0.33 + 0.11 * np.sin(2 * np.pi * 2349.32 * t)) * env(t, 0.105, 0.0015, 0.018) * np.exp(-8 * t)
cues = {'MIKA': call, 'RELOGIO': clock, 'TIMER': timer}
for e in events:
    put(cue_bus, e['aviso'], cues[e['tipo']])
    assert e['captura'] - e['lancamento'] == 750
duck = np.ones(N, dtype=np.float32)
for e in events:
    for at, before, after, depth in [(e['aviso'], 28, 240, 0.62), (e['captura'], 85, 175, 0.86)]:
        lo = max(0, round((at - before) * SR / 1000))
        hi = min(N, round((at + after) * SR / 1000))
        envelope = np.full(hi - lo, depth, dtype=np.float32)
        ramp = min(round(SR * 0.025), len(envelope) // 2)
        envelope[:ramp] = np.linspace(1, depth, ramp)
        envelope[-ramp:] = np.linspace(depth, 1, ramp)
        duck[lo:hi] = np.minimum(duck[lo:hi], envelope)
mix = 3.1 * (rhythm + harmony + melody) * duck[:, None] + cue_bus

def capture(name, midi):
    d = 0.16
    t = time(d)
    body = np.sin(2 * np.pi * np.cumsum(165 + 210 * np.exp(-80 * t)) / SR) * np.exp(-37 * t) * 0.32
    swish = noise(d, 300, 8500, 48, 0.065)
    click = noise(d, 1500, 13000, 170, 0.13)
    tone = pluck(midi, d, 0.085, 'wood')
    sample = body * env(t, d, 0.0007, 0.018) + swish + click + tone
    if name == 'cafe':
        sample += pluck(85, d, 0.065, 'bell')
    elif name == 'croissant':
        sample += noise(d, 3200, 13500, 43, 0.075)
    elif name == 'panqueca':
        sample += np.sin(2 * np.pi * 220 * t) * np.exp(-24 * t) * env(t, d) * 0.08
    else:
        sample += np.sin(2 * np.pi * np.cumsum(430 - 190 * np.minimum(1, t / 0.075)) / SR) * np.exp(-44 * t) * env(t, d) * 0.085
    return sample
foley = {f'captura_{name}': capture(name, midi) for name, midi in [('cafe', 72), ('croissant', 76), ('panqueca', 67), ('cupcake', 69)]}
t = time(0.135)
foley['miss_recepcao'] = (np.sin(2 * np.pi * np.cumsum(245 * np.exp(-10 * t) + 70) / SR) * np.exp(-24 * t) * 0.24 + noise(0.135, 350, 4500, 36, 0.042)) * env(t, 0.135, 0.001, 0.025)
foley['toque_recepcao'] = noise(0.045, 1200, 7500, 90, 0.14) + pluck(79, 0.045, 0.055, 'wood')
foley['brilho_perfect'] = np.zeros(round(SR * 0.16))
put(foley['brilho_perfect'], 14, pluck(96, 0.13, 0.13, 'bell'))
put(foley['brilho_perfect'], 44, pluck(100, 0.1, 0.072, 'bell'))
for name, sample in foley.items():
    assert abs(sample).max() < 0.85
    sample[-round(SR * 0.008):] *= np.linspace(1, 0, round(SR * 0.008))

def write(path, samples, channels):
    assert np.isfinite(samples).all() and abs(samples).max() < 0.94
    with wave.open(str(path), 'wb') as f:
        f.setnchannels(channels)
        f.setsampwidth(2)
        f.setframerate(SR)
        f.writeframes(np.round(samples * 32767).astype('<i2').tobytes())
for name, sample in foley.items():
    write(STAGE / f'{name}.wav', sample, 1)
food_names = ['cafe', 'croissant', 'panqueca', 'cupcake']
for e in events:
    if e['demonstracao']:
        name = food_names[(e['pedido'] + e['id'] - next((x['id'] for x in events if x['pedido'] == e['pedido']))) % 4]
        put(mix, e['captura'], foley[f'captura_{name}'], gain=0.74)
        put(mix, e['captura'], foley['brilho_perfect'], gain=0.27)
for j, note in enumerate([72, 76, 79, 84]):
    put(mix, 75500 + j * 125, pluck(note, 0.4, 0.034, 'bell'), j * 0.1 - 0.15)
for j, note in enumerate([48, 60, 64, 67, 69]):
    put(mix, 76500 + j * 4, pluck(note, 1.5, 0.045 if j == 0 else 0.02, 'guitar'))
mix[:round(SR * 0.02)] *= np.linspace(0, 1, round(SR * 0.02))[:, None]
mix[-2 * SR:] *= np.linspace(1, 0, 2 * SR)[:, None]
peak = float(abs(mix).max())
assert peak < 0.9
track = STAGE / 'cafe_rush_final.wav'
write(track, mix, 2)
write(W / 'dist/audio/cafe-rush-trilha.wav', mix, 2)
preview = mix.copy() * 0.84
for e in events:
    if not e['demonstracao']:
        first = next((x['id'] for x in events if x['pedido'] == e['pedido']))
        name = food_names[(e['pedido'] + e['id'] - first) % 4]
        put(preview, e['captura'], foley[f'captura_{name}'], gain=0.44 * np.sqrt(2))
        put(preview, e['captura'], foley['brilho_perfect'], gain=0.16 * np.sqrt(2))
preview = np.concatenate([preview[28 * SR:40 * SR], preview[55 * SR:69 * SR]], axis=0)
preview[:round(SR * 0.04)] *= np.linspace(0, 1, round(SR * 0.04))[:, None]
preview[-round(SR * 0.12):] *= np.linspace(1, 0, round(SR * 0.12))[:, None]
write(W / 'dist/audio/cafe-rush-previa-com-acertos.wav', preview, 2)
report = dict(recurso_usado='R.raw.cafe_rush_final', bpm=120, duracao_ms=80000, taxa_hz=SR, canais=2, pico=peak, rms=float(np.sqrt(np.mean(mix ** 2))), sha256=hashlib.sha256(track.read_bytes()).hexdigest(), secoes=[dict(inicio_ms=a, fim_ms=b, nome=name) for a, b, name, _ in sections], eventos=events, feedback={name: dict(duracao_ms=round(len(s) / SR * 1000), pico=float(abs(s).max()), sha256=hashlib.sha256((STAGE / f'{name}.wav').read_bytes()).hexdigest()) for name, s in foley.items()})
(W / 'dist/audio/cafe-rush-eventos.json').write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
print('Café Rush recomposta: 80 segundos, 120 BPM, 40 avisos e sete efeitos.')
