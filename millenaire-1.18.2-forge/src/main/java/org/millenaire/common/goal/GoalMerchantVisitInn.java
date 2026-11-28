/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.init.Blocks
 *  net.minecraft.item.ItemStack
 */
package org.millenaire.common.goal;

import java.util.ArrayList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.item.TradeGood;
import org.millenaire.common.utilities.MillLog;

@DocumentedElement.Documentation(value="For local merchants, drop off picked up goods at the Inn for export and take goods for import.")
public class GoalMerchantVisitInn
extends Goal {
    public GoalMerchantVisitInn() {
        this.icon = InvItem.createInvItem((Block)Blocks.CHEST);
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws Exception {
        return this.packDest(villager.getHouse().getResManager().getSellingPos(), villager.getHouse());
    }

    @Override
    public ItemStack[] getHeldItemsTravelling(MillVillager villager) {
        ArrayList<ItemStack> items = new ArrayList<ItemStack>();
        for (InvItem good : villager.getInventoryKeys()) {
            if (villager.countInv(good.getItem(), good.meta) <= 0) continue;
            items.add(org.millenaire.common.item.LegacyItemStacks.stack(good.getItem(), 1, good.meta));
        }
        return items.toArray(new ItemStack[items.size()]);
    }

    @Override
    public boolean isPossibleSpecific(MillVillager villager) throws Exception {
        if (!LocalLogistics.usable(villager.getHouse()) || villager.getTownHall() == null) return false;
        boolean delayOver = !villager.lastGoalTime.containsKey(this) ? true : villager.level.getGameTime() > villager.lastGoalTime.get(this) + 2000L;
        int nb = 0;
        for (InvItem invItem : villager.getInventoryKeys()) {
            int nbcount = Math.min(villager.countInv(invItem),villager.getHouse().storageRoom(invItem));
            if (nbcount <= 0 || villager.getTownHall().nbGoodNeeded(invItem.getItem(), invItem.meta) != 0) continue;
            nb += nbcount;
            if (delayOver) {
                return true;
            }
            if (nb <= 64) continue;
            return true;
        }
        for (TradeGood tradeGood : villager.getTownHall().culture.goodsList) {
            if (tradeGood.item.meta < 0 || villager.getHouse().countGoods(tradeGood.item.getItem(), tradeGood.item.meta) <= 0 || villager.countInv(tradeGood.item.getItem(), tradeGood.item.meta) >= villager.getTownHall().nbGoodNeeded(tradeGood.item.getItem(), tradeGood.item.meta)) continue;
            if (MillConfigValues.LogMerchant >= 1) {
                MillLog.major(this, "Visiting the Inn to take imports");
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean performAction(MillVillager villager) throws Exception {
        if (!LocalLogistics.access(this,villager,villager.getHouse())) return false;
        String s = "";
        for (InvItem invItem : villager.getInventoryKeys()) {
            if (villager.countInv(invItem.getItem(), invItem.meta) <= 0 || villager.getTownHall().nbGoodNeeded(invItem.getItem(), invItem.meta) != 0) continue;
            int nb = villager.putInBuilding(villager.getHouse(), invItem.getItem(), invItem.meta, 99999999);
            if (villager.getCulture().getTradeGood(invItem) == null || nb <= 0) continue;
            s = s + ";" + villager.getCulture().getTradeGood((InvItem)invItem).key + "/" + nb;
        }
        if (s.length() > 0) {
            villager.getHouse().visitorsList.add("storedexports;" + villager.func_70005_c_() + s);
        }
        s = "";
        for (TradeGood good : villager.getTownHall().culture.goodsList) {
            int nb;
            if (good.item.meta < 0) continue;
            int nbNeeded = villager.getTownHall().nbGoodNeeded(good.item.getItem(), good.item.meta);
            if (villager.countInv(good.item.getItem(), good.item.meta) >= nbNeeded || (nb = villager.takeFromBuilding(villager.getHouse(), good.item.getItem(), good.item.meta, nbNeeded - villager.countInv(good.item.getItem(), good.item.meta))) <= 0) continue;
            s = s + ";" + good.key + "/" + nb;
        }
        if (s.length() > 0) {
            villager.getHouse().visitorsList.add("broughtimport;" + villager.func_70005_c_() + s);
        }
        return true;
    }

    @Override
    public boolean isWithinActionRange(MillVillager villager,org.millenaire.common.utilities.Point destination) {
        return WorkAccess.canAccessStock(villager,destination,this.range(villager));
    }

    @Override
    public int priority(MillVillager villager) throws Exception {
        return 100;
    }
}

