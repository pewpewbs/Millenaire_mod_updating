package org.millenaire.common.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import org.millenaire.common.advancements.MillAdvancements;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.item.TradeGood;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.WorldUtilities;
import org.millenaire.common.village.Building;
import org.millenaire.common.world.UserProfile;

public class ContainerTrade extends AbstractContainerMenu {
   public static final int DONATION_REP_MULTIPLIER = 4;
   private Building building;
   private MillVillager merchant;
   public int nbRowSelling = 0;
   public int nbRowBuying = 0;

   public ContainerTrade(Player player, Building building) {
      this.building = building;
      Set<TradeGood> sellingGoods = building.getSellingGoods(player);
      int slotnb = 0;
      if (sellingGoods != null) {
         for (TradeGood g : sellingGoods) {
            int slotrow = slotnb / 13;
            this.addSlot(new ContainerTrade.TradeSlot(building, player, true, g, 8 + 18 * (slotnb - 13 * slotrow), 32 + slotrow * 18));
            slotnb++;
         }
      }

      this.nbRowSelling = slotnb / 13 + 1;
      Set<TradeGood> buyingGoods = building.getBuyingGoods(player);
      slotnb = 0;
      if (buyingGoods != null) {
         for (TradeGood g : buyingGoods) {
            int slotrow = slotnb / 13;
            this.addSlot(new ContainerTrade.TradeSlot(building, player, false, g, 8 + 18 * (slotnb - 13 * slotrow), 86 + slotrow * 18));
            slotnb++;
         }
      }

      this.nbRowBuying = slotnb / 13 + 1;

      for (int l = 0; l < 3; l++) {
         for (int k1 = 0; k1 < 9; k1++) {
            this.addSlot(new Slot(player.getInventory(), k1 + l * 9 + 9, 8 + k1 * 18 + 36, 103 + l * 18 + 37));
         }
      }

      for (int i1 = 0; i1 < 9; i1++) {
         this.addSlot(new Slot(player.getInventory(), i1, 8 + i1 * 18 + 36, 198));
      }

      if (!building.world.isClientSide) {
         UserProfile profile = building.mw.getProfile(player);
         this.unlockTradableGoods(profile);
      }
   }

   public ContainerTrade(Player player, MillVillager merchant) {
      this.merchant = merchant;
      int slotnb = 0;
      Set<TradeGood> sellingGoods = merchant.merchantSells.keySet();
      if (sellingGoods != null) {
         for (TradeGood g : sellingGoods) {
            int slotrow = slotnb / 13;
            this.addSlot(new ContainerTrade.MerchantSlot(merchant, player, g, 8 + 18 * (slotnb - 13 * slotrow), 32 + slotrow * 18));
            slotnb++;
         }
      }

      this.nbRowSelling = slotnb / 13 + 1;

      for (int l = 0; l < 3; l++) {
         for (int k1 = 0; k1 < 9; k1++) {
            this.addSlot(new Slot(player.getInventory(), k1 + l * 9 + 9, 8 + k1 * 18 + 36, 103 + l * 18 + 37));
         }
      }

      for (int i1 = 0; i1 < 9; i1++) {
         this.addSlot(new Slot(player.getInventory(), i1, 8 + i1 * 18 + 36, 198));
      }

      if (!merchant.level.isClientSide) {
         UserProfile profile = merchant.mw.getProfile(player);
         this.unlockTradableGoods(profile);
      }
   }

   public boolean stillValid(Player entityplayer) {
      return true;
   }

   public ItemStack clicked(int slotId, int dragType, ClickType clickTypeIn, Player player) {
      TradeGood good = null;
      if (slotId >= 0 && slotId < this.slots.size()) {
         Slot slot = (Slot)this.slots.get(slotId);
         if (slot != null && slot instanceof ContainerTrade.TradeSlot) {
            ContainerTrade.TradeSlot tslot = (ContainerTrade.TradeSlot)slot;
            good = tslot.good;
            UserProfile profile = this.building.mw.getProfile(player);
            int nbItems = 1;
            if (clickTypeIn == ClickType.QUICK_MOVE) {
               nbItems = 64;
            } else if (clickTypeIn == ClickType.PICKUP && dragType == 1) {
               nbItems = 8;
            }

            if (tslot.isProblem() == null) {
               int playerMoney = MillCommonUtilities.countMoney(player.getInventory());
               if (tslot.sellingSlot) {
                  if (playerMoney < good.getCalculatedSellingPrice(this.building, player) * nbItems) {
                     nbItems = Mth.floor(playerMoney / good.getCalculatedSellingPrice(this.building, player));
                  }

                  if (!good.autoGenerate && this.building.countGoods(good.item.getItem(), good.item.meta) < nbItems) {
                     nbItems = this.building.countGoods(good.item.getItem(), good.item.meta);
                  }

                  nbItems = MillCommonUtilities.putItemsInChest(player.getInventory(), good.item.getItem(), good.item.meta, nbItems);
                  MillCommonUtilities.changeMoney(player.getInventory(), -good.getCalculatedSellingPrice(this.building, player) * nbItems, player);
                  if (!good.autoGenerate) {
                     this.building.takeGoods(good.item.getItem(), good.item.meta, nbItems);
                  }

                  if (this.building.getTownHall().controlledBy != null && !this.building.getTownHall().controlledBy.equals(player.getUUID())) {
                     Player owner = this.building.world.getPlayerByUUID(this.building.getTownHall().controlledBy);
                     if (owner != null) {
                        MillAdvancements.MP_NEIGHBOURTRADE.grant(owner);
                     }
                  }

                  this.building.adjustReputation(player, good.getCalculatedSellingPrice(this.building, player) * nbItems);
                  this.building.getTownHall().adjustLanguage(player, nbItems);
               } else {
                  if (MillCommonUtilities.countChestItems(player.getInventory(), good.item.getItem(), good.item.meta) < nbItems) {
                     nbItems = MillCommonUtilities.countChestItems(player.getInventory(), good.item.getItem(), good.item.meta);
                  }

                  nbItems = this.building.storeGoods(good.item.getItem(), good.item.meta, nbItems);
                  WorldUtilities.getItemsFromChest(player.getInventory(), good.item.getItem(), good.item.meta, nbItems);
                  if (!profile.donationActivated) {
                     MillCommonUtilities.changeMoney(player.getInventory(), good.getCalculatedBuyingPrice(this.building, player) * nbItems, player);
                  }

                  if (this.building.getTownHall().controlledBy != null && !this.building.getTownHall().controlledBy.equals(player.getUUID())) {
                     Player owner = this.building.world.getPlayerByUUID(this.building.getTownHall().controlledBy);
                     if (owner != null) {
                        MillAdvancements.MP_NEIGHBOURTRADE.grant(owner);
                     }
                  }

                  int repMultiplier = 1;
                  if (profile.donationActivated) {
                     repMultiplier = 4;
                  }

                  this.building.adjustReputation(player, good.getCalculatedBuyingPrice(this.building, player) * nbItems * repMultiplier);
                  this.building.getTownHall().adjustLanguage(player, nbItems);
               }
            }

            this.broadcastChanges();
            this.building.invalidateInventoryCache();
            if (!this.building.world.isClientSide) {
               this.building.sendChestPackets(player);
            }

            if (!this.building.world.isClientSide) {
               this.unlockTradableGoods(profile);
            }

            return slot.getItem();
         }

         if (slot != null && slot instanceof ContainerTrade.MerchantSlot) {
            ContainerTrade.MerchantSlot tslotx = (ContainerTrade.MerchantSlot)slot;
            good = tslotx.good;
            int nbItemsx = 1;
            if (dragType == 1) {
               nbItemsx = 64;
            }

            if (tslotx.isProblem() == null) {
               int playerMoney = MillCommonUtilities.countMoney(player.getInventory());
               if (playerMoney < good.getCalculatedSellingPrice(this.merchant) * nbItemsx) {
                  nbItemsx = Mth.floor(playerMoney / good.getCalculatedSellingPrice(this.merchant));
               }

               if (this.merchant.getHouse().countGoods(good.item) < nbItemsx) {
                  nbItemsx = this.merchant.getHouse().countGoods(good.item);
               }

               nbItemsx = MillCommonUtilities.putItemsInChest(player.getInventory(), good.item.getItem(), good.item.meta, nbItemsx);
               MillCommonUtilities.changeMoney(player.getInventory(), -good.getCalculatedSellingPrice(this.merchant) * nbItemsx, player);
               this.merchant.getHouse().takeGoods(good.item, nbItemsx);
               Mill.getMillWorld(player.level).getProfile(player).adjustLanguage(this.merchant.getCulture().key, nbItemsx);
            }

            this.broadcastChanges();
            this.merchant.getHouse().invalidateInventoryCache();
            if (!this.merchant.getHouse().world.isClientSide) {
               this.merchant.getHouse().sendChestPackets(player);
            }

            if (!this.merchant.level.isClientSide) {
               UserProfile profilex = this.merchant.mw.getProfile(player);
               this.unlockTradableGoods(profilex);
            }

            return slot.getItem();
         }
      }

      return null;
   }

   private void unlockTradableGoods(UserProfile profile) {
      List<TradeGood> unlockedGoods = new ArrayList<>();

      for (Slot slot : this.slots) {
         if (slot instanceof ContainerTrade.TradeSlot) {
            ContainerTrade.TradeSlot tradeSlot = (ContainerTrade.TradeSlot)slot;
            if (tradeSlot.isProblem() == null) {
               unlockedGoods.add(tradeSlot.good);
            }
         } else if (slot instanceof ContainerTrade.MerchantSlot) {
            ContainerTrade.MerchantSlot merchantSlot = (ContainerTrade.MerchantSlot)slot;
            if (merchantSlot.isProblem() == null) {
               unlockedGoods.add(merchantSlot.good);
            }
         }
      }

      if (!unlockedGoods.isEmpty()) {
         if (this.building != null) {
            profile.unlockTradeGoods(this.building.culture, unlockedGoods);
         } else if (this.merchant != null) {
            profile.unlockTradeGoods(this.merchant.getCulture(), unlockedGoods);
         }
      }
   }

   public static class MerchantSlot extends Slot {
      public MillVillager merchant;
      public Player player;
      public final TradeGood good;

      public MerchantSlot(MillVillager merchant, Player player, TradeGood good, int xpos, int ypos) {
         super(null, -1, xpos, ypos);
         this.merchant = merchant;
         this.good = good;
         this.player = player;
      }

      public ItemStack remove(int i) {
         return null;
      }

      public boolean hasItem() {
         return this.getItem() != null;
      }

      public int getMaxStackSize() {
         return 0;
      }

      public ItemStack getItem() {
         return org.millenaire.common.item.LegacyItemStacks.stack(this.good.item.getItem(), Math.max(Math.min(this.merchant.getHouse().countGoods(this.good.item), 99), 1), this.good.item.meta);
      }

      public boolean mayPlace(ItemStack itemstack) {
         return true;
      }

      public String isProblem() {
         if (this.merchant.getHouse().countGoods(this.good.item) < 1) {
            return LanguageUtilities.string("ui.outofstock");
         } else {
            int playerMoney = MillCommonUtilities.countMoney(this.player.getInventory());
            if (this.merchant.getCulture().getTradeGood(this.good.item) != null) {
               if (playerMoney < this.good.getCalculatedSellingPrice(this.merchant)) {
                  return LanguageUtilities.string("ui.missingdeniers").replace("<0>", "" + (this.good.getCalculatedSellingPrice(this.merchant) - playerMoney));
               }
            } else {
               MillLog.error(null, "Unknown trade good: " + this.good);
            }

            return null;
         }
      }

      public void setChanged() {
      }

      public void set(ItemStack itemstack) {
      }

      public String toString() {
         return this.good.getName();
      }
   }

   public static class TradeSlot extends Slot {
      public final Building building;
      public final Player player;
      public final TradeGood good;
      public final boolean sellingSlot;

      public TradeSlot(Building building, Player player, boolean sellingSlot, TradeGood good, int xpos, int ypos) {
         super(player.getInventory(), -1, xpos, ypos);
         this.building = building;
         if (good.item.item == Items.AIR) {
            MillLog.error(good, "Trying to add air to the trade UI.");
         }

         this.good = good;
         this.player = player;
         this.sellingSlot = sellingSlot;
      }

      public ItemStack remove(int i) {
         return null;
      }

      public boolean hasItem() {
         return this.getItem() != null;
      }

      public int getMaxStackSize() {
         return 0;
      }

      public ItemStack getItem() {
         return this.sellingSlot
            ? org.millenaire.common.item.LegacyItemStacks.stack(
               this.good.item.getItem(),
               Math.max(Math.min(this.building.countGoods(this.good.item.getItem(), this.good.item.meta), 99), 1),
               this.good.item.meta
            )
            : org.millenaire.common.item.LegacyItemStacks.stack(
               this.good.item.getItem(),
               Math.max(Math.min(MillCommonUtilities.countChestItems(this.player.getInventory(), this.good.item.getItem(), this.good.item.meta), 99), 1),
               this.good.item.meta
            );
      }

      public boolean mayPlace(ItemStack itemstack) {
         return true;
      }

      public String isProblem() {
         if (this.sellingSlot) {
            if (this.building.countGoods(this.good.item.getItem(), this.good.item.meta) < 1
               && this.good.requiredTag != null
               && !this.building.containsTags(this.good.requiredTag)) {
               return LanguageUtilities.string("ui.missingequipment") + ": " + this.good.requiredTag;
            }

            if (this.building.countGoods(this.good.item.getItem(), this.good.item.meta) < 1 && !this.good.autoGenerate) {
               return LanguageUtilities.string("ui.outofstock");
            }

            if (this.building.getTownHall().getReputation(this.player) < this.good.minReputation) {
               return LanguageUtilities.string("ui.reputationneeded", this.building.culture.getReputationLevelLabel(this.good.minReputation));
            }

            int playerMoney = MillCommonUtilities.countMoney(this.player.getInventory());
            if (playerMoney < this.good.getCalculatedSellingPrice(this.building, this.player)) {
               return LanguageUtilities.string("ui.missingdeniers")
                  .replace("<0>", "" + (this.good.getCalculatedSellingPrice(this.building, this.player) - playerMoney));
            }
         } else if (MillCommonUtilities.countChestItems(this.player.getInventory(), this.good.item.getItem(), this.good.item.meta) == 0) {
            return LanguageUtilities.string("ui.noneininventory");
         }

         return null;
      }

      public void setChanged() {
      }

      public void set(ItemStack itemstack) {
      }

      public String toString() {
         return this.good.name + (this.sellingSlot ? LanguageUtilities.string("ui.selling") : LanguageUtilities.string("ui.buying"));
      }
   }
}
