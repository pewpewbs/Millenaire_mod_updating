/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.Blocks
 *  net.minecraft.util.EnumActionResult
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.world.World
 */
package org.millenaire.common.item;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.buildingplan.BuildingImportExport;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.item.ItemMill;
import org.millenaire.common.network.ServerSender;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;
import org.millenaire.common.world.MillWorldData;

public class ItemNegationWand
extends ItemMill {
    public ItemNegationWand(String itemName) {
        super(itemName);
    }

    @Override public InteractionResult onItemUseFirst(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.context.UseOnContext context) {
        Player player=context.getPlayer();Level world=context.getLevel();BlockPos bp=context.getClickedPos();
        if(player==null)return InteractionResult.PASS;
        Point pos = new Point(bp);
        BlockState bs = world.getBlockState(bp);
        if (bs.getBlock() == MillBlocks.IMPORT_TABLE || bs.getBlock() == MillBlocks.ANIMAL_SPAWN
            || bs.getBlock() == MillBlocks.FREE_BLOCK || bs.getBlock() == MillBlocks.DECOR_BLOCK
            || bs.getBlock() == MillBlocks.TREE_SPAWN || bs.getBlock() == MillBlocks.SOURCE
            || bs.getBlock() == MillBlocks.MARKER_BLOCK || bs.getBlock() == MillBlocks.SOIL_BLOCK) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide) {
            if (bs.getBlock() instanceof net.minecraft.world.level.block.StandingSignBlock) {
                BuildingImportExport.negationWandExportBuilding(player, world, pos);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        if(bs.getBlock() instanceof net.minecraft.world.level.block.StandingSignBlock)return InteractionResult.CONSUME;
        if(!world.mayInteract(player,bp) || !player.mayUseItemAt(bp,context.getClickedFace(),stack))return InteractionResult.FAIL;
        MillWorldData mw = Mill.getMillWorld(world);
        if(mw==null)return InteractionResult.FAIL;
        for (int i = 0; i < 2; ++i) {
            MillCommonUtilities.VillageList list = i == 0 ? mw.loneBuildingsList : mw.villagesList;
            for (int j = 0; j < list.names.size(); ++j) {
                Building th;
                Point p = list.pos.get(j);
                int distance = Mth.floor((double)p.horizontalDistanceTo(pos));
                if (distance > 30 || (th = mw.getBuilding(p)) == null || !th.isTownhall) continue;
                if (th.chestLocked && !MillConfigValues.DEV) {
                    ServerSender.sendTranslatedSentence(player, '6', "negationwand.villagelocked", th.villageType.name);
                    return InteractionResult.SUCCESS;
                }
                ServerSender.displayNegationWandGUI(player, th);
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }
}

