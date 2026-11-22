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

public class MockBlockMarker extends Block implements IMetaBlockName, LegacyVariantBlock {
    public static final EnumProperty<Type> VARIANT = EnumProperty.create("variant", Type.class);
    public MockBlockMarker(String name) { super(Properties.of(Material.STONE).strength(-1.0F, 0.0F).noOcclusion()); setRegistryName(name); registerDefaultState(stateDefinition.any().setValue(VARIANT, Type.values()[0])); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(VARIANT); }
    @Override public int func_176201_c(BlockState state) { return state.getValue(VARIANT).meta; }
    @Override public BlockState func_176203_a(int meta) { Type value = Type.fromMeta(meta); return defaultBlockState().setValue(VARIANT, value == null ? Type.values()[0] : value); }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) { return List.of(LegacyItemStacks.stack(this, 1, func_176201_c(state))); }
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) { return LegacyItemStacks.stack(this, 1, func_176201_c(state)); }
    @Override public String getSpecialName(ItemStack stack) { return "tile.millenaire." + getRegistryName().getPath() + "." + func_176203_a(LegacyItemStacks.metadata(stack)).getValue(VARIANT).getSerializedName(); }
    @Override public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items) { for (Type value : Type.values()) items.add(LegacyItemStacks.stack(this, 1, value.meta)); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() != MillItems.NEGATION_WAND) return InteractionResult.PASS;
        if (!level.isClientSide) {
            int meta = func_176201_c(state) + 1; if (Type.fromMeta(meta) == null) meta = 0;
            level.setBlockAndUpdate(pos, state.setValue(VARIANT, Type.fromMeta(meta)));
            Mill.proxy.sendLocalChat(player, 'a', Type.fromMeta(meta).name);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public void initModel() { net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this, net.minecraft.client.renderer.RenderType.cutoutMipped()); }

    @Override public boolean canSustainPlant(BlockState state, BlockGetter world, BlockPos pos, Direction direction, IPlantable plantable) {
        PlantType plant = plantable.getPlantType(world, pos.relative(direction));
        return state.getValue(VARIANT) == Type.PRESERVE_GROUND && (plant == PlantType.DESERT || plant == PlantType.PLAINS);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return state.getValue(VARIANT) == Type.PRESERVE_GROUND ? Shapes.block() : Block.box(0,0,0,16,1,16); }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return state.getValue(VARIANT) == Type.PRESERVE_GROUND ? Shapes.block() : Shapes.empty(); }
    @Override public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) { return state.getValue(VARIANT) == Type.PRESERVE_GROUND ? Shapes.block() : Block.box(0,0,0,16,1,16); }
    @Override public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) { return 1; }
    public void animateTick(BlockState stateIn, Level worldIn, BlockPos pos, Random rand) {
        int color = 0xFFFFFF;
        switch ((Type)((Object)stateIn.getValue(VARIANT))) {
            case PRESERVE_GROUND: {
                return;
            }
            case SLEEPING_POS: {
                color = 14680244;
                break;
            }
            case SELLING_POS: {
                color = 65484;
                break;
            }
            case CRAFTING_POS: {
                color = 1158400;
                break;
            }
            case DEFENDING_POS: {
                color = 0xFF0000;
                break;
            }
            case SHELTER_POS: {
                color = 8323127;
                break;
            }
            case PATH_START_POS: {
                color = 721110;
                break;
            }
            case LEISURE_POS: {
                color = 0xF08800;
                break;
            }
            case STALL: {
                color = 0x969600;
                break;
            }
            case BRICK_SPOT: {
                color = 0x870000;
                break;
            }
            case HEALING_SPOT: {
                color = 53760;
                break;
            }
            case FISHING_SPOT: {
                color = 120;
            }
        }
        double r = (double)(color >> 16 & 0xFF) / 255.0;
        double g = (double)(color >> 8 & 0xFF) / 255.0;
        double b = (double)(color & 0xFF) / 255.0;
        worldIn.addParticle(ParticleTypes.ENTITY_EFFECT, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, r, g, b);
    }

    public static enum Type implements StringRepresentable
    {
        PRESERVE_GROUND(0, "preserveground"),
        SLEEPING_POS(1, "sleepingpos"),
        SELLING_POS(2, "sellingpos"),
        CRAFTING_POS(3, "craftingpos"),
        DEFENDING_POS(4, "defendingpos"),
        SHELTER_POS(5, "shelterpos"),
        PATH_START_POS(6, "pathstartpos"),
        LEISURE_POS(7, "leisurepos"),
        STALL(8, "stall"),
        BRICK_SPOT(9, "brickspot"),
        HEALING_SPOT(10, "healingspot"),
        FISHING_SPOT(11, "fishingspot");

        public final int meta;
        public final String name;

        public static Type fromMeta(int meta) {
            for (Type t : Type.values()) {
                if (t.meta != meta) continue;
                return t;
            }
            return null;
        }

        public static int getMetaFromName(String name) {
            for (Type type : Type.values()) {
                if (!type.name.equalsIgnoreCase(name)) continue;
                return type.meta;
            }
            return -1;
        }

        private Type(int m, String n2) {
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
            return "Marker Pos (" + this.name + ")";
        }
    }
}

