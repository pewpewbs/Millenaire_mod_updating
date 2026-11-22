package org.millenaire.common.block;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.millenaire.common.utilities.BlockItemUtilities;

public class BlockFruitLeaves extends LeavesBlock implements BonemealableBlock, LegacyVariantBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);
    private final BlockMillSapling.EnumMillWoodType type;
    private final ResourceLocation fruitRL, saplingRL;
    public BlockFruitLeaves(String name, BlockMillSapling.EnumMillWoodType type, ResourceLocation sapling, ResourceLocation fruit) {
        super(Properties.copy(Blocks.OAK_LEAVES));
        this.type = type; saplingRL = sapling; fruitRL = fruit; setRegistryName(name);
        registerDefaultState(defaultBlockState().setValue(AGE, 0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(AGE); }
    @Override public boolean isRandomlyTicking(BlockState state) { return true; }
    @Override public boolean isValidBonemealTarget(BlockGetter level, BlockPos pos, BlockState state, boolean client) { return !isMaxAge(state); }
    @Override public boolean isBonemealSuccess(Level level, Random random, BlockPos pos, BlockState state) { return true; }
    @Override public void performBonemeal(ServerLevel level, Random random, BlockPos pos, BlockState state) { level.setBlock(pos, state.setValue(AGE, 3), 2); }
    public int getMaxAge() { return 3; }
    protected int getAge(BlockState state) { return state.getValue(AGE); }
    protected IntegerProperty getAgeProperty() { return AGE; }
    public boolean isMaxAge(BlockState state) { return state.getValue(AGE) == 3; }
    public BlockState withAge(int age) { return defaultBlockState().setValue(AGE, age); }
    public BlockMillSapling.EnumMillWoodType getType() { return type; }
    @Override public int itemMetadata(BlockState state) { return 0; }
    // Legacy bits: age=0..3, nondecaying=4, pending decay check=8.
    @Override public BlockState func_176203_a(int meta) {
        return defaultBlockState().setValue(AGE, meta & 3).setValue(PERSISTENT, (meta & 4) != 0).setValue(DISTANCE, (meta & 8) != 0 ? 7 : 1);
    }
    @Override public int func_176201_c(BlockState state) { return state.getValue(AGE) | (state.getValue(PERSISTENT) ? 4 : 0) | (state.getValue(DISTANCE) == 7 ? 8 : 0); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!isMaxAge(state)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            BlockItemUtilities.checkForHarvestTheft(player, pos);
            popResource(level, pos.below(), new ItemStack(Registry.ITEM.get(fruitRL)));
            level.setBlockAndUpdate(pos, state.setValue(AGE, 0));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
        if (tool != null && (tool.is(Items.SHEARS) || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0)) return List.of(new ItemStack(this));
        int fortune = tool == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, tool);
        int chance = Math.max(10, 20 - (fortune > 0 ? 2 << Math.min(fortune, 4) : 0));
        return builder.getLevel().random.nextInt(chance) == 0 ? List.of(new ItemStack(Registry.ITEM.get(saplingRL))) : List.of();
    }
    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, Random random) {
        super.randomTick(state, level, pos, random);
        if (!level.getBlockState(pos).is(this)) return;
        long time = level.getDayTime() % 24000L;
        int target = time > 3000 && time < 5000 ? 1 : time > 5000 && time < 6000 ? 2 : time > 6000 && time < 10000 ? 3 : 0;
        int previous = (target + 3) % 4;
        if (state.getValue(AGE) != previous) return;
        Deque<BlockPos> pending = new ArrayDeque<>(); Set<BlockPos> visited = new HashSet<>(); pending.add(pos);
        for (int count = 0; !pending.isEmpty() && count < 10000; count++) {
            BlockPos next = pending.removeLast();
            if (!visited.add(next) || !level.hasChunkAt(next)) continue;
            BlockState current = level.getBlockState(next);
            if (!current.is(this) || current.getValue(AGE) != previous) continue;
            level.setBlockAndUpdate(next, current.setValue(AGE, target));
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos neighbor = next.offset(dx, dy, dz); if (!visited.contains(neighbor)) pending.add(neighbor);
            }
        }
    }
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public void initModel() { net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this, net.minecraft.client.renderer.RenderType.cutoutMipped()); }
}
