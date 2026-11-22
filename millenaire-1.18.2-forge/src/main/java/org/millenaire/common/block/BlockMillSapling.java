package org.millenaire.common.block;

import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.SaplingGrowTreeEvent;
import net.minecraftforge.eventbus.api.Event;
import org.millenaire.common.world.*;

public class BlockMillSapling extends BushBlock implements BonemealableBlock, LegacyVariantBlock {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 1);
    private static final VoxelShape SHAPE = Block.box(1.6, 0, 1.6, 14.4, 12.8, 14.4);
    private final EnumMillWoodType type;
    protected BlockMillSapling(String name, EnumMillWoodType type) {
        super(Properties.of(Material.PLANT).noCollission().randomTicks().instabreak().sound(SoundType.GRASS));
        this.type = type; setRegistryName(name);
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(STAGE); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) { return List.of(new ItemStack(this)); }
    @Override public boolean isValidBonemealTarget(BlockGetter level, BlockPos pos, BlockState state, boolean client) { return true; }
    @Override public boolean isBonemealSuccess(Level level, Random random, BlockPos pos, BlockState state) { return random.nextFloat() < 0.45f; }
    @Override public void performBonemeal(ServerLevel level, Random random, BlockPos pos, BlockState state) { grow(level, pos, state, random); }
    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, Random random) {
        if (level.isAreaLoaded(pos, 1) && level.getMaxLocalRawBrightness(pos.above()) >= 9 && random.nextInt(7) == 0) grow(level, pos, state, random);
    }
    public void grow(Level level, BlockPos pos, BlockState state, Random random) {
        if (level.isClientSide) return;
        if (state.getValue(STAGE) == 0) level.setBlock(pos, state.cycle(STAGE), 4);
        else generateTree(level, pos, state, random);
    }
    public void generateTree(Level level, BlockPos pos, BlockState state, Random random) {
        if (!(level instanceof ServerLevel server)) return;
        SaplingGrowTreeEvent event = new SaplingGrowTreeEvent(level, random, pos);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.getResult() == Event.Result.DENY) return;
        MillTreeGenerator generator = switch (type) {
            case APPLETREE -> new WorldGenAppleTree(true);
            case OLIVETREE -> new WorldGenOliveTree(true);
            case PISTACHIO -> new WorldGenPistachio(true);
            case CHERRY -> new WorldGenCherry(true);
            case SAKURA -> new WorldGenSakura(true);
        };
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 4);
        boolean grown = event.getFeature() != null
            ? event.getFeature().value().place(server, server.getChunkSource().getGenerator(), random, pos)
            : generator.func_180709_b(level, random, pos);
        if (!grown) level.setBlock(pos, state, 4);
    }
    public boolean isTypeAt(Level level, BlockPos pos, EnumMillWoodType type) { return this.type == type && level.getBlockState(pos).is(this); }
    @Override public int func_176201_c(BlockState state) { return state.getValue(STAGE); }
    @Override public BlockState func_176203_a(int meta) { return defaultBlockState().setValue(STAGE, meta & 1); }
    @Override public int itemMetadata(BlockState state) { return 0; }
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public void initModel() { net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this, net.minecraft.client.renderer.RenderType.cutout()); }
    public enum EnumMillWoodType {
        APPLETREE, OLIVETREE, PISTACHIO, CHERRY, SAKURA;
        public MaterialColor getMapColor() { return MaterialColor.WOOD; }
    }
}
