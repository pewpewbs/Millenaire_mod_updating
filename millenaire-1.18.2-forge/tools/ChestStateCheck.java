package net.minecraftforge.registries;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.millenaire.common.block.BlockLockedChest;
import org.millenaire.common.entity.*;
import org.millenaire.common.ui.ContainerLockedChest;
import org.millenaire.client.render.TileEntityLockedChestRenderer;

/** Real chest/block/menu classes, isolated from the still-unported world/network services. */
public class ChestStateCheck {
    static void check(boolean passed,String label){if(!passed)throw new AssertionError(label);}
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
        ((ForgeRegistry<net.minecraft.world.level.block.Block>)ForgeRegistries.BLOCKS).unfreeze();
        ((NamespacedDefaultedWrapper<net.minecraft.world.level.block.Block>)Registry.BLOCK).unfreeze();
        BlockLockedChest block=new BlockLockedChest("chest_state_check");
        MillBlockEntities.LOCKED_CHEST=BlockEntityType.Builder.of(TileEntityLockedChest::new,block).build(null);
        for(Direction facing:Direction.Plane.HORIZONTAL) {
            var state=block.defaultBlockState().setValue(BlockLockedChest.FACING,facing);
            check(block.func_176203_a(block.func_176201_c(state)).equals(state),"legacy facing roundtrip");
            check(block.itemMetadata(state)==0,"orientation is not an inventory variant");
            check(TileEntityLockedChestRenderer.chestType(facing,facing.getClockWise())==ChestType.LEFT,"left model orientation");
            check(TileEntityLockedChestRenderer.chestType(facing,facing.getCounterClockWise())==ChestType.RIGHT,"right model orientation");
        }
        TileEntityLockedChest chest=new TileEntityLockedChest(BlockPos.ZERO,block.defaultBlockState());
        chest.setItem(0,new ItemStack(Items.DIAMOND,7));chest.setItem(26,new ItemStack(Items.BREAD,13));
        chest.setCustomName(new net.minecraft.network.chat.TextComponent("Village store"));
        chest.buildingPos=new org.millenaire.common.utilities.Point(new BlockPos(4,65,-8));
        var saved=chest.saveWithoutMetadata();
        TileEntityLockedChest restored=new TileEntityLockedChest(BlockPos.ZERO,block.defaultBlockState());restored.load(saved);
        check(restored.getItem(0).is(Items.DIAMOND) && restored.getItem(0).getCount()==7,"first slot survives save");
        check(restored.getItem(26).is(Items.BREAD) && restored.getItem(26).getCount()==13,"last slot survives save");
        check(restored.getCustomName().getString().equals("Village store"),"custom name survives save");
        check(restored.saveWithoutMetadata().getLong("buildingPos")==saved.getLong("buildingPos"),"ownership tag retained");
        for(Direction side:Direction.values())check(restored.getSlotsForFace(side).length==0 && !restored.canTakeItemThroughFace(0,restored.getItem(0),side) && !restored.canPlaceItemThroughFace(0,new ItemStack(Items.STONE),side),"automation blocked");
        check(restored.triggerEvent(1,2) && restored.numPlayersUsing==2,"lid viewer synchronization");
        SimpleContainer player=new SimpleContainer(36),inventory=new SimpleContainer(27);inventory.setItem(0,new ItemStack(Items.DIAMOND,7));
        var locked=new ContainerLockedChest(3,player,inventory,null,null,true);
        for(ClickType click:ClickType.values())locked.clicked(0,0,click,null);
        check(locked.quickMoveStack(null,0).isEmpty() && inventory.getItem(0).getCount()==7 && player.isEmpty(),"all locked click modes leave contents unchanged");
        check(!locked.slots.get(0).mayPickup(null) && !locked.slots.get(0).mayPlace(new ItemStack(Items.STONE)),"locked slots reject extraction and insertion");
        var building=new org.millenaire.common.village.Building();var unlocked=new ContainerLockedChest(4,player,inventory,null,building,false);
        check(unlocked.quickMoveStack(null,0).getCount()==7 && inventory.isEmpty(),"unlocked transfer works");
        check(building.invalidations>0,"transfer invalidates village inventory cache");
        class ToggleChest extends TileEntityLockedChest {
            boolean locked;
            ToggleChest(){super(BlockPos.ZERO,block.defaultBlockState());}
            @Override public boolean isLockedFor(net.minecraft.world.entity.player.Player player){return locked;}
        }
        ToggleChest first=new ToggleChest(),second=new ToggleChest();first.setItem(0,new ItemStack(Items.EMERALD,9));
        var pair=new TileEntityLockedChest.InventoryLockedLargeChest("Village",first,second);
        var pairMenu=new ContainerLockedChest(5,new SimpleContainer(36),pair,null,null,false);
        second.locked=true;
        check(pair.isLockedFor(null) && pairMenu.quickMoveStack(null,0).isEmpty() && first.getItem(0).getCount()==9,"new lock on either half blocks existing menu");
        second.locked=false;check(!pairMenu.quickMoveStack(null,0).isEmpty(),"unlocked pair permits transfer");
        check(pairMenu.getRows()==6 && pair.contains(first) && pair.contains(second),"double inventory includes both halves");
        System.out.println("PASS: chest persistence, facing, model halves, locked click modes, dynamic double locks, transfers and automation slots");
    }
}
