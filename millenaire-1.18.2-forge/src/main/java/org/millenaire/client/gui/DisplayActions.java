/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiScreen
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.world.World
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package org.millenaire.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.millenaire.client.MillClientUtilities;
import org.millenaire.client.book.TextBook;
import org.millenaire.client.gui.text.GuiConfig;
import org.millenaire.client.gui.text.GuiControlledMilitary;
import org.millenaire.client.gui.text.GuiControlledProjects;
import org.millenaire.client.gui.text.GuiCustomBuilding;
import org.millenaire.client.gui.text.GuiHelp;
import org.millenaire.client.gui.text.GuiHire;
import org.millenaire.client.gui.text.GuiImportTable;
import org.millenaire.client.gui.text.GuiNegationWand;
import org.millenaire.client.gui.text.GuiNewBuildingProject;
import org.millenaire.client.gui.text.GuiNewVillage;
import org.millenaire.client.gui.text.GuiPanelParchment;
import org.millenaire.client.gui.text.GuiQuest;
import org.millenaire.client.gui.text.GuiTravelBook;
import org.millenaire.client.gui.text.GuiVillageHead;
import org.millenaire.common.buildingplan.BuildingCustomPlan;
import org.millenaire.common.culture.VillageType;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;
import org.millenaire.common.world.UserProfile;

@OnlyIn(value=Dist.CLIENT)
public class DisplayActions {
    public static void displayChunkGUI(Player player, Level world) {
        MillClientUtilities.displayChunkPanel(world, player);
    }

    public static void displayConfigGUI() {
        Minecraft.getInstance().setScreen((Screen)new GuiConfig());
    }

    public static void displayControlledMilitaryGUI(Player player, Building townHall) {
        Minecraft.getInstance().setScreen((Screen)new GuiControlledMilitary(player, townHall));
    }

    public static void displayControlledProjectGUI(Player player, Building townHall) {
        Minecraft.getInstance().setScreen((Screen)new GuiControlledProjects(player, townHall));
    }

    public static void displayEditCustomBuildingGUI(Player player, Building building) {
        Minecraft.getInstance().setScreen((Screen)new GuiCustomBuilding(player, building));
    }

    public static void displayHelpGUI() {
        Minecraft.getInstance().setScreen((Screen)new GuiHelp());
    }

    public static void displayHireGUI(Player player, MillVillager villager) {
        Minecraft.getInstance().setScreen((Screen)new GuiHire(player, villager));
    }

    public static void displayImportTableGUI(Player player, Point tablePos) {
        Minecraft.getInstance().setScreen((Screen)new GuiImportTable(player, tablePos));
    }

    public static void displayNegationWandGUI(Player player, Building townHall) {
        Minecraft.getInstance().setScreen((Screen)new GuiNegationWand(player, townHall));
    }

    public static void displayNewBuildingProjectGUI(Player player, Building townHall, Point pos) {
        Minecraft.getInstance().setScreen((Screen)new GuiNewBuildingProject(player, townHall, pos));
    }

    public static void displayNewCustomBuildingGUI(Player player, Building townHall, Point pos, BuildingCustomPlan customBuilding) {
        Minecraft.getInstance().setScreen((Screen)new GuiCustomBuilding(player, townHall, pos, customBuilding));
    }

    public static void displayNewCustomBuildingGUI(Player player, Point pos, VillageType villageType) {
        Minecraft.getInstance().setScreen((Screen)new GuiCustomBuilding(player, pos, villageType));
    }

    public static void displayNewVillageGUI(Player player, Point pos) {
        Minecraft.getInstance().setScreen((Screen)new GuiNewVillage(player, pos));
    }

    public static void displayParchmentPanelGUI(Player player, TextBook book, Building building, int mapType, boolean isParchment) {
        Minecraft.getInstance().setScreen((Screen)new GuiPanelParchment(player, book, building, mapType, isParchment));
    }

    public static void displayQuestGUI(Player player, MillVillager villager) {
        UserProfile profile = Mill.clientWorld.getProfile(player);
        if (profile.villagersInQuests.containsKey(villager.getVillagerId())) {
            Minecraft.getInstance().setScreen((Screen)new GuiQuest(player, villager));
        }
    }

    public static void displayStartupOrError(Player player, boolean error) {
        MillClientUtilities.displayStartupText(error);
    }

    public static void displayTravelBookGUI(Player player) {
        Minecraft.getInstance().setScreen((Screen)new GuiTravelBook(player));
    }

    public static void displayVillageBookGUI(Player player, Point p) {
        MillClientUtilities.displayVillageBook(Mill.clientWorld.world, player, p);
    }

    public static void displayVillageChiefGUI(Player player, MillVillager chief) {
        Minecraft.getInstance().setScreen((Screen)new GuiVillageHead(player, chief));
    }
}

