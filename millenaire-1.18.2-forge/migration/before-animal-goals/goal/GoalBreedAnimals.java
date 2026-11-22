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
import net.minecraft.entity.EntityList;
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
        if (animalClass == Cow.class || animalClass == Sheep.class) {
            return CEREALS;
        }
        if (animalClass == Pig.class) {
            return CARROTS;
        }
        if (animalClass == Chicken.class) {
            return SEEDS;
        }
        return null;
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws Exception {
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
            int targetAnimals = 0;
            for (int i = 0; i < villager.getHouse().getResManager().spawns.size(); ++i) {
                if (!animalClass.isAssignableFrom(EntityList.getClass((ResourceLocation)villager.getHouse().getResManager().spawnTypes.get(i)))) continue;
                targetAnimals = villager.getHouse().getResManager().spawns.get(i).size();
            }
            List<Entity> animals = WorldUtilities.getEntitiesWithinAABB(villager.level, animalClass, villager.getHouse().getPos(), 15, 10);
            int nbAdultAnimal = 0;
            int nbAnimal = 0;
            for (Entity ent : animals) {
                animal = (Animal)ent;
                if (animal.func_70874_b() == 0) {
                    ++nbAdultAnimal;
                }
                ++nbAnimal;
            }
            if (nbAdultAnimal < 2 || nbAnimal >= targetAnimals * 2) continue;
            for (Entity ent : animals) {
                animal = (Animal)ent;
                if (animal.func_70874_b() != 0 || animal.isInLove()) continue;
                return this.packDest(null, villager.getHouse(), (Entity)animal);
            }
        }
        return null;
    }

    @Override
    public ItemStack[] getHeldItemsTravelling(MillVillager villager) throws Exception {
        if (villager.getGoalDestEntity() == null || !(villager.getGoalDestEntity() instanceof Animal)) {
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
        List<Class> validAnimals = this.getValidAnimalClasses(villager);
        for (Class animalClass : validAnimals) {
            List<Entity> animals = WorldUtilities.getEntitiesWithinAABB(villager.level, animalClass, villager.getPos(), 4, 2);
            for (Entity ent : animals) {
                if (ent.isRemoved()) continue;
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
                if (!available || animal.isBaby() || animal.isInLove() || animal.func_70874_b() != 0) continue;
                animal.setInLove(null);
                animal.setTarget(null);
                if (foundBreedingItem != null) {
                    villager.getHouse().takeGoods(foundBreedingItem, 1);
                }
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
    public int range(MillVillager villager) {
        return 5;
    }
}

