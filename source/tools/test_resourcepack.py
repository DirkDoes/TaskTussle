import importlib.util
import json
from pathlib import Path
import tempfile
import unittest


spec = importlib.util.spec_from_file_location("resourcepack", Path(__file__).with_name("check-resourcepack.py"))
resourcepack = importlib.util.module_from_spec(spec)
spec.loader.exec_module(resourcepack)


class ResourcePackReferencesTest(unittest.TestCase):
    def test_pack_references_resolve(self):
        errors, checked = resourcepack.validate(resourcepack.ROOT / "resourcepack")
        self.assertEqual([], errors)
        self.assertGreater(checked, 100)

    def test_reports_missing_and_wrong_case_targets_across_asset_types(self):
        with tempfile.TemporaryDirectory() as folder:
            pack = Path(folder)
            assets = {
                "tasktussle/items/cog.json": {"model": {"type": "minecraft:model", "model": "tasktussle:item/cog"}},
                "tasktussle/models/item/cog.json": {"parent": "minecraft:item/generated", "textures": {"layer0": "tasktussle:item/cog"}},
                "tasktussle/font/menu.json": {"providers": [{"type": "bitmap", "file": "tasktussle:gui/missing.png"}]},
                "minecraft/particles/totem_of_undying.json": {"textures": ["tasktussle:missing"]},
                "minecraft/sounds.json": {"item.totem.use": {"sounds": ["tasktussle:effects/missing"]}},
            }
            for name, data in assets.items():
                path = pack / "assets" / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(json.dumps(data))
            wrong_case = pack / "assets/tasktussle/textures/item/Cog.png"
            wrong_case.parent.mkdir(parents=True)
            wrong_case.touch()
            errors, checked = resourcepack.validate(pack)
            self.assertEqual(5, checked)
            self.assertEqual(4, len(errors))
            self.assertTrue(any("textures/item/cog.png" in error for error in errors))
            self.assertTrue(any("textures/gui/missing.png" in error for error in errors))
            self.assertTrue(any("textures/particle/missing.png" in error for error in errors))
            self.assertTrue(any("sounds/effects/missing.ogg" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
