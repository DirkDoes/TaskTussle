"""Generate unchanged vanilla item models with GUI-only 18px overlays.

Usage: python source/tools/generate-item-overlays.py <minecraft-client.jar> [--check]
Arguments: custom_model_data.strings = ["minecraft:item_id", "overlay_texture_stem"].
Add 18x18 PNGs to textures/item/tasks/overlays and rerun to register more overlays.
"""

import argparse
import json
from pathlib import Path
import struct
from zipfile import ZipFile


ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "resourcepack/assets/tasktussle"
EMPTY = {"type": "minecraft:empty"}


def selector(index, entries, fallback):
    return {"type": "minecraft:select", "property": "minecraft:custom_model_data", "index": index,
            "cases": [{"when": key, "model": model} for key, model in entries], "fallback": fallback}


def generate(jar):
    items = [("minecraft:" + Path(name).stem, json.loads(jar.read(name))["model"])
             for name in sorted(jar.namelist())
             if name.startswith("assets/minecraft/items/") and name.endswith(".json")]
    outputs, overlays = {}, []
    for texture in sorted((ASSETS / "textures/item/tasks/overlays").glob("*.png")):
        data = texture.read_bytes()
        if data[:8] != b"\x89PNG\r\n\x1a\n" or struct.unpack(">II", data[16:24]) != (18, 18):
            raise ValueError(f"Overlay must be an 18x18 PNG: {texture}")
        model = f"item/tasks/overlays/{texture.stem}"
        outputs[f"models/{model}.json"] = {
            "gui_light": "front",
            "textures": {"layer0": f"tasktussle:{model}"},
            # A composite shares GUI lighting with its first item layer.
            "elements": [{"from": [0, 0, 8.5], "to": [16, 16, 8.5],
                          "shade": False, "light_emission": 15,
                          "faces": {"south": {"uv": [0, 0, 16, 16], "texture": "#layer0", "tintindex": 0}}}],
            "display": {"gui": {"scale": [1.125, 1.125, 1], "translation": [0, 0, 16]}},
        }
        # RGB 1,2,3 marks UI overlay faces for the pack's item vertex shader.
        overlays.append((texture.stem, {"type": "minecraft:model", "model": f"tasktussle:{model}",
                                        "tints": [{"type": "minecraft:constant", "value": 0x010203}]}))
    outputs["items/item_overlay.json"] = {
        "oversized_in_gui": True,
        "model": {"type": "minecraft:composite", "models": [
            selector(0, items, dict(items)["minecraft:knowledge_book"]),
            {"type": "minecraft:select", "property": "minecraft:display_context",
             "cases": [{"when": "gui", "model": selector(1, overlays, EMPTY)}], "fallback": EMPTY},
        ]},
    }
    return outputs


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("client_jar", type=Path)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    with ZipFile(args.client_jar) as jar:
        outputs = generate(jar)
    for name, model in outputs.items():
        path = ASSETS / name
        if args.check:
            assert json.loads(path.read_text()) == model, f"Regenerate {path}"
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(model, indent=2) + "\n")
    print(f"{'Checked' if args.check else 'Generated'} item overlay catalog and {len(outputs) - 1} overlays.")


if __name__ == "__main__":
    main()
