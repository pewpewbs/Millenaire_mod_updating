/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.init.Blocks
 *  net.minecraft.init.Items
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntitySign
 *  net.minecraft.util.EnumActionResult
 *  net.minecraft.util.math.AxisAlignedBB
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.text.TextComponentString
 *  net.minecraft.world.World
 */
package org.millenaire.common.ui;

import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.level.Level;
import org.millenaire.common.advancements.MillAdvancements;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.buildingplan.BuildingCustomPlan;
import org.millenaire.common.buildingplan.BuildingImportExport;
import org.millenaire.common.buildingplan.BuildingPlan;
import org.millenaire.common.buildingplan.BuildingPlanSet;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.culture.Culture;
import org.millenaire.common.culture.VillageType;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.item.ItemParchment;
import org.millenaire.common.item.MillItems;
import org.millenaire.common.network.ServerSender;
import org.millenaire.common.quest.QuestInstance;
import org.millenaire.common.quest.SpecialQuestActions;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;
import org.millenaire.common.village.Building;
import org.millenaire.common.village.BuildingProject;
import org.millenaire.common.village.VillagerRecord;
import org.millenaire.common.world.MillWorldData;
import org.millenaire.common.world.UserProfile;
import org.millenaire.common.world.WorldGenVillage;

public class GuiActions {
    public static final int VILLAGE_SCROLL_PRICE = 128;
    public static final int VILLAGE_SCROLL_REPUTATION = 8192;
    public static final int CROP_REPUTATION = 8192;
    public static final int CROP_PRICE = 512;
    public static final int CULTURE_CONTROL_REPUTATION = 131072;

    public static void activateMillChest(Player player, Point p) {
        if(player.level.isClientSide || p==null)return;
        net.minecraft.core.BlockPos chestBlockPos=new net.minecraft.core.BlockPos(p.getiX(),p.getiY(),p.getiZ());
        if(!player.level.hasChunkAt(chestBlockPos) || player.distanceToSqr(p.getiX()+.5,p.getiY()+.5,p.getiZ()+.5)>64
            || !player.level.mayInteract(player,chestBlockPos)
            || !(player.level.getBlockEntity(chestBlockPos) instanceof org.millenaire.common.entity.TileEntityLockedChest))return;

        MillWorldData mw;
        Level world = player.level;
        if (MillConfigValues.DEV && (mw = Mill.getMillWorld(world)) != null && mw.buildingExists(p)) {
            Building ent = mw.getBuilding(p);
            if (player.getInventory().getSelected() != ItemStack.EMPTY && player.getInventory().getSelected().getItem() == Item.byBlock((Block)Blocks.SAND)) {
                ent.testModeGoods();
                return;
            }
            if (player.getInventory().getSelected() != ItemStack.EMPTY && player.getInventory().getSelected().getItem() == Item.byBlock((Block)MillBlocks.PATHDIRT)) {
                ent.recalculatePaths(true);
                ent.clearOldPaths();
                ent.constructCalculatedPaths();
                return;
            }
            if (player.getInventory().getSelected() != ItemStack.EMPTY && player.getInventory().getSelected().getItem() == Item.byBlock((Block)MillBlocks.PATHGRAVEL)) {
                ent.clearOldPaths();
                return;
            }
            if (player.getInventory().getSelected() != ItemStack.EMPTY && player.getInventory().getSelected().getItem() == Item.byBlock((Block)MillBlocks.PATHDIRT_SLAB)) {
                ent.recalculatePaths(true);
                return;
            }
            if (player.getInventory().getSelected() != ItemStack.EMPTY && player.getInventory().getSelected().getItem() == MillItems.DENIER_OR) {
                ent.displayInfos(player);
                return;
            }
            if (player.getInventory().getSelected() != ItemStack.EMPTY && player.getInventory().getSelected().getItem() == Items.GLASS_BOTTLE) {
                mw.setGlobalTag("alchemy");
                MillLog.major(mw, "Set alchemy tag.");
                return;
            }
            if (player.getInventory().getSelected() != ItemStack.EMPTY && player.getInventory().getSelected().getItem() == MillItems.SUMMONING_WAND) {
                ent.displayInfos(player);
                try {
                    if (ent.isTownhall) {
                        ent.rushCurrentConstructions(false);
                    }
                    if (ent.isInn) {
                        ent.attemptMerchantMove(true);
                    }
                    if (ent.hasVisitors) {
                        ent.getVisitorManager().update(true);
                    }
                }
                catch (Exception e) {
                    MillLog.printException(e);
                }
                return;
            }
            if (player.getInventory().getSelected() != ItemStack.EMPTY && player.getInventory().getSelected().getItem() == Item.byBlock((Block)MillBlocks.PAINTED_BRICK_WHITE)) {
                ent.choseAndApplyBrickTheme();
                MillLog.major(mw, "Changed theme of village " + ent.getVillageQualifiedName() + " to: " + ent.brickColourTheme.key);
                return;
            }
        }
        ServerSender.displayMillChest(player, p);
    }

    public static void controlledBuildingsForgetBuilding(Player player, Building townHall, BuildingProject project) {
        townHall.cancelBuilding(project.location);
    }

    public static void controlledBuildingsToggleUpgrades(Player player, Building townHall, BuildingProject project, boolean allow) {
        project.location.upgradesAllowed = allow;
        if (allow) {
            townHall.noProjectsLeft = false;
        }
    }

    public static void controlledMilitaryCancelRaid(Player player, Building townHall) {
        if (townHall.raidStart == 0L) {
            townHall.cancelRaid();
            if (!townHall.world.isClientSide) {
                townHall.sendBuildingPacket(player, false);
            }
        }
    }

    public static void controlledMilitaryDiplomacy(Player player, Building townHall, Point target, int level) {
        townHall.adjustRelation(target, level, true);
        if (!townHall.world.isClientSide) {
            townHall.sendBuildingPacket(player, false);
        }
    }

    public static void controlledMilitaryPlanRaid(Player player, Building townHall, Building target) {
        if (townHall.raidStart == 0L) {
            townHall.adjustRelation(target.getPos(), -100, true);
            townHall.planRaid(target);
            if (!townHall.world.isClientSide) {
                townHall.sendBuildingPacket(player, false);
            }
        }
    }

    public static void hireExtend(Player player, MillVillager villager) {
        villager.hiredBy = player.getName().getString();
        villager.hiredUntil += 24000L;
        MillCommonUtilities.changeMoney((Container)player.getInventory(), -villager.getHireCost(player), player);
    }

    public static void hireHire(Player player, MillVillager villager) {
        villager.hiredBy = player.getName().getString();
        villager.hiredUntil = villager.level.getDayTime() + 24000L;
        VillagerRecord vr = villager.getRecord();
        if (vr != null) {
            vr.awayhired = true;
        }
        MillAdvancements.HIRED.grant(player);
        MillCommonUtilities.changeMoney((Container)player.getInventory(), -villager.getHireCost(player), player);
    }

    public static void hireRelease(Player player, MillVillager villager) {
        villager.hiredBy = null;
        villager.hiredUntil = 0L;
        VillagerRecord vr = villager.getRecord();
        if (vr != null) {
            vr.awayhired = false;
        }
    }

    public static void hireToggleStance(Player player, boolean stance) {
        AABB surroundings = new AABB(player.getX(), player.getY(), player.getZ(), player.getX() + 1.0, player.getY() + 1.0, player.getZ() + 1.0).expandTowards(16.0, 8.0, 16.0).expandTowards(-16.0, -8.0, -16.0);
        List list = player.level.getEntitiesOfClass(MillVillager.class, surroundings);
        for (Object o : list) {
            MillVillager villager = (MillVillager)o;
            if (!player.getName().getString().equals(villager.hiredBy)) continue;
            villager.aggressiveStance = stance;
        }
    }

    public static void newBuilding(Player player, Building townHall, Point pos, String planKey) {
        BuildingPlanSet set = townHall.culture.getBuildingPlanSet(planKey);
        if (set == null) {
            return;
        }
        BuildingPlan plan = set.getRandomStartingPlan();
        BuildingPlan.LocationReturn lr = plan.testSpot(townHall.winfo, townHall.regionMapper, townHall.getPos(), pos.getiX() - townHall.winfo.mapStartX, pos.getiZ() - townHall.winfo.mapStartZ, MillCommonUtilities.getRandom(), -1, true);
        if (lr.location == null) {
            String error = null;
            error = lr.errorCode == 3 ? "ui.constructionforbidden" : (lr.errorCode == 2 ? "ui.locationclash" : (lr.errorCode == 1 ? "ui.outsideradius" : (lr.errorCode == 4 ? "ui.wrongelevation" : (lr.errorCode == 5 ? "ui.danger" : (lr.errorCode == 6 ? "ui.notreachable" : "ui.unknownerror")))));
            if (MillConfigValues.DEV) {
                WorldUtilities.setBlock(townHall.mw.world, lr.errorPos.getRelative(0.0, 30.0, 0.0), Blocks.GRAVEL);
            }
            ServerSender.sendTranslatedSentence(player, '6', "ui.problemat", pos.distanceDirectionShort(lr.errorPos), error);
        } else {
            lr.location.level = -1;
            BuildingProject project = new BuildingProject(set);
            project.location = lr.location;
            GuiActions.setSign(townHall, lr.location.minx, lr.location.minz, project);
            GuiActions.setSign(townHall, lr.location.maxx, lr.location.minz, project);
            GuiActions.setSign(townHall, lr.location.minx, lr.location.maxz, project);
            GuiActions.setSign(townHall, lr.location.maxx, lr.location.maxz, project);
            townHall.buildingProjects.get((Object)BuildingProject.EnumProjects.CUSTOMBUILDINGS).add(project);
            townHall.noProjectsLeft = false;
            ServerSender.sendTranslatedSentence(player, '2', "ui.projectadded", new String[0]);
        }
    }

    public static void newCustomBuilding(Player player, Building townHall, Point pos, String planKey) {
        BuildingCustomPlan customBuilding = townHall.culture.getBuildingCustom(planKey);
        if (customBuilding != null) {
            try {
                townHall.addCustomBuilding(customBuilding, pos);
            }
            catch (Exception e) {
                MillLog.printException("Exception when creation custom building: " + planKey, e);
            }
        }
    }

    public static void newVillageCreation(Player player, Point pos, String cultureKey, String villageTypeKey) {
        Culture culture = Culture.getCultureByName(cultureKey);
        if (culture == null) {
            return;
        }
        VillageType villageType = culture.getVillageType(villageTypeKey);
        if (villageType == null) {
            return;
        }
        WorldGenVillage genVillage = new WorldGenVillage();
        boolean result = genVillage.generateVillageAtPoint(player.level, MillCommonUtilities.random, pos.getiX(), pos.getiY(), pos.getiZ(), player, false, true, false, 0, villageType, null, null, 0.0f);
        if (result) {
            MillAdvancements.SUMMONING_WAND.grant(player);
            if (villageType.playerControlled && MillAdvancements.VILLAGE_LEADER_ADVANCEMENTS.containsKey(cultureKey)) {
                MillAdvancements.VILLAGE_LEADER_ADVANCEMENTS.get(cultureKey).grant(player);
            }
            if (villageType.playerControlled && villageType.customCentre != null) {
                MillAdvancements.AMATEUR_ARCHITECT.grant(player);
            }
        }
    }

    public static void pujasChangeEnchantment(Player player, Building temple, int enchantmentId) {
        if (player.level.isClientSide || !(player.containerMenu instanceof ContainerPuja menu)
                || menu.shrine.temple != temple || !menu.stillValid(player)
                || enchantmentId < 0 || enchantmentId >= menu.shrine.getTargets().size()) return;
        if (temple != null && temple.pujas != null) {
            temple.pujas.changeEnchantment(enchantmentId);
            temple.sendBuildingPacket(player, false);
            if (temple.pujas.type == 0) {
                MillAdvancements.PUJA.grant(player);
            } else if (temple.pujas.type == 1) {
                MillAdvancements.SACRIFICE.grant(player);
            }
        }
    }

    public static void questCompleteStep(Player player, MillVillager villager) {
        UserProfile profile = Mill.getMillWorld(player.level).getProfile(player);
        QuestInstance qi = profile.villagersInQuests.get(villager.getVillagerId());
        if (qi == null) {
            MillLog.error(villager, "Could not find quest instance for this villager.");
        } else {
            qi.completeStep(player, villager);
        }
    }

    public static void questRefuse(Player player, MillVillager villager) {
        UserProfile profile = Mill.getMillWorld(player.level).getProfile(player);
        QuestInstance qi = profile.villagersInQuests.get(villager.getVillagerId());
        if (qi == null) {
            MillLog.error(villager, "Could not find quest instance for this villager.");
        } else {
            qi.refuseQuest(player, villager);
        }
    }

    private static void setSign(Building townHall, int i, int j, BuildingProject project) {
        WorldUtilities.setBlockAndMetadata(townHall.world, i, WorldUtilities.findTopSoilBlock(townHall.world, i, j), j, Blocks.OAK_SIGN, 0, true, false);
        SignBlockEntity sign = (SignBlockEntity)townHall.world.getBlockEntity(new BlockPos(i, WorldUtilities.findTopSoilBlock(townHall.world, i, j), j));
        if (sign != null) {
            sign.setMessage(0, new TextComponent(project.getNativeName()));
            sign.setMessage(1, new TextComponent(""));
            sign.setMessage(2, new TextComponent(project.getGameName()));
            sign.setMessage(3, new TextComponent(""));
        }
    }

    public static void updateCustomBuilding(Player player, Building building) {
        if (building.location.getCustomPlan() != null) {
            building.location.getCustomPlan().registerResources(building, building.location);
        }
    }

    public static void useNegationWand(Player player, Building townHall) {
        ServerSender.sendTranslatedSentence(player, '4', "negationwand.destroyed", townHall.villageType.name);
        if (!townHall.villageType.lonebuilding) {
            MillAdvancements.SCIPIO.grant(player);
        }
        townHall.destroyVillage();
    }

    public static InteractionResult useSummoningWand(ServerPlayer player, Point pos) {
        MillWorldData mw = Mill.getMillWorld(player.level);
        Block block = WorldUtilities.getBlock(player.level, pos);
        Building closestVillage = mw.getClosestVillage(pos);
        if (closestVillage != null && pos.squareRadiusDistance(closestVillage.getPos()) < closestVillage.villageType.radius + 10) {
            if (block == Blocks.OAK_SIGN) {
                return InteractionResult.FAIL;
            }
            if (closestVillage.controlledBy((Player)player)) {
                Building b = closestVillage.getBuildingAtCoordPlanar(pos);
                if (b != null) {
                    if (b.location.isCustomBuilding) {
                        ServerSender.displayNewBuildingProjectGUI((Player)player, closestVillage, pos);
                    } else {
                        ServerSender.sendTranslatedSentence((Player)player, 'e', "ui.wand_locationinuse", new String[0]);
                    }
                } else {
                    ServerSender.displayNewBuildingProjectGUI((Player)player, closestVillage, pos);
                }
                return InteractionResult.SUCCESS;
            }
            ServerSender.sendTranslatedSentence((Player)player, 'e', "ui.wand_invillagerange", closestVillage.getVillageQualifiedName());
            return InteractionResult.FAIL;
        }
        if (block == Blocks.OAK_SIGN) {
            if (!Mill.proxy.isTrueServer() || player.server.getPlayerList().isOp(player.getGameProfile())) {
                BuildingImportExport.summoningWandImportBuildingRequest((Player)player, Mill.serverWorlds.get((int)0).world, pos);
            } else {
                ServerSender.sendTranslatedSentence((Player)player, '4', "ui.serverimportforbidden", new String[0]);
            }
            return InteractionResult.SUCCESS;
        }
        if (block == MillBlocks.LOCKED_CHEST) {
            return InteractionResult.PASS;
        }
        if (block == Blocks.OBSIDIAN) {
            WorldGenVillage genVillage = new WorldGenVillage();
            genVillage.generateVillageAtPoint(player.level, MillCommonUtilities.random, pos.getiX(), pos.getiY(), pos.getiZ(), (Player)player, false, true, false, 0, null, null, null, 0.0f);
            return InteractionResult.SUCCESS;
        }
        if (block == Blocks.GOLD_BLOCK) {
            ServerSender.displayNewVillageGUI((Player)player, pos);
            return InteractionResult.SUCCESS;
        }
        if (mw.getProfile((Player)player).isTagSet("normanmarvel_picklocation")) {
            SpecialQuestActions.normanMarvelPickLocation(mw, (Player)player, pos);
            return InteractionResult.SUCCESS;
        }
        ServerSender.sendTranslatedSentence((Player)player, 'f', "ui.wandinstruction", new String[0]);
        return InteractionResult.FAIL;
    }

    public static void villageChiefPerformBuilding(Player player, MillVillager chief, String planKey) {
        BuildingPlan plan = chief.getTownHall().culture.getBuildingPlanSet(planKey).getRandomStartingPlan();
        chief.getTownHall().buildingsBought.add(planKey);
        MillCommonUtilities.changeMoney((Container)player.getInventory(), -plan.price, player);
        ServerSender.sendTranslatedSentence(player, 'f', "ui.housebought", chief.func_70005_c_(), plan.nativeName);
    }

    public static void villageChiefPerformCrop(Player player, MillVillager chief, String value) {
        ResourceLocation itemId = ResourceLocation.tryParse("millenaire:" + value);
        Item crop = itemId == null ? null : net.minecraft.core.Registry.ITEM.getOptional(itemId).orElse(null);
        if (crop == null) return;
        UserProfile profile = Mill.getMillWorld(player.level).getProfile(player);
        profile.setTag("cropplanting_" + value);
        MillCommonUtilities.changeMoney((Container)player.getInventory(), -512, player);
        ServerSender.sendTranslatedSentence(player, 'f', "ui.croplearned", chief.func_70005_c_(), "ui.crop." + crop.getRegistryName().getPath());
    }

    public static void villageChiefPerformCultureControl(Player player, MillVillager chief) {
        UserProfile profile = Mill.getMillWorld(player.level).getProfile(player);
        profile.setTag("culturecontrol_" + chief.getCulture().key);
        ServerSender.sendTranslatedSentence(player, 'f', "ui.control_gotten", chief.func_70005_c_(), chief.getCulture().getAdjectiveTranslatedKey());
    }

    public static void villageChiefPerformDiplomacy(Player player, MillVillager chief, Point village, boolean praise) {
        float effect = 0.0f;
        effect = praise ? 10.0f : -10.0f;
        int reputation = Math.min(chief.getTownHall().getReputation(player), 32768);
        float coeff = (float)((Math.log(reputation) / Math.log(32768.0) * 2.0 + (double)(reputation / 32768)) / 3.0);
        effect *= coeff;
        effect = (float)((double)effect * ((double)(MillCommonUtilities.randomInt(40) + 80) / 100.0));
        chief.getTownHall().adjustRelation(village, (int)effect, false);
        UserProfile profile = Mill.getMillWorld(player.level).getProfile(player);
        profile.adjustDiplomacyPoint(chief.getTownHall(), -1);
        if (MillConfigValues.LogVillage >= 1) {
            MillLog.major(chief.getTownHall(), "Adjusted relation by " + effect + " (coef: " + coeff + ")");
        }
    }

    public static void villageChiefPerformHuntingDrop(Player player, MillVillager chief, String value) {
        ResourceLocation itemId = ResourceLocation.tryParse("millenaire:" + value);
        Item drop = itemId == null ? null : net.minecraft.core.Registry.ITEM.getOptional(itemId).orElse(null);
        if (drop == null) return;
        UserProfile profile = Mill.getMillWorld(player.level).getProfile(player);
        profile.setTag("huntingdrop_" + value);
        MillCommonUtilities.changeMoney((Container)player.getInventory(), -512, player);
        ServerSender.sendTranslatedSentence(player, 'f', "ui.huntingdroplearned", chief.func_70005_c_(), "ui.huntingdrop." + drop.getRegistryName().getPath());
    }

    public static void villageChiefPerformVillageScroll(Player player, MillVillager chief) {
        for (int i = 0; i < Mill.getMillWorld((Level)player.level).villagesList.pos.size(); ++i) {
            Point p = Mill.getMillWorld((Level)player.level).villagesList.pos.get(i);
            if (!chief.getTownHall().getPos().sameBlock(p)) continue;
            MillCommonUtilities.changeMoney((Container)player.getInventory(), -128, player);
            player.getInventory().add(ItemParchment.createParchmentForVillage(chief.getTownHall()));
            ServerSender.sendTranslatedSentence(player, 'f', "ui.scrollbought", chief.func_70005_c_());
        }
    }
}

