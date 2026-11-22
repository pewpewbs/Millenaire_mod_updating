/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.BlockCocoa
 *  net.minecraft.block.properties.IProperty
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.init.Blocks
 *  net.minecraft.init.Items
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.EnumHand
 */
package org.millenaire.common.goal;

import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.utilities.Point;

@DocumentedElement.Documentation(value="Goal that harvests ripe cacao.")
public class GoalHarvestCacao
extends Goal {
    private static ItemStack[] CACAO = new ItemStack[]{org.millenaire.common.item.LegacyItemStacks.stack(Items.field_151100_aR, 1, 3)};

    public GoalHarvestCacao() {
        this.icon = InvItem.createInvItem(Items.field_151100_aR, 3);
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) {
        Point p = villager.getHouse().getResManager().getCocoaHarvestLocation();
        return this.packDest(p, villager.getHouse());
    }

    @Override
    public ItemStack[] getHeldItemsOffHandDestination(MillVillager villager) {
        return CACAO;
    }

    @Override
    public ItemStack[] getHeldItemsTravelling(MillVillager villager) {
        return villager.getBestHoeStack();
    }

    @Override
    public boolean isPossibleSpecific(MillVillager villager) {
        return this.getDestination(villager).getDest() != null;
    }

    @Override
    public boolean lookAtGoal() {
        return true;
    }

    @Override
    public boolean performAction(MillVillager villager) {
        BlockState bs;
        Point cropPoint = villager.getGoalDestPoint();
        if (cropPoint.getBlock(villager.level) == Blocks.COCOA && (Integer)(bs = cropPoint.getBlockActualState(villager.level)).getValue(CocoaBlock.AGE) >= 2) {
            villager.setBlockAndMetadata(cropPoint, Blocks.AIR, 0);
            int nbcrop = 2;
            float irrigation = villager.getTownHall().getVillageIrrigation();
            double rand = Math.random();
            if (rand < (double)(irrigation / 100.0f)) {
                ++nbcrop;
            }
            villager.addToInv(Items.field_151100_aR, 3, nbcrop);
            villager.swing(InteractionHand.MAIN_HAND);
        }
        return true;
    }

    @Override
    public int priority(MillVillager villager) {
        return 100;
    }
}

