package org.millenaire.common.goal;

import java.util.*;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.village.Building;

/** Shared read-only planning for household and shop supply, not a new economic quota. */
final class LocalLogistics {
    private LocalLogistics() {}
    static boolean usable(Building building) {
        return building != null && building.getResManager().getSellingPos() != null
                && !building.getResManager().chests.isEmpty();
    }
    static Map<InvItem,Integer> householdTargets(MillVillager villager) {
        Map<InvItem,Integer> targets = new LinkedHashMap<>();
        Building home = villager.getHouse();
        if (home == null) return targets;
        for (MillVillager resident : home.getKnownVillagers()) {
            if (resident == null || resident.getHouse() != home) continue;
            for (var entry : resident.requiresGoods().entrySet()) {
                if (entry.getValue() > 0)
                    targets.merge(entry.getKey(), entry.getValue(), (a,b) -> (int)Math.min(Integer.MAX_VALUE,(long)a+b));
            }
        }
        return targets;
    }
    static int deficit(Building home, InvItem item, int target) {
        return Math.max(0, target - home.countGoods(item));
    }
    static boolean low(Building home, InvItem item, int target) {
        // A one-item requirement must also trigger supply when empty.
        return home.countGoods(item) < (target / 2 + target % 2);
    }
    static int room(Building target, InvItem request, Collection<InvItem> concreteKeys) {
        if (!usable(target)) return 0;
        if (request.meta != -1) return target.storageRoom(request);
        // Free slots are shared between variants: use maximum, not sum.
        int best = 0;
        for (InvItem key : concreteKeys)
            if (key.meta != -1 && request.matches(key)) best = Math.max(best, target.storageRoom(key));
        return best;
    }
    static int householdDelivery(MillVillager v, Building home, InvItem item, int target) {
        return Math.min(deficit(home,item,target), Math.min(v.countInv(item),room(home,item,v.getInventoryKeys())));
    }
    static int householdPickup(MillVillager v, Building source, InvItem item, int target) {
        Building home = v.getHouse();
        if (source == home || !usable(source) || !usable(home)) return 0;
        int wanted = Math.max(0, deficit(home,item,target) - v.countInv(item));
        int capacity = Math.max(0, room(home,item,source.stockKeys()) - v.countInv(item));
        return Math.min(wanted, Math.min(capacity, source.nbGoodAvailable(item,false,false,false)));
    }
    static Collection<InvItem> needs(MillVillager v, Building shop) {
        if (!usable(shop) || shop.location == null || shop.location.shop == null) return List.of();
        var needs = v.getCulture().shopNeeds.get(shop.location.shop);
        return needs == null ? List.of() : new LinkedHashSet<>(needs);
    }
    static boolean neededBy(MillVillager v, Building shop, InvItem concrete) {
        for (InvItem item : needs(v,shop)) if (item.matches(concrete)) return true;
        return false;
    }
    static int shopDelivery(MillVillager v, Building shop, InvItem concrete) {
        return concrete.meta == -1 || !neededBy(v,shop,concrete) ? 0
                : Math.min(v.countInv(concrete),shop.storageRoom(concrete));
    }
    static int shopPickup(MillVillager v, Building source, InvItem concrete, List<Building> shops) {
        if (!usable(source) || concrete.meta == -1) return 0;
        int capacity = 0;
        for (Building shop : shops) {
            if (shop == source || !neededBy(v,shop,concrete)) continue;
            capacity = Math.max(capacity,shop.storageRoom(concrete));
        }
        // Existing cargo already occupies part of the recipient's capacity.
        return Math.min(Math.max(0,capacity-v.countInv(concrete)),
                source.nbGoodAvailable(concrete,false,false,true));
    }
    static boolean access(Goal goal, MillVillager v, Building building) {
        return !v.level.isClientSide && usable(building)
                && building.getResManager().getSellingPos().equals(v.getGoalDestPoint())
                && goal.isWithinActionRange(v,v.getGoalDestPoint());
    }
}
