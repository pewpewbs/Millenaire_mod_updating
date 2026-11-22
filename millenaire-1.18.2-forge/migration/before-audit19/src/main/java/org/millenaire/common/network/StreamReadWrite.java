/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.enchantment.Enchantment
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.network.PacketBuffer
 *  net.minecraft.util.ResourceLocation
 */
package org.millenaire.common.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.common.buildingplan.BuildingPlan;
import org.millenaire.common.culture.Culture;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.item.TradeGood;
import org.millenaire.common.quest.Quest;
import org.millenaire.common.quest.QuestInstance;
import org.millenaire.common.quest.QuestInstanceVillager;
import org.millenaire.common.ui.PujaSacrifice;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;
import org.millenaire.common.village.BuildingLocation;
import org.millenaire.common.village.BuildingProject;
import org.millenaire.common.village.VillagerRecord;
import org.millenaire.common.world.MillWorldData;
import org.millenaire.common.world.UserProfile;

public class StreamReadWrite {
    public static final int MAX_STR_LENGTH = 2048;

    public static CopyOnWriteArrayList<Boolean> readBooleanList(FriendlyByteBuf ds) {
        CopyOnWriteArrayList<Boolean> v = new CopyOnWriteArrayList<Boolean>();
        int nb = ds.readInt();
        for (int i = 0; i < nb; ++i) {
            v.add(ds.readBoolean());
        }
        return v;
    }

    public static List<BuildingLocation> readBuildingLocationList(FriendlyByteBuf ds) {
        ArrayList<BuildingLocation> v = new ArrayList<BuildingLocation>();
        int nb = ds.readInt();
        for (int i = 0; i < nb; ++i) {
            v.add(StreamReadWrite.readNullableBuildingLocation(ds));
        }
        return v;
    }

    public static BuildingPlan readBuildingPlanInfo(FriendlyByteBuf ds, Culture culture) {
        String key = ds.readUtf(2048);
        int level = ds.readInt();
        int variation = ds.readInt();
        BuildingPlan plan = new BuildingPlan(key, level, variation, culture);
        plan.planName = StreamReadWrite.readNullableString(ds);
        plan.nativeName = StreamReadWrite.readNullableString(ds);
        plan.requiredGlobalTag = StreamReadWrite.readNullableString(ds);
        plan.forbiddenTagsInVillage = StreamReadWrite.readStringList(ds);
        plan.shop = StreamReadWrite.readNullableString(ds);
        plan.isSubBuilding = ds.readBoolean();
        plan.price = ds.readInt();
        plan.reputation = ds.readInt();
        plan.version = ds.readInt();
        plan.maleResident = StreamReadWrite.readStringList(ds);
        plan.femaleResident = StreamReadWrite.readStringList(ds);
        plan.visitors = StreamReadWrite.readStringList(ds);
        plan.startingSubBuildings = StreamReadWrite.readStringList(ds);
        plan.subBuildings = StreamReadWrite.readStringList(ds);
        plan.tags = StreamReadWrite.readStringList(ds);
        return plan;
    }

    public static HashMap<InvItem, Integer> readInventory(FriendlyByteBuf ds) {
        HashMap<InvItem, Integer> inv = new HashMap<InvItem, Integer>();
        int nb = ds.readInt();
        for (int i = 0; i < nb; ++i) {
            InvItem item = InvItem.createInvItem(Item.byId((int)ds.readInt()), ds.readInt());
            inv.put(item, ds.readInt());
        }
        return inv;
    }

    private static ItemStack readItemStack(FriendlyByteBuf buffer) throws IOException {
        return buffer.readItem();
    }

    public static CopyOnWriteArrayList<ItemStack> readItemStackList(FriendlyByteBuf ds) throws IOException {
        CopyOnWriteArrayList<ItemStack> v = new CopyOnWriteArrayList<ItemStack>();
        int nb = ds.readInt();
        for (int i = 0; i < nb; ++i) {
            v.add(StreamReadWrite.readNullableItemStack(ds));
        }
        return v;
    }

    private static CompoundTag readNBTTagCompound(FriendlyByteBuf buffer) throws IOException {
        return buffer.readNbt();
    }

    public static BuildingLocation readNullableBuildingLocation(FriendlyByteBuf ds) {
        boolean isnull = ds.readBoolean();
        if (isnull) {
            return null;
        }
        BuildingLocation bl = new BuildingLocation();
        bl.isCustomBuilding = ds.readBoolean();
        bl.planKey = StreamReadWrite.readNullableString(ds);
        bl.shop = StreamReadWrite.readNullableString(ds);
        bl.minx = ds.readInt();
        bl.maxx = ds.readInt();
        bl.miny = ds.readInt();
        bl.maxy = ds.readInt();
        bl.minz = ds.readInt();
        bl.maxz = ds.readInt();
        bl.minxMargin = ds.readInt();
        bl.maxxMargin = ds.readInt();
        bl.minyMargin = ds.readInt();
        bl.maxyMargin = ds.readInt();
        bl.minzMargin = ds.readInt();
        bl.maxzMargin = ds.readInt();
        bl.orientation = ds.readInt();
        bl.length = ds.readInt();
        bl.width = ds.readInt();
        bl.level = ds.readInt();
        bl.setVariation(ds.readInt());
        bl.reputation = ds.readInt();
        bl.price = ds.readInt();
        bl.version = ds.readInt();
        bl.pos = StreamReadWrite.readNullablePoint(ds);
        bl.chestPos = StreamReadWrite.readNullablePoint(ds);
        bl.sleepingPos = StreamReadWrite.readNullablePoint(ds);
        bl.sellingPos = StreamReadWrite.readNullablePoint(ds);
        bl.craftingPos = StreamReadWrite.readNullablePoint(ds);
        bl.shelterPos = StreamReadWrite.readNullablePoint(ds);
        bl.defendingPos = StreamReadWrite.readNullablePoint(ds);
        String cultureKey = StreamReadWrite.readNullableString(ds);
        bl.culture = Culture.getCultureByName(cultureKey);
        bl.subBuildings = StreamReadWrite.readStringList(ds);
        bl.showTownHallSigns = ds.readBoolean();
        bl.upgradesAllowed = ds.readBoolean();
        bl.bedrocklevel = ds.readBoolean();
        bl.isSubBuildingLocation = ds.readBoolean();
        return bl;
    }

    public static BuildingProject readNullableBuildingProject(FriendlyByteBuf ds, Culture culture) {
        boolean isnull = ds.readBoolean();
        if (isnull) {
            return null;
        }
        BuildingProject bp = new BuildingProject();
        bp.isCustomBuilding = ds.readBoolean();
        bp.key = StreamReadWrite.readNullableString(ds);
        bp.location = StreamReadWrite.readNullableBuildingLocation(ds);
        if (culture != null) {
            if (bp.isCustomBuilding) {
                bp.customBuildingPlan = culture.getBuildingCustom(bp.key);
            } else {
                bp.planSet = culture.getBuildingPlanSet(bp.key);
            }
        }
        return bp;
    }

    public static TradeGood readNullableGoods(FriendlyByteBuf ds, Culture culture) throws MillLog.MillenaireException {
        boolean isnull = ds.readBoolean();
        if (isnull) {
            return null;
        }
        String cultureKey = ds.readUtf(2048);
        InvItem iv = InvItem.createInvItem(MillCommonUtilities.getItemById(ds.readInt()), (int)ds.readByte());
        TradeGood g = new TradeGood("generated", Culture.getCultureByName(cultureKey), iv);
        g.requiredTag = StreamReadWrite.readNullableString(ds);
        g.travelBookCategory = StreamReadWrite.readNullableString(ds);
        g.autoGenerate = ds.readBoolean();
        g.minReputation = ds.readInt();
        return g;
    }

    public static ItemStack readNullableItemStack(FriendlyByteBuf ds) throws IOException {
        boolean isnull = ds.readBoolean();
        if (isnull) {
            return null;
        }
        return StreamReadWrite.readItemStack(ds);
    }

    public static Point readNullablePoint(FriendlyByteBuf ds) {
        boolean isnull = ds.readBoolean();
        if (isnull) {
            return null;
        }
        int x = ds.readInt();
        int y = ds.readInt();
        int z = ds.readInt();
        return new Point(x, y, z);
    }

    public static QuestInstance readNullableQuestInstance(MillWorldData mw, FriendlyByteBuf ds) {
        boolean isnull = ds.readBoolean();
        if (isnull) {
            return null;
        }
        long id = ds.readLong();
        String questKey = ds.readUtf(2048);
        if (!Quest.quests.containsKey(questKey)) {
            return null;
        }
        Quest quest = Quest.quests.get(questKey);
        UserProfile profile = mw.getProfile(ds.readUUID());
        short currentStep = ds.readUnsignedByte();
        long startTime = ds.readLong();
        long currentStepStart = ds.readLong();
        HashMap<String, QuestInstanceVillager> villagers = new HashMap<String, QuestInstanceVillager>();
        int nb = ds.readUnsignedByte();
        for (int i = 0; i < nb; ++i) {
            String key = ds.readUtf(2048);
            villagers.put(key, StreamReadWrite.readNullableQuestVillager(mw, ds));
        }
        QuestInstance qi = new QuestInstance(mw, quest, profile, villagers, startTime, currentStep, currentStepStart);
        qi.uniqueid = id;
        return qi;
    }

    public static QuestInstanceVillager readNullableQuestVillager(MillWorldData mw, FriendlyByteBuf ds) {
        boolean isnull = ds.readBoolean();
        if (isnull) {
            return null;
        }
        return new QuestInstanceVillager(mw, StreamReadWrite.readNullablePoint(ds), ds.readLong());
    }

    public static ResourceLocation readNullableResourceLocation(FriendlyByteBuf ds) {
        boolean isnull = ds.readBoolean();
        if (isnull) {
            return null;
        }
        return new ResourceLocation(ds.readUtf(2048));
    }

    public static String readNullableString(FriendlyByteBuf ds) {
        boolean isnull = ds.readBoolean();
        if (isnull) {
            return null;
        }
        return ds.readUtf(2048);
    }

    public static UUID readNullableUUID(FriendlyByteBuf ds) {
        boolean isnull = ds.readBoolean();
        if (isnull) {
            return null;
        }
        return ds.readUUID();
    }

    public static VillagerRecord readNullableVillagerRecord(MillWorldData mw, FriendlyByteBuf ds) {
        boolean isnull = ds.readBoolean();
        if (isnull) {
            return null;
        }
        VillagerRecord vr = new VillagerRecord(mw);
        vr.setVillagerId(ds.readLong());
        vr.type = StreamReadWrite.readNullableString(ds);
        vr.firstName = StreamReadWrite.readNullableString(ds);
        vr.familyName = StreamReadWrite.readNullableString(ds);
        vr.texture = StreamReadWrite.readNullableResourceLocation(ds);
        vr.nb = ds.readInt();
        vr.gender = ds.readInt();
        vr.size = ds.readInt();
        vr.scale = ds.readFloat();
        vr.rightHanded = ds.readBoolean();
        vr.setCulture(Culture.getCultureByName(StreamReadWrite.readNullableString(ds)));
        vr.fathersName = StreamReadWrite.readNullableString(ds);
        vr.mothersName = StreamReadWrite.readNullableString(ds);
        vr.spousesName = StreamReadWrite.readNullableString(ds);
        vr.maidenName = StreamReadWrite.readNullableString(ds);
        vr.killed = ds.readBoolean();
        vr.raidingVillage = ds.readBoolean();
        vr.awayraiding = ds.readBoolean();
        vr.awayhired = ds.readBoolean();
        vr.setHousePos(StreamReadWrite.readNullablePoint(ds));
        vr.setTownHallPos(StreamReadWrite.readNullablePoint(ds));
        vr.originalVillagePos = StreamReadWrite.readNullablePoint(ds);
        vr.raiderSpawn = ds.readLong();
        vr.inventory = StreamReadWrite.readInventory(ds);
        vr.questTags = StreamReadWrite.readStringList(ds);
        return vr;
    }

    public static PujaSacrifice readOrUpdateNullablePuja(FriendlyByteBuf ds, Building b, PujaSacrifice puja) {
        boolean isnull = ds.readBoolean();
        if (isnull) {
            return null;
        }
        short type = ds.readShort();
        if (puja == null) {
            puja = new PujaSacrifice(b, type);
        }
        short enchantmentId = ds.readShort();
        puja.type = type;
        puja.currentTarget = null;
        for (var target : type == 0 ? PujaSacrifice.PUJA_TARGETS : PujaSacrifice.MAYAN_TARGETS) {
            if (net.minecraft.core.Registry.ENCHANTMENT.getId(target.enchantment) == enchantmentId) puja.currentTarget = target;
        }
        puja.pujaProgress = ds.readShort();
        puja.offeringNeeded = ds.readInt();
        puja.offeringProgress = ds.readInt();
        return puja;
    }

    public static ConcurrentHashMap<Point, Integer> readPointIntegerMap(FriendlyByteBuf ds) {
        ConcurrentHashMap<Point, Integer> map = new ConcurrentHashMap<Point, Integer>();
        int nb = ds.readInt();
        for (int i = 0; i < nb; ++i) {
            Point p = StreamReadWrite.readNullablePoint(ds);
            map.put(p, ds.readInt());
        }
        return map;
    }

    public static CopyOnWriteArrayList<Point> readPointList(FriendlyByteBuf ds) {
        CopyOnWriteArrayList<Point> v = new CopyOnWriteArrayList<Point>();
        int nb = ds.readInt();
        for (int i = 0; i < nb; ++i) {
            v.add(StreamReadWrite.readNullablePoint(ds));
        }
        return v;
    }

    public static ConcurrentHashMap<BuildingProject.EnumProjects, CopyOnWriteArrayList<BuildingProject>> readProjectListList(FriendlyByteBuf ds, Culture culture) {
        ConcurrentHashMap<BuildingProject.EnumProjects, CopyOnWriteArrayList<BuildingProject>> v = new ConcurrentHashMap<BuildingProject.EnumProjects, CopyOnWriteArrayList<BuildingProject>>();
        int nb = ds.readInt();
        for (int i = 0; i < nb; ++i) {
            int nb2 = ds.readInt();
            CopyOnWriteArrayList<BuildingProject> v2 = new CopyOnWriteArrayList<BuildingProject>();
            for (int j = 0; j < nb2; ++j) {
                v2.add(StreamReadWrite.readNullableBuildingProject(ds, culture));
            }
            v.put(BuildingProject.EnumProjects.getById(i), v2);
        }
        return v;
    }

    public static Collection<String> readStringCollection(FriendlyByteBuf ds) {
        return StreamReadWrite.readStringList(ds);
    }

    public static CopyOnWriteArrayList<String> readStringList(FriendlyByteBuf ds) {
        CopyOnWriteArrayList<String> v = new CopyOnWriteArrayList<String>();
        int nb = ds.readInt();
        for (int i = 0; i < nb; ++i) {
            v.add(StreamReadWrite.readNullableString(ds));
        }
        return v;
    }

    public static String[][] readStringStringArray(FriendlyByteBuf ds) {
        String[][] strings = new String[ds.readInt()][];
        for (int i = 0; i < strings.length; ++i) {
            String[] array = new String[ds.readInt()];
            for (int j = 0; j < array.length; ++j) {
                array[j] = StreamReadWrite.readNullableString(ds);
            }
            strings[i] = array;
        }
        return strings;
    }

    public static HashMap<String, List<String>> readStringStringListMap(FriendlyByteBuf ds) {
        HashMap<String, List<String>> v = new HashMap<String, List<String>>();
        int nb = ds.readInt();
        for (int i = 0; i < nb; ++i) {
            String key = ds.readUtf(2048);
            v.put(key, StreamReadWrite.readStringList(ds));
        }
        return v;
    }

    public static HashMap<String, String> readStringStringMap(FriendlyByteBuf ds) {
        HashMap<String, String> v = new HashMap<String, String>();
        int nb = ds.readInt();
        for (int i = 0; i < nb; ++i) {
            String key = ds.readUtf(2048);
            v.put(key, StreamReadWrite.readNullableString(ds));
        }
        return v;
    }

    public static List<VillagerRecord> readVillagerRecordList(MillWorldData mw, FriendlyByteBuf ds) {
        ArrayList<VillagerRecord> v = new ArrayList<VillagerRecord>();
        int nb = ds.readInt();
        for (int i = 0; i < nb; ++i) {
            v.add(StreamReadWrite.readNullableVillagerRecord(mw, ds));
        }
        return v;
    }

    public static Map<Long, VillagerRecord> readVillagerRecordMap(MillWorldData mw, FriendlyByteBuf ds) {
        HashMap<Long, VillagerRecord> v = new HashMap<Long, VillagerRecord>();
        int nb = ds.readInt();
        for (int i = 0; i < nb; ++i) {
            Long key = ds.readLong();
            v.put(key, StreamReadWrite.readNullableVillagerRecord(mw, ds));
        }
        return v;
    }

    public static void writeBooleanList(List<Boolean> list, FriendlyByteBuf data) {
        data.writeInt(list.size());
        for (Boolean b : list) {
            data.writeBoolean(b.booleanValue());
        }
    }

    public static void writeBuildingLocationList(List<BuildingLocation> bls, FriendlyByteBuf data) {
        data.writeInt(bls.size());
        for (BuildingLocation bl : bls) {
            StreamReadWrite.writeNullableBuildingLocation(bl, data);
        }
    }

    public static void writeBuildingPlanInfo(BuildingPlan plan, FriendlyByteBuf data) {
        data.writeUtf(plan.buildingKey);
        data.writeInt(plan.level);
        data.writeInt(plan.variation);
        StreamReadWrite.writeNullableString(plan.planName, data);
        StreamReadWrite.writeNullableString(plan.nativeName, data);
        StreamReadWrite.writeNullableString(plan.requiredGlobalTag, data);
        StreamReadWrite.writeStringList(plan.forbiddenTagsInVillage, data);
        StreamReadWrite.writeNullableString(plan.shop, data);
        data.writeBoolean(plan.isSubBuilding);
        data.writeInt(plan.price);
        data.writeInt(plan.reputation);
        data.writeInt(plan.version);
        StreamReadWrite.writeStringList(plan.maleResident, data);
        StreamReadWrite.writeStringList(plan.femaleResident, data);
        StreamReadWrite.writeStringList(plan.visitors, data);
        StreamReadWrite.writeStringList(plan.startingSubBuildings, data);
        StreamReadWrite.writeStringList(plan.subBuildings, data);
        StreamReadWrite.writeStringList(plan.tags, data);
    }

    public static void writeInventory(HashMap<InvItem, Integer> inventory, FriendlyByteBuf data) {
        data.writeInt(inventory.size());
        for (InvItem key : inventory.keySet()) {
            data.writeInt(Item.getId((Item)key.getItem()));
            data.writeInt(key.meta);
            data.writeInt(inventory.get(key).intValue());
        }
    }

    private static void writeItemStack(ItemStack stack, FriendlyByteBuf buffer) {
        buffer.writeItem(stack == null ? ItemStack.EMPTY : stack);
    }

    public static void writeItemStackList(List<ItemStack> values, FriendlyByteBuf data) {
        data.writeInt(values.size());
        for (ItemStack s : values) {
            StreamReadWrite.writeNullableItemStack(s, data);
        }
    }

    private static void writeNBTTagCompound(CompoundTag tag, FriendlyByteBuf buffer) {
        buffer.writeNbt(tag);
    }

    public static void writeNullableBuildingLocation(BuildingLocation bl, FriendlyByteBuf data) {
        data.writeBoolean(bl == null);
        if (bl != null) {
            data.writeBoolean(bl.isCustomBuilding);
            StreamReadWrite.writeNullableString(bl.planKey, data);
            StreamReadWrite.writeNullableString(bl.shop, data);
            data.writeInt(bl.minx);
            data.writeInt(bl.maxx);
            data.writeInt(bl.miny);
            data.writeInt(bl.maxy);
            data.writeInt(bl.minz);
            data.writeInt(bl.maxz);
            data.writeInt(bl.minxMargin);
            data.writeInt(bl.maxxMargin);
            data.writeInt(bl.minyMargin);
            data.writeInt(bl.maxyMargin);
            data.writeInt(bl.minzMargin);
            data.writeInt(bl.maxzMargin);
            data.writeInt(bl.orientation);
            data.writeInt(bl.length);
            data.writeInt(bl.width);
            data.writeInt(bl.level);
            data.writeInt(bl.getVariation());
            data.writeInt(bl.reputation);
            data.writeInt(bl.price);
            data.writeInt(bl.version);
            StreamReadWrite.writeNullablePoint(bl.pos, data);
            StreamReadWrite.writeNullablePoint(bl.chestPos, data);
            StreamReadWrite.writeNullablePoint(bl.sleepingPos, data);
            StreamReadWrite.writeNullablePoint(bl.sellingPos, data);
            StreamReadWrite.writeNullablePoint(bl.craftingPos, data);
            StreamReadWrite.writeNullablePoint(bl.shelterPos, data);
            StreamReadWrite.writeNullablePoint(bl.defendingPos, data);
            StreamReadWrite.writeNullableString(bl.culture.key, data);
            StreamReadWrite.writeStringList(bl.subBuildings, data);
            data.writeBoolean(bl.showTownHallSigns);
            data.writeBoolean(bl.upgradesAllowed);
            data.writeBoolean(bl.bedrocklevel);
            data.writeBoolean(bl.isSubBuildingLocation);
        }
    }

    public static void writeNullableBuildingProject(BuildingProject bp, FriendlyByteBuf data) {
        data.writeBoolean(bp == null);
        if (bp != null) {
            data.writeBoolean(bp.isCustomBuilding);
            StreamReadWrite.writeNullableString(bp.key, data);
            StreamReadWrite.writeNullableBuildingLocation(bp.location, data);
        }
    }

    public static void writeNullableGoods(TradeGood g, FriendlyByteBuf data) {
        data.writeBoolean(g == null);
        if (g != null) {
            data.writeUtf(g.culture.key);
            data.writeInt(Item.getId((Item)g.item.getItem()));
            data.writeByte(g.item.meta);
            StreamReadWrite.writeNullableString(g.requiredTag, data);
            StreamReadWrite.writeNullableString(g.travelBookCategory, data);
            data.writeBoolean(g.autoGenerate);
            data.writeInt(g.minReputation);
        }
    }

    public static void writeNullableItemStack(ItemStack is, FriendlyByteBuf data) {
        data.writeBoolean(is == null);
        if (is != null) {
            StreamReadWrite.writeItemStack(is, data);
        }
    }

    public static void writeNullablePoint(Point p, FriendlyByteBuf data) {
        data.writeBoolean(p == null);
        if (p != null) {
            data.writeInt(p.getiX());
            data.writeInt(p.getiY());
            data.writeInt(p.getiZ());
        }
    }

    public static void writeNullablePuja(PujaSacrifice puja, FriendlyByteBuf data) {
        data.writeBoolean(puja == null);
        if (puja != null) {
            data.writeShort((int)puja.type);
            if (puja.currentTarget != null) {
                data.writeShort(net.minecraft.core.Registry.ENCHANTMENT.getId(puja.currentTarget.enchantment));
            } else {
                data.writeShort(-1);
            }
            data.writeShort((int)puja.pujaProgress);
            data.writeInt(puja.offeringNeeded);
            data.writeInt(puja.offeringProgress);
        }
    }

    public static void writeNullableQuestInstance(QuestInstance qi, FriendlyByteBuf ds) {
        ds.writeBoolean(qi == null);
        if (qi != null) {
            ds.writeLong(qi.uniqueid);
            ds.writeUtf(qi.quest.key);
            ds.writeUUID(qi.profile.uuid);
            ds.writeByte(qi.currentStep);
            ds.writeLong(qi.startTime);
            ds.writeLong(qi.currentStepStart);
            ds.writeByte(qi.villagers.size());
            for (String key : qi.villagers.keySet()) {
                ds.writeUtf(key);
                StreamReadWrite.writeNullableQuestVillager(qi.villagers.get(key), ds);
            }
        }
    }

    public static void writeNullableQuestVillager(QuestInstanceVillager v, FriendlyByteBuf data) {
        data.writeBoolean(v == null);
        if (v != null) {
            StreamReadWrite.writeNullablePoint(v.townHall, data);
            data.writeLong(v.id);
        }
    }

    public static void writeNullableResourceLocation(ResourceLocation rs, FriendlyByteBuf data) {
        data.writeBoolean(rs == null);
        if (rs != null) {
            data.writeUtf(rs.toString());
        }
    }

    public static void writeNullableString(String s, FriendlyByteBuf data) {
        data.writeBoolean(s == null);
        if (s != null) {
            data.writeUtf(s);
        }
    }

    public static void writeNullableUUID(UUID uuid, FriendlyByteBuf data) {
        data.writeBoolean(uuid == null);
        if (uuid != null) {
            data.writeUUID(uuid);
        }
    }

    public static void writeNullableVillagerRecord(VillagerRecord vr, FriendlyByteBuf data) {
        data.writeBoolean(vr == null);
        if (vr != null) {
            data.writeLong(vr.getVillagerId());
            StreamReadWrite.writeNullableString(vr.type, data);
            StreamReadWrite.writeNullableString(vr.firstName, data);
            StreamReadWrite.writeNullableString(vr.familyName, data);
            StreamReadWrite.writeNullableResourceLocation(vr.texture, data);
            data.writeInt(vr.nb);
            data.writeInt(vr.gender);
            data.writeInt(vr.size);
            data.writeFloat(vr.scale);
            data.writeBoolean(vr.rightHanded);
            StreamReadWrite.writeNullableString(vr.getCulture().key, data);
            StreamReadWrite.writeNullableString(vr.fathersName, data);
            StreamReadWrite.writeNullableString(vr.mothersName, data);
            StreamReadWrite.writeNullableString(vr.spousesName, data);
            StreamReadWrite.writeNullableString(vr.maidenName, data);
            data.writeBoolean(vr.killed);
            data.writeBoolean(vr.raidingVillage);
            data.writeBoolean(vr.awayraiding);
            data.writeBoolean(vr.awayhired);
            StreamReadWrite.writeNullablePoint(vr.getHousePos(), data);
            StreamReadWrite.writeNullablePoint(vr.getTownHallPos(), data);
            StreamReadWrite.writeNullablePoint(vr.originalVillagePos, data);
            data.writeLong(vr.raiderSpawn);
            StreamReadWrite.writeInventory(vr.inventory, data);
            StreamReadWrite.writeStringList(vr.questTags, data);
        }
    }

    public static void writePointIntegerMap(Map<Point, Integer> map, FriendlyByteBuf data) {
        data.writeInt(map.size());
        for (Point p : map.keySet()) {
            StreamReadWrite.writeNullablePoint(p, data);
            data.writeInt(map.get(p).intValue());
        }
    }

    public static void writePointList(List<Point> points, FriendlyByteBuf data) {
        data.writeInt(points.size());
        for (Point p : points) {
            StreamReadWrite.writeNullablePoint(p, data);
        }
    }

    public static void writeProjectListList(Map<BuildingProject.EnumProjects, CopyOnWriteArrayList<BuildingProject>> projects, FriendlyByteBuf data) {
        data.writeInt(BuildingProject.EnumProjects.values().length);
        for (BuildingProject.EnumProjects ep : BuildingProject.EnumProjects.values()) {
            if (projects.containsKey((Object)ep)) {
                data.writeInt(projects.get((Object)ep).size());
                for (BuildingProject bp : projects.get((Object)ep)) {
                    StreamReadWrite.writeNullableBuildingProject(bp, data);
                }
                continue;
            }
            data.writeInt(0);
        }
    }

    public static void writeStringCollection(Collection<String> strings, FriendlyByteBuf data) {
        data.writeInt(strings.size());
        for (String s : strings) {
            StreamReadWrite.writeNullableString(s, data);
        }
    }

    public static void writeStringList(List<String> strings, FriendlyByteBuf data) {
        data.writeInt(strings.size());
        for (String s : strings) {
            StreamReadWrite.writeNullableString(s, data);
        }
    }

    public static void writeStringStringArray(String[][] strings, FriendlyByteBuf data) {
        data.writeInt(strings.length);
        for (String[] array : strings) {
            data.writeInt(array.length);
            for (String s : array) {
                StreamReadWrite.writeNullableString(s, data);
            }
        }
    }

    public static void writeStringStringListMap(Map<String, List<String>> strings, FriendlyByteBuf data) {
        if (strings == null) {
            data.writeInt(0);
            return;
        }
        data.writeInt(strings.size());
        for (String key : strings.keySet()) {
            data.writeUtf(key);
            StreamReadWrite.writeStringList(strings.get(key), data);
        }
    }

    public static void writeStringStringMap(Map<String, String> strings, FriendlyByteBuf data) {
        if (strings == null) {
            data.writeInt(0);
            return;
        }
        data.writeInt(strings.size());
        for (String s : strings.keySet()) {
            data.writeUtf(s);
            StreamReadWrite.writeNullableString(strings.get(s), data);
        }
    }

    public static void writeVillagerRecordList(List<VillagerRecord> vrecords, FriendlyByteBuf data) {
        data.writeInt(vrecords.size());
        for (VillagerRecord vr : vrecords) {
            StreamReadWrite.writeNullableVillagerRecord(vr, data);
        }
    }

    public static void writeVillagerRecordMap(Map<Long, VillagerRecord> vrecords, FriendlyByteBuf data) {
        if (vrecords == null) {
            data.writeInt(0);
            return;
        }
        data.writeInt(vrecords.size());
        for (Long id : vrecords.keySet()) {
            data.writeLong(id.longValue());
            StreamReadWrite.writeNullableVillagerRecord(vrecords.get(id), data);
        }
    }
}

