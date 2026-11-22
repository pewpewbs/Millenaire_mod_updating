from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');base=r/'src/main/java/org/millenaire/common';bk=r/'migration/before-crafting25';bk.mkdir(exist_ok=False)
def edit(n,fn):
 p=base/n;q=bk/n;q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,q);p.write_text(fn(p.read_text()),encoding='utf-8')
(base/'utilities/GoodsTransaction.java').write_text('''package org.millenaire.common.utilities;
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
''',encoding='utf-8')
def resident(s):
 marker='    public void addToInv(InvItem iv, int nb) {';assert marker in s
 return s.replace(marker,'''    public void restoreGoodsInventory(java.util.Map<InvItem,Integer> snapshot) {
        this.inventory.clear();this.inventory.putAll(snapshot);
        this.updateVillagerRecord();this.updateClothTexturePath();
    }

'''+marker)
edit('entity/MillVillager.java',resident)
def building(s):
 marker='   public int storeGoods(InvItem item, int toPut) {';assert marker in s
 return s.replace(marker,'''   public org.millenaire.common.utilities.GoodsTransaction beginGoodsTransaction(MillVillager villager) {
      return org.millenaire.common.utilities.GoodsTransaction.capture(this, villager);
   }

'''+marker)
edit('village/Building.java',building)
def crafting(s):
 s=s.replace('villager.countInv(item) + b.countGoods(item)', '(long)villager.countInv(item) + b.countGoods(item)')
 a=s.index('        Building dest = villager.getGoalBuildingDest();',s.index('public boolean performAction'));b=s.index('        if (this.sound != null)',a)
 s=s[:a]+'''        Building dest = villager.getGoalBuildingDest();
        if (dest == null || villager.level.isClientSide || !this.validQuantities()) return true;
        if (!this.isDestPossibleSpecific(villager, dest)) return true;
        try (org.millenaire.common.utilities.GoodsTransaction transaction = dest.beginGoodsTransaction(villager)) {
            for (var entry : this.input.entrySet()) {
                int required = entry.getValue();
                int taken = villager.takeFromInv(entry.getKey(), required);
                if (taken < 0 || taken > required) throw new IllegalStateException("Invalid resident ingredient withdrawal");
                int remaining = required - taken;
                if (remaining > 0 && dest.takeGoods(entry.getKey(), remaining) != remaining) return true;
            }
            for (var entry : this.output.entrySet()) {
                if (dest.storeGoods(entry.getKey(), entry.getValue()) != entry.getValue()) return true;
            }
            transaction.commit();
        }
'''+s[b:]
 marker='    @Override\n    public boolean validateGoal() {'
 s=s.replace(marker,'''    private boolean validQuantities() {
        if (this.output.isEmpty()) return false;
        for (Integer amount : this.input.values()) if (amount == null || amount <= 0) return false;
        for (Integer amount : this.output.values()) if (amount == null || amount <= 0) return false;
        return true;
    }

'''+marker)
 s=s.replace('if (this.output.isEmpty()) {','if (!this.validQuantities()) {')
 # private helper has single-line check and is not affected.
 s=s.replace('Generic crafting goals require at least one output.', 'Generic crafting goals require output and strictly positive quantities.')
 return s
edit('goal/generic/GoalGenericCrafting.java',crafting)
print('Crafting all-or-nothing with exact resident/container/output snapshots')
