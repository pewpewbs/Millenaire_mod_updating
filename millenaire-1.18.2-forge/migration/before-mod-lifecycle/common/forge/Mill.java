/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.command.ICommand
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.BannerPattern
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.SoundEvent
 *  net.minecraft.world.World
 *  net.minecraft.world.biome.Biome
 *  net.minecraft.world.gen.structure.MapGenVillage
 *  net.minecraftforge.common.ForgeChunkManager
 *  net.minecraftforge.common.ForgeChunkManager$LoadingCallback
 *  net.minecraftforge.common.MinecraftForge
 *  net.minecraftforge.common.util.EnumHelper
 *  net.minecraftforge.fml.common.FMLCommonHandler
 *  net.minecraftforge.fml.common.IWorldGenerator
 *  net.minecraftforge.fml.common.Mod
 *  net.minecraftforge.fml.common.Mod$EventHandler
 *  net.minecraftforge.fml.common.Mod$Instance
 *  net.minecraftforge.fml.common.ProgressManager
 *  net.minecraftforge.fml.common.ProgressManager$ProgressBar
 *  net.minecraftforge.fml.common.SidedProxy
 *  net.minecraftforge.fml.common.event.FMLInitializationEvent
 *  net.minecraftforge.fml.common.event.FMLPostInitializationEvent
 *  net.minecraftforge.fml.common.event.FMLPreInitializationEvent
 *  net.minecraftforge.fml.common.event.FMLServerStartingEvent
 *  net.minecraftforge.fml.common.network.FMLEventChannel
 *  net.minecraftforge.fml.common.network.NetworkRegistry
 *  net.minecraftforge.fml.common.registry.EntityRegistry
 *  net.minecraftforge.fml.common.registry.GameRegistry
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package org.millenaire.common.forge;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.Mod;
import java.util.function.Consumer;
import net.minecraftforge.fml.loading.progress.StartupMessageManager;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraft.core.Registry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.millenaire.common.advancements.MillAdvancements;
import org.millenaire.common.annotedparameters.ParametersManager;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.buildingplan.BuildingDevUtilities;
import org.millenaire.common.buildingplan.BuildingPlan;
import org.millenaire.common.commands.CommandDebugResendProfiles;
import org.millenaire.common.commands.CommandDebugResetVillagers;
import org.millenaire.common.commands.CommandGiveReputation;
import org.millenaire.common.commands.CommandImportCulture;
import org.millenaire.common.commands.CommandListActiveVillages;
import org.millenaire.common.commands.CommandRenameVillage;
import org.millenaire.common.commands.CommandSpawnVillage;
import org.millenaire.common.commands.CommandSwitchVillageControl;
import org.millenaire.common.commands.CommandTeleportToVillage;
import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.culture.Culture;
import org.millenaire.common.deployer.ContentDeployer;
import org.millenaire.common.entity.EntityTargetedBlaze;
import org.millenaire.common.entity.EntityTargetedGhast;
import org.millenaire.common.entity.EntityTargetedWitherSkeleton;
import org.millenaire.common.entity.EntityWallDecoration;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.entity.VillagerConfig;
import org.millenaire.common.forge.BuildingChunkLoader;
import org.millenaire.common.forge.CommonProxy;
import org.millenaire.common.forge.MillEventController;
import org.millenaire.common.forge.ServerTickHandler;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.item.MillItems;
import org.millenaire.common.network.ServerReceiver;
import org.millenaire.common.quest.Quest;
import org.millenaire.common.utilities.BlockItemUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.virtualdir.VirtualDir;
import org.millenaire.common.world.MillWorldData;
import org.millenaire.common.world.WorldGenVillage;

@Mod(modid="millenaire", name="Mill\u00e9naire", version="Mill\u00e9naire 8.1.2", useMetadata=true)
public class Mill {
    public static final String MODID = "millenaire";
    public static final String MODNAME = "Mill\u00e9naire";
    public static final String VERSION_NUMBER = "8.1.2";
    public static final String MINECRAFT_VERSION_NUMBER = "1.12.2";
    public static final String VERSION = "Mill\u00e9naire 8.1.2";
    public static final Logger LOGGER = LogManager.getLogger((String)"millenaire");
    @SidedProxy(clientSide="org.millenaire.client.forge.ClientProxy", serverSide="org.millenaire.common.forge.CommonProxy")
    public static CommonProxy proxy;
    @Mod.Instance
    public static Mill instance;
    public static List<MillWorldData> serverWorlds;
    public static MillWorldData clientWorld;
    public static List<File> loadingDirs;
    public static VirtualDir virtualLoadingDir;
    public static boolean loadingComplete;
    public static final ResourceLocation ENTITY_PIG;
    public static final ResourceLocation ENTITY_COW;
    public static final ResourceLocation ENTITY_CHICKEN;
    public static final ResourceLocation ENTITY_SHEEP;
    public static final ResourceLocation ENTITY_SQUID;
    public static final ResourceLocation ENTITY_WOLF;
    public static final ResourceLocation ENTITY_POLAR_BEAR;
    public static final ResourceLocation ENTITY_SKELETON;
    public static final ResourceLocation ENTITY_CREEPER;
    public static final ResourceLocation ENTITY_SPIDER;
    public static final ResourceLocation ENTITY_CAVESPIDER;
    public static final ResourceLocation ENTITY_ZOMBIE;
    public static final ResourceLocation ENTITY_TARGETED_GHAST;
    public static final ResourceLocation ENTITY_TARGETED_BLAZE;
    public static final ResourceLocation ENTITY_TARGETED_WITHERSKELETON;
    public static final ResourceLocation CROP_WHEAT;
    public static final ResourceLocation CROP_CARROT;
    public static final ResourceLocation CROP_POTATO;
    public static final ResourceLocation CROP_RICE;
    public static final ResourceLocation CROP_TURMERIC;
    public static final ResourceLocation CROP_MAIZE;
    public static final ResourceLocation CROP_VINE;
    public static final ResourceLocation CROP_CACAO;
    public static final ResourceLocation CROP_FLOWER;
    public static final ResourceLocation CROP_COTTON;
    public static boolean startupError;
    public static boolean checkedMillenaireDir;
    public static boolean displayMillenaireLocationError;
    public static SoundEvent SOUND_NORMAN_BELLS;
    static final Class[] BANNER_CLASSES;
    public static final String[] BANNER_SHORTNAMES;

    public static MillWorldData getMillWorld(Level world) {
        if (clientWorld != null && Mill.clientWorld.world == world) {
            return clientWorld;
        }
        for (MillWorldData mw : serverWorlds) {
            if (mw.world != world) continue;
            return mw;
        }
        if (serverWorlds != null && serverWorlds.size() > 0) {
            return serverWorlds.get(0);
        }
        return null;
    }

    public static boolean isDistantClient() {
        return clientWorld != null && serverWorlds.isEmpty();
    }

    private void addBannerPatterns() {
        org.millenaire.common.item.MillBannerPatterns.initialize();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        if (startupError) {
            return;
        }
        if (MillConfigValues.stopDefaultVillages) {
            MapGenVillage.field_75055_e = Arrays.asList(new Biome[0]);
        }
        MillBlocks.initBlockStates();
        boolean error = false;
        Consumer<String> bar = StartupMessageManager::addModMessage;
        bar.accept("Items...");
        if (!error) {
            InvItem.loadItemList();
        }
        bar.accept("Building points...");
        if (!error) {
            error = BuildingPlan.loadBuildingPoints();
        }
        bar.accept("Villager configs...");
        if (!error) {
            VillagerConfig.loadConfigs();
        }
        bar.accept("Villager goals...");
        if (!error) {
            Goal.initGoals();
        }
        if (!error) {
            error = Culture.loadCultures(bar);
        }
        bar.accept("Quests...");
        if (!error) {
            Quest.loadQuests();
        }
        if (MillConfigValues.generateHelpData) {
            ParametersManager.generateHelpFiles();
            DocumentedElement.generateHelpFiles();
        }
        bar.accept("Registering entities & items...");
        startupError = error;
        int id = 1;
        GameRegistry.addSmelting((ItemStack)org.millenaire.common.item.LegacyItemStacks.stack((Block)MillBlocks.STONE_DECORATION, 1, 0), (ItemStack)org.millenaire.common.item.LegacyItemStacks.stack((Block)MillBlocks.PAINTED_BRICK_WHITE, 1, 0), (float)1.0f);
        GameRegistry.addSmelting((ItemStack)org.millenaire.common.item.LegacyItemStacks.stack((Item)MillItems.BEARMEAT_RAW, 1, 0), (ItemStack)org.millenaire.common.item.LegacyItemStacks.stack((Item)MillItems.BEARMEAT_COOKED, 1, 0), (float)1.0f);
        GameRegistry.addSmelting((ItemStack)org.millenaire.common.item.LegacyItemStacks.stack((Item)MillItems.WOLFMEAT_RAW, 1, 0), (ItemStack)org.millenaire.common.item.LegacyItemStacks.stack((Item)MillItems.WOLFMEAT_COOKED, 1, 0), (float)1.0f);
        GameRegistry.addSmelting((ItemStack)org.millenaire.common.item.LegacyItemStacks.stack((Item)MillItems.SEAFOOD_RAW, 1, 0), (ItemStack)org.millenaire.common.item.LegacyItemStacks.stack((Item)MillItems.SEAFOOD_COOKED, 1, 0), (float)1.0f);
        ResourceLocation location = new ResourceLocation(MODID, "norman_bells");
        SOUND_NORMAN_BELLS = new SoundEvent(location);
        loadingComplete = true;
        if (MillConfigValues.LogOther >= 1) {
            if (startupError) {
                MillLog.major(this, "Mill\u00e9naire Mill\u00e9naire 8.1.2 loaded unsuccessfully.");
            } else {
                MillLog.major(this, "Mill\u00e9naire Mill\u00e9naire 8.1.2 loaded successfully.");
            }
        }
        FMLCommonHandler.instance().bus().register((Object)new ServerTickHandler());
        FMLCommonHandler.instance().bus().register((Object)this);
        proxy.registerForgeClientClasses();
        proxy.registerKeyBindings();
        NetworkRegistry.INSTANCE.registerGuiHandler((Object)instance, proxy.createGuiHandler());
        GameRegistry.registerWorldGenerator((IWorldGenerator)new WorldGenVillage(), (int)1000);
        MinecraftForge.EVENT_BUS.register((Object)new MillEventController());
        proxy.loadLanguagesIfNeeded();
        if (MillConfigValues.DEV && !proxy.isTrueServer()) {
            BuildingDevUtilities.exportMissingTravelBookDesc();
            BuildingDevUtilities.exportTravelBookDescCSV();
        }
        this.addBannerPatterns();
        bar.accept(startupError ? "Millénaire data loading failed" : "Millénaire data loaded");
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent e) {
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ContentDeployer.deployContent(event.getSourceFile());
        MillConfigValues.initConfig();
        proxy.refreshClientResources();
        BlockItemUtilities.initBlockTypes();
    }



    static {
        serverWorlds = new ArrayList<MillWorldData>();
        clientWorld = null;
        loadingDirs = new ArrayList<File>();
        loadingComplete = false;
        ENTITY_PIG = new ResourceLocation("pig");
        ENTITY_COW = new ResourceLocation("cow");
        ENTITY_CHICKEN = new ResourceLocation("chicken");
        ENTITY_SHEEP = new ResourceLocation("sheep");
        ENTITY_SQUID = new ResourceLocation("squid");
        ENTITY_WOLF = new ResourceLocation("wolf");
        ENTITY_POLAR_BEAR = new ResourceLocation("polar_bear");
        ENTITY_SKELETON = new ResourceLocation("skeleton");
        ENTITY_CREEPER = new ResourceLocation("creeper");
        ENTITY_SPIDER = new ResourceLocation("spider");
        ENTITY_CAVESPIDER = new ResourceLocation("cave_spider");
        ENTITY_ZOMBIE = new ResourceLocation("zombie");
        ENTITY_TARGETED_GHAST = new ResourceLocation(MODID, "millghast");
        ENTITY_TARGETED_BLAZE = new ResourceLocation(MODID, "millblaze");
        ENTITY_TARGETED_WITHERSKELETON = new ResourceLocation(MODID, "millwitherskeleton");
        CROP_WHEAT = new ResourceLocation("wheat");
        CROP_CARROT = new ResourceLocation("carrots");
        CROP_POTATO = new ResourceLocation("potatoes");
        CROP_RICE = new ResourceLocation(MODID, "crop_rice");
        CROP_TURMERIC = new ResourceLocation(MODID, "crop_turmeric");
        CROP_MAIZE = new ResourceLocation(MODID, "crop_maize");
        CROP_VINE = new ResourceLocation(MODID, "crop_vine");
        CROP_CACAO = new ResourceLocation("cocoa");
        CROP_FLOWER = new ResourceLocation("flower");
        CROP_COTTON = new ResourceLocation(MODID, "crop_cotton");
        startupError = false;
        checkedMillenaireDir = false;
        displayMillenaireLocationError = false;
        BANNER_CLASSES = new Class[]{String.class, String.class, ItemStack.class};
        BANNER_SHORTNAMES = new String[]{"byz", "by1", "by2", "sjk", "may", "inu", "ind", "in1", "in2", "in3", "in4", "in5", "nor", "ma1", "ma2", "ma3", "ma4", "iu1", "iu2", "iu3", "iu4", "jap", "jaa", "jam", "jar", "jat", "sjkr", "sjkm"};
    }
}

