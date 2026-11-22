/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.ItemSword
 */
package org.millenaire.common.quest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.quest.Quest;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.world.MillWorldData;
import org.millenaire.common.world.UserProfile;

public class QuestStep {
    private final Quest quest;
    int pos;
    public List<String> clearGlobalTagsFailure = new ArrayList<String>();
    public List<String> clearGlobalTagsSuccess = new ArrayList<String>();
    public List<String> clearPlayerTagsFailure = new ArrayList<String>();
    public List<String> clearPlayerTagsSuccess = new ArrayList<String>();
    public List<String[]> clearTagsFailure = new ArrayList<String[]>();
    public List<String[]> clearTagsSuccess = new ArrayList<String[]>();
    public final HashMap<String, String> descriptions = new HashMap();
    public final HashMap<String, String> descriptionsRefuse = new HashMap();
    public final HashMap<String, String> descriptionsSuccess = new HashMap();
    public final HashMap<String, String> descriptionsTimeUp = new HashMap();
    public final HashMap<String, String> labels = new HashMap();
    public final HashMap<String, String> listings = new HashMap();
    public int duration = 1;
    public List<String> forbiddenGlobalTag = new ArrayList<String>();
    public List<String> forbiddenPlayerTag = new ArrayList<String>();
    public int penaltyReputation = 0;
    public HashMap<InvItem, Integer> requiredGood = new HashMap();
    public List<String> stepRequiredGlobalTag = new ArrayList<String>();
    public List<String> stepRequiredPlayerTag = new ArrayList<String>();
    public HashMap<InvItem, Integer> rewardGoods = new HashMap();
    public int rewardMoney = 0;
    public int rewardReputation = 0;
    public List<String> bedrockbuildings = new ArrayList<String>();
    public List<String> setGlobalTagsFailure = new ArrayList<String>();
    public List<String> setGlobalTagsSuccess = new ArrayList<String>();
    public List<String> setPlayerTagsFailure = new ArrayList<String>();
    public List<String> setPlayerTagsSuccess = new ArrayList<String>();
    public List<String[]> setVillagerTagsFailure = new ArrayList<String[]>();
    public List<String[]> setVillagerTagsSuccess = new ArrayList<String[]>();
    public List<String[]> setActionDataSuccess = new ArrayList<String[]>();
    public List<QuestStepRelationChange> relationChanges = new ArrayList<QuestStepRelationChange>();
    public boolean showRequiredGoods = true;
    public String villager;

    public QuestStep(Quest quest, int pos) {
        this.quest = quest;
        this.pos = pos;
    }

    public String getDescription() {
        return LanguageUtilities.questString(this.getStringKey() + "description", true);
    }

    public String getDescriptionRefuse() {
        return LanguageUtilities.questString(this.getStringKey() + "description_refuse", true);
    }

    public String getDescriptionSuccess() {
        return LanguageUtilities.questString(this.getStringKey() + "description_success", true);
    }

    public String getDescriptionTimeUp() {
        return LanguageUtilities.questString(this.getStringKey() + "description_timeup", false);
    }

    public String getLabel() {
        return LanguageUtilities.questString(this.getStringKey() + "label", true);
    }

    public String getListing() {
        return LanguageUtilities.questString(this.getStringKey() + "listing", false);
    }

    public String getStringKey() {
        return this.quest.key + "_" + this.pos + "_";
    }

    public String lackingConditions(Player player) {
        MillWorldData mw = Mill.getMillWorld(player.level);
        UserProfile profile = mw.getProfile(player);
        String lackingGoods = null;
        for (InvItem item : this.requiredGood.keySet()) {
            int diff;
            ItemStack stack;
            int i;
            int nbenchanted;
            if (item.special == 1) {
                nbenchanted = 0;
                for (i = 0; i < player.getInventory().getContainerSize(); ++i) {
                    stack = player.getInventory().getItem(i);
                    if (stack == null || !stack.isEnchanted()) continue;
                    ++nbenchanted;
                }
                diff = this.requiredGood.get(item) - nbenchanted;
            } else if (item.special == 2) {
                nbenchanted = 0;
                for (i = 0; i < player.getInventory().getContainerSize(); ++i) {
                    stack = player.getInventory().getItem(i);
                    if (stack == null || !stack.isEnchanted() || !(stack.getItem() instanceof SwordItem)) continue;
                    ++nbenchanted;
                }
                diff = this.requiredGood.get(item) - nbenchanted;
            } else {
                diff = this.requiredGood.get(item) - MillCommonUtilities.countChestItems((Container)player.getInventory(), item.getItem(), item.meta);
            }
            if (diff <= 0) continue;
            lackingGoods = lackingGoods != null ? lackingGoods + ", " : "";
            lackingGoods = lackingGoods + diff + " " + item.getName();
        }
        if (lackingGoods != null) {
            lackingGoods = this.showRequiredGoods ? LanguageUtilities.string("quest.lackingcondition") + " " + lackingGoods : LanguageUtilities.string("quest.lackinghiddengoods");
        }
        boolean tagsOk = true;
        for (String tag : this.stepRequiredGlobalTag) {
            if (mw.isGlobalTagSet(tag)) continue;
            tagsOk = false;
        }
        for (String tag : this.forbiddenGlobalTag) {
            if (!mw.isGlobalTagSet(tag)) continue;
            tagsOk = false;
        }
        for (String tag : this.stepRequiredPlayerTag) {
            if (profile.isTagSet(tag)) continue;
            tagsOk = false;
        }
        for (String tag : this.forbiddenPlayerTag) {
            if (!profile.isTagSet(tag)) continue;
            tagsOk = false;
        }
        if (!tagsOk) {
            lackingGoods = lackingGoods != null ? lackingGoods + ". " : "";
            lackingGoods = lackingGoods + LanguageUtilities.string("quest.conditionsnotmet");
        }
        return lackingGoods;
    }

    public static class QuestStepRelationChange {
        public final String firstVillager;
        public final String secondVillager;
        public final int change;

        public static QuestStepRelationChange parseString(String input) throws Exception {
            String[] params = input.split(",");
            if (params.length != 3) {
                throw new Exception("Relation changes must have three parameters: villager1, villager2, change");
            }
            return new QuestStepRelationChange(params[0], params[1], Integer.parseInt(params[2]));
        }

        private QuestStepRelationChange(String firstVillager, String secondVillager, int change) {
            this.firstVillager = firstVillager;
            this.secondVillager = secondVillager;
            this.change = change;
        }
    }
}

