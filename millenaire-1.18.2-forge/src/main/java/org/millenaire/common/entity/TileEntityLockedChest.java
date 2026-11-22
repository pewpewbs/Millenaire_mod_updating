package org.millenaire.common.entity;
import java.io.IOException;
import javax.annotation.Nullable;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.*;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import org.millenaire.common.block.BlockLockedChest;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.network.*;
import org.millenaire.common.ui.ContainerLockedChest;
import org.millenaire.common.utilities.*;
import org.millenaire.common.village.Building;
import org.millenaire.common.world.MillWorldData;

public class TileEntityLockedChest
extends RandomizableContainerBlockEntity
implements WorldlyContainer {
    private NonNullList<ItemStack> chestContents = NonNullList.withSize(27,ItemStack.EMPTY);
    public boolean adjacentChestChecked;
    public TileEntityLockedChest adjacentChestZNeg;
    public TileEntityLockedChest adjacentChestXPos;
    public TileEntityLockedChest adjacentChestXNeg;
    public TileEntityLockedChest adjacentChestZPos;
    public float lidAngle;
    public float prevLidAngle;
    public int numPlayersUsing;
    private int ticksSinceSync;
    public Point buildingPos = null;
    public boolean loaded = false;
    public boolean serverDevMode = false;

    public TileEntityLockedChest(BlockPos pos,BlockState state) { super(MillBlockEntities.LOCKED_CHEST,pos,state); }
    @Override protected Component getDefaultName() { return new TranslatableComponent("container.chest"); }
    @Override protected void setItems(NonNullList<ItemStack> items) { chestContents=items; }

    public static void readUpdatePacket(FriendlyByteBuf data, Level world) {
        Point pos = StreamReadWrite.readNullablePoint(data);
        TileEntityLockedChest te = pos==null?null:pos.getMillChest(world);
        if (te != null) {
            try {
                Building building;
                te.buildingPos = StreamReadWrite.readNullablePoint(data);
                te.serverDevMode = data.readBoolean();
                int nb = data.readByte();
                for (int i = 0; i < nb; ++i) {
                    ItemStack stack = StreamReadWrite.readNullableItemStack(data);
                    if (stack == null) {
                        MillLog.error((Object)te, "Received a null stack!");
                        stack = ItemStack.EMPTY;
                    }
                    te.setItem(i, stack);
                }
                te.loaded = true;
                if (Mill.clientWorld != null && (building = Mill.clientWorld.getBuilding(te.buildingPos)) != null) {
                    building.invalidateInventoryCache();
                }
            }
            catch (IOException e) {
                MillLog.printException((Object)((Object)te) + ": Error in readUpdatePacket", e);
            }
        }
    }



    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return false;
    }

    public boolean canPlaceItemThroughFace(int index, ItemStack itemStackIn, Direction direction) {
        return false;
    }

    public void checkForAdjacentChests() {
        if (level != null && !this.adjacentChestChecked) {
            this.adjacentChestChecked = true;
            this.adjacentChestXNeg = this.getAdjacentLockedChest(Direction.WEST);
            this.adjacentChestXPos = this.getAdjacentLockedChest(Direction.EAST);
            this.adjacentChestZNeg = this.getAdjacentLockedChest(Direction.NORTH);
            this.adjacentChestZPos = this.getAdjacentLockedChest(Direction.SOUTH);
        }
    }

    public void stopOpen(Player player) {
        if (level!=null && !player.isSpectator() && getBlockState().getBlock() instanceof BlockLockedChest) {
            this.numPlayersUsing=Math.max(0,this.numPlayersUsing-1);
            this.level.blockEvent(this.worldPosition, getBlockState().getBlock(), 1, this.numPlayersUsing);
            this.level.updateNeighborsAt(worldPosition,getBlockState().getBlock());
        }
    }

    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory) {
        unpackLootTable(inventory.player);
        var data=Mill.getMillWorld(level); Building building=buildingPos==null || data==null?null:data.getBuilding(buildingPos);
        return new ContainerLockedChest(id,inventory,this,inventory.player,building,isLockedFor(inventory.player));
    }

    @Nullable
    protected TileEntityLockedChest getAdjacentLockedChest(Direction side) {
        BlockEntity tileentity;
        BlockPos blockpos = this.worldPosition.relative(side);
        if (level.hasChunkAt(blockpos) && this.isLockedChestAt(blockpos) && (tileentity = this.level.getBlockEntity(blockpos)) instanceof TileEntityLockedChest) {
            TileEntityLockedChest tileentitychest = (TileEntityLockedChest)tileentity;
            tileentitychest.setNeighbor(this, side.getOpposite());
            return tileentitychest;
        }
        return null;
    }

    public Component getDisplayName() {
        if (hasCustomName()) return getCustomName();
        if (this.buildingPos == null) {
            return LanguageUtilities.textComponent("ui.unlockedchest");
        }
        Building building = null;
        MillWorldData data=level==null?null:Mill.getMillWorld(level);
        if (data!=null) building=data.getBuilding(buildingPos);
        if (building == null) return LanguageUtilities.textComponent("ui.lockedchest");
        String s = building.getNativeBuildingName();
        if (building.chestLocked) {
            return new TextComponent(s + ": " + LanguageUtilities.string("ui.lockedchest"));
        }
        return new TextComponent(s + ": " + LanguageUtilities.string("ui.unlockedchest"));
    }

    public String func_174875_k() {
        return "minecraft:chest";
    }

    public int getMaxStackSize() {
        return 64;
    }

    public String getInvLargeName() {
        if (this.buildingPos == null) {
            return LanguageUtilities.string("ui.largeunlockedchest");
        }
        Building building = null;
        MillWorldData data=level==null?null:Mill.getMillWorld(level);
        if (data!=null) building=data.getBuilding(buildingPos);
        if (building == null) return LanguageUtilities.string("ui.largelockedchest");
        String s = building.getNativeBuildingName();
        if (building.chestLocked) {
            return s + ": " + LanguageUtilities.string("ui.largelockedchest");
        }
        return s + ": " + LanguageUtilities.string("ui.largeunlockedchest");
    }

    protected NonNullList<ItemStack> getItems() {
        return this.chestContents;
    }

    public String func_70005_c_() { return getName().getString(); }

    @OnlyIn(value=Dist.CLIENT)
    public AABB getRenderBoundingBox() {
        return new AABB(this.worldPosition.offset(-1, 0, -1), this.worldPosition.offset(2, 2, 2));
    }

    public IItemHandler getSingleChestHandler() { return new net.minecraftforge.items.wrapper.InvWrapper(this); }

    public int getContainerSize() {
        return 27;
    }

    public int[] getSlotsForFace(Direction side) {
        return new int[0];
    }

    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,@Nullable Direction side) {
        if (capability==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return LazyOptional.empty();
        return super.getCapability(capability,side);
    }

    public void setRemoved() {
        super.setRemoved();
        this.func_145836_u();
        this.checkForAdjacentChests();
    }

    public boolean isEmpty() {
        for (ItemStack itemstack : this.chestContents) {
            if (itemstack.isEmpty()) continue;
            return false;
        }
        return true;
    }

    private boolean isLockedChestAt(BlockPos posIn) {
        if (this.level == null) {
            return false;
        }
        Block block = this.level.getBlockState(posIn).getBlock();
        return block instanceof BlockLockedChest;
    }

    public boolean isLockedFor(Player player) {
        if (player == null) {
            MillLog.printException("Null player", new Exception());
            return true;
        }
        if (level==null) return true;
        if (!this.loaded && this.level.isClientSide) {
            return true;
        }
        if (this.buildingPos == null) {
            return false;
        }
        if (!this.level.isClientSide && MillConfigValues.DEV) {
            return false;
        }
        if (this.serverDevMode) {
            return false;
        }
        MillWorldData mw = Mill.getMillWorld(this.level);
        if (mw == null) {
            MillLog.printException("Null MillWorldData", new Exception());
            return true;
        }
        Building building = mw.getBuilding(this.buildingPos);
        if (building == null) {
            return true;
        }
        return building.lockedForPlayer(player);
    }

    public void startOpen(Player player) {
        if (level!=null && !player.isSpectator()) {
            if (this.numPlayersUsing < 0) {
                this.numPlayersUsing = 0;
            }
            ++this.numPlayersUsing;
            this.level.blockEvent(this.worldPosition, getBlockState().getBlock(), 1, this.numPlayersUsing);
            this.level.updateNeighborsAt(worldPosition,getBlockState().getBlock());
        }
    }

    @Override public void load(CompoundTag compound) {
        Building building;
        super.load(compound);
        this.chestContents = NonNullList.withSize(getContainerSize(),ItemStack.EMPTY);
        if (!this.tryLoadLootTable(compound)) {
            ContainerHelper.loadAllItems((CompoundTag)compound, this.chestContents);
        }
        this.buildingPos = Point.read(compound, "buildingPos");
        if (Mill.clientWorld != null && (building = Mill.clientWorld.getBuilding(this.buildingPos)) != null) {
            building.invalidateInventoryCache();
        }
    }

    public boolean triggerEvent(int id, int type) {
        if (id == 1) {
            this.numPlayersUsing = type;
            return true;
        }
        return super.triggerEvent(id, type);
    }

    public void sendUpdatePacket(Player player) {
        ServerSender.sendLockedChestUpdatePacket(this, player);
    }

    private void setNeighbor(TileEntityLockedChest chestTe, Direction side) {
        if (chestTe.isRemoved()) {
            this.adjacentChestChecked = false;
        } else if (this.adjacentChestChecked) {
            switch (side) {
                case NORTH: {
                    if (this.adjacentChestZNeg == chestTe) break;
                    this.adjacentChestChecked = false;
                    break;
                }
                case SOUTH: {
                    if (this.adjacentChestZPos == chestTe) break;
                    this.adjacentChestChecked = false;
                    break;
                }
                case EAST: {
                    if (this.adjacentChestXPos == chestTe) break;
                    this.adjacentChestChecked = false;
                    break;
                }
                case WEST: {
                    if (this.adjacentChestXNeg == chestTe) break;
                    this.adjacentChestChecked = false;
                }
            }
        }
    }

    public void tick() {
        if (level==null) return;
        this.checkForAdjacentChests();
        int i = this.worldPosition.getX();
        int j = this.worldPosition.getY();
        int k = this.worldPosition.getZ();
        ++this.ticksSinceSync;
        if (!this.level.isClientSide && this.numPlayersUsing != 0 && (this.ticksSinceSync + i + j + k) % 200 == 0) {
            this.numPlayersUsing = 0;
            for (Player entityplayer : this.level.getEntitiesOfClass(Player.class, new AABB((double)((float)i - 5.0f), (double)((float)j - 5.0f), (double)((float)k - 5.0f), (double)((float)(i + 1) + 5.0f), (double)((float)(j + 1) + 5.0f), (double)((float)(k + 1) + 5.0f)))) {
                Container iinventory;
                if (!(entityplayer.containerMenu instanceof ContainerLockedChest) || (iinventory = ((ContainerLockedChest)entityplayer.containerMenu).getLowerChestInventory()) != this && (!(iinventory instanceof InventoryLockedLargeChest) || !((InventoryLockedLargeChest)iinventory).isPartOfLargeChest((Container)this))) continue;
                ++this.numPlayersUsing;
            }
        }
        this.prevLidAngle = this.lidAngle;
        if (this.numPlayersUsing > 0 && this.lidAngle == 0.0f && this.adjacentChestZNeg == null && this.adjacentChestXNeg == null) {
            double d1 = (double)i + 0.5;
            double d2 = (double)k + 0.5;
            if (this.adjacentChestZPos != null) {
                d2 += 0.5;
            }
            if (this.adjacentChestXPos != null) {
                d1 += 0.5;
            }
            this.level.playSound((Player)null, d1, (double)j + 0.5, d2, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5f, this.level.random.nextFloat() * 0.1f + 0.9f);
        }
        if (this.numPlayersUsing <= 0 && this.lidAngle > 0.0f || this.numPlayersUsing > 0 && this.lidAngle < 1.0f) {
            float f2 = this.lidAngle;
            this.lidAngle = this.numPlayersUsing > 0 ? (this.lidAngle += 0.1f) : (this.lidAngle -= 0.1f);
            if (this.lidAngle > 1.0f) {
                this.lidAngle = 1.0f;
            }
            if (this.lidAngle < 0.5f && f2 >= 0.5f && this.adjacentChestZNeg == null && this.adjacentChestXNeg == null) {
                double d3 = (double)i + 0.5;
                double d0 = (double)k + 0.5;
                if (this.adjacentChestZPos != null) {
                    d0 += 0.5;
                }
                if (this.adjacentChestXPos != null) {
                    d3 += 0.5;
                }
                this.level.playSound((Player)null, d3, (double)j + 0.5, d0, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5f, this.level.random.nextFloat() * 0.1f + 0.9f);
            }
            if (this.lidAngle < 0.0f) {
                this.lidAngle = 0.0f;
            }
        }
    }

    public void func_145836_u() {
        this.adjacentChestChecked = false;
    }

    @Override protected void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        if (!this.trySaveLootTable(compound)) {
            ContainerHelper.saveAllItems((CompoundTag)compound, this.chestContents);
        }
        if (this.buildingPos != null) {
            this.buildingPos.write(compound, "buildingPos");
        }
    }

    public static class InventoryLockedLargeChest extends CompoundContainer implements WorldlyContainer,MenuProvider {
        private final String name;
        private final TileEntityLockedChest upperChest,lowerChest;
        public InventoryLockedLargeChest(String name,TileEntityLockedChest upper,TileEntityLockedChest lower) {
            super(upper,lower);this.name=name;upperChest=upper;lowerChest=lower;
        }
        public boolean isLockedFor(Player player) { return upperChest.isLockedFor(player) || lowerChest.isLockedFor(player); }
        public boolean isPartOfLargeChest(Container inventory) { return contains(inventory); }
        @Override public int[] getSlotsForFace(Direction side) { return new int[0]; }
        @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side) { return false; }
        @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side) { return false; }
        @Override public Component getDisplayName() {
            return upperChest.hasCustomName()?upperChest.getCustomName():lowerChest.hasCustomName()?lowerChest.getCustomName():new TextComponent(name);
        }
        @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player) {
            var data=Mill.getMillWorld(upperChest.level);Building building=upperChest.buildingPos==null || data==null?null:data.getBuilding(upperChest.buildingPos);
            return new ContainerLockedChest(id,inventory,this,player,building,isLockedFor(player));
        }
    }
}
