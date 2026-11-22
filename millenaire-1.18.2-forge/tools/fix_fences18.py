from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge')
p=r/'src/main/java/org/millenaire/common/utilities/WorldUtilities.java'
b=r/'migration/before-water-labels18'/p.relative_to(r)
b.parent.mkdir(parents=True,exist_ok=True);assert not b.exists();shutil.copy2(p,b)
s=p.read_text(encoding='utf-8')
anchor='''        BlockState previous=world.getBlockState(p.getBlockPos());
        boolean changed=world.setBlock(p.getBlockPos(),bs,b?3:2);'''
assert s.count(anchor)==1
s=s.replace(anchor,'''        // Programmatic placement bypasses FenceBlock.getStateForPlacement.
        // Initialize this fence's connections as well as notifying its neighbours.
        if (bs.getBlock() instanceof net.minecraft.world.level.block.FenceBlock) {
            bs = Block.updateFromNeighbourShapes(bs, world, p.getBlockPos());
        }
'''+anchor)
p.write_text(s,encoding='utf-8')
print('Fence connections now calculated at placement')
