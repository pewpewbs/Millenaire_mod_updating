from pathlib import Path
s=Path('work/check_crafting25.py').read_text(encoding='utf-8-sig').replace('crafting25-check','transactions26-check').replace('Crafting25Check','Transactions26Check')
insert=r'''
  ((net.minecraftforge.registries.ForgeRegistry)net.minecraftforge.registries.ForgeRegistries.ITEMS).unfreeze();
  var iu=net.minecraft.core.Registry.ITEM.getClass().getMethod("unfreeze");iu.setAccessible(true);iu.invoke(net.minecraft.core.Registry.ITEM);
  org.millenaire.common.item.MillItems.DENIER=new org.millenaire.common.item.ItemMill("fixture_copper");
  org.millenaire.common.item.MillItems.DENIER_ARGENT=new org.millenaire.common.item.ItemMill("fixture_silver");
  org.millenaire.common.item.MillItems.DENIER_OR=new org.millenaire.common.item.ItemMill("fixture_gold");
  org.millenaire.common.item.MillItems.PURSE=new org.millenaire.common.item.ItemPurse("fixture_purse");
  for(var item:List.of(org.millenaire.common.item.MillItems.DENIER,org.millenaire.common.item.MillItems.DENIER_ARGENT,org.millenaire.common.item.MillItems.DENIER_OR,org.millenaire.common.item.MillItems.PURSE))net.minecraftforge.registries.ForgeRegistries.ITEMS.register(item);
  var copper=org.millenaire.common.item.MillItems.DENIER;var gold=org.millenaire.common.item.MillItems.DENIER_OR;
  var wallet=new SimpleContainer(4);wallet.setItem(0,new ItemStack(copper,64));
  ok(org.millenaire.common.utilities.MoneyTransactions.change(wallet,-1)&&org.millenaire.common.utilities.MoneyTransactions.count(wallet)==63,"exact debit");
  var beforeMoney=wallet.getItem(0).copy();ok(!org.millenaire.common.utilities.MoneyTransactions.change(wallet,-64)&&ItemStack.matches(beforeMoney,wallet.getItem(0)),"insufficient balance unchanged");
  var tight=new SimpleContainer(1);tight.setItem(0,new ItemStack(gold,1));ok(!org.millenaire.common.utilities.MoneyTransactions.change(tight,-1)&&tight.getItem(0).getItem()==gold,"no room for change unchanged");
  var purse=new ItemStack(org.millenaire.common.item.MillItems.PURSE);purse.getOrCreateTag().putString("custom","keep");wallet.clearContent();wallet.setItem(0,purse);wallet.setItem(1,new ItemStack(gold,1));
  ok(org.millenaire.common.utilities.MoneyTransactions.change(wallet,-1)&&org.millenaire.common.utilities.MoneyTransactions.count(wallet)==4095,"purse and loose coins debit");
  ok(wallet.getItem(0).getTag().getString("custom").equals("keep"),"purse custom NBT retained");
  wallet.getItem(0).getTag().putInt("ml_Purse_DENIER",-1);ok(!org.millenaire.common.utilities.MoneyTransactions.change(wallet,100),"invalid purse rejected");
  wallet.clearContent();wallet.setItem(0,new ItemStack(org.millenaire.common.item.MillItems.PURSE));wallet.setItem(1,new ItemStack(org.millenaire.common.item.MillItems.PURSE));
  ok(org.millenaire.common.utilities.MoneyTransactions.change(wallet,(long)Integer.MAX_VALUE+123)&&org.millenaire.common.utilities.MoneyTransactions.count(wallet)==(long)Integer.MAX_VALUE+123,"multiple purses long balance");
  ok(org.millenaire.common.utilities.MillCommonUtilities.countMoney(wallet)==Integer.MAX_VALUE,"legacy displayed count saturates");
  var store=make(Store.class);store.world=make(net.minecraft.server.level.ServerLevel.class);store.chest=new SimpleContainer(3);store.pit=new ItemStackHandler(3);store.chest.setItem(0,new ItemStack(Items.WHEAT,5));
  wallet.clearContent();wallet.setItem(0,new ItemStack(copper,64));
  int traded=org.millenaire.common.utilities.TradeTransactions.trade(store,wallet,wheat,3,2,true,false,false);
  ok(traded==3&&store.countGoods(wheat)==2&&org.millenaire.common.utilities.MoneyTransactions.count(wallet)==58,"buy exact goods/payment");
  traded=org.millenaire.common.utilities.TradeTransactions.trade(store,wallet,wheat,3,1,false,false,false);
  ok(traded==3&&store.countGoods(wheat)==5&&org.millenaire.common.utilities.MoneyTransactions.count(wallet)==61,"sell exact goods/payment");
  tight.setItem(0,new ItemStack(gold,1));int beforeStock=store.countGoods(wheat);
  ok(org.millenaire.common.utilities.TradeTransactions.trade(store,tight,wheat,1,1,true,false,false)==0&&store.countGoods(wheat)==beforeStock&&tight.getItem(0).is(gold),"failed buy restores both sides");
  wallet.clearContent();wallet.setItem(0,new ItemStack(Items.WHEAT,2));
  for(int i=0;i<3;i++)store.chest.setItem(i,new ItemStack(Items.STONE,64));
  ok(org.millenaire.common.utilities.TradeTransactions.trade(store,wallet,wheat,2,1,false,false,false)==0&&wallet.getItem(0).getCount()==2,"full store restores seller goods");
  store.chest.clearContent();store.throwOutput=true;
  try{org.millenaire.common.utilities.TradeTransactions.trade(store,wallet,wheat,2,1,false,false,false);throw new AssertionError("missing injected failure");}catch(IllegalStateException expected){}
  ok(wallet.getItem(0).getCount()==2&&store.countGoods(wheat)==0,"trade exception rollback");
  System.out.println("PASS money debit/credit/insufficient/capacity/purses/long balances, trade buy/sell/full storage/exception rollback");
'''
s=s.replace('  System.out.println("PASS crafting exact success',insert+'  System.out.println("PASS crafting exact success')
Path('work/check_transactions26.py').write_text(s,encoding='utf-8')
s=Path('work/check_barriers23.py').read_text()
insert='''
  world.blocks.put(origin,Blocks.OAK_FENCE_GATE.defaultBlockState());
  check(evaluator.getBlockPathType(world,0,0,0)==BlockPathTypes.WALKABLE_DOOR,"closed gate route allowed for opener");
  world.blocks.put(origin,Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.OPEN,true));
  check(evaluator.getBlockPathType(world,0,0,0)==BlockPathTypes.WALKABLE,"open gate route");
  world.blocks.put(origin,Blocks.WHEAT.defaultBlockState());world.blocks.put(origin.below(),Blocks.FARMLAND.defaultBlockState());
  check(evaluator.getBlockPathType(world,0,0,0)==BlockPathTypes.DANGER_OTHER,"cropland route penalty");
  System.out.println("PASS gate route nodes and cropland penalty");
'''
s=s.replace('  System.out.println("PASS barriers',insert+'  System.out.println("PASS barriers')
Path('work/check_routes26.py').write_text(s,encoding='utf-8')
