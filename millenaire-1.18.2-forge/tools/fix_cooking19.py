from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');p=r/'src/main/java/org/millenaire/common/goal/generic/GoalGenericCooking.java'
b=r/'migration/before-audit19'/p.relative_to(r);b.parent.mkdir(parents=True,exist_ok=True);assert not b.exists();shutil.copy2(p,b)
s=p.read_text(encoding='utf-8');start=s.index('    private void performAction_firepit(');end=s.index('    @Override\n    public boolean validateGoal()',start)
s=s[:start]+'''    private ItemStack fillCookingInput(Building dest, MillVillager villager, ItemStack current, int slotLimit) {
        ItemStack ingredient = org.millenaire.common.item.LegacyItemStacks.stack(
                this.itemToCook.getItem(), 1, this.itemToCook.meta);
        if (!current.isEmpty() && !ItemStack.isSameItemSameTags(current, ingredient)) return current;
        long available = (long)dest.countGoods(this.itemToCook) + villager.countInv(this.itemToCook);
        if (current.isEmpty() && available < this.minimumToCook) return current;
        int capacity = Math.min(slotLimit, ingredient.getMaxStackSize()) - current.getCount();
        int requested = (int)Math.max(0L, Math.min((long)capacity, available));
        if (requested == 0) return current;
        int taken = dest.takeGoods(this.itemToCook, requested);
        if (taken < requested) taken += villager.takeFromInv(this.itemToCook, requested - taken);
        if (taken == 0) return current;
        ItemStack result = current.isEmpty() ? ingredient : current.copy();
        result.setCount(current.getCount() + taken);
        return result;
    }

    private ItemStack storeCookingOutput(Building dest, ItemStack output) {
        if (output.isEmpty() || output.hasTag()) return output;
        int stored = dest.storeGoods(output.getItem(), output.getDamageValue(), output.getCount());
        if (stored == 0) return output;
        ItemStack remainder = output.copy();
        remainder.shrink(stored);
        return remainder;
    }

    private void performAction_firepit(Building dest, TileEntityFirePit firepit, MillVillager villager) {
        for (int slot = 0; slot < 3; slot++) {
            ItemStack old = firepit.inputs.getStackInSlot(slot);
            ItemStack updated = fillCookingInput(dest, villager, old, firepit.inputs.getSlotLimit(slot));
            if (updated != old) firepit.inputs.setStackInSlot(slot, updated);
        }
        for (int slot = 0; slot < 3; slot++) {
            ItemStack old = firepit.outputs.getStackInSlot(slot);
            ItemStack updated = storeCookingOutput(dest, old);
            if (updated != old) firepit.outputs.setStackInSlot(slot, updated);
        }
    }

    private void performAction_furnace(Building dest, FurnaceBlockEntity furnace, MillVillager villager) {
        ItemStack oldInput = furnace.getItem(0);
        ItemStack input = fillCookingInput(dest, villager, oldInput, furnace.getMaxStackSize());
        if (input != oldInput) furnace.setItem(0, input);
        ItemStack oldOutput = furnace.getItem(2);
        ItemStack output = storeCookingOutput(dest, oldOutput);
        if (output != oldOutput) furnace.setItem(2, output);
    }

'''+s[end:];p.write_text(s,encoding='utf-8')
print('Cooking now consumes real stock/NPC inputs and retains unstored output')
