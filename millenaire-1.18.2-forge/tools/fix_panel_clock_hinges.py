from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');src=r/'src/main/java/org/millenaire/common'
def edit(rel,fn):
 p=src/rel;b=r/'migration/before-panel-clock-hinges'/rel;b.parent.mkdir(parents=True,exist_ok=True)
 if not b.exists():shutil.copy2(p,b)
 p.write_text(fn(p.read_text()),encoding='utf-8')
def panels(s):
 s=s.replace('public long lastSignUpdate = 0L;', 'public long lastSignUpdate = -100L;')
 lines=s.splitlines()
 for i,line in enumerate(lines):
  if 'lastSignUpdate' in line:lines[i]=line.replace('.getDayTime()', '.getGameTime()')
 return '\n'.join(lines)+'\n'
edit('village/buildingmanagers/PanelManager.java',panels)
def plans(s):
 s=s.replace('} else if (block.block == Blocks.OAK_DOOR) {','} else if (block.block instanceof DoorBlock && block.getBlockstate().getValue(DoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {',1)
 a=s.index('      for (BuildingBlock door : doors) {');b=s.index('\n   public List<BuildingPlan.LocationBuildingPair> build(',a)
 s=s[:a]+'''      for (BuildingBlock door : doors) {
         BlockState state = door.getBlockstate();
         Direction facing = state.getValue(DoorBlock.FACING);
         net.minecraft.core.BlockPos pos = door.p.getBlockPos();
         java.util.function.Function<net.minecraft.core.BlockPos, BlockState> at = point -> {
            BuildingBlock planned = map.get(new Point(point));
            return planned == null ? Blocks.AIR.defaultBlockState() : planned.getBlockstate();
         };
         var hinge = DoorHingeSelection.choose(state, at.apply(pos.relative(facing.getCounterClockWise())),
            at.apply(pos.relative(facing.getClockWise())), at.apply(pos.relative(facing.getCounterClockWise()).above()),
            at.apply(pos.relative(facing.getClockWise()).above()));
         door.setBlockstate(state.setValue(DoorBlock.HINGE, hinge));
         if (door.special == BuildingBlock.INVERTED_DOOR) door.special = 0;
      }
   }

'''+s[b:]
 return s
# Verify the actual BuildingBlock setter before installing the plan edit.
assert 'setBlockstate(' in (src/'buildingplan/BuildingBlock.java').read_text()
edit('buildingplan/BuildingPlan.java',plans)
(src/'buildingplan/DoorHingeSelection.java').write_text('''package org.millenaire.common.buildingplan;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.core.BlockPos;

public final class DoorHingeSelection {
    private DoorHingeSelection() {}
    private static boolean door(BlockState s) { return s.getBlock() instanceof DoorBlock && s.getValue(DoorBlock.HALF)==DoubleBlockHalf.LOWER; }
    private static int solid(BlockState s) { return s.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) ? 1 : 0; }
    public static DoorHingeSide choose(BlockState state, BlockState left, BlockState right, BlockState aboveLeft, BlockState aboveRight) {
        boolean leftDoor=door(left), rightDoor=door(right);
        if (leftDoor != rightDoor) return leftDoor ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT;
        int balance=solid(right)+solid(aboveRight)-solid(left)-solid(aboveLeft);
        if (balance!=0) return balance>0 ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT;
        return state.getValue(DoorBlock.HINGE);
    }
}
''',encoding='utf-8')
print('Panel scheduling uses game time; plan door pairs use opposite outer hinges for every door wood type.')
