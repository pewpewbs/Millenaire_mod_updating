package org.millenaire.common.config;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.*;
import org.millenaire.common.goal.generic.*;
import org.millenaire.common.forge.Mill;

/** Ephemeral, server-owned test multipliers. Never stored in world or client config. */
public final class TestTuning {
    private TestTuning() {}
    public static final int QUERY = -1, RESET = -2;
    public static final int MOVE = 0, WORK = 1, YIELD = 2, BUILD = 3;
    private static final int[][] VALUES = {
        {100,125,150,175,200,225,250,275,300},
        {100,200,300,400,500,600,700,800,900,1000,1500,2000},
        {100,200,300,400,500,600,700,800,900,1000,1500,2000},
        {100,200,300,400,500,600,700,800,900,1000,1500,2000}
    };
    public record Settings(int move, int work, int yield, int build) {
        public int value(int field) {
            return switch (field) {
                case MOVE -> move; case WORK -> work; case YIELD -> yield; case BUILD -> build;
                default -> throw new IllegalArgumentException("Unknown test setting");
            };
        }
        Settings with(int field, int value) {
            return switch (field) {
                case MOVE -> new Settings(value, work, yield, build);
                case WORK -> new Settings(move, value, yield, build);
                case YIELD -> new Settings(move, work, value, build);
                case BUILD -> new Settings(move, work, yield, value);
                default -> this;
            };
        }
    }
    public static final Settings DEFAULT = new Settings(100,100,100,100);
    private static final Map<MinecraftServer,Settings> ACTIVE = new WeakHashMap<>();
    private static final UUID MOVE_ID = UUID.fromString("d067bc51-1c37-45b4-9716-2d079fd76ff4");

    public static boolean allowed(ServerPlayer player) {
        return player != null && player.hasPermissions(2);
    }
    public static synchronized Settings get(MinecraftServer server) {
        return server == null ? DEFAULT : ACTIVE.getOrDefault(server, DEFAULT);
    }
    public static Settings get(Level level) {
        return level == null || level.isClientSide ? DEFAULT : get(level.getServer());
    }
    public static int next(int field, int current) {
        if (field < MOVE || field > BUILD) return 100;
        int[] choices = VALUES[field];
        for (int i = 0; i < choices.length; i++)
            if (choices[i] == current) return choices[(i + 1) % choices.length];
        return 100;
    }
    public static boolean valid(int field, int value) {
        if (field < MOVE || field > BUILD) return false;
        for (int choice : VALUES[field]) if (choice == value) return true;
        return false;
    }
    /** Called on server thread; recheck permission on every change, not just on screen open. */
    public static synchronized Settings change(ServerPlayer player, int field, int value) {
        if (!allowed(player)) return DEFAULT;
        MinecraftServer server = player.getServer();
        if (server == null) return DEFAULT;
        Settings old = get(server);
        Settings updated = field == RESET ? DEFAULT : valid(field,value) ? old.with(field,value) : old;
        if (!old.equals(updated)) {
            if (updated.equals(DEFAULT)) ACTIVE.remove(server); else ACTIVE.put(server, updated);
            Mill.LOGGER.info("MILL-TEST-TUNING: move={}%, work={}%, yield={}%, build={}% (by {})",
                    updated.move(),updated.work(),updated.yield(),updated.build(),player.getGameProfile().getName());
        }
        return updated;
    }
    public static void applyMovement(MillVillager villager) {
        if (villager.level.isClientSide) return;
        var attribute = villager.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) return;
        double amount = get(villager.level).move() / 100.0 - 1.0;
        var existing = attribute.getModifier(MOVE_ID);
        if (existing != null && existing.getAmount() == amount) return;
        if (existing != null) attribute.removeModifier(MOVE_ID);
        if (amount != 0.0) attribute.addTransientModifier(new AttributeModifier(MOVE_ID,
                "Millenaire test movement", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
    public static int duration(MillVillager villager, Goal goal, int base) {
        if (base <= 0) return base;
        Settings settings = get(villager.level);
        int percent = 100;
        if (goal instanceof GoalConstructionStepByStep || goal instanceof GoalBuildPath || goal instanceof GoalClearOldPath) {
            percent = settings.build();
        } else if (isWork(goal)) {
            percent = settings.work();
        }
        return percent == 100 ? base : (int)Math.max(1L, ((long)base * 100 + percent - 1) / percent);
    }
    private static boolean isWork(Goal goal) {
        if (goal.leasure || goal == Goal.sleep) return false;
        // Explicit production tasks, not combat, visits, trade, sleep or social timers.
        return goal instanceof GoalGenericCooking || goal instanceof GoalGenericTendFurnace
            || goal instanceof GoalGenericCrafting || goal instanceof GoalGenericMining
            || goal instanceof GoalGenericHarvestCrop || goal instanceof GoalGenericGatherBlocks
            || goal instanceof GoalGenericPlantCrop || goal instanceof GoalGenericPlantSapling
            || goal instanceof GoalGenericSlaughterAnimal
            || goal instanceof GoalMinerMineResource || goal instanceof GoalFish
            || goal instanceof GoalLumbermanChopTrees || goal instanceof GoalLumbermanPlantSaplings
            || goal instanceof GoalHarvestCacao || goal instanceof GoalHarvestWarts
            || goal instanceof GoalPlantCacao || goal instanceof GoalPlantNetherWarts
            || goal instanceof GoalIndianDryBrick || goal instanceof GoalIndianGatherBrick
            || goal instanceof GoalIndianHarvestSugarCane || goal instanceof GoalIndianPlantSugarCane
            || goal instanceof GoalShearSheep || goal instanceof GoalByzantineGatherSilk
            || goal instanceof GoalByzantineGatherSnails || goal instanceof GoalBrewPotions
            || goal instanceof GoalBreedAnimals;
    }
    /** Only call at confirmed natural-production sites, never in inventory-transfer APIs. */
    public static int resourceAmount(Level level, int base) {
        if (base <= 0) return base;
        int multiplier = get(level).yield() / 100;
        return (int)Math.min(Integer.MAX_VALUE, (long)base * multiplier);
    }
}
