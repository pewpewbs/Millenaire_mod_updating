package org.millenaire.common.block;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
public class BlockAlchemistExplosive extends Block {
    private static final int EXPLOSION_RADIUS = 32;
    public BlockAlchemistExplosive(String name) { super(Properties.of(Material.STONE).strength(2.0F, 6.0F).requiresCorrectToolForDrops()); setRegistryName("millenaire", name); }
    @Override public void onBlockExploded(BlockState state, Level level, BlockPos center, Explosion explosion) {
        if (level.isClientSide) return;
        BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
        level.setBlockAndUpdate(center, Blocks.AIR.defaultBlockState());
        for (int dy = EXPLOSION_RADIUS; dy >= -EXPLOSION_RADIUS; dy--) {
            int y = center.getY() + dy;
            if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) continue;
            for (int dx = -EXPLOSION_RADIUS; dx <= EXPLOSION_RADIUS; dx++) for (int dz = -EXPLOSION_RADIUS; dz <= EXPLOSION_RADIUS; dz++) {
                if (dx * dx + dy * dy + dz * dz > EXPLOSION_RADIUS * EXPLOSION_RADIUS) continue;
                target.set(center.getX() + dx, y, center.getZ() + dz);
                if (!level.getBlockState(target).isAir()) level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
            }
        }
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) { return List.of(new ItemStack(this)); }
    @OnlyIn(Dist.CLIENT) public void initModel() { net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this, net.minecraft.client.renderer.RenderType.solid()); }
}
