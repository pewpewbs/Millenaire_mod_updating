package org.millenaire.common.utilities;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Requires the Forge/Minecraft development runtime classpath. */
public final class MerchantStockTransferSmoke {
    private static void check(boolean condition,String label) {
        if (!condition) throw new AssertionError(label);
        System.out.println("PASS " + label);
    }
    private static MerchantStockTransfer.Result transfer(Container a,Container b,boolean swap) {
        return MerchantStockTransfer.transferStores(List.of(a),List.of(b),swap,()->{});
    }
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var left=new SimpleContainer(1);
        var right=new SimpleContainer(1);
        left.setItem(0,new ItemStack(Items.COBBLESTONE,64));
        right.setItem(0,new ItemStack(Items.DIRT,64));
        var blocked=transfer(left,right,false);
        check(!blocked.success() && blocked.reason().equals("DESTINATION_CAPACITY")
                && left.getItem(0).is(Items.COBBLESTONE) && left.getItem(0).getCount()==64
                && right.getItem(0).is(Items.DIRT) && right.getItem(0).getCount()==64
                && blocked.outward().isEmpty(),"full destination defers without mutation or accounting");

        var swapped=transfer(left,right,true);
        check(swapped.success() && left.getItem(0).is(Items.DIRT) && right.getItem(0).is(Items.COBBLESTONE)
                && left.getItem(0).getCount()==64 && right.getItem(0).getCount()==64
                && swapped.outward().values().stream().mapToInt(Integer::intValue).sum()==64
                && swapped.inward().values().stream().mapToInt(Integer::intValue).sum()==64,
                "full warehouses can swap without a temporary empty warehouse");

        left.setItem(0,new ItemStack(Items.COD,20));
        right.setItem(0,new ItemStack(Items.COD,44));
        check(transfer(left,right,false).success() && left.getItem(0).isEmpty()
                && right.getItem(0).getCount()==64,"compatible partial stack filled exactly");

        var tagged=new ItemStack(Items.PAPER,1);
        tagged.getOrCreateTag().putString("test_owner","left");
        left.setItem(0,tagged.copy());
        var other=tagged.copy();
        other.getOrCreateTag().putString("test_owner","right");
        right.setItem(0,other);
        check(!transfer(left,right,false).success() && left.getItem(0).getTag().getString("test_owner").equals("left"),
                "different tags do not merge");
        check(transfer(left,right,true).success()
                && left.getItem(0).getTag().getString("test_owner").equals("right")
                && right.getItem(0).getTag().getString("test_owner").equals("left"),"exchange preserves tags");

        var small=new SimpleContainer(1);
        var large=new SimpleContainer(2);
        small.setItem(0,new ItemStack(Items.STONE,64));
        large.setItem(0,new ItemStack(Items.DIRT,64));
        large.setItem(1,new ItemStack(Items.COD,64));
        var tooLarge=transfer(small,large,true);
        check(!tooLarge.success() && tooLarge.reason().equals("SOURCE_CAPACITY")
                && small.getItem(0).is(Items.STONE) && large.getItem(1).is(Items.COD),
                "asymmetric exchange rejected before writes");

        check(!MerchantStockTransfer.transferStores(List.of(left),List.of(left),true,()->{}).success(),
                "shared container rejected");
        check(!MerchantStockTransfer.transferStores(List.of(left),List.of(),false,()->{}).success(),
                "missing destination rejected");

        var src=new SimpleContainer(1);
        var dst=new SimpleContainer(1);
        src.setItem(0,new ItemStack(Items.STONE,4));
        AtomicInteger invalidations=new AtomicInteger();
        try {
            MerchantStockTransfer.transferStores(List.of(src),List.of(dst),false,()->{
                if (invalidations.incrementAndGet()==1) throw new IllegalStateException("simulated commit failure");
            });
            throw new AssertionError("expected failure");
        } catch (IllegalStateException expected) {
            check(src.getItem(0).is(Items.STONE) && src.getItem(0).getCount()==4 && dst.getItem(0).isEmpty(),
                    "exception restores both inventories");
        }
    }
}
