package org.millenaire.common.goal.generic;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.item.Item;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;

/** Physical stock cap for opt-in crafting recipes, not a trade-availability quota.
 * Counts worn tools as the same item, and does not load chunks. */
final class PhysicalVillageStock {
    private PhysicalVillageStock() {}

    static boolean hasRoom(MillVillager worker, Item item, int limit, long produced) {
        Building townHall = worker.getTownHall();
        if (townHall == null || item == null || limit < 0 || produced <= 0 || produced > limit) return false;
        long remaining = limit - produced;
        Set<Point> buildings = new HashSet<>(townHall.buildings);
        buildings.add(townHall.getPos());
        Set<Point> chests = new HashSet<>();
        for (Point pos : buildings) {
            Building building = pos.equals(townHall.getPos()) ? townHall : townHall.mw.getBuilding(pos);
            // Unknown registered storage must not be interpreted as empty.
            if (building == null || building.getResManager() == null) return false;
            for (Point point : building.getResManager().chests) {
                if (point == null || !chests.add(point)) continue;
                if (!townHall.world.hasChunkAt(point.getBlockPos())) return false;
                var chest = point.getMillChest(townHall.world);
                if (chest == null) continue;
                for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                    var stack = chest.getItem(slot);
                    if (!stack.isEmpty() && stack.is(item)) {
                        remaining -= stack.getCount();
                        if (remaining < 0) return false;
                    }
                }
            }
        }
        // Prefer live inventories to potentially stale persisted copies.
        Set<Long> counted = new HashSet<>();
        Set<MillVillager> residents = new HashSet<>(townHall.getKnownVillagers());
        residents.add(worker);
        for (MillVillager resident : residents) {
            if (resident == null || !resident.isAlive() || resident.isRemoved()
                    || !townHall.getPos().equals(resident.townHallPoint)
                    || !counted.add(resident.getVillagerId())) continue;
            for (var key : resident.getInventoryKeys()) {
                if (key.getItem() != item) continue;
                remaining -= Math.max(0, resident.countInv(key));
                if (remaining < 0) return false;
            }
        }
        for (var record : townHall.getVillagerRecords().values()) {
            if (record == null || record.killed || !counted.add(record.getVillagerId())) continue;
            for (var entry : record.inventory.entrySet()) {
                if (entry.getKey().getItem() != item) continue;
                remaining -= Math.max(0, entry.getValue());
                if (remaining < 0) return false;
            }
        }
        return true;
    }
}
