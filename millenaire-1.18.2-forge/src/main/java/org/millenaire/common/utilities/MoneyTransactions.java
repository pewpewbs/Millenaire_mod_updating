package org.millenaire.common.utilities;
import java.util.*;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import org.millenaire.common.item.MillItems;

public final class MoneyTransactions {
    private MoneyTransactions() {}
    private static int value(Item item) { return item==MillItems.DENIER?1:item==MillItems.DENIER_ARGENT?64:item==MillItems.DENIER_OR?4096:0; }
    private static long purse(ItemStack stack) {
        if(!stack.hasTag())return 0;
        var tag=stack.getTag();
        for(String key:new String[]{"ml_Purse_DENIER","ml_Purse_DENIERargent","ml_Purse_DENIERor"})
            if(tag.contains(key)&&!tag.contains(key,3))throw new IllegalArgumentException("Invalid purse number type");
        long a=tag.getInt("ml_Purse_DENIER"),b=tag.getInt("ml_Purse_DENIERargent"),c=tag.getInt("ml_Purse_DENIERor");
        if(a<0||b<0||c<0)throw new IllegalArgumentException("Negative purse balance");
        long total=a+b*64+c*4096;
        if(total>Integer.MAX_VALUE)throw new IllegalArgumentException("Purse balance overflow");
        return total;
    }
    public static long count(Container inventory) {
        long total=0;for(int i=0;i<inventory.getContainerSize();i++) {
            ItemStack stack=inventory.getItem(i);if(stack.isEmpty())continue;
            if(stack.getItem()==MillItems.PURSE) { if(stack.getCount()!=1)throw new IllegalArgumentException("Stacked purses");total+=purse(stack); }
            else total+=(long)value(stack.getItem())*stack.getCount();
        }return total;
    }
    public static boolean change(Container inventory, long delta) {
        final long total;try{total=Math.addExact(count(inventory),delta);}catch(IllegalArgumentException|ArithmeticException bad){return false;}
        if(total<0)return false;
        List<ItemStack> planned=new ArrayList<>();List<Integer> purses=new ArrayList<>();
        for(int i=0;i<inventory.getContainerSize();i++) {
            ItemStack stack=inventory.getItem(i).copy();
            if(!stack.isEmpty()&&stack.getItem()==MillItems.PURSE)purses.add(i);
            if(value(stack.getItem())>0) { if(stack.hasTag())return false;stack=ItemStack.EMPTY; }
            planned.add(stack);
        }
        long remaining=total;
        if(!purses.isEmpty()) {
            for(int slot:purses) {
                int amount=(int)Math.min(Integer.MAX_VALUE,remaining);remaining-=amount;
                var tag=planned.get(slot).getOrCreateTag();tag.putInt("ml_Purse_DENIER",amount%64);tag.putInt("ml_Purse_DENIERargent",amount/64%64);tag.putInt("ml_Purse_DENIERor",amount/4096);tag.putInt("ml_Purse_rand",1);
            }
        }
        int slots=inventory instanceof Inventory?36:inventory.getContainerSize();
        for(Item coin:new Item[]{MillItems.DENIER_OR,MillItems.DENIER_ARGENT,MillItems.DENIER}) {
            long number=remaining/value(coin);remaining%=value(coin);
            for(int i=0;i<slots&&number>0;i++) if(planned.get(i).isEmpty()) {
                ItemStack probe=new ItemStack(coin);if(!inventory.canPlaceItem(i,probe))continue;
                int amount=(int)Math.min(number,Math.min(inventory.getMaxStackSize(),probe.getMaxStackSize()));
                planned.set(i,new ItemStack(coin,amount));number-=amount;
            }
            if(number!=0)return false;
        }
        try(GoodsTransaction tx=new GoodsTransaction(List.of(inventory),List.of(),null,()->{})) {
            for(int i=0;i<planned.size();i++) if(!ItemStack.matches(planned.get(i),inventory.getItem(i)))inventory.setItem(i,planned.get(i));
            if(count(inventory)!=total)return false;
            inventory.setChanged();tx.commit();return true;
        }
    }
}
