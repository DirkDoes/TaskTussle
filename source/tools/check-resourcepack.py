"""Check local model, texture, font, particle and sound references.

Usage: python source/tools/check-resourcepack.py [resourcepack-directory]
Vanilla references are supplied by Minecraft; this checks TaskTussle resources.
"""

import argparse
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def validate(pack):
    assets = pack / "assets"
    files = {path.relative_to(assets).as_posix() for path in assets.rglob("*") if path.is_file()}
    errors = []
    checked = 0

    def reference(source, resource, folder, suffix):
        nonlocal checked
        if not isinstance(resource, str) or not resource.startswith("tasktussle:"):
            return
        checked += 1
        name = resource.removeprefix("tasktussle:")
        target = f"tasktussle/{folder}/{name}{suffix}"
        if target not in files:
            errors.append(f"{source}: {resource} -> missing {target}")

    def item_models(source, node):
        if isinstance(node, list):
            for child in node:
                item_models(source, child)
        elif isinstance(node, dict):
            if node.get("type") == "minecraft:model":
                reference(source, node.get("model"), "models", ".json")
            if node.get("type") == "minecraft:special":
                reference(source, node.get("base"), "models", ".json")
            for child in node.values():
                item_models(source, child)

    for name in sorted(files):
        if not name.endswith(".json"):
            continue
        try:
            data = json.loads((assets / name).read_text(encoding="utf-8"))
        except (ValueError, UnicodeError) as error:
            errors.append(f"{name}: {error}")
            continue
        category = name.split("/")[1]
        if category == "models":
            reference(name, data.get("parent"), "models", ".json")
            for texture in data.get("textures", {}).values():
                reference(name, texture, "textures", ".png")
        elif category == "items":
            item_models(name, data)
        elif category == "font":
            for provider in data.get("providers", []):
                reference(name, provider.get("file"), "textures", "")
                reference(name, provider.get("id"), "font", ".json")
        elif category == "particles":
            for texture in data.get("textures", []):
                reference(name, texture, "textures/particle", ".png")
        elif category == "sounds.json":
            for event in data.values():
                for sound in event.get("sounds", []):
                    if isinstance(sound, str):
                        reference(name, sound, "sounds", ".ogg")
                    elif sound.get("type", "file") == "file":
                        reference(name, sound.get("name"), "sounds", ".ogg")
    return errors, checked


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("pack", nargs="?", type=Path, default=ROOT / "resourcepack")
    args = parser.parse_args()
    if not (args.pack / "pack.mcmeta").is_file():
        parser.error(f"Not a resource pack: {args.pack}")
    errors, checked = validate(args.pack)
    if errors:
        raise SystemExit("\n".join(errors))
    print(f"Checked {checked} TaskTussle references; all targets exist with matching filename case.")


if __name__ == "__main__":
    main()
