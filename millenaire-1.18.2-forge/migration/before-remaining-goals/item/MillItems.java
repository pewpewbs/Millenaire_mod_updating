/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.init.MobEffects
 *  net.minecraft.init.SoundEvents
 *  net.minecraft.inventory.EntityEquipmentSlot
 *  net.minecraft.item.EnumDyeColor
 *  net.minecraft.item.Item
 *  net.minecraft.item.Item$ToolMaterial
 *  net.minecraft.item.ItemArmor$ArmorMaterial
 *  net.minecraft.potion.PotionEffect
 *  net.minecraft.util.SoundEvent
 *  net.minecraftforge.common.util.EnumHelper
 *  net.minecraftforge.event.RegistryEvent$Register
 *  net.minecraftforge.fml.common.registry.GameRegistry$ObjectHolder
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 *  net.minecraftforge.registries.IForgeRegistryEntry
 */
package org.millenaire.common.item;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.registries.ObjectHolder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.item.ItemAmuletAlchemist;
import org.millenaire.common.item.ItemAmuletSkollHati;
import org.millenaire.common.item.ItemAmuletVishnu;
import org.millenaire.common.item.ItemAmuletYggdrasil;
import org.millenaire.common.item.ItemBannerPattern;
import org.millenaire.common.item.ItemBrickMould;
import org.millenaire.common.item.ItemClothes;
import org.millenaire.common.item.ItemFoodMultiple;
import org.millenaire.common.item.ItemMayanQuestCrown;
import org.millenaire.common.item.ItemMill;
import org.millenaire.common.item.ItemMillSeeds;
import org.millenaire.common.item.ItemMillenaireArmour;
import org.millenaire.common.item.ItemMillenaireAxe;
import org.millenaire.common.item.ItemMillenaireBow;
import org.millenaire.common.item.ItemMillenaireHoe;
import org.millenaire.common.item.ItemMillenairePickaxe;
import org.millenaire.common.item.ItemMillenaireShovel;
import org.millenaire.common.item.ItemMillenaireSword;
import org.millenaire.common.item.ItemMockBanner;
import org.millenaire.common.item.ItemNegationWand;
import org.millenaire.common.item.ItemPaintBucket;
import org.millenaire.common.item.ItemParchment;
import org.millenaire.common.item.ItemPurse;
import org.millenaire.common.item.ItemSummoningWand;
import org.millenaire.common.item.ItemUlu;
import org.millenaire.common.item.ItemWallDecoration;
import org.millenaire.common.utilities.MillLog;

public class MillItems {
    static net.minecraft.world.item.Tier TOOLS_norman = MillMaterials.tool((String)"normanTools", (int)2, (int)1561, (float)10.0f, (float)4.0f, (int)10);
    static net.minecraft.world.item.Tier better_steel = MillMaterials.tool((String)"bettersteel", (int)2, (int)1561, (float)5.0f, (float)3.0f, (int)10);
    static net.minecraft.world.item.Tier TOOLS_byzantine = MillMaterials.tool((String)"byzantineTools", (int)2, (int)1561, (float)12.0f, (float)3.0f, (int)15);
    static net.minecraft.world.item.Tier TOOLS_obsidian = MillMaterials.tool((String)"obsidianTools", (int)3, (int)1561, (float)6.0f, (float)2.0f, (int)25);
    static net.minecraft.world.item.ArmorMaterial ARMOUR_norman = MillMaterials.armor((String)"normanArmour", (String)"millenaire:norman", (int)66, (int[])new int[]{3, 8, 6, 3}, (int)10, (SoundEvent)SoundEvents.ARMOR_EQUIP_IRON, (float)0.0f);
    static net.minecraft.world.item.ArmorMaterial ARMOUR_japanese_red = MillMaterials.armor((String)"japanese", (String)"millenaire:japanese_red", (int)33, (int[])new int[]{2, 6, 5, 2}, (int)25, (SoundEvent)SoundEvents.ARMOR_EQUIP_IRON, (float)0.0f);
    static net.minecraft.world.item.ArmorMaterial ARMOUR_japanese_blue = MillMaterials.armor((String)"japanese", (String)"millenaire:japanese_blue", (int)33, (int[])new int[]{2, 6, 5, 2}, (int)25, (SoundEvent)SoundEvents.ARMOR_EQUIP_IRON, (float)0.0f);
    static net.minecraft.world.item.ArmorMaterial ARMOUR_japaneseGuard = MillMaterials.armor((String)"japaneseGuard", (String)"millenaire:japanese_guard", (int)25, (int[])new int[]{2, 5, 4, 1}, (int)25, (SoundEvent)SoundEvents.ARMOR_EQUIP_IRON, (float)0.0f);
    static net.minecraft.world.item.ArmorMaterial ARMOUR_byzantine = MillMaterials.armor((String)"byzantineArmour", (String)"millenaire:byzantine", (int)33, (int[])new int[]{3, 8, 6, 3}, (int)20, (SoundEvent)SoundEvents.ARMOR_EQUIP_IRON, (float)0.0f);
    static net.minecraft.world.item.ArmorMaterial ARMOUR_mayan_quest_crown = MillMaterials.armor((String)"mayanQuestCrown", (String)"millenaire:mayan_quest_crown", (int)0, (int[])new int[]{3, 6, 8, 3}, (int)10, (SoundEvent)SoundEvents.ARMOR_EQUIP_DIAMOND, (float)2.0f);
    static net.minecraft.world.item.ArmorMaterial ARMOUR_SELJUK = MillMaterials.armor((String)"seljukArmour", (String)"millenaire:seljuk", (int)66, (int[])new int[]{3, 8, 6, 3}, (int)10, (SoundEvent)SoundEvents.ARMOR_EQUIP_IRON, (float)0.0f);
    static net.minecraft.world.item.ArmorMaterial ARMOUR_SELJUK_WOOL = MillMaterials.armor((String)"seljukWoolArmour", (String)"millenaire:seljuk_wool", (int)7, (int[])new int[]{2, 5, 3, 1}, (int)10, (SoundEvent)SoundEvents.WOOL_PLACE, (float)0.0f);
    static net.minecraft.world.item.ArmorMaterial ARMOUR_fur = MillMaterials.armor((String)"fur", (String)"millenaire:furcoat", (int)7, (int[])new int[]{2, 5, 3, 1}, (int)25, (SoundEvent)SoundEvents.ARMOR_EQUIP_LEATHER, (float)0.0f);
    @ObjectHolder(value="millenaire:summoningwand")
    public static ItemSummoningWand SUMMONING_WAND;
    @ObjectHolder(value="millenaire:negationwand")
    public static ItemNegationWand NEGATION_WAND;
    @ObjectHolder(value="millenaire:purse")
    public static ItemPurse PURSE;
    @ObjectHolder(value="millenaire:denier")
    public static ItemMill DENIER;
    @ObjectHolder(value="millenaire:denierargent")
    public static ItemMill DENIER_ARGENT;
    @ObjectHolder(value="millenaire:denieror")
    public static ItemMill DENIER_OR;
    @ObjectHolder(value="millenaire:ciderapple")
    public static ItemFoodMultiple CIDER_APPLE;
    @ObjectHolder(value="millenaire:cider")
    public static ItemFoodMultiple CIDER;
    @ObjectHolder(value="millenaire:calva")
    public static ItemFoodMultiple CALVA;
    @ObjectHolder(value="millenaire:boudin")
    public static ItemFoodMultiple BOUDIN;
    @ObjectHolder(value="millenaire:tripes")
    public static ItemFoodMultiple TRIPES;
    @ObjectHolder(value="millenaire:normanhoe")
    public static ItemMillenaireHoe NORMAN_HOE;
    @ObjectHolder(value="millenaire:normanaxe")
    public static ItemMillenaireAxe NORMAN_AXE;
    @ObjectHolder(value="millenaire:normanpickaxe")
    public static ItemMillenairePickaxe NORMAN_PICKAXE;
    @ObjectHolder(value="millenaire:normanshovel")
    public static ItemMillenaireShovel NORMAN_SHOVEL;
    @ObjectHolder(value="millenaire:normanbroadsword")
    public static ItemMillenaireSword NORMAN_SWORD;
    @ObjectHolder(value="millenaire:normanhelmet")
    public static ItemMillenaireArmour NORMAN_HELMET;
    @ObjectHolder(value="millenaire:normanplate")
    public static ItemMillenaireArmour NORMAN_CHESTPLATE;
    @ObjectHolder(value="millenaire:normanlegs")
    public static ItemMillenaireArmour NORMAN_LEGGINGS;
    @ObjectHolder(value="millenaire:normanboots")
    public static ItemMillenaireArmour NORMAN_BOOTS;
    @ObjectHolder(value="millenaire:tapestry")
    public static ItemWallDecoration TAPESTRY;
    @ObjectHolder(value="millenaire:mayanhoe")
    public static ItemMillenaireHoe MAYAN_HOE;
    @ObjectHolder(value="millenaire:mayanaxe")
    public static ItemMillenaireAxe MAYAN_AXE;
    @ObjectHolder(value="millenaire:mayanpickaxe")
    public static ItemMillenairePickaxe MAYAN_PICKAXE;
    @ObjectHolder(value="millenaire:mayanshovel")
    public static ItemMillenaireShovel MAYAN_SHOVEL;
    @ObjectHolder(value="millenaire:mayanmace")
    public static ItemMillenaireSword MAYAN_MACE;
    @ObjectHolder(value="millenaire:byzantinehoe")
    public static ItemMillenaireHoe BYZANTINE_HOE;
    @ObjectHolder(value="millenaire:byzantineaxe")
    public static ItemMillenaireAxe BYZANTINE_AXE;
    @ObjectHolder(value="millenaire:byzantinepickaxe")
    public static ItemMillenairePickaxe BYZANTINE_PICKAXE;
    @ObjectHolder(value="millenaire:byzantineshovel")
    public static ItemMillenaireShovel BYZANTINE_SHOVEL;
    @ObjectHolder(value="millenaire:byzantinemace")
    public static ItemMillenaireSword BYZANTINE_MACE;
    @ObjectHolder(value="millenaire:yumibow")
    public static ItemMillenaireBow YUMI_BOW;
    @ObjectHolder(value="millenaire:tachisword")
    public static ItemMillenaireSword TACHI_SWORD;
    @ObjectHolder(value="millenaire:olives")
    public static ItemFoodMultiple OLIVES;
    @ObjectHolder(value="millenaire:oliveoil")
    public static ItemFoodMultiple OLIVEOIL;
    @ObjectHolder(value="millenaire:byzantinehelmet")
    public static ItemMillenaireArmour BYZANTINE_HELMET;
    @ObjectHolder(value="millenaire:byzantineplate")
    public static ItemMillenaireArmour BYZANTINE_CHESTPLATE;
    @ObjectHolder(value="millenaire:byzantinelegs")
    public static ItemMillenaireArmour BYZANTINE_LEGGINGS;
    @ObjectHolder(value="millenaire:byzantineboots")
    public static ItemMillenaireArmour BYZANTINE_BOOTS;
    @ObjectHolder(value="millenaire:japaneseredhelmet")
    public static ItemMillenaireArmour JAPANESE_RED_HELMET;
    @ObjectHolder(value="millenaire:japaneseredplate")
    public static ItemMillenaireArmour JAPANESE_RED_CHESTPLATE;
    @ObjectHolder(value="millenaire:japaneseredlegs")
    public static ItemMillenaireArmour JAPANESE_RED_LEGGINGS;
    @ObjectHolder(value="millenaire:japaneseredboots")
    public static ItemMillenaireArmour JAPANESE_RED_BOOTS;
    @ObjectHolder(value="millenaire:japanesebluehelmet")
    public static ItemMillenaireArmour JAPANESE_BLUE_HELMET;
    @ObjectHolder(value="millenaire:japaneseblueplate")
    public static ItemMillenaireArmour JAPANESE_BLUE_CHESTPLATE;
    @ObjectHolder(value="millenaire:japanesebluelegs")
    public static ItemMillenaireArmour JAPANESE_BLUE_LEGGINGS;
    @ObjectHolder(value="millenaire:japaneseblueboots")
    public static ItemMillenaireArmour JAPANESE_BLUE_BOOTS;
    @ObjectHolder(value="millenaire:japaneseguardhelmet")
    public static ItemMillenaireArmour JAPANESE_GUARD_HELMET;
    @ObjectHolder(value="millenaire:japaneseguardplate")
    public static ItemMillenaireArmour JAPANESE_GUARD_CHESTPLATE;
    @ObjectHolder(value="millenaire:japaneseguardlegs")
    public static ItemMillenaireArmour JAPANESE_GUARD_LEGGINGS;
    @ObjectHolder(value="millenaire:japaneseguardboots")
    public static ItemMillenaireArmour JAPANESE_GUARD_BOOTS;
    @ObjectHolder(value="millenaire:parchment_normanvillagers")
    public static ItemParchment PARCHMENT_NORMAN_VILLAGERS;
    @ObjectHolder(value="millenaire:parchment_normanbuildings")
    public static ItemParchment PARCHMENT_NORMAN_BUILDINGS;
    @ObjectHolder(value="millenaire:parchment_normanitems")
    public static ItemParchment PARCHMENT_NORMAN_ITEMS;
    @ObjectHolder(value="millenaire:parchment_normanfull")
    public static ItemParchment PARCHMENT_NORMAN_COMPLETE;
    @ObjectHolder(value="millenaire:parchment_indianvillagers")
    public static ItemParchment PARCHMENT_INDIAN_VILLAGERS;
    @ObjectHolder(value="millenaire:parchment_indianbuildings")
    public static ItemParchment PARCHMENT_INDIAN_BUILDINGS;
    @ObjectHolder(value="millenaire:parchment_indianitems")
    public static ItemParchment PARCHMENT_INDIAN_ITEMS;
    @ObjectHolder(value="millenaire:parchment_indianfull")
    public static ItemParchment PARCHMENT_INDIAN_COMPLETE;
    @ObjectHolder(value="millenaire:parchment_japanesevillagers")
    public static ItemParchment PARCHMENT_JAPANESE_VILLAGERS;
    @ObjectHolder(value="millenaire:parchment_japanesebuildings")
    public static ItemParchment PARCHMENT_JAPANESE_BUILDINGS;
    @ObjectHolder(value="millenaire:parchment_japaneseitems")
    public static ItemParchment PARCHMENT_JAPANESE_ITEMS;
    @ObjectHolder(value="millenaire:parchment_japanesefull")
    public static ItemParchment PARCHMENT_JAPANESE_COMPLETE;
    @ObjectHolder(value="millenaire:parchment_mayanvillagers")
    public static ItemParchment PARCHMENT_MAYAN_VILLAGERS;
    @ObjectHolder(value="millenaire:parchment_mayanbuildings")
    public static ItemParchment PARCHMENT_MAYAN_BUILDINGS;
    @ObjectHolder(value="millenaire:parchment_mayanitems")
    public static ItemParchment PARCHMENT_MAYAN_ITEMS;
    @ObjectHolder(value="millenaire:parchment_mayanfull")
    public static ItemParchment PARCHMENT_MAYAN_COMPLETE;
    @ObjectHolder(value="millenaire:vishnu_amulet")
    public static ItemMill AMULET_VISHNU;
    @ObjectHolder(value="millenaire:alchemist_amulet")
    public static ItemMill AMULET_ALCHEMIST;
    @ObjectHolder(value="millenaire:yggdrasil_amulet")
    public static ItemMill AMULET_YDDRASIL;
    @ObjectHolder(value="millenaire:skoll_hati_amulet")
    public static ItemMill AMULET_SKOLL_HATI;
    @ObjectHolder(value="millenaire:parchment_villagescroll")
    public static ItemParchment PARCHMENT_VILLAGE_SCROLL;
    @ObjectHolder(value="millenaire:rice")
    public static ItemMillSeeds RICE;
    @ObjectHolder(value="millenaire:turmeric")
    public static ItemMillSeeds TURMERIC;
    @ObjectHolder(value="millenaire:maize")
    public static ItemMillSeeds MAIZE;
    @ObjectHolder(value="millenaire:grapes")
    public static ItemMillSeeds GRAPES;
    @ObjectHolder(value="millenaire:vegcurry")
    public static ItemFoodMultiple VEG_CURRY;
    @ObjectHolder(value="millenaire:chickencurry")
    public static ItemFoodMultiple CHICKEN_CURRY;
    @ObjectHolder(value="millenaire:brickmould")
    public static ItemMill BRICK_MOULD;
    @ObjectHolder(value="millenaire:rasgulla")
    public static ItemFoodMultiple RASGULLA;
    @ObjectHolder(value="millenaire:indianstatue")
    public static ItemWallDecoration INDIAN_STATUE;
    @ObjectHolder(value="millenaire:mayanstatue")
    public static ItemWallDecoration MAYAN_STATUE;
    @ObjectHolder(value="millenaire:wah")
    public static ItemFoodMultiple WAH;
    @ObjectHolder(value="millenaire:balche")
    public static ItemFoodMultiple BLANCHE;
    @ObjectHolder(value="millenaire:sikilpah")
    public static ItemFoodMultiple SIKILPAH;
    @ObjectHolder(value="millenaire:masa")
    public static ItemFoodMultiple MASA;
    @ObjectHolder(value="millenaire:parchment_sadhu")
    public static ItemParchment PARCHMENT_SADHU;
    @ObjectHolder(value="millenaire:unknownpowder")
    public static ItemMill UNKNOWN_POWDER;
    @ObjectHolder(value="millenaire:udon")
    public static ItemFoodMultiple UDON;
    @ObjectHolder(value="millenaire:obsidianflake")
    public static ItemMill OBSIDIAN_FLAKE;
    @ObjectHolder(value="millenaire:silk")
    public static ItemMill SILK;
    @ObjectHolder(value="millenaire:byzantineiconsmall")
    public static ItemWallDecoration BYZANTINE_ICON_SMALL;
    @ObjectHolder(value="millenaire:byzantineiconmedium")
    public static ItemWallDecoration BYZANTINE_ICON_MEDIUM;
    @ObjectHolder(value="millenaire:byzantineiconlarge")
    public static ItemWallDecoration BYZANTINE_ICON_LARGE;
    @ObjectHolder(value="millenaire:clothes_byz_wool")
    public static ItemClothes BYZANTINE_CLOTH_WOOL;
    @ObjectHolder(value="millenaire:clothes_byz_silk")
    public static ItemClothes BYZANTINE_CLOTH_SILK;
    @ObjectHolder(value="millenaire:winefancy")
    public static ItemFoodMultiple WINE_FANCY;
    @ObjectHolder(value="millenaire:winebasic")
    public static ItemFoodMultiple WINE_BASIC;
    @ObjectHolder(value="millenaire:feta")
    public static ItemFoodMultiple FETA;
    @ObjectHolder(value="millenaire:souvlaki")
    public static ItemFoodMultiple SOUVLAKI;
    @ObjectHolder(value="millenaire:sake")
    public static ItemFoodMultiple SAKE;
    @ObjectHolder(value="millenaire:cacauhaa")
    public static ItemFoodMultiple CACAUHAA;
    @ObjectHolder(value="millenaire:mayanquestcrown")
    public static ItemMayanQuestCrown MAYAN_QUEST_CROWN;
    @ObjectHolder(value="millenaire:ikayaki")
    public static ItemFoodMultiple IKAYAKI;
    @ObjectHolder(value="millenaire:bearmeat_raw")
    public static ItemFoodMultiple BEARMEAT_RAW;
    @ObjectHolder(value="millenaire:bearmeat_cooked")
    public static ItemFoodMultiple BEARMEAT_COOKED;
    @ObjectHolder(value="millenaire:wolfmeat_raw")
    public static ItemFoodMultiple WOLFMEAT_RAW;
    @ObjectHolder(value="millenaire:wolfmeat_cooked")
    public static ItemFoodMultiple WOLFMEAT_COOKED;
    @ObjectHolder(value="millenaire:seafood_raw")
    public static ItemFoodMultiple SEAFOOD_RAW;
    @ObjectHolder(value="millenaire:seafood_cooked")
    public static ItemFoodMultiple SEAFOOD_COOKED;
    @ObjectHolder(value="millenaire:inuitbearstew")
    public static ItemFoodMultiple INUITBEARSTEW;
    @ObjectHolder(value="millenaire:inuitmeatystew")
    public static ItemFoodMultiple INUITMEATYSTEW;
    @ObjectHolder(value="millenaire:inuitpotatostew")
    public static ItemFoodMultiple INUITPOTATOSTEW;
    @ObjectHolder(value="millenaire:furhelmet")
    public static ItemMillenaireArmour FUR_HELMET;
    @ObjectHolder(value="millenaire:furplate")
    public static ItemMillenaireArmour FUR_CHESTPLATE;
    @ObjectHolder(value="millenaire:furlegs")
    public static ItemMillenaireArmour FUR_LEGGINGS;
    @ObjectHolder(value="millenaire:furboots")
    public static ItemMillenaireArmour FUR_BOOTS;
    @ObjectHolder(value="millenaire:inuitbow")
    public static ItemMillenaireBow INUIT_BOW;
    @ObjectHolder(value="millenaire:ulu")
    public static ItemMill ULU;
    @ObjectHolder(value="millenaire:inuittrident")
    public static ItemMillenaireSword INUIT_TRIDENT;
    @ObjectHolder(value="millenaire:tannedhide")
    public static ItemMill TANNEDHIDE;
    @ObjectHolder(value="millenaire:hidehanging")
    public static ItemWallDecoration HIDEHANGING;
    @ObjectHolder(value="millenaire:villagebanner")
    public static ItemMockBanner VILLAGEBANNER;
    @ObjectHolder(value="millenaire:culturebanner")
    public static ItemMockBanner CULTUREBANNER;
    @ObjectHolder(value="millenaire:bannerpattern")
    public static ItemBannerPattern BANNERPATTERN;
    @ObjectHolder(value="millenaire:ayran")
    public static ItemFoodMultiple AYRAN;
    @ObjectHolder(value="millenaire:yogurt")
    public static ItemFoodMultiple YOGURT;
    @ObjectHolder(value="millenaire:pide")
    public static ItemFoodMultiple PIDE;
    @ObjectHolder(value="millenaire:lokum")
    public static ItemFoodMultiple LOKUM;
    @ObjectHolder(value="millenaire:helva")
    public static ItemFoodMultiple HELVA;
    @ObjectHolder(value="millenaire:pistachios")
    public static ItemFoodMultiple PISTACHIOS;
    @ObjectHolder(value="millenaire:cotton")
    public static ItemMillSeeds COTTON;
    @ObjectHolder(value="millenaire:seljukscimitar")
    public static ItemMillenaireSword SELJUK_SCIMITAR;
    @ObjectHolder(value="millenaire:seljukbow")
    public static ItemMillenaireBow SElJUK_BOW;
    @ObjectHolder(value="millenaire:seljukturban")
    public static ItemMillenaireArmour SELJUK_TURBAN;
    @ObjectHolder(value="millenaire:seljukhelmet")
    public static ItemMillenaireArmour SELJUK_HELMET;
    @ObjectHolder(value="millenaire:seljukplate")
    public static ItemMillenaireArmour SELJUK_CHESTPLATE;
    @ObjectHolder(value="millenaire:seljuklegs")
    public static ItemMillenaireArmour SELJUK_LEGGINGS;
    @ObjectHolder(value="millenaire:seljukboots")
    public static ItemMillenaireArmour SELJUK_BOOTS;
    @ObjectHolder(value="millenaire:wallcarpetsmall")
    public static ItemWallDecoration WALLCARPETSMALL;
    @ObjectHolder(value="millenaire:wallcarpetmedium")
    public static ItemWallDecoration WALLCARPETMEDIUM;
    @ObjectHolder(value="millenaire:wallcarpetlarge")
    public static ItemWallDecoration WALLCARPETLARGE;
    @ObjectHolder(value="millenaire:clothes_seljuk_wool")
    public static ItemClothes SELJUK_CLOTH_WOOL;
    @ObjectHolder(value="millenaire:clothes_seljuk_cotton")
    public static ItemClothes SELJUK_CLOTH_COTTON;
    public static Map<DyeColor, ItemPaintBucket> PAINT_BUCKETS;
    @ObjectHolder(value="millenaire:cherries")
    public static ItemFoodMultiple CHERRIES;
    @ObjectHolder(value="millenaire:cherry_blossom")
    public static ItemFoodMultiple CHERRY_BLOSSOM;

    @OnlyIn(value=Dist.CLIENT)
    public static void registerItemModels() {
        try {
            Field[] fields;
            for (Field f : fields = MillItems.class.getFields()) {
                Object item;
                if (ItemMill.class.isAssignableFrom(f.getType())) {
                    item = (ItemMill)((Object)f.get(null));
                    if (item instanceof ItemBannerPattern pattern) pattern.initModel();
                    continue;
                }
                if (ItemMillenaireArmour.class.isAssignableFrom(f.getType())) {
                    item = (ItemMillenaireArmour)((Object)f.get(null));

                    continue;
                }
                if (ItemMillenaireAxe.class.isAssignableFrom(f.getType())) {
                    item = (ItemMillenaireAxe)((Object)f.get(null));

                    continue;
                }
                if (ItemMillenaireHoe.class.isAssignableFrom(f.getType())) {
                    item = (ItemMillenaireHoe)((Object)f.get(null));

                    continue;
                }
                if (ItemMillenairePickaxe.class.isAssignableFrom(f.getType())) {
                    item = (ItemMillenairePickaxe)((Object)f.get(null));

                    continue;
                }
                if (ItemMillenaireShovel.class.isAssignableFrom(f.getType())) {
                    item = (ItemMillenaireShovel)((Object)f.get(null));

                    continue;
                }
                if (ItemMillenaireSword.class.isAssignableFrom(f.getType())) {
                    item = (ItemMillenaireSword)f.get(null);

                    continue;
                }
                if (ItemFoodMultiple.class.isAssignableFrom(f.getType())) {
                    item = (ItemFoodMultiple)((Object)f.get(null));
                    continue;
                }
                if (!ItemMillenaireBow.class.isAssignableFrom(f.getType())) continue;
                item = (ItemMillenaireBow)((Object)f.get(null));
                // Bow item models and pull predicates are registered during client setup.
            }
        }
        catch (IllegalArgumentException e) {
            MillLog.printException("Error, illegal argument, while initialising item models", e);
        }
        catch (IllegalAccessException e) {
            MillLog.printException("Error, illegal access, while initialising item models", e);
        }
    }

    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new ItemSummoningWand("summoningwand"));
        event.getRegistry().register(new ItemNegationWand("negationwand"));
        event.getRegistry().register(new ItemMill("denier"));
        event.getRegistry().register(new ItemMill("denierargent"));
        event.getRegistry().register(new ItemMill("denieror"));
        event.getRegistry().register(new ItemPurse("purse"));
        event.getRegistry().register(new ItemMillSeeds((Block)MillBlocks.CROP_RICE, "rice"));
        event.getRegistry().register(new ItemMillSeeds((Block)MillBlocks.CROP_TURMERIC, "turmeric"));
        event.getRegistry().register(new ItemMillSeeds((Block)MillBlocks.CROP_MAIZE, "maize"));
        event.getRegistry().register(new ItemMillSeeds((Block)MillBlocks.CROP_VINE, "grapes"));
        event.getRegistry().register(new ItemMillSeeds((Block)MillBlocks.CROP_COTTON, "cotton"));
        event.getRegistry().register(new ItemMillenairePickaxe("normanpickaxe", TOOLS_norman));
        event.getRegistry().register(new ItemMillenaireAxe("normanaxe", TOOLS_norman, 8.0f, -3.0f));
        event.getRegistry().register(new ItemMillenaireShovel("normanshovel", TOOLS_norman));
        event.getRegistry().register(new ItemMillenaireHoe("normanhoe", TOOLS_norman));
        event.getRegistry().register(new ItemMillenairePickaxe("mayanpickaxe", TOOLS_obsidian));
        event.getRegistry().register(new ItemMillenaireAxe("mayanaxe", TOOLS_obsidian, 8.0f, -3.0f));
        event.getRegistry().register(new ItemMillenaireShovel("mayanshovel", TOOLS_obsidian));
        event.getRegistry().register(new ItemMillenaireHoe("mayanhoe", TOOLS_obsidian));
        event.getRegistry().register(new ItemMillenairePickaxe("byzantinepickaxe", TOOLS_byzantine));
        event.getRegistry().register(new ItemMillenaireAxe("byzantineaxe", TOOLS_byzantine, 8.0f, -3.0f));
        event.getRegistry().register(new ItemMillenaireShovel("byzantineshovel", TOOLS_byzantine));
        event.getRegistry().register(new ItemMillenaireHoe("byzantinehoe", TOOLS_byzantine));
        event.getRegistry().register(new ItemMillenaireSword("normanbroadsword", TOOLS_norman, -1, false));
        event.getRegistry().register(new ItemMillenaireSword("mayanmace", net.minecraft.world.item.Tiers.IRON, -1, false));
        event.getRegistry().register(new ItemMillenaireSword("tachisword", TOOLS_obsidian, -1, false));
        event.getRegistry().register(new ItemMillenaireSword("byzantinemace", net.minecraft.world.item.Tiers.IRON, -1, true));
        event.getRegistry().register(new ItemMillenaireSword("inuittrident", net.minecraft.world.item.Tiers.IRON, 20, false));
        event.getRegistry().register(new ItemMillenaireSword("seljukscimitar", better_steel, -1, false));
        event.getRegistry().register(new ItemMillenaireBow("yumibow", 2.0f, 0.5f, 1));
        event.getRegistry().register(new ItemMillenaireBow("inuitbow", 1.0f, 0.0f, 20));
        event.getRegistry().register(new ItemMillenaireBow("seljukbow", 1.5f, 1.5f, 20));
        event.getRegistry().register(new ItemMillenaireArmour("normanhelmet", ARMOUR_norman, EquipmentSlot.HEAD));
        event.getRegistry().register(new ItemMillenaireArmour("normanplate", ARMOUR_norman, EquipmentSlot.CHEST));
        event.getRegistry().register(new ItemMillenaireArmour("normanlegs", ARMOUR_norman, EquipmentSlot.LEGS));
        event.getRegistry().register(new ItemMillenaireArmour("normanboots", ARMOUR_norman, EquipmentSlot.FEET));
        event.getRegistry().register(new ItemMillenaireArmour("japanesebluehelmet", ARMOUR_japanese_blue, EquipmentSlot.HEAD));
        event.getRegistry().register(new ItemMillenaireArmour("japaneseblueplate", ARMOUR_japanese_blue, EquipmentSlot.CHEST));
        event.getRegistry().register(new ItemMillenaireArmour("japanesebluelegs", ARMOUR_japanese_blue, EquipmentSlot.LEGS));
        event.getRegistry().register(new ItemMillenaireArmour("japaneseblueboots", ARMOUR_japanese_blue, EquipmentSlot.FEET));
        event.getRegistry().register(new ItemMillenaireArmour("japaneseredhelmet", ARMOUR_japanese_red, EquipmentSlot.HEAD));
        event.getRegistry().register(new ItemMillenaireArmour("japaneseredplate", ARMOUR_japanese_red, EquipmentSlot.CHEST));
        event.getRegistry().register(new ItemMillenaireArmour("japaneseredlegs", ARMOUR_japanese_red, EquipmentSlot.LEGS));
        event.getRegistry().register(new ItemMillenaireArmour("japaneseredboots", ARMOUR_japanese_red, EquipmentSlot.FEET));
        event.getRegistry().register(new ItemMillenaireArmour("japaneseguardhelmet", ARMOUR_japaneseGuard, EquipmentSlot.HEAD));
        event.getRegistry().register(new ItemMillenaireArmour("japaneseguardplate", ARMOUR_japaneseGuard, EquipmentSlot.CHEST));
        event.getRegistry().register(new ItemMillenaireArmour("japaneseguardlegs", ARMOUR_japaneseGuard, EquipmentSlot.LEGS));
        event.getRegistry().register(new ItemMillenaireArmour("japaneseguardboots", ARMOUR_japaneseGuard, EquipmentSlot.FEET));
        event.getRegistry().register(new ItemMillenaireArmour("byzantinehelmet", ARMOUR_byzantine, EquipmentSlot.HEAD));
        event.getRegistry().register(new ItemMillenaireArmour("byzantineplate", ARMOUR_byzantine, EquipmentSlot.CHEST));
        event.getRegistry().register(new ItemMillenaireArmour("byzantinelegs", ARMOUR_byzantine, EquipmentSlot.LEGS));
        event.getRegistry().register(new ItemMillenaireArmour("byzantineboots", ARMOUR_byzantine, EquipmentSlot.FEET));
        event.getRegistry().register(new ItemMillenaireArmour("furhelmet", ARMOUR_fur, EquipmentSlot.HEAD));
        event.getRegistry().register(new ItemMillenaireArmour("furplate", ARMOUR_fur, EquipmentSlot.CHEST));
        event.getRegistry().register(new ItemMillenaireArmour("furlegs", ARMOUR_fur, EquipmentSlot.LEGS));
        event.getRegistry().register(new ItemMillenaireArmour("furboots", ARMOUR_fur, EquipmentSlot.FEET));
        event.getRegistry().register(new ItemMillenaireArmour("seljukhelmet", ARMOUR_SELJUK, EquipmentSlot.HEAD));
        event.getRegistry().register(new ItemMillenaireArmour("seljukturban", ARMOUR_SELJUK_WOOL, EquipmentSlot.HEAD));
        event.getRegistry().register(new ItemMillenaireArmour("seljukplate", ARMOUR_SELJUK, EquipmentSlot.CHEST));
        event.getRegistry().register(new ItemMillenaireArmour("seljuklegs", ARMOUR_SELJUK, EquipmentSlot.LEGS));
        event.getRegistry().register(new ItemMillenaireArmour("seljukboots", ARMOUR_SELJUK, EquipmentSlot.FEET));
        event.getRegistry().register(new ItemFoodMultiple("ciderapple", 0, 0, 1, 0.05f, false, 0, 0, 64, false));
        event.getRegistry().register(new ItemFoodMultiple("olives", 0, 0, 1, 0.05f, false, 0, 0, 64, false));
        event.getRegistry().register(new ItemFoodMultiple("oliveoil", 0, 0, 0, 0.0f, true, 0, 0, 16, false).setClearEffects(true));
        event.getRegistry().register(new ItemFoodMultiple("cider", 4, 15, 0, 0.0f, true, 5, 384, 1, true));
        event.getRegistry().register(new ItemFoodMultiple("calva", 8, 30, 0, 0.0f, true, 10, 1024, 1, true));
        event.getRegistry().register(new ItemFoodMultiple("boudin", 0, 0, 8, 1.0f, false, 0, 384, 1, true));
        event.getRegistry().register(new ItemFoodMultiple("tripes", 0, 0, 10, 1.0f, false, 0, 512, 1, true));
        event.getRegistry().register(new ItemFoodMultiple("vegcurry", 0, 0, 6, 0.6f, false, 0, 384, 1, false));
        event.getRegistry().register(new ItemFoodMultiple("chickencurry", 0, 0, 8, 0.8f, false, 0, 512, 1, false));
        event.getRegistry().register(new ItemFoodMultiple("rasgulla", 2, 30, 0, 0.0f, false, 0, 0, 64, true).withEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 9600, 1), 1.0f));
        event.getRegistry().register(new ItemFoodMultiple("masa", 0, 0, 6, 0.6f, false, 0, 256, 1, false));
        event.getRegistry().register(new ItemFoodMultiple("wah", 0, 0, 10, 1.0f, false, 0, 384, 1, false));
        event.getRegistry().register(new ItemFoodMultiple("balche", 6, 20, 0, 0.0f, true, 7, 512, 1, true).withEffect(new MobEffectInstance(MobEffects.JUMP, 9600, 1), 1.0f));
        event.getRegistry().register(new ItemFoodMultiple("sikilpah", 0, 0, 7, 0.7f, false, 0, 448, 1, false));
        event.getRegistry().register(new ItemFoodMultiple("cacauhaa", 6, 30, 0, 0.0f, true, 0, 384, 1, true).withEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 9600, 1), 1.0f));
        event.getRegistry().register(new ItemFoodMultiple("udon", 0, 0, 8, 0.8f, false, 0, 384, 1, false));
        event.getRegistry().register(new ItemFoodMultiple("sake", 8, 30, 0, 0.0f, true, 10, 512, 1, true).withEffect(new MobEffectInstance(MobEffects.JUMP, 9600, 1), 1.0f));
        event.getRegistry().register(new ItemFoodMultiple("ikayaki", 0, 0, 10, 1.0f, false, 0, 512, 1, true).withEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 9600, 2), 1.0f));
        event.getRegistry().register(new ItemFoodMultiple("feta", 2, 15, 0, 0.0f, false, 0, 0, 64, false));
        event.getRegistry().register(new ItemFoodMultiple("souvlaki", 0, 0, 10, 1.0f, false, 0, 512, 1, true).withEffect(new MobEffectInstance(MobEffects.HEAL, 1, 2), 1.0f));
        event.getRegistry().register(new ItemFoodMultiple("winebasic", 3, 15, 0, 0.0f, true, 5, 384, 1, true));
        event.getRegistry().register(new ItemFoodMultiple("winefancy", 8, 30, 0, 0.0f, true, 5, 1024, 1, true).withEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 9600, 2), 1.0f));
        event.getRegistry().register(new ItemFoodMultiple("bearmeat_raw", 0, 0, 4, 0.5f, false, 0, 0, 64, true).withEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 4800, 1), 1.0f));
        event.getRegistry().register(new ItemFoodMultiple("bearmeat_cooked", 0, 0, 10, 1.0f, false, 0, 0, 64, true).withEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 9600, 2), 1.0f));
        event.getRegistry().register(new ItemFoodMultiple("wolfmeat_raw", 0, 0, 3, 0.3f, false, 0, 0, 64, false));
        event.getRegistry().register(new ItemFoodMultiple("wolfmeat_cooked", 0, 0, 5, 0.6f, false, 0, 0, 64, true).withEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 1), 1.0f));
        event.getRegistry().register(new ItemFoodMultiple("seafood_raw", 0, 0, 2, 0.2f, false, 0, 0, 1, true));
        event.getRegistry().register(new ItemFoodMultiple("seafood_cooked", 0, 0, 2, 0.25f, false, 0, 0, 1, true).withEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 1200, 1), 1.0f));
        event.getRegistry().register(new ItemFoodMultiple("inuitpotatostew", 0, 0, 6, 0.6f, false, 0, 384, 1, false));
        event.getRegistry().register(new ItemFoodMultiple("inuitmeatystew", 0, 0, 8, 0.8f, false, 0, 512, 1, false));
        event.getRegistry().register(new ItemFoodMultiple("inuitbearstew", 0, 0, 8, 1.0f, false, 0, 512, 1, true).withEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 9600, 3), 1.0f));
        event.getRegistry().register(new ItemFoodMultiple("ayran", 3, 15, 0, 0.2f, true, 2, 6, 1, true));
        event.getRegistry().register(new ItemFoodMultiple("yogurt", 0, 15, 0, 0.4f, false, 0, 0, 64, false));
        event.getRegistry().register(new ItemFoodMultiple("pide", 0, 0, 8, 1.0f, false, 0, 8, 1, false));
        event.getRegistry().register(new ItemFoodMultiple("lokum", 0, 0, 3, 0.0f, false, 0, 0, 1, true).withEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2400, 1), 0.2f));
        event.getRegistry().register(new ItemFoodMultiple("helva", 0, 0, 5, 0.0f, false, 0, 0, 1, true).withEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2400, 1), 0.2f));
        event.getRegistry().register(new ItemFoodMultiple("pistachios", 0, 0, 1, 0.1f, false, 0, 0, 64, false));
        event.getRegistry().register(new ItemFoodMultiple("cherries", 0, 0, 1, 0.1f, false, 0, 0, 64, false));
        event.getRegistry().register(new ItemFoodMultiple("cherry_blossom", 0, 0, 1, 0.1f, false, 0, 0, 64, false));
        event.getRegistry().register(new ItemWallDecoration("tapestry", 1));
        event.getRegistry().register(new ItemWallDecoration("indianstatue", 2));
        event.getRegistry().register(new ItemWallDecoration("mayanstatue", 3));
        event.getRegistry().register(new ItemWallDecoration("byzantineiconsmall", 4));
        event.getRegistry().register(new ItemWallDecoration("byzantineiconmedium", 5));
        event.getRegistry().register(new ItemWallDecoration("byzantineiconlarge", 6));
        event.getRegistry().register(new ItemWallDecoration("hidehanging", 7));
        event.getRegistry().register(new ItemWallDecoration("wallcarpetsmall", 8));
        event.getRegistry().register(new ItemWallDecoration("wallcarpetmedium", 9));
        event.getRegistry().register(new ItemWallDecoration("wallcarpetlarge", 10));
        for (DyeColor colour : DyeColor.values()) {
            ItemPaintBucket bucket = (ItemPaintBucket)new ItemPaintBucket("paint_bucket", colour);
            event.getRegistry().register(bucket);
            PAINT_BUCKETS.put(colour, bucket);
        }
        event.getRegistry().register(new ItemParchment("parchment_normanvillagers", 1, true));
        event.getRegistry().register(new ItemParchment("parchment_normanbuildings", 2, true));
        event.getRegistry().register(new ItemParchment("parchment_normanitems", 3, true));
        event.getRegistry().register(new ItemParchment("parchment_normanfull", new int[]{1, 2, 3}, true));
        event.getRegistry().register(new ItemParchment("parchment_indianvillagers", 5, true));
        event.getRegistry().register(new ItemParchment("parchment_indianbuildings", 6, true));
        event.getRegistry().register(new ItemParchment("parchment_indianitems", 7, true));
        event.getRegistry().register(new ItemParchment("parchment_indianfull", new int[]{5, 6, 7}, true));
        event.getRegistry().register(new ItemParchment("parchment_mayanvillagers", 9, true));
        event.getRegistry().register(new ItemParchment("parchment_mayanbuildings", 10, true));
        event.getRegistry().register(new ItemParchment("parchment_mayanitems", 11, true));
        event.getRegistry().register(new ItemParchment("parchment_mayanfull", new int[]{9, 10, 11}, true));
        event.getRegistry().register(new ItemParchment("parchment_japanesevillagers", 16, true));
        event.getRegistry().register(new ItemParchment("parchment_japanesebuildings", 17, true));
        event.getRegistry().register(new ItemParchment("parchment_japaneseitems", 18, true));
        event.getRegistry().register(new ItemParchment("parchment_japanesefull", new int[]{16, 17, 18}, true));
        event.getRegistry().register(new ItemParchment("parchment_villagescroll", 4, false));
        event.getRegistry().register(new ItemBrickMould("brickmould"));
        event.getRegistry().register(new ItemMill("obsidianflake"));
        event.getRegistry().register(new ItemMill("silk"));
        event.getRegistry().register(new ItemClothes("clothes_byz_wool", 1));
        event.getRegistry().register(new ItemClothes("clothes_byz_silk", 2));
        event.getRegistry().register(new ItemClothes("clothes_seljuk_wool", 1));
        event.getRegistry().register(new ItemClothes("clothes_seljuk_cotton", 2));
        event.getRegistry().register(new ItemAmuletVishnu("vishnu_amulet"));
        event.getRegistry().register(new ItemAmuletAlchemist("alchemist_amulet"));
        event.getRegistry().register(new ItemAmuletYggdrasil("yggdrasil_amulet"));
        event.getRegistry().register(new ItemAmuletSkollHati("skoll_hati_amulet"));
        event.getRegistry().register(new ItemUlu("ulu"));
        event.getRegistry().register(new ItemMill("tannedhide"));
        for (int pattern=0;pattern<MillBannerPatterns.NAMES.length;pattern++)
            event.getRegistry().register(new ItemBannerPattern(pattern==0?"bannerpattern":"bannerpattern_"+pattern,pattern));
        event.getRegistry().register(new ItemParchment("parchment_sadhu", 15, false));
        event.getRegistry().register(new ItemMill("unknownpowder"));
        event.getRegistry().register(new ItemMayanQuestCrown("mayanquestcrown", EquipmentSlot.HEAD));
    }

    static {
        PAINT_BUCKETS = new HashMap<DyeColor, ItemPaintBucket>();
    }

    public static final class MillItemNames {
        private static final String DENIER = "denier";
        private static final String DENIEROR = "denieror";
        private static final String DENIERARGENT = "denierargent";
        private static final String CALVA = "calva";
        private static final String TRIPES = "tripes";
        private static final String BOUDIN = "boudin";
        private static final String NORMANPICKAXE = "normanpickaxe";
        private static final String NORMANAXE = "normanaxe";
        private static final String NORMANSHOVEL = "normanshovel";
        private static final String NORMANHOE = "normanhoe";
        private static final String NORMANBROADSWORD = "normanbroadsword";
        private static final String NORMANHELMET = "normanhelmet";
        private static final String NORMANPLATE = "normanplate";
        private static final String NORMANLEGS = "normanlegs";
        private static final String NORMANBOOTS = "normanboots";
        private static final String RICE = "rice";
        private static final String TURMERIC = "turmeric";
        private static final String VEGCURRY = "vegcurry";
        private static final String CHICKENCURRY = "chickencurry";
        private static final String BRICKMOULD = "brickmould";
        private static final String RASGULLA = "rasgulla";
        private static final String INDIANSTATUE = "indianstatue";
        private static final String CIDERAPPLE = "ciderapple";
        private static final String OLIVES = "olives";
        public static final String CIDERAPPLE_PUBLIC = "ciderapple";
        public static final String OLIVES_PUBLIC = "olives";
        private static final String OLIVEOIL = "oliveoil";
        private static final String CIDER = "cider";
        private static final String SUMMONINGWAND = "summoningwand";
        private static final String NEGATIONWAND = "negationwand";
        private static final String NORMANVILLAGERS = "parchment_normanvillagers";
        private static final String NORMANITEMS = "parchment_normanitems";
        private static final String NORMANBUILDINGS = "parchment_normanbuildings";
        private static final String NORMANFULL = "parchment_normanfull";
        private static final String TAPESTRY = "tapestry";
        private static final String VISHNU_AMULET = "vishnu_amulet";
        private static final String ALCHEMIST_AMULET = "alchemist_amulet";
        private static final String YGGDRASIL_AMULET = "yggdrasil_amulet";
        private static final String SKOLL_HATI_AMULET = "skoll_hati_amulet";
        private static final String VILLAGESCROLL = "parchment_villagescroll";
        private static final String PAINT_BUCKET = "paint_bucket";
        private static final String INDIANVILLAGERS = "parchment_indianvillagers";
        private static final String INDIANITEMS = "parchment_indianitems";
        private static final String INDIANBUILDINGS = "parchment_indianbuildings";
        private static final String INDIANFULL = "parchment_indianfull";
        private static final String WAH = "wah";
        private static final String BLANCHE = "balche";
        private static final String SIKILPAH = "sikilpah";
        private static final String MASA = "masa";
        private static final String MAIZE = "maize";
        private static final String MAYANSTATUE = "mayanstatue";
        private static final String MAYANVILLAGERS = "parchment_mayanvillagers";
        private static final String MAYANITEMS = "parchment_mayanitems";
        private static final String MAYANBUILDINGS = "parchment_mayanbuildings";
        private static final String MAYANFULL = "parchment_mayanfull";
        private static final String MAYANMACE = "mayanmace";
        private static final String MAYANPICKAXE = "mayanpickaxe";
        private static final String MAYANAXE = "mayanaxe";
        private static final String MAYANSHOVEL = "mayanshovel";
        private static final String MAYANHOE = "mayanhoe";
        private static final String OBSIDIANFLAKE = "obsidianflake";
        private static final String CACAUHAA = "cacauhaa";
        private static final String UDON = "udon";
        private static final String TACHISWORD = "tachisword";
        private static final String YUMIBOW = "yumibow";
        private static final String SAKE = "sake";
        private static final String IKAYAKI = "ikayaki";
        private static final String JAPANESEBLUELEGS = "japanesebluelegs";
        private static final String JAPANESEBLUEHELMET = "japanesebluehelmet";
        private static final String JAPANESEBLUEPLATE = "japaneseblueplate";
        private static final String JAPANESEBLUEBOOTS = "japaneseblueboots";
        private static final String JAPANESEREDLEGS = "japaneseredlegs";
        private static final String JAPANESEREDHELMET = "japaneseredhelmet";
        private static final String JAPANESEREDPLATE = "japaneseredplate";
        private static final String JAPANESEREDBOOTS = "japaneseredboots";
        private static final String JAPANESEGUARDLEGS = "japaneseguardlegs";
        private static final String JAPANESEGUARDHELMET = "japaneseguardhelmet";
        private static final String JAPANESEGUARDPLATE = "japaneseguardplate";
        private static final String JAPANESEGUARDBOOTS = "japaneseguardboots";
        private static final String JAPANESEVILLAGERS = "parchment_japanesevillagers";
        private static final String JAPANESEITEMS = "parchment_japaneseitems";
        private static final String JAPANESEBUILDINGS = "parchment_japanesebuildings";
        private static final String JAPANESEFULL = "parchment_japanesefull";
        private static final String PARCHMENTSADHU = "parchment_sadhu";
        private static final String UNKNOWNPOWDER = "unknownpowder";
        private static final String MAYANQUESTCROWN = "mayanquestcrown";
        private static final String GRAPES = "grapes";
        private static final String WINEFANCY = "winefancy";
        private static final String SILK = "silk";
        private static final String BYZANTINEICONSMALL = "byzantineiconsmall";
        private static final String BYZANTINEICONMEDIUM = "byzantineiconmedium";
        private static final String BYZANTINEICONLARGE = "byzantineiconlarge";
        private static final String BYZANTINEBOOTS = "byzantineboots";
        private static final String BYZANTINELEGS = "byzantinelegs";
        private static final String BYZANTINEPLATE = "byzantineplate";
        private static final String BYZANTINEHELMET = "byzantinehelmet";
        private static final String BYZANTINEMACE = "byzantinemace";
        private static final String BYZANTINEPICKAXE = "byzantinepickaxe";
        private static final String BYZANTINEAXE = "byzantineaxe";
        private static final String BYZANTINESHOVEL = "byzantineshovel";
        private static final String BYZANTINEHOE = "byzantinehoe";
        private static final String CLOTHES_BYZ_WOOL = "clothes_byz_wool";
        private static final String CLOTHES_BYZ_SILK = "clothes_byz_silk";
        private static final String FETA = "feta";
        private static final String WINEBASIC = "winebasic";
        private static final String SOUVLAKI = "souvlaki";
        private static final String PURSE = "purse";
        private static final String BEARMEAT_RAW = "bearmeat_raw";
        private static final String BEARMEAT_COOKED = "bearmeat_cooked";
        private static final String WOLFMEAT_RAW = "wolfmeat_raw";
        private static final String WOLFMEAT_COOKED = "wolfmeat_cooked";
        private static final String SEAFOOD_RAW = "seafood_raw";
        private static final String SEAFOOD_COOKED = "seafood_cooked";
        private static final String INUITBEARSTEW = "inuitbearstew";
        private static final String INUITMEATYSTEW = "inuitmeatystew";
        private static final String INUITPOTATOSTEW = "inuitpotatostew";
        private static final String INUIT_TRIDENT = "inuittrident";
        private static final String FURHELMET = "furhelmet";
        private static final String FURPLATE = "furplate";
        private static final String FURLEGS = "furlegs";
        private static final String FURBOOTS = "furboots";
        private static final String INUIT_BOW = "inuitbow";
        private static final String ULU = "ulu";
        private static final String TANNEDHIDE = "tannedhide";
        private static final String HIDEHANGING = "hidehanging";
        private static final String VILLAGEBANNER = "villagebanner";
        private static final String CULTUREBANNER = "culturebanner";
        public static final String VILLAGEBANNER_PUBLIC = "villagebanner";
        public static final String CULTUREBANNER_PUBLIC = "culturebanner";
        public static final String BANNERPATTERN = "bannerpattern";
        private static final String AYRAN = "ayran";
        private static final String YOGURT = "yogurt";
        private static final String PIDE = "pide";
        private static final String LOKUM = "lokum";
        private static final String HELVA = "helva";
        private static final String COTTON = "cotton";
        private static final String PISTACHIOS = "pistachios";
        public static final String PISTACHIOS_PUBLIC = "pistachios";
        private static final String SELJUK_SCIMITAR = "seljukscimitar";
        private static final String SELJUK_BOW = "seljukbow";
        private static final String SELJUK_BOOTS = "seljukboots";
        private static final String SELJUK_LEGGINGS = "seljuklegs";
        private static final String SELJUK_CHESTPLATE = "seljukplate";
        private static final String SELJUK_HELMET = "seljukhelmet";
        private static final String SELJUK_TURBAN = "seljukturban";
        private static final String WALLCARPETSMALL = "wallcarpetsmall";
        private static final String WALLCARPETMEDIUM = "wallcarpetmedium";
        private static final String WALLCARPETLARGE = "wallcarpetlarge";
        private static final String CLOTHES_SELJUK_WOOL = "clothes_seljuk_wool";
        private static final String CLOTHES_SELJUK_COTTON = "clothes_seljuk_cotton";
        private static final String CHERRIES = "cherries";
        public static final String CHERRIES_PUBLIC = "cherries";
        private static final String CHERRY_BLOSSOM = "cherry_blossom";
        public static final String CHERRY_BLOSSOM_PUBLIC = "cherry_blossom";
    }
}

