"""Static regression checks; actual container scenarios are in MerchantStockTransferSmoke.java."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/org/millenaire/common"

def read(path):
    return (JAVA/path).read_text(encoding="utf-8")

class MerchantProductionWiring(unittest.TestCase):
    def test_plan_before_writes_and_rollback(self):
        code=read("utilities/MerchantStockTransfer.java")
        self.assertIn("rightPlan=state(right,exchange)",code)
        self.assertIn("leftPlan=state(left,true)",code)
        self.assertLess(code.index("Result.blocked(\"SOURCE_CAPACITY\")"),code.index("write(left,leftPlan)"))
        self.assertIn("ItemStack.isSameItemSameTags(existing,cargo)",code)
        self.assertIn("if (stores.contains(slot.chest()))",code)
        self.assertIn("if (!unchanged(left) || !unchanged(right))",code)
        self.assertIn("new GoodsTransaction",code)
        self.assertLess(code.index("write(right,rightPlan)"),code.index("tx.commit()"))

    def test_migration_and_accounting_after_stock_transfer(self):
        building=read("village/Building.java")
        for name,mode in (("moveMerchant","false"),("swapMerchants","true")):
            body=building.split("private void "+name+"(",1)[1]
            self.assertLess(body.index("if (!transferMerchantStock(destInn, "+mode+")) return;"),
                            body.index("this.transferVillagerPermanently"))
        gate=building.split("private boolean transferMerchantStock(",1)[1].split("private void moveMerchant(",1)[0]
        self.assertLess(gate.index("if (!result.success())"),gate.index("this.addToExports"))
        self.assertIn("result.outward().entrySet()",gate)
        self.assertIn("result.inward().entrySet()",gate)

    def test_local_merchant_capacity(self):
        inn=read("goal/GoalMerchantVisitInn.java")
        building=read("goal/GoalMerchantVisitBuilding.java")
        self.assertIn("storageRoom(invItem)",inn)
        self.assertIn("storageRoom(good.item)",building)
        for src in (inn,building):
            self.assertIn("LocalLogistics.access",src)
            self.assertIn("WorkAccess.canAccessStock",src)

    def test_live_mining_source_at_selection_and_action(self):
        code=read("goal/generic/GoalGenericMining.java")
        selection=code.split("public Goal.GoalInformation getDestination(",1)[1].split("public ItemStack[] getHeldItemsTravelling",1)[0]
        self.assertIn("if (possibleDest == null || !this.validateDest(villager,possibleDest)) continue;",selection)
        self.assertIn("if (this.sourceBlockState != types.get(index)) continue;",selection)
        self.assertIn("if (!liveSource(villager,point)) continue;",selection)
        self.assertIn("NO_REGISTERED_MATCHING_LIVE_SOURCE",code)
        live=code.split("private boolean liveSource(",1)[1].split("@Override",1)[0]
        self.assertIn("villager.level.hasChunkAt(point.getBlockPos())",live)
        self.assertIn("WorldUtilities.getBlockState(villager.level,point) == this.sourceBlockState",live)
        target=code.split("private boolean validTarget(",1)[1].split("@Override",1)[0]
        self.assertIn("!liveSource(villager, point)",target)
        self.assertIn("!getBuildings(villager).contains(destination)",target)
        self.assertIn("types.get(index)==this.sourceBlockState && sources.get(index).contains(point)",target)
        self.assertIn("return false;",target)
        action=code.split("public boolean performAction(",1)[1].split("public boolean stuckAction",1)[0]
        for guard in ("!validTarget(villager,destination,point)", "!this.passesStockLimits(villager)",
                      "!this.validateDest(villager,destination)", "!this.isWithinActionRange(villager,point)"):
            self.assertLess(action.index(guard),action.index("villager.addToInv"))
        stuck=code.split("public boolean stuckAction(",1)[1].split("}",1)[0]
        self.assertNotIn("performAction",stuck)
        self.assertIn("return false;",stuck)

    def test_lone_miner_can_deposit_gravel(self):
        path=ROOT/"src/main/resources/todeploy/millenaire/cultures/japanese/villagers/japanese_loneminer.txt"
        lines=path.read_text(encoding="utf-8-sig").lower().splitlines()
        self.assertIn("goal=minegravel",lines)
        self.assertIn("bringbackhomegood=gravel",lines)

if __name__=="__main__":
    unittest.main(verbosity=2)
