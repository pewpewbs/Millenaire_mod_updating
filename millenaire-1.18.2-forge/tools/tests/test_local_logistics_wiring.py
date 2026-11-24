"""Static regression checks for the shared local stock-transfer paths."""
from pathlib import Path
import unittest

JAVA = Path(__file__).resolve().parents[2] / "src/main/java/org/millenaire/common"

def read(name):
    return (JAVA / name).read_text(encoding="utf-8")

class LocalLogisticsWiring(unittest.TestCase):
    def test_partial_transfer_preserves_cargo_and_variant(self):
        entity = read("entity/MillVillager.java")
        deposit = entity.split("public int putInBuilding(",1)[1].split("public void readAdditionalSaveData",1)[0]
        self.assertIn("int stored = building.storeGoods(item, meta, taken)",deposit)
        self.assertIn("if (stored < taken) this.addToInv(item, meta, taken - stored)",deposit)
        self.assertIn("return stored;",deposit)
        self.assertIn("!requested.matches(actual)",deposit)
        self.assertIn("actual.getItem(), actual.meta, nb - stored",deposit)
        pickup = entity.split("public int takeFromBuilding(",1)[1].split("public int takeFromInv(",1)[0]
        self.assertIn("building.stockKeys()",pickup)
        self.assertIn("actual.getItem(), actual.meta, nb - total",pickup)
        self.assertIn("if (nb > 0) this.addToInv",pickup)

    def test_storage_and_exact_output_metadata(self):
        building = read("village/Building.java")
        self.assertIn("public int storageRoom(InvItem item)",building)
        self.assertIn("if (!this.world.hasChunkAt(p.getBlockPos())) continue;",building)
        self.assertIn("getItemsFromFurnace(furnace, item, meta, toTake - taken)",building)
        self.assertIn("getItemsFromFirePit(firepit, item, meta, toTake - taken)",building)
        utils = read("utilities/MillCommonUtilities.java")
        self.assertIn("Math.max(0, Math.min(chest.getMaxStackSize(), stack.getMaxStackSize()) - stack.getCount())",utils)
        self.assertIn("if (amount <= 0) continue;",utils)

    def test_household_deficit_and_reserved_supply(self):
        helper = read("goal/LocalLogistics.java")
        self.assertIn("target - home.countGoods(item)",helper)
        self.assertIn("target / 2 + target % 2",helper)
        self.assertIn("deficit(home,item,target) - carried",helper)
        self.assertIn("source.nbGoodAvailable(item,false,false,false)",helper)
        self.assertIn("source == home",helper)
        pickup = read("goal/GoalGetGoodsForHousehold.java")
        self.assertIn("if (source == home) return true;",pickup)
        self.assertIn("LocalLogistics.householdPickup",pickup)
        self.assertIn("LocalLogistics.householdDelivery",pickup)
        self.assertIn("LocalLogistics.householdDelivery",read("goal/GoalDeliverGoodsHousehold.java"))

    def test_shop_capacity_and_no_self_supply(self):
        helper = read("goal/LocalLogistics.java")
        self.assertIn("shop == source",helper)
        self.assertIn("source.nbGoodAvailable(concrete,false,false,true)",helper)
        self.assertIn("capacity-carried",helper)
        self.assertIn("shop.storageRoom(concrete)",helper)
        pickup = read("goal/GoalGetResourcesForShops.java")
        deliver = read("goal/GoalDeliverResourcesShop.java")
        self.assertIn("LocalLogistics.shopPickup",pickup)
        self.assertIn("LocalLogistics.shopDelivery",deliver)
        self.assertIn("if (amount > bestAmount)",deliver)

    def test_physical_access_remains_required(self):
        helper = read("goal/LocalLogistics.java")
        self.assertIn("getSellingPos().equals(v.getGoalDestPoint())",helper)
        self.assertIn("goal.isWithinActionRange(v,v.getGoalDestPoint())",helper)
        for name in ("GoalBringBackResourcesHome","GoalDeliverGoodsHousehold","GoalGetGoodsForHousehold","GoalDeliverResourcesShop","GoalGetResourcesForShops"):
            self.assertIn("LocalLogistics.access",read("goal/"+name+".java"),name)

if __name__ == "__main__":
    unittest.main(verbosity=2)
