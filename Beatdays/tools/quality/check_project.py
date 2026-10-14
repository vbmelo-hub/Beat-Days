"""Confere pacotes, recursos e os arquivos de áudio preservados."""
from pathlib import Path
import csv
import hashlib
import json
import re
import struct
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "app/src"
RES = SRC / "main/res"
ANDROID = "{http://schemas.android.com/apk/res/android}"
resources = set()
classes = set()
java_sources = list(SRC.rglob("*.java"))

for path in java_sources:
    text = path.read_text(encoding="utf-8")
    package = re.search(r"^package\s+([\w.]+);", text, re.M)
    assert package, f"Pacote ausente: {path}"
    source_root = next(parent for parent in path.parents if parent.name == "java")
    assert path.relative_to(source_root).as_posix() == package[1].replace(".", "/") + "/" + path.name
    assert not re.search(r"^import\s+(?:static\s+)?[\w.]+\.\*;", text, re.M), f"Import genérico: {path}"
    if "main" in path.relative_to(SRC).parts:
        classes.add(package[1] + "." + path.stem)

for path in RES.rglob("*"):
    if not path.is_file():
        continue
    kind = path.parent.name.split("-")[0]
    if kind == "values":
        for node in ET.parse(path).getroot():
            if node.get("name"):
                resources.add((node.tag, node.get("name").replace(".", "_")))
    else:
        resources.add((kind, path.stem))
    if path.suffix == ".xml":
        text = path.read_text(encoding="utf-8")
        for name in re.findall(r"@\+id/(\w+)", text):
            assert re.fullmatch(r"[a-z][a-z0-9_]*", name), f"Nome de recurso inválido: {name}"
            resources.add(("id", name))
        for node in ET.parse(path).iter():
            if node.tag.startswith("com.example.beatdays."):
                assert node.tag in classes, f"View ausente: {node.tag}"

for path in java_sources:
    text = path.read_text(encoding="utf-8")
    for ref in re.finditer(r"(?<!android\.)\bR\.(\w+)\.(\w+)", text):
        assert (ref[1], ref[2]) in resources, f"Recurso sem definição: {ref[0]} em {path}"

for node in ET.parse(SRC / "main/AndroidManifest.xml").iter("activity"):
    name = node.get(ANDROID + "name")
    assert ("com.example.beatdays" + name if name.startswith(".") else name) in classes

metadata = json.loads((ROOT / "tools/audio/metadata/cafe_rush.json").read_text(encoding="utf-8"))
expected = {"cafe_rush_final": metadata["sha256"]}
expected.update({name: info["sha256"] for name, info in metadata["feedback"].items()})
for name, digest in expected.items():
    assert hashlib.sha256((RES / "raw" / (name + ".wav")).read_bytes()).hexdigest() == digest, f"Áudio alterado: {name}"
assert set(path.stem for path in (RES / "raw").glob("*.wav")) == set(expected)
with (ROOT / "tools/audio/partitura_cafe_rush.csv").open(encoding="utf-8-sig") as stream:
    events = list(csv.reader(stream))
assert [(int(e[0]), int(e[4]), int(e[5])) for e in events] == [(e["id"], e["aviso"], e["captura"]) for e in metadata["eventos"]]

sprites = json.loads((RES / "raw/sprites_geometry.json").read_text(encoding="utf-8"))
for family in sprites.values():
    assert 0 <= family["ancora"] <= 1 and 0 <= family["base"] <= 1
    for name, info in family["quadros"].items():
        png = RES / "drawable-nodpi" / (name + ".png")
        header = png.read_bytes()[:24]
        assert header[:8] == b"\x89PNG\r\n\x1a\n"
        width, height = struct.unpack(">II", header[16:24])
        left, top, right, bottom = info["crop"]
        assert 0 <= left < right <= width and 0 <= top < bottom <= height
        assert ("drawable", name) in resources

print(f"Estrutura conferida: {len(java_sources)} arquivos Java, {len(resources)} recursos e 8 áudios preservados.")
