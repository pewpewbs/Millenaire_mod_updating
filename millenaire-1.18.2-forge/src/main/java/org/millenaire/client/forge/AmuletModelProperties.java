package org.millenaire.client.forge;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.millenaire.common.item.MillItems;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;

@Mod.EventBusSubscriber(modid = "millenaire", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AmuletModelProperties {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(AmuletModelProperties::register);
    }
    private static void register() {
        ItemProperties.register(MillItems.AMULET_VISHNU, new ResourceLocation("score"), new ItemPropertyFunction() {
            long lastUpdateTick = Long.MIN_VALUE;
            LivingEntity lastEntity;
            float savedScore;
            public float call(ItemStack stack, @Nullable net.minecraft.client.multiplayer.ClientLevel ignoredWorld, @Nullable LivingEntity entityIn, int seed) {
                if (entityIn == null) {
                    return 0.0f;
                }
                Level world = entityIn.level;
                if (entityIn != this.lastEntity || world.getGameTime() != this.lastUpdateTick) {
                    double level = 0.0;
                    double closestDistance = Double.MAX_VALUE;
                    if (world != null && entityIn != null) {
                        Point p = new Point((Entity)entityIn);
                        List<Entity> entities = WorldUtilities.getEntitiesWithinAABB(world, Monster.class, p, 20, 20);
                        for (Entity ent : entities) {
                            if (!(p.distanceTo(ent) < closestDistance)) continue;
                            closestDistance = p.distanceTo(ent);
                        }
                    }
                    level = closestDistance > 20.0 ? 0.0 : (20.0 - closestDistance) / 20.0;
                    this.savedScore = (float)(level * 15.0);
                    this.lastUpdateTick = world.getGameTime();
                    this.lastEntity = entityIn;
                    return this.savedScore;
                }
                return this.savedScore;
            }
        });
        ItemProperties.register(MillItems.AMULET_ALCHEMIST, new ResourceLocation("score"), new ItemPropertyFunction() {
            long lastUpdateTick = Long.MIN_VALUE;
            LivingEntity lastEntity;
            float savedScore;
            public float call(ItemStack stack, @Nullable net.minecraft.client.multiplayer.ClientLevel ignoredWorld, @Nullable LivingEntity entityIn, int seed) {
                if (entityIn == null) {
                    return 0.0f;
                }
                Level world = entityIn.level;
                if (entityIn != this.lastEntity || world.getGameTime() != this.lastUpdateTick) {
                    float score = 0.0f;
                    if (world != null && entityIn != null) {
                        Point p = new Point((Entity)entityIn);
                        int startY = Math.max(p.getiY() - 5, world.getMinBuildHeight());
                        int endY = Math.min(p.getiY() + 5, world.getMaxBuildHeight());
                        for (int i = p.getiX() - 5; i < p.getiX() + 5; ++i) {
                            for (int j = p.getiZ() - 5; j < p.getiZ() + 5; ++j) {
                                for (int k = startY; k < endY; ++k) {
                                    Block block = WorldUtilities.getBlock(world, i, k, j);
                                    if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) {
                                        score += 1.0f;
                                        continue;
                                    }
                                    if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
                                        score += 30.0f;
                                        continue;
                                    }
                                    if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) {
                                        score += 30.0f;
                                        continue;
                                    }
                                    if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) {
                                        score += 10.0f;
                                        continue;
                                    }
                                    if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) {
                                        score += 5.0f;
                                        continue;
                                    }
                                    if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) {
                                        score += 10.0f;
                                        continue;
                                    }
                                    if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) {
                                        score += 5.0f;
                                        continue;
                                    }

                                }
                            }
                        }
                    }
                    if (score > 100.0f) {
                        score = 100.0f;
                    }
                    this.savedScore = score * 15.0f / 100.0f;
                    this.lastUpdateTick = world.getGameTime();
                    this.lastEntity = entityIn;
                    return this.savedScore;
                }
                return this.savedScore;
            }
        });
        ItemProperties.register(MillItems.AMULET_YDDRASIL, new ResourceLocation("score"), new ItemPropertyFunction() {
            long lastUpdateTick = Long.MIN_VALUE;
            LivingEntity lastEntity;
            float savedScore;
            public float call(ItemStack stack, @Nullable net.minecraft.client.multiplayer.ClientLevel ignoredWorld, @Nullable LivingEntity entityIn, int seed) {
                if (entityIn == null) {
                    return 0.0f;
                }
                Level world = entityIn.level;
                if (entityIn != this.lastEntity || world.getGameTime() != this.lastUpdateTick) {
                    int level = 0;
                    Point p = new Point((Entity)entityIn);
                    level = Math.max(0, p.getiY());
                    if (level > 127) {
                        level = 127;
                    }
                    this.savedScore = level / 8;
                    this.lastUpdateTick = world.getGameTime();
                    this.lastEntity = entityIn;
                    return this.savedScore;
                }
                return this.savedScore;
            }
        });
    }
}
