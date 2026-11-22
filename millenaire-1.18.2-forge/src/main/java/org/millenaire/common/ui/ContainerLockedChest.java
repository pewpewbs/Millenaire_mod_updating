package org.millenaire.common.ui;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.millenaire.common.entity.TileEntityLockedChest;
import org.millenaire.common.village.Building;

public class ContainerLockedChest extends AbstractContainerMenu {
    private final Container lowerChestInventory;
    private final int numRows;
    private final boolean locked;
    public ContainerLockedChest(Container playerInventory,Container chest,Player player,Building building,boolean locked) { this(0,playerInventory,chest,player,building,locked); }
    public ContainerLockedChest(int id,Container playerInventory,Container chest,Player player,Building building,boolean locked) {
        super(MillMenus.LOCKED_CHEST,id);
        lowerChestInventory=chest;numRows=chest.getContainerSize()/9;this.locked=locked;
        if (numRows!=3 && numRows!=6) throw new IllegalArgumentException("Invalid Millenaire chest size: "+chest.getContainerSize());
        chest.startOpen(player); int offset=(numRows-4)*18;
        for (int row=0;row<numRows;row++) for (int col=0;col<9;col++)
            addSlot(locked?new LockedSlot(chest,col+row*9,8+col*18,18+row*18):new CachedSlot(chest,col+row*9,8+col*18,18+row*18,building));
        for (int row=0;row<3;row++) for (int col=0;col<9;col++) addSlot(new Slot(playerInventory,col+row*9+9,8+col*18,103+row*18+offset));
        for (int col=0;col<9;col++) addSlot(new Slot(playerInventory,col,8+col*18,161+offset));
    }
    public Container getLowerChestInventory() { return lowerChestInventory; }
    public int getRows() { return numRows; }
    public boolean isLocked() { return locked; }
    private boolean isLockedFor(Player player) {
        if (locked) return true;
        if (lowerChestInventory instanceof TileEntityLockedChest chest) return chest.isLockedFor(player);
        if (lowerChestInventory instanceof TileEntityLockedChest.InventoryLockedLargeChest chest) return chest.isLockedFor(player);
        return false;
    }
    @Override public boolean stillValid(Player player) { return lowerChestInventory.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player);lowerChestInventory.stopOpen(player); }
    @Override public void clicked(int slot,int button,ClickType type,Player player) {
        // Server-side guard covers number keys, dragging, double-click collection and shift-click.
        if (isLockedFor(player)) return;
        super.clicked(slot,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if (isLockedFor(player) || index<0 || index>=slots.size()) return ItemStack.EMPTY;
        Slot slot=slots.get(index);if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),before=stack.copy();int size=numRows*9;
        if (index<size ? !moveItemStackTo(stack,size,slots.size(),true) : !moveItemStackTo(stack,0,size,false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount()==before.getCount()) return ItemStack.EMPTY;
        slot.onTake(player,stack);return before;
    }
    public static class LockedSlot extends Slot {
        public LockedSlot(Container inventory,int index,int x,int y) { super(inventory,index,x,y); }
        @Override public boolean mayPickup(Player player) { return false; }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }
    public static class CachedSlot extends Slot {
        private final Building building;
        public CachedSlot(Container inventory,int index,int x,int y,Building building) { super(inventory,index,x,y);this.building=building; }
        @Override public void setChanged() { super.setChanged();if (building!=null) building.invalidateInventoryCache(); }
    }
}
