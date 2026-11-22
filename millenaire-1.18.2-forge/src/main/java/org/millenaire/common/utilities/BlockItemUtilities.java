/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockBush
 *  net.minecraft.block.BlockCrops
 *  net.minecraft.block.BlockDoor
 *  net.minecraft.block.BlockFarmland
 *  net.minecraft.block.BlockFlowerPot$EnumFlowerType
 *  net.minecraft.block.BlockLiquid
 *  net.minecraft.block.BlockSlab
 *  net.minecraft.block.BlockStairs
 *  net.minecraft.block.material.Material
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.Blocks
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.BlockPos
 *  net.minecraftforge.fluids.IFluidBlock
 */
package org.millenaire.common.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraftforge.fluids.IFluidBlock;
import org.millenaire.common.block.BlockMillCrops;
import org.millenaire.common.block.BlockPathSlab;
import org.millenaire.common.block.IBlockPath;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.network.ServerSender;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.VillageUtilities;
import org.millenaire.common.village.Building;
import org.millenaire.common.village.BuildingLocation;
import org.millenaire.common.world.MillWorldData;
import org.millenaire.common.world.UserProfile;

public class BlockItemUtilities {
    private static final Map<Material, String> MATERIAL_NAME_MAP = BlockItemUtilities.createMaterialNameMap();
    private static final Set<String> FORBIDDEN_MATERIALS = new HashSet<String>(20);
    private static final Set<String> FORBIDDEN_BLOCKS = new HashSet<String>(20);
    private static final Set<String> FORBIDDEN_EXCEPTIONS = new HashSet<String>(20);
    private static final Set<String> GROUND_MATERIALS = new HashSet<String>(20);
    private static final Set<String> GROUND_BLOCKS = new HashSet<String>(20);
    private static final Set<String> GROUND_EXCEPTIONS = new HashSet<String>(20);
    private static final Set<String> DANGER_MATERIALS = new HashSet<String>(20);
    private static final Set<String> DANGER_BLOCKS = new HashSet<String>(20);
    private static final Set<String> DANGER_EXCEPTIONS = new HashSet<String>(20);
    private static final Set<String> WATER_MATERIALS = new HashSet<String>(20);
    private static final Set<String> WATER_BLOCKS = new HashSet<String>(20);
    private static final Set<String> WATER_EXCEPTIONS = new HashSet<String>(20);
    private static final Set<String> PATH_REPLACEABLE_MATERIALS = new HashSet<String>(20);
    private static final Set<String> PATH_REPLACEABLE_BLOCKS = new HashSet<String>(20);
    private static final Set<String> PATH_REPLACEABLE_EXCEPTIONS = new HashSet<String>(20);

    public static void checkForHarvestTheft(Player player, BlockPos pos) {
        Building building;
        BuildingLocation location;
        Point actionPos;
        MillWorldData mwd = Mill.getMillWorld(player.level);
        Building closestVillageTH = mwd.getClosestVillage(actionPos = new Point(pos));
        if (closestVillageTH != null && !closestVillageTH.controlledBy(player) && (location = closestVillageTH.getLocationAtCoord(actionPos)) != null && (building = location.getBuilding(player.level)) != null) {
            UserProfile serverProfile;
            boolean isBuildingPlayerOwned;
            boolean bl = isBuildingPlayerOwned = building.location.getPlan() != null && (building.location.getPlan().price > 0 || building.location.getPlan().isgift);
            if (!isBuildingPlayerOwned && (serverProfile = VillageUtilities.getServerProfile(player.level, player)) != null) {
                int reputationLost = 100;
                serverProfile.adjustReputation(closestVillageTH, -100);
                ServerSender.sendTranslatedSentence(player, '6', "ui.stealingcrops", "100");
            }
        }
    }

    private static Map<Material, String> createMaterialNameMap() {
        HashMap<Material, String> result = new HashMap<Material, String>(50);
        result.put(Material.AIR, "air");
        result.put(Material.GRASS, "grass");
        result.put(Material.DIRT, "ground");
        result.put(Material.WOOD, "wood");
        result.put(Material.STONE, "rock");
        result.put(Material.METAL, "iron");
        result.put(Material.HEAVY_METAL, "anvil");
        result.put(Material.WATER, "water");
        result.put(Material.LAVA, "lava");
        result.put(Material.LEAVES, "leaves");
        result.put(Material.PLANT, "plants");
        result.put(Material.REPLACEABLE_PLANT, "vine");
        result.put(Material.SPONGE, "sponge");
        result.put(Material.WOOL, "cloth");
        result.put(Material.FIRE, "fire");
        result.put(Material.SAND, "sand");
        result.put(Material.DECORATION, "circuits");
        result.put(Material.CLOTH_DECORATION, "carpet");
        result.put(Material.GLASS, "glass");
        result.put(Material.BUILDABLE_GLASS, "redstone_light");
        result.put(Material.EXPLOSIVE, "tnt");
        // The unused legacy coral Material has no modern equivalent.
        result.put(Material.ICE, "ice");
        result.put(Material.ICE_SOLID, "packed_ice");
        result.put(Material.TOP_SNOW, "snow");
        result.put(Material.SNOW, "crafted_snow");
        result.put(Material.CACTUS, "cactus");
        result.put(Material.CLAY, "clay");
        result.put(Material.VEGETABLE, "gourd");
        result.put(Material.EGG, "dragon_egg");
        result.put(Material.PORTAL, "portal");
        result.put(Material.CAKE, "cake");
        result.put(Material.WEB, "web");
        return Collections.unmodifiableMap(result);
    }

    public static String getBlockCanonicalName(Block block) {
        if (block != null) {
            return net.minecraft.core.Registry.BLOCK.getKey(block).toString();
        }
        return null;
    }

    public static String getBlockMaterialName(Block block) {
        if (block != null && block.defaultBlockState() != null) {
            return MATERIAL_NAME_MAP.get(block.defaultBlockState().getMaterial());
        }
        return null;
    }

    public static ItemStack getItemStackFromBlockState(BlockState state, int quantity) {
        return org.millenaire.common.item.LegacyItemStacks.stack(state.getBlock(), quantity, org.millenaire.common.item.LegacyItemStacks.metadata(state));
    }

    public static BlockState getLogBlockstateFromPlankMeta(int plankMeta) {
        return org.millenaire.common.block.WoodVariant.byMetadata(plankMeta).log().defaultBlockState();
    }

    public static void initBlockTypes() {
        File mainBlockTypesFile = new File(MillCommonUtilities.getMillenaireContentDir(), "blocktypes.txt");
        if (!mainBlockTypesFile.exists()) {
            System.err.println("ERROR: Could not find the blocktypes file at " + mainBlockTypesFile.getAbsolutePath());
            Mill.startupError = true;
            return;
        }
        boolean success = BlockItemUtilities.readBlockTypesFile(mainBlockTypesFile);
        if (!success) {
            System.err.println("ERROR: Could not read the blocktypes file at " + mainBlockTypesFile.getAbsolutePath());
            Mill.startupError = true;
            return;
        }
        File customBlockTypesFile = new File(MillCommonUtilities.getMillenaireCustomContentDir(), "blocktypes.txt");
        if (customBlockTypesFile.exists()) {
            BlockItemUtilities.readBlockTypesFile(customBlockTypesFile);
        }
    }

    private static boolean matchesConfiguredBlock(Set<String> configured, Block block) {
        return configured.stream().anyMatch(name -> LegacyBlockStates.matchesName(block, name));
    }

    public static boolean isBlockDangerous(Block b) {
        if (b == null || b == Blocks.AIR || matchesConfiguredBlock(DANGER_EXCEPTIONS, b)) {
            return false;
        }
        if (DANGER_MATERIALS.contains(BlockItemUtilities.getBlockMaterialName(b))) {
            return true;
        }
        return matchesConfiguredBlock(DANGER_BLOCKS, b);
    }

    public static boolean isBlockDecorativePlant(Block block) {
        if (block == null || block == Blocks.AIR) {
            return false;
        }
        if (block instanceof BlockMillCrops || block instanceof CropBlock) {
            return false;
        }
        return block instanceof BushBlock;
    }

    public static boolean isBlockForbidden(Block b) {
        if (b == null || b == Blocks.AIR || matchesConfiguredBlock(FORBIDDEN_EXCEPTIONS, b)) {
            return false;
        }
        if (b.defaultBlockState().hasBlockEntity()) {
            return true;
        }
        if (FORBIDDEN_MATERIALS.contains(BlockItemUtilities.getBlockMaterialName(b))) {
            return true;
        }
        return matchesConfiguredBlock(FORBIDDEN_BLOCKS, b);
    }

    public static boolean isBlockGround(Block b) {
        if (b == null || b == Blocks.AIR || matchesConfiguredBlock(GROUND_EXCEPTIONS, b)) {
            return false;
        }
        if (GROUND_MATERIALS.contains(BlockItemUtilities.getBlockMaterialName(b))) {
            return true;
        }
        return matchesConfiguredBlock(GROUND_BLOCKS, b);
    }

    public static boolean isBlockLiquid(Block b) {
        if (b == null || b == Blocks.AIR) {
            return false;
        }
        return b instanceof LiquidBlock || b instanceof IFluidBlock;
    }

    public static boolean isBlockOpaqueCube(Block block) {
        BlockState bs = block.defaultBlockState();
        return bs.isSolidRender(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    public static boolean isBlockPathReplaceable(Block b) {
        if (b == null) {
            return false;
        }
        if (b == Blocks.AIR) {
            return false;
        }
        if (matchesConfiguredBlock(PATH_REPLACEABLE_EXCEPTIONS, b)) {
            return false;
        }
        if (PATH_REPLACEABLE_MATERIALS.contains(BlockItemUtilities.getBlockMaterialName(b))) {
            return true;
        }
        return matchesConfiguredBlock(PATH_REPLACEABLE_BLOCKS, b);
    }

    public static boolean isBlockSolid(Block b) {
        if (b == null) {
            return false;
        }
        if (isBlockOpaqueCube(b) || b.defaultBlockState().isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
            return true;
        }
        return b == Blocks.GLASS || b == Blocks.GLASS_PANE || b == Blocks.STONE_SLAB || b instanceof SlabBlock || b instanceof StairBlock || BlockItemUtilities.isFence(b) || b == MillBlocks.PAPER_WALL || b instanceof IBlockPath || b instanceof FarmBlock;
    }

    public static boolean isBlockWalkable(Block b) {
        if (b == null) {
            return false;
        }
        if (isBlockOpaqueCube(b) || b.defaultBlockState().isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
            return true;
        }
        return b == Blocks.GLASS || b == Blocks.STONE_SLAB || b instanceof SlabBlock || b instanceof StairBlock || b instanceof IBlockPath || b instanceof FarmBlock;
    }

    public static boolean isBlockWater(Block b) {
        if (b == null || b == Blocks.AIR || matchesConfiguredBlock(WATER_EXCEPTIONS, b)) {
            return false;
        }
        if (WATER_MATERIALS.contains(BlockItemUtilities.getBlockMaterialName(b))) {
            return true;
        }
        return matchesConfiguredBlock(WATER_BLOCKS, b);
    }

    public static boolean isFence(Block block) {
        return block == Blocks.ACACIA_FENCE || block == Blocks.BIRCH_FENCE || block == Blocks.DARK_OAK_FENCE || block == Blocks.JUNGLE_FENCE || block == Blocks.OAK_FENCE || block == Blocks.SPRUCE_FENCE;
    }

    public static boolean isFenceGate(Block block) {
        return block == Blocks.ACACIA_FENCE_GATE || block == Blocks.BIRCH_FENCE_GATE || block == Blocks.DARK_OAK_FENCE_GATE || block == Blocks.JUNGLE_FENCE_GATE || block == Blocks.OAK_FENCE_GATE || block == Blocks.SPRUCE_FENCE_GATE;
    }

    public static boolean isPath(Block block) {
        return block instanceof IBlockPath || block instanceof BlockPathSlab;
    }

    public static boolean isPathSlab(Block block) {
        return block instanceof BlockPathSlab;
    }

    public static boolean isWoodenDoor(Block block) {
        return block instanceof DoorBlock && (block.defaultBlockState().getMaterial() == Material.WOOD || block.defaultBlockState().getMaterial() == Material.NETHER_WOOD);
    }

    private static boolean readBlockTypesFile(File file) {
        if (!file.exists()) {
            return false;
        }
        try {
            BufferedReader reader = MillCommonUtilities.getReader(file);
            String line = reader.readLine();
            while (line != null) {
                String[] temp;
                if (line.trim().length() > 0 && !line.startsWith("//") && (temp = line.split("=")).length == 2) {
                    String key = temp[0].trim().toLowerCase();
                    String value = temp[1];
                    if (key.equals("forbidden_materials")) {
                        FORBIDDEN_MATERIALS.clear();
                        FORBIDDEN_MATERIALS.addAll(Arrays.asList(value.split(",")));
                    } else if (key.equals("forbidden_blocks")) {
                        FORBIDDEN_BLOCKS.clear();
                        FORBIDDEN_BLOCKS.addAll(Arrays.asList(value.split(",")));
                    } else if (key.equals("forbidden_exceptions")) {
                        FORBIDDEN_EXCEPTIONS.clear();
                        FORBIDDEN_EXCEPTIONS.addAll(Arrays.asList(value.split(",")));
                    } else if (key.equals("ground_materials")) {
                        GROUND_MATERIALS.clear();
                        GROUND_MATERIALS.addAll(Arrays.asList(value.split(",")));
                    } else if (key.equals("ground_blocks")) {
                        GROUND_BLOCKS.clear();
                        GROUND_BLOCKS.addAll(Arrays.asList(value.split(",")));
                    } else if (key.equals("ground_exceptions")) {
                        GROUND_EXCEPTIONS.clear();
                        GROUND_EXCEPTIONS.addAll(Arrays.asList(value.split(",")));
                    } else if (key.equals("danger_materials")) {
                        DANGER_MATERIALS.clear();
                        DANGER_MATERIALS.addAll(Arrays.asList(value.split(",")));
                    } else if (key.equals("danger_blocks")) {
                        DANGER_BLOCKS.clear();
                        DANGER_BLOCKS.addAll(Arrays.asList(value.split(",")));
                    } else if (key.equals("danger_exceptions")) {
                        DANGER_EXCEPTIONS.clear();
                        DANGER_EXCEPTIONS.addAll(Arrays.asList(value.split(",")));
                    } else if (key.equals("water_materials")) {
                        WATER_MATERIALS.clear();
                        WATER_MATERIALS.addAll(Arrays.asList(value.split(",")));
                    } else if (key.equals("water_blocks")) {
                        WATER_BLOCKS.clear();
                        WATER_BLOCKS.addAll(Arrays.asList(value.split(",")));
                    } else if (key.equals("water_exceptions")) {
                        WATER_EXCEPTIONS.clear();
                        WATER_EXCEPTIONS.addAll(Arrays.asList(value.split(",")));
                    } else if (key.equals("path_replaceable_materials")) {
                        PATH_REPLACEABLE_MATERIALS.clear();
                        PATH_REPLACEABLE_MATERIALS.addAll(Arrays.asList(value.split(",")));
                    } else if (key.equals("path_replaceable_blocks")) {
                        PATH_REPLACEABLE_BLOCKS.clear();
                        PATH_REPLACEABLE_BLOCKS.addAll(Arrays.asList(value.split(",")));
                    } else if (key.equals("path_replaceable_exceptions")) {
                        PATH_REPLACEABLE_EXCEPTIONS.clear();
                        PATH_REPLACEABLE_EXCEPTIONS.addAll(Arrays.asList(value.split(",")));
                    } else {
                        MillLog.error(null, "Unknown block type category on line: " + line);
                    }
                }
                line = reader.readLine();
            }
            reader.close();
        }
        catch (Exception e) {
            MillLog.printException(e);
            return false;
        }
        return true;
    }
}

