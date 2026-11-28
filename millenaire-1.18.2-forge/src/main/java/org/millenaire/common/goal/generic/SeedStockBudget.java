package org.millenaire.common.goal.generic;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;

/** Computes a bounded stock budget only when a secondary seed actually drops.
 * No world/chunk loading, no periodic scans or destructive cleanup. */
final class SeedStockBudget {
    private SeedStockBudget() {}

    private static boolean storageLoaded(Building b, MillVillager v) {
        for (Point p : b.getResManager().chests) if (!v.level.hasChunkAt(p.getBlockPos())) return false;
        for (Point p : b.getResManager().furnaces) if (!v.level.hasChunkAt(p.getBlockPos())) return false;
        for (Point p : b.getResManager().firepits) if (!v.level.hasChunkAt(p.getBlockPos())) return false;
        return true;
    }

    private static long plantingReserve(Building b, ResourceLocation crop) {
        var points = b.getResManager().getSoilPoints(crop);
        return SeedSurplusPolicy.plantingReserve(points == null ? 0 : points.size());
    }

    static int allowed(MillVillager v, Building dest, ResourceLocation crop, InvItem seed, int requested) {
        Building town = v.getTownHall();
        if (dest == null || town == null || crop == null || seed == null) return requested;
        Building home = v.getHouse();
        Building local = dest.getResManager().chests.isEmpty() && home != null ? home : dest;
        if (!storageLoaded(local, v)) return requested; // Unknown local stock: do not risk starvation.
        long localTarget = Math.max(16L, plantingReserve(dest, crop));
        long localStock = Math.max(0, local.countGoods(seed)) + (long)Math.max(0, v.countInv(seed));
        if (localTarget - localStock >= requested) return requested;

        Set<Building> buildings = new HashSet<>(town.getBuildings());
        buildings.add(town);
        buildings.add(dest);
        buildings.add(local);
        long totalStock = 0, totalTarget = 0;
        boolean complete = true;
        for (Building b : buildings) {
            if (b == null) continue;
            totalTarget += plantingReserve(b, crop);
            if (seed.getItem() == Items.WHEAT_SEEDS) {
                var rm = b.getResManager();
                for (int i = 0; i < Math.min(rm.spawnTypes.size(), rm.spawns.size()); i++) {
                    if (Registry.ENTITY_TYPE.getOptional(rm.spawnTypes.get(i)).orElse(null) == EntityType.CHICKEN)
                        totalTarget += SeedSurplusPolicy.chickenFeedReserve(rm.spawns.get(i).size());
                }
            }
            if (storageLoaded(b, v)) totalStock += Math.max(0, b.countGoods(seed));
            else complete = false;
        }

        for (var record : town.getVillagerRecords().values()) {
            if (record.killed || record.getType() == null) continue;
            for (var requirement : record.getType().requiredGoods.entrySet()) {
                if (seed.matches(requirement.getKey()) && requirement.getValue() != null)
                    totalTarget += Math.max(0, requirement.getValue());
            }
        }
        if (town.culture != null) {
            var trade = town.culture.getTradeGood(seed);
            if (trade != null) totalTarget += Math.max(0, Math.max(trade.targetQuantity, trade.reservedQuantity));
        }
        Set<MillVillager> counted = new HashSet<>(town.getKnownVillagers());
        counted.add(v);
        for (MillVillager worker : counted) {
            if (worker != null && worker.isAlive()) totalStock += Math.max(0, worker.countInv(seed));
        }
        // Missing remote stock is not assumed empty: fall back to the local
        // sowing buffer rather than accumulating indefinitely or loading chunks.
        if (!complete) return SeedSurplusPolicy.allowedDrop(requested, localStock, localTarget, 0, 0);
        return SeedSurplusPolicy.allowedDrop(requested, localStock, localTarget,
                totalStock, Math.max(localTarget, totalTarget));
    }
}
