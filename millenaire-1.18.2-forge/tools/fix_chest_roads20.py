from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');base=r/'src/main/java/org/millenaire/common';backup=r/'migration/before-chest-roads20';backup.mkdir(exist_ok=False)
def edit(rel,fn):
 p=base/rel;d=backup/p.relative_to(r);d.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,d);p.write_text(fn(p.read_text(encoding='utf-8')),encoding='utf-8')
for name in ['BlockPath.java','BlockPathSlab.java']:
 def light(s):
  old='    @Override public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) { return 15; }\n'
  assert old in s
  return s.replace(old,'    // Use the vanilla light attenuation for the actual partial-block shape.\n')
 edit('block/'+name,light)
p=base/'pathing/ChestPathRules.java';assert not p.exists()
p.write_text('''package org.millenaire.common.pathing;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import org.millenaire.common.block.BlockLockedChest;

public final class ChestPathRules {
    private ChestPathRules() {}
    public static boolean isChest(Block block) {
        return block instanceof ChestBlock || block instanceof EnderChestBlock
                || block instanceof BlockLockedChest;
    }
}
''',encoding='utf-8')
p=base/'pathing/VillagerWalkNodeEvaluator.java';assert not p.exists()
p.write_text('''package org.millenaire.common.pathing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public final class VillagerWalkNodeEvaluator extends WalkNodeEvaluator {
    @Override
    public BlockPathTypes getBlockPathType(BlockGetter world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        // Neither walk through a partial chest shape nor use its lid as a step.
        if (ChestPathRules.isChest(world.getBlockState(pos).getBlock())
                || y > world.getMinBuildHeight()
                && ChestPathRules.isChest(world.getBlockState(pos.below()).getBlock())) {
            return BlockPathTypes.BLOCKED;
        }
        return super.getBlockPathType(world, x, y, z);
    }
}
''',encoding='utf-8')
def nav(s):
 a='    @Override public boolean moveTo(Path requested, double speed) {';assert a in s
 return s.replace(a,'''    @Override protected net.minecraft.world.level.pathfinder.PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new org.millenaire.common.pathing.VillagerWalkNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new net.minecraft.world.level.pathfinder.PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }
'''+a)
edit('entity/PathNavigateSimple.java',nav)
def astar(s):
 a='''        Block block = blockState.getBlock();
        if (iy > world.getMinBuildHeight()'''
 assert s.count(a)==1
 return s.replace(a,'''        Block block = blockState.getBlock();
        if (org.millenaire.common.pathing.ChestPathRules.isChest(block)
                || iy > world.getMinBuildHeight()
                && org.millenaire.common.pathing.ChestPathRules.isChest(
                    ThreadSafeUtilities.getBlock(world, ix, iy - 1, iz))) return false;
        if (iy > world.getMinBuildHeight()''')
edit('pathing/atomicstryker/AStarStatic.java',astar)
print('Both pathfinders avoid chests and chest tops; roads use shape-based vanilla light attenuation')
