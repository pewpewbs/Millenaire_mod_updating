from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');base=r/'src/main/java/org/millenaire/common';bk=r/'migration/before-transactions26';bk.mkdir(exist_ok=False)
def edit(n,fn):
 p=base/n;q=bk/n;q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,q);p.write_text(fn(p.read_text(encoding='utf-8-sig')),encoding='utf-8')
(base/'utilities/MoneyTransactions.java').write_text('''package org.millenaire.common.utilities;
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
        var tag=stack.getTag();long a=tag.getInt("ml_Purse_DENIER"),b=tag.getInt("ml_Purse_DENIERargent"),c=tag.getInt("ml_Purse_DENIERor");
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
''',encoding='utf-8')
edit('utilities/GoodsTransaction.java',lambda s:s.replace('new HashMap<>(resident.inventory)','resident == null ? new HashMap<>() : new HashMap<>(resident.inventory)').replace('villager.restoreGoodsInventory(inventory);','if(villager!=null) villager.restoreGoodsInventory(inventory);'))
def common(s):
 a=s.index('   public static void changeMoney(');b=s.index('   public static int countChestItems(',a)
 s=s[:a]+'''   public static boolean changeMoney(Container chest, int toChange, Player player) {
      if (player != null && player.level.isClientSide) return false;
      boolean changed = MoneyTransactions.change(chest, toChange);
      if (changed && player != null && MoneyTransactions.count(chest) >= 4096) MillAdvancements.CRESUS.grant(player);
      return changed;
   }

'''+s[b:]
 a=s.index('   public static int countMoney(');b=s.index('   public static boolean deleteDir(',a)
 s=s[:a]+'''   public static int countMoney(Container chest) {
      try { return (int)Math.min(Integer.MAX_VALUE, MoneyTransactions.count(chest)); }
      catch (IllegalArgumentException invalid) { return 0; }
   }

'''+s[b:];return s
edit('utilities/MillCommonUtilities.java',common)
(base/'utilities/TradeTransactions.java').write_text('''package org.millenaire.common.utilities;
import java.util.*;
import net.minecraft.world.Container;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.village.Building;

public final class TradeTransactions {
    private TradeTransactions() {}
    public static int trade(Building store, Container inventory, InvItem item, int requested, int price, boolean buying, boolean generated, boolean donation) {
        if(requested<=0||requested>64||price<0||store.world.isClientSide)return 0;
        // Try smaller quantities too, preserving partial-stack trading without partial transactions.
        for(int n=requested;n>0;n--) {
            long amount=(long)price*n;
            try(GoodsTransaction goods=store.beginGoodsTransaction(null);
                GoodsTransaction player=new GoodsTransaction(List.of(inventory),List.of(),null,()->{})) {
                if(buying) {
                    if(!generated&&store.takeGoods(item,n)!=n)continue;
                    if(!MoneyTransactions.change(inventory,-amount))continue;
                    if(MillCommonUtilities.putItemsInChest(inventory,item.getItem(),item.meta,n)!=n)continue;
                } else {
                    if(WorldUtilities.getItemsFromChest(inventory,item.getItem(),item.meta,n)!=n)continue;
                    if(store.storeGoods(item,n)!=n)continue;
                    if(!donation&&!MoneyTransactions.change(inventory,amount))continue;
                }
                goods.commit();player.commit();return n;
            }
        }
        return 0;
    }
}
''',encoding='utf-8')
def trade(s):
 a=s.index('                  nbItems = MillCommonUtilities.putItemsInChest');b=s.index('                  if (this.building.getTownHall().controlledBy',a)
 s=s[:a]+'''                  nbItems = org.millenaire.common.utilities.TradeTransactions.trade(this.building, player.getInventory(), good.item, nbItems, price, true, good.autoGenerate, false);
                  if (nbItems == 0) return;

'''+s[b:]
 a=s.index('                  nbItems = this.building.storeGoods');b=s.index('                  if (this.building.getTownHall().controlledBy',a)
 s=s[:a]+'''                  nbItems = org.millenaire.common.utilities.TradeTransactions.trade(this.building, player.getInventory(), good.item, nbItems, good.getCalculatedBuyingPrice(this.building, player), false, false, profile.donationActivated);
                  if (nbItems == 0) return;

'''+s[b:]
 a=s.index('               nbItemsx = MillCommonUtilities.putItemsInChest');b=s.index('               Mill.getMillWorld(player.level)',a)
 s=s[:a]+'''               nbItemsx = org.millenaire.common.utilities.TradeTransactions.trade(this.merchant.getHouse(), player.getInventory(), good.item, nbItemsx, price, true, false, false);
               if (nbItemsx == 0) return;
'''+s[b:]
 return s
edit('ui/ContainerTrade.java',trade)
def gui(s):
 for method in ['hireExtend','hireHire']:
  a=s.index('    public static void '+method+'(');b=s.index('\n    public static',a+5);part=s[a:b]
  line='        MillCommonUtilities.changeMoney((Container)player.getInventory(), -villager.getHireCost(player), player);'
  assert line in part;part=part.replace(line+'\n','');pos=part.index('\n')+1
  part=part[:pos]+'''        int cost = villager.getHireCost(player);
        if (cost < 0 || !MillCommonUtilities.changeMoney(player.getInventory(), -cost, player)) return;
'''+part[pos:];s=s[:a]+part+s[b:]
 s=s.replace('        chief.getTownHall().buildingsBought.add(planKey);\n        MillCommonUtilities.changeMoney((Container)player.getInventory(), -plan.price, player);','        if (plan.price < 0 || !MillCommonUtilities.changeMoney(player.getInventory(), -plan.price, player)) return;\n        chief.getTownHall().buildingsBought.add(planKey);')
 for tag in ['cropplanting_','huntingdrop_']:
  old='        profile.setTag("'+tag+'" + value);\n        MillCommonUtilities.changeMoney((Container)player.getInventory(), -512, player);';assert old in s
  s=s.replace(old,'        if (!MillCommonUtilities.changeMoney(player.getInventory(), -512, player)) return;\n        profile.setTag("'+tag+'" + value);')
 old='''            MillCommonUtilities.changeMoney((Container)player.getInventory(), -128, player);
            player.getInventory().add(ItemParchment.createParchmentForVillage(chief.getTownHall()));''';assert old in s
 s=s.replace(old,'''            try (org.millenaire.common.utilities.GoodsTransaction transaction = new org.millenaire.common.utilities.GoodsTransaction(java.util.List.of(player.getInventory()), java.util.List.of(), null, () -> {})) {
                if (!org.millenaire.common.utilities.MoneyTransactions.change(player.getInventory(), -128)) return;
                ItemStack scroll = ItemParchment.createParchmentForVillage(chief.getTownHall());
                if (!player.getInventory().add(scroll) || !scroll.isEmpty()) return;
                transaction.commit();
            }''')
 return s
edit('ui/GuiActions.java',gui)
def quest(s):
 marker='    public String completeStep(Player player, MillVillager villager) {';assert marker in s
 s=s.replace(marker,marker+'''\n        if (this.getCurrentStep().rewardMoney > 0 && !MillCommonUtilities.changeMoney(player.getInventory(), this.getCurrentStep().rewardMoney, player))
            return org.millenaire.common.utilities.LanguageUtilities.string("ui.nospace");''')
 s=s.replace('            MillCommonUtilities.changeMoney((Container)player.getInventory(), this.getCurrentStep().rewardMoney, player);\n','')
 return s
edit('quest/QuestInstance.java',quest)
print('Staged money, transactional trading, payment-before-service, protected scroll purchase')
