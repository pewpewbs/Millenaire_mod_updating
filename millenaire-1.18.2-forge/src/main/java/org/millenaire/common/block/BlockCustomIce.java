package org.millenaire.common.block;

import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;

public class BlockCustomIce extends Block {
    public BlockCustomIce(String name) {
        super(Properties.of(Material.ICE).strength(0.5F).friction(0.98F).sound(SoundType.GLASS).noOcclusion());
        setRegistryName("millenaire", name);
    }
    @Override public int getLightBlock(BlockState state, BlockGetter world, BlockPos pos) { return 20; }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder context) { return List.of(new ItemStack(this)); }
}
