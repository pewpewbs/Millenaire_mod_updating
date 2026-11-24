/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.init.Blocks
 *  net.minecraft.item.ItemStack
 */
package org.millenaire.common.goal;

import java.util.ArrayList;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.village.Building;

@DocumentedElement.Documentation(value="Deliver resources to a shop from the villager's inventory. Paired with gethousethresources that picks them up.")
public class GoalDeliverResourcesShop
extends Goal {
    public GoalDeliverResourcesShop() {
        this.icon = InvItem.createInvItem(Blocks.OAK_LOG);
    }

    @Override
    public int actionDuration(MillVillager villager) {
        return 40;
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) {
        return this.getDestination(villager, false);
    }

    public Goal.GoalInformation getDestination(MillVillager villager, boolean test) {
        if (villager.getTownHall() == null) return null;
        boolean delayOver = !test || !villager.lastGoalTime.containsKey(this)
                || villager.level.getGameTime() > villager.lastGoalTime.get(this) + 2000L;
        Building best = null;
        long bestAmount = 0;
        for (Building shop : villager.getTownHall().getShops()) {
            long amount = 0;
            for (InvItem item : villager.getInventoryKeys())
                amount += LocalLogistics.shopDelivery(villager,shop,item);
            if (amount > bestAmount) { best = shop; bestAmount = amount; }
        }
        if (best != null && (delayOver || bestAmount > 16))
            return this.packDest(best.getResManager().getSellingPos(),best);
        GoalDiagnostics.note(villager,this,"NO_RECEIVABLE_SHOP_CARGO_OR_DELIVERY_COOLDOWN");
        return null;
    }

    @Override
    public ItemStack[] getHeldItemsTravelling(MillVillager villager) {
        ArrayList<ItemStack> items = new ArrayList<ItemStack>();
        Building shop = villager.getGoalBuildingDest();
        if (shop != null && villager.getCulture().shopNeeds.containsKey(shop.location.shop)) {
            for (InvItem item : villager.getCulture().shopNeeds.get(shop.location.shop)) {
                if (villager.countInv(item.getItem(), item.meta) <= 0) continue;
                items.add(org.millenaire.common.item.LegacyItemStacks.stack(item.getItem(), 1, item.meta));
            }
        }
        return items.toArray(new ItemStack[items.size()]);
    }

    @Override
    public boolean isPossibleSpecific(MillVillager villager) {
        return this.getDestination(villager, true) != null;
    }

    @Override
    public boolean isWithinActionRange(MillVillager villager,
            org.millenaire.common.utilities.Point destination) {
        return WorkAccess.canAccessStock(villager, destination, this.range(villager));
    }

    @Override
    public boolean performAction(MillVillager villager) {
        Building shop = villager.getGoalBuildingDest();
        if (shop == null) return true;
        if (!LocalLogistics.access(this,villager,shop)) return false;
        for (InvItem item : new ArrayList<>(villager.getInventoryKeys())) {
            int amount = LocalLogistics.shopDelivery(villager,shop,item);
            if (amount > 0) villager.putInBuilding(shop,item.getItem(),item.meta,Math.min(256,amount));
        }
        return true;
    }

    @Override
    public int priority(MillVillager villager) {
        if (villager.getTownHall() == null) return 0;
        var shops = villager.getTownHall().getShops();
        long total = 0;
        // A carried item is counted once even if several shops accept it.
        for (InvItem item : villager.getInventoryKeys()) {
            int best = 0;
            for (Building shop : shops) best = Math.max(best,LocalLogistics.shopDelivery(villager,shop,item));
            total += best;
        }
        return (int)Math.min(Integer.MAX_VALUE,total*10);
    }
}
