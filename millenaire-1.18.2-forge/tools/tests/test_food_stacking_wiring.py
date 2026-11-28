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
        self.assertIn("MULTI_SERVING_STACK_LIMIT = 16", self.src)
        limit = self.src.split("public int getItemStackLimit(ItemStack stack)", 1)[1].split("@Override", 1)[0]
        self.assertIn("if (stack.getDamageValue() > 0) return 1;", limit)
        self.assertIn("drink || stack.getMaxDamage() > DAMAGE_PER_PORTION", limit)
        self.assertIn("? MULTI_SERVING_STACK_LIMIT : FULL_STACK_LIMIT", limit)
        self.assertNotIn("setCount(", limit)
        self.assertNotIn("shrink(", limit)
        self.assertIn("properties.durability(durability)", self.src)

    def test_existing_chest_stacks_are_loaded_and_transferred_without_truncation(self):
        common = JAVA.parent
        chest = (common / "entity/TileEntityLockedChest.java").read_text(encoding="utf-8")
        self.assertIn("ContainerHelper.loadAllItems((CompoundTag)compound, this.chestContents)", chest)
        self.assertIn("ContainerHelper.saveAllItems((CompoundTag)compound, this.chestContents)", chest)
        put = (common / "utilities/MillCommonUtilities.java").read_text(encoding="utf-8").split(
            "public static int putItemsInChest(Container chest, Item item, int meta, int toPut)", 1
        )[1].split("public static int randomInt", 1)[0]
        self.assertIn("Math.max(0, Math.min(chest.getMaxStackSize(), stack.getMaxStackSize()) - stack.getCount())", put)
        self.assertIn("if (amount <= 0) continue;", put)
        self.assertIn("Math.min(toPut - nb, Math.min(chest.getMaxStackSize(), stack.getMaxStackSize()))", put)
        take = (common / "utilities/WorldUtilities.java").read_text(encoding="utf-8").split(
            "public static int getItemsFromChest(Container chest, Item item, int meta, int toTake)", 1
        )[1].split("public static int getItemsFromFirePit", 1)[0]
        self.assertIn("nb += stack.getCount()", take)
        self.assertIn("chest.removeItem(i, toTake - nb)", take)

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
