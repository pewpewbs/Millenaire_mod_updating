from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2] / "src/main"
JAVA = ROOT / "java/org/millenaire/common/goal/generic"
DATA = ROOT / "resources/todeploy/millenaire"

class PhysicalVillageStockTests(unittest.TestCase):
    def test_white_paint_is_low_priority_and_hard_capped(self):
        cfg = (DATA / "goals/genericcrafting/indian/makepaintedbucketwhite.txt").read_text(encoding="utf-8")
        self.assertIn("priority=20", cfg)
        self.assertIn("physicalvillagelimit=paintbucketwhite,4", cfg)
        self.assertIn("townhalllimit=paintbucketwhite,4", cfg)
        self.assertIn("input=wood_acacia,1", cfg)
        self.assertIn("output=paintbucketwhite,1", cfg)

    def test_opt_in_cap_is_checked_before_crafting_transaction(self):
        source = (JAVA / "GoalGenericCrafting.java").read_text(encoding="utf-8")
        self.assertIn('paramName="physicalvillagelimit"', source)
        self.assertIn("PhysicalVillageStock.hasRoom(villager, item, cap.getValue(), produced)", source)
        action = source.split("public boolean performAction(", 1)[1]
        self.assertLess(action.index("passesStockLimits"), action.index("beginGoodsTransaction"))

    def test_counts_physical_items_not_available_trade_stock(self):
        src = (JAVA / "PhysicalVillageStock.java").read_text(encoding="utf-8")
        for guard in ("townHall.buildings", "buildings.add(townHall.getPos())", "chests.add(point)",
                      "hasChunkAt(point.getBlockPos())", "stack.is(item)", "limit - produced",
                      "remaining < 0", "getKnownVillagers()", "residents.add(worker)",
                      "getVillagerRecords()", "counted.add(record.getVillagerId())",
                      "record.inventory.entrySet()", "key.getItem() != item"):
            self.assertIn(guard, src)
        self.assertLess(src.index("hasChunkAt"), src.index("getMillChest"))
        for forbidden in ("nbGoodAvailable", "takeGoods(", "takeFromInv(", "getChunk("):
            self.assertNotIn(forbidden, src)

if __name__ == "__main__":
    unittest.main()
