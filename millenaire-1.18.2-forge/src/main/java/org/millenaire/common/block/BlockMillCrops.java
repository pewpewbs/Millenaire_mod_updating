package org.millenaire.common.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.ForgeHooks;
import org.millenaire.common.utilities.BlockItemUtilities;

public class BlockMillCrops extends CropBlock implements LegacyVariantBlock {
    private final boolean requireIrrigation;
    private final boolean slowGrowth;
    private final ResourceLocation seed;
    public BlockMillCrops(String name, boolean requireIrrigation, boolean slowGrowth, ResourceLocation seed) {
        super(Properties.of(Material.PLANT).noCollission().randomTicks().instabreak().sound(SoundType.GRASS));
        this.requireIrrigation = requireIrrigation;
        this.slowGrowth = slowGrowth;
        this.seed = seed;
        setRegistryName("millenaire", name);
    }
    protected Item getSeed() { return Registry.ITEM.get(seed); }
    @Override protected ItemLike getBaseSeedId() { return getSeed(); }
    protected static float getGrowthChance(BlockMillCrops crop, Level level, BlockPos pos) {
        BlockState soil = level.getBlockState(pos.below());
        if (crop.requireIrrigation && (!soil.hasProperty(FarmBlock.MOISTURE) || soil.getValue(FarmBlock.MOISTURE) == 0)) return 0;
        return crop.slowGrowth ? 4 : 8;
    }
    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, Random random) {
        if (!state.canSurvive(level, pos)) { level.destroyBlock(pos, true); return; }
        if (!level.isAreaLoaded(pos, 1) || level.getMaxLocalRawBrightness(pos.above()) < 9 || isMaxAge(state)) return;
        float chance = getGrowthChance(this, level, pos);
        if (chance > 0 && ForgeHooks.onCropsGrowPre(level, pos, state, random.nextInt((int)(25 / chance)) == 0)) {
            level.setBlock(pos, getStateForAge(getAge(state) + 1), 2);
            ForgeHooks.onCropsGrowPost(level, pos, state);
        }
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder context) {
        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(getSeed()));
        if (isMaxAge(state)) {
            ItemStack tool = context.getOptionalParameter(LootContextParams.TOOL);
            int fortune = tool == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, tool);
            Random random = context.getLevel().random;
            for (int i = 0; i < 3 + fortune; i++) if (random.nextInt(2 * getMaxAge()) <= getAge(state)) drops.add(new ItemStack(getSeed()));
        }
        return drops;
    }
    public BlockState withAge(int age) { return getStateForAge(age); }
    public void grow(Level level, BlockPos pos, BlockState state) { level.setBlock(pos, getStateForAge(Math.min(getMaxAge(), getAge(state) + getBonemealAgeIncrease(level))), 2); }
    @Override public void performBonemeal(ServerLevel level, Random random, BlockPos pos, BlockState state) { grow(level, pos, state); }
    @Override public BlockState func_176203_a(int metadata) { return getStateForAge(Math.max(0, Math.min(getMaxAge(), metadata))); }
    @Override public int func_176201_c(BlockState state) { return getAge(state); }
    @Override public int itemMetadata(BlockState state) { return 0; }
    @Override public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity entity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, entity, tool);
        if (!level.isClientSide) BlockItemUtilities.checkForHarvestTheft(player, pos);
    }
}
