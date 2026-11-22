import net.minecraft.world.entity.EquipmentSlot;
import org.millenaire.common.item.MillMaterials;

public class MaterialRegressionCheck {
    public static void main(String[] args) {
        var norman = MillMaterials.tool("normanTools", 2, 1561, 10F, 4F, 10);
        require(norman.getLevel() == 2 && norman.getUses() == 1561, "Norman harvest level/durability");
        require(norman.getSpeed() == 10F && norman.getAttackDamageBonus() == 4F, "Norman mining/combat");
        require(norman.getEnchantmentValue() == 10, "Norman enchantability");
        int[] protection = {3, 8, 6, 3};
        var armor = MillMaterials.armor("normanArmour", "millenaire:norman", 66, protection, 10, null, 0F);
        protection[0] = 100;
        require(armor.getDefenseForSlot(EquipmentSlot.FEET) == 3, "Armor settings must be copied");
        require(armor.getDefenseForSlot(EquipmentSlot.LEGS) == 8, "Leg protection order");
        require(armor.getDefenseForSlot(EquipmentSlot.CHEST) == 6, "Chest protection order");
        require(armor.getDefenseForSlot(EquipmentSlot.HEAD) == 3, "Head protection order");
        require(armor.getDurabilityForSlot(EquipmentSlot.FEET) == 858, "Boot durability");
        require(armor.getDurabilityForSlot(EquipmentSlot.LEGS) == 990, "Leg durability");
        require(armor.getDurabilityForSlot(EquipmentSlot.CHEST) == 1056, "Chest durability");
        require(armor.getDurabilityForSlot(EquipmentSlot.HEAD) == 726, "Helmet durability");
        require(armor.getName().equals("millenaire:norman"), "Armor texture identity");
        require(armor.getKnockbackResistance() == 0, "Do not introduce modern knockback resistance");
        System.out.println("Material regression checks passed");
    }
    private static void require(boolean value, String name) {
        if (!value) throw new AssertionError(name);
    }
}
