/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityHanging
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.SoundEvents
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.network.PacketBuffer
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.World
 *  net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package org.millenaire.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameRules;
import net.minecraft.network.protocol.Packet;
import net.minecraftforge.network.NetworkHooks;
import java.util.ArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.item.MillItems;
import org.millenaire.common.utilities.BlockItemUtilities;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;

public class EntityWallDecoration
extends HangingEntity
implements IEntityAdditionalSpawnData {
    public static final ResourceLocation WALL_DECORATION = new ResourceLocation("millenaire", "walldecoration");
    public static final int NORMAN_TAPESTRY = 1;
    public static final int INDIAN_STATUE = 2;
    public static final int MAYAN_STATUE = 3;
    public static final int BYZANTINE_ICON_SMALL = 4;
    public static final int BYZANTINE_ICON_MEDIUM = 5;
    public static final int BYZANTINE_ICON_LARGE = 6;
    public static final int HIDE_HANGING = 7;
    public static final int WALL_CARPET_SMALL = 8;
    public static final int WALL_CARPET_MEDIUM = 9;
    public static final int WALL_CARPET_LARGE = 10;
    public EnumWallDecoration millArt = EnumWallDecoration.Griffon;
    public int type = NORMAN_TAPESTRY;

    public static EntityWallDecoration createWallDecoration(Level world, Point p, int type) {
        Direction facing = EntityWallDecoration.guessOrientation(world, p);
        return new EntityWallDecoration(world, p.getBlockPos(), facing, type, true);
    }

    private static Direction guessOrientation(Level world, Point p) {
        if (BlockItemUtilities.isBlockSolid(WorldUtilities.getBlock(world, p.getNorth()))) {
            return Direction.SOUTH;
        }
        if (BlockItemUtilities.isBlockSolid(WorldUtilities.getBlock(world, p.getSouth()))) {
            return Direction.NORTH;
        }
        if (BlockItemUtilities.isBlockSolid(WorldUtilities.getBlock(world, p.getEast()))) {
            return Direction.WEST;
        }
        if (BlockItemUtilities.isBlockSolid(WorldUtilities.getBlock(world, p.getWest()))) {
            return Direction.EAST;
        }
        return Direction.WEST;
    }

    public EntityWallDecoration(EntityType<? extends EntityWallDecoration> entityType, Level world) {
        super(entityType, world);
    }

    public EntityWallDecoration(Level world) {
        this(MillDecorationEntities.WALL_DECORATION, world);
    }

    public EntityWallDecoration(Level world, BlockPos pos, Direction facing, int type, boolean largestPossible) {
        super(MillDecorationEntities.WALL_DECORATION, world, pos);
        this.type = type;
        ArrayList<EnumWallDecoration> arraylist = new ArrayList<EnumWallDecoration>();
        int maxSize = 0;
        for (EnumWallDecoration enumart : EnumWallDecoration.values()) {
            if (enumart.type != type) continue;
            this.millArt = enumart;
            this.setDirection(facing);
            if (!this.survives()) continue;
            if (!largestPossible && enumart.sizeX * enumart.sizeY > maxSize) {
                arraylist.clear();
            }
            arraylist.add(enumart);
            maxSize = enumart.sizeX * enumart.sizeY;
        }
        if (arraylist.size() > 0) {
            this.millArt = (EnumWallDecoration)MillCommonUtilities.getWeightedChoice(arraylist, null);
        }
        if (MillConfigValues.LogBuildingPlan >= 1) {
            MillLog.major((Object)this, "Creating wall decoration: " + pos + "/" + facing + "/" + type + "/" + largestPossible + ". Result: " + this.millArt.title + " picked among " + arraylist.size());
        }
        this.setDirection(facing);
    }

    @OnlyIn(value=Dist.CLIENT)
    public EntityWallDecoration(Level world, int type) {
        this(world);
        this.millArt = findArt("", type);
        this.type = this.millArt.type;
    }

    public Item getDropItem() {
        if (this.type == 1) {
            return MillItems.TAPESTRY;
        }
        if (this.type == 2) {
            return MillItems.INDIAN_STATUE;
        }
        if (this.type == 3) {
            return MillItems.MAYAN_STATUE;
        }
        if (this.type == 4) {
            return MillItems.BYZANTINE_ICON_SMALL;
        }
        if (this.type == 5) {
            return MillItems.BYZANTINE_ICON_MEDIUM;
        }
        if (this.type == 6) {
            return MillItems.BYZANTINE_ICON_LARGE;
        }
        if (this.type == 7) {
            return MillItems.HIDEHANGING;
        }
        if (this.type == 8) {
            return MillItems.WALLCARPETSMALL;
        }
        if (this.type == 9) {
            return MillItems.WALLCARPETMEDIUM;
        }
        if (this.type == 10) {
            return MillItems.WALLCARPETLARGE;
        }
        MillLog.error((Object)this, "Unknown walldecoration type: " + this.type);
        return null;
    }

    public int getHeight() {
        return this.millArt == null ? 16 : this.millArt.sizeY;
    }

    public int getWidth() {
        return this.millArt == null ? 16 : this.millArt.sizeX;
    }

    public void dropItem(Entity brokenEntity) {
        if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.playSound(SoundEvents.PAINTING_BREAK, 1.0f, 1.0f);
            if (brokenEntity instanceof Player) {
                Player entityplayer = (Player)brokenEntity;
                if (entityplayer.getAbilities().instabuild) {
                    return;
                }
            }
            this.spawnAtLocation(new ItemStack(this.getDropItem()), 0.0f);
        }
    }

    public void tick() {
        super.tick();
    }

    public boolean survives() {
        return super.survives();
    }

    public void playPlacementSound() {
        this.playSound(SoundEvents.PAINTING_PLACE, 1.0f, 1.0f);
    }

    public static EnumWallDecoration findArt(String title, int type) {
        for (EnumWallDecoration art : EnumWallDecoration.values()) {
            if (art.title.equals(title)) return art;
        }
        for (EnumWallDecoration art : EnumWallDecoration.values()) {
            if (art.type == type) return art;
        }
        return EnumWallDecoration.Griffon;
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        millArt = findArt(tag.getString("Motive"), tag.getInt("Type"));
        type = millArt.type;
        super.readAdditionalSaveData(tag);
        setDirection(Direction.from2DDataValue(tag.getByte("Facing")));
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Type", type);
        tag.putString("Motive", millArt.title);
        tag.putByte("Facing", (byte) direction.get2DDataValue());
        super.addAdditionalSaveData(tag);
    }

    @Override public void readSpawnData(FriendlyByteBuf data) {
        int savedType = data.readInt();
        millArt = findArt(data.readUtf(2048), savedType);
        type = millArt.type;
        pos = data.readBlockPos();
        setDirection(Direction.from2DDataValue(data.readByte()));
    }

    @Override public void writeSpawnData(FriendlyByteBuf data) {
        data.writeInt(type);
        data.writeUtf(millArt.title, 2048);
        data.writeBlockPos(pos);
        data.writeByte(direction.get2DDataValue());
    }

    @Override public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override public ItemStack getPickResult() {
        Item item = getDropItem();
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override public void moveTo(double x, double y, double z, float yaw, float pitch) {
        setPos(x, y, z);
    }

    @Override public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
        BlockPos anchor = pos.offset(x - getX(), y - getY(), z - getZ());
        setPos(anchor.getX(), anchor.getY(), anchor.getZ());
    }

    @Override public String toString() {
        return "Tapestry (" + millArt.title + ") " + super.toString();
    }

    public static enum EnumWallDecoration implements MillCommonUtilities.WeightedChoice
    {
        Griffon("Griffon", 16, 16, 0, 0, 1),
        Oiseau("Oiseau", 16, 16, 16, 0, 1),
        CorbeauRenard("CorbeauRenard", 32, 16, 32, 0, 1),
        Serment("Serment", 80, 48, 0, 16, 1),
        MortHarold("MortHarold", 64, 48, 80, 16, 1),
        Drakar("Drakar", 96, 48, 144, 16, 1),
        MontStMichel("MontStMichel", 48, 32, 0, 64, 1),
        Bucherons("Bucherons", 48, 32, 48, 64, 1),
        Cuisine("Cuisine", 48, 32, 96, 64, 1),
        Flotte("Flotte", 240, 48, 0, 96, 1),
        Chasse("Chasse", 96, 48, 0, 144, 1),
        Siege("Siege", 256, 48, 0, 192, 1),
        Ganesh("Ganesh", 32, 48, 0, 0, 2),
        Kali("Kali", 32, 48, 32, 0, 2),
        Shiva("Shiva", 32, 48, 64, 0, 2),
        Osiyan("Osiyan", 32, 48, 96, 0, 2),
        Durga("Durga", 32, 48, 128, 0, 2),
        MayanTeal("MayanTeal", 32, 32, 0, 48, 3),
        MayanGold("MayanGold", 32, 32, 32, 48, 3),
        LargeJesus("LargeJesus", 32, 48, 0, 80, 6),
        LargeVirgin("LargeVirgin", 32, 48, 32, 80, 6),
        MediumVirgin1("MediumVirgin1", 32, 32, 0, 128, 5),
        MediumVirgin2("MediumVirgin2", 32, 32, 32, 128, 5),
        SmallJesus1("SmallJesus1", 16, 16, 0, 160, 4),
        SmallJesus2("SmallJesus2", 16, 16, 16, 160, 4),
        SmallSaint1("SmallSaint1", 16, 16, 32, 160, 4),
        SmallAngel1("SmallAngel1", 16, 16, 48, 160, 4),
        SmallVirgin1("SmallVirgin1", 16, 16, 64, 160, 4),
        SmallAngel2("SmallAngel2", 16, 16, 80, 160, 4),
        HideSmallCow("HideSmallCow", 16, 16, 0, 176, 7, 10),
        HideSmallRabbit("HideSmallRabbit", 16, 16, 16, 176, 7, 10),
        HideSmallSpider("HideSmallSpider", 16, 16, 32, 176, 7, 1),
        HideLargeCow("HideLargeCow", 32, 32, 0, 192, 7, 10),
        HideLargeBear("HideLargeBear", 32, 32, 32, 192, 7, 5),
        HideLargeZombie("HideLargeZombie", 32, 32, 64, 192, 7, 1),
        HideLargeWolf("HideLargeWolf", 32, 32, 96, 192, 7, 5),
        WallCarpet1("WallCarpet1", 16, 32, 0, 224, 8),
        WallCarpet2("WallCarpet2", 16, 32, 16, 224, 8),
        WallCarpet3("WallCarpet3", 16, 32, 32, 224, 8),
        WallCarpet4("WallCarpet4", 16, 32, 48, 224, 8),
        WallCarpet5("WallCarpet5", 16, 32, 64, 224, 8),
        WallCarpet6("WallCarpet6", 16, 32, 80, 224, 8),
        WallCarpet7("WallCarpet7", 16, 32, 96, 224, 8),
        WallCarpet8("WallCarpet8", 32, 48, 160, 176, 9),
        WallCarpet9("WallCarpet9", 32, 48, 192, 176, 9),
        WallCarpet10("WallCarpet10", 32, 48, 224, 176, 9),
        WallCarpet11("WallCarpet11", 48, 32, 112, 224, 10),
        WallCarpet12("WallCarpet12", 48, 32, 160, 224, 10),
        WallCarpet13("WallCarpet13", 48, 32, 208, 224, 10);

        public static final int maxArtTitleLength;
        public final String title;
        public final int sizeX;
        public final int sizeY;
        public final int offsetX;
        public final int offsetY;
        public final int type;
        private final int weight;

        private EnumWallDecoration(String title, int sizeX, int sizeY, int offsetX, int offsetY, int type) {
            this.title = title;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.type = type;
            this.weight = 1;
        }

        private EnumWallDecoration(String title, int sizeX, int sizeY, int offsetX, int offsetY, int type, int weight) {
            this.title = title;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.type = type;
            this.weight = weight;
        }

        @Override
        public int getChoiceWeight(Player player) {
            return this.weight;
        }

        static {
            maxArtTitleLength = "SkullAndRoses".length();
        }
    }
}

