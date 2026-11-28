package org.millenaire.common.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;

/** Visual target only: never changes a crafting destination or production. */
public final class CraftingWorkplaceFacing {
    private static final int SEARCH_RADIUS = 3;
    private static final int SEARCH_HEIGHT = 1;
    private static final double MAX_DISTANCE = 3.25;
    private static final long SEARCH_INTERVAL = 40L;
    private static final long VISIBILITY_INTERVAL = 5L;

    private BlockPos workplace;
    private Point buildingPos;
    private BlockPos table;
    private long nextSearch;
    private long nextVisibilityCheck;

    /** Called only by an actively working server-side crafter. */
    public boolean face(MillVillager villager) {
        Building building = villager.getGoalBuildingDest();
        Point destination = villager.getGoalDestPoint();
        if (building == null || building.location == null || destination == null) return false;
        BlockPos origin = destination.getBlockPos();
        if (!destination.equals(building.getResManager().getCraftingPos())
                || villager.position().distanceToSqr(Vec3.atBottomCenterOf(origin)) >= 1.5625) return false;

        long now = villager.level.getGameTime();
        if (!origin.equals(workplace) || !building.getPos().equals(buildingPos)) {
            workplace = origin;
            buildingPos = building.getPos();
            table = null;
            nextSearch = now;
            nextVisibilityCheck = now;
        }
        // Destruction/unloading must invalidate the cached block immediately.
        if (table != null && !isTable(villager, building, table)) {
            table = null;
            nextSearch = now;
        }
        if (table != null && now >= nextVisibilityCheck) {
            nextVisibilityCheck = now + VISIBILITY_INTERVAL;
            if (!accessible(villager, table)) {
                table = null;
                nextSearch = now;
            }
        }
        // Negative results are cached as well. A new nearby table is found on
        // the next bounded refresh, with no chunk loads or whole-building scan.
        if (now >= nextSearch) {
            table = findNearest(villager, building, origin);
            nextSearch = now + SEARCH_INTERVAL;
            nextVisibilityCheck = now + VISIBILITY_INTERVAL;
        }
        if (table == null) return false;

        Vec3 target = Vec3.atCenterOf(table).add(0, 0.25, 0);
        double dx = target.x - villager.getX();
        double dz = target.z - villager.getZ();
        float yaw = (float)(Mth.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float turned = villager.getYRot() + Mth.clamp(Mth.wrapDegrees(yaw - villager.getYRot()), -15.0f, 15.0f);
        villager.setYRot(turned);
        villager.setYBodyRot(turned);
        villager.getLookControl().setLookAt(target.x, target.y, target.z, 15.0f, (float)villager.getMaxHeadXRot());
        return true;
    }

    private static BlockPos findNearest(MillVillager villager, Building building, BlockPos origin) {
        BlockPos best = null;
        double bestDistance = MAX_DISTANCE * MAX_DISTANCE;
        for (int dy = -SEARCH_HEIGHT; dy <= SEARCH_HEIGHT; dy++) {
            for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    BlockPos candidate = origin.offset(dx, dy, dz);
                    double distance = villager.position().add(0, 0.8, 0).distanceToSqr(Vec3.atCenterOf(candidate));
                    if (distance >= bestDistance || !isTable(villager, building, candidate)
                            || !accessible(villager, candidate)) continue;
                    best = candidate;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    private static boolean isTable(MillVillager villager, Building building, BlockPos p) {
        var loc = building.location;
        return loc != null && p.getX() >= loc.minx && p.getX() <= loc.maxx
                && p.getY() >= loc.miny && p.getY() <= loc.maxy
                && p.getZ() >= loc.minz && p.getZ() <= loc.maxz
                && villager.level.hasChunkAt(p)
                && villager.level.getBlockState(p).is(Blocks.CRAFTING_TABLE);
    }

    private static boolean accessible(MillVillager villager, BlockPos p) {
        Vec3 target = Vec3.atCenterOf(p).add(0, 0.25, 0);
        Vec3 eyes = villager.getEyePosition();
        return WorkAccess.canAccessResource(villager, new Point(p), MAX_DISTANCE)
                && WorkAccess.loadedBetween(villager.level, new BlockPos(eyes), p)
                && WorkAccess.visible(villager.level, eyes, target, p);
    }
}
