package net.minecraftforge.registries;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.millenaire.common.item.LegacyItemStacks;

public class LegacyItemStacksCheck {
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        check(LegacyItemStacks.stack(Blocks.OAK_PLANKS, 3, 2).is(Items.BIRCH_PLANKS), "birch planks");
        check(LegacyItemStacks.stack(Blocks.OAK_LOG, 2, 3).is(Items.JUNGLE_LOG), "jungle logs");
        check(LegacyItemStacks.stack(Blocks.WHITE_WOOL, 4, 14).is(Items.RED_WOOL), "red wool");
        check(LegacyItemStacks.stack(Items.INK_SAC, 1, 1).is(Items.RED_DYE), "renamed dye");
        check(LegacyItemStacks.fromLegacyId("minecraft:wool", 2, 14).is(Items.RED_WOOL), "old item list names");
        check(LegacyItemStacks.matches(new ItemStack(Items.RED_WOOL), Items.WHITE_WOOL, 14), "inventory color match");
        check(!LegacyItemStacks.matches(new ItemStack(Items.BLUE_WOOL), Items.WHITE_WOOL, 14), "wrong inventory color rejected");
        check(LegacyItemStacks.matches(new ItemStack(Items.ACACIA_LOG), Items.OAK_LOG, -1), "all-log building wildcard");
        check(!LegacyItemStacks.matches(new ItemStack(Items.ACACIA_PLANKS), Items.OAK_LOG, -1), "logs distinct from planks");
        var sword = LegacyItemStacks.stack(Items.IRON_SWORD, 1, 17);
        check(sword.is(Items.IRON_SWORD) && sword.getDamageValue() == 17, "damage is not a variant");
        var itemRegistry = (net.minecraftforge.registries.ForgeRegistry<Item>)net.minecraftforge.registries.ForgeRegistries.ITEMS;
        itemRegistry.unfreeze();
        ((net.minecraftforge.registries.NamespacedDefaultedWrapper<Item>)Registry.ITEM).unfreeze();
        var custom = new Item(new Item.Properties());
        custom.setRegistryName("millenaire", "variant_test");
        itemRegistry.register(custom);
        // Plain item fixtures exercise legacy identity conversion without Forge enum transformation.
        Item[] patterns = new Item[28];
        for (int index=0;index<patterns.length;index++) {
            patterns[index] = new Item(new Item.Properties());
            patterns[index].setRegistryName("millenaire",index==0?"bannerpattern":"bannerpattern_"+index);
            itemRegistry.register(patterns[index]);
        }
        itemRegistry.freeze();
        for (int index=0;index<patterns.length;index++) {
            ItemStack pattern = LegacyItemStacks.stack(patterns[0],3,index);
            check(pattern.is(patterns[index]) && pattern.getCount()==3,"old pattern metadata resolves to distinct loom item");
            check(LegacyItemStacks.matches(pattern,patterns[0],index),"exact pattern matches after flattening");
            check(LegacyItemStacks.matches(pattern,patterns[0],-1),"all-pattern wildcard retained");
            check(!LegacyItemStacks.matches(pattern,patterns[0],(index+1)%28),"different culture pattern rejected");
            check(LegacyItemStacks.fromLegacyId("millenaire:bannerpattern",2,index).is(patterns[index]),"old item-list pattern resolves");
        }
        var original = LegacyItemStacks.stack(custom, 6, 4);
        var restored = ItemStack.of(original.save(new CompoundTag()));
        check(restored.is(custom) && restored.getCount() == 6 && LegacyItemStacks.metadata(restored) == 4, "custom variant NBT roundtrip");
        check(LegacyItemStacks.matches(restored, custom, 4) && !LegacyItemStacks.matches(restored, custom, 3), "custom inventory variants distinct");
        check(LegacyItemStacks.stack(Blocks.OAK_PLANKS, 7, -1).getCount() == 7, "wildcard count");
        System.out.println("Legacy item stack checks passed");
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
