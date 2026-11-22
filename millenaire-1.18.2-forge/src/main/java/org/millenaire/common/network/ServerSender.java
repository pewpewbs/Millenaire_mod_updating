/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.Unpooled
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.passive.EntityAnimal
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.network.PacketBuffer
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentString
 *  net.minecraft.util.text.TextFormatting
 *  net.minecraft.world.World
 *  net.minecraftforge.fml.common.network.NetworkRegistry$TargetPoint
 *  net.minecraftforge.fml.common.network.internal.FMLProxyPacket
 */
package org.millenaire.common.network;

import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkRegistry;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.entity.TileEntityLockedChest;
import org.millenaire.common.entity.TileEntityPanel;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.network.StreamReadWrite;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;
import org.millenaire.common.world.MillWorldData;

public class ServerSender {
    public static void displayControlledMilitaryGUI(Player player, Building townHall) {
        townHall.sendBuildingPacket(player, false);
        MillWorldData mw = Mill.getMillWorld(player.level);
        for (Point p : townHall.getKnownVillages()) {
            Building b = mw.getBuilding(p);
            if (b == null) continue;
            b.sendBuildingPacket(player, false);
        }
        FriendlyByteBuf data = ServerSender.getPacketBuffer();
        data.writeInt(104);
        data.writeInt(14);
        StreamReadWrite.writeNullablePoint(townHall.getPos(), data);
        ServerSender.sendPacketToPlayer(data, player);
    }

    public static void displayControlledProjectGUI(Player player, Building townHall) {
        townHall.sendBuildingPacket(player, false);
        FriendlyByteBuf data = ServerSender.getPacketBuffer();
        data.writeInt(104);
        data.writeInt(11);
        StreamReadWrite.writeNullablePoint(townHall.getPos(), data);
        ServerSender.sendPacketToPlayer(data, player);
    }

    public static void displayHireGUI(Player player, MillVillager villager) {
        villager.getTownHall().sendBuildingPacket(player, false);
        FriendlyByteBuf data = ServerSender.getPacketBuffer();
        data.writeInt(104);
        data.writeInt(12);
        data.writeLong(villager.getVillagerId());
        ServerSender.sendPacketToPlayer(data, player);
    }

    public static void displayImportTableGUI(Player player, Point tableLocation) {
        FriendlyByteBuf data = ServerSender.getPacketBuffer();
        data.writeInt(104);
        data.writeInt(15);
        StreamReadWrite.writeNullablePoint(tableLocation, data);
        ServerSender.sendPacketToPlayer(data, player);
    }

    public static void displayMerchantTradeGUI(Player player, MillVillager villager) {
        villager.getHouse().sendBuildingPacket(player, true);
        villager.getTownHall().sendBuildingPacket(player, true);
        org.millenaire.common.ui.ContainerTrade.open(player, villager);
    }

    public static void displayMillChest(Player player, Point chestPos) {
        TileEntityLockedChest chest = chestPos.getMillChest(player.level);
        if (chest == null) {
            return;
        }
        MillWorldData mw = Mill.getMillWorld(player.level);
        if (chest.buildingPos != null) {
            Building building = mw==null?null:mw.getBuilding(chest.buildingPos);
            if (building != null) {
                building.sendBuildingPacket(player, true);
            } else {
                chest.sendUpdatePacket(player);
            }
        } else {
            chest.sendUpdatePacket(player);
        }
        org.millenaire.common.block.BlockLockedChest.openChest(player,new net.minecraft.core.BlockPos(chestPos.getiX(),chestPos.getiY(),chestPos.getiZ()));
    }

    public static void displayNegationWandGUI(Player player, Building townHall) {
        townHall.sendBuildingPacket(player, false);
        FriendlyByteBuf data = ServerSender.getPacketBuffer();
        data.writeInt(104);
        data.writeInt(9);
        StreamReadWrite.writeNullablePoint(townHall.getPos(), data);
        ServerSender.sendPacketToPlayer(data, player);
    }

    public static void displayNewBuildingProjectGUI(Player player, Building townHall, Point pos) {
        townHall.sendBuildingPacket(player, false);
        FriendlyByteBuf data = ServerSender.getPacketBuffer();
        data.writeInt(104);
        data.writeInt(10);
        StreamReadWrite.writeNullablePoint(townHall.getPos(), data);
        StreamReadWrite.writeNullablePoint(pos, data);
        ServerSender.sendPacketToPlayer(data, player);
    }

    public static void displayNewVillageGUI(Player player, Point pos) {
        FriendlyByteBuf data = ServerSender.getPacketBuffer();
        data.writeInt(104);
        data.writeInt(13);
        StreamReadWrite.writeNullablePoint(pos, data);
        ServerSender.sendPacketToPlayer(data, player);
    }

    public static void displayPanel(Player player, Point signPos) {
        Building building;
        TileEntityPanel panel = signPos.getPanel(player.level);
        if (panel == null) {
            return;
        }
        MillWorldData mw = Mill.getMillWorld(player.level);
        if (panel.buildingPos != null && (building = mw.getBuilding(panel.buildingPos)) != null) {
            building.sendBuildingPacket(player, true);
        }
        FriendlyByteBuf data = ServerSender.getPacketBuffer();
        data.writeInt(104);
        data.writeInt(7);
        StreamReadWrite.writeNullablePoint(signPos, data);
        ServerSender.sendPacketToPlayer(data, player);
    }

    public static void displayQuestGUI(Player player, MillVillager villager) {
        FriendlyByteBuf data = ServerSender.getPacketBuffer();
        data.writeInt(104);
        data.writeInt(3);
        data.writeLong(villager.getVillagerId());
        ServerSender.sendPacketToPlayer(data, player);
    }

    public static void displayVillageBookGUI(Player player, Point p) {
        MillWorldData mw = Mill.getMillWorld(player.level);
        Building th = mw.getBuilding(p);
        if (th == null) {
            return;
        }
        th.sendBuildingPacket(player, true);
        FriendlyByteBuf data = ServerSender.getPacketBuffer();
        data.writeInt(104);
        data.writeInt(5);
        StreamReadWrite.writeNullablePoint(p, data);
        ServerSender.sendPacketToPlayer(data, player);
    }

    public static void displayVillageChiefGUI(Player player, MillVillager chief) {
        if (chief.getTownHall() == null) {
            MillLog.error(chief, "Needed to send chief's TH but TH is null.");
            return;
        }
        chief.getTownHall().sendBuildingPacket(player, false);
        MillWorldData mw = Mill.getMillWorld(player.level);
        for (Point p : chief.getTownHall().getKnownVillages()) {
            Building b = mw.getBuilding(p);
            if (b == null) continue;
            b.sendBuildingPacket(player, false);
        }
        FriendlyByteBuf data = ServerSender.getPacketBuffer();
        data.writeInt(104);
        data.writeInt(4);
        data.writeLong(chief.getVillagerId());
        ServerSender.sendPacketToPlayer(data, player);
    }

    public static void displayVillageTradeGUI(Player player, Building building) {
        building.computeShopGoods(player);
        building.sendShopPacket(player);
        building.sendBuildingPacket(player, true);
        if (!building.isTownhall) {
            building.getTownHall().sendBuildingPacket(player, false);
        }
        org.millenaire.common.ui.ContainerTrade.open(player, building);
    }

    public static FriendlyByteBuf getPacketBuffer() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    public static void sendAdvancementEarned(ServerPlayer player, String advancementKey) {
        if (player == null) {
            return;
        }
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        FriendlyByteBuf data = ServerSender.getPacketBuffer();
        data.writeInt(109);
        data.writeUtf(advancementKey);
        ServerSender.sendPacketToPlayer(data, (Player)player);
    }

    public static void sendAnimalBreeding(Animal animal) {
        FriendlyByteBuf data = ServerSender.getPacketBuffer();
        Point pos = new Point((Entity)animal);
        data.writeInt(107);
        StreamReadWrite.writeNullablePoint(pos, data);
        data.writeInt(animal.getId());
        ServerSender.sendPacketToPlayersInRange(data, animal.level, pos, 50);
    }

    public static void sendChat(Player player, ChatFormatting colour, String s) {
        TextComponent chat = new TextComponent(s);
        chat.withStyle(colour);
        player.sendMessage(chat, net.minecraft.Util.NIL_UUID);
    }

    public static void sendContentUnlocked(Player player, int contentType, String cultureKey, String contentKey, int nbUnlocked, int nbTotal) {
        if (player == null) {
            return;
        }
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        FriendlyByteBuf data = ServerSender.getPacketBuffer();
        data.writeInt(110);
        data.writeInt(contentType);
        data.writeUtf(cultureKey);
        data.writeUtf(contentKey);
        data.writeInt(nbUnlocked);
        data.writeInt(nbTotal);
        ServerSender.sendPacketToPlayer(data, player);
    }

    public static void sendContentUnlockedMultiple(Player player, int contentType, String cultureKey, List<String> contentKeys, int nbUnlocked, int nbTotal) {
        if (player == null) {
            return;
        }
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        FriendlyByteBuf data = ServerSender.getPacketBuffer();
        data.writeInt(111);
        data.writeInt(contentType);
        data.writeUtf(cultureKey);
        StreamReadWrite.writeStringList(contentKeys, data);
        data.writeInt(nbUnlocked);
        data.writeInt(nbTotal);
        ServerSender.sendPacketToPlayer(data, player);
    }

    public static void sendLockedChestUpdatePacket(TileEntityLockedChest chest, Player player) {
        FriendlyByteBuf data = ServerSender.getPacketBuffer();
        Point pos = new Point(chest.getBlockPos());
        data.writeInt(5);
        StreamReadWrite.writeNullablePoint(pos, data);
        StreamReadWrite.writeNullablePoint(chest.buildingPos, data);
        data.writeBoolean(MillConfigValues.DEV);
        data.writeByte(chest.getContainerSize());
        for (int i = 0; i < chest.getContainerSize(); ++i) {
            StreamReadWrite.writeNullableItemStack(chest.getItem(i), data);
        }
        ServerSender.sendPacketToPlayer(data, player);
    }

    public static void sendPacketToPlayer(FriendlyByteBuf buffer,Player player) {
        if(player instanceof ServerPlayer server)MillNetwork.sendToPlayer(buffer,server);
    }

    public static void sendPacketToPlayersInRange(FriendlyByteBuf buffer,Level world,Point pos,int range) {
        MillNetwork.sendNear(buffer,world,pos,range);
    }

    public static void sendTranslatedSentence(Player player, char colour, String code, String ... values) {
        if (player == null) {
            return;
        }
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        FriendlyByteBuf data = ServerSender.getPacketBuffer();
        data.writeInt(100);
        data.writeChar((int)colour);
        data.writeUtf(code);
        data.writeInt(values.length);
        for (String value : values) {
            StreamReadWrite.writeNullableString(value, data);
        }
        ServerSender.sendPacketToPlayer(data, player);
    }

    public static void sendTranslatedSentenceInRange(Level world, Point p, int range, char colour, String key, String ... values) {
        for (Object oplayer : world.players()) {
            Player player = (Player)oplayer;
            if (!(p.distanceTo((Entity)player) < (double)range)) continue;
            ServerSender.sendTranslatedSentence(player, colour, key, values);
        }
    }

    public static void sendVillagerSentence(ServerPlayer player, MillVillager v) {
        if (player == null) {
            return;
        }
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        FriendlyByteBuf data = ServerSender.getPacketBuffer();
        data.writeInt(108);
        data.writeLong(v.getVillagerId());
        ServerSender.sendPacketToPlayer(data, (Player)player);
    }

    public static void sendVillageSentenceInRange(Level world, Point p, int range, MillVillager v) {
        for (Object oplayer : world.players()) {
            ServerPlayer player = (ServerPlayer)oplayer;
            if (!(p.distanceTo((Entity)player) < (double)range)) continue;
            ServerSender.sendVillagerSentence(player, v);
        }
    }
}

