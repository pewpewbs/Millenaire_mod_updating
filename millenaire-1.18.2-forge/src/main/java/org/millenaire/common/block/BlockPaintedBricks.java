package org.millenaire.common.block;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.millenaire.common.advancements.MillAdvancements;
import org.millenaire.common.item.ItemPaintBucket;

public class BlockPaintedBricks extends Block implements IPaintedBlock, LegacyVariantBlock {
    public static final BooleanProperty TOP_FRIEZE = BooleanProperty.create("top_frieze");
    public static final BooleanProperty BOTTOM_FRIEZE = BooleanProperty.create("bottom_frieze");
    private final String baseBlockName;
    private final DyeColor colour;

    public BlockPaintedBricks(String name, DyeColor colour) {
        super(Properties.of(Material.STONE, colour.getMaterialColor()).strength(1.5F, 6.0F).requiresCorrectToolForDrops());
        baseBlockName = name;
        this.colour = colour;
        setRegistryName("millenaire", name + "_" + getColorName(colour));
        registerDefaultState(defaultBlockState().setValue(TOP_FRIEZE, false).setValue(BOTTOM_FRIEZE, false));
    }
    public static String getColorName(DyeColor colour) { return colour == DyeColor.LIGHT_GRAY ? "silver" : colour.getSerializedName(); }
    public static DyeColor getColourFromBlockState(BlockState state) { return state.getBlock() instanceof IPaintedBlock painted ? painted.getDyeColour() : null; }
    public static BlockState getBlockStateWithColour(BlockState input, DyeColor colour) {
        if (!(input.getBlock() instanceof IPaintedBlock painted)) return input;
        var family = MillBlocks.PAINTED_BRICK_MAP.get(painted.getBlockType());
        Block target = family == null ? null : family.get(colour);
        if (target == null) return input;
        BlockState result = target.defaultBlockState();
        for (Property<?> property : input.getProperties()) if (result.hasProperty(property)) result = copyProperty(result, input, property);
        return result;
    }
    private static <T extends Comparable<T>> BlockState copyProperty(BlockState target, BlockState source, Property<T> property) { return target.setValue(property, source.getValue(property)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(TOP_FRIEZE, BOTTOM_FRIEZE); }
    public int friezePriority(BlockGetter level, BlockPos pos, BlockState ourState, BlockState otherState, Direction side) {
        if (getColourFromBlockState(otherState) == colour) return otherState.isFaceSturdy(level, pos, side) ? 0 : 3;
        if (otherState.getCollisionShape(level, pos).isEmpty()) return 5;
        if (otherState.getBlock() instanceof IronBarsBlock) return 2;
        if (otherState.getBlock() instanceof IPaintedBlock) return 1;
        return 10;
    }
    public BlockState func_176221_a(BlockState state, BlockGetter level, BlockPos pos) {
        int top = friezePriority(level, pos.above(), state, level.getBlockState(pos.above()), Direction.DOWN);
        int bottom = friezePriority(level, pos.below(), state, level.getBlockState(pos.below()), Direction.UP);
        return state.setValue(TOP_FRIEZE, top > bottom).setValue(BOTTOM_FRIEZE, top <= bottom && bottom > 0);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return func_176221_a(defaultBlockState(), context.getLevel(), context.getClickedPos()); }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) { return func_176221_a(state, level, pos); }
    @Override public String getBlockType() { return baseBlockName; }
    @Override public DyeColor getDyeColour() { return colour; }
    @Override public int func_176201_c(BlockState state) { return 0; }
    @Override public BlockState func_176203_a(int metadata) { return defaultBlockState(); }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) { return List.of(new ItemStack(this)); }
    @Override @OnlyIn(Dist.CLIENT) public void initModel() {
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this, net.minecraft.client.renderer.RenderType.cutoutMipped());
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack bucket = player.getItemInHand(hand);
        if (!(bucket.getItem() instanceof ItemPaintBucket paint) || colour == paint.getColour()) return InteractionResult.PASS;
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), bucket)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        var pending = new ArrayDeque<BlockPos>();
        var visited = new HashSet<BlockPos>();
        pending.add(pos);
        int painted = 0;
        while (!pending.isEmpty()) {
            BlockPos target = pending.removeLast();
            if (!visited.add(target) || !level.hasChunkAt(target)) continue;
            BlockState old = level.getBlockState(target);
            if (getColourFromBlockState(old) != colour || !level.mayInteract(player, target)
                    || !player.mayUseItemAt(target, hit.getDirection(), bucket)) continue;
            BlockState replacement = getBlockStateWithColour(old, paint.getColour());
            if (replacement == old || !level.setBlock(target, replacement, 3)) continue;
            painted++;
            for (Direction direction : Direction.values()) pending.add(target.relative(direction));
        }
        if (painted == 0) return InteractionResult.PASS;
        if (!player.getAbilities().instabuild) {
            if (painted >= bucket.getMaxDamage() - bucket.getDamageValue()) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            else bucket.hurtAndBreak(painted, player, entity -> entity.broadcastBreakEvent(hand));
        }
        MillAdvancements.RAINBOW.grant(player);
        return InteractionResult.CONSUME;
    }
}
