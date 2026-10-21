import copy
import importlib.util
from pathlib import Path
import unittest


spec = importlib.util.spec_from_file_location("backplates", Path(__file__).with_name("generate-backplates.py"))
backplates = importlib.util.module_from_spec(spec)
spec.loader.exec_module(backplates)


class SharedTransformsTest(unittest.TestCase):
    def test_common_transform_moves_around_selector_but_exception_stays(self):
        diamond = backplates.composite({"type": "minecraft:model", "model": "minecraft:item/diamond"}, [0, 0, 1], [.75] * 3)
        emerald = copy.deepcopy(diamond)
        emerald["models"][0]["model"] = "minecraft:item/emerald"
        exception = backplates.composite({"type": "minecraft:model", "model": "minecraft:item/decorated_pot"}, [0, 1, 0], [.75] * 3)
        entries = [("diamond", diamond), ("emerald", emerald), ("pot", exception)]
        original = copy.deepcopy(entries)
        result = backplates.shared_select(1, entries, diamond)
        common, special = result["cases"]
        self.assertEqual(["diamond", "emerald"], common["when"])
        self.assertEqual(diamond["transformation"], common["model"]["transformation"])
        self.assertTrue(all("transformation" not in case["model"] for case in common["model"]["cases"]))
        self.assertEqual(exception, special["model"])
        self.assertEqual(diamond, result["fallback"])
        self.assertEqual(original, entries)

    def test_equal_branch_transforms_are_hoisted(self):
        leaf = {"type": "minecraft:model", "model": "minecraft:item/diamond", "transformation": {"scale": [.75] * 3}}
        node = backplates.select(1, [("diamond", copy.deepcopy(leaf))], copy.deepcopy(leaf))
        result = backplates.share_transforms(backplates.composite(node))
        self.assertEqual("minecraft:select", result["type"])
        self.assertEqual(leaf["transformation"], result["transformation"])
        self.assertTrue(all("transformation" not in child for child in backplates.children(result)))


if __name__ == "__main__":
    unittest.main()
