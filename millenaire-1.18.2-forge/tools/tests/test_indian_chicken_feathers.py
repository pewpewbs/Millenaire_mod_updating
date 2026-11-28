from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2] / "src/main/java/org/millenaire/common"

class IndianChickenFeathersTests(unittest.TestCase):
    def test_only_plain_indian_npc_chicken_drops_are_removed(self):
        source = (ROOT / "forge/MillEventController.java").read_text(encoding="utf-8")
        handler = source.split("public void discardUnusedIndianChickenFeathers(", 1)[1].split(
            "public void addInuitDrops(", 1)[0]
        for guard in ("level.isClientSide", "instanceof net.minecraft.world.entity.animal.Chicken",
                      "event.getSource().getEntity() instanceof MillVillager", '"indian".equals(culture.key)',
                      "IndianChickenDropPolicy.canDiscardFeathers(culture)", "Items.FEATHER",
                      "!drop.getItem().hasTag()", "event.getDrops().removeIf"):
            self.assertIn(guard, handler)
        self.assertNotIn("takeGoods(", handler)
        self.assertNotIn("takeFromInv(", handler)

    def test_configured_uses_prevent_deletion(self):
        policy = (ROOT / "goal/IndianChickenDropPolicy.java").read_text(encoding="utf-8")
        for demand in ("getTradeGood", "shopNeeds", "requiredGoods", "requiredFoodAndGoods",
                       "itemsNeeded", "foreignMerchantStock", "foodsGrowth", "foodsConception",
                       "crafting.input", "cooking.itemToCook", "planting.seed", "transfer.collectGoods",
                       "plan.resCost", "step.requiredGood"):
            self.assertIn(demand, policy)
        self.assertNotIn("type.collectGoods", policy)
        self.assertNotIn("type.bringBackHomeGoods", policy)
        self.assertIn("Quest.quests.isEmpty()", policy)

if __name__ == "__main__":
    unittest.main()
