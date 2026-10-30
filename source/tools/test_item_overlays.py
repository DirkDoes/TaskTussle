import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from zipfile import ZipFile


spec = importlib.util.spec_from_file_location("overlays", Path(__file__).with_name("generate-item-overlays.py"))
overlays = importlib.util.module_from_spec(spec)
spec.loader.exec_module(overlays)


class ItemOverlaysTest(unittest.TestCase):
    def test_native_models_stay_unchanged_and_overlays_are_discovered(self):
        native = {"type": "minecraft:condition", "property": "minecraft:using_item",
                  "on_true": {"type": "minecraft:model", "model": "minecraft:item/bow_pulling_0"},
                  "on_false": {"type": "minecraft:model", "model": "minecraft:item/bow"}}
        with tempfile.TemporaryDirectory() as folder:
            assets = Path(folder)
            textures = assets / "textures/item/tasks/overlays"
            textures.mkdir(parents=True)
            png = (overlays.ASSETS / "textures/item/tasks/overlays/trade_task_overlay.png").read_bytes()
            for index in range(10):
                (textures / f"overlay_{index}.png").write_bytes(png)
            with ZipFile(assets / "client.jar", "w") as jar:
                jar.writestr("assets/minecraft/items/bow.json", json.dumps({"model": native}))
                jar.writestr("assets/minecraft/items/knowledge_book.json", json.dumps({"model": native}))
            with patch.object(overlays, "ASSETS", assets), ZipFile(assets / "client.jar") as jar:
                result = overlays.generate(jar)
            catalog = result["items/item_overlay.json"]
            self.assertTrue(catalog["oversized_in_gui"])
            item, gui = catalog["model"]["models"]
            self.assertEqual(native, item["cases"][0]["model"])
            self.assertEqual(0, item["index"])
            self.assertEqual("gui", gui["cases"][0]["when"])
            self.assertEqual(overlays.EMPTY, gui["fallback"])
            choices = gui["cases"][0]["model"]
            self.assertEqual(1, choices["index"])
            self.assertEqual(10, len(choices["cases"]))
            for case in choices["cases"]:
                self.assertEqual([{"type": "minecraft:constant", "value": 0x010203}], case["model"]["tints"])
            self.assertEqual(overlays.EMPTY, choices["fallback"])
            for name, model in result.items():
                if name.startswith("models/"):
                    self.assertEqual([1.125, 1.125, 1], model["display"]["gui"]["scale"])
                    self.assertGreater(model["display"]["gui"]["translation"][2], 0)
                    face = model["elements"][0]
                    self.assertFalse(face["shade"])
                    self.assertEqual(15, face["light_emission"])
                    self.assertEqual([0, 0, 8.5], face["from"])
                    self.assertEqual([16, 16, 8.5], face["to"])
                    self.assertEqual([0, 0, 16, 16], face["faces"]["south"]["uv"])
                    self.assertEqual(0, face["faces"]["south"]["tintindex"])


if __name__ == "__main__":
    unittest.main()
