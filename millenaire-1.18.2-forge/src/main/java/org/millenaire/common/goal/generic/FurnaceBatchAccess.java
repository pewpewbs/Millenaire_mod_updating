package org.millenaire.common.goal.generic;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import org.millenaire.common.entity.TileEntityFirePit;

/** Evaluate whether more output can accumulate without a villager visit. */
final class FurnaceBatchAccess {
    private FurnaceBatchAccess() {}
    static boolean furnaceOutputDue(Level level, FurnaceBlockEntity furnace) {
        ItemStack output = furnace.getItem(2);
        if (output.isEmpty()) return false;
        int capacity = FurnaceBatchPolicy.capacity(furnace.getMaxStackSize(), output.getMaxStackSize());
        var state = furnace.getBlockState();
        boolean lit = state.hasProperty(AbstractFurnaceBlock.LIT) && state.getValue(AbstractFurnaceBlock.LIT);
        return outputDue(level, furnace.getItem(0), furnace.getItem(1), output, capacity, lit);
    }
    static boolean firepitOutputDue(Level level, TileEntityFirePit firepit, int slot) {
        ItemStack output = firepit.outputs.getStackInSlot(slot);
        if (output.isEmpty()) return false;
        int capacity = FurnaceBatchPolicy.capacity(firepit.outputs.getSlotLimit(slot), output.getMaxStackSize());
        return outputDue(level, firepit.inputs.getStackInSlot(slot), firepit.fuel.getStackInSlot(0),
                output, capacity, firepit.getBurnTime() > 0);
    }
    private static boolean outputDue(Level level, ItemStack input, ItemStack fuel, ItemStack output,
                                     int capacity, boolean lit) {
        if (FurnaceBatchPolicy.outputDue(output.getCount(), capacity, true)) return true;
        if (input.isEmpty() || (!lit && TileEntityFirePit.fuelTime(fuel) <= 0)) return true;
        // An incompatible or oversized result prevents further accumulation.
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(input), level);
        if (recipe.isEmpty()) return true;
        ItemStack next = recipe.get().assemble(new SimpleContainer(input));
        return next.isEmpty() || !ItemStack.isSameItemSameTags(next, output)
                || next.getCount() + output.getCount() > capacity;
    }
}
