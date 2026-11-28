"""Source-level guards for stack-sensitive multi-serving food; live Forge tests are separate."""
from pathlib import Path
import unittest

JAVA = Path(__file__).resolve().parents[2] / "src/main/java/org/millenaire/common/item"

class FoodStackingWiring(unittest.TestCase):
    def setUp(self):
        self.src = (JAVA / "ItemFoodMultiple.java").read_text(encoding="utf-8")

    def test_stack_limit_preserves_old_portion_metadata(self):
        self.assertIn("FULL_STACK_LIMIT = 64", self.src)
        self.assertIn("DAMAGE_PER_PORTION = 64", self.src)
        self.assertIn("public int getItemStackLimit(ItemStack stack)", self.src)
        self.assertIn("stack.getDamageValue() > 0 ? 1 : FULL_STACK_LIMIT", self.src)
        self.assertIn("properties.durability(durability)", self.src)

    def test_open_only_one_item_and_preserve_unopened_remainder(self):
        action = self.src.split("public ItemStack finishUsingItem(", 1)[1]
        self.assertIn("if (level.isClientSide || stack.isEmpty()) return stack;", action)
        self.assertIn("stack.getDamageValue() + DAMAGE_PER_PORTION", action)
        self.assertIn("if (nextDamage < stack.getMaxDamage())", action)
        split = action.index("stack.split(stack.getCount() - 1)")
        damage = action.index("stack.setDamageValue(nextDamage)")
        insert = action.index("player.getInventory().add(remainder)")
        self.assertLess(split, damage)
        self.assertLess(damage, insert)
        self.assertIn("if (!remainder.isEmpty()) player.drop(remainder, false);", action)
        self.assertIn("entity.spawnAtLocation(remainder);", action)
        self.assertIn("stack.shrink(1);", action)

if __name__ == "__main__":
    unittest.main(verbosity=2)
