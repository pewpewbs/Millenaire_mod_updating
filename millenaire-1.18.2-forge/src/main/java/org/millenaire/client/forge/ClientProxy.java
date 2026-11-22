/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockLeaves
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.color.IBlockColor
 *  net.minecraft.client.renderer.color.IItemColor
 *  net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
 *  net.minecraft.client.settings.KeyBinding
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentString
 *  net.minecraft.util.text.TextFormatting
 *  net.minecraft.world.ColorizerFoliage
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.biome.BiomeColorHelper
 *  net.minecraftforge.client.event.ModelRegistryEvent
 *  net.minecraftforge.fml.client.FMLClientHandler
 *  net.minecraftforge.fml.client.registry.ClientRegistry
 *  net.minecraftforge.fml.client.registry.IRenderFactory
 *  net.minecraftforge.fml.client.registry.RenderingRegistry
 *  net.minecraftforge.fml.common.FMLCommonHandler
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.eventhandler.SubscribeEvent
 *  net.minecraftforge.fml.common.network.IGuiHandler
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 *  org.lwjgl.input.Keyboard
 */
package org.millenaire.client.forge;

import java.io.File;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.millenaire.client.MillClientUtilities;
import org.millenaire.client.forge.ClientGuiHandler;
import org.millenaire.client.forge.ClientTickHandler;
import org.millenaire.client.network.ClientReceiver;
import org.millenaire.client.network.ClientSender;
import org.millenaire.client.render.RenderMillVillager;
import org.millenaire.client.render.RenderWallDecoration;
import org.millenaire.client.render.TESRFirePit;
import org.millenaire.client.render.TESRMockBanner;
import org.millenaire.client.render.TESRPanel;
import org.millenaire.client.render.TileEntityLockedChestRenderer;
import org.millenaire.client.render.TileEntityMillBedRenderer;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.entity.EntityWallDecoration;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.entity.TileEntityFirePit;
import org.millenaire.common.entity.TileEntityLockedChest;
import org.millenaire.common.entity.TileEntityMillBed;
import org.millenaire.common.entity.TileEntityMockBanner;
import org.millenaire.common.entity.TileEntityPanel;
import org.millenaire.common.forge.CommonProxy;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.item.MillItems;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.world.UserProfile;

@Mod.EventBusSubscriber(modid="millenaire", value=Dist.CLIENT, bus=Mod.EventBusSubscriber.Bus.MOD)
@OnlyIn(value=Dist.CLIENT)
public class ClientProxy
extends CommonProxy {
    @Override public void executeWorldTask(net.minecraft.world.level.Level world, Runnable action) {
        if (world instanceof net.minecraft.client.multiplayer.ClientLevel) {
            var client = net.minecraft.client.Minecraft.getInstance();
            if (client.isSameThread()) action.run(); else client.execute(action);
        } else super.executeWorldTask(world, action);
    }

    @Override public java.util.List<net.minecraft.world.entity.Entity> loadedEntities(net.minecraft.world.level.Level world) {
        if (!(world instanceof net.minecraft.client.multiplayer.ClientLevel client)) return super.loadedEntities(world);
        java.util.List<net.minecraft.world.entity.Entity> entities = new java.util.ArrayList<>();
        client.entitiesForRendering().forEach(entities::add);
        return entities;
    }

    public static final KeyMapping KB_MENU = MillKeyBindings.MENU;
    public static final KeyMapping KB_VILLAGES = MillKeyBindings.VILLAGES;
    public static final KeyMapping KB_ESCORTS = MillKeyBindings.ESCORTS;

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        MillItems.registerItemModels();
        MillBlocks.registerItemBlockModels();
    }

    @Override
    public org.millenaire.common.forge.ServerGuiHandler createGuiHandler() {
        return new ClientGuiHandler();
    }

    @Override
    public String getBlockName(Block block, int meta) {
        if (block == null) {
            MillLog.printException(new MillLog.MillenaireException("Trying to get the name of a null block."));
            return null;
        }
        if (meta == -1) {
            meta = 0;
        }
        return org.millenaire.common.item.LegacyItemStacks.stack(block, 1, meta).getHoverName().getString();
    }

    @Override
    public UserProfile getClientProfile() {
        if (Mill.clientWorld == null || Mill.proxy.getTheSinglePlayer() == null) {
            return null;
        }
        if (Mill.clientWorld.profiles.containsKey(Mill.proxy.getTheSinglePlayer().getUUID())) {
            return Mill.clientWorld.profiles.get(Mill.proxy.getTheSinglePlayer().getUUID());
        }
        UserProfile profile = new UserProfile(Mill.clientWorld, Mill.proxy.getTheSinglePlayer());
        Mill.clientWorld.profiles.put(profile.uuid, profile);
        return profile;
    }

    @Override
    public File getConfigFile() {
        return new File(MillCommonUtilities.getMillenaireContentDir(), "config.txt");
    }

    @Override
    public File getCustomConfigFile() {
        return new File(MillCommonUtilities.getMillenaireCustomContentDir(), "config-custom.txt");
    }

    @Override
    public String getItemName(Item item, int meta) {
        if (item == null) {
            MillLog.printException(new MillLog.MillenaireException("Trying to get the name of a null item."));
            return null;
        }
        if (meta == -1) {
            meta = 0;
        }
        return org.millenaire.common.item.LegacyItemStacks.stack(item, 1, meta).getHoverName().getString();
    }

    @Override
    public String getKeyString(int value) {
        return com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(value).getName();
    }

    @Override
    public File getLogFile() {
        return new File(MillCommonUtilities.getMillenaireCustomContentDir(), "millenaire.log");
    }

    @Override
    public String getQuestKeyName() {
        return KB_MENU.getTranslatedKeyMessage().getString();
    }

    @Override
    public String getSinglePlayerName() {
        if (this.getTheSinglePlayer() != null) {
            return this.getTheSinglePlayer().getName().getString();
        }
        return "NULL_PLAYER";
    }

    @Override
    public Player getTheSinglePlayer() {
        return Minecraft.getInstance().player;
    }

    @Override
    public void handleClientGameUpdate() {
        MillClientUtilities.handleKeyPress(Mill.clientWorld.world);
        if (Mill.clientWorld.world.getDayTime() % 20L == 0L) {
            Mill.clientWorld.clearPanelQueue();
        }
        this.loadLanguagesIfNeeded();
    }

    @Override
    public void handleClientLogin() {
        ClientSender.sendVersionInfo();
        ClientSender.sendAvailableContent();
    }

    @Override
    public void initNetwork() {
    }

    @Override
    public boolean isTrueServer() {
        return false;
    }

    @Override
    public int loadKeySetting(String value) {
        return LegacyKeyNames.parse(value);
    }

    @Override
    public void loadLanguagesIfNeeded() {
        Minecraft minecraft = Minecraft.getInstance();
        LanguageUtilities.loadLanguages(minecraft.options.languageCode);
    }

    @Override
    public void localTranslatedSentence(Player player, char colour, String code, String ... values) {
        for (int i = 0; i < values.length; ++i) {
            values[i] = LanguageUtilities.unknownString(values[i]);
        }
        this.sendLocalChat(player, colour, LanguageUtilities.string(code, values));
    }

    @Override
    public String logPrefix() {
        return "CLIENT ";
    }

    @Override
    public void refreshClientResources() {
        Minecraft.getInstance().reloadResourcePacks();
    }

    @SubscribeEvent
    public static void registerBlockColors(net.minecraftforge.client.event.ColorHandlerEvent.Block event) {
        event.getBlockColors().register((state, world, pos, tint) ->
            world != null && pos != null ? BiomeColors.getAverageFoliageColor(world, pos)
                : FoliageColor.getDefaultColor(), MillBlocks.LEAVES_PISTACHIO);
    }

    @SubscribeEvent
    public static void registerItemColors(net.minecraftforge.client.event.ColorHandlerEvent.Item event) {
        event.getItemColors().register((stack, tint) -> FoliageColor.getDefaultColor(),
            MillBlocks.LEAVES_PISTACHIO);
    }

    @Override
    public void registerKeyBindings() {
        MillKeyBindings.register();
    }

    @Override
    public void sendChatAdmin(String s) {
        s = s.trim();
        Minecraft.getInstance().gui.getChat().addMessage((Component)new TextComponent(s));
    }

    @Override
    public void sendChatAdmin(String s, ChatFormatting colour) {
        s = s.trim();
        TextComponent cc = new TextComponent(s);
        cc.withStyle(colour);
        Minecraft.getInstance().gui.getChat().addMessage((Component)cc);
    }

    @Override
    public void sendLocalChat(Player player, char colour, String s) {
        s = s.trim();
        Minecraft.getInstance().gui.getChat().addMessage((Component)new TextComponent("\u00a7" + colour + s));
    }

    @Override public net.minecraft.world.entity.Entity getEntityByUUID(net.minecraft.world.level.Level world,java.util.UUID id) {
        if (world instanceof net.minecraft.client.multiplayer.ClientLevel client)
            for (net.minecraft.world.entity.Entity entity:client.entitiesForRendering()) if(entity.getUUID().equals(id)) return entity;
        return super.getEntityByUUID(world,id);
    }
}

