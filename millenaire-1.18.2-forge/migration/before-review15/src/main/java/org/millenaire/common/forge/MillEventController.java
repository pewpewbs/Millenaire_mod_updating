/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.item.EntityItem
 *  net.minecraft.entity.monster.EntityElderGuardian
 *  net.minecraft.entity.monster.EntityGuardian
 *  net.minecraft.entity.monster.EntityPolarBear
 *  net.minecraft.entity.passive.EntitySquid
 *  net.minecraft.entity.passive.EntityWolf
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.world.WorldServer
 *  net.minecraft.world.WorldServerMulti
 *  net.minecraftforge.event.entity.living.LivingDamageEvent
 *  net.minecraftforge.event.entity.living.LivingDropsEvent
 *  net.minecraftforge.event.world.WorldEvent$Load
 *  net.minecraftforge.event.world.WorldEvent$Save
 *  net.minecraftforge.event.world.WorldEvent$Unload
 *  net.minecraftforge.fml.common.eventhandler.SubscribeEvent
 *  net.minecraftforge.fml.common.gameevent.PlayerEvent$ItemSmeltedEvent
 *  net.minecraftforge.fml.common.gameevent.PlayerEvent$PlayerLoggedInEvent
 *  net.minecraftforge.fml.common.network.FMLNetworkEvent$ClientConnectedToServerEvent
 *  net.minecraftforge.fml.common.network.FMLNetworkEvent$ServerDisconnectionFromClientEvent
 */
package org.millenaire.common.forge;

import java.util.ArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.millenaire.common.advancements.MillAdvancements;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.item.MillItems;
import org.millenaire.common.network.ServerSender;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.VillageUtilities;
import org.millenaire.common.village.Building;
import org.millenaire.common.village.BuildingLocation;
import org.millenaire.common.world.MillWorldData;
import org.millenaire.common.world.UserProfile;

public class MillEventController {
    @SubscribeEvent
    public void addInuitDrops(LivingDropsEvent event) {
        if (event.getEntityLiving().level.isClientSide) return;
        if (event.getEntityLiving() instanceof Guardian || event.getEntityLiving() instanceof Squid) {
            this.inuitDropsSeaFood(event);
        } else if (event.getEntityLiving() instanceof Wolf) {
            this.inuitDropsWolfMeat(event);
        } else if (event.getEntityLiving() instanceof PolarBear) {
            int quantity = 1 + MillCommonUtilities.randomInt(2);
            event.getDrops().add(new ItemEntity(event.getEntityLiving().level, event.getEntityLiving().getX(), event.getEntityLiving().getY(), event.getEntityLiving().getZ(), new ItemStack((Item)MillItems.BEARMEAT_RAW, quantity)));
        }
    }

    @SubscribeEvent
    public void connectionClosed(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getPlayer().level.isClientSide || Mill.serverWorlds == null) return;
        for (MillWorldData mw : Mill.serverWorlds) {
            UserProfile profile = mw.profiles.get(event.getPlayer().getUUID());
            if (profile != null && profile.connected) profile.disconnectUser();
        }
    }

    @SubscribeEvent
    public void damageOnPlayer(LivingDamageEvent event) {
        if (event.getEntityLiving().level.isClientSide) return;
        if (event.getEntityLiving() instanceof Player) {
            LivingEntity source = null;
            if (event.getSource().getDirectEntity() != null && event.getSource().getDirectEntity() instanceof LivingEntity) {
                source = (LivingEntity)event.getSource().getDirectEntity();
            } else if (event.getSource().getEntity() != null && event.getSource().getEntity() instanceof LivingEntity) {
                source = (LivingEntity)event.getSource().getEntity();
            }
            if (source != null) {
                MillWorldData mw = Mill.getMillWorld(event.getEntityLiving().level);
                if (mw == null) return;
                Player player = (Player)event.getEntityLiving();
                String playerName = player.getName().getString();
                for (MillVillager villager : mw.getAllKnownVillagers()) {
                    if (!playerName.equals(villager.hiredBy)) continue;
                    villager.setTarget(source);
                }
            }
        }
    }

    @SubscribeEvent
    public void handleFurnaceWithdrawals(PlayerEvent.ItemSmeltedEvent event) {
        Building building;
        BuildingLocation location;
        Point playerPos;
        if (event.getSmelting().getCount() == 0) {
            return;
        }
        Player player = event.getPlayer();
        if (player.level.isClientSide) return;
        MillWorldData mwd = Mill.getMillWorld(player.level);
        if (mwd == null) return;
        Building closestVillageTH = mwd.getClosestVillage(playerPos = new Point((Entity)player));
        if (closestVillageTH != null && !closestVillageTH.controlledBy(player) && (location = closestVillageTH.getLocationAtCoordWithTolerance(playerPos, 4)) != null && (building = location.getBuilding(player.level)) != null) {
            UserProfile serverProfile;
            boolean isBuildingPlayerOwned;
            boolean bl = isBuildingPlayerOwned = building.location.getPlan() != null && (building.location.getPlan().price > 0 || building.location.getPlan().isgift);
            if (!isBuildingPlayerOwned && !building.getResManager().furnaces.isEmpty() && (serverProfile = VillageUtilities.getServerProfile(player.level, player)) != null) {
                int reputationLost = event.getSmelting().getCount() * 100;
                serverProfile.adjustReputation(closestVillageTH, -reputationLost);
                ServerSender.sendTranslatedSentence(player, '6', "ui.stealingsmelteditems", "" + reputationLost);
            }
        }
    }

    private void inuitDropsSeaFood(LivingDropsEvent event) {
        if (event.getSource() != null && event.getSource().getEntity() != null && event.getSource().getEntity() instanceof Player) {
            Player player = (Player)event.getSource().getEntity();
            MillWorldData world = Mill.getMillWorld(event.getEntity().level);
            if (world == null) return;
            UserProfile profile = world.getProfile(player);
            if (profile.isTagSet("huntingdrop_" + MillItems.SEAFOOD_RAW.getRegistryName().getPath())) {
                int quantity = 0;
                if (event.getEntityLiving() instanceof Squid) {
                    if (MillCommonUtilities.chanceOn(10)) {
                        quantity = 1;
                    }
                } else if (event.getEntityLiving() instanceof ElderGuardian) {
                    quantity = 5 + MillCommonUtilities.randomInt(5);
                } else if (event.getEntityLiving() instanceof Guardian) {
                    quantity = 2 + MillCommonUtilities.randomInt(2);
                }
                if (quantity > 0) {
                    event.getDrops().add(new ItemEntity(event.getEntityLiving().level, event.getEntityLiving().getX(), event.getEntityLiving().getY(), event.getEntityLiving().getZ(), new ItemStack((Item)MillItems.SEAFOOD_RAW, quantity)));
                    MillAdvancements.GREAT_HUNTER.grant(player);
                }
            }
        }
    }

    private void inuitDropsWolfMeat(LivingDropsEvent event) {
        if (event.getSource() != null && event.getSource().getEntity() != null && event.getSource().getEntity() instanceof Player) {
            int quantity;
            Player player = (Player)event.getSource().getEntity();
            MillWorldData world = Mill.getMillWorld(event.getEntity().level);
            if (world == null) return;
            UserProfile profile = world.getProfile(player);
            if (profile.isTagSet("huntingdrop_" + MillItems.WOLFMEAT_RAW.getRegistryName().getPath()) && (quantity = MillCommonUtilities.randomInt(3)) > 0) {
                event.getDrops().add(new ItemEntity(event.getEntityLiving().level, event.getEntityLiving().getX(), event.getEntityLiving().getY(), event.getEntityLiving().getZ(), new ItemStack((Item)MillItems.WOLFMEAT_RAW, quantity)));
                MillAdvancements.GREAT_HUNTER.grant(player);
            }
        }
    }

    @SubscribeEvent
    public void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer().level.isClientSide) return;
        try {
            UserProfile profile = VillageUtilities.getServerProfile(event.getPlayer().level, event.getPlayer());
            if (profile != null && !event.getPlayer().getGameProfile().getName().equals(profile.playerName)) {
                MillLog.major(null, "Name of player with UUID '" + profile.uuid + "' changed from '" + profile.playerName + "' to '" + event.getPlayer().getGameProfile().getName() + "'.");
                profile.playerName = event.getPlayer().getGameProfile().getName();
                profile.saveProfile();
            }
            if (profile != null) {
                profile.connectUser();
            } else {
                MillLog.error(this, "Could not get profile on login for user: " + event.getPlayer().getGameProfile().getName());
            }
        }
        catch (Exception e) {
            MillLog.printException("Error in ConnectionHandler.playerLoggedIn:", e);
        }
    }

    @SubscribeEvent
    public void worldLoaded(WorldEvent.Load event) {
        if (!(event.getWorld() instanceof Level level)) return;
        Mill.proxy.loadLanguagesIfNeeded();
        if (Mill.startupError || Mill.displayMillenaireLocationError) return;
        if (level.isClientSide) {
            if (Mill.clientWorld == null || Mill.clientWorld.world != level)
                Mill.clientWorld = new MillWorldData(level);
        } else if (level instanceof ServerLevel && level.dimension().equals(Level.OVERWORLD)) {
            // Original saves belong to the overworld; secondary dimensions must not load the same directory.
            if (Mill.serverWorlds == null) Mill.serverWorlds = new ArrayList<>();
            for (MillWorldData existing : Mill.serverWorlds) if (existing.world == level) return;
            MillWorldData newWorld = new MillWorldData(level);
            Mill.serverWorlds.add(newWorld);
            newWorld.loadData();
        }
    }

    @SubscribeEvent
    public void worldSaved(WorldEvent.Save event) {
        if (Mill.startupError || !(event.getWorld() instanceof ServerLevel level)
                || !level.dimension().equals(Level.OVERWORLD) || Mill.serverWorlds == null) return;
        for (MillWorldData mw : Mill.serverWorlds) if (mw.world == level) mw.saveEverything();
    }

    @SubscribeEvent
    public void worldUnloaded(WorldEvent.Unload event) {
        // Always release matching state, including failed startup and client dimension changes.
        if (Mill.clientWorld != null && Mill.clientWorld.world == event.getWorld()) Mill.clientWorld = null;
        if (Mill.serverWorlds != null) Mill.serverWorlds.removeIf(mw -> mw.world == event.getWorld());
    }
}
