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

public class MockBlockAnimalSpawn extends Block implements IMetaBlockName, LegacyVariantBlock {
    public static final EnumProperty<Creature> CREATURE = EnumProperty.create("creature", Creature.class);
    public MockBlockAnimalSpawn(String name) { super(Properties.of(Material.STONE).strength(-1.0F, 0.0F)); setRegistryName(name); registerDefaultState(stateDefinition.any().setValue(CREATURE, Creature.values()[0])); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(CREATURE); }
    @Override public int func_176201_c(BlockState state) { return state.getValue(CREATURE).meta; }
    @Override public BlockState func_176203_a(int meta) { Creature value = Creature.fromMeta(meta); return defaultBlockState().setValue(CREATURE, value == null ? Creature.values()[0] : value); }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) { return List.of(LegacyItemStacks.stack(this, 1, func_176201_c(state))); }
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) { return LegacyItemStacks.stack(this, 1, func_176201_c(state)); }
    @Override public String getSpecialName(ItemStack stack) { return "tile.millenaire." + getRegistryName().getPath() + "." + func_176203_a(LegacyItemStacks.metadata(stack)).getValue(CREATURE).getSerializedName(); }
    @Override public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items) { for (Creature value : Creature.values()) items.add(LegacyItemStacks.stack(this, 1, value.meta)); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() != MillItems.NEGATION_WAND) return InteractionResult.PASS;
        if (!level.isClientSide) {
            int meta = func_176201_c(state) + 1; if (Creature.fromMeta(meta) == null) meta = 0;
            level.setBlockAndUpdate(pos, state.setValue(CREATURE, Creature.fromMeta(meta)));
            Mill.proxy.sendLocalChat(player, 'a', Creature.fromMeta(meta).name);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public void initModel() { net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this, net.minecraft.client.renderer.RenderType.solid()); }
    public static enum Creature implements StringRepresentable
    {
        COW(0, "cow"),
        PIG(1, "pig"),
        SHEEP(2, "sheep"),
        CHICKEN(3, "chicken"),
        SQUID(4, "squid"),
        WOLF(5, "wolf"),
        POLARBEAR(6, "polarbear");

        public final int meta;
        public final String name;

        public static Creature fromMeta(int meta) {
            for (Creature t : Creature.values()) {
                if (t.meta != meta) continue;
                return t;
            }
            return null;
        }

        public static int getMetaFromName(String name) {
            for (Creature creature : Creature.values()) {
                if (!creature.name.equalsIgnoreCase(name)) continue;
                return creature.meta;
            }
            return -1;
        }

        private Creature(int m, String n2) {
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
            return "Animal Spawn (" + this.name + ")";
        }
    }
}

