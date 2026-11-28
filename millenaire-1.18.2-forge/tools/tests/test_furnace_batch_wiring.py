from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2] / "src/main/java/org/millenaire/common/goal/generic"

class FurnaceBatchWiringTests(unittest.TestCase):
    def test_both_goals_share_output_batch_and_input_fuel_limits(self):
        cook = (ROOT / "GoalGenericCooking.java").read_text(encoding="utf-8")
        tend = (ROOT / "GoalGenericTendFurnace.java").read_text(encoding="utf-8")
        for code in (cook, tend):
            self.assertIn("FurnaceBatchAccess.furnaceOutputDue", code)
            self.assertIn("FurnaceBatchAccess.firepitOutputDue", code)
        self.assertIn("FurnaceBatchPolicy.inputDue", cook)
        self.assertIn("FurnaceBatchPolicy.fuelDue", tend)
        self.assertIn("tending.serviceFuelAt(villager, dest, target, tileEntity)", cook)
        self.assertIn("cooking.serviceInputAt(villager, destination, point, entity)", tend)
        self.assertIn("if (!inputSlotDue(current, slotLimit)) return current;", cook)
        self.assertIn("if (available < minimumBatch(slotLimit)) return current;", cook)

    def test_final_partial_or_blocked_output_is_recoverable(self):
        access = (ROOT / "FurnaceBatchAccess.java").read_text(encoding="utf-8")
        self.assertIn("input.isEmpty()", access)
        self.assertIn("TileEntityFirePit.fuelTime(fuel) <= 0", access)
        self.assertIn("next.getCount() + output.getCount() > capacity", access)
        self.assertIn("!ItemStack.isSameItemSameTags(next, output)", access)

if __name__ == "__main__":
    unittest.main()
