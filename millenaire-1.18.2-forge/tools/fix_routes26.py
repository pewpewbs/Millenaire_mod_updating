from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');b=r/'src/main/java/org/millenaire/common';bk=r/'migration/before-transactions26'
def edit(n,fn):
 p=b/n;q=bk/n;q.parent.mkdir(parents=True,exist_ok=True)
 if not q.exists():shutil.copy2(p,q)
 p.write_text(fn(p.read_text(encoding='utf-8-sig')),encoding='utf-8')
def nodes(s):
 marker='        return super.getBlockPathType(world, x, y, z);';assert marker in s
 return s.replace(marker,'''        var state = world.getBlockState(pos);
        if (state.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock)
            return state.getValue(net.minecraft.world.level.block.FenceGateBlock.OPEN) ? BlockPathTypes.WALKABLE : BlockPathTypes.WALKABLE_DOOR;
        BlockPathTypes type = super.getBlockPathType(world, x, y, z);
        if ((type == BlockPathTypes.WALKABLE || type == BlockPathTypes.OPEN)
                && (state.getBlock() instanceof net.minecraft.world.level.block.CropBlock
                    || world.getBlockState(pos.below()).getBlock() instanceof net.minecraft.world.level.block.FarmBlock))
            return BlockPathTypes.DANGER_OTHER;
        return type;''')
edit('pathing/VillagerWalkNodeEvaluator.java',nodes)
edit('entity/PathNavigateSimple.java',lambda s:s.replace('        setCanFloat(true);','''        setCanFloat(true);
        mob.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WATER, 32.0F);
        mob.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.DANGER_OTHER, 8.0F);
        setMaxVisitedNodesMultiplier(4.0F);'''))
def resident(s):
 a=s.index('                        double olddistance = this.prevPoint.horizontalDistanceToSquared(this.getPathDestPoint());');b=s.index('                        break block82;',a)
 s=s[:a]+'''                        // Physical movement watchdog handles stalls. Distance-to-target heuristics
                        // incorrectly cancel detours that first lead away from the destination.
                        this.longDistanceStuck = 0;
                        this.localStuck = 0;
'''+s[b:]
 return s
edit('entity/MillVillager.java',resident)
def purse(s):
 a=s.index('    private void storeDeniersInPurse(');b=s.index('    public int totalDeniers(',a)
 s=s[:a]+'''    private void storeDeniersInPurse(ItemStack purse, Player player) {
        org.millenaire.common.utilities.MoneyTransactions.change(player.getInventory(), 0);
    }

'''+s[b:]
 s=s.replace('return InteractionResultHolder.consume(purse);','return InteractionResultHolder.consume(playerIn.getItemInHand(handIn));')
 return s
edit('item/ItemPurse.java',purse)
print('Gate nodes, water/farmland cost and physical-only stall detection; safe purse deposit')
