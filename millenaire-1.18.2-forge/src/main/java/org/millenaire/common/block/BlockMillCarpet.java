package org.millenaire.common.block;
import java.util.List;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
public class BlockMillCarpet extends CarpetBlock implements LegacyVariantBlock {
    public BlockMillCarpet(String name) { super(Properties.of(Material.CLOTH_DECORATION).sound(SoundType.WOOL)); setRegistryName("millenaire", name); }
    @Override public BlockState func_176203_a(int metadata) { return defaultBlockState(); }
    @Override public int func_176201_c(BlockState state) { return 0; }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) { return List.of(new ItemStack(this)); }
    @OnlyIn(Dist.CLIENT) public void initModel() { net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this, net.minecraft.client.renderer.RenderType.solid()); }
}
