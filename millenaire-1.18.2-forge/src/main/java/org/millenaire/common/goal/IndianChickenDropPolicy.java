package org.millenaire.common.goal;

import net.minecraft.world.item.Items;
import org.millenaire.common.culture.Culture;
import org.millenaire.common.goal.generic.GoalGenericCooking;
import org.millenaire.common.goal.generic.GoalGenericCrafting;
import org.millenaire.common.goal.generic.GoalGenericPlantCrop;
import org.millenaire.common.goal.generic.GoalGenericTakeFromBuilding;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.quest.Quest;

/** Keep feathers whenever the loaded culture or quest configuration needs them.
 * Collection/return instructions alone are not a productive use. */
public final class IndianChickenDropPolicy {
    private IndianChickenDropPolicy() {}

    private static boolean feather(InvItem item) {
        return item != null && item.getItem() == Items.FEATHER;
    }

    private static boolean containsFeather(Iterable<InvItem> goods) {
        if (goods == null) return false;
        for (InvItem item : goods) if (feather(item)) return true;
        return false;
    }

    public static boolean canDiscardFeathers(Culture culture) {
        // Fail closed until the standard culture and quest data are available.
        if (culture == null || !"indian".equals(culture.key)
                || culture.villagerTypes.isEmpty() || culture.ListPlanSets.isEmpty()
                || Quest.quests == null || Quest.quests.isEmpty()) return false;
        if (culture.getTradeGood("feather") != null
                || culture.getTradeGood(InvItem.createInvItem(Items.FEATHER)) != null) return false;
        for (var needs : culture.shopNeeds.values())
            if (containsFeather(needs)) return false;
        for (var type : culture.villagerTypes.values()) {
            if (containsFeather(type.requiredGoods.keySet())
                    || containsFeather(type.requiredFoodAndGoods.keySet())
                    || containsFeather(type.itemsNeeded)
                    || containsFeather(type.foreignMerchantStock.keySet())) return false;
            if (type.villagerConfig != null && (containsFeather(type.villagerConfig.foodsGrowth.keySet())
                    || containsFeather(type.villagerConfig.foodsConception.keySet()))) return false;
            for (Goal goal : type.goals) {
                if (goal instanceof GoalGenericCrafting crafting && containsFeather(crafting.input.keySet())) return false;
                if (goal instanceof GoalGenericCooking cooking && feather(cooking.itemToCook)) return false;
                if (goal instanceof GoalGenericPlantCrop planting && feather(planting.seed)) return false;
                if (goal instanceof GoalGenericTakeFromBuilding transfer && containsFeather(transfer.collectGoods.keySet())) return false;
            }
        }
        for (var set : culture.ListPlanSets)
            for (var variation : set.plans)
                for (var plan : variation)
                    if (plan != null && plan.resCost != null && containsFeather(plan.resCost.keySet())) return false;
        // Conservative: keep feathers if any loaded quest explicitly needs them.
        for (var quest : Quest.quests.values())
            for (var step : quest.steps)
                if (containsFeather(step.requiredGood.keySet())) return false;
        return true;
    }
}
