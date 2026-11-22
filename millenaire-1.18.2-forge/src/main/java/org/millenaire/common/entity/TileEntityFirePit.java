package org.millenaire.common.entity;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.RangedWrapper;
import org.millenaire.common.block.BlockFirePit;
import org.millenaire.common.ui.firepit.ContainerFirePit;

public class TileEntityFirePit extends BlockEntity implements MenuProvider {
    private final ItemStackHandler items = new ItemStackHandler(7) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            if (slot < 3) return level != null && isFirePitBurnable(level, stack);
            return slot == 3 && (fuelTime(stack) > 0 || stack.is(Items.BUCKET));
        }
        @Override protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };
    public final IItemHandlerModifiable inputs = new RangedWrapper(items, 0, 3);
    public final IItemHandlerModifiable fuel = new RangedWrapper(items, 3, 4);
    public final IItemHandlerModifiable outputs = new RangedWrapper(items, 4, 7);
    private LazyOptional<IItemHandler> allCapability = LazyOptional.of(() -> items);
    private LazyOptional<IItemHandler> inputCapability = LazyOptional.of(() -> inputs);
    private LazyOptional<IItemHandler> fuelCapability = LazyOptional.of(() -> fuel);
    private LazyOptional<IItemHandler> outputCapability = LazyOptional.of(() -> outputs);
    private int[] cookTimes = new int[3];
    private int burnTime, totalBurnTime;

    public TileEntityFirePit(BlockPos pos, BlockState state) { super(MillBlockEntities.FIRE_PIT, pos, state); }
    public static int fuelTime(ItemStack stack) { return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING); }
    private static ItemStack result(Level level, ItemStack input) {
        if (level == null || input.isEmpty()) return ItemStack.EMPTY;
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(input), level)
            .map(recipe -> recipe.assemble(new SimpleContainer(input))).orElse(ItemStack.EMPTY);
    }
    public static boolean isFirePitBurnable(Level level, ItemStack stack) {
        ItemStack cooked = result(level, stack);
        return !cooked.isEmpty() && (stack.isEdible() || cooked.isEdible());
    }
    private boolean canSmelt(int index) {
        ItemStack input = inputs.getStackInSlot(index);
        if (!isFirePitBurnable(level, input)) return false;
        ItemStack cooked = result(level, input), output = outputs.getStackInSlot(index);
        return output.isEmpty() || ItemHandlerHelper.canItemStacksStack(cooked, output)
            && cooked.getCount() + output.getCount() <= Math.min(output.getMaxStackSize(), outputs.getSlotLimit(index));
    }
    public void smeltItem(int index) {
        if (!canSmelt(index)) return;
        ItemStack input = inputs.getStackInSlot(index), cooked = result(level, input).copy();
        ItemStack output = outputs.getStackInSlot(index).copy();
        if (output.isEmpty()) output = cooked; else output.grow(cooked.getCount());
        outputs.setStackInSlot(index, output);
        inputs.extractItem(index, 1, false);
        setChanged();
    }
    public void tick() {
        if (level == null || level.isClientSide) return;
        boolean wasBurning = burnTime > 0;
        boolean changed = wasBurning;
        if (wasBurning) burnTime--;
        for (int i = 0; i < 3; i++) {
            ItemStack fuelStack = fuel.getStackInSlot(0);
            if ((!fuelStack.isEmpty() || burnTime > 0) && !inputs.getStackInSlot(i).isEmpty()) {
                if (burnTime == 0 && canSmelt(i)) {
                    totalBurnTime = burnTime = fuelTime(fuelStack);
                    if (burnTime > 0) {
                        ItemStack remaining = fuelStack.getContainerItem();
                        fuel.extractItem(0, 1, false);
                        if (fuel.getStackInSlot(0).isEmpty() && !remaining.isEmpty()) fuel.setStackInSlot(0, remaining);
                        changed = true;
                    }
                }
                if (burnTime > 0 && canSmelt(i)) {
                    if (++cookTimes[i] >= 200) { cookTimes[i] = 0; smeltItem(i); }
                    changed = true;
                } else if (cookTimes[i] != 0) { cookTimes[i] = 0; changed = true; }
            } else if (burnTime == 0 && cookTimes[i] > 0) { cookTimes[i] = Math.max(0, cookTimes[i] - 2); changed = true; }
        }
        if (wasBurning != (burnTime > 0) && getBlockState().getBlock() instanceof BlockFirePit)
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(BlockFirePit.LIT, burnTime > 0));
        if (changed) setChanged();
    }
    public void dropAll() {
        if (level == null || level.isClientSide) return;
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (!stack.isEmpty()) { Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack); items.setStackInSlot(i, ItemStack.EMPTY); }
        }
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (!isRemoved() && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
            return (side == null ? allCapability : side == Direction.UP ? inputCapability : side == Direction.DOWN ? outputCapability : fuelCapability).cast();
        return super.getCapability(capability, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); allCapability.invalidate(); inputCapability.invalidate(); fuelCapability.invalidate(); outputCapability.invalidate(); }
    @Override public void reviveCaps() {
        super.reviveCaps(); allCapability = LazyOptional.of(() -> items); inputCapability = LazyOptional.of(() -> inputs);
        fuelCapability = LazyOptional.of(() -> fuel); outputCapability = LazyOptional.of(() -> outputs);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        CompoundTag inventory = tag.getCompound("Inventory").copy(); inventory.putInt("Size", 7); items.deserializeNBT(inventory);
        burnTime = Math.max(0, tag.getInt("BurnTime")); totalBurnTime = Math.max(0, tag.getInt("TotalBurnTime"));
        cookTimes = Arrays.copyOf(tag.getIntArray("CookTime"), 3);
        for (int i = 0; i < 3; i++) cookTimes[i] = Math.max(0, Math.min(199, cookTimes[i]));
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        CompoundTag inventory = items.serializeNBT(); inventory.remove("Size"); tag.put("Inventory", inventory);
        tag.putInt("BurnTime", burnTime); tag.putInt("TotalBurnTime", totalBurnTime); tag.putIntArray("CookTime", cookTimes);
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    public int getBurnTime() { return burnTime; }
    public int getTotalBurnTime() { return totalBurnTime; }
    public int getCookTime(int index) { return cookTimes[index]; }
    public void setBurnTime(int time) { burnTime = time; }
    public void setTotalBurnTime(int time) { totalBurnTime = time; }
    public void setCookTime(int index, int time) { cookTimes[index] = time; }
    @Override public Component getDisplayName() { return new TranslatableComponent(getBlockState().getBlock().getDescriptionId()); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new ContainerFirePit(id, inventory, this); }
}
