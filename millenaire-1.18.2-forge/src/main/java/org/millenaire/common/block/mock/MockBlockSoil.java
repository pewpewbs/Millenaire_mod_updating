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

public class MockBlockSoil extends Block implements IMetaBlockName, LegacyVariantBlock {
    public static final EnumProperty<CropType> CROPTYPE = EnumProperty.create("croptype", CropType.class);
    public MockBlockSoil(String name) { super(Properties.of(Material.STONE).strength(-1.0F, 0.0F)); setRegistryName(name); registerDefaultState(stateDefinition.any().setValue(CROPTYPE, CropType.values()[0])); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(CROPTYPE); }
    @Override public int func_176201_c(BlockState state) { return state.getValue(CROPTYPE).meta; }
    @Override public BlockState func_176203_a(int meta) { CropType value = CropType.fromMeta(meta); return defaultBlockState().setValue(CROPTYPE, value == null ? CropType.values()[0] : value); }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) { return List.of(LegacyItemStacks.stack(this, 1, func_176201_c(state))); }
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) { return LegacyItemStacks.stack(this, 1, func_176201_c(state)); }
    @Override public String getSpecialName(ItemStack stack) { return "tile.millenaire." + getRegistryName().getPath() + "." + func_176203_a(LegacyItemStacks.metadata(stack)).getValue(CROPTYPE).getSerializedName(); }
    @Override public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items) { for (CropType value : CropType.values()) items.add(LegacyItemStacks.stack(this, 1, value.meta)); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() != MillItems.NEGATION_WAND) return InteractionResult.PASS;
        if (!level.isClientSide) {
            int meta = func_176201_c(state) + 1; if (CropType.fromMeta(meta) == null) meta = 0;
            level.setBlockAndUpdate(pos, state.setValue(CROPTYPE, CropType.fromMeta(meta)));
            Mill.proxy.sendLocalChat(player, 'a', CropType.fromMeta(meta).name);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public void initModel() { net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this, net.minecraft.client.renderer.RenderType.solid()); }
    public static enum CropType implements StringRepresentable
    {
        WHEAT(0, "soil"),
        RICE(1, "ricesoil"),
        TURMERIC(2, "turmericsoil"),
        SUGAR_CANE(3, "sugarcanesoil"),
        POTATO(4, "potatosoil"),
        NETHER_WART(5, "netherwartsoil"),
        GRAPE(6, "vinesoil"),
        MAIZE(7, "maizesoil"),
        CACAO(8, "cacaospot"),
        CARROT(9, "carrotsoil"),
        FLOWER(10, "flowersoil"),
        COTTON(11, "cottonsoil");

        public final int meta;
        public final String name;

        public static CropType fromMeta(int meta) {
            for (CropType t : CropType.values()) {
                if (t.meta != meta) continue;
                return t;
            }
            return null;
        }

        private CropType(int m, String n2) {
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

