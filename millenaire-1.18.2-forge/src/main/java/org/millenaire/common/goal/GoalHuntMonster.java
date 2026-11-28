/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.monster.EntityCreeper
 *  net.minecraft.entity.monster.EntityMob
 *  net.minecraft.init.Items
 *  net.minecraft.item.ItemStack
 */
package org.millenaire.common.goal;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.WorldUtilities;

@DocumentedElement.Documentation(value="Seek out mobs around the village and attack them.")
public class GoalHuntMonster
extends Goal {
    public GoalHuntMonster() {
        this.icon = InvItem.createInvItem(Items.DIAMOND_SWORD);
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws Exception {
        if (villager.getTownHall() == null) return null;
        List<Entity> mobs = WorldUtilities.getEntitiesWithinAABB(villager.level, Monster.class, villager.getTownHall().getPos(), 50, 10);
        if (mobs == null) {
            return null;
        }
        int bestDist = Integer.MAX_VALUE;
        Entity target = null;
        for (Entity ent : mobs) {
            if (!(ent instanceof Monster) || ent instanceof Creeper || !(villager.getPos().distanceToSquared(ent) < (double)bestDist) || !((double)villager.getTownHall().getAltitude((int)ent.getX(), (int)ent.getZ()) < ent.getY())) continue;
            target = ent;
            bestDist = (int)villager.getPos().distanceToSquared(ent);
        }
        if (target == null) {
            return null;
        }
        return this.packDest(null, null, target);
    }

    @Override
    public ItemStack[] getHeldItemsTravelling(MillVillager villager) {
        return new ItemStack[]{villager.getWeapon()};
    }

    @Override
    public boolean isFightingGoal() {
        return true;
    }

    @Override
    public boolean isPossibleSpecific(MillVillager villager) throws Exception {
        return this.getDestination(villager) != null;
    }

    @Override
    public boolean isStillValidSpecific(MillVillager villager) throws Exception {
        Entity target = villager.getGoalDestEntity();
        return target instanceof Monster && !(target instanceof Creeper) && target.isAlive()
                && villager.getTownHall() != null
                && villager.getTownHall().getPos().horizontalDistanceTo(target) <= 50.0;
    }

    @Override
    public boolean performAction(MillVillager villager) throws Exception {
        List<Entity> mobs = WorldUtilities.getEntitiesWithinAABB(villager.level, Monster.class, villager.getPos(), 4, 4);
        for (Entity ent : mobs) {
            if (ent.isRemoved() || !(ent instanceof Monster) || !villager.hasLineOfSight(ent)) continue;
            Monster mob = (Monster)ent;
            villager.setTarget((LivingEntity)mob);
            if (MillConfigValues.LogGeneralAI < 1) continue;
            MillLog.major(this, "Attacking entity: " + ent);
        }
        return true;
    }

    @Override
    public int priority(MillVillager villager) throws Exception {
        return 50;
    }
}

