package org.millenaire.common.utilities;

import java.util.*;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.village.Building;

/** Plans a whole inn shipment on detached stacks before touching live inventories. */
public final class MerchantStockTransfer {
    private MerchantStockTransfer() {}

    public record Result(boolean success, String reason, Map<InvItem,Integer> outward,
                         Map<InvItem,Integer> inward) {
        static Result blocked(String reason) {
            return new Result(false, reason, Map.of(), Map.of());
        }
    }
    private record Slot(Container chest, int index, ItemStack original) {}

    private static List<Slot> snapshot(Building building) {
        List<Slot> slots = new ArrayList<>();
        Set<Container> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Point p : building.getResManager().chests) {
            // Keep the existing selected-inn block-entity lookup semantics.
            // Do not require both villages to be active: distant trade is
            // intentional. A lookup which cannot resolve a chest is not empty stock.
            Container chest = p.getMillChest(building.world);
            if (chest == null) return null;
            if (!seen.add(chest)) continue;
            for (int i=0; i<chest.getContainerSize(); i++)
                slots.add(new Slot(chest,i,chest.getItem(i).copy()));
        }
        return slots.isEmpty() ? null : slots;
    }
    private static List<ItemStack> contents(List<Slot> slots) {
        return slots.stream().map(Slot::original).filter(s -> !s.isEmpty()).map(ItemStack::copy).toList();
    }
    private static List<ItemStack> state(List<Slot> slots, boolean clear) {
        List<ItemStack> result = new ArrayList<>(slots.size());
        for (Slot slot : slots) result.add(clear ? ItemStack.EMPTY : slot.original().copy());
        return result;
    }
    private static boolean insert(List<Slot> slots, List<ItemStack> plan, ItemStack cargo) {
        int remaining = cargo.getCount();
        // First fill compatible partial stacks, then empty slots. Exact tags,
        // damage and item variants survive; no legacy item reconstruction.
        for (int pass=0; pass<2 && remaining>0; pass++) {
            for (int i=0; i<slots.size() && remaining>0; i++) {
                Slot slot = slots.get(i);
                ItemStack existing = plan.get(i);
                if (pass==0 && (existing.isEmpty() || !ItemStack.isSameItemSameTags(existing,cargo))) continue;
                if (pass==1 && !existing.isEmpty()) continue;
                if (!slot.chest().canPlaceItem(slot.index(),cargo)) continue;
                int cap = Math.min(slot.chest().getMaxStackSize(),cargo.getMaxStackSize());
                int amount = Math.min(remaining, Math.max(0,cap-existing.getCount()));
                if (amount<=0) continue;
                if (existing.isEmpty()) {
                    ItemStack stack=cargo.copy(); stack.setCount(amount); plan.set(i,stack);
                } else existing.grow(amount);
                remaining-=amount;
            }
        }
        return remaining==0;
    }
    private static boolean same(ItemStack a,ItemStack b) {
        return a.isEmpty() ? b.isEmpty() : !b.isEmpty() && a.getCount()==b.getCount()
                && ItemStack.isSameItemSameTags(a,b);
    }
    private static boolean unchanged(List<Slot> slots) {
        for (Slot s : slots) if (!same(s.original(),s.chest().getItem(s.index()))) return false;
        return true;
    }
    private static void write(List<Slot> slots,List<ItemStack> plan) {
        for (int i=0;i<slots.size();i++) {
            Slot slot=slots.get(i);
            if (!same(slot.original(),plan.get(i))) slot.chest().setItem(slot.index(),plan.get(i).copy());
        }
        for (int i=0;i<slots.size();i++) {
            Slot slot=slots.get(i);
            if (!same(slot.chest().getItem(slot.index()),plan.get(i)))
                throw new IllegalStateException("Merchant chest write did not preserve the planned stack");
        }
    }
    private static Map<InvItem,Integer> totals(List<ItemStack> cargo) {
        Map<InvItem,Integer> result=new HashMap<>();
        for (ItemStack stack:cargo) result.merge(InvItem.createInvItem(stack),stack.getCount(),Math::addExact);
        return Map.copyOf(result);
    }

    public static Result transfer(Building source,Building destination,boolean exchange) {
        if (source==null || destination==null || source==destination || source.world!=destination.world
                || source.world.isClientSide) return Result.blocked("INVALID_INNS");
        List<Slot> left=snapshot(source),right=snapshot(destination);
        if (left==null || right==null) return Result.blocked("STOCK_UNLOADED_OR_MISSING");
        Set<Container> stores=Collections.newSetFromMap(new IdentityHashMap<>());
        for (Slot slot:left) stores.add(slot.chest());
        for (Slot slot:right) if (stores.contains(slot.chest())) return Result.blocked("SHARED_CHEST");
        for (Slot slot:right) stores.add(slot.chest());

        List<ItemStack> outgoing=contents(left),incoming=exchange ? contents(right) : List.of();
        // Both inventories are logically emptied before an exchange. A one-way
        // move preserves pre-existing destination cargo.
        List<ItemStack> leftPlan=state(left,true),rightPlan=state(right,exchange);
        for (ItemStack stack:outgoing)
            if (!insert(right,rightPlan,stack)) return Result.blocked("DESTINATION_CAPACITY");
        for (ItemStack stack:incoming)
            if (!insert(left,leftPlan,stack)) return Result.blocked("SOURCE_CAPACITY");
        if (!unchanged(left) || !unchanged(right)) return Result.blocked("STOCK_CHANGED");
        Map<InvItem,Integer> outward=totals(outgoing),inward=totals(incoming);
        try (var tx=new GoodsTransaction(new ArrayList<>(stores),List.of(),null,() -> {
            source.invalidateInventoryCache(); destination.invalidateInventoryCache();
        })) {
            write(left,leftPlan); write(right,rightPlan);
            for (Container store:stores) store.setChanged();
            source.invalidateInventoryCache(); destination.invalidateInventoryCache();
            tx.commit();
        }
        return new Result(true,"OK",outward,inward);
    }
}
