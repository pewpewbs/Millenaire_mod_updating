/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.init.Blocks
 *  net.minecraft.init.Items
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.EnumHand
 *  net.minecraft.world.World
 */
package org.millenaire.common.goal;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.utilities.Point;

@DocumentedElement.Documentation(value="Plant cacao seeds at home.")
public class GoalPlantCacao
extends Goal {

    public GoalPlantCacao() {
        this.icon = InvItem.createInvItem(Items.COCOA_BEANS, 0);
    }

    private BlockState getCocoaState(Level world, Point p) {
        BlockState result = null;
        // Keep the original tie-breaking order, but only use supports valid for cocoa.
        for (Direction direction : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            BlockState support = p.getRelative(direction.getStepX(), 0, direction.getStepZ()).getBlockActualState(world);
            if (support.is(BlockTags.JUNGLE_LOGS))
                result = Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.FACING, direction).setValue(CocoaBlock.AGE, 0);
        }
        return result;
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) {
        if (villager.getHouse() == null) return null;
        Point p = villager.getHouse().getResManager().getCocoaPlantingLocation();
        return p == null ? null : this.packDest(p, villager.getHouse());
    }

    @Override
    public ItemStack[] getHeldItemsTravelling(MillVillager villager) {
        return new ItemStack[]{new ItemStack(Items.COCOA_BEANS)};
    }

    @Override
    public boolean isPossibleSpecific(MillVillager villager) {
        return this.getDestination(villager) != null;
    }

    @Override
    public boolean lookAtGoal() {
        return true;
    }

    @Override
    public boolean performAction(MillVillager villager) {
        Point cropPoint = villager.getGoalDestPoint();
        if (cropPoint == null || !cropPoint.getBlockActualState(villager.level).isAir()) return true;
        BlockState state = this.getCocoaState(villager.level, cropPoint);
        if (state != null && villager.setBlockstate(cropPoint, state))
            villager.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    @Override
    public int priority(MillVillager villager) {
        return 120;
    }
}

