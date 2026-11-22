/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.EnumActionResult
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.World
 */
package org.millenaire.common.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.millenaire.common.entity.EntityWallDecoration;
import org.millenaire.common.item.ItemMill;

public class ItemWallDecoration
extends ItemMill {
    public int type;

    public ItemWallDecoration(String itemName, int type) {
        super(itemName);
        this.type = type;
    }

    public InteractionResult func_180614_a(Player player, Level worldIn, BlockPos pos, InteractionHand hand, Direction facing, float hitX, float hitY, float hitZ) {
        ItemStack itemstack = player.getItemInHand(hand);
        BlockPos blockpos = pos.relative(facing);
        if (facing != Direction.DOWN && facing != Direction.UP && player.mayUseItemAt(blockpos, facing, itemstack)) {
            EntityWallDecoration entityhanging = new EntityWallDecoration(worldIn, blockpos, facing, this.type, false);
            if (entityhanging != null && entityhanging.survives()) {
                if (!worldIn.isClientSide) {
                    entityhanging.playPlacementSound();
                    worldIn.addEntity((Entity)entityhanging);
                }
                itemstack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }
}

