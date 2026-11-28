/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.passive.EntitySheep
 *  net.minecraft.init.Blocks
 *  net.minecraft.init.Items
 *  net.minecraft.item.Item
 *  net.minecraft.util.EnumHand
 */
package org.millenaire.common.goal;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionHand;
import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.pathing.atomicstryker.AStarConfig;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;

@DocumentedElement.Documentation(value="Sheer sheeps present around the villager's house.")
public class GoalShearSheep
extends Goal {
    // Five sheep normally produce 15 wool. Return these goods before
    // repeatedly picking a new shearing target from a large flock.
    public static final int WOOL_DELIVERY_BATCH = 15;

    /** Only naturally collected vanilla sheep wool, regardless of colour. */
    public static boolean isSheepWool(InvItem item) {
        if (item == null || item.getItem() == null) return false;
        ResourceLocation id = Registry.ITEM.getKey(item.getItem());
        return id != null && "minecraft".equals(id.getNamespace())
                && id.getPath().endsWith("_wool");
    }

    /** Restrict the emergency delivery priority to actual shepherd jobs. */
    public static boolean hasShearingGoal(MillVillager villager) {
        List<Goal> goals = villager.getGoals();
        if (goals == null) return false;
        for (Goal goal : goals) {
            if (goal instanceof GoalShearSheep) return true;
        }
        return false;
    }

    /** Count all colours; coloured wool must not get stuck in NPC inventory. */
    public static int woolInInventory(MillVillager villager) {
        int count = 0;
        for (InvItem key : villager.getInventoryKeys()) {
            if (isSheepWool(key)) count += villager.countInv(key);
        }
        return count;
    }

    /** Byzantine textile recipes require white wool and add dyes separately. */
    public static boolean usesWhiteWool(org.millenaire.common.culture.Culture culture){
        return culture!=null&&"byzantines".equals(culture.key);
    }

    public GoalShearSheep() {
        this.buildingLimit.put(InvItem.createInvItem(Blocks.WHITE_WOOL, 0), 1024);
        this.townhallLimit.put(InvItem.createInvItem(Blocks.WHITE_WOOL, 0), 1024);
        this.icon = InvItem.createInvItem((Item)Items.SHEARS);
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws Exception {
        if (villager.getHouse() == null
                || woolInInventory(villager) >= WOOL_DELIVERY_BATCH) return null;
        Point pos = villager.getPos();
        Entity closestSheep = null;
        double sheepBestDist = Double.MAX_VALUE;
        List<Entity> sheep = WorldUtilities.getEntitiesWithinAABB(villager.level, Sheep.class, villager.getHouse().getPos(), 30, 10);
        if (sheep == null) return null;
        for (Entity ent : sheep) {
            if (!ent.isAlive() || villager.isAnimalWorkTargetCoolingDown(ent) || ((Sheep)ent).isSheared() || ((Sheep)ent).isBaby() || closestSheep != null && !(pos.distanceTo(ent) < sheepBestDist)) continue;
            closestSheep = ent;
            sheepBestDist = pos.distanceTo(ent);
        }
        if (closestSheep != null) {
            return this.packDest(null, villager.getHouse(), closestSheep);
        }
        return null;
    }

    @Override
    public AStarConfig getPathingConfig(MillVillager villager) {
        if (!villager.canVillagerClearLeaves()) {
            return JPS_CONFIG_WIDE_NO_LEAVES;
        }
        return JPS_CONFIG_WIDE;
    }

    @Override
    public boolean isFightingGoal() {
        return true;
    }

    @Override
    public boolean isPossibleSpecific(MillVillager villager) throws Exception {
        if (villager.getHouse() == null || !villager.getHouse().containsTags("sheeps")
                || woolInInventory(villager) >= WOOL_DELIVERY_BATCH) {
            return false;
        }
        List<Entity> sheep = WorldUtilities.getEntitiesWithinAABB(villager.level, Sheep.class, villager.getHouse().getPos(), 30, 10);
        if (sheep == null) {
            return false;
        }
        for (Entity ent : sheep) {
            Sheep asheep = (Sheep)ent;
            if (asheep.isBaby() || !asheep.isAlive() || villager.isAnimalWorkTargetCoolingDown(ent) || ((Sheep)ent).isSheared()) continue;
            return true;
        }
        return false;
    }

    @Override
    public boolean lookAtGoal() {
        return true;
    }

    @Override
    public boolean isWithinActionRange(MillVillager villager, Point ignored) {
        Entity target = villager.getGoalDestEntity();
        return target != null && villager.distanceToSqr(target) < 4.0
                && WorkAccess.visible(villager, target.getBoundingBox().getCenter(), null);
    }

    @Override
    protected boolean isStillValidSpecific(MillVillager villager) throws Exception {
        Entity target = villager.getGoalDestEntity();
        return target instanceof Sheep sheep && sheep.isAlive() && !sheep.isBaby()
                && !sheep.isSheared() && villager.getHouse() != null
                && woolInInventory(villager) < WOOL_DELIVERY_BATCH;
    }

    @Override
    public boolean performAction(MillVillager villager) throws Exception {
        if (villager.level.isClientSide || villager.getHouse() == null
                || woolInInventory(villager) >= WOOL_DELIVERY_BATCH) return true;
        int carriedWool = woolInInventory(villager);
        List<Entity> sheep = WorldUtilities.getEntitiesWithinAABB(villager.level, Sheep.class, villager.getPos(), 4, 4);
        for (Entity ent : sheep) {
            if (carriedWool >= WOOL_DELIVERY_BATCH) break;
            Sheep animal;
            if (!ent.isAlive() || (animal = (Sheep)ent).isBaby() || animal.isSheared()) continue;
            if (villager.distanceToSqr(animal) >= 4.0
                    || !WorkAccess.visible(villager, animal.getBoundingBox().getCenter(), null)) continue;
            int amount = org.millenaire.common.config.TestTuning.resourceAmount(villager.level, 3);
            Item wool=usesWhiteWool(villager.getCulture())?Items.WHITE_WOOL:
                    Registry.ITEM.get(new ResourceLocation(animal.getColor().getName() + "_wool"));
            villager.addToInv(wool,amount);
            carriedWool += amount;
            ((Sheep)ent).setSheared(true);
            if (MillConfigValues.LogCattleFarmer >= 1 && villager.extraLog) {
                MillLog.major(this, "Shearing: " + ent);
            }
            villager.swing(InteractionHand.MAIN_HAND);
        }
        return true;
    }

    @Override
    public int priority(MillVillager villager) throws Exception {
        return 50;
    }

    @Override
    public int range(MillVillager villager) {
        return 2;
    }
}

