/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.Blocks
 *  net.minecraft.init.Items
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.pathfinding.PathPoint
 *  net.minecraft.util.text.TextFormatting
 *  net.minecraft.world.World
 */
package org.millenaire.common.utilities;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import net.minecraft.core.Registry;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.item.MillItems;
import org.millenaire.common.network.ServerSender;
import org.millenaire.common.pathing.atomicstryker.AStarConfig;
import org.millenaire.common.pathing.atomicstryker.AStarNode;
import org.millenaire.common.pathing.atomicstryker.AStarPathPlannerJPS;
import org.millenaire.common.pathing.atomicstryker.IAStarPathedEntity;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.ThreadSafeUtilities;
import org.millenaire.common.utilities.WorldUtilities;

public class DevModUtilities {
    private static HashMap<Player, Integer> autoMoveDirection = new HashMap();
    private static HashMap<Player, Integer> autoMoveTarget = new HashMap();

    public static void fillInFreeGoods(Player player) {
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), (Item)MillItems.JAPANESE_BLUE_LEGGINGS, 1);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), (Item)MillItems.JAPANESE_BLUE_BOOTS, 1);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), (Item)MillItems.JAPANESE_BLUE_HELMET, 1);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), (Item)MillItems.JAPANESE_BLUE_CHESTPLATE, 1);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), (Item)MillItems.JAPANESE_RED_LEGGINGS, 1);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), (Item)MillItems.JAPANESE_RED_BOOTS, 1);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), (Item)MillItems.JAPANESE_RED_HELMET, 1);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), (Item)MillItems.JAPANESE_RED_CHESTPLATE, 1);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), (Item)MillItems.JAPANESE_GUARD_LEGGINGS, 1);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), (Item)MillItems.JAPANESE_GUARD_BOOTS, 1);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), (Item)MillItems.JAPANESE_GUARD_HELMET, 1);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), (Item)MillItems.JAPANESE_GUARD_CHESTPLATE, 1);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), MillItems.SUMMONING_WAND, 1);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), MillItems.AMULET_SKOLL_HATI, 1);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), Items.CLOCK, 1);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), (Item)MillItems.NORMAN_AXE, 1);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), (Item)MillItems.NORMAN_PICKAXE, 1);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), (Item)MillItems.NORMAN_SHOVEL, 1);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), Blocks.GOLD_BLOCK, 0, 64);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), Blocks.OAK_LOG, 64);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), Items.COAL, 64);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), Blocks.COBBLESTONE, 128);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), Blocks.STONE, 512);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), (Block)Blocks.SAND, 128);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), Blocks.WHITE_WOOL, 64);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), (Item)MillItems.CALVA, 0, 2);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), (Item)MillItems.CHICKEN_CURRY, 2);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), MillItems.RICE, 0, 64);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), MillItems.MAIZE, 0, 64);
        MillCommonUtilities.putItemsInChest((Container)player.getInventory(), MillItems.TURMERIC, 0, 64);
    }

    public static void runAutoMove(Level world) {
        for (Object o : world.players()) {
            Player p;
            if (!(o instanceof Player) || !autoMoveDirection.containsKey(p = (Player)o)) continue;
            if (autoMoveDirection.get(p) == 1) {
                if ((double)autoMoveTarget.get(p).intValue() < p.getX()) {
                    autoMoveDirection.put(p, -1);
                    autoMoveTarget.put(p, (int)(p.getX() - 100000.0));
                    ServerSender.sendChat(p, ChatFormatting.GREEN, "Auto-move: turning back.");
                }
            } else if (autoMoveDirection.get(p) == -1 && (double)autoMoveTarget.get(p).intValue() > p.getX()) {
                autoMoveDirection.put(p, 1);
                autoMoveTarget.put(p, (int)(p.getX() + 100000.0));
                ServerSender.sendChat(p, ChatFormatting.GREEN, "Auto-move: turning back again.");
            }
            p.teleportTo(p.getX() + (double)autoMoveDirection.get(p).intValue() * 0.5, p.getY(), p.getZ());
            p.absMoveTo(p.getX() + (double)autoMoveDirection.get(p).intValue() * 0.5, p.getY(), p.getZ(), p.getYRot(), p.getXRot());
        }
    }

    public static void testGetItemFromBlock() {
        long starttime = System.nanoTime();
        Iterator iterator = Registry.BLOCK.iterator();
        int count = 0;
        while (iterator.hasNext()) {
            Block block = (Block)iterator.next();
            Item.byBlock((Block)block);
            ++count;
        }
        MillLog.temp(null, "Took " + 1.0 * (double)(System.nanoTime() - starttime) / 1000000.0 + " ms to load " + count + " items from blocks.");
    }

    public static void testPaths(Player player) {
        Point centre = new Point((Entity)player);
        MillLog.temp(null, "Attempting test path around: " + player);
        Point start = null;
        Point end = null;
        int toleranceMode = 0;
        for (int i = 0; i < 100 && (start == null || end == null); ++i) {
            for (int j = 0; j < 100 && (start == null || end == null); ++j) {
                for (int k = 0; k < 100 && (start == null || end == null); ++k) {
                    for (int l = 0; l < 8 && (start == null || end == null); ++l) {
                        Point p = centre.getRelative(i * (1 - (l & 1) * 2), j * (1 - (l & 2)), k * (1 - (l & 4) / 2));
                        Block block = WorldUtilities.getBlock(player.level, p);
                        if (start == null && block == Blocks.GOLD_BLOCK) {
                            start = p;
                        }
                        if (end == null && block == Blocks.IRON_BLOCK) {
                            end = p.getAbove();
                            toleranceMode = 0;
                            continue;
                        }
                        if (end == null && block == Blocks.DIAMOND_BLOCK) {
                            end = p.getAbove();
                            toleranceMode = 1;
                            continue;
                        }
                        if (end != null || block != Blocks.LAPIS_BLOCK) continue;
                        end = p.getAbove();
                        toleranceMode = 2;
                    }
                }
            }
        }
        if (start != null && end != null) {
            DevPathedEntity pathedEntity = new DevPathedEntity(player.level, player);
            AStarConfig jpsConfig = toleranceMode == 1 ? new AStarConfig(true, false, false, true, true, 2, 2) : (toleranceMode == 2 ? new AStarConfig(true, false, false, true, true, 2, 20) : new AStarConfig(true, false, false, true, true));
            ServerSender.sendChat(player, ChatFormatting.DARK_GREEN, "Calculating path. Tolerance H: " + jpsConfig.toleranceHorizontal);
            AStarPathPlannerJPS jpsPathPlanner = new AStarPathPlannerJPS(player.level, pathedEntity, true);
            try {
                jpsPathPlanner.getPath(start.getiX(), start.getiY(), start.getiZ(), end.getiX(), end.getiY(), end.getiZ(), jpsConfig);
            }
            catch (ThreadSafeUtilities.ChunkAccessException e) {
                MillLog.printException(e);
            }
        } else {
            ServerSender.sendChat(player, ChatFormatting.DARK_RED, "Could not find start or end: " + start + " - " + end);
        }
    }

    public static void toggleAutoMove(Player player) {
        if (autoMoveDirection.containsKey(player)) {
            autoMoveDirection.remove(player);
            autoMoveTarget.remove(player);
            ServerSender.sendChat(player, ChatFormatting.GREEN, "Auto-move disabled");
        } else {
            autoMoveDirection.put(player, 1);
            autoMoveTarget.put(player, (int)(player.getX() + 100000.0));
            ServerSender.sendChat(player, ChatFormatting.GREEN, "Auto-move enabled");
        }
    }

    public static void validateResourceMap(Map<InvItem, Integer> map) {
        int errors = 0;
        for (InvItem item : map.keySet()) {
            if (item == null) {
                MillLog.printException(new MillLog.MillenaireException("Found a null InvItem in map!"));
                ++errors;
                continue;
            }
            if (!map.containsKey(item)) {
                MillLog.printException(new MillLog.MillenaireException("Key: " + item + " not present in map???"));
                ++errors;
                continue;
            }
            if (map.get(item) != null) continue;
            MillLog.printException(new MillLog.MillenaireException("Key: " + item + " has null value in map."));
            ++errors;
        }
        if (map.size() > 0) {
            MillLog.error(null, "Validated map. Found " + errors + " amoung " + map.size() + " keys.");
        }
    }

    public static void villagerInteractDev(Player entityplayer, MillVillager villager) {
        MillVillager child;
        if (villager.isBaby()) {
            villager.growSize();
            ServerSender.sendChat(entityplayer, ChatFormatting.GREEN, villager.func_70005_c_() + ": Size: " + villager.getSize() + " gender: " + villager.gender);
            if (entityplayer.getInventory().getSelected() != null && entityplayer.getInventory().getSelected().getItem() == MillItems.SUMMONING_WAND) {
                villager.getRecord().size = 20;
                villager.growSize();
            }
        }
        if (entityplayer.getInventory().getSelected() == ItemStack.EMPTY || entityplayer.getInventory().getSelected().getItem() == Items.AIR) {
            ServerSender.sendChat(entityplayer, ChatFormatting.GREEN, villager.func_70005_c_() + ": Current goal: " + villager.getGoalLabel(villager.goalKey) + " Current pos: " + villager.getPos());
            ServerSender.sendChat(entityplayer, ChatFormatting.GREEN, villager.func_70005_c_() + ": House: " + villager.housePoint + " Town Hall: " + villager.townHallPoint);
            ServerSender.sendChat(entityplayer, ChatFormatting.GREEN, villager.func_70005_c_() + ": ID: " + villager.getVillagerId());
            if (villager.getRecord() != null) {
                ServerSender.sendChat(entityplayer, ChatFormatting.GREEN, villager.func_70005_c_() + ": Spouse: " + villager.getRecord().spousesName);
            }
            if (villager.getPathDestPoint() != null && villager.pathEntity != null && villager.pathEntity.getNodeCount() > 1) {
                ServerSender.sendChat(entityplayer, ChatFormatting.GREEN, villager.func_70005_c_() + ": Dest: " + villager.getPathDestPoint() + " distance: " + villager.getPathDestPoint().distanceTo((Entity)villager) + " stuck: " + villager.longDistanceStuck + " jump:" + villager.pathEntity.getNextTargetPathPoint());
            } else {
                ServerSender.sendChat(entityplayer, ChatFormatting.GREEN, villager.func_70005_c_() + ": No dest point.");
            }
            String s = "";
            if (villager.getRecord() != null) {
                for (String tag : villager.getRecord().questTags) {
                    s = s + tag + " ";
                }
            }
            if (villager.mw.getProfile((Player)entityplayer).villagersInQuests.containsKey(villager.getVillagerId())) {
                s = s + " quest: " + villager.mw.getProfile((Player)entityplayer).villagersInQuests.get((Object)Long.valueOf((long)villager.getVillagerId())).quest.key + "/" + villager.mw.getProfile((Player)entityplayer).villagersInQuests.get((Object)Long.valueOf((long)villager.getVillagerId())).getCurrentVillager().id;
            }
            if (s != null && s.length() > 0) {
                ServerSender.sendChat(entityplayer, ChatFormatting.GREEN, "Tags: " + s);
            }
            s = "";
            for (InvItem key : villager.inventory.keySet()) {
                if (villager.inventory.get(key) <= 0) continue;
                s = s + key + ":" + villager.inventory.get(key) + " ";
            }
            if (villager.getTarget() != null) {
                s = s + "attacking: " + villager.getTarget() + " ";
            }
            if (s != null && s.length() > 0) {
                ServerSender.sendChat(entityplayer, ChatFormatting.GREEN, "Inv: " + s);
            }
        } else if (entityplayer.getInventory().getSelected() != ItemStack.EMPTY && entityplayer.getInventory().getSelected().getItem() == Item.byBlock((Block)Blocks.SAND)) {
            if (villager.hiredBy == null) {
                villager.hiredBy = entityplayer.getName().getString();
                ServerSender.sendChat(entityplayer, ChatFormatting.GREEN, "Hired: " + entityplayer.getName().getString());
            } else {
                villager.hiredBy = null;
                ServerSender.sendChat(entityplayer, ChatFormatting.GREEN, "No longer hired");
            }
        } else if (entityplayer.getInventory().getSelected() != ItemStack.EMPTY && entityplayer.getInventory().getSelected().getItem() == Item.byBlock((Block)Blocks.DIRT) && villager.pathEntity != null) {
            int meta = MillCommonUtilities.randomInt(16);
            for (Node p : villager.pathEntity.pointsCopy) {
                if (WorldUtilities.getBlock(villager.level, p.x, p.y - 1, p.z) == MillBlocks.LOCKED_CHEST) continue;
                WorldUtilities.setBlockstate(villager.level, new Point(p).getBelow(), LegacyBlockStates.fromName("minecraft:wool", meta), true, true);
            }
            Node p = villager.pathEntity.getCurrentTargetPathPoint();
            if (p != null && WorldUtilities.getBlock(villager.level, p.x, p.y - 1, p.z) != MillBlocks.LOCKED_CHEST) {
                WorldUtilities.setBlockAndMetadata(villager.level, new Point(p).getBelow(), Blocks.GOLD_BLOCK, 0);
            }
            if ((p = villager.pathEntity.getNextTargetPathPoint()) != null && WorldUtilities.getBlock(villager.level, p.x, p.y - 1, p.z) != MillBlocks.LOCKED_CHEST) {
                WorldUtilities.setBlockAndMetadata(villager.level, new Point(p).getBelow(), Blocks.DIAMOND_BLOCK, 0);
            }
            if ((p = villager.pathEntity.getPreviousTargetPathPoint()) != null && WorldUtilities.getBlock(villager.level, p.x, p.y - 1, p.z) != MillBlocks.LOCKED_CHEST) {
                WorldUtilities.setBlockAndMetadata(villager.level, new Point(p).getBelow(), Blocks.IRON_BLOCK, 0);
            }
        }
        if (villager.hasChildren() && entityplayer.getInventory().getSelected() != ItemStack.EMPTY && entityplayer.getInventory().getSelected().getItem() == MillItems.SUMMONING_WAND && (child = villager.getHouse().createChild(villager, villager.getTownHall(), villager.getRecord().spousesName)) != null) {
            child.getRecord().size = 20;
            child.growSize();
        }
    }

    private static class DevPathedEntity
    implements IAStarPathedEntity {
        Level world;
        Player caller;

        DevPathedEntity(Level w, Player p) {
            this.world = w;
            this.caller = p;
        }

        @Override
        public void onFoundPath(List<AStarNode> result) {
            // Copy coordinates before returning control to the pathfinding worker.
            List<Point> markers = new java.util.ArrayList<>();
            for (int i = 1; i + 1 < result.size(); i++) markers.add(new Point(result.get(i)).getBelow());
            Mill.proxy.executeWorldTask(this.world, () -> {
                var state = LegacyBlockStates.fromName("minecraft:wool", MillCommonUtilities.randomInt(16));
                for (Point marker : markers) WorldUtilities.setBlockstate(this.world, marker, state, true, true);
            });
        }

        @Override
        public void onNoPathAvailable() {
            Mill.proxy.executeWorldTask(this.world, () -> ServerSender.sendChat(this.caller, ChatFormatting.DARK_RED, "No path available."));
        }
    }
}

