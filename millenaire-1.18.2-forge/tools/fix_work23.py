from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');base=r/'src/main/java/org/millenaire/common';bk=r/'migration/before-work23';bk.mkdir(exist_ok=False)
def edit(n,fn):
 p=base/n;q=bk/n;q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,q);p.write_text(fn(p.read_text()),encoding='utf-8')
def planting(s):
 old='if (dest == null || villager.getGoalDestPoint() == null)';assert s.count(old)==2
 return s.replace(old,'if (dest == null)',1)
edit('goal/generic/GoalGenericPlantCrop.java',planting)
def breed(s):
 assert 'return 5;' in s;s=s.replace('return 5;','return 3;')
 # Action search is 4 horizontal and 2 vertical; arrival must be inside both.
 marker='    @Override\n    public int range(MillVillager villager)'
 s=s.replace(marker,'''    @Override
    public boolean isWithinActionRange(MillVillager villager, org.millenaire.common.utilities.Point destination) {
        return destination != null && destination.distanceTo(villager) < this.range(villager)
            && Math.abs(destination.getdY() - villager.getY()) < 2.0;
    }

'''+marker)
 return s
edit('goal/GoalBreedAnimals.java',breed)
(base/'pathing/BarrierPathRules.java').write_text('''package org.millenaire.common.pathing;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.common.block.BlockMillWall;

/** Solid barriers must not be treated as a walk-through node or a one-block step. */
public final class BarrierPathRules {
    private BarrierPathRules() {}
    public static boolean isBarrier(BlockState state) {
        Block block = state.getBlock();
        return block instanceof FenceBlock || block instanceof WallBlock
            || block instanceof BlockMillWall || block instanceof IronBarsBlock;
    }
    public static boolean blocksStepAbove(BlockState state) {
        return isBarrier(state) || state.getBlock() instanceof FenceGateBlock && !state.getValue(FenceGateBlock.OPEN);
    }
}
''',encoding='utf-8')
def evaluator(s):
 s=s.replace('        return super.getBlockPathType', '''        if (BarrierPathRules.isBarrier(world.getBlockState(pos))
                || y > world.getMinBuildHeight() && BarrierPathRules.blocksStepAbove(world.getBlockState(pos.below())))
            return BlockPathTypes.BLOCKED;
        return super.getBlockPathType''');return s
edit('pathing/VillagerWalkNodeEvaluator.java',evaluator)
def astar(s):
 marker='        if (org.millenaire.common.pathing.ChestPathRules.isChest(block)'
 s=s.replace(marker,'''        if (org.millenaire.common.pathing.BarrierPathRules.isBarrier(blockState)
                || iy > world.getMinBuildHeight() && org.millenaire.common.pathing.BarrierPathRules.blocksStepAbove(
                    ThreadSafeUtilities.getBlockState(world, ix, iy - 1, iz))) return false;
'''+marker);return s
edit('pathing/atomicstryker/AStarStatic.java',astar)
print('Plant goal can start without prior target; breeding arrival inside action area; hard fence/wall nodes in both path planners')
