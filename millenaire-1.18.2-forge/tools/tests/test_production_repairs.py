"""Regression checks for fixes following the first full production audit."""
from pathlib import Path
import unittest
import sys

PROJECT = Path(__file__).resolve().parents[2]
DATA = PROJECT / "src/main/resources/todeploy/millenaire"
sys.path.insert(0, str(PROJECT / "tools"))
from audit_production import load_goals, read_config, markers, DEFAULT_JAVA


class ProductionRepairs(unittest.TestCase):
    def test_unique_global_goal_ids(self):
        goals, duplicates = load_goals(DATA, DEFAULT_JAVA)
        self.assertEqual(duplicates, [])
        for key in ("makebonemeal", "makesandstone", "makesandstoneseljuk",
                    "makesugar", "makesugarseljuk"):
            self.assertIn(key, goals)
        self.assertEqual(goals["makesandstone"].cfg.last("input"), "sand,40")
        self.assertEqual(goals["makesandstoneseljuk"].cfg.last("input"), "sand,1")
        self.assertEqual(goals["makesandstoneseljuk"].cfg.last("buildingtag"), "stonecrafter")
        self.assertEqual(goals["makesugarseljuk"].cfg.last("buildingtag"), "kitchen")
        self.assertEqual(goals["makesugar"].cfg.last("buildingtag"), "")

    def test_seljuk_role_references(self):
        roles = DATA / "cultures/seljuk/villagers/male"
        for path in ("lone/turk_loneminer.txt", "turk_stonecrafter.txt"):
            self.assertIn("makesandstoneseljuk", read_config(roles / path).values("goal"))
        self.assertIn("makesugarseljuk", read_config(roles / "turk_cook.txt").values("goal"))

    def test_smelting_uses_real_input(self):
        for name, item, output in (("cookclay", "clay", "brick"),
                                   ("cooksandbyz", "sand", "glass")):
            config = read_config(DATA / "goals/genericcooking/byzantinerefresh" / (name + ".txt"))
            self.assertEqual(config.last("itemtocook"), item)
            self.assertEqual(config.last("itemsbalance"), item + "," + output)

    def test_quarry_sources_at_supported_existing_points(self):
        try:
            from PIL import Image
        except ImportError:
            self.skipTest("Pillow not installed")
        palette = markers(DATA)
        plans = [
            ("indian/buildings/basic/quarry_A0.png", 14, [(1,2,5),(1,6,6)], (200,20,20), (155,155,155)),
            ("indian/buildings/basic/quarry_B0.png", 14, [(2,3,3),(3,9,8)], (200,20,20), (155,155,155)),
            ("japanese/buildings/lone/lonemine_A0.png", 20, [(7,4,2),(7,2,4)], (60,60,60), (109,71,35)),
            ("mayan/buildings/core/mayanmining_A0.png", 12, [(2,4,5),(2,3,9)], (200,20,20), (155,155,155)),
        ]
        for path, width, points, color, support in plans:
            expected_marker = "gravelsource" if path.startswith("japanese/") else "sandsource"
            self.assertEqual(palette.get(color), expected_marker, path)
            with Image.open(DATA / "cultures" / path) as raw:
                image = raw.convert("RGB")
                for floor, x, y in points:
                    self.assertEqual(image.getpixel((floor*(width+1)+x,y)), color, path)
                    self.assertEqual(image.getpixel(((floor-1)*(width+1)+x,y)), support, path)
                self.assertEqual(palette.get((180,20,20)), "stonesource")
                self.assertGreater(sum(n for n, p in image.getcolors(image.width * image.height)
                                       if p == (180,20,20)), 0)

    def test_lone_fisherman_delivery_and_log_variants(self):
        root = DATA / "cultures"
        fisher = read_config(root / "inuits/villagers/inuit_lonefisherman.txt")
        self.assertIn("bringbackresourceshome", fisher.values("goal"))
        self.assertTrue({"fishraw", "boneblock"}.issubset(fisher.values("bringbackhomegood")))
        logger = read_config(root / "seljuk/villagers/male/turk_lumberman.txt")
        self.assertIn("wood_any", logger.values("bringbackhomegood"))
        for name in ("inuit_lumberman", "inuit_lonelumberman"):
            profile = read_config(root / "inuits/villagers" / (name + ".txt"))
            self.assertTrue({"wood_acacia","wood_darkoak"}.issubset(profile.values("bringbackhomegood")))


if __name__ == "__main__":
    unittest.main(verbosity=2)
