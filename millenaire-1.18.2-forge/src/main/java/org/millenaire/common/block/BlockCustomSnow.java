package org.millenaire.common.block;

import java.util.List;
import java.util.Random;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.core.BlockPos;

public class BlockCustomSnow extends Block {
    public BlockCustomSnow(String name) {
        super(Properties.of(Material.SNOW).strength(0.4F).sound(SoundType.SNOW).randomTicks().requiresCorrectToolForDrops());
        setRegistryName("millenaire", name);
    }
    @Override public void randomTick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        if (world.getBrightness(LightLayer.BLOCK, pos) > 11) {
            dropResources(state, world, pos);
            world.removeBlock(pos, false);
        }
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder context) {
        ItemStack tool = context.getOptionalParameter(LootContextParams.TOOL);
        if (tool != null && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0)
            return List.of(new ItemStack(this));
        return List.of(new ItemStack(Items.SNOWBALL));
    }
}
