"""Synthetic regression tests for the offline economy auditor; no game dependencies."""
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from audit_production import audit, matches, reachable

class ProductionAuditTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        root = Path(self.tmp.name)
        self.data, self.java = root / "data", root / "java"
        self.put(self.java, "goal/Goal.java", 'goals.put("bringbackresourceshome", x);\ngoals.put("fish", x);')
        self.put(self.data, "itemlist.txt", "\n".join(
            f"{key};minecraft:{key};0" for key in ("sand", "glass", "pane", "gravel", "fishraw", "fishcooked", "fuel")) +
            "\nwood_any;minecraft:log;-1\nwood_acacia;minecraft:log2;0\nwood;minecraft:log;0\n")
        self.put(self.data, "blocklist.txt", "sandsource;;0;;200/20/20;;;0\n")
        self.put(self.data, "goals/genericmining/minesand.txt", "sourceblockstate=minecraft:sand\nloot=sand,1\n")
        self.put(self.data, "goals/genericcooking/cooksand.txt", "itemtocook=sand\nitemsbalance=sand,glass\n")
        self.put(self.data, "goals/genericcrafting/makepane.txt", "input=glass,1\noutput=pane,1\n")
        self.put(self.data, "goals/generictendfurnace/tend.txt", "minimumfuel=3\n")
        self.put(self.data, "cultures/test/villagers/miner.txt", "goal=MineSand\ngoal=bringBackResourcesHome\nbringBackHomeGood=sand\n")
        self.put(self.data, "cultures/test/villagers/crafter.txt", "goal=cooksand\ngoal=makepane\ngoal=tend\nrequiredGood=fuel,2\n")
        self.put(self.data, "cultures/test/buildings/quarry_A.txt", "initial.male=miner\nupgrade1.female=crafter\n")
        self.put(self.data, "cultures/test/villages/demo.txt", "centre=quarry\n")
        self.put(self.data, "cultures/test/traded_goods.txt", "fuel,2,1,0,0,0,false,,0\n")
        self.put(self.data, "cultures/test/shops/workshop.txt", "deliverto=sand,glass\n")

    @staticmethod
    def put(root, name, content):
        path = root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")

    def run_audit(self):
        return audit(self.data, self.java)

    def test_catalog_graph_and_stage(self):
        result = self.run_audit()
        self.assertEqual(result["summary"]["profiles"], 2)
        self.assertEqual(result["summary"]["cultures"], 1)
        self.assertEqual(result["summary"]["findings"].get("ERROR", 0), 0)
        village = result["cultures"]["test"]["villages"][0]
        self.assertEqual(village["initial_roles"], ["miner"])
        self.assertEqual(village["possible_roles"], ["crafter", "miner"])
        self.assertEqual(village["potential_internal_outputs"], ["glass", "pane", "sand"])
        self.assertIn("fuel", result["cultures"]["test"]["player_buy"])
        self.assertEqual(village["unresolved_recipe_inputs"], [])

    def test_missing_return_and_unknown_goal(self):
        self.put(self.data, "cultures/test/villagers/miner.txt",
                 "goal=minesand\ngoal=bringbackresourceshome\ngoal=not_registered\n")
        findings = self.run_audit()["findings"]
        self.assertTrue(any(f["code"] == "MISSING_HOME_RETURN" and f["level"] == "REVIEW" for f in findings))
        self.assertTrue(any(f["code"] == "UNKNOWN_GOAL" and f["level"] == "ERROR" for f in findings))

    def test_maintenance_partner_and_scope(self):
        self.put(self.data, "cultures/test/villagers/miner.txt", "goal=minesand\ngoal=tend\n")
        self.put(self.data, "cultures/test/villagers/crafter.txt", "goal=cooksand\n")
        findings = self.run_audit()["findings"]
        self.assertFalse(any(f["code"] == "FURNACE_MAINTENANCE_UNCONFIRMED" for f in findings))
        self.put(self.data, "goals/generictendfurnace/tend.txt", "buildingtag=kitchen\n")
        findings = self.run_audit()["findings"]
        self.assertTrue(any(f["code"] == "FURNACE_MAINTENANCE_UNCONFIRMED" for f in findings))

    def test_invalid_quantity(self):
        self.put(self.data, "goals/genericcrafting/makepane.txt", "input=glass,0\noutput=pane,1\n")
        self.assertTrue(any(f["code"] == "INVALID_QUANTITY" for f in self.run_audit()["findings"]))

    def test_legacy_wood_variants(self):
        aliases = {"wood_any": ("minecraft:log", "-1"), "acacia": ("minecraft:log2", "0"),
                   "oak": ("minecraft:log", "0"), "birch": ("minecraft:log", "2")}
        self.assertTrue(matches("wood_any", "acacia", aliases))
        self.assertFalse(matches("oak", "birch", aliases))

    def test_cycle_needs_external_seed(self):
        edges = [dict(role="r", output="a", inputs=["b"]), dict(role="r", output="b", inputs=["a"])]
        self.assertEqual(reachable(edges, {"r"}, {}), set())
        self.assertEqual(reachable(edges, {"r"}, {}, {"a"}), {"a", "b"})

    def test_legacy_encoding_across_config_readers(self):
        paths = [
            self.data / "goals/genericmining/minesand.txt",
            self.data / "cultures/test/villagers/miner.txt",
            self.data / "cultures/test/traded_goods.txt",
            self.data / "itemlist.txt",
            self.data / "blocklist.txt",
        ]
        original = {}
        for path in paths:
            raw = b"// ancien r\xf4le et co\xfbts\n" + path.read_bytes()
            path.write_bytes(raw)
            original[path] = raw
        result = audit(self.data, self.java, True)
        self.assertEqual(result["summary"]["profiles"], 2)
        self.assertEqual(result["summary"]["findings"].get("ERROR", 0), 0)
        for path, raw in original.items():
            self.assertEqual(path.read_bytes(), raw)

    def test_latin1_fallback_and_utf8_bom(self):
        from audit_production import read_legacy_text
        path = self.data / "encoding.txt"
        path.write_bytes(b"// \x81\xf4\nloot=sand,1\n")
        self.assertIn("loot=sand,1", read_legacy_text(path))
        path.write_bytes(b"\xef\xbb\xbfloot=sand,1\n")
        self.assertEqual(read_legacy_text(path), "loot=sand,1\n")

    def test_drops_require_collection_and_are_inferred(self):
        self.put(self.data, "itemlist.txt", (self.data / "itemlist.txt").read_text() +
                 "beefraw;minecraft:beef;0\nleather;minecraft:leather;0\n")
        self.put(self.data, "goals/genericslaughteranimal/cow.txt",
                 "animalkey=minecraft:cow\n")
        self.put(self.data, "cultures/test/villagers/butcher.txt",
                 "goal=cow\ngoal=bringbackresourceshome\ncollectGood=beefraw\nbringBackHomeGood=beefraw\n")
        result = self.run_audit()
        edges = [e for e in result["edges"] if e["role"] == "butcher"]
        self.assertEqual([e["output"] for e in edges], ["beefraw"])
        self.assertTrue(edges[0]["inferred"])
        self.assertEqual(edges[0]["collection"], "potential_drop")
        self.assertEqual(result["goal_catalog"]["cow"]["possible_entity_drops"], ["beefraw", "leather"])

    def test_planting_seed_retention_not_missing_return(self):
        self.put(self.data, "goals/genericplanting/plant.txt", "seed=sand\n")
        self.put(self.data, "cultures/test/villagers/miner.txt",
                 "goal=minesand\ngoal=plant\ngoal=bringbackresourceshome\n")
        result = self.run_audit()
        self.assertFalse(any(f["code"] == "MISSING_HOME_RETURN" and "miner.txt" in f["file"]
                             for f in result["findings"]))
        edge = next(e for e in result["edges"] if e["role"] == "miner" and e["output"] == "sand")
        self.assertTrue(edge["retained_planting_input"])

    def test_png_optional_source_evidence(self):
        try:
            from PIL import Image
        except ImportError:
            self.skipTest("Pillow not installed")
        path = self.data / "cultures/test/buildings/quarry_A0.png"
        Image.new("RGBA", (1,1), (200,20,20,255)).save(path)
        result = audit(self.data, self.java, True)
        self.assertTrue(result["cultures"]["test"]["png_scanned"])
        self.assertFalse(any(f["code"] == "NO_HOME_SOURCE_MARKER_SEEN" for f in result["findings"]))
        for alpha in (0, 128):
            Image.new("RGBA", (1,1), (200,20,20,alpha)).save(path)
            result = audit(self.data, self.java, True)
            self.assertTrue(any(f["code"] == "NO_HOME_SOURCE_MARKER_SEEN" for f in result["findings"]))

if __name__ == "__main__":
    unittest.main(verbosity=2)
