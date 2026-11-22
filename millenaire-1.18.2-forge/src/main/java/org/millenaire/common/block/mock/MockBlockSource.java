package org.millenaire.common.block.mock;
import java.util.List;
import java.util.Random;
import net.minecraft.core.*;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.PlantType;
import org.millenaire.common.block.LegacyVariantBlock;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.item.*;

public class MockBlockSource extends Block implements IMetaBlockName, LegacyVariantBlock {
    public static final EnumProperty<Resource> RESOURCE = EnumProperty.create("resource", Resource.class);
    public MockBlockSource(String name) { super(Properties.of(Material.STONE).strength(-1.0F, 0.0F)); setRegistryName(name); registerDefaultState(stateDefinition.any().setValue(RESOURCE, Resource.values()[0])); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(RESOURCE); }
    @Override public int func_176201_c(BlockState state) { return state.getValue(RESOURCE).meta; }
    @Override public BlockState func_176203_a(int meta) { Resource value = Resource.fromMeta(meta); return defaultBlockState().setValue(RESOURCE, value == null ? Resource.values()[0] : value); }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) { return List.of(LegacyItemStacks.stack(this, 1, func_176201_c(state))); }
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) { return LegacyItemStacks.stack(this, 1, func_176201_c(state)); }
    @Override public String getSpecialName(ItemStack stack) { return "tile.millenaire." + getRegistryName().getPath() + "." + func_176203_a(LegacyItemStacks.metadata(stack)).getValue(RESOURCE).getSerializedName(); }
    @Override public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items) { for (Resource value : Resource.values()) items.add(LegacyItemStacks.stack(this, 1, value.meta)); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() != MillItems.NEGATION_WAND) return InteractionResult.PASS;
        if (!level.isClientSide) {
            int meta = func_176201_c(state) + 1; if (Resource.fromMeta(meta) == null) meta = 0;
            level.setBlockAndUpdate(pos, state.setValue(RESOURCE, Resource.fromMeta(meta)));
            Mill.proxy.sendLocalChat(player, 'a', Resource.fromMeta(meta).name);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public void initModel() { net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this, net.minecraft.client.renderer.RenderType.solid()); }
    public static enum Resource implements StringRepresentable
    {
        STONE(0, "stone"),
        SAND(1, "sand"),
        SANDSTONE(2, "sandstone"),
        CLAY(3, "clay"),
        GRAVEL(4, "gravel"),
        GRANITE(5, "granite"),
        DORITE(6, "diorite"),
        ANDERITE(7, "andesite"),
        SNOW(8, "snow"),
        ICE(9, "ice"),
        RED_SANDSTONE(10, "redsandstone"),
        QUARTZ(11, "quartz");

        public final int meta;
        public final String name;

        public static Resource fromMeta(int meta) {
            for (Resource t : Resource.values()) {
                if (t.meta != meta) continue;
                return t;
            }
            return null;
        }

        private Resource(int m, String n2) {
            this.meta = m;
            this.name = n2;
        }

        public int getMetadata() {
            return this.meta;
        }

        public String getSerializedName() {
            return this.name;
        }

        public String toString() {
            return "Source Block (" + this.name + ")";
        }
    }
}

