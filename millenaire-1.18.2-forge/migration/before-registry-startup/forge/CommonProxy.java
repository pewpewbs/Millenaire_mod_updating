/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockLeaves
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.Item
 *  net.minecraft.util.text.TextFormatting
 *  net.minecraftforge.event.RegistryEvent$Register
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.eventhandler.SubscribeEvent
 *  net.minecraftforge.fml.common.network.IGuiHandler
 */
package org.millenaire.common.forge;

import java.io.File;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.ChatFormatting;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.forge.ServerGuiHandler;
import org.millenaire.common.item.MillItems;
import org.millenaire.common.network.ServerReceiver;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.world.UserProfile;

@Mod.EventBusSubscriber(modid="millenaire", bus=Mod.EventBusSubscriber.Bus.MOD)
public class CommonProxy {
    public void executeWorldTask(net.minecraft.world.level.Level world, Runnable action) {
        if (world instanceof net.minecraft.server.level.ServerLevel server) {
            if (server.getServer().isSameThread()) action.run();
            else server.getServer().execute(action);
        }
    }

    /** Snapshot the loaded entities; rebuilds may schedule removals after walking the list. */
    public java.util.List<net.minecraft.world.entity.Entity> loadedEntities(net.minecraft.world.level.Level world) {
        java.util.List<net.minecraft.world.entity.Entity> entities = new java.util.ArrayList<>();
        if (world instanceof net.minecraft.server.level.ServerLevel server) server.getAllEntities().forEach(entities::add);
        return entities;
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        MillBlocks.registerBlocks(event);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        MillBlocks.registerItemBlocks(event);
        MillItems.registerItems(event);
    }

    public ServerGuiHandler createGuiHandler() {
        return new ServerGuiHandler();
    }

    public String getBlockName(Block block, int meta) {
        return null;
    }

    public UserProfile getClientProfile() {
        return null;
    }

    public File getConfigFile() {
        return new File(MillCommonUtilities.getMillenaireContentDir(), "config-server.txt");
    }

    public File getCustomConfigFile() {
        return new File(MillCommonUtilities.getMillenaireCustomContentDir(), "config-server-custom.txt");
    }

    public String getItemName(Item item, int meta) {
        return "";
    }

    public String getKeyString(int value) {
        return "";
    }

    public File getLogFile() {
        return new File(MillCommonUtilities.getMillenaireCustomContentDir(), "millenaire-server.log");
    }

    public String getQuestKeyName() {
        return "";
    }

    public String getSinglePlayerName() {
        return null;
    }

    public Player getTheSinglePlayer() {
        return null;
    }

    public void handleClientGameUpdate() {
    }

    public void handleClientLogin() {
    }

    public void initNetwork() {
    }

    public boolean isTrueServer() {
        return true;
    }

    public int loadKeySetting(String value) {
        return 0;
    }

    public void loadLanguagesIfNeeded() {
        LanguageUtilities.loadLanguages(null);
    }

    public void localTranslatedSentence(Player player, char colour, String code, String ... values) {
    }

    public String logPrefix() {
        return "SRV ";
    }

    public void refreshClientResources() {
    }

    public void registerForgeClientClasses() {
    }

    public void registerKeyBindings() {
    }

    public void sendChatAdmin(String s) {
    }

    public void sendChatAdmin(String s, ChatFormatting colour) {
    }

    public void sendLocalChat(Player player, char colour, String s) {
    }

    public net.minecraft.world.entity.Entity getEntityByUUID(net.minecraft.world.level.Level world,java.util.UUID id) {
        return world instanceof net.minecraft.server.level.ServerLevel server?server.getEntity(id):null;
    }
}

