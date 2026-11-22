import net.minecraft.world.item.*;import net.minecraft.world.level.block.*;import java.util.*;
public class InventoryHarness {
 final Map<InvItem,Integer> inventory=new HashMap<>();
 void updateVillagerRecord(){} void updateClothTexturePath(){}
 void addToInv(Item item,int meta,int count){inventory.merge(InvItem.createInvItem(item,meta),count,Integer::sum);}
    public int countInv(InvItem key) {
        if (key.block == Blocks.OAK_LOG && key.meta == -1) {
            int count = 0;
            for (org.millenaire.common.block.WoodVariant wood : org.millenaire.common.block.WoodVariant.values())
                count += this.inventory.getOrDefault(InvItem.createInvItem(wood.log()), 0);
            return count;
        }
        if (key.meta == -1) {
            int count = 0;
            for (var entry : this.inventory.entrySet()) {
                InvItem item = entry.getKey();
                if (org.millenaire.common.item.LegacyItemStacks.matches(
                        org.millenaire.common.item.LegacyItemStacks.stack(item.getItem(), 1, item.meta), key.item, -1))
                    count += entry.getValue();
            }
            return count;
        }
        if (this.inventory.containsKey(key)) {
            return this.inventory.get(key);
        }
        return 0;
    }
    public int takeFromInv(Item item, int meta, int nb) {
        if (nb <= 0) return 0;
        if (item == Blocks.OAK_LOG.asItem() && meta == -1) {
            int total = 0;
            for (org.millenaire.common.block.WoodVariant wood : org.millenaire.common.block.WoodVariant.values()) {
                if (total == nb) break;
                total += this.takeFromInv(wood.log().asItem(), 0, nb - total);
            }
            return total;
        }
        InvItem key = InvItem.createInvItem(item, meta);
        if (this.inventory.containsKey(key)) {
            nb = Math.min(nb, this.inventory.get(key));
            this.inventory.put(key, this.inventory.get(key) - nb);
            this.updateVillagerRecord();
            this.updateClothTexturePath();
            return nb;
        }
        return 0;
    }
    public int takeFromBuilding(Building building, Item item, int meta, int nb) {
        if (nb <= 0) return 0;
        if (item == Blocks.OAK_LOG.asItem() && meta == -1) {
            int total = 0;
            for (org.millenaire.common.block.WoodVariant wood : org.millenaire.common.block.WoodVariant.values()) {
                if (total == nb) break;
                Item log = wood.log().asItem();
                int taken = building.takeGoods(log, 0, nb - total);
                if (taken > 0) this.addToInv(log, 0, taken);
                total += taken;
            }
            return total;
        }
        nb = building.takeGoods(item, meta, nb);
        this.addToInv(item, meta, nb);
        return nb;
    }
}
class InvItem {
 final Item item;final Block block;final int meta;
 InvItem(Item item,int meta){this.item=item;this.meta=meta;this.block=Block.byItem(item);}
 Item getItem(){return item;}
 static InvItem createInvItem(Item item,int meta){return new InvItem(item,meta);}
 static InvItem createInvItem(Block block){return new InvItem(block.asItem(),0);}
 public int hashCode(){return item.hashCode()*31+meta;}
 public boolean equals(Object obj){return obj instanceof InvItem i&&i.item==item&&i.meta==meta;}
}
class Building {
 final Map<Item,Integer> goods=new HashMap<>();
 int takeGoods(Item item,int meta,int nb){int take=Math.min(nb,goods.getOrDefault(item,0));goods.put(item,goods.getOrDefault(item,0)-take);return take;}
}
