from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');d=r/'src/main/java/org/millenaire/common';b=r/'migration/before-interactions30';b.mkdir(exist_ok=False)
def edit(name,fn):
 p=d/name;dst=b/name;dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,dst);p.write_text(fn(p.read_text()),encoding='utf-8')
def villager(s):
 a=s.index('    private void handleDoorsAndFenceGates() {');z=s.index('    private void handleLeaveClearing()',a)
 s=s[:a]+'''    private void handleDoorsAndFenceGates() {
        if (this.level.isClientSide) return;
        if (this.doorToClose != null) {
            var pos = this.doorToClose.getBlockPos();
            if (this.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) > 9.0
                    && this.level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(pos).inflate(0.7, 1.0, 0.7), e -> e.isAlive()).isEmpty()) {
                var state = this.level.getBlockState(pos);
                if (BlockItemUtilities.isWoodenDoor(state.getBlock()) && state.getValue(DoorBlock.OPEN))
                    ((DoorBlock)state.getBlock()).setOpen(null, this.level, state, pos, false);
                else if (state.getBlock() instanceof FenceGateBlock)
                    this.closeFenceGate(pos.getX(), pos.getY(), pos.getZ());
                this.doorToClose = null;
            }
        }
        // Use the route actually followed by navigation, including native recomputations.
        Path route = this.navigation.getPath();
        if (route == null || route.isDone()) return;
        java.util.Set<net.minecraft.core.BlockPos> candidates = new java.util.LinkedHashSet<>();
        for (int i = Math.max(0, route.getNextNodeIndex() - 1);
                i < Math.min(route.getNodeCount(), route.getNextNodeIndex() + 3); i++) {
            var pos = route.getNode(i).asBlockPos();
            candidates.add(pos); candidates.add(pos.below()); candidates.add(pos.above());
        }
        for (var pos : candidates) {
            if (this.position().distanceToSqr(Vec3.atCenterOf(pos)) > 6.25) continue;
            var state = this.level.getBlockState(pos);
            if (BlockItemUtilities.isWoodenDoor(state.getBlock())) {
                ((DoorBlock)state.getBlock()).setOpen(null, this.level, state, pos, true);
                this.doorToClose = new Point(pos);
            } else if (state.getBlock() instanceof FenceGateBlock) {
                this.openFenceGate(pos.getX(), pos.getY(), pos.getZ());
                this.doorToClose = new Point(pos);
            }
        }
    }

'''+s[z:]
 s=s.replace('    public Point getPathDestPoint() {\n        return this.pathDestPoint;','''    public Point getPathDestPoint() {
        if (this.getActiveGoal() instanceof org.millenaire.common.goal.generic.GoalGenericCooking cooking
                && this.getGoalDestPoint() != null)
            return cooking.getWorkPosition(this);
        return this.pathDestPoint;''')
 return s
edit('entity/MillVillager.java',villager)
edit('utilities/WorldUtilities.java',lambda s:s.replace('if (bs.getBlock() instanceof net.minecraft.world.level.block.FenceBlock) {','''if (bs.getBlock() instanceof net.minecraft.world.level.block.FenceBlock
                || bs.getBlock() instanceof net.minecraft.world.level.block.WallBlock
                || bs.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock
                || bs.getBlock() instanceof org.millenaire.common.block.BlockMillWall) {'''))
(d/'goal/WorkAccess.java').write_text('''package org.millenaire.common.goal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.millenaire.common.entity.MillVillager;
public final class WorkAccess {
    private WorkAccess() {}
    public static boolean visible(MillVillager villager, Vec3 target, BlockPos allowedTarget) {
        // A waist-height ray prevents reaching across a fence or through a wall.
        Vec3 from = villager.position().add(0, 0.8, 0);
        var hit = villager.level.clip(new ClipContext(from, target,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, villager));
        return hit.getType() == HitResult.Type.MISS
                || allowedTarget != null && hit.getBlockPos().equals(allowedTarget);
    }
}
''',encoding='utf-8')
def cooking(s):
 a=s.index('    @Override\n    public boolean performAction(')
 s=s[:a]+'''    public Point getWorkPosition(MillVillager villager) {
        Point furnace = villager.getGoalDestPoint();
        if (furnace == null) return null;
        var state = villager.level.getBlockState(furnace.getBlockPos());
        if (state.hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING))
            return new Point(furnace.getBlockPos().relative(state.getValue(
                    net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)));
        return furnace;
    }
    @Override public int range(MillVillager villager) { return 1; }
    @Override public boolean isWithinActionRange(MillVillager villager, Point ignored) {
        Point target = villager.getGoalDestPoint(), work = getWorkPosition(villager);
        if (target == null || work == null) return false;
        var standing = net.minecraft.world.phys.Vec3.atBottomCenterOf(work.getBlockPos());
        return villager.position().distanceToSqr(standing) < 1.5625
                && org.millenaire.common.goal.WorkAccess.visible(villager,
                    net.minecraft.world.phys.Vec3.atCenterOf(target.getBlockPos()), target.getBlockPos());
    }

'''+s[a:]
 return s.replace('        BlockEntity tileEntity = villager.getGoalDestPoint().getTileEntity(villager.level);','''        if (!this.isWithinActionRange(villager, null)) return false;
        BlockEntity tileEntity = villager.getGoalDestPoint().getTileEntity(villager.level);''')
edit('goal/generic/GoalGenericCooking.java',cooking)
def shear(s):
 a=s.index('    @Override\n    public boolean performAction(')
 s=s[:a]+'''    @Override
    public boolean isWithinActionRange(MillVillager villager, Point ignored) {
        Entity target = villager.getGoalDestEntity();
        return target != null && villager.distanceToSqr(target) < 4.0
                && WorkAccess.visible(villager, target.getBoundingBox().getCenter(), null);
    }

'''+s[a:]
 s=s.replace('            villager.addToInv(','''            if (villager.distanceToSqr(animal) >= 4.0
                    || !WorkAccess.visible(villager, animal.getBoundingBox().getCenter(), null)) continue;
            villager.addToInv(''').replace('        return 5;','        return 2;')
 return s
edit('goal/GoalShearSheep.java',shear)
print('Door route lookahead, occupied passage protection, furnace front access, sheep visibility, wall placement shape updates')
