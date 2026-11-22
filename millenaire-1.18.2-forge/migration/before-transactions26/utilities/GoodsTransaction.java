package org.millenaire.common.utilities;
import java.util.*;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.village.Building;

/** Server-thread transaction: preserve exact slots, tags and resident inventory on failure. */
public final class GoodsTransaction implements AutoCloseable {
    private final IdentityHashMap<Container,List<ItemStack>> containers=new IdentityHashMap<>();
    private final IdentityHashMap<IItemHandlerModifiable,List<ItemStack>> handlers=new IdentityHashMap<>();
    private final MillVillager villager;
    private final Map<InvItem,Integer> inventory;
    private final Runnable invalidate;
    private boolean committed;
    public GoodsTransaction(List<Container> stores, List<IItemHandlerModifiable> outputs, MillVillager resident, Runnable invalidate) {
        this.villager=resident;this.inventory=new HashMap<>(resident.inventory);this.invalidate=invalidate;
        for(Container store:stores) if(store!=null && !containers.containsKey(store)) {
            List<ItemStack> copy=new ArrayList<>();for(int i=0;i<store.getContainerSize();i++)copy.add(store.getItem(i).copy());containers.put(store,copy);
        }
        for(IItemHandlerModifiable output:outputs) if(output!=null && !handlers.containsKey(output)) {
            List<ItemStack> copy=new ArrayList<>();for(int i=0;i<output.getSlots();i++)copy.add(output.getStackInSlot(i).copy());handlers.put(output,copy);
        }
    }
    public static GoodsTransaction capture(Building building, MillVillager resident) {
        List<Container> stores=new ArrayList<>();List<IItemHandlerModifiable> outputs=new ArrayList<>();
        for(Point p:building.getResManager().chests) stores.add(p.getMillChest(building.world));
        for(Point p:building.getResManager().furnaces) stores.add(p.getFurnace(building.world));
        for(Point p:building.getResManager().firepits) {
            var pit=p.getFirePit(building.world);if(pit!=null)outputs.add(pit.outputs);
        }
        return new GoodsTransaction(stores,outputs,resident,building::invalidateInventoryCache);
    }
    public void commit() { committed=true; }
    @Override public void close() {
        if(committed)return;
        for(var entry:containers.entrySet()) {
            for(int i=0;i<entry.getValue().size();i++)entry.getKey().setItem(i,entry.getValue().get(i).copy());
            entry.getKey().setChanged();
        }
        for(var entry:handlers.entrySet()) for(int i=0;i<entry.getValue().size();i++)entry.getKey().setStackInSlot(i,entry.getValue().get(i).copy());
        villager.restoreGoodsInventory(inventory);invalidate.run();
    }
}
