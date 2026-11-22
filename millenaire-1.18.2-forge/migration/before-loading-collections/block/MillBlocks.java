/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockBanner
 *  net.minecraft.block.BlockSlab
 *  net.minecraft.block.SoundType
 *  net.minecraft.block.material.MapColor
 *  net.minecraft.block.material.Material
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.client.renderer.block.model.ModelResourceLocation
 *  net.minecraft.creativetab.CreativeTabs
 *  net.minecraft.init.Blocks
 *  net.minecraft.item.EnumDyeColor
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemBlock
 *  net.minecraft.item.ItemSlab
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.ResourceLocation
 *  net.minecraftforge.client.model.ModelLoader
 *  net.minecraftforge.event.RegistryEvent$Register
 *  net.minecraftforge.fml.common.registry.GameRegistry
 *  net.minecraftforge.fml.common.registry.GameRegistry$ObjectHolder
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 *  net.minecraftforge.registries.IForgeRegistryEntry
 */
package org.millenaire.common.block;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.item.ItemSlab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.registries.ObjectHolder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.millenaire.common.block.BlockAlchemistExplosive;
import org.millenaire.common.block.BlockBars;
import org.millenaire.common.block.BlockCustomIce;
import org.millenaire.common.block.BlockCustomSnow;
import org.millenaire.common.block.BlockDecorativeEarth;
import org.millenaire.common.block.BlockDecorativeStone;
import org.millenaire.common.block.BlockDecorativeWood;
import org.millenaire.common.block.BlockExtendedMudBrick;
import org.millenaire.common.block.BlockFirePit;
import org.millenaire.common.block.BlockFruitLeaves;
import org.millenaire.common.block.BlockGrapeVine;
import org.millenaire.common.block.BlockImportTable;
import org.millenaire.common.block.BlockLockedChest;
import org.millenaire.common.block.BlockMillBed;
import org.millenaire.common.block.BlockMillCrops;
import org.millenaire.common.block.BlockMillPane;
import org.millenaire.common.block.BlockMillSandstone;
import org.millenaire.common.block.BlockMillSandstoneDecorated;
import org.millenaire.common.block.BlockMillSapling;
import org.millenaire.common.block.BlockMillSlab;
import org.millenaire.common.block.BlockMillStainedGlass;
import org.millenaire.common.block.BlockMillStairs;
import org.millenaire.common.block.BlockMillStatue;
import org.millenaire.common.block.BlockMillWall;
import org.millenaire.common.block.BlockOrientedSlab;
import org.millenaire.common.block.BlockOrientedSlabDoubleDecorated;
import org.millenaire.common.block.BlockPaintedBricks;
import org.millenaire.common.block.BlockPaintedSlab;
import org.millenaire.common.block.BlockPaintedStairs;
import org.millenaire.common.block.BlockPaintedWall;
import org.millenaire.common.block.BlockPanel;
import org.millenaire.common.block.BlockPath;
import org.millenaire.common.block.BlockPathSlab;
import org.millenaire.common.block.BlockRosette;
import org.millenaire.common.block.BlockRosetteBars;
import org.millenaire.common.block.BlockSilkWorm;
import org.millenaire.common.block.BlockSlabStone;
import org.millenaire.common.block.BlockSlabWood;
import org.millenaire.common.block.BlockSnailSoil;
import org.millenaire.common.block.BlockSod;
import org.millenaire.common.block.BlockWetBrick;
import org.millenaire.common.block.IPaintedBlock;
import org.millenaire.common.block.mock.MockBlockAnimalSpawn;
import org.millenaire.common.block.mock.MockBlockBannerHanging;
import org.millenaire.common.block.mock.MockBlockBannerStanding;
import org.millenaire.common.block.mock.MockBlockDecor;
import org.millenaire.common.block.mock.MockBlockFree;
import org.millenaire.common.block.mock.MockBlockMainChest;
import org.millenaire.common.block.mock.MockBlockMarker;
import org.millenaire.common.block.mock.MockBlockSoil;
import org.millenaire.common.block.mock.MockBlockSource;
import org.millenaire.common.block.mock.MockBlockTreeSpawn;
import org.millenaire.common.entity.TileEntityFirePit;
import org.millenaire.common.entity.TileEntityImportTable;
import org.millenaire.common.entity.TileEntityLockedChest;
import org.millenaire.common.entity.TileEntityMillBed;
import org.millenaire.common.entity.TileEntityMockBanner;
import org.millenaire.common.entity.TileEntityPanel;
import org.millenaire.common.item.ItemBlockMeta;
import org.millenaire.common.item.ItemHalfSlab;
import org.millenaire.common.item.ItemMillBed;
import org.millenaire.common.item.ItemMillSapling;
import org.millenaire.common.item.ItemMockBanner;
import org.millenaire.common.item.ItemPathSlab;
import org.millenaire.common.item.ItemSlabMeta;
import org.millenaire.common.item.MillItems;

public class MillBlocks {
    @ObjectHolder(value="millenaire:wood_deco")
    public static BlockDecorativeWood WOOD_DECORATION;
    @ObjectHolder(value="millenaire:stone_deco")
    public static BlockDecorativeStone STONE_DECORATION;
    @ObjectHolder(value="millenaire:earth_deco")
    public static BlockDecorativeEarth EARTH_DECORATION;
    @ObjectHolder(value="millenaire:wall_mud_brick")
    public static BlockMillWall WALL_MUD_BRICK;
    public static Map<String, Map<DyeColor, Block>> PAINTED_BRICK_MAP;
    @ObjectHolder(value="millenaire:painted_brick_white")
    public static BlockPaintedBricks PAINTED_BRICK_WHITE;
    @ObjectHolder(value="millenaire:painted_brick_decorated_white")
    public static BlockPaintedBricks PAINTED_BRICK_DECORATED_WHITE;
    @ObjectHolder(value="millenaire:extended_mud_brick")
    public static BlockExtendedMudBrick EXTENDED_MUD_BRICK;
    @ObjectHolder(value="millenaire:slab_wood_deco")
    public static BlockSlabWood SLAB_WOOD_DECORATION;
    @ObjectHolder(value="millenaire:slab_stone_deco")
    public static BlockSlabStone SLAB_STONE_DECORATION;
    @ObjectHolder(value="millenaire:stairs_timberframe")
    public static BlockMillStairs STAIRS_TIMBERFRAME;
    @ObjectHolder(value="millenaire:stairs_mudbrick")
    public static BlockMillStairs STAIRS_MUDBRICK;
    @ObjectHolder(value="millenaire:stairs_cookedbrick")
    public static BlockMillStairs STAIRS_COOKEDBRICK;
    @ObjectHolder(value="millenaire:stairs_thatch")
    public static BlockMillStairs STAIRS_THATCH;
    @ObjectHolder(value="millenaire:stairs_byzantine_tiles")
    public static BlockMillStairs STAIRS_BYZ_TILES;
    @ObjectHolder(value="millenaire:sandstone_carved")
    public static BlockMillSandstone SANDSTONE_CARVED;
    @ObjectHolder(value="millenaire:sandstone_red_carved")
    public static BlockMillSandstone SANDSTONE_RED_CARVED;
    @ObjectHolder(value="millenaire:sandstone_ochre_carved")
    public static BlockMillSandstone SANDSTONE_OCHRE_CARVED;
    @ObjectHolder(value="millenaire:stairs_sandstone_carved")
    public static BlockMillStairs STAIRS_SANDSTONE_CARVED;
    @ObjectHolder(value="millenaire:stairs_sandstone_red_carved")
    public static BlockMillStairs STAIRS_SANDSTONE_RED_CARVED;
    @ObjectHolder(value="millenaire:stairs_sandstone_ochre_carved")
    public static BlockMillStairs STAIRS_SANDSTONE_OCHRE_CARVED;
    @ObjectHolder(value="millenaire:slab_sandstone_carved")
    public static BlockMillSlab SLAB_SANDSTONE_CARVED;
    @ObjectHolder(value="millenaire:slab_sandstone_red_carved")
    public static BlockMillSlab SLAB_SANDSTONE_RED_CARVED;
    @ObjectHolder(value="millenaire:slab_sandstone_ochre_carved")
    public static BlockMillSlab SLAB_SANDSTONE_OCHRE_CARVED;
    @ObjectHolder(value="millenaire:wall_sandstone_carved")
    public static BlockMillWall WALL_SANDSTONE_CARVED;
    @ObjectHolder(value="millenaire:wall_sandstone_red_carved")
    public static BlockMillWall WALL_SANDSTONE_RED_CARVED;
    @ObjectHolder(value="millenaire:wall_sandstone_ochre_carved")
    public static BlockMillWall WALL_SANDSTONE_OCHRE_CARVED;
    @ObjectHolder(value="millenaire:wet_brick")
    public static BlockWetBrick WET_BRICK;
    @ObjectHolder(value="millenaire:silk_worm")
    public static BlockSilkWorm SILK_WORM;
    @ObjectHolder(value="millenaire:snail_soil")
    public static BlockSnailSoil SNAIL_SOIL;
    @ObjectHolder(value="millenaire:pathdirt")
    public static BlockPath PATHDIRT;
    @ObjectHolder(value="millenaire:pathdirt_slab")
    public static BlockPathSlab PATHDIRT_SLAB;
    @ObjectHolder(value="millenaire:pathgravel")
    public static BlockPath PATHGRAVEL;
    @ObjectHolder(value="millenaire:pathgravel_slab")
    public static BlockPathSlab PATHGRAVEL_SLAB;
    @ObjectHolder(value="millenaire:pathslabs")
    public static BlockPath PATHSLABS;
    @ObjectHolder(value="millenaire:pathslabs_slab")
    public static BlockPathSlab PATHSLABS_SLAB;
    @ObjectHolder(value="millenaire:pathsandstone")
    public static BlockPath PATHSANDSTONE;
    @ObjectHolder(value="millenaire:pathsandstone_slab")
    public static BlockPathSlab PATHSANDSTONE_SLAB;
    @ObjectHolder(value="millenaire:pathgravelslabs")
    public static BlockPath PATHGRAVELSLABS;
    @ObjectHolder(value="millenaire:pathgravelslabs_slab")
    public static BlockPathSlab PATHGRAVELSLABS_SLAB;
    @ObjectHolder(value="millenaire:pathochretiles")
    public static BlockPath PATHOCHRESLABS;
    @ObjectHolder(value="millenaire:pathochretiles_slab")
    public static BlockPathSlab PATHOCHRESLABS_SLAB;
    @ObjectHolder(value="millenaire:pathsnow")
    public static BlockPath PATHSNOW;
    @ObjectHolder(value="millenaire:pathsnow_slab")
    public static BlockPathSlab PATHSNOW_SLAB;
    @ObjectHolder(value="millenaire:locked_chest")
    public static BlockLockedChest LOCKED_CHEST;
    @ObjectHolder(value="millenaire:stained_glass")
    public static BlockMillStainedGlass STAINED_GLASS;
    @ObjectHolder(value="millenaire:rosette")
    public static BlockRosette ROSETTE;
    @ObjectHolder(value="millenaire:panel")
    public static BlockPanel PANEL;
    @ObjectHolder(value="millenaire:paper_wall")
    public static BlockMillPane PAPER_WALL;
    @ObjectHolder(value="millenaire:wooden_bars")
    public static BlockBars WOODEN_BARS;
    @ObjectHolder(value="millenaire:wooden_bars_indian")
    public static BlockBars WOODEN_BARS_INDIAN;
    @ObjectHolder(value="millenaire:wooden_bars_rosette")
    public static BlockRosetteBars WOODEN_BARS_ROSETTE;
    @ObjectHolder(value="millenaire:byzantine_tiles")
    public static BlockOrientedSlab.BlockOrientedSlabDouble BYZANTINE_TILES;
    @ObjectHolder(value="millenaire:byzantine_tiles_slab")
    public static BlockOrientedSlab.BlockOrientedSlabSlab BYZANTINE_TILES_SLAB;
    @ObjectHolder(value="millenaire:byzantine_stone_tiles")
    public static BlockOrientedSlabDoubleDecorated BYZANTINE_STONE_TILES;
    @ObjectHolder(value="millenaire:byzantine_sandstone_tiles")
    public static BlockOrientedSlabDoubleDecorated BYZANTINE_SANDSTONE_TILES;
    @ObjectHolder(value="millenaire:byzantine_stone_ornament")
    public static BlockMillSandstoneDecorated BYZANTINE_STONE_ORNAMENT;
    @ObjectHolder(value="millenaire:byzantine_sandstone_ornament")
    public static BlockMillSandstoneDecorated BYZANTINE_SANDSTONE_ORNAMENT;
    @ObjectHolder(value="millenaire:crop_rice")
    public static BlockMillCrops CROP_RICE;
    @ObjectHolder(value="millenaire:crop_turmeric")
    public static BlockMillCrops CROP_TURMERIC;
    @ObjectHolder(value="millenaire:crop_maize")
    public static BlockMillCrops CROP_MAIZE;
    @ObjectHolder(value="millenaire:crop_vine")
    public static BlockGrapeVine CROP_VINE;
    @ObjectHolder(value="millenaire:crop_cotton")
    public static BlockMillCrops CROP_COTTON;
    @ObjectHolder(value="millenaire:alchemistexplosive")
    public static BlockAlchemistExplosive ALCHEMIST_EXPLOSIVE;
    @ObjectHolder(value="millenaire:bed_straw")
    public static BlockMillBed BED_STRAW;
    @ObjectHolder(value="millenaire:bed_charpoy")
    public static BlockMillBed BED_CHARPOY;
    @ObjectHolder(value="millenaire:markerblock")
    public static MockBlockMarker MARKER_BLOCK;
    @ObjectHolder(value="millenaire:mainchest")
    public static MockBlockMainChest MAIN_CHEST;
    @ObjectHolder(value="millenaire:animalspawn")
    public static MockBlockAnimalSpawn ANIMAL_SPAWN;
    @ObjectHolder(value="millenaire:source")
    public static MockBlockSource SOURCE;
    @ObjectHolder(value="millenaire:freeblock")
    public static MockBlockFree FREE_BLOCK;
    @ObjectHolder(value="millenaire:treespawn")
    public static MockBlockTreeSpawn TREE_SPAWN;
    @ObjectHolder(value="millenaire:soil")
    public static MockBlockSoil SOIL_BLOCK;
    @ObjectHolder(value="millenaire:decorblock")
    public static MockBlockDecor DECOR_BLOCK;
    @ObjectHolder(value="millenaire:villagebannerwall")
    public static MockBlockBannerHanging VILLAGE_BANNER_WALL;
    @ObjectHolder(value="millenaire:villagebannerstanding")
    public static MockBlockBannerStanding VILLAGE_BANNER_STANDING;
    @ObjectHolder(value="millenaire:culturebannerwall")
    public static MockBlockBannerHanging CULTURE_BANNER_WALL;
    @ObjectHolder(value="millenaire:culturebannerstanding")
    public static MockBlockBannerStanding CULTURE_BANNER_STANDING;
    @ObjectHolder(value="millenaire:sod")
    public static BlockSod SOD;
    @ObjectHolder(value="millenaire:icebrick")
    public static BlockCustomIce ICE_BRICK;
    @ObjectHolder(value="millenaire:snowbrick")
    public static BlockCustomSnow SNOW_BRICK;
    @ObjectHolder(value="millenaire:inuitcarving")
    public static BlockMillStatue INUIT_CARVING;
    @ObjectHolder(value="millenaire:snowwall")
    public static BlockMillWall SNOW_WALL;
    @ObjectHolder(value="millenaire:fire_pit")
    public static BlockFirePit FIRE_PIT;
    @ObjectHolder(value="millenaire:import_table")
    public static BlockImportTable IMPORT_TABLE;
    @ObjectHolder(value="millenaire:sapling_appletree")
    public static BlockMillSapling SAPLING_APPLETREE;
    @ObjectHolder(value="millenaire:leaves_appletree")
    public static BlockFruitLeaves LEAVES_APPLETREE;
    @ObjectHolder(value="millenaire:sapling_olivetree")
    public static BlockMillSapling SAPLING_OLIVETREE;
    @ObjectHolder(value="millenaire:leaves_olivetree")
    public static BlockFruitLeaves LEAVES_OLIVETREE;
    @ObjectHolder(value="millenaire:sapling_pistachio")
    public static BlockMillSapling SAPLING_PISTACHIO;
    @ObjectHolder(value="millenaire:leaves_pistachio")
    public static BlockFruitLeaves LEAVES_PISTACHIO;
    @ObjectHolder(value="millenaire:stairs_gray_tiles")
    public static BlockMillStairs STAIRS_GRAY_TILES;
    @ObjectHolder(value="millenaire:stairs_green_tiles")
    public static BlockMillStairs STAIRS_GREEN_TILES;
    @ObjectHolder(value="millenaire:stairs_red_tiles")
    public static BlockMillStairs STAIRS_RED_TILES;
    @ObjectHolder(value="millenaire:gray_tiles")
    public static BlockOrientedSlab.BlockOrientedSlabDouble GRAY_TILES;
    @ObjectHolder(value="millenaire:green_tiles")
    public static BlockOrientedSlab.BlockOrientedSlabDouble GREEN_TILES;
    @ObjectHolder(value="millenaire:red_tiles")
    public static BlockOrientedSlab.BlockOrientedSlabDouble RED_TILES;
    @ObjectHolder(value="millenaire:gray_tiles_slab")
    public static BlockOrientedSlab.BlockOrientedSlabSlab GRAY_TILES_SLAB;
    @ObjectHolder(value="millenaire:green_tiles_slab")
    public static BlockOrientedSlab.BlockOrientedSlabSlab GREEN_TILES_SLAB;
    @ObjectHolder(value="millenaire:red_tiles_slab")
    public static BlockOrientedSlab.BlockOrientedSlabSlab RED_TILES_SLAB;
    @ObjectHolder(value="millenaire:wooden_bars_dark")
    public static BlockBars WOODEN_BARS_DARK;
    @ObjectHolder(value="millenaire:sapling_cherry")
    public static BlockMillSapling SAPLING_CHERRY;
    @ObjectHolder(value="millenaire:cherry_leaves")
    public static BlockFruitLeaves CHERRY_LEAVES;
    @ObjectHolder(value="millenaire:sapling_sakura")
    public static BlockMillSapling SAPLING_SAKURA;
    @ObjectHolder(value="millenaire:sakura_leaves")
    public static BlockFruitLeaves SAKURA_LEAVES;
    public static BlockState BS_WET_BRICK;
    public static BlockState BS_MUD_BRICK;
    public static CreativeModeTab tabMillenaire;
    public static CreativeModeTab tabMillenaireContentCreator;

    public static void initBlockStates() {
        BS_WET_BRICK = WET_BRICK.defaultBlockState().setValue(BlockWetBrick.PROGRESS, (BlockWetBrick.EnumType.WETBRICK0));
        BS_MUD_BRICK = STONE_DECORATION.defaultBlockState().setValue(BlockDecorativeStone.VARIANT, (BlockDecorativeStone.EnumType.MUDBRICK));
    }

    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        BlockDecorativeWood decorativeWood = new BlockDecorativeWood("wood_deco");
        event.getRegistry().register(decorativeWood);
        BlockDecorativeStone decorativeStone = new BlockDecorativeStone("stone_deco");
        event.getRegistry().register(decorativeStone);
        event.getRegistry().register(new BlockDecorativeEarth("earth_deco"));
        event.getRegistry().register(new BlockMillWall("wall_mud_brick", (Block)decorativeStone));
        PAINTED_BRICK_MAP.put("painted_brick", new HashMap());
        PAINTED_BRICK_MAP.put("painted_brick_decorated", new HashMap());
        PAINTED_BRICK_MAP.put("stairs_painted_brick", new HashMap());
        PAINTED_BRICK_MAP.put("slab_painted_brick", new HashMap());
        PAINTED_BRICK_MAP.put("wall_painted_brick", new HashMap());
        for (DyeColor colour : DyeColor.values()) {
            BlockPaintedBricks paintedBrick = new BlockPaintedBricks("painted_brick", colour);
            event.getRegistry().register(paintedBrick);
            PAINTED_BRICK_MAP.get("painted_brick").put(colour, paintedBrick);
            BlockPaintedBricks paintedBrickDecorated = new BlockPaintedBricks("painted_brick_decorated", colour);
            event.getRegistry().register(paintedBrickDecorated);
            PAINTED_BRICK_MAP.get("painted_brick_decorated").put(colour, paintedBrickDecorated);
            BlockPaintedStairs paintedBrickStairs = new BlockPaintedStairs("stairs_painted_brick", paintedBrick.defaultBlockState(), colour);
            event.getRegistry().register(paintedBrickStairs);
            PAINTED_BRICK_MAP.get("stairs_painted_brick").put(colour, (Block)paintedBrickStairs);
            BlockPaintedSlab paintedBrickSlabs = new BlockPaintedSlab("slab_painted_brick", paintedBrick, colour);
            event.getRegistry().register(paintedBrickSlabs);
            PAINTED_BRICK_MAP.get("slab_painted_brick").put(colour, paintedBrickSlabs);
            BlockPaintedWall paintedBrickWall = new BlockPaintedWall("wall_painted_brick", paintedBrick, colour);
            event.getRegistry().register(paintedBrickWall);
            PAINTED_BRICK_MAP.get("wall_painted_brick").put(colour, paintedBrickWall);
        }
        event.getRegistry().register(new BlockExtendedMudBrick("extended_mud_brick"));
        event.getRegistry().register(new BlockSlabWood("slab_wood_deco"));
        event.getRegistry().register(new BlockSlabStone("slab_stone_deco"));
        event.getRegistry().register(new BlockMillStairs("stairs_cookedbrick", decorativeStone.defaultBlockState().setValue(BlockDecorativeStone.VARIANT, (BlockDecorativeStone.EnumType.COOKEDBRICK))));
        event.getRegistry().register(new BlockMillStairs("stairs_mudbrick", decorativeStone.defaultBlockState().setValue(BlockDecorativeStone.VARIANT, (BlockDecorativeStone.EnumType.MUDBRICK))));
        event.getRegistry().register(new BlockMillStairs("stairs_timberframe", decorativeWood.defaultBlockState().setValue(BlockDecorativeWood.VARIANT, (BlockDecorativeWood.EnumType.TIMBERFRAMEPLAIN))));
        event.getRegistry().register(new BlockMillStairs("stairs_thatch", decorativeWood.defaultBlockState().setValue(BlockDecorativeWood.VARIANT, (BlockDecorativeWood.EnumType.THATCH))));
        BlockMillSandstone sandstoneCarved = new BlockMillSandstone("sandstone_carved");
        event.getRegistry().register(sandstoneCarved);
        BlockMillSandstone redSandstoneCarved = new BlockMillSandstone("sandstone_red_carved");
        event.getRegistry().register(redSandstoneCarved);
        BlockMillSandstone ochreSandstoneCarved = new BlockMillSandstone("sandstone_ochre_carved");
        event.getRegistry().register(ochreSandstoneCarved);
        event.getRegistry().register(new BlockMillStairs("stairs_sandstone_carved", sandstoneCarved.defaultBlockState()));
        event.getRegistry().register(new BlockMillStairs("stairs_sandstone_red_carved", redSandstoneCarved.defaultBlockState()));
        event.getRegistry().register(new BlockMillStairs("stairs_sandstone_ochre_carved", ochreSandstoneCarved.defaultBlockState()));
        event.getRegistry().register(new BlockMillSlab("slab_sandstone_carved", sandstoneCarved));
        event.getRegistry().register(new BlockMillSlab("slab_sandstone_red_carved", redSandstoneCarved));
        event.getRegistry().register(new BlockMillSlab("slab_sandstone_ochre_carved", ochreSandstoneCarved));
        event.getRegistry().register(new BlockMillWall("wall_sandstone_carved", sandstoneCarved));
        event.getRegistry().register(new BlockMillWall("wall_sandstone_red_carved", redSandstoneCarved));
        event.getRegistry().register(new BlockMillWall("wall_sandstone_ochre_carved", ochreSandstoneCarved));
        event.getRegistry().register(new BlockMillStainedGlass("stained_glass"));
        event.getRegistry().register(new BlockRosette("rosette", Material.GLASS, SoundType.GLASS));
        event.getRegistry().register(new BlockWetBrick("wet_brick"));
        event.getRegistry().register(new BlockSilkWorm("silk_worm"));
        event.getRegistry().register(new BlockSnailSoil("snail_soil"));
        event.getRegistry().register(new BlockPath("pathdirt", MaterialColor.DIRT, SoundType.GRAVEL));
        event.getRegistry().register(new BlockPathSlab("pathdirt", MaterialColor.DIRT, SoundType.GRAVEL));
        event.getRegistry().register(new BlockPath("pathgravel", MaterialColor.COLOR_GRAY, SoundType.GRAVEL));
        event.getRegistry().register(new BlockPathSlab("pathgravel", MaterialColor.COLOR_GRAY, SoundType.GRAVEL));
        event.getRegistry().register(new BlockPath("pathslabs", MaterialColor.STONE, SoundType.STONE));
        event.getRegistry().register(new BlockPathSlab("pathslabs", MaterialColor.STONE, SoundType.STONE));
        event.getRegistry().register(new BlockPath("pathsandstone", MaterialColor.SAND, SoundType.STONE));
        event.getRegistry().register(new BlockPathSlab("pathsandstone", MaterialColor.SAND, SoundType.STONE));
        event.getRegistry().register(new BlockPath("pathgravelslabs", MaterialColor.COLOR_GRAY, SoundType.STONE));
        event.getRegistry().register(new BlockPathSlab("pathgravelslabs", MaterialColor.COLOR_GRAY, SoundType.STONE));
        event.getRegistry().register(new BlockPath("pathochretiles", MaterialColor.TERRACOTTA_BROWN, SoundType.STONE));
        event.getRegistry().register(new BlockPathSlab("pathochretiles", MaterialColor.TERRACOTTA_BROWN, SoundType.STONE));
        event.getRegistry().register(new BlockPath("pathsnow", MaterialColor.SNOW, SoundType.SNOW));
        event.getRegistry().register(new BlockPathSlab("pathsnow", MaterialColor.SNOW, SoundType.SNOW));
        event.getRegistry().register(new BlockLockedChest("locked_chest"));
        event.getRegistry().register(new BlockPanel("panel"));
        event.getRegistry().register(new BlockMillCrops("crop_rice", true, false, new ResourceLocation("millenaire", "rice")));
        event.getRegistry().register(new BlockMillCrops("crop_turmeric", false, false, new ResourceLocation("millenaire", "turmeric")));
        event.getRegistry().register(new BlockMillCrops("crop_maize", false, true, new ResourceLocation("millenaire", "maize")));
        event.getRegistry().register(new BlockGrapeVine("crop_vine", false, false, new ResourceLocation("millenaire", "grapes")));
        event.getRegistry().register(new BlockMillCrops("crop_cotton", true, false, new ResourceLocation("millenaire", "cotton")));
        event.getRegistry().register(new BlockMillPane("paper_wall", Material.WOOD, SoundType.WOOL));
        event.getRegistry().register(new BlockBars("wooden_bars"));
        event.getRegistry().register(new BlockBars("wooden_bars_indian"));
        event.getRegistry().register(new BlockRosetteBars("wooden_bars_rosette", Material.WOOD, SoundType.WOOD));
        Block byzantineTiles = new BlockOrientedSlab.BlockOrientedSlabDouble("byzantine_tiles");
        event.getRegistry().register(byzantineTiles);
        event.getRegistry().register(new BlockOrientedSlab.BlockOrientedSlabSlab("byzantine_tiles_slab"));
        event.getRegistry().register(new BlockOrientedSlabDoubleDecorated("byzantine_stone_tiles"));
        event.getRegistry().register(new BlockOrientedSlabDoubleDecorated("byzantine_sandstone_tiles"));
        BlockMillSandstoneDecorated byzantine_stone_ornament = new BlockMillSandstoneDecorated("byzantine_stone_ornament");
        event.getRegistry().register(byzantine_stone_ornament);
        BlockMillSandstoneDecorated byzantine_sandstone_ornament = new BlockMillSandstoneDecorated("byzantine_sandstone_ornament");
        event.getRegistry().register(byzantine_sandstone_ornament);
        event.getRegistry().register(new BlockMillStairs("stairs_byzantine_tiles", byzantineTiles.defaultBlockState()));
        event.getRegistry().register(new BlockAlchemistExplosive("alchemistexplosive"));
        event.getRegistry().register(new BlockSod("sod"));
        event.getRegistry().register(new BlockCustomIce("icebrick"));
        event.getRegistry().register(new BlockCustomSnow("snowbrick"));
        event.getRegistry().register(new BlockMillWall("snowwall", Blocks.SNOW_BLOCK));
        event.getRegistry().register(new BlockMillBed("bed_straw", 4));
        event.getRegistry().register(new BlockMillBed("bed_charpoy", 4));
        event.getRegistry().register(new BlockImportTable("import_table"));
        event.getRegistry().register(new BlockMillSapling("sapling_appletree", BlockMillSapling.EnumMillWoodType.APPLETREE));
        event.getRegistry().register(new BlockFruitLeaves("leaves_appletree", BlockMillSapling.EnumMillWoodType.APPLETREE, new ResourceLocation("millenaire", "sapling_appletree"), new ResourceLocation("millenaire", "ciderapple")));
        event.getRegistry().register(new BlockMillSapling("sapling_olivetree", BlockMillSapling.EnumMillWoodType.OLIVETREE));
        event.getRegistry().register(new BlockFruitLeaves("leaves_olivetree", BlockMillSapling.EnumMillWoodType.OLIVETREE, new ResourceLocation("millenaire", "sapling_olivetree"), new ResourceLocation("millenaire", "olives")));
        event.getRegistry().register(new BlockMillSapling("sapling_pistachio", BlockMillSapling.EnumMillWoodType.PISTACHIO));
        event.getRegistry().register(new BlockFruitLeaves("leaves_pistachio", BlockMillSapling.EnumMillWoodType.PISTACHIO, new ResourceLocation("millenaire", "sapling_pistachio"), new ResourceLocation("millenaire", "pistachios")));
        event.getRegistry().register(new BlockMillSapling("sapling_cherry", BlockMillSapling.EnumMillWoodType.CHERRY));
        event.getRegistry().register(new BlockFruitLeaves("cherry_leaves", BlockMillSapling.EnumMillWoodType.CHERRY, new ResourceLocation("millenaire", "sapling_cherry"), new ResourceLocation("millenaire", "cherries")));
        event.getRegistry().register(new BlockMillSapling("sapling_sakura", BlockMillSapling.EnumMillWoodType.SAKURA));
        event.getRegistry().register(new BlockFruitLeaves("sakura_leaves", BlockMillSapling.EnumMillWoodType.SAKURA, new ResourceLocation("millenaire", "sapling_sakura"), new ResourceLocation("millenaire", "cherry_blossom")));
        event.getRegistry().register(new MockBlockMarker("markerblock"));
        event.getRegistry().register(new MockBlockMainChest("mainchest"));
        event.getRegistry().register(new MockBlockAnimalSpawn("animalspawn"));
        event.getRegistry().register(new MockBlockSource("source"));
        event.getRegistry().register(new MockBlockFree("freeblock"));
        event.getRegistry().register(new MockBlockTreeSpawn("treespawn"));
        event.getRegistry().register(new MockBlockSoil("soil"));
        event.getRegistry().register(new MockBlockDecor("decorblock"));
        MockBlockBannerHanging villageBannerWall = new MockBlockBannerHanging(ItemMockBanner.BANNER_VILLAGE);
        villageBannerWall.setRegistryName("villagebannerwall");
        event.getRegistry().register(villageBannerWall);
        MockBlockBannerStanding villageBannerStanding = new MockBlockBannerStanding(ItemMockBanner.BANNER_VILLAGE);
        villageBannerStanding.setRegistryName("villagebannerstanding");
        event.getRegistry().register(villageBannerStanding);
        MockBlockBannerHanging cultureBannerWall = new MockBlockBannerHanging(ItemMockBanner.BANNER_CULTURE);
        cultureBannerWall.setRegistryName("culturebannerwall");
        event.getRegistry().register(cultureBannerWall);
        MockBlockBannerStanding cultureBannerStanding = new MockBlockBannerStanding(ItemMockBanner.BANNER_CULTURE);
        cultureBannerStanding.setRegistryName("culturebannerstanding");
        event.getRegistry().register(cultureBannerStanding);
        event.getRegistry().register(new BlockFirePit("fire_pit"));
        Block grayTiles = new BlockOrientedSlab.BlockOrientedSlabDouble("gray_tiles");
        Block greenTiles = new BlockOrientedSlab.BlockOrientedSlabDouble("green_tiles");
        Block redTiles = new BlockOrientedSlab.BlockOrientedSlabDouble("red_tiles");
        event.getRegistry().register(grayTiles);
        event.getRegistry().register(greenTiles);
        event.getRegistry().register(redTiles);
        event.getRegistry().register(new BlockOrientedSlab.BlockOrientedSlabSlab("gray_tiles_slab"));
        event.getRegistry().register(new BlockOrientedSlab.BlockOrientedSlabSlab("green_tiles_slab"));
        event.getRegistry().register(new BlockOrientedSlab.BlockOrientedSlabSlab("red_tiles_slab"));
        event.getRegistry().register(new BlockMillStairs("stairs_gray_tiles", grayTiles.defaultBlockState()));
        event.getRegistry().register(new BlockMillStairs("stairs_green_tiles", greenTiles.defaultBlockState()));
        event.getRegistry().register(new BlockMillStairs("stairs_red_tiles", redTiles.defaultBlockState()));
        event.getRegistry().register(new BlockBars("wooden_bars_dark"));
        event.getRegistry().register(new BlockMillStatue("inuitcarving", SoundType.SNOW, Material.ICE));
    }

    @OnlyIn(value=Dist.CLIENT)
    public static void registerItemBlockModels() {
        WALL_MUD_BRICK.initModel();
        for (Map<DyeColor, Block> blockMap : PAINTED_BRICK_MAP.values()) {
            for (Block block : blockMap.values()) {
                ((IPaintedBlock)block).initModel();
            }
        }
        STAIRS_COOKEDBRICK.initModel();
        STAIRS_MUDBRICK.initModel();
        STAIRS_TIMBERFRAME.initModel();
        STAIRS_THATCH.initModel();
        STAIRS_BYZ_TILES.initModel();
        STAINED_GLASS.initModel();
        ROSETTE.initModel();
        WET_BRICK.initModel();
        SILK_WORM.initModel();
        SNAIL_SOIL.initModel();
        SANDSTONE_CARVED.initModel();
        SANDSTONE_RED_CARVED.initModel();
        SANDSTONE_OCHRE_CARVED.initModel();
        STAIRS_SANDSTONE_CARVED.initModel();
        STAIRS_SANDSTONE_RED_CARVED.initModel();
        STAIRS_SANDSTONE_OCHRE_CARVED.initModel();
        SLAB_SANDSTONE_CARVED.initModel();
        SLAB_SANDSTONE_RED_CARVED.initModel();
        SLAB_SANDSTONE_OCHRE_CARVED.initModel();
        WALL_SANDSTONE_CARVED.initModel();
        WALL_SANDSTONE_RED_CARVED.initModel();
        WALL_SANDSTONE_OCHRE_CARVED.initModel();














        PAPER_WALL.initModel();
        WOODEN_BARS.initModel();
        WOODEN_BARS_INDIAN.initModel();
        WOODEN_BARS_ROSETTE.initModel();

        BYZANTINE_STONE_ORNAMENT.initModel();
        BYZANTINE_SANDSTONE_ORNAMENT.initModel();

        ALCHEMIST_EXPLOSIVE.initModel();
        INUIT_CARVING.initModel();
        SNOW_WALL.initModel();
        BED_STRAW.initModel();
        BED_CHARPOY.initModel();
        IMPORT_TABLE.initModel();
        MARKER_BLOCK.initModel();
        MAIN_CHEST.initModel();
        ANIMAL_SPAWN.initModel();
        SOURCE.initModel();
        FREE_BLOCK.initModel();
        TREE_SPAWN.initModel();
        SOIL_BLOCK.initModel();
        DECOR_BLOCK.initModel();
        FIRE_PIT.initModel();
        SAPLING_APPLETREE.initModel();
        LEAVES_APPLETREE.initModel();
        SAPLING_OLIVETREE.initModel();
        LEAVES_OLIVETREE.initModel();
        SAPLING_PISTACHIO.initModel();
        LEAVES_PISTACHIO.initModel();
        STAIRS_GRAY_TILES.initModel();
        STAIRS_GREEN_TILES.initModel();
        STAIRS_RED_TILES.initModel();

        WOODEN_BARS_DARK.initModel();
        SAPLING_CHERRY.initModel();
        CHERRY_LEAVES.initModel();
        SAPLING_SAKURA.initModel();
        SAKURA_LEAVES.initModel();
    }

    public static void registerItemBlocks(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new ItemBlockMeta((Block)WOOD_DECORATION).setRegistryName(WOOD_DECORATION.getRegistryName()));
        event.getRegistry().register(new ItemBlockMeta((Block)STONE_DECORATION).setRegistryName(STONE_DECORATION.getRegistryName()));
        event.getRegistry().register(new ItemBlockMeta(EARTH_DECORATION).setRegistryName(EARTH_DECORATION.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)WALL_MUD_BRICK, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(WALL_MUD_BRICK.getRegistryName()));
        for (Map<DyeColor, Block> blockMap : PAINTED_BRICK_MAP.values()) {
            for (Block block : blockMap.values()) {
                if (block instanceof BlockPaintedSlab) {
                    BlockPaintedSlab blockSlab = (BlockPaintedSlab)block;
                    event.getRegistry().register(new ItemHalfSlab(blockSlab).setRegistryName(block.getRegistryName()));
                    continue;
                }
                event.getRegistry().register(new BlockItem(block, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(block.getRegistryName()));
            }
        }
        event.getRegistry().register(new ItemBlockMeta((Block)EXTENDED_MUD_BRICK).setRegistryName(EXTENDED_MUD_BRICK.getRegistryName()));
        event.getRegistry().register(new ItemSlabMeta(SLAB_WOOD_DECORATION, WOOD_DECORATION).setRegistryName(SLAB_WOOD_DECORATION.getRegistryName()));
        event.getRegistry().register(new ItemSlabMeta(SLAB_STONE_DECORATION, STONE_DECORATION).setRegistryName(SLAB_STONE_DECORATION.getRegistryName()));
        event.getRegistry().register(new ItemBlockMeta((Block)STAINED_GLASS).setRegistryName(STAINED_GLASS.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)ROSETTE, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(ROSETTE.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)STAIRS_MUDBRICK, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(STAIRS_MUDBRICK.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)STAIRS_TIMBERFRAME, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(STAIRS_TIMBERFRAME.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)STAIRS_THATCH, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(STAIRS_THATCH.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)STAIRS_BYZ_TILES, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(STAIRS_BYZ_TILES.getRegistryName()));
        event.getRegistry().register(new ItemBlockMeta(WET_BRICK).setRegistryName(WET_BRICK.getRegistryName()));
        event.getRegistry().register(new ItemBlockMeta(SILK_WORM).setRegistryName(SILK_WORM.getRegistryName()));
        event.getRegistry().register(new ItemBlockMeta(SNAIL_SOIL).setRegistryName(SNAIL_SOIL.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)SANDSTONE_CARVED, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(SANDSTONE_CARVED.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)SANDSTONE_RED_CARVED, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(SANDSTONE_RED_CARVED.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)SANDSTONE_OCHRE_CARVED, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(SANDSTONE_OCHRE_CARVED.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)STAIRS_SANDSTONE_CARVED, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(STAIRS_SANDSTONE_CARVED.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)STAIRS_SANDSTONE_RED_CARVED, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(STAIRS_SANDSTONE_RED_CARVED.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)STAIRS_SANDSTONE_OCHRE_CARVED, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(STAIRS_SANDSTONE_OCHRE_CARVED.getRegistryName()));
        event.getRegistry().register(new ItemHalfSlab(SLAB_SANDSTONE_CARVED).setRegistryName(SLAB_SANDSTONE_CARVED.getRegistryName()));
        event.getRegistry().register(new ItemHalfSlab(SLAB_SANDSTONE_RED_CARVED).setRegistryName(SLAB_SANDSTONE_RED_CARVED.getRegistryName()));
        event.getRegistry().register(new ItemHalfSlab(SLAB_SANDSTONE_OCHRE_CARVED).setRegistryName(SLAB_SANDSTONE_OCHRE_CARVED.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)WALL_SANDSTONE_CARVED, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(WALL_SANDSTONE_CARVED.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)WALL_SANDSTONE_RED_CARVED, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(WALL_SANDSTONE_RED_CARVED.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)WALL_SANDSTONE_OCHRE_CARVED, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(WALL_SANDSTONE_OCHRE_CARVED.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)PATHDIRT, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(PATHDIRT.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)PATHGRAVEL, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(PATHGRAVEL.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)PATHSANDSTONE, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(PATHSANDSTONE.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)PATHSLABS, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(PATHSLABS.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)PATHGRAVELSLABS, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(PATHGRAVELSLABS.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)PATHOCHRESLABS, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(PATHOCHRESLABS.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)PATHSNOW, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(PATHSNOW.getRegistryName()));
        event.getRegistry().register(new ItemPathSlab(PATHDIRT_SLAB, PATHDIRT).setRegistryName(PATHDIRT_SLAB.getRegistryName()));
        event.getRegistry().register(new ItemPathSlab(PATHGRAVEL_SLAB, PATHGRAVEL).setRegistryName(PATHGRAVEL_SLAB.getRegistryName()));
        event.getRegistry().register(new ItemPathSlab(PATHSANDSTONE_SLAB, PATHSANDSTONE).setRegistryName(PATHSANDSTONE_SLAB.getRegistryName()));
        event.getRegistry().register(new ItemPathSlab(PATHSLABS_SLAB, PATHSLABS).setRegistryName(PATHSLABS_SLAB.getRegistryName()));
        event.getRegistry().register(new ItemPathSlab(PATHGRAVELSLABS_SLAB, PATHGRAVELSLABS).setRegistryName(PATHGRAVELSLABS_SLAB.getRegistryName()));
        event.getRegistry().register(new ItemPathSlab(PATHOCHRESLABS_SLAB, PATHOCHRESLABS).setRegistryName(PATHOCHRESLABS_SLAB.getRegistryName()));
        event.getRegistry().register(new ItemPathSlab(PATHSNOW_SLAB, PATHSNOW).setRegistryName(PATHSNOW_SLAB.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)PAPER_WALL, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(PAPER_WALL.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)WOODEN_BARS, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(WOODEN_BARS.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)WOODEN_BARS_INDIAN, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(WOODEN_BARS_INDIAN.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)WOODEN_BARS_ROSETTE, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(WOODEN_BARS_ROSETTE.getRegistryName()));
        event.getRegistry().register(new org.millenaire.common.item.ItemLockedChest(LOCKED_CHEST).setRegistryName(LOCKED_CHEST.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)BYZANTINE_STONE_TILES, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(BYZANTINE_STONE_TILES.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)BYZANTINE_SANDSTONE_TILES, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(BYZANTINE_SANDSTONE_TILES.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)BYZANTINE_STONE_ORNAMENT, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(BYZANTINE_STONE_ORNAMENT.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)BYZANTINE_SANDSTONE_ORNAMENT, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(BYZANTINE_SANDSTONE_ORNAMENT.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)BYZANTINE_TILES, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(BYZANTINE_TILES.getRegistryName()));
        event.getRegistry().register(new ItemSlabMeta(BYZANTINE_TILES_SLAB, BYZANTINE_TILES).setRegistryName(BYZANTINE_TILES_SLAB.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)ALCHEMIST_EXPLOSIVE, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(ALCHEMIST_EXPLOSIVE.getRegistryName()));
        event.getRegistry().register(new ItemBlockMeta(SOD).setRegistryName(SOD.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)ICE_BRICK, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(ICE_BRICK.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)SNOW_BRICK, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(SNOW_BRICK.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)INUIT_CARVING, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(INUIT_CARVING.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)SNOW_WALL, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(SNOW_WALL.getRegistryName()));
        event.getRegistry().register(new ItemMillBed((Block)BED_STRAW).setRegistryName(BED_STRAW.getRegistryName()));
        event.getRegistry().register(new ItemMillBed((Block)BED_CHARPOY).setRegistryName(BED_CHARPOY.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)IMPORT_TABLE, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(IMPORT_TABLE.getRegistryName()));
        event.getRegistry().register(new ItemMillSapling((Block)SAPLING_APPLETREE, "sapling_appletree").setRegistryName(SAPLING_APPLETREE.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)LEAVES_APPLETREE, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(LEAVES_APPLETREE.getRegistryName()));
        event.getRegistry().register(new ItemMillSapling((Block)SAPLING_OLIVETREE, "sapling_olivetree").setRegistryName(SAPLING_OLIVETREE.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)LEAVES_OLIVETREE, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(LEAVES_OLIVETREE.getRegistryName()));
        event.getRegistry().register(new ItemMillSapling((Block)SAPLING_PISTACHIO, "sapling_pistachio").setRegistryName(SAPLING_PISTACHIO.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)LEAVES_PISTACHIO, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(LEAVES_PISTACHIO.getRegistryName()));
        event.getRegistry().register(new ItemBlockMeta(MARKER_BLOCK).setRegistryName(MARKER_BLOCK.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)MAIN_CHEST, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(MAIN_CHEST.getRegistryName()));
        event.getRegistry().register(new ItemBlockMeta(ANIMAL_SPAWN).setRegistryName(ANIMAL_SPAWN.getRegistryName()));
        event.getRegistry().register(new ItemBlockMeta(SOURCE).setRegistryName(SOURCE.getRegistryName()));
        event.getRegistry().register(new ItemBlockMeta(FREE_BLOCK).setRegistryName(FREE_BLOCK.getRegistryName()));
        event.getRegistry().register(new ItemBlockMeta(TREE_SPAWN).setRegistryName(TREE_SPAWN.getRegistryName()));
        event.getRegistry().register(new ItemBlockMeta(SOIL_BLOCK).setRegistryName(SOIL_BLOCK.getRegistryName()));
        event.getRegistry().register(new ItemBlockMeta(DECOR_BLOCK).setRegistryName(DECOR_BLOCK.getRegistryName()));
        event.getRegistry().register(new ItemMockBanner(VILLAGE_BANNER_STANDING, VILLAGE_BANNER_WALL, ItemMockBanner.BANNER_COLOURS[ItemMockBanner.BANNER_VILLAGE], ItemMockBanner.BANNER_VILLAGE).setRegistryName("villagebanner"));
        event.getRegistry().register(new ItemMockBanner(CULTURE_BANNER_STANDING, CULTURE_BANNER_WALL, ItemMockBanner.BANNER_COLOURS[ItemMockBanner.BANNER_CULTURE], ItemMockBanner.BANNER_CULTURE).setRegistryName("culturebanner"));
        event.getRegistry().register(new BlockItem((Block)FIRE_PIT, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(FIRE_PIT.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)STAIRS_GRAY_TILES, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(STAIRS_GRAY_TILES.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)STAIRS_GREEN_TILES, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(STAIRS_GREEN_TILES.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)STAIRS_RED_TILES, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(STAIRS_RED_TILES.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)GRAY_TILES, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(GRAY_TILES.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)GREEN_TILES, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(GREEN_TILES.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)RED_TILES, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(RED_TILES.getRegistryName()));
        event.getRegistry().register(new ItemSlabMeta(GRAY_TILES_SLAB, GRAY_TILES).setRegistryName(GRAY_TILES_SLAB.getRegistryName()));
        event.getRegistry().register(new ItemSlabMeta(GREEN_TILES_SLAB, GREEN_TILES).setRegistryName(GREEN_TILES_SLAB.getRegistryName()));
        event.getRegistry().register(new ItemSlabMeta(RED_TILES_SLAB, RED_TILES).setRegistryName(RED_TILES_SLAB.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)WOODEN_BARS_DARK, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(WOODEN_BARS_DARK.getRegistryName()));
        event.getRegistry().register(new ItemMillSapling((Block)SAPLING_CHERRY, "sapling_cherry").setRegistryName(SAPLING_CHERRY.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)CHERRY_LEAVES, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(CHERRY_LEAVES.getRegistryName()));
        event.getRegistry().register(new ItemMillSapling((Block)SAPLING_SAKURA, "sapling_sakura").setRegistryName(SAPLING_SAKURA.getRegistryName()));
        event.getRegistry().register(new BlockItem((Block)SAKURA_LEAVES, new net.minecraft.world.item.Item.Properties().tab(tabMillenaire)).setRegistryName(SAKURA_LEAVES.getRegistryName()));
    }

    static {
        PAINTED_BRICK_MAP = new HashMap<String, Map<DyeColor, Block>>();
        tabMillenaire = new CreativeModeTab("millenaire"){

            public ItemStack makeIcon() {
                return new ItemStack((Item)MillItems.DENIER_OR, 1);
            }
        };
        tabMillenaireContentCreator = new CreativeModeTab("millenaire_content_creator"){

            public ItemStack makeIcon() {
                return new ItemStack((Block)DECOR_BLOCK, 1);
            }
        };
    }

    public static class MillBlockNames {
        private static final String WOOD_DECO = "wood_deco";
        private static final String STONE_DECO = "stone_deco";
        private static final String EARTH_DECO = "earth_deco";
        public static final String PAINTED_BRICK = "painted_brick";
        public static final String PAINTED_BRICK_DECORATED = "painted_brick_decorated";
        public static final String STAIRS_PAINTED_BRICK = "stairs_painted_brick";
        public static final String SLAB_PAINTED_BRICK = "slab_painted_brick";
        public static final String WALL_PAINTED_BRICK = "wall_painted_brick";
        private static final String EXTENDED_MUD_BRICK = "extended_mud_brick";
        private static final String SLAB_WOOD_DECO = "slab_wood_deco";
        private static final String SLAB_STONE_DECO = "slab_stone_deco";
        private static final String WALL_MUD_BRICK = "wall_mud_brick";
        private static final String WET_BRICK = "wet_brick";
        private static final String SILK_WORM = "silk_worm";
        private static final String SNAIL_SOIL = "snail_soil";
        private static final String PATHDIRT = "pathdirt";
        private static final String PATHGRAVEL = "pathgravel";
        private static final String PATHSLABS = "pathslabs";
        private static final String PATHSANDSTONE = "pathsandstone";
        private static final String PATHGRAVELSLABS = "pathgravelslabs";
        private static final String PATHOCHRETILES = "pathochretiles";
        private static final String PATHSNOW = "pathsnow";
        private static final String LOCKED_CHEST = "locked_chest";
        private static final String PANEL = "panel";
        private static final String CROP_RICE = "crop_rice";
        private static final String CROP_TURMERIC = "crop_turmeric";
        private static final String CROP_MAIZE = "crop_maize";
        private static final String CROP_VINE = "crop_vine";
        private static final String CROP_COTTON = "crop_cotton";
        private static final String STAINED_GLASS = "stained_glass";
        private static final String ROSETTE = "rosette";
        private static final String PAPER_WALL = "paper_wall";
        private static final String WOODEN_BARS = "wooden_bars";
        private static final String WOODEN_BARS_INDIAN = "wooden_bars_indian";
        private static final String WOODEN_BARS_ROSETTE = "wooden_bars_rosette";
        private static final String BYZ_TILES = "byzantine_tiles";
        private static final String BYZ_TILES_SLAB = "byzantine_tiles_slab";
        private static final String BYZ_STONE_TILES = "byzantine_stone_tiles";
        private static final String BYZ_SANDSTONE_TILES = "byzantine_sandstone_tiles";
        private static final String BYZ_STONE_ORNAMENT = "byzantine_stone_ornament";
        private static final String BYZ_SANDSTONE_ORNAMENT = "byzantine_sandstone_ornament";
        private static final String ALCHEMIST_EXPLOSIVE = "alchemistexplosive";
        private static final String MOCK_BLOCK_MARKER = "markerblock";
        private static final String MAIN_CHEST = "mainchest";
        private static final String ANIMAL_SPAWN = "animalspawn";
        private static final String SOURCE = "source";
        private static final String FREE_BLOCK = "freeblock";
        private static final String TREE_SPAWN = "treespawn";
        private static final String SOIL_BLOCK = "soil";
        private static final String DECOR_BLOCK = "decorblock";
        private static final String VILLAGE_BANNER_WALL = "villagebannerwall";
        private static final String VILLAGE_BANNER_STANDING = "villagebannerstanding";
        private static final String CULTURE_BANNER_WALL = "culturebannerwall";
        private static final String CULTURE_BANNER_STANDING = "culturebannerstanding";
        private static final String MOCK_BANNER = "mockbanner";
        private static final String IMPORT_TABLE = "import_table";
        private static final String STAIRS_TIMBERFRAME = "stairs_timberframe";
        private static final String STAIRS_MUDBRICK = "stairs_mudbrick";
        private static final String STAIRS_COOKEDBRICK = "stairs_cookedbrick";
        private static final String STAIRS_THATCH = "stairs_thatch";
        private static final String STAIRS_BYZ_TILES = "stairs_byzantine_tiles";
        public static final String SANDSTONE_CARVED = "sandstone_carved";
        public static final String SANDSTONE_RED_CARVED = "sandstone_red_carved";
        public static final String SANDSTONE_OCHRE_CARVED = "sandstone_ochre_carved";
        private static final String STAIRS_SANDSTONE_CARVED = "stairs_sandstone_carved";
        private static final String STAIRS_SANDSTONE_RED_CARVED = "stairs_sandstone_red_carved";
        private static final String STAIRS_SANDSTONE_OCHRE_CARVED = "stairs_sandstone_ochre_carved";
        private static final String SLAB_SANDSTONE_CARVED = "slab_sandstone_carved";
        private static final String SLAB_SANDSTONE_RED_CARVED = "slab_sandstone_red_carved";
        private static final String SLAB_SANDSTONE_OCHRE_CARVED = "slab_sandstone_ochre_carved";
        private static final String WALL_SANDSTONE_CARVED = "wall_sandstone_carved";
        private static final String WALL_SANDSTONE_RED_CARVED = "wall_sandstone_red_carved";
        private static final String WALL_SANDSTONE_OCHRE_CARVED = "wall_sandstone_ochre_carved";
        private static final String SOD_BLOCK = "sod";
        private static final String ICE_BRICK_BLOCK = "icebrick";
        private static final String SNOW_BRICK_BLOCK = "snowbrick";
        private static final String INUIT_CARVING = "inuitcarving";
        private static final String SNOW_WALL = "snowwall";
        private static final String BED_STRAW = "bed_straw";
        private static final String BED_CHARPOY = "bed_charpoy";
        private static final String FIRE_PIT = "fire_pit";
        private static final String SAPLING_APPLETREE = "sapling_appletree";
        private static final String LEAVES_APPLETREE = "leaves_appletree";
        private static final String SAPLING_OLIVETREE = "sapling_olivetree";
        private static final String LEAVES_OLIVETREE = "leaves_olivetree";
        private static final String SAPLING_PISTACHIO = "sapling_pistachio";
        private static final String LEAVES_PISTACHIO = "leaves_pistachio";
        private static final String GREEN_TILES = "green_tiles";
        private static final String GRAY_TILES = "gray_tiles";
        private static final String RED_TILES = "red_tiles";
        private static final String GREEN_TILES_SLAB = "green_tiles_slab";
        private static final String GRAY_TILES_SLAB = "gray_tiles_slab";
        private static final String RED_TILES_SLAB = "red_tiles_slab";
        private static final String STAIRS_GRAY_TILES = "stairs_gray_tiles";
        private static final String STAIRS_GREEN_TILES = "stairs_green_tiles";
        private static final String STAIRS_RED_TILES = "stairs_red_tiles";
        private static final String WOODEN_BARS_DARK = "wooden_bars_dark";
        private static final String SAPLING_CHERRY = "sapling_cherry";
        private static final String CHERRY_LEAVES = "cherry_leaves";
        private static final String SAPLING_SAKURA = "sapling_sakura";
        private static final String SAKURA_LEAVES = "sakura_leaves";
    }
}

