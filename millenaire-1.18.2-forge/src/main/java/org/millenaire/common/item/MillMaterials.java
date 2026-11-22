package org.millenaire.common.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/** Explicit materials replace the removed runtime enum extension mechanism. */
public final class MillMaterials {
    private MillMaterials() {}

    public static Tier tool(String name, int level, int uses, float speed, float damage, int enchantment) {
        // A tier without a custom sorting tag uses Forge's legacy numeric harvest-level
        // path, retaining these 1.12 materials' level rather than inventing a new tier.
        return new Tier() {
            public int getUses() { return uses; }
            public float getSpeed() { return speed; }
            public float getAttackDamageBonus() { return damage; }
            public int getLevel() { return level; }
            public int getEnchantmentValue() { return enchantment; }
            public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
            public String toString() { return name; }
        };
    }

    public static ArmorMaterial armor(String id, String texture, int multiplier, int[] defense,
                                       int enchantment, SoundEvent sound, float toughness) {
        int[] protection = defense.clone();
        int[] durability = {13, 15, 16, 11};
        if (protection.length != 4) throw new IllegalArgumentException("Four armor slots required: " + id);
        return new ArmorMaterial() {
            public int getDurabilityForSlot(EquipmentSlot slot) { return durability[slot.getIndex()] * multiplier; }
            public int getDefenseForSlot(EquipmentSlot slot) { return protection[slot.getIndex()]; }
            public int getEnchantmentValue() { return enchantment; }
            public SoundEvent getEquipSound() { return sound; }
            public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
            public String getName() { return texture; }
            public float getToughness() { return toughness; }
            public float getKnockbackResistance() { return 0.0F; }
        };
    }
}
