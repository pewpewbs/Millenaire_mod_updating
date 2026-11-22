/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockBanner
 *  net.minecraft.block.BlockBed
 *  net.minecraft.block.BlockChest
 *  net.minecraft.block.BlockDoor
 *  net.minecraft.block.BlockDoublePlant
 *  net.minecraft.block.BlockFenceGate
 *  net.minecraft.block.BlockFlowerPot
 *  net.minecraft.block.BlockFlowerPot$EnumFlowerType
 *  net.minecraft.block.BlockLeaves
 *  net.minecraft.block.BlockLiquid
 *  net.minecraft.block.BlockNewLeaf
 *  net.minecraft.block.BlockNewLog
 *  net.minecraft.block.BlockOldLeaf
 *  net.minecraft.block.BlockOldLog
 *  net.minecraft.block.BlockPlanks$EnumType
 *  net.minecraft.block.properties.IProperty
 *  net.minecraft.block.properties.PropertyEnum
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.Blocks
 *  net.minecraft.tileentity.TileEntitySign
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentString
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 */
package org.millenaire.common.buildingplan;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import javax.imageio.ImageIO;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import org.millenaire.common.utilities.LegacyFlowerPots;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import org.millenaire.common.block.WoodVariant;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import org.millenaire.client.network.ClientSender;
import org.millenaire.common.block.IBlockPath;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.block.mock.MockBlockAnimalSpawn;
import org.millenaire.common.block.mock.MockBlockDecor;
import org.millenaire.common.block.mock.MockBlockFree;
import org.millenaire.common.block.mock.MockBlockMarker;
import org.millenaire.common.block.mock.MockBlockSoil;
import org.millenaire.common.block.mock.MockBlockSource;
import org.millenaire.common.block.mock.MockBlockTreeSpawn;
import org.millenaire.common.buildingplan.BuildingDevUtilities;
import org.millenaire.common.buildingplan.BuildingPlan;
import org.millenaire.common.buildingplan.BuildingPlanSet;
import org.millenaire.common.buildingplan.PointType;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.culture.Culture;
import org.millenaire.common.entity.TileEntityImportTable;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.network.ServerSender;
import org.millenaire.common.utilities.BlockItemUtilities;
import org.millenaire.common.utilities.BlockStateUtilities;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;
import org.millenaire.common.utilities.virtualdir.VirtualDir;
import org.millenaire.common.village.BuildingLocation;

public class BuildingImportExport {
    public static String EXPORT_DIR = "exportdir";
    private static HashMap<BlockState, PointType> reverseColourPoints = new HashMap<>();

    public static Point adjustForOrientation(int x, int y, int z, int xoffset, int zoffset, int orientation) {
        Point pos = null;
        if (orientation == 0) {
            pos = new Point(x + xoffset, y, z + zoffset);
        } else if (orientation == 1) {
            pos = new Point(x + zoffset, y, z - xoffset);
        } else if (orientation == 2) {
            pos = new Point(x - xoffset, y, z - zoffset - 1);
        } else if (orientation == 3) {
            pos = new Point(x - zoffset - 1, y, z + xoffset);
        }
        return pos;
    }

    private static void copyPlanSetToExportDir(BuildingPlanSet planSet) {
        File exportDir = MillCommonUtilities.getExportDir();
        Path exportPath = exportDir.toPath();
        Path inputPath = planSet.getFirstStartingPlan().getLoadedFromFile().toPath().getParent();
        try {
            for (int exportVariation = 0; exportVariation < planSet.plans.size(); ++exportVariation) {
                char exportVariationLetter = (char)(65 + exportVariation);
                String txtFileName = planSet.key + "_" + exportVariationLetter + ".txt";
                Files.copy(inputPath.resolve(txtFileName), exportPath.resolve(txtFileName), StandardCopyOption.REPLACE_EXISTING);
                for (int buildingUpgrade = 0; buildingUpgrade < planSet.plans.get(exportVariation).length; ++buildingUpgrade) {
                    String pngFileName = planSet.key + "_" + exportVariationLetter + buildingUpgrade + ".png";
                    Files.copy(inputPath.resolve(pngFileName), exportPath.resolve(pngFileName), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        catch (IOException e) {
            MillLog.printException("Error when copying files to export dir:", e);
        }
    }

    private static void doubleHeightPlan(Player player, BuildingPlanSet existingSet) {
        for (BuildingPlan[] plans : existingSet.plans) {
            for (BuildingPlan plan : plans) {
                PointType[][][] newPlan = new PointType[plan.plan.length * 2][plan.plan[0].length][plan.plan[0][0].length];
                for (int i = 0; i < plan.plan.length; ++i) {
                    for (int j = 0; j < plan.plan[0].length; ++j) {
                        for (int k = 0; k < plan.plan[0][0].length; ++k) {
                            newPlan[i * 2][j][k] = plan.plan[i][j][k];
                            newPlan[i * 2 + 1][j][k] = plan.plan[i][j][k];
                        }
                    }
                }
                plan.plan = newPlan;
                plan.nbfloors *= 2;
            }
        }
        ServerSender.sendTranslatedSentence(player, 'f', "import.doublevertical", new String[0]);
        MillLog.major(null, "Building height: " + existingSet.plans.get((int)0)[0].plan.length);
    }

    private static void drawWoolBorders(Player player, Point tablePos, int orientatedLength, int orientatedWidth) {
        int meta;
        for (int x = 1; x <= orientatedLength; ++x) {
            meta = 0;
            if ((x - 1) % 10 < 5) {
                meta = 14;
            }
            WorldUtilities.setBlockstate(player.level, tablePos.getRelative(x, -1.0, 0.0), org.millenaire.common.utilities.LegacyBlockStates.fromName("minecraft:wool", meta), true, false);
            WorldUtilities.setBlockstate(player.level, tablePos.getRelative(x, -1.0, orientatedWidth + 1), org.millenaire.common.utilities.LegacyBlockStates.fromName("minecraft:wool", meta), true, false);
        }
        for (int z = 1; z <= orientatedWidth; ++z) {
            meta = 0;
            if ((z - 1) % 10 < 5) {
                meta = 14;
            }
            WorldUtilities.setBlockstate(player.level, tablePos.getRelative(0.0, -1.0, z), org.millenaire.common.utilities.LegacyBlockStates.fromName("minecraft:wool", meta), true, false);
            WorldUtilities.setBlockstate(player.level, tablePos.getRelative(orientatedLength + 1, -1.0, z), org.millenaire.common.utilities.LegacyBlockStates.fromName("minecraft:wool", meta), true, false);
        }
        WorldUtilities.setBlockstate(player.level, tablePos.getRelative(0.0, -1.0, 0.0), org.millenaire.common.utilities.LegacyBlockStates.fromName("minecraft:wool", 15), true, false);
        WorldUtilities.setBlockstate(player.level, tablePos.getRelative(orientatedLength + 1, -1.0, 0.0), org.millenaire.common.utilities.LegacyBlockStates.fromName("minecraft:wool", 15), true, false);
        WorldUtilities.setBlockstate(player.level, tablePos.getRelative(0.0, -1.0, orientatedWidth + 1), org.millenaire.common.utilities.LegacyBlockStates.fromName("minecraft:wool", 15), true, false);
        WorldUtilities.setBlockstate(player.level, tablePos.getRelative(orientatedLength + 1, -1.0, orientatedWidth + 1), org.millenaire.common.utilities.LegacyBlockStates.fromName("minecraft:wool", 15), true, false);
    }

    public static int exportBuilding(Level world, Point startPoint, String planName, int variation, int length, int width, int orientation, int upgradeLevel, int startLevel, boolean exportSnow, boolean exportRegularChests, boolean autoconvertToPreserveGround) throws Exception, IOException, UnsupportedEncodingException, FileNotFoundException {
        BuildingImportExport.loadReverseBuildingPoints(autoconvertToPreserveGround, exportRegularChests);
        File exportDir = new File(MillCommonUtilities.getMillenaireCustomContentDir(), "exports");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        char variationLetter = 'A';
        variationLetter = (char)(variationLetter + variation);
        File buildingFile = new File(exportDir, planName + "_" + variationLetter + ".txt");
        PointType[][][] existingPoints = null;
        int existingMinLevel = 0;
        BuildingPlanSet existingSet = null;
        if (buildingFile.exists()) {
            existingSet = BuildingImportExport.loadPlanSetFromExportDir(planName);
            if (existingSet.plans.get((int)variation)[0].length != length) {
                Mill.proxy.localTranslatedSentence(Mill.proxy.getTheSinglePlayer(), '6', "export.errorlength", "" + length, "" + existingSet.plans.get((int)variation)[0].length);
                return upgradeLevel;
            }
            if (existingSet.plans.get((int)variation)[0].width != width) {
                Mill.proxy.localTranslatedSentence(Mill.proxy.getTheSinglePlayer(), '6', "export.errorwidth", "" + width, "" + existingSet.plans.get((int)variation)[0].width);
                return upgradeLevel;
            }
            if (upgradeLevel == -1) {
                upgradeLevel = existingSet.plans.get(variation).length;
            }
            if (existingSet.plans.get((int)variation)[0].parentBuildingPlan != null) {
                String parentBuildingPlanKey = existingSet.plans.get((int)variation)[0].parentBuildingPlan;
                String parentSuffix = parentBuildingPlanKey.split("_")[parentBuildingPlanKey.split("_").length - 1].toUpperCase();
                int parentVariation = parentSuffix.charAt(0) - 65;
                int parentLevel = Integer.parseInt(parentSuffix.substring(1, parentSuffix.length()));
                String parentBuildingKey = parentBuildingPlanKey.substring(0, parentBuildingPlanKey.length() - parentSuffix.length() - 1);
                BuildingPlanSet parentSet = BuildingImportExport.loadPlanSetFromExportDir(parentBuildingKey);
                existingPoints = BuildingImportExport.getConsolidatedPlanWithParent(parentSet, parentVariation, parentLevel, existingSet, variation, upgradeLevel - 1);
                existingMinLevel = Math.min(existingSet.getMinLevel(variation, upgradeLevel - 1), parentSet.getMinLevel(parentVariation, parentLevel));
            } else {
                existingPoints = BuildingImportExport.getConsolidatedPlan(existingSet, variation, upgradeLevel - 1);
                existingMinLevel = existingSet.getMinLevel(variation, upgradeLevel - 1);
            }
        } else {
            upgradeLevel = 0;
        }
        ArrayList<PointType[][]> export = new ArrayList<PointType[][]>();
        boolean stop = false;
        int orientatedLength = length;
        int orientatedWidth = width;
        if (orientation % 2 == 1) {
            orientatedLength = width;
            orientatedWidth = length;
        }
        Point centrePos = startPoint.getRelative(orientatedLength / 2 + 1, 0.0, orientatedWidth / 2 + 1);
        int x = centrePos.getiX();
        int y = centrePos.getiY();
        int z = centrePos.getiZ();
        int lengthOffset = (int)Math.floor((double)length * 0.5);
        int widthOffset = (int)Math.floor((double)width * 0.5);
        int dy = 0;
        while (!stop) {
            PointType[][] level = new PointType[length][width];
            boolean blockFound = false;
            for (int dx = 0; dx < length; ++dx) {
                for (int dz = 0; dz < width; ++dz) {
                    PointType pt;
                    level[dx][dz] = null;
                    Point p = BuildingImportExport.adjustForOrientation(x, y + dy + startLevel, z, dx - lengthOffset, dz - widthOffset, orientation);
                    BlockState state = world.getBlockState(p.getBlockPos());
                    for (int turn = 0; turn < Math.floorMod(orientation, 4); turn++)
                        state = state.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
                    Block block = state.getBlock();
                    if (!state.isAir()) blockFound = true;
                    else state = Blocks.AIR.defaultBlockState();
                    if ((pt = reverseColourPoints.get(BuildingImportExport.getPointHash(state))) != null) {
                        if (!exportSnow && pt.getBlock() == Blocks.SNOW) continue;
                        PointType existing = null;
                        if (existingPoints != null && dy + startLevel >= existingMinLevel && dy + startLevel < existingMinLevel + existingPoints.length && (existing = existingPoints[dy + startLevel - existingMinLevel][dx][dz]) == null) {
                            MillLog.major(null, "Existing pixel is null");
                        }
                        if (existing == null) {
                            if (pt.specialType == null && pt.getBlock() == Blocks.AIR && upgradeLevel == 0) continue;
                            level[dx][dz] = pt;
                            continue;
                        }
                        if (existing == pt || existing.isType("empty") && pt.getBlock() == Blocks.AIR) continue;
                        level[dx][dz] = pt;
                        continue;
                    }
                    if (block instanceof BedBlock || block instanceof DoublePlantBlock || block instanceof LiquidBlock) continue;
                    Mill.proxy.localTranslatedSentence(Mill.proxy.getTheSinglePlayer(), '6', "export.errorunknownblockid", state.toString());
                }
            }
            if (blockFound || existingPoints != null && export.size() < existingPoints.length) {
                export.add(level);
            } else {
                stop = true;
            }
            if (++dy + startPoint.getiY() + startLevel < 256) continue;
            stop = true;
        }
        BufferedImage pict = new BufferedImage(export.size() * width + export.size() - 1, length, 1);
        Graphics2D graphics = pict.createGraphics();
        graphics.setColor(new Color(11730865));
        graphics.fillRect(0, 0, pict.getWidth(), pict.getHeight());
        for (dy = 0; dy < export.size(); ++dy) {
            PointType[][] level = (PointType[][])export.get(dy);
            for (int i = 0; i < length; ++i) {
                for (int k = 0; k < width; ++k) {
                    int colour = 0xFFFFFF;
                    PointType pt = level[i][k];
                    if (pt != null) {
                        colour = pt.colour;
                    }
                    graphics.setColor(new Color(colour));
                    graphics.fillRect(dy * width + dy + width - k - 1, i, 1, 1);
                }
            }
        }
        String fileName = planName + "_" + variationLetter + upgradeLevel + ".png";
        ImageIO.write((RenderedImage)pict, "png", new File(exportDir, fileName));
        if (upgradeLevel == 0 && existingSet == null) {
            BufferedWriter writer = MillCommonUtilities.getWriter(new File(exportDir, planName + "_" + variationLetter + ".txt"));
            writer.write("building.length=" + length + "\n");
            writer.write("building.width=" + width + "\n");
            writer.write("\n");
            writer.write("initial.startlevel=" + startLevel + "\n");
            writer.write("initial.nativename=" + planName + "\n");
            writer.close();
        } else if (upgradeLevel > existingSet.plans.get(variation).length) {
            BufferedWriter writer = MillCommonUtilities.getAppendWriter(new File(exportDir, planName + "_" + variationLetter + ".txt"));
            writer.write("upgrade" + upgradeLevel + ".startlevel=" + startLevel + "\n");
            writer.close();
        }
        Mill.proxy.localTranslatedSentence(Mill.proxy.getTheSinglePlayer(), 'f', "export.buildingexported", fileName);
        return upgradeLevel;
    }

    private static PointType[][][] getConsolidatedPlan(BuildingPlanSet planSet, int variation, int upgradeLevel) {
        int minLevel = planSet.getMinLevel(variation, upgradeLevel);
        int maxLevel = planSet.getMaxLevel(variation, upgradeLevel);
        int length = planSet.plans.get((int)variation)[0].plan[0].length;
        int width = planSet.plans.get((int)variation)[0].plan[0][0].length;
        PointType[][][] consolidatedPlan = new PointType[maxLevel - minLevel][length][width];
        for (int lid = 0; lid <= upgradeLevel; ++lid) {
            BuildingPlan plan = planSet.plans.get(variation)[lid];
            if (MillConfigValues.LogBuildingPlan >= 1) {
                MillLog.major(planSet, "Consolidating plan: adding level " + lid);
            }
            int ioffset = plan.startLevel - minLevel;
            for (int i = 0; i < plan.plan.length; ++i) {
                for (int j = 0; j < length; ++j) {
                    for (int k = 0; k < width; ++k) {
                        PointType pt = plan.plan[i][j][k];
                        if (pt.isType("empty") && lid != 0) continue;
                        consolidatedPlan[i + ioffset][j][k] = pt;
                    }
                }
            }
        }
        return consolidatedPlan;
    }

    private static PointType[][][] getConsolidatedPlanWithParent(BuildingPlanSet parentSet, int parentVariation, int parentUpgradeLevel, BuildingPlanSet planSet, int variation, int upgradeLevel) {
        int minLevel = Math.min(planSet.getMinLevel(variation, upgradeLevel), parentSet.getMinLevel(parentVariation, parentUpgradeLevel));
        int maxLevel = Math.max(planSet.getMaxLevel(variation, upgradeLevel), parentSet.getMaxLevel(parentVariation, parentUpgradeLevel));
        int length = planSet.plans.get((int)variation)[0].plan[0].length;
        int width = planSet.plans.get((int)variation)[0].plan[0][0].length;
        PointType[][][] consolidatedPlan = new PointType[maxLevel - minLevel][length][width];
        for (int lid = 0; lid <= parentUpgradeLevel; ++lid) {
            BuildingPlan plan = parentSet.plans.get(parentVariation)[lid];
            if (MillConfigValues.LogBuildingPlan >= 1) {
                MillLog.major(parentSet, "Consolidating plan: adding level " + lid);
            }
            int ioffset = plan.startLevel - minLevel;
            for (int i = 0; i < plan.plan.length; ++i) {
                for (int j = 0; j < length; ++j) {
                    for (int k = 0; k < width; ++k) {
                        PointType pt = plan.plan[i][j][k];
                        if (pt.isType("empty") && lid != 0) continue;
                        consolidatedPlan[i + ioffset][j][k] = pt;
                    }
                }
            }
        }
        PointType airPt = reverseColourPoints.get(BuildingImportExport.getPointHash(Blocks.AIR, 0));
        if (parentSet.getMaxLevel(parentVariation, parentUpgradeLevel) < planSet.getMaxLevel(variation, upgradeLevel)) {
            for (int i = parentSet.getMaxLevel(parentVariation, parentUpgradeLevel); i <= planSet.getMaxLevel(variation, upgradeLevel); ++i) {
                for (int j = 0; j < length; ++j) {
                    for (int k = 0; k < width; ++k) {
                        consolidatedPlan[i][j][k] = airPt;
                    }
                }
            }
        }
        for (int lid = 0; lid <= upgradeLevel; ++lid) {
            BuildingPlan plan = planSet.plans.get(variation)[lid];
            if (MillConfigValues.LogBuildingPlan >= 1) {
                MillLog.major(planSet, "Consolidating plan: adding level " + lid);
            }
            int ioffset = plan.startLevel - minLevel;
            for (int i = 0; i < plan.plan.length; ++i) {
                for (int j = 0; j < length; ++j) {
                    for (int k = 0; k < width; ++k) {
                        PointType pt = plan.plan[i][j][k];
                        if (pt.isType("empty")) continue;
                        consolidatedPlan[i + ioffset][j][k] = pt;
                    }
                }
            }
        }
        return consolidatedPlan;
    }

    private static BlockState getPointHash(Block block, int meta) {
        return org.millenaire.common.utilities.LegacyWorldMetadata.write(block, meta);
    }

    private static BlockState getPointHash(BlockState state) {
        return state;
    }

    private static PointType getPointTypeFromBlockState(BlockState blockState) {
        for (PointType newPt : PointType.colourPoints.values()) {
            if (newPt.getBlockState() == null || !newPt.getBlockState().equals(blockState)) continue;
            return newPt;
        }
        return null;
    }

    public static void importTableCreateNewBuilding(Player player, TileEntityImportTable importTable, int length, int width, int startLevel, boolean clearGround) {
        File exportDir = MillCommonUtilities.getExportDir();
        VirtualDir exportVirtualDir = new VirtualDir(exportDir);
        int exportNumber = 1;
        while (exportVirtualDir.getChildFile("export" + exportNumber + "_A.txt") != null) {
            ++exportNumber;
        }
        if (clearGround) {
            for (int x = importTable.getBlockPos().getX(); x < importTable.getBlockPos().getX() + 2 + length; ++x) {
                for (int z = importTable.getBlockPos().getZ(); z < importTable.getBlockPos().getZ() + 2 + width; ++z) {
                    int y;
                    int startingY = Math.min(0, startLevel);
                    for (y = importTable.getBlockPos().getY() + startingY; y < importTable.getBlockPos().getY(); ++y) {
                        player.level.setBlockAndUpdate(new BlockPos(x, y, z), MillBlocks.MARKER_BLOCK.defaultBlockState().setValue(MockBlockMarker.VARIANT, (MockBlockMarker.Type.PRESERVE_GROUND)));
                    }
                    for (y = importTable.getBlockPos().getY(); y < importTable.getBlockPos().getY() + 50; ++y) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (pos.equals((Object)importTable.getBlockPos())) continue;
                        player.level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        BuildingImportExport.drawWoolBorders(player, importTable.getPosPoint(), length, width);
        importTable.updatePlan("export" + exportNumber, length, width, 0, 0, startLevel, player);
    }

    public static void importTableExportBuildingPlan(Level world, TileEntityImportTable importTable, int level) {
        if (importTable.getOrientation() != 0) {
            Mill.proxy.localTranslatedSentence(Mill.proxy.getTheSinglePlayer(), '6', "export.northfacingonly", new String[0]);
            return;
        }
        try {
            int upgradeLevelExported = BuildingImportExport.exportBuilding(world, new Point(importTable.getBlockPos()), importTable.getBuildingKey(), importTable.getVariation(), importTable.getLength(), importTable.getWidth(), importTable.getOrientation(), level, importTable.getStartingLevel(), importTable.exportSnow(), importTable.exportRegularChests(), importTable.autoconvertToPreserveGround());
            if (upgradeLevelExported != level) {
                ClientSender.importTableUpdateSettings(new Point(importTable.getBlockPos()), upgradeLevelExported, importTable.getOrientation(), importTable.getStartingLevel(), importTable.exportSnow(), importTable.importMockBlocks(), importTable.autoconvertToPreserveGround(), importTable.exportRegularChests());
            }
        }
        catch (Exception e) {
            MillLog.printException("Error when exporting building:", e);
        }
    }

    public static void importTableExportPlanCost(String buildingKey) {
        BuildingPlanSet set = BuildingImportExport.loadPlanSetFromExportDir(buildingKey);
        File file = new File(MillCommonUtilities.getExportDir(), set.key + " resources used.txt");
        try {
            BufferedWriter writer = MillCommonUtilities.getWriter(file);
            BuildingDevUtilities.writePlanCostTextStyle(set, writer);
            writer.close();
            Mill.proxy.localTranslatedSentence(Mill.proxy.getTheSinglePlayer(), 'f', "importtable.costexported", "export/" + file.getName());
        }
        catch (IOException e) {
            MillLog.printException(e);
        }
    }

    public static int importTableHandleImportRequest(Player player, Point tablePos, String source, String buildingKey, boolean importAll, int variation, int level, int orientation, boolean importMockBlocks) {
        BuildingPlanSet importSet = null;
        if (source.equals(EXPORT_DIR)) {
            importSet = BuildingImportExport.loadPlanSetFromExportDir(buildingKey);
        } else {
            importSet = Culture.getCultureByName(source).getBuildingPlanSet(buildingKey);
            BuildingImportExport.copyPlanSetToExportDir(importSet);
            if (importSet.getFirstStartingPlan().parentBuildingPlan != null) {
                String parentBuildingPlanKey = importSet.getFirstStartingPlan().parentBuildingPlan;
                String parentSuffix = parentBuildingPlanKey.split("_")[parentBuildingPlanKey.split("_").length - 1].toUpperCase();
                String parentBuildingKey = parentBuildingPlanKey.substring(0, parentBuildingPlanKey.length() - parentSuffix.length() - 1);
                BuildingImportExport.copyPlanSetToExportDir(Culture.getCultureByName(source).getBuildingPlanSet(parentBuildingKey));
            }
        }
        if (importSet != null) {
            if (!importAll) {
                BuildingImportExport.importTableImportBuilding(player, tablePos, null, importSet, variation, level, orientation, importMockBlocks);
                return importSet.plans.get((int)variation)[0].length + 2 + importSet.plans.get((int)variation)[0].areaToClear;
            }
            int xDelta = 0;
            for (int aVariation = 0; aVariation < importSet.plans.size(); ++aVariation) {
                BuildingPlan basePlan = importSet.plans.get(aVariation)[0];
                int orientatedLength = basePlan.length;
                int orientatedWidth = basePlan.width;
                int orientedGapLength = basePlan.areaToClearLengthAfter + basePlan.areaToClearLengthBefore;
                int orientedGapWidth = basePlan.areaToClearWidthAfter + basePlan.areaToClearWidthBefore;
                if (orientation % 2 == 1) {
                    orientatedLength = basePlan.width;
                    orientatedWidth = basePlan.length;
                    orientedGapLength = basePlan.areaToClearWidthAfter + basePlan.areaToClearWidthBefore;
                    orientedGapWidth = basePlan.areaToClearLengthAfter + basePlan.areaToClearLengthBefore;
                }
                int zDelta = 0;
                for (int aLevel = 0; aLevel < importSet.plans.get(aVariation).length; ++aLevel) {
                    BuildingImportExport.importTableImportBuilding(player, tablePos.getRelative(xDelta, 0.0, zDelta), tablePos, importSet, aVariation, aLevel, orientation, importMockBlocks);
                    zDelta += orientatedWidth + 2 + orientedGapWidth;
                }
                xDelta += orientatedLength + 2 + orientedGapLength;
            }
            return xDelta;
        }
        return 0;
    }

    public static void importTableImportBuilding(Player player, Point tablePos, Point parentTablePos, BuildingPlanSet planSet, int variation, int maxLevel, int orientation, boolean importMockBlocks) {
        BuildingPlan basePlan = planSet.getPlan(variation, 0);
        int orientatedLength = basePlan.length;
        int orientatedWidth = basePlan.width;
        if (orientation % 2 == 1) {
            orientatedLength = basePlan.width;
            orientatedWidth = basePlan.length;
        }
        if (basePlan.parentBuildingPlan != null) {
            String parentBuildingPlanKey = basePlan.parentBuildingPlan;
            String parentSuffix = parentBuildingPlanKey.split("_")[parentBuildingPlanKey.split("_").length - 1].toUpperCase();
            int parentVariation = parentSuffix.charAt(0) - 65;
            int parentLevel = Integer.parseInt(parentSuffix.substring(1, parentSuffix.length()));
            String parentBuildingKey = parentBuildingPlanKey.substring(0, parentBuildingPlanKey.length() - parentSuffix.length() - 1);
            BuildingPlanSet parentBuildingSet = BuildingImportExport.loadPlanSetFromExportDir(parentBuildingKey);
            BuildingLocation parentLocation = new BuildingLocation(parentBuildingSet.getPlan(parentVariation, parentLevel), tablePos.getRelative(orientatedLength / 2 + 1, 0.0, orientatedWidth / 2 + 1), orientation);
            for (int level = 0; level <= parentLevel; ++level) {
                parentLocation.level = level;
                parentBuildingSet.buildLocation(Mill.getMillWorld(player.level), null, parentLocation, !importMockBlocks, false, null, true, importMockBlocks, null);
            }
        }
        BuildingLocation location = new BuildingLocation(basePlan, tablePos.getRelative(orientatedLength / 2 + 1, 0.0, orientatedWidth / 2 + 1), orientation);
        int level = 0;
        while (level <= maxLevel) {
            location.level = level++;
            planSet.buildLocation(Mill.getMillWorld(player.level), null, location, true, false, null, true, importMockBlocks, null);
        }
        BuildingImportExport.drawWoolBorders(player, tablePos, orientatedLength, orientatedWidth);
        TileEntityImportTable table = tablePos.getImportTable(player.level);
        if (table == null) {
            tablePos.setBlock(player.level, MillBlocks.IMPORT_TABLE, 0, true, false);
            table = tablePos.getImportTable(player.level);
        }
        if (table == null) {
            MillLog.error(null, "Can neither find nor create import table at location: " + tablePos);
        } else {
            BuildingPlan plan = planSet.getPlan(variation, maxLevel);
            table.updatePlan(planSet.key, plan.length, plan.width, variation, maxLevel, plan.startLevel, player);
            table.setParentTablePos(parentTablePos);
        }
        ServerSender.sendTranslatedSentence(player, 'f', "importtable.importedbuildingplan", planSet.getPlan((int)variation, (int)maxLevel).planName);
    }

    private static BuildingPlanSet loadPlanSetFromExportDir(String parentBuildingKey) {
        File exportDir = MillCommonUtilities.getExportDir();
        VirtualDir exportVirtualDir = new VirtualDir(exportDir);
        File parentBuildingFile = new File(exportDir, parentBuildingKey + "_A.txt");
        BuildingPlanSet parentBuildingSet = new BuildingPlanSet(null, parentBuildingKey, exportVirtualDir, parentBuildingFile);
        try {
            parentBuildingSet.loadPictPlans(true);
        }
        catch (Exception e) {
            MillLog.printException("Exception when loading plan:", e);
        }
        return parentBuildingSet;
    }

    public static void loadReverseBuildingPoints(Boolean exportPreserveGround, Boolean exportRegularChests) {
        reverseColourPoints.clear();
        for (PointType pt : PointType.colourPoints.values()) {
            if (pt.specialType != null) continue;
            Block block = pt.getBlock();
            if (block == null) {
                MillLog.error(pt, "PointType has neither name nor block.");
                continue;
            }
            if (block instanceof FlowerPotBlock) {
                reverseColourPoints.put(getPointHash(Blocks.FLOWER_POT, LegacyFlowerPots.toPlan(LegacyFlowerPots.fromPlan(pt.getMeta()).getBlock())), pt);
                continue;
            }
            reverseColourPoints.put(BuildingImportExport.getPointHash(pt.getBlockState()), pt);
            if (block instanceof LeavesBlock) {
                for (boolean persistent : new boolean[]{false, true})
                    for (int distance = 1; distance <= 7; distance++)
                        reverseColourPoints.put(pt.getBlockState().setValue(LeavesBlock.PERSISTENT, persistent)
                            .setValue(LeavesBlock.DISTANCE, distance), pt);
                continue;
            }
            if (BlockItemUtilities.isPath(block)) {
                reverseColourPoints.put(BuildingImportExport.getPointHash(pt.getBlockState().setValue(IBlockPath.STABLE, Boolean.valueOf(false))), pt);
                continue;
            }
            if (pt.getBlock() == MillBlocks.PANEL) {
                reverseColourPoints.put(BuildingImportExport.getPointHash(Blocks.OAK_WALL_SIGN, pt.getMeta()), pt);
                continue;
            }
            if (pt.getBlock() instanceof DoorBlock) {
                reverseColourPoints.put(BuildingImportExport.getPointHash(pt.getBlockState()), pt);
                reverseColourPoints.put(BuildingImportExport.getPointHash(pt.getBlockState().setValue(DoorBlock.OPEN, Boolean.valueOf(true))), pt);
                continue;
            }
            if (pt.getBlock() instanceof FenceGateBlock) {
                reverseColourPoints.put(BuildingImportExport.getPointHash(pt.getBlockState()), pt);
                reverseColourPoints.put(BuildingImportExport.getPointHash(pt.getBlockState().setValue(FenceGateBlock.OPEN, Boolean.valueOf(true))), pt);
                continue;
            }
            if (pt.getBlock() == Blocks.FURNACE) {
                reverseColourPoints.put(BuildingImportExport.getPointHash(pt.getBlockState()), pt);
                BlockState litBlockState = pt.getBlockState().setValue(net.minecraft.world.level.block.FurnaceBlock.LIT, true);
                reverseColourPoints.put(BuildingImportExport.getPointHash(litBlockState), pt);
                continue;
            }
            
        }
        for (PointType pt : PointType.colourPoints.values()) {
            BlockState bs;
            if (pt.specialType == null) continue;
            if (pt.specialType.equals("brewingstand")) {
                for (int i = 0; i < 16; ++i) {
                    reverseColourPoints.put(BuildingImportExport.getPointHash(Blocks.BREWING_STAND, i), pt);
                }
                continue;
            }
            if (pt.specialType.equals("lockedchestTop")) {
                BlockState bs2 = MillBlocks.LOCKED_CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST);
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs2), pt);
                if (exportRegularChests.booleanValue()) continue;
                bs2 = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST);
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs2), pt);
                continue;
            }
            if (pt.specialType.equals("lockedchestBottom")) {
                BlockState bs3 = MillBlocks.LOCKED_CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST);
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs3), pt);
                if (exportRegularChests.booleanValue()) continue;
                bs3 = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST);
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs3), pt);
                continue;
            }
            if (pt.specialType.equals("lockedchestLeft")) {
                BlockState bs4 = MillBlocks.LOCKED_CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH);
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs4), pt);
                if (exportRegularChests.booleanValue()) continue;
                bs4 = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH);
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs4), pt);
                continue;
            }
            if (pt.specialType.equals("lockedchestRight")) {
                BlockState bs5 = MillBlocks.LOCKED_CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH);
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs5), pt);
                if (exportRegularChests.booleanValue()) continue;
                bs5 = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH);
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs5), pt);
                continue;
            }
            if (pt.specialType.equals("mainchestTop")) {
                BlockState bs6 = MillBlocks.MAIN_CHEST.func_176203_a(Direction.WEST.get3DDataValue());
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs6), pt);
                continue;
            }
            if (pt.specialType.equals("mainchestBottom")) {
                BlockState bs7 = MillBlocks.MAIN_CHEST.func_176203_a(Direction.EAST.get3DDataValue());
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs7), pt);
                continue;
            }
            if (pt.specialType.equals("mainchestLeft")) {
                BlockState bs8 = MillBlocks.MAIN_CHEST.func_176203_a(Direction.SOUTH.get3DDataValue());
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs8), pt);
                continue;
            }
            if (pt.specialType.equals("mainchestRight")) {
                BlockState bs9 = MillBlocks.MAIN_CHEST.func_176203_a(Direction.NORTH.get3DDataValue());
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs9), pt);
                continue;
            }
            if (pt.specialType.equals("grass") && !exportPreserveGround.booleanValue()) {
                BlockState bs10 = Blocks.GRASS_BLOCK.defaultBlockState();
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs10), pt);
                continue;
            }
            if (pt.isSubType("villageBannerWall")) {
                String facing = pt.specialType.substring(17);
                BlockState bs11 = MillBlocks.VILLAGE_BANNER_WALL.defaultBlockState().setValue(net.minecraft.world.level.block.WallBannerBlock.FACING, Direction.byName((String)facing));
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs11), pt);
                continue;
            }
            if (pt.isSubType("villageBannerStanding")) {
                int rotation = Integer.parseInt(pt.specialType.substring(21));
                BlockState bs12 = MillBlocks.VILLAGE_BANNER_STANDING.func_176203_a(rotation);
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs12), pt);
                continue;
            }
            if (pt.isSubType("cultureBannerWall")) {
                String facing = pt.specialType.substring(17);
                BlockState bs13 = MillBlocks.CULTURE_BANNER_WALL.defaultBlockState().setValue(net.minecraft.world.level.block.WallBannerBlock.FACING, Direction.byName((String)facing));
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs13), pt);
                continue;
            }
            if (pt.isSubType("cultureBannerStanding")) {
                int rotation = Integer.parseInt(pt.specialType.substring(21));
                BlockState bs14 = MillBlocks.CULTURE_BANNER_STANDING.func_176203_a(rotation);
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs14), pt);
                continue;
            }
            for (MockBlockMarker.Type type : MockBlockMarker.Type.values()) {
                if (!type.name.equalsIgnoreCase(pt.specialType)) continue;
                bs = MillBlocks.MARKER_BLOCK.func_176203_a(type.meta);
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs), pt);
            }
            if (exportPreserveGround.booleanValue() && "preserveground".equalsIgnoreCase(pt.specialType)) {
                reverseColourPoints.put(BuildingImportExport.getPointHash((Block)Blocks.GRASS_BLOCK, 0), pt);
                reverseColourPoints.put(BuildingImportExport.getPointHash((Block)Blocks.SAND, 0), pt);
            }
            for (Enum enum_ : MockBlockAnimalSpawn.Creature.values()) {
                if (!pt.specialType.equalsIgnoreCase(((MockBlockAnimalSpawn.Creature)enum_).name + "spawn")) continue;
                bs = MillBlocks.ANIMAL_SPAWN.func_176203_a(((MockBlockAnimalSpawn.Creature)enum_).meta);
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs), pt);
            }
            for (Enum enum_ : MockBlockSource.Resource.values()) {
                if (!pt.specialType.equalsIgnoreCase(((MockBlockSource.Resource)enum_).name + "source")) continue;
                bs = MillBlocks.SOURCE.func_176203_a(((MockBlockSource.Resource)enum_).meta);
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs), pt);
            }
            for (Enum enum_ : MockBlockFree.Resource.values()) {
                if (!pt.specialType.equalsIgnoreCase("free" + ((MockBlockFree.Resource)enum_).name)) continue;
                bs = MillBlocks.FREE_BLOCK.func_176203_a(((MockBlockFree.Resource)enum_).meta);
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs), pt);
            }
            for (Enum enum_ : MockBlockTreeSpawn.TreeType.values()) {
                if (!pt.specialType.equalsIgnoreCase(((MockBlockTreeSpawn.TreeType)enum_).name + "spawn")) continue;
                bs = MillBlocks.TREE_SPAWN.func_176203_a(((MockBlockTreeSpawn.TreeType)enum_).meta);
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs), pt);
            }
            for (Enum enum_ : MockBlockSoil.CropType.values()) {
                if (!pt.specialType.equalsIgnoreCase(((MockBlockSoil.CropType)enum_).name)) continue;
                bs = MillBlocks.SOIL_BLOCK.func_176203_a(((MockBlockSoil.CropType)enum_).meta);
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs), pt);
            }
            for (Enum enum_ : MockBlockDecor.Type.values()) {
                if (!pt.specialType.equalsIgnoreCase(((MockBlockDecor.Type)enum_).name)) continue;
                bs = MillBlocks.DECOR_BLOCK.func_176203_a(((MockBlockDecor.Type)enum_).meta);
                reverseColourPoints.put(BuildingImportExport.getPointHash(bs), pt);
            }
        }
    }

    private static BlockState mirrorBlock(PointType pt, boolean horizontal) {
        Comparable rawFacingValue = BlockStateUtilities.getPropertyValueByName(pt.getBlockState(), "facing");
        if (rawFacingValue != null && rawFacingValue instanceof Direction) {
            Direction facing = (Direction)rawFacingValue;
            if (horizontal) {
                if (facing == Direction.EAST) {
                    facing = Direction.WEST;
                } else if (facing == Direction.WEST) {
                    facing = Direction.EAST;
                }
            } else if (facing == Direction.NORTH) {
                facing = Direction.SOUTH;
            } else if (facing == Direction.SOUTH) {
                facing = Direction.NORTH;
            }
            BlockState adjustedBlockState = BlockStateUtilities.setPropertyValueByName(pt.getBlockState(), "facing", facing);
            return adjustedBlockState;
        }
        return null;
    }

    private static void mirrorPlan(BuildingPlanSet existingSet, boolean horizontalmirror) {
        for (BuildingPlan[] plans : existingSet.plans) {
            for (BuildingPlan plan : plans) {
                int planLength = plan.plan[0].length;
                int planWidth = plan.plan[0][0].length;
                for (int floorPos = 0; floorPos < plan.plan.length; ++floorPos) {
                    for (int lengthPos = 0; lengthPos < plan.plan[0].length; ++lengthPos) {
                        for (int widthPos = 0; widthPos < plan.plan[0][0].length; ++widthPos) {
                            int newWidthPos;
                            int newLengthPos;
                            if (horizontalmirror) {
                                newLengthPos = planLength - lengthPos - 1;
                                newWidthPos = widthPos;
                            } else {
                                newLengthPos = lengthPos;
                                newWidthPos = planWidth - widthPos - 1;
                            }
                            if (plan.plan[floorPos][lengthPos][widthPos].isType("empty")) continue;
                            if (plan.plan[floorPos][lengthPos][widthPos].specialType != null) {
                                plan.plan[floorPos][newLengthPos][newWidthPos] = plan.plan[floorPos][lengthPos][widthPos];
                                continue;
                            }
                            BlockState blockState = BuildingImportExport.mirrorBlock(plan.plan[floorPos][lengthPos][widthPos], horizontalmirror);
                            if (blockState != null) {
                                PointType newPt = BuildingImportExport.getPointTypeFromBlockState(blockState);
                                if (newPt == null) continue;
                                plan.plan[floorPos][lengthPos][widthPos] = newPt;
                                continue;
                            }
                            plan.plan[floorPos][newLengthPos][newWidthPos] = plan.plan[floorPos][lengthPos][widthPos];
                        }
                    }
                }
            }
        }
    }

    public static void negationWandExportBuilding(Player player, Level world, Point startPoint) {
        try {
            int zEnd;
            int xEnd;
            SignBlockEntity sign = startPoint.getSign(world);
            if (sign == null) {
                return;
            }
            if (sign.getMessage(0, false) == null || sign.getMessage(0, false).getString().length() == 0) {
                Mill.proxy.localTranslatedSentence(Mill.proxy.getTheSinglePlayer(), '6', "export.errornoname", new String[0]);
                return;
            }
            String planName = sign.getMessage(0, false).getString().toLowerCase();
            int variation = 0;
            for (int letter = 0; letter < 26; ++letter) {
                if (!planName.endsWith("_" + (char)(97 + letter))) continue;
                planName = planName.substring(0, planName.length() - 2);
                variation = letter;
            }
            int upgradeLevel = -1;
            if (sign.getMessage(1, false) != null && sign.getMessage(1, false).getString().length() > 0) {
                try {
                    upgradeLevel = Integer.parseInt(sign.getMessage(1, false).getString());
                }
                catch (Exception e) {
                    ServerSender.sendTranslatedSentence(player, '6', "export.errorinvalidupgradelevel", new String[0]);
                    return;
                }
            }
            boolean found = false;
            for (xEnd = startPoint.getiX() + 1; !found && xEnd < startPoint.getiX() + 257; ++xEnd) {
                Block block = WorldUtilities.getBlock(world, xEnd, startPoint.getiY(), startPoint.getiZ());
                if (!(block instanceof net.minecraft.world.level.block.StandingSignBlock)) continue;
                found = true;
                break;
            }
            if (!found) {
                Mill.proxy.localTranslatedSentence(Mill.proxy.getTheSinglePlayer(), '6', "export.errornoendsigneast", new String[0]);
                return;
            }
            found = false;
            for (zEnd = startPoint.getiZ() + 1; !found && zEnd < startPoint.getiZ() + 257; ++zEnd) {
                Block block = WorldUtilities.getBlock(world, startPoint.getiX(), startPoint.getiY(), zEnd);
                if (!(block instanceof net.minecraft.world.level.block.StandingSignBlock)) continue;
                found = true;
                break;
            }
            if (!found) {
                Mill.proxy.localTranslatedSentence(Mill.proxy.getTheSinglePlayer(), '6', "export.errornoendsignsouth", new String[0]);
                return;
            }
            int startLevel = -1;
            if (sign.getMessage(2, false) != null && sign.getMessage(2, false).getString().length() > 0) {
                try {
                    startLevel = Integer.parseInt(sign.getMessage(2, false).getString());
                }
                catch (Exception e) {
                    Mill.proxy.localTranslatedSentence(Mill.proxy.getTheSinglePlayer(), '6', "export.errorstartinglevel", new String[0]);
                }
            } else {
                Mill.proxy.localTranslatedSentence(Mill.proxy.getTheSinglePlayer(), 'f', "export.defaultstartinglevel", new String[0]);
            }
            boolean exportSnow = false;
            if (sign.getMessage(3, false) != null && sign.getMessage(3, false).getString().equals("snow")) {
                exportSnow = true;
            }
            int length = xEnd - startPoint.getiX() - 1;
            int width = zEnd - startPoint.getiZ() - 1;
            int orientation = 0;
            if (sign.getMessage(3, false) != null && sign.getMessage(3, false).getString().startsWith("or:")) {
                String orientationString = sign.getMessage(3, false).getString().substring(3, sign.getMessage(3, false).getString().length());
                orientation = Integer.parseInt(orientationString);
            }
            if (orientation != 0) {
                Mill.proxy.localTranslatedSentence(Mill.proxy.getTheSinglePlayer(), '6', "export.northfacingonly", new String[0]);
                return;
            }
            BuildingImportExport.exportBuilding(world, startPoint, planName, variation, length, width, orientation, upgradeLevel, startLevel, exportSnow, false, true);
        }
        catch (Exception e) {
            MillLog.printException("Error when trying to store a building: ", e);
        }
    }

    private static void replaceWoodType(BuildingPlanSet existingSet, WoodVariant newWoodType) {
        for (BuildingPlan[] plans : existingSet.plans) {
            for (BuildingPlan plan : plans) {
                for (PointType[][] floor : plan.plan) {
                    for (PointType[] row : floor) {
                        for (int i = 0; i < row.length; i++) {
                            PointType point = row[i];
                            if (point == null || point.getBlockState() == null) continue;
                            BlockState previous = point.getBlockState();
                            BlockState replacement = newWoodType.replace(previous);
                            if (replacement == previous) continue;
                            PointType updated = getPointTypeFromBlockState(replacement);
                            if (updated != null) row[i] = updated;
                        }
                    }
                }
            }
        }
    }

    public static void summoningWandImportBuildingPlan(Player player, Level world, Point startPoint, int variation, BuildingPlanSet existingSet, int upgradeLevel, boolean includeSpecialPoints, int orientation, boolean createSign) {
        SignBlockEntity sign;
        BuildingPlan basePlan = existingSet.plans.get(variation)[0];
        BuildingPlan upgradePlan = existingSet.plans.get(variation)[upgradeLevel];
        if (createSign) {
            Point startingSign = startPoint;
            startingSign.setBlock(world, Blocks.OAK_SIGN, 0, true, false);
            sign = startingSign.getSign(world);
            char variationLetter = (char)(65 + variation);
            sign.setMessage(0, new TextComponent(existingSet.key + "_" + variationLetter));
            sign.setMessage(1, new TextComponent("" + upgradeLevel));
            sign.setMessage(2, new TextComponent("" + upgradePlan.startLevel));
            sign.setMessage(3, !includeSpecialPoints ? new TextComponent("nomock") : (orientation > 0 ? new TextComponent("or:" + orientation) : new TextComponent("")));
        }
        int orientatedLength = basePlan.length;
        int orientatedWidth = basePlan.width;
        if (orientation % 2 == 1) {
            orientatedLength = basePlan.width;
            orientatedWidth = basePlan.length;
        }
        BuildingLocation location = new BuildingLocation(basePlan, startPoint.getRelative(orientatedLength / 2 + 1, 0.0, orientatedWidth / 2 + 1), orientation);
        for (int i = 0; i <= upgradeLevel; ++i) {
            if (player != null) {
                ServerSender.sendTranslatedSentence(player, 'f', "import.buildinglevel", "" + i);
            }
            existingSet.buildLocation(Mill.getMillWorld(world), null, location, !includeSpecialPoints, false, null, true, includeSpecialPoints, null);
            ++location.level;
        }
        Point eastSign = startPoint.getRelative(orientatedLength + 1, 0.0, 0.0);
        eastSign.setBlock(world, Blocks.OAK_SIGN, 0, true, false);
        sign = eastSign.getSign(world);
        sign.setMessage(0, new TextComponent("East End"));
        sign.setMessage(1, new TextComponent("(length)"));
        sign.setMessage(2, new TextComponent(""));
        sign.setMessage(3, new TextComponent(""));
        Point southSign = startPoint.getRelative(0.0, 0.0, orientatedWidth + 1);
        southSign.setBlock(world, Blocks.OAK_SIGN, 0, true, false);
        sign = southSign.getSign(world);
        sign.setMessage(0, new TextComponent("South End"));
        sign.setMessage(1, new TextComponent("(width)"));
        sign.setMessage(2, new TextComponent(""));
        sign.setMessage(3, new TextComponent(""));
    }

    public static void summoningWandImportBuildingRequest(Player player, Level world, Point startPoint) {
        try {
            SignBlockEntity sign = startPoint.getSign(world);
            if (sign == null) {
                return;
            }
            if (sign.getMessage(0, false) == null || sign.getMessage(0, false).getString().length() == 0) {
                ServerSender.sendTranslatedSentence(player, '6', "import.errornoname", new String[0]);
                return;
            }
            String buildingKey = sign.getMessage(0, false).getString().toLowerCase();
            int variation = 0;
            boolean explicitVariation = false;
            for (int letter = 0; letter < 26; ++letter) {
                if (!buildingKey.endsWith("_" + (char)(97 + letter))) continue;
                buildingKey = buildingKey.substring(0, buildingKey.length() - 2);
                variation = letter;
                explicitVariation = true;
            }
            char variationLetter = 'A';
            variationLetter = (char)(variationLetter + variation);
            File exportDir = MillCommonUtilities.getExportDir();
            File buildingFile = new File(exportDir, buildingKey + "_" + variationLetter + ".txt");
            if (!buildingFile.exists()) {
                File foundFile = null;
                BuildingPlanSet foundPlanSet = null;
                for (Culture culture : Culture.ListCultures) {
                    for (BuildingPlanSet planSet : culture.ListPlanSets) {
                        if (foundFile != null || !planSet.key.equals(buildingKey) || planSet.plans.size() <= variation) continue;
                        foundFile = planSet.plans.get(variation)[0].getLoadedFromFile();
                        foundPlanSet = planSet;
                    }
                }
                if (foundFile == null) {
                    for (Culture culture : Culture.ListCultures) {
                        for (BuildingPlanSet planSet : culture.ListPlanSets) {
                            if (foundFile != null || planSet.plans.size() <= variation || planSet.plans.get((int)variation)[0].nativeName == null || !planSet.plans.get((int)variation)[0].nativeName.toLowerCase().equals(buildingKey)) continue;
                            foundFile = planSet.plans.get(variation)[0].getLoadedFromFile();
                            foundPlanSet = planSet;
                            buildingKey = planSet.key;
                            Component[] oldSignData = java.util.stream.IntStream.range(0, 4).mapToObj(i -> sign.getMessage(i, false)).toArray(Component[]::new);
                            startPoint.setBlock(world, Blocks.OAK_SIGN, 0, true, false);
                            SignBlockEntity newSign = startPoint.getSign(world);
                            newSign.setMessage(0, new TextComponent(buildingKey));
                            newSign.setMessage(1, oldSignData[1]);
                            newSign.setMessage(2, oldSignData[2]);
                            newSign.setMessage(3, oldSignData[3]);
                        }
                    }
                }
                if (foundFile != null) {
                    ServerSender.sendTranslatedSentence(player, '6', "import.copyingfrom", foundFile.getAbsolutePath().replace("\\", "/"));
                    Path exportPath = exportDir.toPath();
                    Path inputPath = foundFile.toPath().getParent();
                    for (int exportVariation = 0; exportVariation < foundPlanSet.plans.size(); ++exportVariation) {
                        char exportVariationLetter = (char)(65 + exportVariation);
                        String txtFileName = foundPlanSet.key + "_" + exportVariationLetter + ".txt";
                        Files.copy(inputPath.resolve(txtFileName), exportPath.resolve(txtFileName), StandardCopyOption.REPLACE_EXISTING);
                        for (int buildingUpgrade = 0; buildingUpgrade < foundPlanSet.plans.get(exportVariation).length; ++buildingUpgrade) {
                            String string = foundPlanSet.key + "_" + exportVariationLetter + buildingUpgrade + ".png";
                            Files.copy(inputPath.resolve(string), exportPath.resolve(string), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                } else {
                    ServerSender.sendTranslatedSentence(player, '6', "import.errornotfound", new String[0]);
                    return;
                }
            }
            BuildingPlanSet existingSet = BuildingImportExport.loadPlanSetFromExportDir(buildingKey);
            boolean importAll = sign.getMessage(1, false) != null && sign.getMessage(1, false).getString().equalsIgnoreCase("all");
            boolean includeSpecialPoints = sign.getMessage(3, false) == null || !sign.getMessage(3, false).getString().equalsIgnoreCase("nomock");
            int orientation = 0;
            if (sign.getMessage(3, false) != null && sign.getMessage(3, false).getString().startsWith("or:")) {
                String orientationString = sign.getMessage(3, false).getString().substring(3, sign.getMessage(3, false).getString().length());
                orientation = Integer.parseInt(orientationString);
            }
            if (!importAll) {
                int upgradeLevel = 0;
                if (sign.getMessage(1, false) != null && sign.getMessage(1, false).getString().length() > 0) {
                    try {
                        upgradeLevel = Integer.parseInt(sign.getMessage(1, false).getString());
                        ServerSender.sendTranslatedSentence(player, 'f', "import.buildingupto", "" + upgradeLevel);
                    }
                    catch (Exception e) {
                        ServerSender.sendTranslatedSentence(player, '6', "import.errorinvalidupgradelevel", new String[0]);
                        return;
                    }
                } else {
                    ServerSender.sendTranslatedSentence(player, 'f', "import.buildinginitialphase", new String[0]);
                }
                if (upgradeLevel >= existingSet.plans.get(variation).length) {
                    ServerSender.sendTranslatedSentence(player, '6', "import.errorupgradeleveltoohigh", new String[0]);
                    return;
                }
                if (sign.getMessage(2, false) != null) {
                    String signLine = sign.getMessage(2, false).getString();
                    if (signLine.equals("x2")) {
                        BuildingImportExport.doubleHeightPlan(player, existingSet);
                    } else if (signLine.equals("hmirror") || signLine.equals("vmirror")) {
                        boolean horizontalmirror = signLine.equals("hmirror");
                        BuildingImportExport.mirrorPlan(existingSet, horizontalmirror);
                    } else if (signLine.startsWith("wood:")) {
                        String woodTypeName = signLine.substring("wood:".length(), signLine.length());
                        WoodVariant woodType = null;
                        for (WoodVariant type : WoodVariant.values()) {
                            if (!type.getSerializedName().equals(woodTypeName)) continue;
                            woodType = type;
                        }
                        if (woodType == null) {
                            ServerSender.sendTranslatedSentence(player, '6', "import.errorunknownwoodtype", woodTypeName);
                        } else {
                            BuildingImportExport.replaceWoodType(existingSet, woodType);
                        }
                    }
                }
                BuildingImportExport.summoningWandImportBuildingPlan(player, world, startPoint, variation, existingSet, upgradeLevel, includeSpecialPoints, orientation, false);
            } else {
                Point adjustedStartPoint = startPoint.getRelative(1.0, 0.0, 0.0);
                int variationStart = 0;
                int variationEnd = existingSet.plans.size();
                if (explicitVariation) {
                    variationStart = variation;
                    variationEnd = variation + 1;
                }
                for (int variationPos = variationStart; variationPos < variationEnd; ++variationPos) {
                    for (int i = 0; i < existingSet.plans.get(variationPos).length; ++i) {
                        BuildingImportExport.summoningWandImportBuildingPlan(player, world, adjustedStartPoint, variationPos, existingSet, i, includeSpecialPoints, orientation, true);
                        adjustedStartPoint = adjustedStartPoint.getRelative(existingSet.plans.get((int)variationPos)[0].length + 10, 0.0, 0.0);
                    }
                    adjustedStartPoint = new Point(startPoint.x, startPoint.y, adjustedStartPoint.z + (double)existingSet.plans.get((int)variationPos)[0].width + 10.0);
                }
            }
        }
        catch (Exception e) {
            MillLog.printException("Error when importing a building:", e);
        }
    }
}

