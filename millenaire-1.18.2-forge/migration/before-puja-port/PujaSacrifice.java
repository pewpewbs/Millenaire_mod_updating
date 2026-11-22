/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.enchantment.Enchantment
 *  net.minecraft.enchantment.EnchantmentHelper
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.Blocks
 *  net.minecraft.init.Enchantments
 *  net.minecraft.init.Items
 *  net.minecraft.inventory.EntityEquipmentSlot
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemArmor
 *  net.minecraft.item.ItemAxe
 *  net.minecraft.item.ItemBow
 *  net.minecraft.item.ItemPickaxe
 *  net.minecraft.item.ItemSpade
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.ItemSword
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.util.text.ITextComponent
 */
package org.millenaire.common.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.item.MillItems;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.village.Building;

public class PujaSacrifice
implements Container {
    public static final int TOOL = 1;
    public static final int ARMOUR = 2;
    public static final int HELMET = 3;
    public static final int BOOTS = 4;
    public static final int SWORD_AXE = 5;
    public static final int SWORD = 6;
    public static final int BOW = 7;
    public static final int UNBREAKABLE = 8;
    public static PrayerTarget[] PUJA_TARGETS = new PrayerTarget[]{new PrayerTarget(Enchantments.BLOCK_EFFICIENCY, "pujas.god0", 0, 188, 46, 188, 1), new PrayerTarget(Enchantments.UNBREAKING, "pujas.god1", 0, 205, 46, 205, 1), new PrayerTarget(Enchantments.BLOCK_FORTUNE, "pujas.god2", 0, 222, 46, 222, 1), new PrayerTarget(Enchantments.SILK_TOUCH, "pujas.god3", 0, 239, 46, 239, 1)};
    public static PrayerTarget[] MAYAN_TARGETS = new PrayerTarget[]{new PrayerTarget(Enchantments.ALL_DAMAGE_PROTECTION, "mayan.god0", 0, 188, 120, 188, 2), new PrayerTarget(Enchantments.FIRE_PROTECTION, "mayan.god1", 20, 188, 140, 188, 2), new PrayerTarget(Enchantments.BLAST_PROTECTION, "mayan.god2", 40, 188, 160, 188, 2), new PrayerTarget(Enchantments.PROJECTILE_PROTECTION, "mayan.god3", 60, 188, 180, 188, 2), new PrayerTarget(Enchantments.THORNS, "mayan.god4", 80, 188, 200, 188, 2), new PrayerTarget(Enchantments.RESPIRATION, "mayan.god5", 100, 188, 120, 188, 3), new PrayerTarget(Enchantments.AQUA_AFFINITY, "mayan.god6", 0, 208, 120, 208, 3), new PrayerTarget(Enchantments.FALL_PROTECTION, "mayan.god7", 20, 208, 140, 208, 4), new PrayerTarget(Enchantments.SHARPNESS, "mayan.god8", 40, 208, 160, 208, 5), new PrayerTarget(Enchantments.SMITE, "mayan.god9", 0, 188, 120, 188, 5), new PrayerTarget(Enchantments.BANE_OF_ARTHROPODS, "mayan.god10", 80, 188, 200, 188, 5), new PrayerTarget(Enchantments.KNOCKBACK, "mayan.god11", 60, 208, 180, 208, 6), new PrayerTarget(Enchantments.FIRE_ASPECT, "mayan.god12", 20, 188, 140, 188, 6), new PrayerTarget(Enchantments.MOB_LOOTING, "mayan.god13", 80, 208, 200, 208, 6), new PrayerTarget(Enchantments.POWER_ARROWS, "mayan.god14", 40, 208, 160, 208, 7), new PrayerTarget(Enchantments.PUNCH_ARROWS, "mayan.god15", 60, 208, 180, 208, 7), new PrayerTarget(Enchantments.FLAMING_ARROWS, "mayan.god16", 20, 188, 140, 188, 7), new PrayerTarget(Enchantments.INFINITY_ARROWS, "mayan.god17", 80, 208, 200, 208, 7), new PrayerTarget(Enchantments.UNBREAKING, "mayan.god18", 100, 208, 220, 208, 8)};
    public static int PUJA_DURATION = 30;
    public static final short PUJA = 0;
    public static final short MAYAN = 1;
    private ItemStack[] items;
    public PrayerTarget currentTarget = null;
    public int offeringProgress = 0;
    public int offeringNeeded = 1;
    public short pujaProgress = 0;
    public Building temple = null;
    public MillVillager priest = null;
    public short type = 0;

    public static boolean validForItem(int type, Item item) {
        if (type == 1) {
            return item instanceof ShovelItem || item instanceof AxeItem || item instanceof PickaxeItem;
        }
        if (type == 2) {
            return item instanceof ArmorItem;
        }
        if (type == 3) {
            return item instanceof ArmorItem && ((ArmorItem)item).slot == EquipmentSlot.HEAD;
        }
        if (type == 4) {
            return item instanceof ArmorItem && ((ArmorItem)item).slot == EquipmentSlot.FEET;
        }
        if (type == 5) {
            return item instanceof SwordItem || item instanceof AxeItem;
        }
        if (type == 6) {
            return item instanceof SwordItem;
        }
        if (type == 7) {
            return item instanceof BowItem;
        }
        if (type == 8) {
            return item instanceof SwordItem || item instanceof ArmorItem || item instanceof BowItem;
        }
        return false;
    }

    public PujaSacrifice(Building temple, CompoundTag tag) {
        this.temple = temple;
        if (temple.containsTags("sacrifices")) {
            this.type = 1;
        }
        this.readFromNBT(tag);
    }

    public PujaSacrifice(Building temple, short type) {
        this.temple = temple;
        this.items = new ItemStack[this.getContainerSize()];
        for (int i = 0; i < this.items.length; ++i) {
            this.items[i] = ItemStack.EMPTY;
        }
        this.type = type;
    }

    public void calculateOfferingsNeeded() {
        int currentLevel;
        this.offeringNeeded = 0;
        if (this.items[4] == ItemStack.EMPTY || this.currentTarget == null) {
            return;
        }
        ItemStack tool = this.items[4];
        if (EnchantmentHelper.getItemEnchantmentLevel((Enchantment)this.currentTarget.enchantment, (ItemStack)tool) >= this.currentTarget.enchantment.getMaxLevel()) {
            return;
        }
        if (!this.currentTarget.enchantment.canEnchant(tool)) {
            return;
        }
        int nbother = 0;
        if (tool.isEnchanted()) {
            ListTag nbttaglist = tool.getEnchantmentTags();
            nbother = nbttaglist.size();
            Map existingEnchantments = EnchantmentHelper.getEnchantments((ItemStack)tool);
            for (Enchantment enchId : existingEnchantments.keySet()) {
                if (enchId == this.currentTarget.enchantment || enchId.isCompatibleWith(this.currentTarget.enchantment)) continue;
                return;
            }
        }
        if ((currentLevel = EnchantmentHelper.getItemEnchantmentLevel((Enchantment)this.currentTarget.enchantment, (ItemStack)tool)) > 0) {
            --nbother;
        }
        int cost = 50 + this.currentTarget.enchantment.getMinCost(currentLevel + 1) * 10;
        cost *= nbother / 2 + 1;
        if (MillConfigValues.LogPujas >= 2) {
            MillLog.minor(this, "Offering needed: " + cost);
        }
        this.offeringNeeded = cost;
    }

    public boolean canPray() {
        if (this.offeringNeeded <= this.offeringProgress) {
            return false;
        }
        return this.items[0] != ItemStack.EMPTY;
    }

    public void changeEnchantment(int i) {
        if (this.currentTarget == this.getTargets().get(i)) {
            return;
        }
        this.currentTarget = this.getTargets().get(i);
        this.offeringProgress = 0;
        this.calculateOfferingsNeeded();
    }

    public void clearContent() {
    }

    public void stopOpen(Player player) {
    }

    private void completeOffering() {
        int currentlevel = EnchantmentHelper.getItemEnchantmentLevel((Enchantment)this.currentTarget.enchantment, (ItemStack)this.items[4]);
        if (currentlevel == 0) {
            this.items[4].enchant(this.currentTarget.enchantment, 1);
        } else {
            ListTag enchList = this.items[4].getEnchantmentTags();
            for (int i = 0; i < enchList.size(); ++i) {
                Enchantment e = Enchantment.byId((int)enchList.getCompound(i).getShort("id"));
                if (e != this.currentTarget.enchantment) continue;
                enchList.getCompound(i).putShort("lvl", (short)(currentlevel + 1));
            }
        }
        this.offeringProgress = 0;
        this.calculateOfferingsNeeded();
        this.temple.getTownHall().requestSave("Puja/sacrifice offering complete");
    }

    public ItemStack removeItem(int slot, int nb) {
        if (this.items[slot] != ItemStack.EMPTY) {
            if (this.items[slot].getCount() <= nb) {
                ItemStack itemstack = this.items[slot];
                this.items[slot] = ItemStack.EMPTY;
                return itemstack;
            }
            ItemStack itemstack1 = this.items[slot].split(nb);
            if (this.items[slot].getCount() == 0) {
                this.items[slot] = ItemStack.EMPTY;
            }
            return itemstack1;
        }
        return ItemStack.EMPTY;
    }

    private void endPuja() {
        ItemStack offer = this.items[0];
        if (offer == ItemStack.EMPTY) {
            return;
        }
        int offerValue = this.getOfferingValue(offer);
        this.offeringProgress += offerValue;
        offer.setCount(offer.getCount() - 1);
        if (offer.getCount() == 0) {
            this.items[0] = ItemStack.EMPTY;
        }
        if (this.offeringProgress >= this.offeringNeeded) {
            this.completeOffering();
        }
    }

    public Component getDisplayName() {
        return null;
    }

    public int func_174887_a_(int id) {
        return 0;
    }

    public int func_174890_g() {
        return 0;
    }

    public int getMaxStackSize() {
        return 64;
    }

    public String func_70005_c_() {
        return LanguageUtilities.string("pujas.invanme");
    }

    public int getOfferingProgressScaled(int scale) {
        if (this.offeringNeeded == 0) {
            return 0;
        }
        return this.offeringProgress * scale / this.offeringNeeded;
    }

    public int getOfferingValue(ItemStack is) {
        if (this.type == 0) {
            return this.getOfferingValuePuja(is);
        }
        if (this.type == 1) {
            return this.getOfferingValueMayan(is);
        }
        return 0;
    }

    public int getOfferingValueMayan(ItemStack is) {
        if (is.getItem() == Items.field_151144_bL) {
            return 4096;
        }
        if (is.getItem() == Items.GHAST_TEAR) {
            return 384;
        }
        if (is.getItem() == Items.BLAZE_ROD) {
            return 64;
        }
        if (is.getItem() == MillItems.CACAUHAA) {
            return 64;
        }
        if (is.getItem() == Items.CHICKEN) {
            return 1;
        }
        if (is.getItem() == Items.BEEF) {
            return 1;
        }
        if (is.getItem() == Items.PORKCHOP) {
            return 1;
        }
        if (is.getItem() == Items.field_151115_aP) {
            return 1;
        }
        if (is.getItem() == Items.LEATHER) {
            return 1;
        }
        if (is.getItem() == Items.field_151100_aR && is.getDamageValue() == 0) {
            return 1;
        }
        if (is.getItem() == Items.SLIME_BALL) {
            return 1;
        }
        if (is.getItem() == Items.ROTTEN_FLESH) {
            return 2;
        }
        if (is.getItem() == Items.BONE) {
            return 2;
        }
        if (is.getItem() == Items.MAGMA_CREAM) {
            return 4;
        }
        if (is.getItem() == Items.GUNPOWDER) {
            return 4;
        }
        if (is.getItem() == Items.SPIDER_EYE) {
            return 4;
        }
        if (is.getItem() == Items.ENDER_PEARL) {
            return 6;
        }
        return 0;
    }

    public int getOfferingValuePuja(ItemStack is) {
        if (is.getItem() == Items.DIAMOND) {
            return 384;
        }
        if (is.getItem() == Items.MILK_BUCKET) {
            return 128;
        }
        if (is.getItem() == Items.GOLDEN_APPLE) {
            return 96;
        }
        if (is.getItem() == Items.GOLD_INGOT) {
            return 64;
        }
        if (is.getItem() == MillItems.RICE) {
            return 8;
        }
        if (is.getItem() == MillItems.RASGULLA) {
            return 64;
        }
        if (is.getItem() == Item.byBlock((Block)Blocks.field_150328_O) || is.getItem() == Item.byBlock((Block)Blocks.DANDELION)) {
            return 16;
        }
        if (is.getItem() == Item.byBlock((Block)Blocks.field_150329_H) || is.getItem() == Items.APPLE) {
            return 8;
        }
        if (is.getItem() == Item.byBlock((Block)Blocks.field_150325_L) && is.getDamageValue() == 0) {
            return 8;
        }
        if (is.getItem() == Items.MELON_SLICE) {
            return 4;
        }
        return 0;
    }

    public int getPujaProgressScaled(int scale) {
        return this.pujaProgress * scale / PUJA_DURATION;
    }

    public int getContainerSize() {
        return 5;
    }

    public ItemStack getItem(int par1) {
        return this.items[par1];
    }

    public List<PrayerTarget> getTargets() {
        if (this.items[4] == ItemStack.EMPTY) {
            return new ArrayList<PrayerTarget>();
        }
        if (this.type == 0) {
            ArrayList<PrayerTarget> targets = new ArrayList<PrayerTarget>();
            for (PrayerTarget t : PUJA_TARGETS) {
                if (!t.validForItem(this.items[4].getItem())) continue;
                targets.add(t);
            }
            return targets;
        }
        if (this.type == 1) {
            ArrayList<PrayerTarget> targets = new ArrayList<PrayerTarget>();
            for (PrayerTarget t : MAYAN_TARGETS) {
                if (!t.validForItem(this.items[4].getItem())) continue;
                targets.add(t);
            }
            return targets;
        }
        return new ArrayList<PrayerTarget>();
    }

    public boolean hasCustomName() {
        return false;
    }

    public boolean isActive() {
        return false;
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean canPlaceItem(int i, ItemStack itemstack) {
        return true;
    }

    public boolean stillValid(Player player) {
        return false;
    }

    public void setChanged() {
    }

    public void startOpen(Player player) {
    }

    public boolean performPuja(MillVillager priest) {
        this.priest = priest;
        if (this.pujaProgress == 0) {
            boolean success = this.startPuja();
            if (success) {
                this.pujaProgress = 1;
            }
            return success;
        }
        if (this.pujaProgress >= PUJA_DURATION) {
            this.endPuja();
            this.pujaProgress = 0;
            return this.canPray();
        }
        this.pujaProgress = (short)(this.pujaProgress + 1);
        return this.canPray();
    }

    public void readFromNBT(CompoundTag par1NBTTagCompound) {
        ListTag nbttaglist = par1NBTTagCompound.getList("Items", 10);
        this.items = new ItemStack[this.getContainerSize()];
        for (int i = 0; i < nbttaglist.size(); ++i) {
            CompoundTag nbttagcompound = nbttaglist.getCompound(i);
            byte byte0 = nbttagcompound.getByte("Slot");
            if (byte0 < 0 || byte0 >= this.items.length) continue;
            this.items[byte0] = new ItemStack(nbttagcompound);
        }
        Enchantment ench = Enchantment.byId((int)par1NBTTagCompound.getShort("enchantmentTarget"));
        if (ench != null) {
            for (PrayerTarget t : this.getTargets()) {
                if (t.enchantment != ench) continue;
                this.currentTarget = t;
            }
        }
        if (MillConfigValues.LogPujas >= 2) {
            MillLog.minor(this, "Reading enchantmentTarget: " + ench + ", " + this.currentTarget);
        }
        this.offeringProgress = par1NBTTagCompound.getShort("offeringProgress");
        this.pujaProgress = par1NBTTagCompound.getShort("pujaProgress");
        this.calculateOfferingsNeeded();
    }

    public ItemStack removeItemNoUpdate(int par1) {
        if (this.items[par1] != ItemStack.EMPTY) {
            ItemStack itemstack = this.items[par1];
            this.items[par1] = ItemStack.EMPTY;
            this.setChanged();
            return itemstack;
        }
        return ItemStack.EMPTY;
    }

    public void func_174885_b(int id, int value) {
    }

    public void setItem(int par1, ItemStack par2ItemStack) {
        this.items[par1] = par2ItemStack;
        if (par2ItemStack != ItemStack.EMPTY && par2ItemStack.getCount() > this.getMaxStackSize()) {
            par2ItemStack.setCount(this.getMaxStackSize());
        }
        this.setChanged();
    }

    private boolean startPuja() {
        int money = MillCommonUtilities.countMoney(this);
        if (money == 0) {
            return false;
        }
        if (this.offeringNeeded == 0 || this.offeringProgress >= this.offeringNeeded) {
            return false;
        }
        if (this.items[0] == ItemStack.EMPTY) {
            return false;
        }
        int denier = (money -= 8) % 64;
        int denier_argent = (money - denier) / 64 % 64;
        int denier_or = (money - denier - denier_argent * 64) / 4096;
        this.items[1] = denier == 0 ? ItemStack.EMPTY : new ItemStack((Item)MillItems.DENIER, denier);
        this.items[2] = denier_argent == 0 ? ItemStack.EMPTY : new ItemStack((Item)MillItems.DENIER_ARGENT, denier_argent);
        this.items[3] = denier_or == 0 ? ItemStack.EMPTY : new ItemStack((Item)MillItems.DENIER_OR, denier_or);
        return true;
    }

    public void writeToNBT(CompoundTag par1NBTTagCompound) {
        if (this.currentTarget != null) {
            par1NBTTagCompound.putShort("enchantmentTarget", (short)Enchantment.func_185258_b((Enchantment)this.currentTarget.enchantment));
            if (MillConfigValues.LogPujas >= 2) {
                MillLog.minor(this, "Writing enchantmentTarget: " + this.currentTarget.enchantment + ", " + this.currentTarget);
            }
        }
        par1NBTTagCompound.putShort("offeringProgress", (short)this.offeringProgress);
        par1NBTTagCompound.putShort("pujaProgress", this.pujaProgress);
        ListTag nbttaglist = new ListTag();
        for (int i = 0; i < this.items.length; ++i) {
            if (this.items[i] == null) continue;
            CompoundTag nbttagcompound = new CompoundTag();
            nbttagcompound.putByte("Slot", (byte)i);
            this.items[i].save(nbttagcompound);
            nbttaglist.add((Tag)nbttagcompound);
        }
        par1NBTTagCompound.put("Items", (Tag)nbttaglist);
    }

    public static class PrayerTarget {
        public final Enchantment enchantment;
        public final String mouseOver;
        public final int startX;
        public final int startY;
        public final int startXact;
        public final int startYact;
        public final int toolType;

        public PrayerTarget(Enchantment enchantment, String mouseOver, int startX, int startY, int startXact, int startYact, int toolType) {
            this.enchantment = enchantment;
            this.mouseOver = mouseOver;
            this.startX = startX;
            this.startY = startY;
            this.startXact = startXact;
            this.startYact = startYact;
            this.toolType = toolType;
        }

        public boolean validForItem(Item item) {
            return PujaSacrifice.validForItem(this.toolType, item);
        }
    }
}

