package org.millenaire.common.item;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.village.Building;

/** Creative-only tool for exercising the real child-to-adult transition. */
public final class ItemGrowthWand extends Item {
    public ItemGrowthWand() {
        super(new Item.Properties().stacksTo(1).tab(MillBlocks.tabMillenaireContentCreator));
        setRegistryName("millenaire", "growth_wand");
    }

    public InteractionResult useOnVillager(Player player, MillVillager child) {
        if (!player.isCreative()) return InteractionResult.FAIL;
        if (child.level.isClientSide) return InteractionResult.SUCCESS;
        if (!child.isAlive() || !child.isBaby()) return InteractionResult.CONSUME;
        Building townHall = child.getTownHall();
        if (child.getRecord() == null || child.getHouse() == null || townHall == null) {
            message(player, "unavailable");
            return InteractionResult.CONSUME;
        }
        // Skip the waiting/food consumption, not the culture's job and housing rules.
        child.getRecord().size = MillVillager.MAX_CHILD_SIZE;
        child.computeChildScale();
        try {
            Goal goal = Goal.goals == null ? null : Goal.goals.get("becomeadult");
            Goal.GoalInformation destination = goal == null ? null : goal.getDestination(child);
            if (destination != null && destination.getDestBuildingPos() != null) {
                for (Building house : townHall.getBuildings()) {
                    if (house == null || !destination.getDestBuildingPos().equals(house.getPos())
                            || !house.canChildMoveIn(child.gender, child.familyName)) continue;
                    house.addAdult(child);
                    message(player, child.isRemoved() ? "success" : "unavailable");
                    return InteractionResult.CONSUME;
                }
            }
            message(player, "no_home");
        } catch (Exception failure) {
            MillLog.printException("Growth wand: could not mature villager", failure);
            message(player, "unavailable");
        }
        return InteractionResult.CONSUME;
    }

    private static void message(Player player, String result) {
        player.displayClientMessage(new TranslatableComponent("item.millenaire.growth_wand." + result), true);
    }
}
