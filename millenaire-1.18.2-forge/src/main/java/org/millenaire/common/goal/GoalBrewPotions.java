package org.millenaire.common.goal;

import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.village.Building;
import org.millenaire.common.utilities.Point;

@DocumentedElement.Documentation(value="Brew awkward potions from water bottles, nether wart and blaze powder.")
public class GoalBrewPotions extends Goal {
    public GoalBrewPotions() { this.icon = InvItem.createInvItem(Items.POTION); }
    private static boolean idle(BrewingStandBlockEntity stand) {
        return stand.saveWithoutMetadata().getShort("BrewTime") == 0;
    }
    private static boolean finished(ItemStack stack) {
        return stack.is(Items.POTION) && PotionUtils.getPotion(stack) == Potions.AWKWARD;
    }
    private static boolean needsFuel(BrewingStandBlockEntity stand) {
        return stand.saveWithoutMetadata().getByte("Fuel") == 0 && stand.getItem(4).isEmpty();
    }
    @Override public GoalInformation getDestination(MillVillager villager) throws Exception {
        Building house = villager.getHouse();
        if (house == null || villager.level.isClientSide) return null;
        boolean belowLimit = house.countGoods(Items.POTION, -1) < 64;
        for (Point pos : house.getResManager().brewingStands) {
            var stand = pos.getBrewingStand(villager.level);
            if (stand == null || !idle(stand)) continue;
            for (int slot = 0; slot < 3; slot++) {
                if (finished(stand.getItem(slot))) return packDest(pos, house);
                if (belowLimit && stand.getItem(slot).isEmpty() && house.countGoods(Items.GLASS_BOTTLE) > 0)
                    return packDest(pos, house);
            }
            if (belowLimit && ((stand.getItem(3).isEmpty() && house.countGoods(Items.NETHER_WART) > 0)
                || (needsFuel(stand) && house.countGoods(Items.BLAZE_POWDER) > 0))) return packDest(pos, house);
        }
        return null;
    }
    @Override public boolean isPossibleSpecific(MillVillager villager) throws Exception { return getDestination(villager) != null; }
    @Override public boolean performAction(MillVillager villager) throws Exception {
        Building house = villager.getHouse();
        Point pos = villager.getGoalDestPoint();
        if (house == null || pos == null || villager.level.isClientSide) return true;
        var stand = pos.getBrewingStand(villager.level);
        if (stand == null || !idle(stand)) return true;
        boolean changed = false;
        for (int slot = 0; slot < 3; slot++) {
            var stack = stand.getItem(slot);
            if (finished(stack) && house.storeItemStack(stack.copy())) {
                stand.setItem(slot, ItemStack.EMPTY);
                changed = true;
            }
        }
        if (house.countGoods(Items.POTION, -1) < 64) {
            if (needsFuel(stand) && house.takeGoods(Items.BLAZE_POWDER, 1) == 1) {
                stand.setItem(4, new ItemStack(Items.BLAZE_POWDER)); changed = true;
            }
            if (stand.getItem(3).isEmpty() && house.takeGoods(Items.NETHER_WART, 1) == 1) {
                stand.setItem(3, new ItemStack(Items.NETHER_WART)); changed = true;
            }
            for (int slot = 0; slot < 3; slot++) {
                if (stand.getItem(slot).isEmpty() && house.takeGoods(Items.GLASS_BOTTLE, 1) == 1) {
                    stand.setItem(slot, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER)); changed = true;
                }
            }
        }
        if (changed) stand.setChanged();
        return true;
    }
    @Override public int priority(MillVillager villager) { return 100; }
    @Override public boolean swingArms() { return true; }
}
