package org.millenaire.common.utilities;
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
