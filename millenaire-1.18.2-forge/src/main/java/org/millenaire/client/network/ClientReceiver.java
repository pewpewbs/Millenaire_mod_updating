/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.toasts.IToast
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.passive.EntityAnimal
 *  net.minecraft.network.PacketBuffer
 *  net.minecraftforge.fml.common.FMLCommonHandler
 *  net.minecraftforge.fml.common.eventhandler.SubscribeEvent
 *  net.minecraftforge.fml.common.network.FMLNetworkEvent$ClientCustomPacketEvent
 */
package org.millenaire.client.network;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.millenaire.client.MillClientUtilities;
import org.millenaire.client.gui.DisplayActions;
import org.millenaire.client.gui.UnlockingToast;
import org.millenaire.common.advancements.MillAdvancements;
import org.millenaire.common.buildingplan.BuildingPlanSet;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.culture.Culture;
import org.millenaire.common.culture.VillageType;
import org.millenaire.common.culture.VillagerType;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.entity.TileEntityLockedChest;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.item.TradeGood;
import org.millenaire.common.network.StreamReadWrite;
import org.millenaire.common.ui.MillMapInfo;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;
import org.millenaire.common.village.Building;
import org.millenaire.common.world.UserProfile;

public class ClientReceiver {
    /** Called only on the client thread; the payload owns its bytes. */
    public static void receive(org.millenaire.common.network.MillPacketPayload payload) {
        if(Mill.clientWorld==null || Minecraft.getInstance().level!=Mill.clientWorld.world)return;
        FriendlyByteBuf buffer=payload.open();
        try { new ClientReceiver().processPacket(buffer); }
        finally { buffer.release(); }
    }

    private void processPacket(FriendlyByteBuf data) {
        try {
            int packettype = data.readInt();
            Mill.clientWorld.millenaireEnabled = true;
            if (MillConfigValues.LogNetwork >= 3) {
                MillLog.debug(this, "Received client packet type: " + packettype);
            }
            UserProfile profile = Mill.proxy.getClientProfile();
            if (packettype == org.millenaire.common.network.ServerReceiver.PACKET_GOAL_DIAGNOSTICS_STATE) {
                int entityId = data.readInt(), count = data.readVarInt();
                if (count < 0 || count > 512) return;
                List<String> rows = new ArrayList<>();
                for (int i=0;i<count;i++) rows.add(data.readUtf(240));
                if (Minecraft.getInstance().screen instanceof org.millenaire.client.gui.text.GuiGoalDiagnostics screen)
                    screen.receiveState(entityId, rows);
            } else if (packettype == org.millenaire.common.network.ServerReceiver.PACKET_TEST_TUNING_STATE) {
                boolean allowed = data.readBoolean();
                var settings = new org.millenaire.common.config.TestTuning.Settings(data.readInt(), data.readInt(), data.readInt(), data.readInt());
                if (Minecraft.getInstance().screen instanceof org.millenaire.client.gui.text.GuiTestTuning screen)
                    screen.receiveState(allowed, settings);
            } else if (packettype == 2) {
                Building.readBuildingPacket(Mill.clientWorld, data);
            } else if (packettype == 11) {
                Building.readShopPacket(Mill.clientWorld, data);
            } else if (packettype == 3) {
                MillVillager.readVillagerPacket(data);
            } else if (packettype == 100) {
                this.readTranslatedChatPackage(data);
            } else if (packettype == 108) {
                this.readVillagerSentencePackage(data);
            } else if (packettype == 109) {
                this.readAdvancementPackage(data);
            } else if (packettype == 110) {
                this.readContentUnlockedPackage(data);
            } else if (packettype == 111) {
                this.readContentUnlockedPackageMultiple(data);
            } else if (packettype == 5) {
                TileEntityLockedChest.readUpdatePacket(data, Mill.clientWorld.world);
            } else if (packettype == 101) {
                profile.receiveProfilePacket(data);
            } else if (packettype == 102) {
                profile.receiveQuestInstancePacket(data);
            } else if (packettype == 103) {
                profile.receiveQuestInstanceDestroyPacket(data);
            } else if (packettype == 104) {
                this.readGUIPacket(data);
            } else if (packettype == 7) {
                MillMapInfo.readPacket(data);
            } else if (packettype == 9) {
                Mill.clientWorld.receiveVillageListPacket(data);
            } else if (packettype == 10) {
                this.readServerContentPacket(data);
            } else if (packettype == 107) {
                this.readAnimalBreedPacket(data);
            } else {
                MillLog.error(null, "Received packet with unknown type: " + packettype);
            }
        }
        catch (Exception e) {
            MillLog.printException("Error in ClientReceiver.onPacketData:", e);
        }
    }

    private void readAdvancementPackage(FriendlyByteBuf data) {
        String advancementKey = data.readUtf(2048);
        MillAdvancements.addToStats(Mill.proxy.getTheSinglePlayer(), advancementKey);
    }

    private void readAnimalBreedPacket(FriendlyByteBuf data) {
        Point p = StreamReadWrite.readNullablePoint(data);
        int endId = data.readInt();
        List<Entity> animals = WorldUtilities.getEntitiesWithinAABB(Mill.clientWorld.world, Animal.class, p, 5, 5);
        for (Entity ent : animals) {
            Animal animal = (Animal)ent;
            if (animal.getId() != endId) continue;
            animal.setInLove(null);
            MillCommonUtilities.generateHearts((Entity)animal);
        }
    }

    private void readContentUnlockedPackage(FriendlyByteBuf data) {
        int contentType = data.readInt();
        String cultureKey = data.readUtf(2048);
        String contentKey = data.readUtf(2048);
        int nbUnlocked = data.readInt();
        int nbTotal = data.readInt();
        Culture culture = Culture.getCultureByName(cultureKey);
        if (culture != null) {
            VillagerType villagerType;
            if (contentType == 1) {
                BuildingPlanSet planSet = culture.getBuildingPlanSet(contentKey);
                if (planSet != null) {
                    Minecraft.getInstance().getToasts().addToast((Toast)new UnlockingToast(planSet, nbUnlocked, nbTotal));
                }
            } else if (contentType == 4) {
                TradeGood tradeGood = culture.getTradeGood(contentKey);
                if (tradeGood != null) {
                    Minecraft.getInstance().getToasts().addToast((Toast)new UnlockingToast(tradeGood, nbUnlocked, nbTotal));
                }
            } else if (contentType == 2) {
                VillageType villageType = culture.getVillageType(contentKey);
                if (villageType != null) {
                    Minecraft.getInstance().getToasts().addToast((Toast)new UnlockingToast(villageType, nbUnlocked, nbTotal));
                }
            } else if (contentType == 3 && (villagerType = culture.getVillagerType(contentKey)) != null) {
                Minecraft.getInstance().getToasts().addToast((Toast)new UnlockingToast(villagerType, nbUnlocked, nbTotal));
            }
        }
    }

    private void readContentUnlockedPackageMultiple(FriendlyByteBuf data) {
        int contentType = data.readInt();
        String cultureKey = data.readUtf(2048);
        CopyOnWriteArrayList<String> contentKeys = StreamReadWrite.readStringList(data);
        int nbUnlocked = data.readInt();
        int nbTotal = data.readInt();
        Culture culture = Culture.getCultureByName(cultureKey);
        if (culture != null && contentType == 5) {
            ArrayList<TradeGood> tradeGoods = new ArrayList<TradeGood>();
            for (String contentKey : contentKeys) {
                TradeGood tradeGood = culture.getTradeGood(contentKey);
                if (tradeGood == null) continue;
                tradeGoods.add(tradeGood);
            }
            if (tradeGoods.size() > 0) {
                Minecraft.getInstance().getToasts().addToast((Toast)new UnlockingToast(tradeGoods, nbUnlocked, nbTotal));
            }
        }
    }

    private void readGUIPacket(FriendlyByteBuf data) {
        int guiId = data.readInt();
        if (guiId == 3) {
            MillVillager v = Mill.clientWorld.getVillagerById(data.readLong());
            if (v != null) {
                DisplayActions.displayQuestGUI(Mill.proxy.getTheSinglePlayer(), v);
            } else {
                MillLog.error(this, "Unknown villager id in readGUIPacket: " + guiId);
            }
        } else if (guiId == 12) {
            MillVillager v = Mill.clientWorld.getVillagerById(data.readLong());
            if (v != null) {
                DisplayActions.displayHireGUI(Mill.proxy.getTheSinglePlayer(), v);
            } else {
                MillLog.error(this, "Unknown villager id in readGUIPacket: " + guiId);
            }
        } else if (guiId == 4) {
            MillVillager v = Mill.clientWorld.getVillagerById(data.readLong());
            if (v != null) {
                DisplayActions.displayVillageChiefGUI(Mill.proxy.getTheSinglePlayer(), v);
            } else {
                MillLog.error(this, "Unknown villager id in readGUIPacket: " + guiId);
            }
        } else if (guiId == 5) {
            Point p = StreamReadWrite.readNullablePoint(data);
            if (p != null) {
                DisplayActions.displayVillageBookGUI(Mill.proxy.getTheSinglePlayer(), p);
            } else {
                MillLog.error(this, "Unknown point in readGUIPacket: " + guiId);
            }
        } else if (guiId == 9) {
            Point p = StreamReadWrite.readNullablePoint(data);
            if (p != null) {
                Building building = Mill.clientWorld.getBuilding(p);
                if (building != null) {
                    DisplayActions.displayNegationWandGUI(Mill.proxy.getTheSinglePlayer(), building);
                }
            } else {
                MillLog.error(this, "Unknown point in readGUIPacket: " + guiId);
            }
        } else if (guiId == 10) {
            Point thPos = StreamReadWrite.readNullablePoint(data);
            Point pos = StreamReadWrite.readNullablePoint(data);
            if (thPos != null && pos != null) {
                Building townHall = Mill.clientWorld.getBuilding(thPos);
                if (townHall != null) {
                    Building building = townHall.getBuildingAtCoordPlanar(pos);
                    if (building == null || !building.location.isCustomBuilding) {
                        DisplayActions.displayNewBuildingProjectGUI(Mill.proxy.getTheSinglePlayer(), townHall, pos);
                    } else {
                        DisplayActions.displayEditCustomBuildingGUI(Mill.proxy.getTheSinglePlayer(), building);
                    }
                }
            } else {
                MillLog.error(this, "Unknown point in readGUIPacket: " + guiId);
            }
        } else if (guiId == 13) {
            Point pos = StreamReadWrite.readNullablePoint(data);
            if (pos != null) {
                DisplayActions.displayNewVillageGUI(Mill.proxy.getTheSinglePlayer(), pos);
            } else {
                MillLog.error(this, "Unknown point in readGUIPacket: " + guiId);
            }
        } else if (guiId == 11) {
            Point thPos = StreamReadWrite.readNullablePoint(data);
            if (thPos != null) {
                Building building = Mill.clientWorld.getBuilding(thPos);
                if (building != null) {
                    DisplayActions.displayControlledProjectGUI(Mill.proxy.getTheSinglePlayer(), building);
                }
            } else {
                MillLog.error(this, "Unknown point in readGUIPacket: " + guiId);
            }
        } else if (guiId == 14) {
            Point thPos = StreamReadWrite.readNullablePoint(data);
            if (thPos != null) {
                Building building = Mill.clientWorld.getBuilding(thPos);
                if (building != null) {
                    DisplayActions.displayControlledMilitaryGUI(Mill.proxy.getTheSinglePlayer(), building);
                }
            } else {
                MillLog.error(this, "Unknown point in readGUIPacket: " + guiId);
            }
        } else if (guiId == 7) {
            Point p = StreamReadWrite.readNullablePoint(data);
            if (p != null) {
                MillClientUtilities.displayPanel(Mill.clientWorld.world, Mill.proxy.getTheSinglePlayer(), p);
            } else {
                MillLog.error(this, "Unknown point in readGUIPacket: " + guiId);
            }
        } else if (guiId == 1 || guiId == 2 || guiId == 8) {
            // These inventory menus are opened exclusively by the server's vanilla
            // container packet. A legacy GUI-only packet cannot supply a valid menu ID.
            MillLog.error(this, "Rejected obsolete inventory GUI packet: " + guiId);
        } else if (guiId == 15) {
            Point tablePos = StreamReadWrite.readNullablePoint(data);
            if (tablePos != null) {
                DisplayActions.displayImportTableGUI(Mill.proxy.getTheSinglePlayer(), tablePos);
            } else {
                MillLog.error(this, "Unknown point in readGUIPacket: " + guiId);
            }
        } else {
            MillLog.error(null, "Unknown GUI: " + guiId);
        }
    }

    private void readServerContentPacket(FriendlyByteBuf data) {
        int nbCultures = data.readShort();
        for (int i = 0; i < nbCultures; ++i) {
            Culture.readCultureMissingContentPacket(data);
        }
        Culture.refreshLists();
    }

    private void readTranslatedChatPackage(FriendlyByteBuf data) {
        char colour = data.readChar();
        String s = data.readUtf(2048);
        String[] values = new String[data.readInt()];
        for (int i = 0; i < values.length; ++i) {
            values[i] = LanguageUtilities.unknownString(StreamReadWrite.readNullableString(data));
        }
        s = LanguageUtilities.string(s, values);
        Mill.proxy.sendLocalChat(Mill.proxy.getTheSinglePlayer(), colour, s);
    }

    private void readVillagerSentencePackage(FriendlyByteBuf data) {
        MillVillager v = Mill.clientWorld.getVillagerById(data.readLong());
        if (v != null) {
            MillClientUtilities.putVillagerSentenceInChat(v);
        }
    }
}

