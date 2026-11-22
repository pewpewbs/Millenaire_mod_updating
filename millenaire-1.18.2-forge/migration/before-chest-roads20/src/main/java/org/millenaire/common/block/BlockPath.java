package org.millenaire.common.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.millenaire.common.item.IMetaBlockName;

public class BlockPath extends Block implements IBlockPath, LegacyVariantBlock, IMetaBlockName {
    private static final VoxelShape BOTTOM = Block.box(0, 0, 0, 16, 15, 16);
    private final String pathName;
    public BlockPath(String name, MaterialColor color, SoundType sound) {
        super(Properties.of(Material.DIRT, color).strength(0.8F).sound(sound).noOcclusion());
        pathName = name;
        setRegistryName("millenaire", name);
        registerDefaultState(defaultBlockState().setValue(STABLE, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(STABLE); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return BOTTOM; }
    @Override public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos,
            net.minecraft.world.level.pathfinder.PathComputationType type) { return false; }
    @Override public boolean useShapeForLightOcclusion(BlockState state) { return true; }
    @Override public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) { return 15; }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(STABLE, true);
        return state;
    }
    @Override public BlockState func_176203_a(int meta) { return defaultBlockState().setValue(STABLE, (meta & 1) != 0); }
    @Override public int func_176201_c(BlockState state) { return (state.getValue(STABLE) ? 1 : 0); }
    @Override public BlockPath getDoubleSlab() { return (BlockPath)Registry.BLOCK.get(new ResourceLocation("millenaire", pathName)); }
    @Override public BlockPathSlab getSingleSlab() { return (BlockPathSlab)Registry.BLOCK.get(new ResourceLocation("millenaire", pathName + "_slab")); }
    @Override public boolean isFullPath() { return true; }
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) { return new ItemStack(this); }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) { return List.of(new ItemStack(this)); }
    @Override public String getSpecialName(ItemStack stack) { return getDescriptionId(); }
    @Override public int itemMetadata(BlockState state) { return 0; }
}
