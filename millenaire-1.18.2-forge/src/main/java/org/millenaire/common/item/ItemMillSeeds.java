package org.millenaire.common.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.PlantType;
import org.millenaire.common.advancements.MillAdvancements;
import org.millenaire.common.block.BlockGrapeVine;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.network.ServerSender;

public class ItemMillSeeds extends ItemMill implements IPlantable {
    public final Block crops;
    public final String cropKey;
    public ItemMillSeeds(Block crops, String cropKey) { super(cropKey); this.crops = crops; this.cropKey = cropKey; }
    @Override public BlockState getPlant(BlockGetter level, BlockPos pos) { return crops.defaultBlockState(); }
    @Override public PlantType getPlantType(BlockGetter level, BlockPos pos) { return PlantType.CROP; }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel(); var player = context.getPlayer();
        BlockPos soilPos = context.getClickedPos(), pos = soilPos.above();
        ItemStack stack = context.getItemInHand(); BlockState soil = level.getBlockState(soilPos);
        if (player == null || context.getClickedFace() != Direction.UP || !level.isEmptyBlock(pos)
            || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, Direction.UP, stack)
            || !soil.canSustainPlant(level, soilPos, Direction.UP, this)) return InteractionResult.FAIL;
        if (crops instanceof BlockGrapeVine && (!level.isEmptyBlock(pos.above()) || pos.getY() >= level.getMaxBuildHeight() - 1
            || !level.mayInteract(player, pos.above()) || !player.mayUseItemAt(pos.above(), Direction.UP, stack))) return InteractionResult.FAIL;
        BlockState planted = crops.getStateForPlacement(new BlockPlaceContext(context));
        if (planted == null) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        var data=Mill.getMillWorld(level);
        var profile = data==null?null:data.getProfile(player);
        if (!MillConfigValues.DEV && (profile==null || !profile.isTagSet("cropplanting_" + cropKey))) {
            ServerSender.sendTranslatedSentence(player, 'f', "ui.cropplantingknowledge", new String[0]);
            return InteractionResult.FAIL;
        }
        if (!level.setBlockAndUpdate(pos, planted)) return InteractionResult.FAIL;
        crops.setPlacedBy(level, pos, planted, player, stack);
        MillAdvancements.MASTER_FARMER.grant(player);
        if (player instanceof ServerPlayer serverPlayer) CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, stack);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.CONSUME;
    }
}
