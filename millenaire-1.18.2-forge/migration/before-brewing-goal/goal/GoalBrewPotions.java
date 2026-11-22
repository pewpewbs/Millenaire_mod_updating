/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.init.Items
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagString
 *  net.minecraft.tileentity.TileEntityBrewingStand
 */
package org.millenaire.common.goal;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.utilities.Point;

@DocumentedElement.Documentation(value="Brew alchemical potions from nether warts. Currently broken.")
public class GoalBrewPotions
extends Goal {
    public GoalBrewPotions() {
        this.icon = InvItem.createInvItem((Item)Items.POTION);
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws Exception {
        int nbWarts = villager.getHouse().countGoods(Items.NETHER_WART);
        int nbBottles = villager.getHouse().countGoods(Items.GLASS_BOTTLE);
        int nbPotions = villager.getHouse().countGoods((Item)Items.POTION, -1);
        for (Point p : villager.getHouse().getResManager().brewingStands) {
            BrewingStandBlockEntity brewingStand = p.getBrewingStand(villager.level);
            if (brewingStand == null || brewingStand.func_174887_a_(0) != 0) continue;
            if (brewingStand.getItem(3) == ItemStack.EMPTY && nbWarts > 0 && nbPotions < 64) {
                return this.packDest(p, villager.getHouse());
            }
            if (nbBottles > 2 && (brewingStand.getItem(0) == ItemStack.EMPTY || brewingStand.getItem(1) == ItemStack.EMPTY || brewingStand.getItem(2) == ItemStack.EMPTY) && nbPotions < 64) {
                return this.packDest(p, villager.getHouse());
            }
            for (int i = 0; i < 3; ++i) {
                if (brewingStand.getItem(i) == null || brewingStand.getItem(i).getItem() != Items.POTION || brewingStand.getItem(i).getDamageValue() != 16) continue;
                return this.packDest(p, villager.getHouse());
            }
        }
        return null;
    }

    @Override
    public boolean isPossibleSpecific(MillVillager villager) throws Exception {
        return this.getDestination(villager) != null;
    }

    @Override
    public boolean performAction(MillVillager villager) throws Exception {
        int nbWarts = villager.getHouse().countGoods(Items.NETHER_WART);
        int nbBottles = villager.getHouse().countGoods(Items.GLASS_BOTTLE);
        int nbPotions = villager.getHouse().countGoods((Item)Items.POTION);
        BrewingStandBlockEntity brewingStand = villager.getGoalDestPoint().getBrewingStand(villager.level);
        if (brewingStand == null) {
            return true;
        }
        if (brewingStand.func_174887_a_(0) == 0) {
            int i;
            if (brewingStand.getItem(3) == ItemStack.EMPTY && nbWarts > 0 && nbPotions < 64) {
                brewingStand.setItem(3, new ItemStack(Items.NETHER_WART, 1));
                villager.getHouse().takeGoods(Items.NETHER_WART, 1);
            }
            if (nbBottles > 2 && nbPotions < 64) {
                for (i = 0; i < 3; ++i) {
                    if (brewingStand.getItem(i) != ItemStack.EMPTY) continue;
                    ItemStack waterPotion = org.millenaire.common.item.LegacyItemStacks.stack((Item)Items.POTION, 1, 0);
                    waterPotion.addTagElement("Potion", (Tag)new StringTag("minecraft:water"));
                    brewingStand.setItem(i, waterPotion);
                    villager.getHouse().takeGoods(Items.GLASS_BOTTLE, 1);
                }
            }
            for (i = 0; i < 3; ++i) {
                if (brewingStand.getItem(i) == ItemStack.EMPTY || brewingStand.getItem(i).getItem() != Items.POTION || brewingStand.getItem(i).getDamageValue() != 16) continue;
                brewingStand.setItem(i, ItemStack.EMPTY);
                villager.getHouse().storeGoods((Item)Items.POTION, 16, 1);
            }
        }
        return true;
    }

    @Override
    public int priority(MillVillager villager) throws Exception {
        return 100;
    }

    @Override
    public boolean swingArms() {
        return true;
    }
}

