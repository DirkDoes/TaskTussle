"""Generate the generic plate/item selectors from the matching Minecraft client JAR.

Usage: python source/tools/generate-backplates.py <minecraft-client.jar> [--check]
Requires NumPy for model transform and bounds calculations. Generated JSON lives
in the resource pack; Python is only needed when regenerating that catalog.
"""

import argparse
import copy
import itertools
import json
import math
from functools import lru_cache
from pathlib import Path
from zipfile import ZipFile

import numpy as np


ROOT = Path(__file__).resolve().parents[2]
HAND_CONTEXTS = ["firstperson_righthand", "firstperson_lefthand", "thirdperson_righthand", "thirdperson_lefthand"]


def box(lo, hi):
    return np.array(list(itertools.product(*zip(lo, hi))), dtype=float)


def rotation(angles):
    x, y, z = np.radians(angles)
    cx, sx, cy, sy, cz, sz = math.cos(x), math.sin(x), math.cos(y), math.sin(y), math.cos(z), math.sin(z)
    return np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]]) @ np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]) @ np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])


def quaternion(q):
    x, y, z, w = q
    return np.array([[1-2*(y*y+z*z), 2*(x*y-z*w), 2*(x*z+y*w)], [2*(x*y+z*w), 1-2*(x*x+z*z), 2*(y*z-x*w)], [2*(x*z-y*w), 2*(y*z+x*w), 1-2*(x*x+y*y)]])


def matrix(t):
    if isinstance(t, list):
        return np.array(t).reshape(4, 4)
    m = np.eye(4)
    m[:3, :3] = quaternion(t.get("left_rotation", [0, 0, 0, 1])) @ np.diag(t.get("scale", [1, 1, 1])) @ quaternion(t.get("right_rotation", [0, 0, 0, 1]))
    m[:3, 3] = t.get("translation", [0, 0, 0])
    return m


def transform(points, m):
    return points @ m[:3, :3].T + m[:3, 3]


def children(n, context=None):
    if n.get("type") == "minecraft:select" and n.get("property") == "minecraft:display_context" and context:
        for case in n["cases"]:
            when = case["when"] if isinstance(case["when"], list) else [case["when"]]
            if context in when:
                return [case["model"]]
        return [n["fallback"]] if "fallback" in n else []
    if n.get("type") == "minecraft:select":
        return [c["model"] for c in n["cases"]] + ([n["fallback"]] if "fallback" in n else [])
    if n.get("type") == "minecraft:range_dispatch":
        return [c["model"] for c in n["entries"]] + ([n["fallback"]] if "fallback" in n else [])
    if n.get("type") == "minecraft:condition":
        return [n["on_true"], n["on_false"]]
    return n.get("models", []) if n.get("type") == "minecraft:composite" else []


def composite(model, translation=None, scale=None):
    result = {"type": "minecraft:composite", "models": [model]}
    if translation is not None or scale is not None:
        result["transformation"] = {"translation": translation or [0, 0, 0], "left_rotation": [0, 0, 0, 1], "scale": scale or [1, 1, 1], "right_rotation": [0, 0, 0, 1]}
    return result


def select(index, entries, fallback):
    return {"type": "minecraft:select", "property": "minecraft:custom_model_data", "index": index, "cases": [{"when": key, "model": value} for key, value in entries], "fallback": fallback}


def views(gui, held, fixed):
    return {"type": "minecraft:select", "property": "minecraft:display_context", "cases": [{"when": HAND_CONTEXTS, "model": held}, {"when": "fixed", "model": fixed}], "fallback": gui}


def shared_select(index, entries, fallback):
    # Select the transform once per group, then select the item inside that group.
    groups = {}
    for key, value in entries:
        signature = json.dumps(value.get("transformation"), sort_keys=True)
        groups.setdefault(signature, []).append((key, value))
    cases = []
    for group in groups.values():
        transformation = group[0][1].get("transformation")
        if transformation is None or len(group) == 1:
            cases.extend(group)
            continue
        models = [(key, {k: v for k, v in value.items() if k != "transformation"}) for key, value in group]
        node = select(index, models, {"type": "minecraft:empty"})
        node["transformation"] = transformation
        cases.append(([key for key, _ in group], node))
    return select(index, cases, fallback)


def share_transforms(node):
    for key, value in list(node.items()):
        if isinstance(value, dict) and "type" in value:
            node[key] = share_transforms(value)
        elif isinstance(value, list):
            node[key] = [share_transforms(entry) if isinstance(entry, dict) else entry for entry in value]
        elif isinstance(value, dict) and "model" in value:
            node[key] = share_transforms(value)
    parts = children(node)
    if node.get("type") == "minecraft:composite" and len(parts) == 1 and "transformation" not in node:
        return parts[0]
    if parts and "transformation" not in node:
        common = parts[0].get("transformation")
        if common is not None and all(part.get("transformation") == common for part in parts):
            node["transformation"] = common
            for part in parts:
                del part["transformation"]
    return node


class Generator:
    def __init__(self, jar):
        self.jar = jar
        self.overrides = json.loads(Path(__file__).with_name("backplate-overrides.json").read_text())
        self.common = self.display("tasktussle:item/advancement_task", "fixed")

    @lru_cache(None)
    def model(self, ref):
        namespace, name = ref.split(":") if ":" in ref else ("minecraft", ref)
        file = f"assets/{namespace}/models/{name}.json"
        own = json.loads(self.jar.read(file)) if namespace == "minecraft" else json.loads((ROOT / "resourcepack" / file).read_text())
        parent = own.get("parent", "")
        inherited = self.model(parent) if parent and not parent.startswith("builtin/") else {}
        return inherited | own | {"display": inherited.get("display", {}) | own.get("display", {})}

    def display(self, ref, context):
        t = self.model(ref).get("display", {}).get(context, {})
        m = np.eye(4)
        m[:3, :3] = rotation(t.get("rotation", [0, 0, 0])) @ np.diag(t.get("scale", [1, 1, 1]))
        m[:3, 3] = np.array(t.get("translation", [0, 0, 0])) / 16 - m[:3, :3] @ np.full(3, .5)
        return m

    @lru_cache(None)
    def geometry(self, ref):
        points = []
        for e in self.model(ref).get("elements", []):
            p = box(e["from"], e["to"])
            if "rotation" in e:
                r = e["rotation"]
                angles = [0, 0, 0]
                axis = "xyz".index(r["axis"])
                angles[axis] = r["angle"]
                p = (p - r["origin"]) @ rotation(angles).T
                if r.get("rescale"):
                    factors = [1 / math.cos(math.radians(r["angle"]))] * 3
                    factors[axis] = 1
                    p *= factors
                p += r["origin"]
            points.append(p / 16)
        return np.concatenate(points) if points else box([0, 0, 7.5 / 16], [1, 1, 8.5 / 16])

    def volume(self, node):
        if node.get("type") == "minecraft:special":
            return True
        if node.get("type") == "minecraft:model":
            return bool(self.model(node["model"]).get("elements"))
        return any(self.volume(child) for child in children(node, "gui"))

    def points(self, node):
        kind = node.get("type")
        if kind == "minecraft:model":
            points = self.geometry(node["model"])
        elif kind == "minecraft:special":
            # shortcut: uncommon special renderers use conservative bounds; tune an item in the overrides file when needed.
            points = box([-.5, -1, -.5], [.5, 0, .5])
        else:
            parts = [self.points(child) for child in children(node, "fixed")]
            points = np.concatenate(parts) if parts else box([0, 0, 7.5 / 16], [1, 1, 8.5 / 16])
        return transform(points, matrix(node.get("transformation", {})))

    def normalize(self, node, parent):
        combined = parent @ matrix(node.pop("transformation", {}))
        if node.get("type") in ("minecraft:model", "minecraft:special"):
            ref = node["model"] if node["type"] == "minecraft:model" else node["base"]
            native = self.display(ref, "fixed")
            local = np.linalg.inv(native) @ self.common @ combined
            local[np.abs(local) < 1e-12] = 0
            assert np.allclose(native @ local, self.common @ combined), ref
            node["transformation"] = local.reshape(16).tolist()
        else:
            for child in children(node):
                self.normalize(child, combined)

    def fixed(self, native):
        points = self.points(native)
        # Match the vanilla block GUI's facing. The former 45 degree yaw showed the pumpkin's back.
        r = rotation([30, 225, 0] if np.ptp(points[:, 2]) > .1 else [0, 0, 0])
        rotated = points @ r.T
        lo, hi = rotated.min(axis=0), rotated.max(axis=0)
        scale = .75 / max(hi[0] - lo[0], hi[1] - lo[1], .001)
        layout = np.eye(4)
        layout[:3, :3] = r * scale
        layout[:3, 3] = [.5 - scale * (lo[0] + hi[0]) / 2, .5 - scale * (lo[1] + hi[1]) / 2, 9 / 16 + 1 / 128 - scale * lo[2]]
        placed = transform(points, layout)
        assert abs(placed[:, 2].min() - (9 / 16 + 1 / 128)) < 1e-6
        assert np.all(placed[:, :2].min(axis=0) >= .125 - 1e-6)
        assert np.all(placed[:, :2].max(axis=0) <= .875 + 1e-6)
        result = copy.deepcopy(native)
        self.normalize(result, layout)
        return composite(result)

    def generate(self):
        gui_icons, held_icons, fixed_icons, volumes = [], [], [], []
        for name in sorted(self.jar.namelist()):
            if not name.startswith("assets/minecraft/items/") or not name.endswith(".json"):
                continue
            item = "minecraft:" + name.removeprefix("assets/minecraft/items/").removesuffix(".json")
            native = json.loads(self.jar.read(name))["model"]
            override = self.overrides.get(item, {})
            volume = override.get("volume", self.volume(native))
            if volume:
                volumes.append(item)
            translation = override.get("gui_translation", [.25, .25, 0] if volume else [.125, .125, 1])
            gui = composite(copy.deepcopy(native), translation, [.75] * 3)
            held = copy.deepcopy(gui)
            if not volume:
                held["transformation"]["translation"][2] = .0625
            fixed = override.get("fixed_model") or self.fixed(native)
            gui_icons.append((item, share_transforms(gui)))
            held_icons.append((item, share_transforms(held)))
            fixed_icons.append((item, share_transforms(fixed)))
        def icons(entries):
            return shared_select(1, entries, dict(entries)["minecraft:knowledge_book"])
        plates = []
        for frame in ("task", "goal", "challenge"):
            plate = {"type": "minecraft:model", "model": f"tasktussle:item/advancement_{frame}"}
            gui = select(1, [(volumes, composite(plate, [0, 0, -2]))], plate)
            held = select(1, [(volumes, composite(plate, [0, 0, -.375]))], plate)
            fixed = composite(plate, [0, 0, -.5], [1, 1, 2])
            plates.append((frame, views(gui, held, fixed)))
        return {"model": {"type": "minecraft:composite", "models": [select(0, plates, dict(plates)["task"]), views(icons(gui_icons), icons(held_icons), icons(fixed_icons))]}}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("client_jar", type=Path)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    with ZipFile(args.client_jar) as jar:
        result = Generator(jar).generate()
    path = ROOT / "resourcepack/assets/tasktussle/items/advancement_icon.json"
    if args.check:
        assert json.loads(path.read_text()) == result, "Regenerate the backplate item catalog"
    else:
        path.write_text(json.dumps(result, indent=2) + "\n")
    print(f"{'Checked' if args.check else 'Generated'} all item selectors and three independent plates.")


if __name__ == "__main__":
    main()
