"""Regression checks for Indian cotton, wool and acacia furniture supply."""
from pathlib import Path
import unittest

DATA = Path(__file__).resolve().parents[2] / "src/main/resources/todeploy/millenaire"
IND = DATA / "cultures/indian"

def lines(path):
    return [s.strip().lower() for s in path.read_text(encoding="utf-8").splitlines()
            if s.strip() and not s.lstrip().startswith("//")]

class IndianTextileSupply(unittest.TestCase):
    def test_cotton_harvest_has_delivery(self):
        wife = lines(IND / "villagers/indian_peasantwife.txt")
        self.assertEqual(wife.count("goal=harvestcotton"), 1)
        self.assertIn("goal=bringbackresourceshome", wife)
        self.assertIn("bringbackhomegood=cotton", wife)
        self.assertIn("goal=getgoodshousehold", wife)
        self.assertIn("goal=gethousethresources", wife)

    def test_wool_is_exported_after_small_local_reserve(self):
        weaver = lines(IND / "villagers/indian_weaver.txt")
        self.assertIn("goal=makewoolfromcotton", weaver)
        self.assertIn("requiredgood=cotton,32", weaver)
        self.assertIn("requiredgood=wool_white,8", weaver)
        delivery = next(s for s in lines(IND / "shops/townhall.txt") if s.startswith("deliverto="))
        self.assertIn("wool_white", delivery.split("=", 1)[1].split(","))
        recipe = lines(DATA / "goals/genericcrafting/woolcarpets/makewoolfromcotton.txt")
        self.assertIn("input=cotton,4", recipe)
        self.assertIn("output=wool_white,1", recipe)

    def test_autonomous_fort_and_palace_have_early_textile_projects(self):
        for name in ("indian_fort", "indian_palace_autonomous", "indian_agricole"):
            with self.subTest(village=name):
                config = lines(IND / "villages" / (name + ".txt"))
                for building in ("cottonfield", "weaverhouse"):
                    self.assertEqual(config.count("core=" + building), 1)
                    self.assertNotIn("never=" + building, config)
                    self.assertNotIn("secondary=" + building, config)
        field = lines(IND / "buildings/basic/cottonfield_A.txt")
        house = lines(IND / "buildings/basic/weaverhouse_A.txt")
        self.assertIn("initial.villagetag=cottonfield", field)
        self.assertIn("initial.requiredvillagetag=cottonfield", house)
        self.assertIn("initial.priority=6000", field)
        self.assertIn("initial.priority=5900", house)

    def test_charpoy_preserves_acacia_recipe_and_craftsman(self):
        craftsman = lines(IND / "villagers/indian_craftsman.txt")
        self.assertIn("goal=makecharpoybed", craftsman)
        self.assertIn("requiredgood=wood_acacia,20", craftsman)
        recipe = lines(DATA / "goals/genericcrafting/indian/makecharpoybed.txt")
        self.assertEqual([s for s in recipe if s.startswith("input=")], ["input=wood_acacia,2"])
        self.assertIn("output=bed_charpoy,1", recipe)
        self.assertIn("initial.male=indian_craftsman", lines(IND / "buildings/basic/craftsmanhouse_A.txt"))
        self.assertIn("core=craftsmanhouse", lines(IND / "villages/indian_fort.txt"))
        delivery = next(s for s in lines(IND / "shops/townhall.txt") if s.startswith("deliverto="))
        self.assertIn("bed_charpoy", delivery.split("=", 1)[1].split(","))

if __name__ == "__main__":
    unittest.main()
