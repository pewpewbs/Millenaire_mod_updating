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
import net.minecraft.world.WorldServerMulti;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
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
    public void clientLoggedIn(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        Mill.proxy.handleClientLogin();
    }

    @SubscribeEvent
    public void connectionClosed(FMLNetworkEvent.ServerDisconnectionFromClientEvent event) {
        for (MillWorldData mw : Mill.serverWorlds) {
            mw.checkConnections();
        }
    }

    @SubscribeEvent
    public void damageOnPlayer(LivingDamageEvent event) {
        if (event.getEntityLiving() instanceof Player) {
            LivingEntity source = null;
            if (event.getSource().getDirectEntity() != null && event.getSource().getDirectEntity() instanceof LivingEntity) {
                source = (LivingEntity)event.getSource().getDirectEntity();
            } else if (event.getSource().getEntity() != null && event.getSource().getEntity() instanceof LivingEntity) {
                source = (LivingEntity)event.getSource().getEntity();
            }
            if (source != null) {
                MillWorldData mw = Mill.getMillWorld(event.getEntityLiving().level);
                Player player = (Player)event.getEntityLiving();
                String playerName = player.getName().getString();
                for (MillVillager villager : mw.getAllKnownVillagers()) {
                    if (!playerName.equals(villager.hiredBy)) continue;
                    villager.setTarget((LivingEntity)event.getSource().getEntity());
                }
            }
        }
    }

    @SubscribeEvent
    public void handleFurnaceWithdrawals(PlayerEvent.ItemSmeltedEvent event) {
        Building building;
        BuildingLocation location;
        Point playerPos;
        if (event.smelting.getCount() == 0) {
            return;
        }
        Player player = event.player;
        MillWorldData mwd = Mill.getMillWorld(player.level);
        Building closestVillageTH = mwd.getClosestVillage(playerPos = new Point((Entity)player));
        if (closestVillageTH != null && !closestVillageTH.controlledBy(player) && (location = closestVillageTH.getLocationAtCoordWithTolerance(playerPos, 4)) != null && (building = location.getBuilding(player.level)) != null) {
            UserProfile serverProfile;
            boolean isBuildingPlayerOwned;
            boolean bl = isBuildingPlayerOwned = building.location.getPlan() != null && (building.location.getPlan().price > 0 || building.location.getPlan().isgift);
            if (!isBuildingPlayerOwned && !building.getResManager().furnaces.isEmpty() && (serverProfile = VillageUtilities.getServerProfile(player.level, player)) != null) {
                int reputationLost = event.smelting.getCount() * 100;
                serverProfile.adjustReputation(closestVillageTH, -reputationLost);
                ServerSender.sendTranslatedSentence(player, '6', "ui.stealingsmelteditems", "" + reputationLost);
            }
        }
    }

    private void inuitDropsSeaFood(LivingDropsEvent event) {
        if (event.getSource() != null && event.getSource().getEntity() != null && event.getSource().getEntity() instanceof Player) {
            Player player = (Player)event.getSource().getEntity();
            UserProfile profile = Mill.getMillWorld(event.getEntity().level).getProfile(player);
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
            UserProfile profile = Mill.getMillWorld(event.getEntity().level).getProfile(player);
            if (profile.isTagSet("huntingdrop_" + MillItems.WOLFMEAT_RAW.getRegistryName().getPath()) && (quantity = MillCommonUtilities.randomInt(3)) > 0) {
                event.getDrops().add(new ItemEntity(event.getEntityLiving().level, event.getEntityLiving().getX(), event.getEntityLiving().getY(), event.getEntityLiving().getZ(), new ItemStack((Item)MillItems.WOLFMEAT_RAW, quantity)));
                MillAdvancements.GREAT_HUNTER.grant(player);
            }
        }
    }

    @SubscribeEvent
    public void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        try {
            UserProfile profile = VillageUtilities.getServerProfile(event.player.level, event.player);
            if (profile != null && !event.player.func_70005_c_().equals(profile.playerName)) {
                MillLog.major(null, "Name of player with UUID '" + profile.uuid + "' changed from '" + profile.playerName + "' to '" + event.player.func_70005_c_() + "'.");
                profile.playerName = event.player.func_70005_c_();
                profile.saveProfile();
            }
            if (profile != null) {
                profile.connectUser();
            } else {
                MillLog.error(this, "Could not get profile on login for user: " + event.player.func_70005_c_());
            }
        }
        catch (Exception e) {
            MillLog.printException("Error in ConnectionHandler.playerLoggedIn:", e);
        }
    }

    @SubscribeEvent
    public void worldLoaded(WorldEvent.Load event) {
        Mill.proxy.loadLanguagesIfNeeded();
        if (Mill.displayMillenaireLocationError && !Mill.proxy.isTrueServer()) {
            Mill.proxy.sendLocalChat(Mill.proxy.getTheSinglePlayer(), '4', "ERREUR: Impossible de trouver le fichier de configuration " + Mill.proxy.getConfigFile().getAbsolutePath() + ". V\u00e9rifiez que le dossier millenaire est bien dans minecraft/mods/");
            Mill.proxy.sendLocalChat(Mill.proxy.getTheSinglePlayer(), '4', "ERROR: Could not find the config file at " + Mill.proxy.getConfigFile().getAbsolutePath() + ". Check that the millenaire directory is in minecraft/mods/");
            return;
        }
        if (!(event.getWorld() instanceof ServerLevel)) {
            Mill.clientWorld = new MillWorldData(event.getWorld());
        } else if (!(event.getWorld() instanceof WorldServerMulti)) {
            MillWorldData newWorld = new MillWorldData(event.getWorld());
            Mill.serverWorlds.add(newWorld);
            newWorld.loadData();
        }
    }

    @SubscribeEvent
    public void worldSaved(WorldEvent.Save event) {
        if (Mill.startupError) {
            return;
        }
        if (event.getWorld().dimension.getDimension() != 0) {
            return;
        }
        if (!(event.getWorld() instanceof ServerLevel)) {
            Mill.clientWorld.saveEverything();
        } else {
            for (MillWorldData mw : Mill.serverWorlds) {
                if (mw.world != event.getWorld()) continue;
                mw.saveEverything();
            }
        }
    }

    @SubscribeEvent
    public void worldUnloaded(WorldEvent.Unload event) {
        if (Mill.startupError) {
            return;
        }
        if (event.getWorld().dimension.getDimension() != 0) {
            return;
        }
        if (!(event.getWorld() instanceof ServerLevel)) {
            if (Mill.clientWorld.world == event.getWorld()) {
                Mill.clientWorld = null;
            }
        } else {
            ArrayList<MillWorldData> toDelete = new ArrayList<MillWorldData>();
            for (MillWorldData mw : Mill.serverWorlds) {
                if (mw.world != event.getWorld()) continue;
                toDelete.add(mw);
            }
            for (MillWorldData mw : toDelete) {
                Mill.serverWorlds.remove(mw);
            }
        }
    }
}

