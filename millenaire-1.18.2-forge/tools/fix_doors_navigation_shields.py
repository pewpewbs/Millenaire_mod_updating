from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge')
def edit(rel,fn):
 p=r/rel;b=r/'migration/before-door-navigation-fix'/rel;b.parent.mkdir(parents=True,exist_ok=True)
 if not b.exists():shutil.copy2(p,b)
 p.write_text(fn(p.read_text(encoding='utf-8')),encoding='utf-8')
def doors(s):
 s=s.replace('if (this.special == 0) {\n                blockSet = this.buildNormalBlock', 'if (this.block instanceof DoorBlock && (this.special == 0 || this.special == INVERTED_DOOR)) {\n                blockSet = DoorPlacement.place(world, this.p.getBlockPos(), this.blockState, this.special == INVERTED_DOOR);\n            } else if (this.special == 0) {\n                blockSet = this.buildNormalBlock',1)
 start=s.index('            } else if (this.special == INVERTED_DOOR) {')
 end=s.index('\n            }',start+len('            } else if (this.special == INVERTED_DOOR) {'))
 # Remove the old special path; all actual doors are handled before other markers.
 s=s[:start]+s[end:]
 return s
edit('src/main/java/org/millenaire/common/buildingplan/BuildingBlock.java',doors)
p=r/'src/main/java/org/millenaire/common/buildingplan/DoorPlacement.java'
p.write_text('''package org.millenaire.common.buildingplan;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

/** Place both halves before allowing neighbour shape updates to inspect the pair. */
public final class DoorPlacement {
    private DoorPlacement() {}
    public static BlockState half(BlockState state, boolean inverted, DoubleBlockHalf half) {
        return (inverted ? state.setValue(DoorBlock.HINGE, DoorHingeSide.RIGHT) : state)
            .setValue(DoorBlock.HALF, half);
    }
    public static boolean place(Level world, BlockPos pos, BlockState state, boolean inverted) {
        BlockPos bottom = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        BlockPos top = bottom.above();
        if (world.isOutsideBuildHeight(bottom) || world.isOutsideBuildHeight(top)) return false;
        BlockState lower = half(state, inverted, DoubleBlockHalf.LOWER);
        BlockState upper = half(state, inverted, DoubleBlockHalf.UPPER);
        // UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE: an incomplete pair must not remove itself.
        world.setBlock(bottom, lower, 18);
        world.setBlock(top, upper, 18);
        world.updateNeighborsAt(bottom, state.getBlock());
        world.updateNeighborsAt(top, state.getBlock());
        return world.getBlockState(bottom).equals(lower) && world.getBlockState(top).equals(upper);
    }
}
''',encoding='utf-8')
def nav(s):
 s=s.replace('this.nodeEvaluator.setCanPassDoors(true);','this.nodeEvaluator.setCanPassDoors(true);\n        this.nodeEvaluator.setCanOpenDoors(true);')
 needle='''            this.path.setNextNodeIndex(j1);
            break;
        }
    }'''
 assert s.count(needle)==1
 return s.replace(needle,'''            this.path.setNextNodeIndex(j1);
            break;
        }
        this.doStuckDetection(vec3d);
    }''')
edit('src/main/java/org/millenaire/common/entity/PathNavigateSimple.java',nav)
shield=r/'src/main/resources/assets/millenaire/textures/entity/shield'
for name in ['seljuk_mil','seljuk_rel']:
 target=shield/(name+'.png')
 assert not target.exists()
 shutil.copy2(shield/'seljuk.png',target)
print('Consistent door halves, navigation stuck detection, existing Seljuk shield fallback installed.')
