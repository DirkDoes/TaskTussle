"""Copy vanilla status-effect icons and generate their item models.

Usage: python source/tools/extract-status-effect-icons.py <minecraft-client.jar> [--check]
"""

import argparse
import json
from pathlib import Path
from zipfile import ZipFile


ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "resourcepack/assets/tasktussle"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("client_jar", type=Path)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    count = 0
    with ZipFile(args.client_jar) as jar:
        for name in sorted(jar.namelist()):
            if not name.startswith("assets/minecraft/textures/mob_effect/") or not name.endswith(".png"):
                continue
            effect = Path(name).stem
            outputs = {
                f"textures/item/status_effects/{effect}.png": jar.read(name),
                f"models/item/status_effect/{effect}.json": (json.dumps({
                    "parent": "minecraft:item/generated",
                    "textures": {"layer0": f"tasktussle:item/status_effects/{effect}"},
                }, indent=2) + "\n").encode(),
                f"items/status_effect/{effect}.json": (json.dumps({
                    "model": {"type": "minecraft:model", "model": f"tasktussle:item/status_effect/{effect}"},
                }, indent=2) + "\n").encode(),
            }
            for relative, data in outputs.items():
                path = ASSETS / relative
                if args.check:
                    assert path.read_bytes() == data, f"Regenerate {path}"
                else:
                    path.parent.mkdir(parents=True, exist_ok=True)
                    path.write_bytes(data)
            count += 1
    assert count > 0, "Client JAR has no status-effect icons"
    print(f"{'Checked' if args.check else 'Extracted'} {count} status-effect icons and item models.")


if __name__ == "__main__":
    main()
