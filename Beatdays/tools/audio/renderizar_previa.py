from pathlib import Path
import json, wave
import numpy as np
project = Path(__file__).resolve().parents[2]
root = project / 'app/src/main/res/raw'

def load(name):
    with wave.open(str(root / (name + '.wav')), 'rb') as f:
        assert f.getframerate() == 44100
        channels = f.getnchannels()
        data = np.frombuffer(f.readframes(f.getnframes()), dtype='<i2').astype(np.float64) / 32767
        return data.reshape(-1, channels)
preview = load('cafe_rush_final') * 0.84
events = json.loads((project / 'tools/audio/metadata/cafe_rush.json').read_text(encoding='utf-8'))['eventos']
foods = ['cafe', 'croissant', 'panqueca', 'cupcake']

def add(ms, sample, gain):
    start = round(ms * 44.1)
    preview[start:start + len(sample)] += sample * gain
for e in events:
    if e['demonstracao']:
        continue
    if e['captura'] == 61500:
        add(e['captura'] + 8, load('miss_recepcao'), 0.3)
    else:
        first = next((x['id'] for x in events if x['pedido'] == e['pedido']))
        food = foods[(e['pedido'] + e['id'] - first) % 4]
        add(e['captura'] + 8, load('captura_' + food), 0.44)
        add(e['captura'] + 8, load('brilho_perfect'), 0.16)
preview = np.concatenate([preview[28 * 44100:40 * 44100], preview[55 * 44100:69 * 44100]])
preview[:1764] *= np.linspace(0, 1, 1764)[:, None]
preview[-5292:] *= np.linspace(1, 0, 5292)[:, None]
assert abs(preview).max() < 0.9
with wave.open(str(project / 'dist/audio/cafe-rush-previa.wav'), 'wb') as f:
    f.setnchannels(2)
    f.setsampwidth(2)
    f.setframerate(44100)
    f.writeframes(np.round(preview * 32767).astype('<i2').tobytes())
print('Prévia: 26 segundos, música, acertos, PERFECT e um exemplo de MISS; ganhos iguais aos configurados no aplicativo.')
