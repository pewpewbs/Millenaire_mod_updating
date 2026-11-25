/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityList
 *  net.minecraft.entity.passive.EntityAnimal
 *  net.minecraft.entity.passive.EntityChicken
 *  net.minecraft.entity.passive.EntityCow
 *  net.minecraft.entity.passive.EntityPig
 *  net.minecraft.entity.passive.EntitySheep
 *  net.minecraft.init.Items
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.ResourceLocation
 */
package org.millenaire.common.goal;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.item.MillItems;
import org.millenaire.common.network.ServerSender;
import org.millenaire.common.pathing.atomicstryker.AStarConfig;
import org.millenaire.common.utilities.WorldUtilities;

@DocumentedElement.Documentation(value="Breed animals at the villager's house, of types decided by tags like 'cattle', 'pig' etc tags.")
public class GoalBreedAnimals
extends Goal {
    private static final Item[] CEREALS = new Item[]{Items.WHEAT, MillItems.RICE, MillItems.MAIZE};
    private static final Item[] SEEDS = new Item[]{Items.WHEAT_SEEDS, MillItems.RICE, MillItems.MAIZE};
    private static final Item[] CARROTS = new Item[]{Items.CARROT};

    public GoalBreedAnimals() {
        this.icon = InvItem.createInvItem(Items.WHEAT);
    }

    private Item[] getBreedingItems(Class animalClass) {
        if (Cow.class.isAssignableFrom(animalClass) || Sheep.class.isAssignableFrom(animalClass)) {
            return CEREALS;
        }
        if (Pig.class.isAssignableFrom(animalClass)) {
            return CARROTS;
        }
        if (Chicken.class.isAssignableFrom(animalClass)) {
            return SEEDS;
        }
        return null;
    }

    private int getTargetAnimals(MillVillager villager, Class animalClass) {
        int target = 0;
        for (int i = 0; i < villager.getHouse().getResManager().spawns.size(); i++) {
            EntityType<?> type = Registry.ENTITY_TYPE.getOptional(villager.getHouse().getResManager().spawnTypes.get(i)).orElse(null);
            boolean matches = animalClass == Cow.class && (type == EntityType.COW || type == EntityType.MOOSHROOM)
                    || animalClass == Sheep.class && type == EntityType.SHEEP
                    || animalClass == Pig.class && type == EntityType.PIG
                    || animalClass == Chicken.class && type == EntityType.CHICKEN;
            if (matches) target = villager.getHouse().getResManager().spawns.get(i).size();
        }
        return target;
    }

    private boolean hasBreedingPartner(MillVillager villager,Animal animal,List<Entity> herd) {
        for(Entity e:herd){
            if(e==animal||!e.isAlive()||e.getType()!=animal.getType()||!(e instanceof Animal other))continue;
            if(other.getAge()!=0||other.distanceToSqr(animal)>64.0D)continue;
            if(org.millenaire.common.village.AnimalPenAccess.connected(villager.level,animal,animal.position(),other.position()))return true;
        }
        return false;
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws Exception {
        if (villager.getHouse() == null) return null;
        List<Class> validAnimals = this.getValidAnimalClasses(villager);
        for (Class animalClass : validAnimals) {
            Animal animal;
            Item[] breedingItems = this.getBreedingItems(animalClass);
            boolean available = false;
            if (breedingItems == null) {
                available = true;
            } else {
                for (Item breedingItem : breedingItems) {
                    if (available || villager.getHouse().countGoods(breedingItem) <= 0) continue;
                    available = true;
                }
            }
            if (!available) continue;
            int targetAnimals = this.getTargetAnimals(villager, animalClass);
            List<Entity> animals = WorldUtilities.getEntitiesWithinAABB(villager.level, animalClass, villager.getHouse().getPos(), 15, 10);
            int nbAdultAnimal = 0;
            int nbAnimal = 0;
            for (Entity ent : animals) {
                if (!ent.isAlive()) continue;
                animal = (Animal)ent;
                if (animal.getAge() == 0) {
                    ++nbAdultAnimal;
                }
                ++nbAnimal;
            }
            if (nbAdultAnimal < 2 || nbAnimal >= targetAnimals * 2) continue;
            for (Entity ent : animals) {
                if (!ent.isAlive()) continue;
                animal = (Animal)ent;
                if (animal.getAge() != 0 || animal.isInLove() || villager.isAnimalWorkTargetCoolingDown(ent)
                        || !this.hasBreedingPartner(villager,animal,animals)) continue;
                return this.packDest(null, villager.getHouse(), (Entity)animal);
            }
        }
        return null;
    }

    @Override
    public ItemStack[] getHeldItemsTravelling(MillVillager villager) throws Exception {
        if (villager.getHouse() == null || villager.getGoalDestEntity() == null || !(villager.getGoalDestEntity() instanceof Animal)) {
            return null;
        }
        Animal animal = (Animal)villager.getGoalDestEntity();
        Item[] breedingItems = this.getBreedingItems(animal.getClass());
        if (breedingItems != null) {
            for (Item breedingItem : breedingItems) {
                if (villager.getHouse().countGoods(breedingItem) <= 0) continue;
                return new ItemStack[]{new ItemStack(breedingItem, 1)};
            }
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

    private List<Class> getValidAnimalClasses(MillVillager villager) {
        ArrayList<Class> validAnimals = new ArrayList<Class>();
        if (villager.getHouse().containsTags("sheeps")) {
            validAnimals.add(Sheep.class);
            validAnimals.add(Chicken.class);
        }
        if (villager.getHouse().containsTags("cattle")) {
            validAnimals.add(Cow.class);
        }
        if (villager.getHouse().containsTags("pigs")) {
            validAnimals.add(Pig.class);
        }
        if (villager.getHouse().containsTags("chicken")) {
            validAnimals.add(Chicken.class);
        }
        return validAnimals;
    }

    @Override
    public boolean isFightingGoal() {
        return false;
    }

    @Override
    public boolean isPossibleSpecific(MillVillager villager) throws Exception {
        return this.getDestination(villager) != null;
    }

    @Override
    public boolean lookAtGoal() {
        return true;
    }

    @Override
    public boolean performAction(MillVillager villager) throws Exception {
        if (villager.getHouse() == null || villager.level.isClientSide) return true;
        List<Class> validAnimals = this.getValidAnimalClasses(villager);
        for (Class animalClass : validAnimals) {
            List<Entity> herd = WorldUtilities.getEntitiesWithinAABB(villager.level, animalClass, villager.getHouse().getPos(), 15, 10);
            long count = herd.stream().filter(Entity::isAlive).count();
            long ready = herd.stream().filter(Entity::isAlive).filter(e -> ((Animal)e).getAge() == 0).count();
            if (ready < 2 || count >= (long)this.getTargetAnimals(villager, animalClass) * 2) continue;
            List<Entity> animals = WorldUtilities.getEntitiesWithinAABB(villager.level, animalClass, villager.getPos(), 4, 2);
            for (Entity ent : animals) {
                if (!ent.isAlive() || !herd.contains(ent)) continue;
                Animal animal = (Animal)ent;
                Item[] breedingItems = this.getBreedingItems(animal.getClass());
                boolean available = false;
                Item foundBreedingItem = null;
                if (breedingItems == null) {
                    available = true;
                } else {
                    for (Item breedingItem : breedingItems) {
                        if (available || villager.getHouse().countGoods(breedingItem) <= 0) continue;
                        available = true;
                        foundBreedingItem = breedingItem;
                    }
                }
                if (!available || animal.isBaby() || animal.isInLove() || animal.getAge() != 0
                        || !villager.hasLineOfSight(animal) || !this.hasBreedingPartner(villager,animal,herd)) continue;
                if (foundBreedingItem != null && villager.getHouse().takeGoods(foundBreedingItem, 1) != 1) continue;
                animal.setInLove(null);
                animal.setTarget(null);
                villager.swing(InteractionHand.MAIN_HAND);
                ServerSender.sendAnimalBreeding(animal);
            }
        }
        return true;
    }

    @Override
    public int priority(MillVillager villager) throws Exception {
        return 10000;
    }

    @Override
    public boolean isWithinActionRange(MillVillager villager, org.millenaire.common.utilities.Point destination) {
        Entity target = villager.getGoalDestEntity();
        if (target == null || !target.isAlive()) return false;
        double reach = this.range(villager);
        return villager.distanceToSqr(target) < reach * reach && villager.hasLineOfSight(target);
    }

    @Override
    public int range(MillVillager villager) {
        return 3;
    }
}


