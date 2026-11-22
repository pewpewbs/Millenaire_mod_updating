package org.millenaire.common.entity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class TileEntityMockBanner extends BannerBlockEntity {
    public TileEntityMockBanner(BlockPos pos, BlockState state) { super(pos, state, ((AbstractBannerBlock)state.getBlock()).getColor()); }
    @Override public BlockEntityType<?> getType() { return MillBlockEntities.MOCK_BANNER; }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("Base", getBaseColor().getId()); }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Base", Tag.TAG_ANY_NUMERIC)) {
            ItemStack stack = new ItemStack(Items.WHITE_BANNER);
            stack.getOrCreateTagElement("BlockEntityTag").put("Patterns", tag.getList("Patterns", Tag.TAG_COMPOUND).copy());
            if (getCustomName() != null) stack.setHoverName(getCustomName());
            super.fromItem(stack, DyeColor.byId(tag.getInt("Base")));
        }
    }
    /** Building-plan entry point: the supplied banner may belong to any village culture. */
    public void func_175112_a(ItemStack stack, boolean useItemColor) {
        CompoundTag data = stack.getTagElement("BlockEntityTag");
        DyeColor color = data != null && data.contains("Base", Tag.TAG_ANY_NUMERIC) ? DyeColor.byId(data.getInt("Base"))
            : stack.getItem() instanceof BannerItem banner ? banner.getColor() : getBaseColor();
        super.fromItem(stack, color); setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override public ItemStack getItem() {
        ItemStack stack = new ItemStack(getBlockState().getBlock());
        stack.addTagElement("BlockEntityTag", saveWithoutMetadata());
        if (getCustomName() != null) stack.setHoverName(getCustomName());
        return stack;
    }
    public static List<ItemStack> drops(BlockState state, LootContext.Builder builder) {
        BlockEntity entity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        return List.of(entity instanceof TileEntityMockBanner banner ? banner.getItem() : new ItemStack(state.getBlock()));
    }
}
