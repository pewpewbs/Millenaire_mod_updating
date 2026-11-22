from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');d=r/'src/main/java/org/millenaire/common'
p=d/'goal/generic/GoalGenericCrafting.java';b=r/'migration/before-interactions30/goal/generic/GoalGenericCrafting.java';shutil.copy2(p,b)
s=p.read_text();a=s.index('    @Override\n    public boolean performAction(')
s=s[:a]+'''    @Override public int range(MillVillager villager) { return 1; }
    @Override public boolean isWithinActionRange(MillVillager villager, org.millenaire.common.utilities.Point ignored) {
        var destination = villager.getGoalDestPoint();
        if (destination == null) return false;
        var standing = net.minecraft.world.phys.Vec3.atBottomCenterOf(destination.getBlockPos());
        return villager.position().distanceToSqr(standing) < 1.5625
                && org.millenaire.common.goal.WorkAccess.visible(villager,
                    standing.add(0, 0.8, 0), destination.getBlockPos());
    }

'''+s[a:];p.write_text(s,encoding='utf-8')
p=d/'goal/WorkAccess.java';s=p.read_text();s=s.replace('        var hit = villager.level.clip(new ClipContext(from, target,','''        return visible(villager.level, from, target, allowedTarget);
    }
    public static boolean visible(net.minecraft.world.level.BlockGetter world, Vec3 from,
            Vec3 target, BlockPos allowedTarget) {
        var hit = world.clip(new ClipContext(from, target,''').replace('ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, villager','ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null');p.write_text(s,encoding='utf-8')
print('Crafting requires arrival at work position; shared physical line-of-access helper')
