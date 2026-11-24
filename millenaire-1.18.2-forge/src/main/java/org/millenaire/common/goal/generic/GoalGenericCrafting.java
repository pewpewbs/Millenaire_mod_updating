/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemStack
 */
package org.millenaire.common.goal.generic;

import java.util.HashMap;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.millenaire.common.annotedparameters.AnnotedParameter;
import org.millenaire.common.annotedparameters.ConfigAnnotations;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.goal.generic.GoalGeneric;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.WorldUtilities;
import org.millenaire.common.village.Building;

public class GoalGenericCrafting
extends GoalGeneric {
    public static final String GOAL_TYPE = "crafting";
    @ConfigAnnotations.ConfigField(type=AnnotedParameter.ParameterType.INVITEM_NUMBER_ADD)
    @ConfigAnnotations.FieldDocumentation(explanation="Each action will require and use all the inputs.")
    public HashMap<InvItem, Integer> input = new HashMap();
    @ConfigAnnotations.ConfigField(type=AnnotedParameter.ParameterType.INVITEM_NUMBER_ADD)
    @ConfigAnnotations.FieldDocumentation(explanation="Each action will produce all the outputs.")
    public HashMap<InvItem, Integer> output = new HashMap();

    @Override
    public void applyDefaultSettings() {
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws Exception {
        List<Building> buildings = this.getBuildings(villager);
        for (Building dest : buildings) {
            if (!this.isDestPossible(villager, dest)) continue;
            return this.packDest(dest.getResManager().getCraftingPos(), dest);
        }
        return null;
    }

    @Override
    public ItemStack getIcon() {
        if (this.icon != null) {
            return this.icon.getItemStack();
        }
        if (!this.output.isEmpty()) {
            return this.output.keySet().iterator().next().getItemStack();
        }
        return null;
    }

    @Override
    public String getTypeLabel() {
        return GOAL_TYPE;
    }

    @Override
    public boolean isDestPossibleSpecific(MillVillager villager, Building b) {
        for (InvItem item : this.input.keySet()) {
            if ((long)villager.countInv(item) + b.countGoods(item) >= this.input.get(item)) continue;
            return false;
        }
        return true;
    }

    @Override
    public boolean isPossibleGenericGoal(MillVillager villager) throws Exception {
        return true;
    }

    @Override public int range(MillVillager villager) { return 1; }
    @Override public boolean isWithinActionRange(MillVillager villager, org.millenaire.common.utilities.Point ignored) {
        var destination = villager.getGoalDestPoint();
        if (destination == null) return false;
        var standing = net.minecraft.world.phys.Vec3.atBottomCenterOf(destination.getBlockPos());
        return villager.position().distanceToSqr(standing) < 1.5625
                && org.millenaire.common.goal.WorkAccess.visible(villager,
                    standing.add(0, 0.8, 0), destination.getBlockPos());
    }

    @Override
    public boolean performAction(MillVillager villager) throws Exception {
        Building dest = villager.getGoalBuildingDest();
        if (dest == null || villager.level.isClientSide || !this.validQuantities()) return true;
        if (!this.getBuildings(villager).contains(dest) || !this.passesStockLimits(villager)
                || !this.passesResourceBalance(villager) || !this.isDestPossible(villager,dest)) return true;
        if (villager.getGoalDestPoint() == null || !villager.getGoalDestPoint().equals(dest.getResManager().getCraftingPos())) return true;
        if (!this.isWithinActionRange(villager,null)) return false;
        try (org.millenaire.common.utilities.GoodsTransaction transaction = dest.beginGoodsTransaction(villager)) {
            for (var entry : this.input.entrySet()) {
                int required = entry.getValue();
                int taken = villager.takeFromInv(entry.getKey(), required);
                if (taken < 0 || taken > required) throw new IllegalStateException("Invalid resident ingredient withdrawal");
                int remaining = required - taken;
                if (remaining > 0 && dest.takeGoods(entry.getKey(), remaining) != remaining) return true;
            }
            for (var entry : this.output.entrySet()) {
                if (dest.storeGoods(entry.getKey(), entry.getValue()) != entry.getValue()) return true;
            }
            transaction.commit();
        }
        if (this.sound != null) {
            WorldUtilities.playSoundByMillName(villager.level, villager.getPos(), this.sound, 1.0f);
        }
        return true;
    }

    @Override
    public boolean swingArms() {
        return true;
    }

    private boolean validQuantities() {
        if (this.output.isEmpty()) return false;
        for (Integer amount : this.input.values()) if (amount == null || amount <= 0) return false;
        for (Integer amount : this.output.values()) if (amount == null || amount <= 0) return false;
        return true;
    }

    @Override
    public boolean validateGoal() {
        if (!this.validQuantities()) {
            MillLog.error(this, "Generic crafting goals require output and strictly positive quantities.");
            return false;
        }
        return true;
    }
}

