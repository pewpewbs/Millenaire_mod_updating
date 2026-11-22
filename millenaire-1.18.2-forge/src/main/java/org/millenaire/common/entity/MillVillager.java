/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockBed
 *  net.minecraft.block.BlockDoor
 *  net.minecraft.block.BlockFenceGate
 *  net.minecraft.block.BlockHorizontal
 *  net.minecraft.block.BlockLeaves
 *  net.minecraft.block.properties.IProperty
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityCreature
 *  net.minecraft.entity.EntityList
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.SharedMonsterAttributes
 *  net.minecraft.entity.ai.attributes.AttributeModifier
 *  net.minecraft.entity.ai.attributes.IAttributeInstance
 *  net.minecraft.entity.item.EntityItem
 *  net.minecraft.entity.monster.EntityCreeper
 *  net.minecraft.entity.monster.EntityMob
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.projectile.EntityArrow
 *  net.minecraft.entity.projectile.EntityTippedArrow
 *  net.minecraft.init.Blocks
 *  net.minecraft.init.Items
 *  net.minecraft.init.SoundEvents
 *  net.minecraft.inventory.EntityEquipmentSlot
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.ItemTool
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.network.PacketBuffer
 *  net.minecraft.pathfinding.Path
 *  net.minecraft.pathfinding.PathPoint
 *  net.minecraft.util.DamageSource
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.EnumHandSide
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.AxisAlignedBB
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.world.EnumDifficulty
 *  net.minecraft.world.World
 *  net.minecraft.world.WorldServer
 *  net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData
 */
package org.millenaire.common.entity;

import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import org.millenaire.common.advancements.MillAdvancements;
import org.millenaire.common.block.BlockFruitLeaves;
import org.millenaire.common.block.BlockMillCrops;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.culture.Culture;
import org.millenaire.common.culture.CultureLanguage;
import org.millenaire.common.culture.VillagerType;
import org.millenaire.common.entity.VillagerConfig;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.item.ItemClothes;
import org.millenaire.common.item.ItemMillenaireBow;
import org.millenaire.common.item.TradeGood;
import org.millenaire.common.network.ServerSender;
import org.millenaire.common.network.StreamReadWrite;
import org.millenaire.common.pathing.atomicstryker.AS_PathEntity;
import org.millenaire.common.pathing.atomicstryker.AStarConfig;
import org.millenaire.common.pathing.atomicstryker.AStarNode;
import org.millenaire.common.pathing.atomicstryker.AStarPathPlannerJPS;
import org.millenaire.common.pathing.atomicstryker.AStarStatic;
import org.millenaire.common.pathing.atomicstryker.IAStarPathedEntity;
import org.millenaire.common.quest.QuestInstance;
import org.millenaire.common.utilities.BlockItemUtilities;
import org.millenaire.common.utilities.BlockStateUtilities;
import org.millenaire.common.utilities.DevModUtilities;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.ThreadSafeUtilities;
import org.millenaire.common.utilities.VillageUtilities;
import org.millenaire.common.utilities.WorldUtilities;
import org.millenaire.common.village.Building;
import org.millenaire.common.village.BuildingLocation;
import org.millenaire.common.village.ConstructionIP;
import org.millenaire.common.village.VillagerRecord;
import org.millenaire.common.world.MillWorldData;
import org.millenaire.common.world.UserProfile;

public abstract class MillVillager
extends PathfinderMob
implements IEntityAdditionalSpawnData,
IAStarPathedEntity {
    private static final UUID SPRINT_SPEED_BOOST_ID = UUID.fromString("B9766B59-8456-5632-BC1F-2EE2A276D836");
    private static final AttributeModifier SPRINT_SPEED_BOOST = new AttributeModifier(SPRINT_SPEED_BOOST_ID, "Sprint speed boost", 0.1, AttributeModifier.Operation.MULTIPLY_BASE);
    private static final double DEFAULT_MOVE_SPEED = 0.5;
    public static final int ATTACK_RANGE_DEFENSIVE = 20;
    private static final String FREE_CLOTHES = "free";
    private static final String NATURAL = "natural";
    private static final int CONCEPTION_CHANCE = 2;
    private static final int VISITOR_NB_NIGHTS_BEFORE_LEAVING = 5;
    public static final int MALE = 1;
    public static final int FEMALE = 2;
    public static final ResourceLocation GENERIC_VILLAGER = new ResourceLocation("millenaire", "genericvillager");
    public static final ResourceLocation GENERIC_ASYMM_FEMALE = new ResourceLocation("millenaire", "genericasimmfemale");
    public static final ResourceLocation GENERIC_SYMM_FEMALE = new ResourceLocation("millenaire", "genericsimmfemale");
    public static final ResourceLocation GENERIC_ZOMBIE = new ResourceLocation("millenaire", "genericzombie");
    private static ItemStack[] WOODDEN_HOE_STACK = new ItemStack[]{new ItemStack(Items.WOODEN_HOE, 1)};
    private static ItemStack[] WOODDEN_SHOVEL_STACK = new ItemStack[]{new ItemStack(Items.WOODEN_SHOVEL, 1)};
    private static ItemStack[] WOODDEN_PICKAXE_STACK = new ItemStack[]{new ItemStack(Items.WOODEN_PICKAXE, 1)};
    private static ItemStack[] WOODDEN_AXE_STACK = new ItemStack[]{new ItemStack(Items.WOODEN_AXE, 1)};
    static final int GATHER_RANGE = 20;
    private static final int HOLD_DURATION = 20;
    public static final int ATTACK_RANGE = 80;
    public static final int ARCHER_RANGE = 20;
    public static final int MAX_CHILD_SIZE = 20;
    private static final AStarConfig JPS_CONFIG_DEFAULT = new AStarConfig(true, false, false, false, true);
    private static final AStarConfig JPS_CONFIG_NO_LEAVES = new AStarConfig(true, false, false, false, false);
    public VillagerType vtype;
    public int action = 0;
    public String goalKey = null;
    private Goal.GoalInformation goalInformation = null;
    private Point pathDestPoint;
    private Building house = null;
    private Building townHall = null;
    public Point housePoint = null;
    public Point prevPoint = null;
    public Point townHallPoint = null;
    public boolean extraLog = false;
    public String firstName = "";
    public String familyName = "";
    public ItemStack heldItem = ItemStack.EMPTY;
    public ItemStack heldItemOffHand = ItemStack.EMPTY;
    public long timer = 0L;
    public long actionStart = 0L;
    public boolean allowRandomMoves = false;
    public boolean stopMoving = false;
    public int gender = 0;
    public boolean registered = false;
    public int longDistanceStuck;
    public boolean nightActionPerformed = false;
    public long speech_started = 0L;
    public HashMap<InvItem, Integer> inventory;
    private BlockState previousCropState = Blocks.AIR.defaultBlockState();
    public long pathingTime;
    public long timeSinceLastPathingTimeDisplay;
    private long villagerId = -1L;
    public int nbPathsCalculated = 0;
    public int nbPathNoStart = 0;
    public int nbPathNoEnd = 0;
    public int nbPathAborted = 0;
    public int nbPathFailure = 0;
    public long goalStarted = 0L;
    public int constructionJobId = -1;
    public int heldItemCount = 0;
    public int heldItemId = -1;
    public int heldItemOffHandId = -1;
    public String speech_key = null;
    public int speech_variant = 0;
    public String dialogueKey = null;
    public int dialogueRole = 0;
    public long dialogueStart = 0L;
    public char dialogueColour = (char)102;
    public boolean dialogueChat = false;
    public String dialogueTargetFirstName = null;
    public String dialogueTargetLastName = null;
    private Point doorToClose = null;
    public int visitorNbNights = 0;
    public int foreignMerchantStallId = -1;
    public boolean lastAttackByPlayer = false;
    public HashMap<Goal, Long> lastGoalTime = new HashMap();
    public String hiredBy = null;
    public boolean aggressiveStance = false;
    public long hiredUntil = 0L;
    public boolean isUsingBow;
    public boolean isUsingHandToHand;
    public boolean isRaider = false;
    public AStarPathPlannerJPS pathPlannerJPS;
    public AS_PathEntity pathEntity;
    public int updateCounter = 0;
    public long client_lastupdated;
    public MillWorldData mw;
    private volatile boolean pathFailedSincelastTick = false;
    private long nextVanillaPathAttempt = 0L;
    private long nextStockGateProbeTick = 0L;
    // A resident who entered a walled courtyard through a gate should prefer
    // the same opening when leaving for a destination on the other side.
    private BlockPos recentlyUsedGate = null;
    private long recentlyUsedGateTick = Long.MIN_VALUE;
    private long nextGateExitAttemptTick = 0L;
    private final org.millenaire.common.pathing.MovementWatchdog movementWatchdog = new org.millenaire.common.pathing.MovementWatchdog();
    private long nextPathFailureDiagnostic = 0L;
    private final PathFailureRecovery pathFailureRecovery = new PathFailureRecovery();
    private volatile List<AStarNode> pathCalculatedSinceLastTick = null;
    private int localStuck = 0;
    private final ResourceLocation[] clothTexture = new ResourceLocation[2];
    private String clothName = null;
    public boolean shouldLieDown = false;
    public LinkedHashMap<TradeGood, Integer> merchantSells = new LinkedHashMap();
    public ResourceLocation texture = null;
    private int attackTime;
    public boolean isDeadOnServer = false;
    public boolean travelBookMockVillager = false;

    public static MillVillager createMockVillager(VillagerRecord villagerRecord, Level world) {
        MillVillager villager = MillVillagerEntities.create(villagerRecord.getType().getEntityName(), world);
        if (villager == null) {
            MillLog.error(villagerRecord, "Could not create mock villager of dynamic type: " + villagerRecord.getType() + " entity: " + villagerRecord.getType().getEntityName());
            return null;
        }
        villager.vtype = villagerRecord.getType();
        villager.gender = villagerRecord.getType().gender;
        villager.firstName = villagerRecord.firstName;
        villager.familyName = villagerRecord.familyName;
        villager.texture = villagerRecord.texture;
        villager.setHealth(villager.getMaxHealth());
        villager.getAttribute(Attributes.MAX_HEALTH).setBaseValue((double)villagerRecord.getType().health);
        villager.updateClothTexturePath();
        return villager;
    }

    public static MillVillager createVillager(VillagerRecord villagerRecord, Level world, Point spawnPos, boolean respawn) {
        if (world.isClientSide || !(world instanceof ServerLevel)) {
            MillLog.printException("Tried creating a villager in client world: " + world, new Exception());
            return null;
        }
        if (villagerRecord == null) {
            MillLog.error(villagerRecord, "Tried creating villager from a null record");
            return null;
        }
        if (villagerRecord.getType() == null) {
            MillLog.error(null, "Tried creating villager of null type: " + villagerRecord.getType());
            return null;
        }
        MillVillager villager = MillVillagerEntities.create(villagerRecord.getType().getEntityName(), world);
        if (villager == null) {
            MillLog.error(villagerRecord, "Could not create villager of dynamic type: " + villagerRecord.getType() + " entity: " + villagerRecord.getType().getEntityName());
            return null;
        }
        villager.housePoint = villagerRecord.getHousePos();
        villager.townHallPoint = villagerRecord.getTownHallPos();
        villager.vtype = villagerRecord.getType();
        villager.setVillagerId(villagerRecord.getVillagerId());
        villager.gender = villagerRecord.getType().gender;
        villager.firstName = villagerRecord.firstName;
        villager.familyName = villagerRecord.familyName;
        villager.texture = villagerRecord.texture;
        villager.setHealth(villager.getMaxHealth());
        villager.getAttribute(Attributes.MAX_HEALTH).setBaseValue((double)villagerRecord.getType().health);
        villager.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((double)villagerRecord.getType().baseSpeed);
        villager.updateClothTexturePath();
        if (!respawn) {
            for (InvItem item : villagerRecord.getType().startingInv.keySet()) {
                villager.addToInv(item.getItem(), item.meta, (int)villagerRecord.getType().startingInv.get(item));
            }
        }
        villager.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        if (MillConfigValues.LogVillagerSpawn >= 1) {
            MillLog.major(villager, "Created new villager from record.");
        }
        return villager;
    }

    public static void readVillagerPacket(FriendlyByteBuf data) {
        try {
            long villager_id = data.readLong();
            if (Mill.clientWorld.getVillagerById(villager_id) != null) {
                Mill.clientWorld.getVillagerById(villager_id).readVillagerStreamdata(data);
            } else if (MillConfigValues.LogNetwork >= 2) {
                MillLog.minor(null, "readVillagerPacket for unknown villager: " + villager_id);
            }
        }
        catch (IOException e) {
            MillLog.printException(e);
        }
    }

    protected MillVillager(EntityType<? extends MillVillager> type, Level world) {
        super(type, world);
        this.level = world;
        this.mw = Mill.getMillWorld(world);
        this.inventory = new HashMap();
        this.setHealth(this.getMaxHealth());
        this.client_lastupdated = world.getDayTime();
        if (!world.isClientSide) {
            this.pathPlannerJPS = new AStarPathPlannerJPS(world, this, MillConfigValues.jpsPathing);
        }
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.5);
        if (MillConfigValues.LogVillagerSpawn >= 3) {
            Exception e = new Exception();
            MillLog.printException("Creating villager " + this + " in world: " + world, e);
        }
    }

    private Player findPlayerByName(String name) {
        if (name == null) return null;
        for (Player player : level.players()) {
            if (name.equals(player.getGameProfile().getName())) return player;
        }
        return null;
    }

    /** Commodity inventory cannot represent custom NBT; leave such drops intact. */
    public boolean pickUpGoods(ItemEntity entity) {
        if (entity == null || entity.isRemoved()) return false;
        ItemStack stack = entity.getItem();
        if (stack.isEmpty() || stack.hasTag()) return false;
        int count = stack.getCount();
        if ((long)this.countInv(stack.getItem(), stack.getDamageValue()) + count > Integer.MAX_VALUE) return false;
        this.addToInv(stack.getItem(), stack.getDamageValue(), count);
        entity.discard();
        return true;
    }

    public void addToInv(Block block, int nb) {
        this.addToInv(Item.byBlock((Block)block), 0, nb);
    }

    public void addToInv(Block block, int meta, int nb) {
        this.addToInv(Item.byBlock((Block)block), meta, nb);
    }

    public void addToInv(BlockState bs, int nb) {
        this.addToInv(InvItem.createInvItem(bs), nb);
    }

    public void restoreGoodsInventory(java.util.Map<InvItem,Integer> snapshot) {
        this.inventory.clear();this.inventory.putAll(snapshot);
        this.updateVillagerRecord();this.updateClothTexturePath();
    }

    public void addToInv(InvItem iv, int nb) {
        this.addToInv(iv.getItem(), iv.meta, nb);
    }

    public void addToInv(Item item, int nb) {
        this.addToInv(item, 0, nb);
    }

    public void addToInv(Item item, int meta, int nb) {
        InvItem key = InvItem.createInvItem(item, meta);
        if (this.inventory.containsKey(key)) {
            this.inventory.put(key, this.inventory.get(key) + nb);
        } else {
            this.inventory.put(key, nb);
        }
        this.updateVillagerRecord();
        this.updateClothTexturePath();
    }

    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new PathNavigateSimple(this, level);
    }

    public static AttributeSupplier.Builder createVillagerAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.MAX_HEALTH, 40.0);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void applyPathCalculatedSinceLastTick() {
        try {
            AS_PathEntity path = AStarStatic.translateAStarPathtoPathEntity(this.level, this.pathCalculatedSinceLastTick, this.getPathingConfig());
            // A builder may have changed the world after this snapshot was captured.
            for (int i = 1; i < path.getNodeCount(); i++) {
                Node node = path.getNode(i);
                if (!AStarStatic.isViable(this.level, node.x, node.y, node.z, 0, this.getPathingConfig())) {
                    org.millenaire.common.pathing.NavigationMetrics.stale();
                    this.nextVanillaPathAttempt = 0L;
                    return;
                }
            }
            this.registerNewPath(path);
        }
        catch (Exception e) {
            MillLog.printException("Exception when finding JPS path:", e);
        }
        finally { this.pathCalculatedSinceLastTick = null; }
    }

    public boolean attackEntity(Entity entity) {
        double distance = this.getPos().distanceTo(entity);
        if (this.vtype.isArcher && distance > 5.0 && this.hasBow()) {
            this.isUsingBow = true;
            this.attackEntity_testHiredGoon(entity);
            if (distance < 20.0 && entity instanceof LivingEntity) {
                if (this.attackTime <= 0) {
                    this.attackTime = 100;
                    this.swing(InteractionHand.MAIN_HAND);
                    float distanceFactor = (float)(distance / 20.0);
                    distanceFactor = Mth.clamp((float)distanceFactor, (float)0.1f, (float)1.0f);
                    this.attackEntityWithRangedAttack((LivingEntity)entity, distanceFactor);
                } else {
                    --this.attackTime;
                }
            }
        } else {
            if (this.attackTime <= 0 && distance < 2.0 && entity.getBoundingBox().maxY > this.getBoundingBox().minY && entity.getBoundingBox().minY < this.getBoundingBox().maxY) {
                this.attackTime = 20;
                this.swing(InteractionHand.MAIN_HAND);
                this.attackEntity_testHiredGoon(entity);
                return entity.hurt(DamageSource.mobAttack((LivingEntity)this), (float)this.getAttackStrength());
            }
            --this.attackTime;
            this.isUsingHandToHand = true;
        }
        return true;
    }

    private void attackEntity_testHiredGoon(Entity targetedEntity) {
        Player owner;
        if (targetedEntity instanceof Player && this.hiredBy != null && (owner = this.findPlayerByName(this.hiredBy)) != null && owner != targetedEntity) {
            MillAdvancements.MP_HIREDGOON.grant(owner);
        }
    }

    public boolean hurt(DamageSource ds, float i) {
        if (ds.getEntity() == null && ds != DamageSource.OUT_OF_WORLD) {
            return false;
        }
        boolean hadFullHealth = this.getMaxHealth() == this.getHealth();
        boolean b = super.hurt(ds, i);
        Entity entity = ds.getEntity();
        this.lastAttackByPlayer = false;
        if (entity != null && entity instanceof LivingEntity) {
            if (entity instanceof Player) {
                if (!((Player)entity).isSpectator() && !((Player)entity).isCreative()) {
                    this.lastAttackByPlayer = true;
                    Player player = (Player)entity;
                    if (!this.isRaider) {
                        UserProfile serverProfile;
                        if (this.vtype != null && !this.vtype.hostile && (serverProfile = VillageUtilities.getServerProfile(player.level, player)) != null) {
                            serverProfile.adjustReputation(this.getTownHall(), (int)(-i * 10.0f));
                        }
                        if (this.level.getDifficulty() != Difficulty.PEACEFUL && this.getHealth() < this.getMaxHealth() - 10.0f) {
                            this.setTarget((LivingEntity)entity);
                            this.clearGoal();
                            if (this.getTownHall() != null) {
                                this.getTownHall().callForHelp((LivingEntity)entity);
                            }
                        }
                        if (this.vtype != null && !this.vtype.hostile && hadFullHealth && (player.getItemInHand(InteractionHand.MAIN_HAND) == null || MillCommonUtilities.getItemWeaponDamage(player.getUseItem().getItem()) <= 1.0) && !this.level.isClientSide) {
                            ServerSender.sendTranslatedSentence(player, '6', "ui.communicationexplanations", new String[0]);
                        }
                    }
                    if (this.lastAttackByPlayer && this.getHealth() <= 0.0f) {
                        if (this.vtype != null && this.vtype.hostile) {
                            MillAdvancements.SELF_DEFENSE.grant(player);
                        } else {
                            MillAdvancements.DARK_SIDE.grant(player);
                        }
                    }
                }
            } else if (entity instanceof MillVillager) {
                MillVillager attackingVillager = (MillVillager)entity;
                if (this.isRaider != attackingVillager.isRaider || this.getTownHall() != attackingVillager.getTownHall()) {
                    this.setTarget((LivingEntity)entity);
                    this.clearGoal();
                    if (this.getTownHall() != null) {
                        this.getTownHall().callForHelp((LivingEntity)entity);
                    }
                }
            } else {
                this.setTarget((LivingEntity)entity);
                this.clearGoal();
                if (this.getTownHall() != null) {
                    this.getTownHall().callForHelp((LivingEntity)entity);
                }
            }
        }
        return b;
    }

    public void attackEntityWithRangedAttack(LivingEntity target, float distanceFactor) {
        Item item;
        AbstractArrow entityarrow = this.getArrow(distanceFactor);
        double d0 = target.getX() - this.getX();
        double d1 = target.getBoundingBox().minY + (double)(target.getBbHeight() / 3.0f) - entityarrow.getY();
        double d2 = target.getZ() - this.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        float speedFactor = 1.0f;
        float damageBonus = 0.0f;
        ItemStack weapon = this.getWeapon();
        if (weapon != null && (item = weapon.getItem()) instanceof ItemMillenaireBow) {
            ItemMillenaireBow bow = (ItemMillenaireBow)item;
            if (bow.speedFactor > speedFactor) {
                speedFactor = bow.speedFactor;
            }
            if (bow.damageBonus > damageBonus) {
                damageBonus = bow.damageBonus;
            }
        }
        entityarrow.setBaseDamage(entityarrow.getBaseDamage() + (double)damageBonus);
        entityarrow.shoot(d0, d1 + d3 * (double)0.2f, d2, 1.6f, (float)(14 - this.level.getDifficulty().getId() * 4) * speedFactor);
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0f, 1.0f / (this.getRandom().nextFloat() * 0.4f + 0.8f));
        this.level.addFreshEntity(entityarrow);
    }

    public boolean attemptChildConception() {
        int nbChildren = 0;
        for (MillVillager villager : this.getHouse().getKnownVillagers()) {
            if (!villager.isBaby()) continue;
            ++nbChildren;
        }
        if (nbChildren > 1) {
            if (MillConfigValues.LogChildren >= 3) {
                MillLog.debug(this, "Wife already has " + nbChildren + " children, no need for more.");
            }
            return true;
        }
        int nbChildVillage = this.getTownHall().countChildren();
        if (nbChildVillage > MillConfigValues.maxChildrenNumber) {
            if (MillConfigValues.LogChildren >= 3) {
                MillLog.debug(this, "Village already has " + nbChildVillage + ", no need for more.");
            }
            return true;
        }
        boolean couldMoveIn = false;
        for (Point housePoint : this.getTownHall().buildings) {
            Building house = this.mw.getBuilding(housePoint);
            if (house == null || house.equals(this.getHouse()) || !house.isHouse() || !house.canChildMoveIn(1, this.familyName) && !house.canChildMoveIn(2, this.familyName)) continue;
            couldMoveIn = true;
        }
        if (nbChildVillage > 5 && !couldMoveIn) {
            if (MillConfigValues.LogChildren >= 3) {
                MillLog.debug(this, "Village already has " + nbChildVillage + " and no slot is available for the new child.");
            }
            return true;
        }
        List<Entity> entities = WorldUtilities.getEntitiesWithinAABB(this.level, MillVillager.class, this.getPos(), 4, 2);
        boolean manFound = false;
        for (Entity ent : entities) {
            MillVillager villager = (MillVillager)ent;
            if (villager.gender != 1 || villager.isBaby()) continue;
            manFound = true;
        }
        if (!manFound) {
            return false;
        }
        if (MillConfigValues.LogChildren >= 3) {
            MillLog.debug(this, "Less than two kids and man present, trying for new child.");
        }
        boolean createChild = false;
        int conceptionChances = 2;
        InvItem conceptionFood = this.getConfig().getBestConceptionFood(this.getHouse());
        if (conceptionFood != null) {
            this.getHouse().takeGoods(conceptionFood, 1);
            conceptionChances += this.getConfig().foodsConception.get(conceptionFood).intValue();
        }
        if (MillCommonUtilities.randomInt(10) < conceptionChances) {
            createChild = true;
            if (MillConfigValues.LogChildren >= 2) {
                MillLog.minor(this, "Conceiving child. Food available: " + conceptionFood);
            }
        } else if (MillConfigValues.LogChildren >= 2) {
            MillLog.minor(this, "Failed to conceive child. Food available: " + conceptionFood);
        }
        if (MillConfigValues.DEV) {
            createChild = true;
        }
        if (createChild) {
            this.getHouse().createChild(this, this.getTownHall(), this.getRecord().spousesName);
        }
        return true;
    }

    public void calculateMerchantGoods() {
        for (InvItem key : this.vtype.foreignMerchantStock.keySet()) {
            if (this.getCulture().getTradeGood(key) == null || this.getBasicForeignMerchantPrice(key) <= 0) continue;
            this.merchantSells.put(this.getCulture().getTradeGood(key), this.getBasicForeignMerchantPrice(key));
        }
    }

    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public boolean canMeditate() {
        return this.vtype.canMeditate;
    }

    public boolean canPerformSacrifices() {
        return this.vtype.canPerformSacrifices;
    }

    public boolean canVillagerClearLeaves() {
        return !this.vtype.noleafclearing;
    }

    private void checkGoalHeldItems(Goal goal, Point target) throws Exception {
        if (this.heldItemCount > 20) {
            ItemStack[] heldItems = null;
            heldItems = target != null && target.horizontalDistanceTo((Entity)this) < (double)goal.range(this) ? goal.getHeldItemsDestination(this) : goal.getHeldItemsTravelling(this);
            if (heldItems != null && heldItems.length > 0) {
                this.heldItemId = (this.heldItemId + 1) % heldItems.length;
                this.heldItem = heldItems[this.heldItemId];
            }
            heldItems = null;
            heldItems = target != null && target.horizontalDistanceTo((Entity)this) < (double)goal.range(this) ? goal.getHeldItemsOffHandDestination(this) : goal.getHeldItemsOffHandTravelling(this);
            if (heldItems != null && heldItems.length > 0) {
                this.heldItemOffHandId = (this.heldItemOffHandId + 1) % heldItems.length;
                this.heldItemOffHand = heldItems[this.heldItemOffHandId];
            }
            this.heldItemCount = 0;
        }
        if (this.heldItemCount == 0 && goal.swingArms(this)) {
            this.swing(InteractionHand.MAIN_HAND);
        }
        ++this.heldItemCount;
    }

    public void checkGoals() throws Exception {
        Goal goal = Goal.goals.get(this.goalKey);
        if (goal == null) {
            MillLog.error(this, "Invalid goal key: " + this.goalKey);
            this.goalKey = null;
            return;
        }
        if (this.getGoalDestEntity() != null) {
            if (this.getGoalDestEntity().isRemoved()) {
                this.setGoalDestEntity(null);
                this.setPathDestPoint(null, 0);
            } else {
                this.setPathDestPoint(new Point(this.getGoalDestEntity()), 2);
            }
        }
        Point target = null;
        boolean continuingGoal = true;
        if (this.getPathDestPoint() != null) {
            target = this.getPathDestPoint();
            // Construction reach is measured to the block, not an alternate access node.
            if (goal != Goal.construction && !(goal instanceof org.millenaire.common.goal.GoalLumbermanChopTrees)
                    && !(goal instanceof org.millenaire.common.goal.GoalBringBackResourcesHome)
                    && this.pathEntity != null && this.pathEntity.canReach() && this.pathEntity.getNodeCount() > 0) {
                target = new Point(this.pathEntity.getEndNode());
            }
        }
        this.speakSentence(goal.sentenceKey());
        if (this.getGoalDestPoint() == null && this.getGoalDestEntity() == null) {
            goal.setVillagerDest(this);
            if (MillConfigValues.LogGeneralAI >= 2 && this.extraLog) {
                MillLog.minor(this, "Goal destination: " + this.getGoalDestPoint() + "/" + this.getGoalDestEntity());
            }
        } else if (goal.isWithinActionRange(this, target)) {
            if (this.actionStart == 0L) {
                this.stopMoving = goal.stopMovingWhileWorking();
                this.actionStart = this.level.getGameTime();
                this.shouldLieDown = goal.shouldVillagerLieDown();
                if (MillConfigValues.LogGeneralAI >= 2 && this.extraLog) {
                    MillLog.minor(this, "Starting action: " + this.actionStart);
                }
            }
            if (this.level.getGameTime() - this.actionStart >= (long)goal.actionDuration(this)) {
                if (goal.performAction(this)) {
                    this.clearGoal();
                    this.goalKey = goal.nextGoal(this);
                    this.stopMoving = false;
                    this.shouldLieDown = false;
                    this.heldItem = ItemStack.EMPTY;
                    this.heldItemOffHand = ItemStack.EMPTY;
                    continuingGoal = false;
                    if (MillConfigValues.LogGeneralAI >= 2 && this.extraLog) {
                        MillLog.minor(this, "Goal performed. Now doing: " + this.goalKey);
                    }
                } else {
                    this.stopMoving = goal.stopMovingWhileWorking();
                }
                this.actionStart = 0L;
                this.goalStarted = this.level.getGameTime();
            }
        } else {
            this.stopMoving = false;
            this.shouldLieDown = false;
        }
        if (!continuingGoal) {
            return;
        }
        if (goal.isStillValid(this)) {
            if (this.level.getGameTime() - this.goalStarted > goal.stuckDelay(this)) {
                boolean actionDone = goal.stuckAction(this);
                if (actionDone) {
                    this.goalStarted = this.level.getGameTime();
                }
                if (goal.isStillValid(this)) {
                    this.allowRandomMoves = goal.allowRandomMoves();
                    if (this.stopMoving) {
                        this.navigation.stop();
                        this.pathEntity = null;
                    }
                    this.checkGoalHeldItems(goal, target);
                }
            } else {
                this.checkGoalHeldItems(goal, target);
            }
        } else {
            this.stopMoving = false;
            this.shouldLieDown = false;
            goal.onComplete(this);
            this.clearGoal();
            this.goalKey = goal.nextGoal(this);
            this.heldItemCount = 21;
            this.heldItemId = -1;
            this.heldItemOffHandId = -1;
        }
    }

    public void clearGoal() {
        this.setGoalDestPoint(null);
        this.setGoalBuildingDestPoint(null);
        this.setGoalDestEntity(null);
        this.goalKey = null;
        this.shouldLieDown = false;
    }

    private boolean closeFenceGate(int i, int j, int k) {
        Point p = new Point(i, j, k);
        BlockState state = p.getBlockActualState(this.level);
        if (BlockItemUtilities.isFenceGate(state.getBlock()) && ((Boolean)state.getValue(FenceGateBlock.OPEN)).booleanValue()) {
            p.setBlockState(this.level, state.setValue(FenceGateBlock.OPEN, Boolean.valueOf(false)));
            return true;
        }
        return false;
    }

    public void computeChildScale() {
        if (this.getRecord() == null) {
            return;
        }
        this.getRecord().scale = this.getSize() == 20 ? (this.gender == 1 ? 0.9f : 0.8f) : 0.5f + (float)this.getSize() / 100.0f;
    }

    public float computeMaxHealth() {
        if (this.vtype == null || this.getRecord() == null) {
            return 40.0f;
        }
        if (this.isBaby()) {
            return 10 + this.getSize();
        }
        return this.vtype.health;
    }

    private List<Node> computeNewPath(Point dest) {
        this.pathCalculatedSinceLastTick = null;
        this.pathFailedSincelastTick = false;
        if (this.pathPlannerJPS != null) this.pathPlannerJPS.stopPathSearch(true);
        block9: {
            if (this.getPos().sameBlock(dest)) {
                return null;
            }
            try {
                Goal goal;
                if (this.goalKey != null && Goal.goals.containsKey(this.goalKey) && (goal = Goal.goals.get(this.goalKey)).isWithinActionRange(this, this.getPathDestPoint())) {
                    return null;
                }
                if (this.tryGroundRoute(dest)) return null;
                if (this.pathPlannerJPS.isBusy()) {
                    this.pathPlannerJPS.stopPathSearch(true);
                }
                AStarNode destNode = null;
                if (this.goalKey != null && (Goal.goals.get(this.goalKey) == Goal.construction
                        || Goal.goals.get(this.goalKey) instanceof org.millenaire.common.goal.GoalLumbermanChopTrees)) {
                    // Building search already accepts a walkable node near the target,
                    // including ground below a canopy. Do not offset that tolerance
                    // around a second access point outside the builder's working reach.
                    destNode = new AStarNode(dest.getiX(), dest.getiY(), dest.getiZ());
                } else {
                    AStarNode[] possibles = AStarStatic.getAccessNodesSorted(this.level, this.doubleToInt(this.getX()), this.doubleToInt(this.getY()), this.doubleToInt(this.getZ()), this.getPathDestPoint().getiX(), this.getPathDestPoint().getiY(), this.getPathDestPoint().getiZ(), this.getPathingConfig());
                    boolean stockGoal = isStockTransferGoal(Goal.goals.get(this.goalKey));
                    for (AStarNode candidate : possibles) {
                        // The closest geometrical node can be on the other side of a wall.
                        // A genuine stock access node must have line of sight to the stock point.
                        if (!stockGoal || canSeeStockFromNode(candidate.x, candidate.y,
                                candidate.z, dest)) {
                            destNode = candidate;
                            break;
                        }
                    }
                }
                if (destNode != null) {
                    Point startPos = this.getPos().getBelow();
                    if (!startPos.isBlockPassable(this.level) && !(startPos = startPos.getAbove()).isBlockPassable(this.level)) {
                        startPos = startPos.getAbove();
                    }
                    this.pathPlannerJPS.getPath(this.doubleToInt(this.getX()), this.doubleToInt(this.getY()), this.doubleToInt(this.getZ()), destNode.x, destNode.y, destNode.z, this.getPathingConfig());
                } else {
                    this.onNoPathAvailable();
                }
            }
            catch (ThreadSafeUtilities.ChunkAccessException e) {
                if (MillConfigValues.LogChunkLoader < 2) break block9;
                MillLog.minor(this, "Chunk access violation while calculating path.");
            }
        }
        return null;
    }

    public int countInv(Block block, int meta) {
        return this.countInv(InvItem.createInvItem(Item.byBlock((Block)block), meta));
    }

    public int countInv(BlockState blockState) {
        return this.countInv(InvItem.createInvItem(blockState));
    }

    public int countInv(InvItem key) {
        if (key.block == Blocks.OAK_LOG && key.meta == -1) {
            int count = 0;
            for (org.millenaire.common.block.WoodVariant wood : org.millenaire.common.block.WoodVariant.values())
                count += this.inventory.getOrDefault(InvItem.createInvItem(wood.log()), 0);
            return count;
        }
        if (key.meta == -1) {
            int count = 0;
            for (var entry : this.inventory.entrySet()) {
                InvItem item = entry.getKey();
                if (org.millenaire.common.item.LegacyItemStacks.matches(
                        org.millenaire.common.item.LegacyItemStacks.stack(item.getItem(), 1, item.meta), key.item, -1))
                    count += entry.getValue();
            }
            return count;
        }
        if (this.inventory.containsKey(key)) {
            return this.inventory.get(key);
        }
        return 0;
    }

    public int countInv(Item item) {
        return this.countInv(item, 0);
    }

    public int countInv(Item item, int meta) {
        return this.countInv(InvItem.createInvItem(item, meta));
    }

    public int countItemsAround(Item[] items, int radius) {
        List<Entity> list = WorldUtilities.getEntitiesWithinAABB(this.level, ItemEntity.class, this.getPos(), radius, radius);
        int count = 0;
        if (list != null) {
            for (int i = 0; i < list.size(); ++i) {
                if (list.get(i).getClass() != ItemEntity.class) continue;
                ItemEntity entity = (ItemEntity)list.get(i);
                if (entity.isRemoved()) continue;
                for (Item id : items) {
                    if (id != entity.getItem().getItem()) continue;
                    ++count;
                }
            }
        }
        return count;
    }

    public void despawnVillager() {
        Player owner;
        if (this.level.isClientSide) {
            return;
        }
        if (this.hiredBy != null && (owner = this.findPlayerByName(this.hiredBy)) != null) {
            ServerSender.sendTranslatedSentence(owner, '4', "hire.hiredied", this.func_70005_c_());
        }
        this.mw.clearVillagerOfId(this.getVillagerId());
        super.remove(Entity.RemovalReason.DISCARDED);
    }

    public void despawnVillagerSilent() {
        if (MillConfigValues.LogVillagerSpawn >= 3) {
            Exception e = new Exception();
            MillLog.printException("Despawning villager: " + this, e);
        }
        // Removing a duplicate must not unregister the retained resident.
        if (this.mw.getVillagerById(this.getVillagerId()) == this) {
            this.mw.clearVillagerOfId(this.getVillagerId());
        }
        super.remove(Entity.RemovalReason.DISCARDED);
    }

    public void detrampleCrops() {
        Point pos = this.getPos();
        Block previousBlock = previousCropState.getBlock();
        if (pos.sameBlock(this.prevPoint) && (previousBlock == Blocks.WHEAT || previousBlock instanceof BlockMillCrops)
                && this.getBlock(pos) != Blocks.AIR && this.getBlock(pos.getBelow()) == Blocks.DIRT) {
            this.setBlock(pos.getBelow(), Blocks.FARMLAND);
            this.setBlockstate(pos, previousCropState);
        }
        previousCropState = level.getBlockState(pos.getBlockPos());
    }

    public int doubleToInt(double input) {
        return AStarStatic.getIntCoordFromDoubleCoord(input);
    }

    // Entity equality/hashCode must remain Minecraft's runtime entity identity.
    // Persistent villager IDs are used explicitly by MillWorldData's maps.

    public void lookAt(Entity par1Entity, float par2, float par3) {
    }

    public void faceEntityMill(Entity entityIn, float par2, float par3) {
        this.getLookControl().setLookAt(entityIn, par2, par3);
    }

    public void facePoint(Point p, float par2, float par3) {
        double x = p.x + 0.5;
        double z = p.z + 0.5;
        double y = p.y + 1.0;
        this.getLookControl().setLookAt(x, y, z, 10.0f, (float)this.getMaxHeadXRot());
    }

    private void foreignMerchantUpdate() {
        if (this.foreignMerchantStallId < 0) {
            for (int i = 0; i < this.getHouse().getResManager().stalls.size() && this.foreignMerchantStallId < 0; ++i) {
                boolean taken = false;
                for (MillVillager v : this.getHouse().getKnownVillagers()) {
                    if (v.foreignMerchantStallId != i) continue;
                    taken = true;
                }
                if (taken) continue;
                this.foreignMerchantStallId = i;
            }
        }
        if (this.foreignMerchantStallId < 0) {
            this.foreignMerchantStallId = 0;
        }
    }

    private Goal getActiveGoal() {
        if (this.goalKey != null && Goal.goals.containsKey(this.goalKey)) {
            return Goal.goals.get(this.goalKey);
        }
        return null;
    }

    protected AbstractArrow getArrow(float distanceFactor) {
        Arrow entitytippedarrow = new Arrow(this.level, (LivingEntity)this);
        entitytippedarrow.setEnchantmentEffectsFromEntity((LivingEntity)this, distanceFactor);
        return entitytippedarrow;
    }

    public int getAttackStrength() {
        int attackStrength = this.vtype.baseAttackStrength;
        ItemStack weapon = this.getWeapon();
        if (weapon != null) {
            attackStrength = (int)((double)attackStrength + Math.ceil((float)MillCommonUtilities.getItemWeaponDamage(weapon.getItem()) / 2.0f));
        }
        return attackStrength;
    }

    public int getBasicForeignMerchantPrice(InvItem item) {
        if (this.getTownHall() == null) {
            return 0;
        }
        if (this.getCulture().getTradeGood(item) != null) {
            if (this.getCulture() != this.getTownHall().culture) {
                return (int)((double)this.getCulture().getTradeGood((InvItem)item).foreignMerchantPrice * 1.5);
            }
            return this.getCulture().getTradeGood((InvItem)item).foreignMerchantPrice;
        }
        return 0;
    }

    public float getBedOrientationInDegrees() {
        Block block;
        Point ref = this.getPos();
        if (this.getGoalDestPoint() != null) {
            ref = this.getGoalDestPoint();
        }
        if ((block = WorldUtilities.getBlock(this.level, ref)) instanceof BedBlock) {
            BlockState state = ref.getBlockActualState(this.level);
            Direction side = (Direction)state.getValue(HorizontalDirectionalBlock.FACING);
            if (side == Direction.SOUTH) {
                return 0.0f;
            }
            if (side == Direction.NORTH) {
                return 180.0f;
            }
            if (side == Direction.EAST) {
                return 270.0f;
            }
            if (side == Direction.WEST) {
                return 90.0f;
            }
        } else {
            if (WorldUtilities.getBlock(this.level, ref.getSouth()) == Blocks.AIR) {
                return 0.0f;
            }
            if (WorldUtilities.getBlock(this.level, ref.getWest()) == Blocks.AIR) {
                return 90.0f;
            }
            if (WorldUtilities.getBlock(this.level, ref.getNorth()) == Blocks.AIR) {
                return 180.0f;
            }
            if (WorldUtilities.getBlock(this.level, ref.getEast()) == Blocks.AIR) {
                return 270.0f;
            }
        }
        return 0.0f;
    }

    public DiggerItem getBestAxe() {
        InvItem bestItem = this.getConfig().getBestAxe(this);
        if (bestItem != null) {
            return (DiggerItem)bestItem.item;
        }
        return (DiggerItem)Items.WOODEN_AXE;
    }

    public ItemStack[] getBestAxeStack() {
        InvItem bestItem = this.getConfig().getBestAxe(this);
        if (bestItem != null) {
            return bestItem.staticStackArray;
        }
        return WOODDEN_AXE_STACK;
    }

    public ItemStack[] getBestHoeStack() {
        InvItem bestItem = this.getConfig().getBestHoe(this);
        if (bestItem != null) {
            return bestItem.staticStackArray;
        }
        return WOODDEN_HOE_STACK;
    }

    public DiggerItem getBestPickaxe() {
        InvItem bestItem = this.getConfig().getBestPickaxe(this);
        if (bestItem != null) {
            return (DiggerItem)bestItem.item;
        }
        return (DiggerItem)Items.WOODEN_PICKAXE;
    }

    public ItemStack[] getBestPickaxeStack() {
        InvItem bestItem = this.getConfig().getBestPickaxe(this);
        if (bestItem != null) {
            return bestItem.staticStackArray;
        }
        return WOODDEN_PICKAXE_STACK;
    }

    public DiggerItem getBestShovel() {
        InvItem bestItem = this.getConfig().getBestShovel(this);
        if (bestItem != null) {
            return (DiggerItem)bestItem.item;
        }
        return (DiggerItem)Items.WOODEN_SHOVEL;
    }

    public ItemStack[] getBestShovelStack() {
        InvItem bestItem = this.getConfig().getBestShovel(this);
        if (bestItem != null) {
            return bestItem.staticStackArray;
        }
        return WOODDEN_SHOVEL_STACK;
    }

    public Block getBlock(Point p) {
        return WorldUtilities.getBlock(this.level, p);
    }

    public int getBlockMeta(Point p) {
        return WorldUtilities.getBlockMeta(this.level, p);
    }

    public float getWalkTargetValue(BlockPos pos) {
        if (!this.allowRandomMoves) {
            if (MillConfigValues.LogPathing >= 3 && this.extraLog) {
                MillLog.debug(this, "Forbiding random moves. Current goal: " + Goal.goals.get(this.goalKey) + " Returning: " + -99999.0f);
            }
            return Float.NEGATIVE_INFINITY;
        }
        Point rp = new Point(pos);
        double dist = rp.distanceTo(this.housePoint);
        if (WorldUtilities.getBlock(this.level, rp.getBelow()) == Blocks.FARMLAND) {
            return -50.0f;
        }
        if (dist > 10.0) {
            return -((float)dist);
        }
        return MillCommonUtilities.randomInt(10);
    }

    public ItemEntity getClosestItemVertical(List<InvItem> goods, int radius, int vertical) {
        return WorldUtilities.getClosestItemVertical(this.level, this.getPos(), goods, radius, vertical);
    }

    public ResourceLocation getClothTexturePath(int layer) {
        return this.clothTexture[layer];
    }

    public VillagerConfig getConfig() {
        if (this.vtype == null || this.vtype.villagerConfig == null) {
            return VillagerConfig.DEFAULT_CONFIG;
        }
        return this.vtype.villagerConfig;
    }

    public Culture getCulture() {
        if (this.vtype == null) {
            return null;
        }
        return this.vtype.culture;
    }

    public ConstructionIP getCurrentConstruction() {
        ConstructionIP cip;
        if (this.constructionJobId > -1 && this.constructionJobId < this.getTownHall().getConstructionsInProgress().size() && ((cip = this.getTownHall().getConstructionsInProgress().get(this.constructionJobId)).getBuilder() == null || cip.getBuilder() == this)) {
            return cip;
        }
        return null;
    }

    public Goal getCurrentGoal() {
        if (Goal.goals.containsKey(this.goalKey)) {
            return Goal.goals.get(this.goalKey);
        }
        return null;
    }

    protected int getExperienceReward(Player par1EntityPlayer) {
        return this.vtype.expgiven;
    }

    public String getFemaleChild() {
        return this.vtype.femaleChild;
    }

    public String getGameOccupationName(String playername) {
        if (this.getCulture() == null || this.vtype == null || this.getRecord() == null) {
            return "";
        }
        if (!this.getCulture().canReadVillagerNames()) {
            return "";
        }
        if (this.isBaby() && this.getSize() == 20) {
            return this.getCulture().getCultureString("villager." + this.vtype.altkey);
        }
        return this.getCulture().getCultureString("villager." + this.vtype.key);
    }

    public String getGameSpeech(String playername) {
        if (this.getCulture() == null) {
            return null;
        }
        String speech = VillageUtilities.getVillagerSentence(this, playername, false);
        if (speech != null) {
            int duration = 10 + speech.length() / 5;
            if (this.speech_started + (long)(20 * (duration = Math.min(duration, 30))) < this.level.getDayTime()) {
                return null;
            }
        }
        return speech;
    }

    public int getGatheringRange() {
        return 20;
    }

    public String getGenderString() {
        if (this.gender == 1) {
            return "male";
        }
        return "female";
    }

    public Building getGoalBuildingDest() {
        return this.mw.getBuilding(this.getGoalBuildingDestPoint());
    }

    public Point getGoalBuildingDestPoint() {
        if (this.goalInformation == null) {
            return null;
        }
        return this.goalInformation.getDestBuildingPos();
    }

    public Entity getGoalDestEntity() {
        if (this.goalInformation == null) {
            return null;
        }
        return this.goalInformation.getTargetEnt();
    }

    public Point getGoalDestPoint() {
        if (this.goalInformation == null) {
            return null;
        }
        return this.goalInformation.getDest();
    }

    public String getGoalLabel(String goal) {
        if (Goal.goals.containsKey(goal)) {
            return Goal.goals.get(goal).gameName(this);
        }
        return "none";
    }

    public List<Goal> getGoals() {
        if (this.vtype != null) {
            return this.vtype.goals;
        }
        return null;
    }

    public List<InvItem> getGoodsToBringBackHome() {
        return this.vtype.bringBackHomeGoods;
    }

    public List<InvItem> getGoodsToCollect() {
        return this.vtype.collectGoods;
    }

    public int getHireCost(Player player) {
        int cost = this.vtype.hireCost;
        if (this.getTownHall().controlledBy(player)) {
            cost /= 2;
        }
        return cost;
    }

    public Building getHouse() {
        if (this.house != null) {
            return this.house;
        }
        if (MillConfigValues.LogVillager >= 3 && this.extraLog) {
            MillLog.debug(this, "Seeking uncached house");
        }
        if (this.mw != null) {
            this.house = this.mw.getBuilding(this.housePoint);
            return this.house;
        }
        return null;
    }

    public Set<InvItem> getInventoryKeys() {
        return this.inventory.keySet();
    }

    public List<InvItem> getItemsNeeded() {
        return this.vtype.itemsNeeded;
    }

    public ItemStack getItemBySlot(EquipmentSlot slotIn) {
        if (slotIn == EquipmentSlot.HEAD) {
            for (InvItem item : this.getConfig().armoursHelmetSorted) {
                if (this.countInv(item) <= 0) continue;
                return item.getItemStack();
            }
            return ItemStack.EMPTY;
        }
        if (slotIn == EquipmentSlot.CHEST) {
            for (InvItem item : this.getConfig().armoursChestplateSorted) {
                if (this.countInv(item) <= 0) continue;
                return item.getItemStack();
            }
            return ItemStack.EMPTY;
        }
        if (slotIn == EquipmentSlot.LEGS) {
            for (InvItem item : this.getConfig().armoursLeggingsSorted) {
                if (this.countInv(item) <= 0) continue;
                return item.getItemStack();
            }
            return ItemStack.EMPTY;
        }
        if (slotIn == EquipmentSlot.FEET) {
            for (InvItem item : this.getConfig().armoursBootsSorted) {
                if (this.countInv(item) <= 0) continue;
                return item.getItemStack();
            }
            return ItemStack.EMPTY;
        }
        if (this.heldItem != null && slotIn == EquipmentSlot.MAINHAND) {
            return this.heldItem;
        }
        if (this.heldItemOffHand != null && slotIn == EquipmentSlot.OFFHAND) {
            return this.heldItemOffHand;
        }
        return ItemStack.EMPTY;
    }

    public String getMaleChild() {
        return this.vtype.maleChild;
    }

    public String func_70005_c_() {
        return this.firstName + " " + this.familyName;
    }

    @Override
    public net.minecraft.network.chat.Component getName() {
        return new net.minecraft.network.chat.TextComponent(this.func_70005_c_());
    }

    public String getNameKey() {
        if (this.vtype == null || this.getRecord() == null) {
            return "";
        }
        if (this.isBaby() && this.getSize() == 20) {
            return this.vtype.altkey;
        }
        return this.vtype.key;
    }

    public String getNativeOccupationName() {
        if (this.vtype == null) {
            return null;
        }
        if (this.isBaby() && this.getSize() == 20) {
            return this.vtype.altname;
        }
        return this.vtype.name;
    }

    public String getNativeSpeech(String playername) {
        if (this.getCulture() == null) {
            return null;
        }
        String speech = VillageUtilities.getVillagerSentence(this, playername, true);
        if (speech != null) {
            int duration = 10 + speech.length() / 5;
            if (this.speech_started + (long)(20 * (duration = Math.min(duration, 30))) < this.level.getDayTime()) {
                return null;
            }
        }
        return speech;
    }

    public Point getPathDestPoint() {
        if (this.getActiveGoal() instanceof org.millenaire.common.goal.generic.GoalGenericCooking cooking
                && this.getGoalDestPoint() != null)
            return cooking.getWorkPosition(this);
        return this.pathDestPoint;
    }

    private AStarConfig getPathingConfig() {
        if (this.getActiveGoal() != null) {
            return this.getActiveGoal().getPathingConfig(this);
        }
        return this.getVillagerPathingConfig();
    }

    public Node getPathPointPos() {
        return new Node(Mth.floor((double)this.getBoundingBox().minX), Mth.floor((double)this.getBoundingBox().minY), Mth.floor((double)this.getBoundingBox().minZ));
    }

    public Point getPos() {
        return new Point(this.getX(), this.getY(), this.getZ());
    }

    public HumanoidArm getMainArm() {
        if (this.getRecord() != null && this.getRecord().rightHanded) {
            return HumanoidArm.RIGHT;
        }
        return HumanoidArm.LEFT;
    }

    public String getRandomFamilyName() {
        return this.getCulture().getRandomNameFromList(this.vtype.familyNameList);
    }

    public VillagerRecord getRecord() {
        if (this.mw == null) {
            return null;
        }
        return this.mw.getVillagerRecordById(this.getVillagerId());
    }

    public int getSize() {
        if (this.getRecord() == null) {
            return 0;
        }
        return this.getRecord().size;
    }

    public MillVillager getSpouse() {
        if (this.getHouse() == null || this.isBaby()) {
            return null;
        }
        for (MillVillager v : this.getHouse().getKnownVillagers()) {
            if (v.isBaby() || v.gender == this.gender) continue;
            return v;
        }
        return null;
    }

    public ResourceLocation getTexture() {
        return this.texture;
    }

    public List<String> getToolsCategoriesNeeded() {
        return this.vtype.toolsCategoriesNeeded;
    }

    public int getArmorValue() {
        if (this.getRecord() == null) {
            return 0;
        }
        return this.getRecord().getTotalArmorValue();
    }

    public Building getTownHall() {
        if (this.townHall != null) {
            return this.townHall;
        }
        if (MillConfigValues.LogVillager >= 3 && this.extraLog) {
            MillLog.debug(this, "Seeking uncached townHall");
        }
        if (this.mw != null) {
            this.townHall = this.mw.getBuilding(this.townHallPoint);
            return this.townHall;
        }
        return null;
    }

    public long getVillagerId() {
        return this.villagerId;
    }

    public AStarConfig getVillagerPathingConfig() {
        if (this.vtype.noleafclearing) {
            return JPS_CONFIG_NO_LEAVES;
        }
        return JPS_CONFIG_DEFAULT;
    }

    public ItemStack getWeapon() {
        InvItem weapon;
        if (this.vtype == null) {
            return ItemStack.EMPTY;
        }
        if (this.isUsingBow && (weapon = this.getConfig().getBestWeaponRanged(this)) != null) {
            return weapon.getItemStack();
        }
        if ((this.isUsingHandToHand || !this.vtype.isArcher) && (weapon = this.getConfig().getBestWeaponHandToHand(this)) != null) {
            return weapon.getItemStack();
        }
        if (this.vtype.startingWeapon != null) {
            return this.vtype.startingWeapon.getItemStack();
        }
        return ItemStack.EMPTY;
    }

    public void growSize() {
        if (this.getRecord() == null) {
            return;
        }
        int growth = 2;
        int nb = 0;
        nb = this.getHouse().takeGoods(Items.EGG, 1);
        if (nb == 1) {
            growth += 1 + MillCommonUtilities.randomInt(5);
        }
        for (InvItem food : this.getConfig().foodsGrowthSorted) {
            if (growth >= 10 || this.getRecord().size + growth >= 20 || this.getHouse().countGoods(food) <= 0) continue;
            this.getHouse().takeGoods(food, 1);
            growth += this.getConfig().foodsGrowth.get(food) + MillCommonUtilities.randomInt(this.getConfig().foodsGrowth.get(food));
        }
        this.getRecord().size += growth;
        if (this.getRecord().size > 20) {
            this.getRecord().size = 20;
        }
        this.computeChildScale();
        if (MillConfigValues.LogChildren >= 2) {
            MillLog.minor(this, "Child growing by " + growth + ", new size: " + this.getRecord().size);
        }
    }

    private void handleDoorsAndFenceGates() {
        if (this.level.isClientSide) return;

        // Convert the previous per-villager closing hint into a shared lease.
        if (this.doorToClose != null) {
            org.millenaire.common.pathing.VillageDoorAccess.adoptLegacy(
                this.level, this.doorToClose.getBlockPos());
            this.doorToClose = null;
        }

        // Open all door/gate nodes along the active path. A shared manager closes
        // them only after the entire passage is clear, not after one villager leaves.
        Path route = this.navigation.getPath();
        if (route != null && !route.isDone()) {
            java.util.Set<net.minecraft.core.BlockPos> candidates = new java.util.LinkedHashSet<>();
            for (int i = Math.max(0, route.getNextNodeIndex() - 1);
                    i < Math.min(route.getNodeCount(), route.getNextNodeIndex() + 3); i++) {
                var node = route.getNode(i);
                if (node == null) continue;
                var pos = node.asBlockPos();
                candidates.add(pos);
                candidates.add(pos.below());
                candidates.add(pos.above());
            }
            for (var pos : candidates) {
                double distanceSquared = this.position().distanceToSqr(Vec3.atCenterOf(pos));
                if (distanceSquared <= 8.0D) {
                    org.millenaire.common.pathing.VillageDoorAccess.openForResident(this.level, pos);
                    // Remember a gate only when physically beside its opening,
                    // not merely while inspecting an upcoming route node.
                    if (distanceSquared <= 2.25D
                            && this.level.getBlockState(pos).getBlock() instanceof FenceGateBlock) {
                        this.recentlyUsedGate = pos.immutable();
                        this.recentlyUsedGateTick = this.level.getGameTime();
                    }
                }
            }
        }

        // If the closed passage prevented a route from being created, allow nearby
        // doors in the *direction of travel* to open so a retry can find the exit.
        if (route != null && !route.isDone() || this.stopMoving || this.getPathDestPoint() == null
                || (this.level.getGameTime() + this.getVillagerId()) % 4L != 0L) return;
        Vec3 target = Vec3.atCenterOf(this.getPathDestPoint().getBlockPos());
        double dx = target.x - this.getX(), dz = target.z - this.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length <= 2.0D) return;
        dx /= length;
        dz /= length;
        var feet = this.blockPosition();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= 1; y++) {
                    var pos = feet.offset(x, y, z);
                    Vec3 middle = Vec3.atCenterOf(pos);
                    double forward = (middle.x - this.getX()) * dx + (middle.z - this.getZ()) * dz;
                    if (forward < 0.1D || forward > 3.0D
                            || this.position().distanceToSqr(middle) > 8.0D) continue;
                    org.millenaire.common.pathing.VillageDoorAccess.openForResident(this.level, pos);
                }
            }
        }
    }

    private void handleLeaveClearing() {
        if (this.pathEntity != null && this.pathEntity.getNodeCount() > 0) {
            Point p;
            ArrayList<Point> pointsToCheck = new ArrayList<Point>();
            if (this.pathEntity.getCurrentTargetPathPoint() != null) {
                p = new Point(this.pathEntity.getCurrentTargetPathPoint());
                pointsToCheck.add(p);
                pointsToCheck.add(p.getAbove());
            }
            if (this.pathEntity.getNextTargetPathPoint() != null) {
                p = new Point(this.pathEntity.getNextTargetPathPoint());
                for (int dx = -1; dx < 2; ++dx) {
                    for (int dz = -1; dz < 2; ++dz) {
                        pointsToCheck.add(p.getRelative(dx, 0.0, dz));
                        pointsToCheck.add(p.getRelative(dx, 1.0, dz));
                    }
                }
            }
            for (Point point : pointsToCheck) {
                BlockState blockState = point.getBlockActualState(this.level);
                if (!(blockState.getBlock() instanceof LeavesBlock)) continue;
                if (blockState.is(net.minecraft.tags.BlockTags.LEAVES)) {
                    if (!!blockState.getValue(LeavesBlock.PERSISTENT)) continue;
                    WorldUtilities.setBlock(this.level, point, Blocks.AIR, true, true);
                    continue;
                }
                if (blockState.getBlock() instanceof BlockFruitLeaves) continue;
                if (BlockStateUtilities.hasPropertyByName(blockState, "decayable")) {
                    if (!!blockState.getValue(LeavesBlock.PERSISTENT)) continue;
                    WorldUtilities.setBlock(this.level, point, Blocks.AIR, true, true);
                    continue;
                }
                WorldUtilities.setBlock(this.level, point, Blocks.AIR, true, true);
            }
        }
    }

    private boolean hasBow() {
        return this.getConfig().getBestWeaponRanged(this) != null;
    }

    public boolean hasChildren() {
        return this.vtype.maleChild != null && this.vtype.femaleChild != null;
    }


    public boolean helpsInAttacks() {
        return this.vtype.helpInAttacks;
    }

    public void interactDev(Player entityplayer) {
        DevModUtilities.villagerInteractDev(entityplayer, this);
    }

    public boolean interactSpecial(Player entityplayer) {
        if (this.getTownHall() == null) {
            MillLog.error(this, "Trying to interact with a villager with no TH.");
        }
        if (this.getHouse() == null) {
            MillLog.error(this, "Trying to interact with a villager with no house.");
        }
        if (this.isChief()) {
            ServerSender.displayVillageChiefGUI(entityplayer, this);
            return true;
        }
        UserProfile profile = this.mw.getProfile(entityplayer);
        if (this.canMeditate() && this.mw.isGlobalTagSet("pujas") || this.canPerformSacrifices() && this.mw.isGlobalTagSet("mayansacrifices")) {
            if (MillConfigValues.LogPujas >= 3) {
                MillLog.debug(this, "canMeditate");
            }
            if (this.getTownHall().getReputation(entityplayer) >= -1024) {
                for (BuildingLocation l : this.getTownHall().getLocations()) {
                    if (l.level < 0 || l.getSellingPos() == null || !(l.getSellingPos().distanceTo((Entity)this) < 8.0)) continue;
                    Building b = l.getBuilding(this.level);
                    if (b.pujas == null) continue;
                    if (MillConfigValues.LogPujas >= 3) {
                        MillLog.debug(this, "Found shrine: " + b);
                    }
                    Point p = b.getPos();
                    org.millenaire.common.ui.ContainerPuja.open(entityplayer, b);
                    return true;
                }
            } else {
                ServerSender.sendTranslatedSentence(entityplayer, 'f', "ui.sellerboycott", this.func_70005_c_());
                return false;
            }
        }
        if (this.isSeller() && !this.getTownHall().controlledBy(entityplayer)) {
            if (this.getTownHall().getReputation(entityplayer) >= -1024 && this.getTownHall().chestLocked) {
                for (BuildingLocation l : this.getTownHall().getLocations()) {
                    if (l.level < 0 || l.shop == null || l.shop.length() <= 0 || !(l.getSellingPos() != null && l.getSellingPos().distanceTo((Entity)this) < 5.0) && !(l.sleepingPos.distanceTo((Entity)this) < 5.0)) continue;
                    ServerSender.displayVillageTradeGUI(entityplayer, l.getBuilding(this.level));
                    return true;
                }
            } else {
                if (!this.getTownHall().chestLocked) {
                    ServerSender.sendTranslatedSentence(entityplayer, 'f', "ui.sellernotcurrently possible", this.func_70005_c_());
                    return false;
                }
                ServerSender.sendTranslatedSentence(entityplayer, 'f', "ui.sellerboycott", this.func_70005_c_());
                return false;
            }
        }
        if (this.isForeignMerchant()) {
            ServerSender.displayMerchantTradeGUI(entityplayer, this);
            return true;
        }
        if (this.vtype.hireCost > 0) {
            if (this.hiredBy == null || this.hiredBy.equals(entityplayer.getName().getString())) {
                ServerSender.displayHireGUI(entityplayer, this);
                return true;
            }
            ServerSender.sendTranslatedSentence(entityplayer, 'f', "hire.hiredbyotherplayer", this.func_70005_c_(), this.hiredBy);
            return false;
        }
        if (this.isLocalMerchant() && !profile.villagersInQuests.containsKey(this.getVillagerId())) {
            ServerSender.sendTranslatedSentence(entityplayer, '6', "other.localmerchantinteract", this.func_70005_c_());
            return false;
        }
        return false;
    }

    public boolean isChief() {
        return this.vtype.isChief;
    }

    public boolean isBaby() {
        if (this.vtype == null) {
            return false;
        }
        return this.vtype.isChild;
    }

    public boolean isForeignMerchant() {
        return this.vtype.isForeignMerchant;
    }

    public boolean isHostile() {
        return this.vtype.hostile;
    }

    public boolean isLocalMerchant() {
        return this.vtype.isLocalMerchant;
    }

    protected boolean isImmobile() {
        return this.getHealth() <= 0.0f || this.isVillagerSleeping();
    }

    public boolean isReallyDead() {
        return this.isRemoved() && this.getHealth() <= 0.0f;
    }

    public boolean isSeller() {
        return this.vtype.canSell;
    }

    public boolean isTextureValid(String texture) {
        if (this.vtype != null) {
            return this.vtype.isTextureValid(texture);
        }
        return true;
    }

    public boolean isVillagerSleeping() {
        return this.shouldLieDown;
    }

    public boolean isVisitor() {
        if (this.vtype == null) {
            return false;
        }
        return this.vtype.visitor;
    }

    private void jumpToDest() {
        Point jumpTo = WorldUtilities.findVerticalStandingPos(this.level, this.getPathDestPoint());
        if (jumpTo != null && jumpTo.distanceTo(this.getPathDestPoint()) < 4.0
            && this.level.noCollision(this, this.getBoundingBox().move(
                jumpTo.getiX() + 0.5 - this.getX(), jumpTo.getiY() - this.getY(), jumpTo.getiZ() + 0.5 - this.getZ()))) {
            if (MillConfigValues.LogPathing >= 1 && this.extraLog) {
                MillLog.major(this, "Jumping from " + this.getPos() + " to " + jumpTo);
            }
            this.setPos((double)jumpTo.getiX() + 0.5, (double)jumpTo.getiY(), (double)jumpTo.getiZ() + 0.5);
            this.longDistanceStuck = 0;
            this.localStuck = 0;
        } else if (this.goalKey != null && Goal.goals.containsKey(this.goalKey)) {
            Goal goal = Goal.goals.get(this.goalKey);
            try {
                goal.unreachableDestination(this);
            }
            catch (Exception e) {
                MillLog.printException(this + ": Exception in handling unreachable dest for goal " + this.goalKey, e);
            }
        }
    }

    public void killVillager() {
        VillagerRecord vr;
        Player owner;
        if (this.level.isClientSide || !(this.level instanceof ServerLevel)) {
            super.remove(Entity.RemovalReason.DISCARDED);
            return;
        }
        for (InvItem iv : this.inventory.keySet()) {
            if (this.inventory.get(iv) <= 0) continue;
            WorldUtilities.spawnItem(this.level, this.getPos(), org.millenaire.common.item.LegacyItemStacks.stack(iv.getItem(), this.inventory.get(iv).intValue(), iv.meta), 0.0f);
        }
        if (this.hiredBy != null && (owner = this.findPlayerByName(this.hiredBy)) != null) {
            ServerSender.sendTranslatedSentence(owner, 'f', "hire.hiredied", this.func_70005_c_());
        }
        if ((vr = this.getRecord()) != null) {
            if (MillConfigValues.LogGeneralAI >= 1) {
                MillLog.major(this, this.getTownHall() + ": Villager has been killed!");
            }
            vr.killed = true;
        }
        super.remove(Entity.RemovalReason.DISCARDED);
    }

    private void leaveVillage() {
        for (InvItem iv : this.vtype.foreignMerchantStock.keySet()) {
            this.getHouse().takeGoods(iv.getItem(), iv.meta, (int)this.vtype.foreignMerchantStock.get(iv));
        }
        this.mw.removeVillagerRecord(this.villagerId);
        this.despawnVillager();
    }

    public void localMerchantUpdate() throws Exception {
        if (this.getHouse() != null && this.getHouse() == this.getTownHall()) {
            List<Building> buildings = this.getTownHall().getBuildingsWithTag("inn");
            Building inn = null;
            for (Building building : buildings) {
                if (building.merchantRecord != null) continue;
                inn = building;
            }
            if (inn == null) {
                this.mw.removeVillagerRecord(this.villagerId);
                this.despawnVillager();
                MillLog.error(this, "Merchant had Town Hall as house and inn is full. Killing him.");
            } else {
                this.setHousePoint(inn.getPos());
                VillagerRecord vr = this.getRecord();
                vr.updateRecord(this);
                this.mw.registerVillagerRecord(vr, true);
                MillLog.error(this, "Merchant had Town Hall as house. Moving him to the inn.");
            }
        }
    }

    public void die(DamageSource cause) {
        super.die(cause);
    }

    @Override
    public void onFoundPath(List<AStarNode> result) {
        this.pathCalculatedSinceLastTick = result;
    }

    @Override
    public void push(Entity other) {
        // Vanilla skips separation when both horizontal centres are within 0.01 blocks.
        // Shared work destinations can put two villagers exactly at that position.
        if (other instanceof MillVillager && !this.level.isClientSide
                && !this.noPhysics && !other.noPhysics
                && this.isAlive() && other.isAlive()
                && !this.isPassenger() && !other.isPassenger()
                && !this.isVehicle() && !other.isVehicle()
                && !this.isVillagerSleeping() && !((MillVillager) other).isVillagerSleeping()
                && this.getBoundingBox().intersects(other.getBoundingBox())
                && Math.max(Math.abs(this.getX() - other.getX()),
                            Math.abs(this.getZ() - other.getZ())) < 0.01D) {
            // One impulse per pair; normal movement still resolves walls and doors.
            if (this.getId() < other.getId()) {
                this.push(-0.025D, 0.0D, 0.0D);
                other.push(0.025D, 0.0D, 0.0D);
            }
            return;
        }
        super.push(other);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Route permission alone does not make a mob swim towards the surface.
        this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
    }

    public void aiStep() {
        super.aiStep();
        this.updateSwingTime();
        this.setFacingDirection();
        if (this.isVillagerSleeping()) {
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    public void onNoPathAvailable() {
        this.pathFailedSincelastTick = true;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public void tick() {
        long startTime = System.nanoTime();
        if (!this.level.dimension().equals(Level.OVERWORLD)) {
            this.despawnVillagerSilent();
        }
        try {
            if (this.vtype == null) {
                if (!this.isRemoved()) {
                    MillLog.error(this, "Unknown villager type. Killing him.");
                    this.despawnVillagerSilent();
                }
                return;
            }
            if (this.pathFailedSincelastTick) {
                this.pathFailedSinceLastTick();
            }
            if (this.pathCalculatedSinceLastTick != null) {
                this.applyPathCalculatedSinceLastTick();
            }
            if (this.level.isClientSide) {
                super.tick();
                return;
            }
            if (this.isRemoved()) {
                super.tick();
                return;
            }
            if (Math.abs(this.level.getGameTime() + (long)this.hashCode()) % 10L == 2L) {
                this.sendVillagerPacket();
            }
            if (Math.abs(this.level.getGameTime() + (long)this.hashCode()) % 40L == 4L) {
                this.unlockForNearbyPlayers();
            }
            if (this.hiredBy != null) {
                this.updateHired();
                super.tick();
                return;
            }
            if (this.getTownHall() == null || this.getHouse() == null) {
                return;
            }
            if (this.getTownHall() != null && !this.getTownHall().isActive) {
                return;
            }
            if (this.getPos().distanceTo(this.getTownHall().getPos()) > (double)(this.getTownHall().villageType.radius + 100)) {
                MillLog.error(this, "Villager is far away from village. Despawning him.");
                this.despawnVillagerSilent();
            }
            try {
                block82: {
                    block81: {
                        Node nextPoint;
                        double newdistance;
                        block80: {
                            block79: {
                                ++this.timer;
                                if (this.getHealth() < this.getMaxHealth() & MillCommonUtilities.randomInt(1600) == 0) {
                                    this.setHealth(this.getHealth() + 1.0f);
                                }
                                this.detrampleCrops();
                                this.allowRandomMoves = true;
                                this.stopMoving = false;
                                if (this.getTownHall() == null || this.getHouse() == null) {
                                    super.tick();
                                    return;
                                }
                                if (Goal.beSeller.key.equals(this.goalKey)) {
                                    this.townHall.seller = this;
                                } else if (Goal.getResourcesForBuild.key.equals(this.goalKey) || Goal.construction.key.equals(this.goalKey)) {
                                    if (MillConfigValues.LogTileEntityBuilding >= 3) {
                                        MillLog.debug(this, "Registering as builder for: " + this.townHall);
                                    }
                                    if (this.constructionJobId > -1 && this.townHall.getConstructionsInProgress().size() > this.constructionJobId) {
                                        this.townHall.getConstructionsInProgress().get(this.constructionJobId).setBuilder(this);
                                    }
                                }
                                if (this.getTownHall().underAttack) {
                                    if (this.goalKey == null || !this.goalKey.equals(Goal.raidVillage.key) && !this.goalKey.equals(Goal.defendVillage.key) && !this.goalKey.equals(Goal.hide.key)) {
                                        this.clearGoal();
                                    }
                                    if (this.isRaider) {
                                        this.goalKey = Goal.raidVillage.key;
                                        this.targetDefender();
                                    } else if (this.helpsInAttacks()) {
                                        this.goalKey = Goal.defendVillage.key;
                                        this.targetRaider();
                                    } else {
                                        this.goalKey = Goal.hide.key;
                                    }
                                    this.checkGoals();
                                }
                                if (this.getTarget() == null) break block79;
                                if (this.vtype.isDefensive && this.getPos().distanceTo(this.getHouse().getResManager().getDefendingPos()) > 20.0) {
                                    this.setTarget(null);
                                } else if (!this.getTarget().isAlive() || this.getPos().distanceTo((Entity)this.getTarget()) > 80.0 || this.level.getDifficulty() == Difficulty.PEACEFUL && this.getTarget() instanceof Player) {
                                    this.setTarget(null);
                                }
                                if (this.getTarget() != null) {
                                    this.shouldLieDown = false;
                                    this.attackEntity((Entity)this.getTarget());
                                    if (!this.getTarget().hasImpulse) {
                                        this.setPathDestPoint(new Point((Entity)this.getTarget()), 1);
                                        break block80;
                                    } else {
                                        Point posToAttack = new Point((Entity)this.getTarget());
                                        while (posToAttack.y > 0.0 && posToAttack.isBlockPassable(this.level)) {
                                            posToAttack = posToAttack.getBelow();
                                        }
                                        if (posToAttack != null) {
                                            this.setPathDestPoint(posToAttack.getAbove(), 3);
                                        }
                                    }
                                }
                                break block80;
                            }
                            if (this.isHostile() && this.level.getDifficulty() != Difficulty.PEACEFUL && this.getTownHall().closestPlayer != null && this.getPos().distanceTo((Entity)this.getTownHall().closestPlayer) <= 80.0) {
                                int range = 80;
                                if (this.vtype.isDefensive) {
                                    range = 20;
                                }
                                this.setTarget((LivingEntity)this.level.getNearestPlayer(this.getX(), this.getY(), this.getZ(), (double)range, true));
                                this.clearGoal();
                            }
                        }
                        if (this.getTarget() != null) {
                            this.setGoalDestPoint(new Point((Entity)this.getTarget()));
                            this.heldItem = this.getWeapon();
                            this.heldItemOffHand = ItemStack.EMPTY;
                            if (this.goalKey != null && !Goal.goals.get(this.goalKey).isFightingGoal()) {
                                this.clearGoal();
                            }
                        } else if (!this.getTownHall().underAttack) {
                            if (this.level.isDay()) {
                                ItemEntity item;
                                this.speakSentence("greeting", 12000, 3, 10);
                                this.nightActionPerformed = false;
                                List<InvItem> goods = this.getGoodsToCollect();
                                if (goods != null && (this.level.getGameTime() + this.getVillagerId()) % 20L == 0L && (item = this.getClosestItemVertical(goods, 5, 30)) != null) {
                                    this.pickUpGoods(item);
                                }
                                this.specificUpdate();
                                if (!this.isRaider) {
                                    if (this.goalKey == null) {
                                        this.setNextGoal();
                                    }
                                    if (this.goalKey != null) {
                                        this.checkGoals();
                                    } else {
                                        this.shouldLieDown = false;
                                    }
                                }
                            } else if (!this.isRaider) {
                                if (this.goalKey == null) {
                                    this.setNextGoal();
                                }
                                if (this.goalKey != null) {
                                    this.checkGoals();
                                } else {
                                    this.shouldLieDown = false;
                                }
                            }
                        }
                        if (this.getPathDestPoint() == null || this.pathEntity == null || this.pathEntity.getNodeCount() <= 0 || this.stopMoving) break block81;
                        // Physical movement watchdog handles stalls. Distance-to-target heuristics
                        // incorrectly cancel detours that first lead away from the destination.
                        this.longDistanceStuck = 0;
                        this.localStuck = 0;
                        break block82;
                    }
                    this.longDistanceStuck = 0;
                    this.localStuck = 0;
                }
                if (this.getPathDestPoint() != null && !this.stopMoving) {
                    this.recoverStationaryMovement();
                    this.updatePathIfNeeded(this.getPathDestPoint());
                } else {
                    this.movementWatchdog.reset();
                }
                // Keep following a usable route while its replacement is calculated.
                if (this.stopMoving) {
                    this.navigation.stop();
                    this.pathEntity = null;
                }
                this.prevPoint = this.getPos();
                if (this.canVillagerClearLeaves() && Math.abs(this.level.getGameTime() + (long)this.hashCode()) % 10L == 6L) {
                    this.handleLeaveClearing();
                }
                this.handleDoorsAndFenceGates();
                if (System.currentTimeMillis() - this.timeSinceLastPathingTimeDisplay > 10000L) {
                    if (this.pathingTime > 500L) {
                        if (this.getPathDestPoint() != null) {
                            MillLog.warning(this, "Pathing time in last 10 secs: " + this.pathingTime + " dest: " + this.getPathDestPoint() + " dest bid: " + WorldUtilities.getBlock(this.level, this.getPathDestPoint()) + " above bid: " + WorldUtilities.getBlock(this.level, this.getPathDestPoint().getAbove()));
                        } else {
                            MillLog.warning(this, "Pathing time in last 10 secs: " + this.pathingTime + " null dest point.");
                        }
                        MillLog.warning(this, "nbPathsCalculated: " + this.nbPathsCalculated + " nbPathNoStart: " + this.nbPathNoStart + " nbPathNoEnd: " + this.nbPathNoEnd + " nbPathAborted: " + this.nbPathAborted + " nbPathFailure: " + this.nbPathFailure);
                        if (this.goalKey != null) {
                            MillLog.warning(this, "Current goal: " + Goal.goals.get(this.goalKey));
                        }
                    }
                    this.timeSinceLastPathingTimeDisplay = System.currentTimeMillis();
                    this.pathingTime = 0L;
                    this.nbPathsCalculated = 0;
                    this.nbPathNoStart = 0;
                    this.nbPathNoEnd = 0;
                    this.nbPathAborted = 0;
                    this.nbPathFailure = 0;
                }
            }
            catch (MillLog.MillenaireException e) {
                Mill.proxy.sendChatAdmin(this.func_70005_c_() + ": Error in onUpdate(). Check millenaire.log.");
                MillLog.error(this, e.getMessage());
            }
            catch (Exception e) {
                Mill.proxy.sendChatAdmin(this.func_70005_c_() + ": Error in onUpdate(). Check millenaire.log.");
                MillLog.error(this, "Exception in Villager.onUpdate(): ");
                MillLog.printException(e);
            }
            if (Math.abs(this.level.getGameTime() + (long)this.hashCode()) % 10L == 5L) {
                this.triggerMobAttacks();
            }
            this.updateDialogue();
            this.isUsingBow = false;
            this.isUsingHandToHand = false;
            super.tick();
            if (MillConfigValues.DEV) {
                if (this.getPathDestPoint() == null || this.pathPlannerJPS.isBusy() || this.pathEntity == null) {
                    // empty if block
                }
                if (this.getPathDestPoint() != null && this.getGoalDestPoint() != null && !(this.getPathDestPoint().distanceTo(this.getGoalDestPoint()) > 20.0)) {
                    // empty if block
                }
            }
        }
        catch (Exception e) {
            MillLog.printException("Exception in onUpdate() of villager: " + this, e);
        }
        if (this.getTownHall() != null) {
            this.mw.reportTime(this.getTownHall(), System.nanoTime() - startTime, true);
        }
    }

    private boolean openFenceGate(int i, int j, int k) {
        Point p = new Point(i, j, k);
        BlockState state = p.getBlockActualState(this.level);
        if (BlockItemUtilities.isFenceGate(state.getBlock()) && !((Boolean)state.getValue(FenceGateBlock.OPEN)).booleanValue()) {
            p.setBlockState(this.level, state.setValue(FenceGateBlock.OPEN, Boolean.valueOf(true)));
        }
        return true;
    }

    private void recoverStationaryMovement() {
        Goal goal = Goal.goals.get(this.goalKey);
        Point dest = this.getPathDestPoint();
        // Working, sleeping and fighting are not failed travel.
        if (this.getTarget() != null || this.isVillagerSleeping()
                || (goal != null && goal.isWithinActionRange(this, dest))) {
            this.movementWatchdog.reset();
            return;
        }
        var action = this.movementWatchdog.sample(this.level.getGameTime(),
                dest.getBlockPos(), this.getX(), this.getY(), this.getZ());
        if (action == org.millenaire.common.pathing.MovementWatchdog.Action.NONE) return;
        this.navigation.stop();
        this.pathEntity = null;
        this.nextVanillaPathAttempt = 0L;
        // Even a formally "reachable" path can keep walking into the outside
        // wall if the side gate never entered the chosen route. A physical stall
        // is a separate reason to try the gate, before the teleport fallback.
        if (isStockTransferGoal(goal) && dest != null) {
            Path entrance = this.tryStockGateApproach(dest, Math.max(1, goal.range(this)));
            if (entrance != null && entrance.getNodeCount() > 1) {
                Node[] nodes = new Node[entrance.getNodeCount()];
                for (int i = 0; i < nodes.length; i++) nodes[i] = entrance.getNode(i);
                AS_PathEntity adapted = new AS_PathEntity(
                        nodes, entrance.getTarget(), entrance.canReach());
                if (this.navigation.moveTo(adapted, 0.5)) {
                    this.pathEntity = adapted;
                    this.nextVanillaPathAttempt = this.level.getGameTime() + 40L;
                    this.pathFailureRecovery.reset();
                    this.movementWatchdog.reset();
                    MillLog.major(this, "NAV-GATE rescued stalled route: goal="
                            + this.goalKey + " stock=" + dest);
                    return;
                }
            }
        }
        if (action == org.millenaire.common.pathing.MovementWatchdog.Action.RECOVER
                && this.vtype != null && (!this.vtype.noTeleport
                    || this.getRecord() != null && this.getRecord().raidingVillage)) {
            if (isStockTransferGoal(goal)) {
                MillLog.major(this, "NAV-TELEPORT stock recovery: at=" + this.blockPosition()
                        + " target=" + dest + " goal=" + this.goalKey);
            }
            this.jumpToDest();
        }
    }

    private static boolean isStockTransferGoal(Goal goal) {
        return goal instanceof org.millenaire.common.goal.GoalGetResourcesForShops
                || goal instanceof org.millenaire.common.goal.GoalGetGoodsForHousehold
                || goal instanceof org.millenaire.common.goal.GoalDeliverResourcesShop
                || goal instanceof org.millenaire.common.goal.GoalDeliverGoodsHousehold
                || goal instanceof org.millenaire.common.goal.GoalBringBackResourcesHome;
    }

    private boolean canSeeStockFromNode(int x, int y, int z, Point dest) {
        if (dest == null) return false;
        net.minecraft.core.BlockPos stand = new net.minecraft.core.BlockPos(x, y, z);
        Vec3 eye = Vec3.atBottomCenterOf(stand).add(0, 0.8D, 0);
        return org.millenaire.common.goal.WorkAccess.visible(
            this.level, eye, Vec3.atCenterOf(dest.getBlockPos()), dest.getBlockPos());
    }

    /**
     * When the stock is behind an enclosure, the nearest access node may lie
     * against an outside wall. Try the actual fence gate as an intermediate
     * waypoint instead of repeatedly choosing that inaccessible node.
     *
     * Server-only, bounded scan of loaded blocks near the stock destination.
     * Rate-limited to once per six seconds per villager to protect server TPS.
     */
    private Path tryStockGateApproach(Point dest, int range) {
        long now = this.level.getGameTime();
        if (now < this.nextStockGateProbeTick) return null;
        this.nextStockGateProbeTick = now + 120L;
        if (dest == null || this.level.isClientSide) return null;

        java.util.List<BlockPos> gates = new ArrayList<>();
        BlockPos target = dest.getBlockPos();
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                if (dx * dx + dz * dz > 100) continue;
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos pos = target.offset(dx, dy, dz);
                    if (!this.level.hasChunkAt(pos)
                            || this.position().distanceToSqr(Vec3.atCenterOf(pos)) > 400.0D) continue;
                    BlockState state = this.level.getBlockState(pos);
                    if (!(state.getBlock() instanceof FenceGateBlock)
                            || state.getValue(FenceGateBlock.POWERED)) continue;
                    gates.add(pos.immutable());
                }
            }
        }
        // A gate can be several blocks to the side of the direct line to
        // the chest; rank by its proximity to BOTH the villager and the stock.
        gates.sort(Comparator.comparingDouble(pos ->
            this.position().distanceTo(Vec3.atCenterOf(pos))
            + Vec3.atCenterOf(pos).distanceTo(Vec3.atCenterOf(target))));

        for (int i = 0; i < Math.min(3, gates.size()); i++) {
            BlockPos gate = gates.get(i);
            double gateDistanceSquared = this.position().distanceToSqr(Vec3.atCenterOf(gate));
            // Do not swing open a livestock gate just to plan a route from
            // twenty blocks away. The walk evaluator already allows a closed
            // gate as a door node; keep it closed until a resident approaches.
            boolean nearGate = gateDistanceSquared <= 12.25D;
            if (nearGate) {
                org.millenaire.common.pathing.VillageDoorAccess.openForResident(this.level, gate);
            }

            // First verify that the gate itself is genuinely reachable. It
            // must not become another inaccessible "nearest" route endpoint.
            Path approach = this.navigation.createPath(gate, 0);
            if (approach == null || !approach.canReach()
                    || approach.getNodeCount() <= 0) continue;
            Node endGate = approach.getEndNode();
            if (endGate == null || endGate.asBlockPos().distManhattan(gate) > 1) continue;

            // If opening the gate made a complete path possible, follow that.
            Path through = this.navigation.createPath(target, Math.min(2, range - 1));
            if (through != null && through.canReach() && through.getNodeCount() > 0) {
                Node end = through.getEndNode();
                if (new Point(end).horizontalDistanceTo(dest) < range
                        && canSeeStockFromNode(end.x, end.y, end.z, dest)) {
                    boolean passesGate = false;
                    for (int index = 0; index < through.getNodeCount(); index++) {
                        Node step = through.getNode(index);
                        if (Math.abs(step.x - gate.getX()) <= 1
                                && Math.abs(step.y - gate.getY()) <= 1
                                && Math.abs(step.z - gate.getZ()) <= 1) {
                            passesGate = true;
                            break;
                        }
                    }
                    if (passesGate) {
                        MillLog.major(this, "NAV-GATE complete path: goal=" + this.goalKey
                                + " gate=" + gate + " stock=" + target);
                        return through;
                    }
                }
            }

            // A bounded first leg gets the resident to the side entrance.
            // Once this leg finishes, the normal stock route is recalculated.
            MillLog.major(this, "NAV-GATE waypoint: goal=" + this.goalKey
                    + " gate=" + gate + " stock=" + target);
            return approach;
        }
        if (!gates.isEmpty()) {
            MillLog.major(this, "NAV-GATE ENTRY rejected: at=" + this.blockPosition()
                    + " stock=" + target + " goal=" + this.goalKey
                    + " detectedGates=" + gates.size());
        }
        return null;
    }

    /**
     * Route out of a courtyard through a nearby fence gate, even if the
     * villager was teleported INTO the enclosure and never walked past it.
     * Search only locally, no chunk loads, and at most once every 160 ticks.
     */
    private boolean tryRecentGateExit(Point dest) {
        if (dest == null || this.level.isClientSide) return false;
        long now = this.level.getGameTime();
        if (now < this.nextGateExitAttemptTick) return false;
        if (this.getPos().horizontalDistanceTo(dest) < 5.0D) return false;
        this.nextGateExitAttemptTick = now + 160L;

        java.util.List<BlockPos> candidates = new ArrayList<>();
        BlockPos feet = this.blockPosition();
        if (this.recentlyUsedGate != null
                && this.level.hasChunkAt(this.recentlyUsedGate)
                && now >= this.recentlyUsedGateTick
                && now - this.recentlyUsedGateTick < 1800L) {
            candidates.add(this.recentlyUsedGate);
        }

        // Bounded fallback for teleport-in: the previous fix looked ONLY at
        // recently used gates, which is empty after a teleport to a stock chest.
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                if (dx * dx + dz * dz > 64) continue;
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos pos = feet.offset(dx, dy, dz);
                    if (!this.level.hasChunkAt(pos)) continue;
                    BlockState state = this.level.getBlockState(pos);
                    if (!(state.getBlock() instanceof FenceGateBlock)
                            || state.getValue(FenceGateBlock.POWERED)
                            || candidates.contains(pos)) continue;
                    candidates.add(pos.immutable());
                }
            }
        }
        // Prefer a gate physically crossed earlier, otherwise use the closest
        // candidate to avoid introducing unnecessary detours across a village.
        BlockPos remembered = this.recentlyUsedGate;
        candidates.sort(Comparator.comparingDouble(pos ->
                (remembered != null && remembered.equals(pos) ? -1000.0D : 0.0D)
                        + this.position().distanceTo(Vec3.atCenterOf(pos))));

        int eligible = 0;
        int pathRejected = 0;
        int checked = 0;
        for (BlockPos gate : candidates) {
            if (checked >= 3) break;
            BlockState state = this.level.getBlockState(gate);
            if (!(state.getBlock() instanceof FenceGateBlock)) continue;
            Vec3 centre = Vec3.atCenterOf(gate);
            double near = this.position().distanceToSqr(centre);
            if (near > 81.0D || near < 1.0D) continue;

            net.minecraft.core.Direction.Axis axis =
                    state.getValue(FenceGateBlock.FACING).getAxis();
            double residentSide = axis == net.minecraft.core.Direction.Axis.X
                    ? this.getX() - centre.x : this.getZ() - centre.z;
            double targetSide = axis == net.minecraft.core.Direction.Axis.X
                    ? dest.x - centre.x : dest.z - centre.z;
            // We only need this gate if it separates the resident and target.
            if (Math.abs(residentSide) < 0.75D
                    || residentSide * targetSide >= -3.0D) continue;
            eligible++;
            checked++;

            net.minecraft.core.Direction exitDirection =
                    axis == net.minecraft.core.Direction.Axis.X
                    ? (targetSide > 0 ? net.minecraft.core.Direction.EAST
                            : net.minecraft.core.Direction.WEST)
                    : (targetSide > 0 ? net.minecraft.core.Direction.SOUTH
                            : net.minecraft.core.Direction.NORTH);
            BlockPos exit = gate.relative(exitDirection, 2);
            if (!this.level.hasChunkAt(exit)) continue;

            // Keep the gate CLOSED while computing the route so livestock stay
            // inside. The route follower will open it only near the passage.
            Path through = this.navigation.createPath(exit, 0);
            if (through == null || !through.canReach()
                    || through.getNodeCount() <= 1 || through.getEndNode() == null
                    || through.getEndNode().asBlockPos().distManhattan(exit) > 1) {
                pathRejected++;
                continue;
            }

            // Reaching a waypoint NEAR a gate via the wall is not equivalent
            // to actually walking through the opening.
            boolean crossesOpening = false;
            for (int i = 0; i < through.getNodeCount(); i++) {
                Node step = through.getNode(i);
                if (step.x == gate.getX() && step.z == gate.getZ()
                        && step.y == gate.getY()) {
                    crossesOpening = true;
                    break;
                }
            }
            if (!crossesOpening) {
                pathRejected++;
                continue;
            }

            Node[] nodes = new Node[through.getNodeCount()];
            for (int i = 0; i < nodes.length; i++) nodes[i] = through.getNode(i);
            // This only reaches the outside waypoint, NOT the actual goal.
            // Mark it partial so checkGoals cannot complete an unrelated
            // task simply because the NPC arrived near the gate.
            AS_PathEntity route = new AS_PathEntity(
                    nodes, through.getTarget(), false);
            if (!this.navigation.moveTo(route, 0.5)) {
                pathRejected++;
                continue;
            }
            this.pathEntity = route;
            this.pathFailureRecovery.reset();
            this.recentlyUsedGate = gate.immutable();
            this.recentlyUsedGateTick = now;
            MillLog.major(this, "NAV-GATE EXIT chosen gate=" + gate
                    + " exit=" + exit + " finalTarget=" + dest
                    + " goal=" + this.goalKey + " nodes=" + through.getNodeCount());
            return true;
        }
        if (!candidates.isEmpty()) {
            MillLog.major(this, "NAV-GATE EXIT rejected: at=" + this.blockPosition()
                    + " target=" + dest + " goal=" + this.goalKey
                    + " detectedGates=" + candidates.size()
                    + " eligibleGates=" + eligible + " pathRejected=" + pathRejected);
        }
        return false;
    }

    private boolean tryGroundRoute(Point dest) {
        long now = this.level.getGameTime();
        if (now < this.nextVanillaPathAttempt) return false;
        this.nextVanillaPathAttempt = now + 40L;
        this.navigation.stop();
        // This must run before the direct search: a route over stone stairs
        // can technically be valid while ignoring an accessible courtyard gate.
        if (this.tryRecentGateExit(dest)) return true;
        Goal goal = Goal.goals.get(this.goalKey);
        int range = goal == null ? 1 : Math.max(1, goal.range(this));
        boolean stockGoal = isStockTransferGoal(goal);
        Path fallback = this.navigation.createPath(dest.getBlockPos(), Math.min(2, range - 1));
        boolean reached = fallback != null && fallback.canReach() && fallback.getNodeCount() > 0
                && new Point(fallback.getEndNode()).horizontalDistanceTo(dest) < range;
        if (reached && stockGoal) {
            Node end = fallback.getEndNode();
            reached = canSeeStockFromNode(end.x, end.y, end.z, dest);
        }

        // A partial route that stops at the outside face of a wall is not a route
        // to the stock. Search all visible, physically valid approach positions
        // in one multi-target search, rather than picking the first candidate.
        if (stockGoal && !reached) {
            try {
                java.util.Set<net.minecraft.core.BlockPos> access =
                        new java.util.LinkedHashSet<>();
                AStarNode[] candidates = AStarStatic.getAccessNodesSorted(
                        this.level, this.blockPosition().getX(), this.blockPosition().getY(),
                        this.blockPosition().getZ(), dest.getiX(), dest.getiY(),
                        dest.getiZ(), this.getPathingConfig());
                for (AStarNode candidate : candidates) {
                    if (Math.abs(candidate.y - dest.getiY()) > 1
                            || Math.abs(candidate.x - dest.getiX()) > 2
                            || Math.abs(candidate.z - dest.getiZ()) > 2
                            || !canSeeStockFromNode(candidate.x, candidate.y, candidate.z, dest)) {
                        continue;
                    }
                    access.add(new net.minecraft.core.BlockPos(
                            candidate.x, candidate.y, candidate.z));
                    if (access.size() >= 32) break;
                }
                if (!access.isEmpty()) {
                    Path alternatives = this.navigation.createPath(access, 0);
                    if (alternatives != null && alternatives.canReach()
                            && alternatives.getNodeCount() > 0) {
                        Node end = alternatives.getEndNode();
                        if (new Point(end).horizontalDistanceTo(dest) < range
                                && canSeeStockFromNode(end.x, end.y, end.z, dest)) {
                            fallback = alternatives;
                            reached = true;
                        }
                    }
                }
            } catch (ThreadSafeUtilities.ChunkAccessException exception) {
                // The normal pathfinder can still retry as chunks become available.
            }
        }

        // If the stock is behind a wall, search for an actual nearby gate,
        // including side entrances outside the direct villager-to-stock line.
        if (stockGoal && !reached) {
            Path gateAccess = this.tryStockGateApproach(dest, range);
            if (gateAccess != null) {
                fallback = gateAccess;
                reached = true; // This may be a waypoint, not permission to transfer goods.
            }
        }

        boolean progressing = !reached
                && org.millenaire.common.pathing.PartialPathRecovery.canUse(
                    fallback, this.position(), new Vec3(dest.x, dest.y, dest.z));
        if (stockGoal && fallback != null && fallback.getEndNode() != null
                && new Point(fallback.getEndNode()).horizontalDistanceTo(dest)
                    <= Math.max(6, range + 2)) {
            // Never accept a short partial approach ending immediately at a wall.
            progressing = false;
        }
        if (!reached && !progressing) return false;
        Node[] nodes = new Node[fallback.getNodeCount()];
        for (int i = 0; i < nodes.length; i++) nodes[i] = fallback.getNode(i);
        AS_PathEntity adapted = new AS_PathEntity(nodes, fallback.getTarget(), fallback.canReach());
        if (this.navigation.moveTo(adapted, 0.5)) {
            this.pathEntity = adapted;
            this.pathFailureRecovery.reset();
            return true;
        }
        return false;
    }

    private void pathFailedSinceLastTick() {
        this.pathFailedSincelastTick = false;
        Point dest = this.getPathDestPoint();
        if (dest == null || this.stopMoving) return;
        // A failed background refresh must not interrupt a still usable route.
        if (this.pathEntity != null && !this.pathEntity.isDone()
                && !this.navigation.isDone() && !this.navigation.isStuck()) return;
        long now = this.level.getGameTime();
        if (this.tryGroundRoute(dest)) return;
        if (now >= this.nextPathFailureDiagnostic) {
            this.nextPathFailureDiagnostic = now + 200L;
            MillLog.major(this, "Navigation recovery: goal=" + this.goalKey
                    + " position=" + this.getPos() + " target=" + dest
                    + " distance=" + dest.horizontalDistanceTo((Entity)this));
        }
        if (this.pathFailureRecovery.failed(now)
            && (!this.vtype.noTeleport || this.getRecord() != null && this.getRecord().raidingVillage)) {
            this.jumpToDest();
        }
    }

    public boolean performNightAction() {
        if (this.getRecord() == null || this.getHouse() == null || this.getTownHall() == null) {
            return false;
        }
        if (this.isBaby()) {
            if (this.getSize() < 20) {
                this.growSize();
            } else {
                this.teenagerNightAction();
            }
        }
        if (this.getHouse().hasVisitors) {
            this.visitorNightAction();
        }
        if (this.hasChildren()) {
            return this.attemptChildConception();
        }
        return true;
    }

    @Override
    public net.minecraft.world.InteractionResult mobInteract(Player entityplayer, InteractionHand hand) {
        if (this.isVillagerSleeping()) {
            return net.minecraft.world.InteractionResult.sidedSuccess(this.level.isClientSide);
        }
        if (this.level.isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
        MillAdvancements.FIRST_CONTACT.grant(entityplayer);
        if (this.vtype != null && (this.vtype.key.equals("indian_sadhu") || this.vtype.key.equals("alchemist"))) {
            MillAdvancements.MAITRE_A_PENSER.grant(entityplayer);
        }
        UserProfile profile = this.mw.getProfile(entityplayer);
        if (profile.villagersInQuests.containsKey(this.getVillagerId())) {
            QuestInstance qi = profile.villagersInQuests.get(this.getVillagerId());
            if (qi.getCurrentVillager().id == this.getVillagerId()) {
                ServerSender.displayQuestGUI(entityplayer, this);
            } else {
                this.interactSpecial(entityplayer);
            }
        } else {
            this.interactSpecial(entityplayer);
        }
        if (MillConfigValues.DEV) {
            this.interactDev(entityplayer);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(this.level.isClientSide);
    }

    public int putInBuilding(Building building, Item item, int meta, int nb) {
        nb = this.takeFromInv(item, meta, nb);
        building.storeGoods(item, meta, nb);
        return nb;
    }

    public void readAdditionalSaveData(CompoundTag nbttagcompound) {
        int i;
        super.readAdditionalSaveData(nbttagcompound);
        String type = nbttagcompound.getString("vtype");
        String culture = nbttagcompound.getString("culture");
        if (Culture.getCultureByName(culture) != null) {
            if (Culture.getCultureByName(culture).getVillagerType(type) != null) {
                this.vtype = Culture.getCultureByName(culture).getVillagerType(type);
            } else {
                MillLog.error(this, "Could not load dynamic NPC: unknown type: " + type + " in culture: " + culture);
            }
        } else {
            MillLog.error(this, "Could not load dynamic NPC: unknown culture: " + culture);
        }
        this.texture = new ResourceLocation("millenaire", nbttagcompound.getString("texture"));
        this.housePoint = Point.read(nbttagcompound, "housePos");
        if (this.housePoint == null) {
            MillLog.error(this, "Error when loading villager: housePoint null");
            Mill.proxy.sendChatAdmin(this.func_70005_c_() + ": Could not load house position. Check millenaire.log");
        }
        this.townHallPoint = Point.read(nbttagcompound, "townHallPos");
        if (this.townHallPoint == null) {
            MillLog.error(this, "Error when loading villager: townHallPoint null");
            Mill.proxy.sendChatAdmin(this.func_70005_c_() + ": Could not load town hall position. Check millenaire.log");
        }
        this.setGoalDestPoint(Point.read(nbttagcompound, "destPoint"));
        this.setPathDestPoint(Point.read(nbttagcompound, "pathDestPoint"), 0);
        this.setGoalBuildingDestPoint(Point.read(nbttagcompound, "destBuildingPoint"));
        this.prevPoint = Point.read(nbttagcompound, "prevPoint");
        this.doorToClose = Point.read(nbttagcompound, "doorToClose");
        this.action = nbttagcompound.getInt("action");
        this.goalKey = nbttagcompound.getString("goal");
        if (this.goalKey.trim().length() == 0) {
            this.goalKey = null;
        }
        if (this.goalKey != null && !Goal.goals.containsKey(this.goalKey)) {
            this.goalKey = null;
        }
        this.constructionJobId = nbttagcompound.getInt("constructionJobId");
        this.dialogueKey = nbttagcompound.getString("dialogueKey");
        this.dialogueStart = nbttagcompound.getLong("dialogueStart");
        this.dialogueRole = nbttagcompound.getInt("dialogueRole");
        this.dialogueColour = (char)nbttagcompound.getInt("dialogueColour");
        this.dialogueChat = nbttagcompound.getBoolean("dialogueChat");
        if (this.dialogueKey.trim().length() == 0) {
            this.dialogueKey = null;
        }
        this.familyName = nbttagcompound.getString("familyName");
        this.firstName = nbttagcompound.getString("firstName");
        this.gender = nbttagcompound.getInt("gender");
        if (nbttagcompound.contains("villager_lid")) {
            this.setVillagerId(Math.abs(nbttagcompound.getLong("villager_lid")));
        }
        if (!this.isTextureValid(this.texture.getPath())) {
            ResourceLocation newTexture = this.vtype.getNewTexture();
            MillLog.major(this, "Texture " + this.texture.getPath() + " cannot be found, replacing it with " + newTexture.getPath());
            this.texture = newTexture;
        }
        ListTag nbttaglist = nbttagcompound.getList("inventoryNew", 10);
        MillCommonUtilities.readInventory(nbttaglist, this.inventory);
        if (nbttagcompound.contains("previousCropState", 10)) {
            this.previousCropState = net.minecraft.nbt.NbtUtils.readBlockState(nbttagcompound.getCompound("previousCropState"));
        } else if (nbttagcompound.getInt("previousBlock") == 59) {
            // Vanilla's old wheat id is stable; old mod block ids require the original world's registry map.
            this.previousCropState = Blocks.WHEAT.defaultBlockState().setValue(
                    net.minecraft.world.level.block.CropBlock.AGE, Mth.clamp(nbttagcompound.getInt("previousBlockMeta"), 0, 7));
        } else {
            this.previousCropState = Blocks.AIR.defaultBlockState();
        }
        this.hiredBy = nbttagcompound.getString("hiredBy");
        this.hiredUntil = nbttagcompound.getLong("hiredUntil");
        this.aggressiveStance = nbttagcompound.getBoolean("aggressiveStance");
        this.isRaider = nbttagcompound.getBoolean("isRaider");
        this.visitorNbNights = nbttagcompound.getInt("visitorNbNights");
        if (this.hiredBy.equals("")) {
            this.hiredBy = null;
        }
        if (nbttagcompound.contains("clothTexture")) {
            this.clothTexture[0] = new ResourceLocation("millenaire", nbttagcompound.getString("clothTexture"));
        } else {
            for (i = 0; i < 2; ++i) {
                if (nbttagcompound.getString("clothTexture_" + i).length() > 0) {
                    String texture = nbttagcompound.getString("clothTexture_" + i);
                    if (texture.contains(":")) {
                        this.clothTexture[i] = new ResourceLocation(texture);
                        continue;
                    }
                    this.clothTexture[i] = new ResourceLocation("millenaire", texture);
                    continue;
                }
                this.clothTexture[i] = null;
            }
        }
        this.clothName = nbttagcompound.getString("clothName");
        if (this.clothName.equals("")) {
            this.clothName = null;
            for (i = 0; i < 2; ++i) {
                this.clothTexture[i] = null;
            }
        }
        this.updateClothTexturePath();
    }

    public void load(CompoundTag compound) {
        super.load(compound);
    }

    public void readSpawnData(FriendlyByteBuf ds) {
        FriendlyByteBuf data = new FriendlyByteBuf(ds);
        try {
            this.setVillagerId(data.readLong());
            this.readVillagerStreamdata(data);
        }
        catch (IOException e) {
            MillLog.printException("Error in readSpawnData for villager " + this, e);
        }
    }

    private void readVillagerStreamdata(FriendlyByteBuf data) throws IOException {
        Entity ent;
        int goalDestEntityID;
        Culture culture = Culture.getCultureByName(StreamReadWrite.readNullableString(data));
        String vt = StreamReadWrite.readNullableString(data);
        if (culture != null) {
            this.vtype = culture.getVillagerType(vt);
        }
        this.texture = StreamReadWrite.readNullableResourceLocation(data);
        this.goalKey = StreamReadWrite.readNullableString(data);
        this.constructionJobId = data.readInt();
        this.housePoint = StreamReadWrite.readNullablePoint(data);
        this.townHallPoint = StreamReadWrite.readNullablePoint(data);
        this.firstName = StreamReadWrite.readNullableString(data);
        this.familyName = StreamReadWrite.readNullableString(data);
        this.gender = data.readInt();
        this.hiredBy = StreamReadWrite.readNullableString(data);
        this.aggressiveStance = data.readBoolean();
        this.hiredUntil = data.readLong();
        this.isUsingBow = data.readBoolean();
        this.isUsingHandToHand = data.readBoolean();
        this.isRaider = data.readBoolean();
        this.speech_key = StreamReadWrite.readNullableString(data);
        this.speech_variant = data.readInt();
        this.speech_started = data.readLong();
        this.heldItem = StreamReadWrite.readNullableItemStack(data);
        this.heldItemOffHand = StreamReadWrite.readNullableItemStack(data);
        this.inventory = StreamReadWrite.readInventory(data);
        this.clothName = StreamReadWrite.readNullableString(data);
        for (int i = 0; i < 2; ++i) {
            this.clothTexture[i] = StreamReadWrite.readNullableResourceLocation(data);
        }
        this.setGoalDestPoint(StreamReadWrite.readNullablePoint(data));
        this.shouldLieDown = data.readBoolean();
        this.dialogueTargetFirstName = StreamReadWrite.readNullableString(data);
        this.dialogueTargetLastName = StreamReadWrite.readNullableString(data);
        this.dialogueColour = data.readChar();
        this.dialogueChat = data.readBoolean();
        this.setHealth(data.readFloat());
        this.visitorNbNights = data.readInt();
        UUID uuid = StreamReadWrite.readNullableUUID(data);
        if (uuid != null) {
            Entity targetEntity = WorldUtilities.getEntityByUUID(this.level, uuid);
            if (targetEntity != null && targetEntity instanceof LivingEntity) {
                this.setTarget((LivingEntity)targetEntity);
            } else {
                this.setTarget(null);
            }
        } else {
            this.setTarget(null);
        }
        int nbMerchantSells = data.readInt();
        if (nbMerchantSells > -1) {
            this.merchantSells.clear();
            for (int i = 0; i < nbMerchantSells; ++i) {
                try {
                    TradeGood g = StreamReadWrite.readNullableGoods(data, culture);
                    this.merchantSells.put(g, data.readInt());
                    continue;
                }
                catch (MillLog.MillenaireException e) {
                    MillLog.printException(e);
                }
            }
        }
        if ((goalDestEntityID = data.readInt()) != -1 && (ent = this.level.getEntity(goalDestEntityID)) != null) {
            this.setGoalDestEntity(ent);
        }
        this.isDeadOnServer = data.readBoolean();
        this.client_lastupdated = this.level.getDayTime();
    }

    public void registerNewPath(AS_PathEntity path) throws Exception {
        if (path == null) {
            boolean handled = false;
            if (this.goalKey != null) {
                Goal goal = Goal.goals.get(this.goalKey);
                handled = goal.unreachableDestination(this);
            }
            if (!handled) {
                this.clearGoal();
            }
        } else {
            this.pathFailureRecovery.reset();
            try {
                this.navigation.moveTo((Path)path, 0.5);
            }
            catch (Exception e) {
                MillLog.major(null, "Goal : " + this.goalKey);
                MillLog.major(null, "Path to : " + this.pathDestPoint.x + "/" + this.pathDestPoint.y + "/" + this.pathDestPoint.z);
                MillLog.printException(this + ": Pathing error detected", e);
            }
            this.pathEntity = path;
            this.xxa = 0.0f;
        }
    }

    public void registerNewPath(List<Node> result) throws Exception {
        AS_PathEntity path = null;
        if (result != null) {
            Node[] pointsCopy = new Node[result.size()];
            int i = 0;
            for (Node p : result) {
                Node p2;
                pointsCopy[i] = p == null ? null : (p2 = new Node(p.x, p.y, p.z));
                ++i;
            }
            path = new AS_PathEntity(pointsCopy);
        }
        this.registerNewPath(path);
    }

    public HashMap<InvItem, Integer> requiresGoods() {
        if (this.isBaby() && this.getSize() < 20) {
            return this.vtype.requiredFoodAndGoods;
        }
        if (this.hasChildren() && this.getHouse() != null && this.getHouse().getKnownVillagers().size() < 4) {
            return this.vtype.requiredFoodAndGoods;
        }
        return this.vtype.requiredGoods;
    }

    private void sendVillagerPacket() {
        FriendlyByteBuf data = ServerSender.getPacketBuffer();
        try {
            data.writeInt(3);
            this.writeVillagerStreamData((ByteBuf)data, false);
        }
        catch (IOException e) {
            MillLog.printException(this + ": Error in sendVillagerPacket", e);
        }
        ServerSender.sendPacketToPlayersInRange(data, this.level, this.getPos(), 100);
    }

    public boolean setBlock(Point p, Block block) {
        return WorldUtilities.setBlock(this.level, p, block, true, true);
    }

    public boolean setBlockAndMetadata(Point p, Block block, int metadata) {
        return WorldUtilities.setBlockAndMetadata(this.level, p, block, metadata, true, true);
    }

    public boolean setBlockMetadata(Point p, int metadata) {
        return WorldUtilities.setBlockMetadata(this.level, p, metadata);
    }

    public boolean setBlockstate(Point p, BlockState bs) {
        return WorldUtilities.setBlockstate(this.level, p, bs, true, true);
    }

    public void remove() {
        remove(Entity.RemovalReason.DISCARDED);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (this.pathPlannerJPS != null) this.pathPlannerJPS.stopPathSearch(true);
        this.pathCalculatedSinceLastTick = null;
        if (!isRemoved() && reason.shouldDestroy() && this.getHealth() <= 0.0F) {
            this.killVillager();
        }
        super.remove(reason);
    }

    private void setFacingDirection() {
        if (this.getTarget() != null) {
            this.faceEntityMill((Entity)this.getTarget(), 30.0f, 30.0f);
            return;
        }
        if (this.goalKey != null && (this.getGoalDestPoint() != null || this.getGoalDestEntity() != null)) {
            Player player;
            Goal goal = Goal.goals.get(this.goalKey);
            if (goal.lookAtGoal()) {
                if (this.getGoalDestEntity() != null && this.getPos().distanceTo(this.getGoalDestEntity()) < (double)goal.range(this)) {
                    this.faceEntityMill(this.getGoalDestEntity(), 10.0f, 10.0f);
                } else if (this.getGoalDestPoint() != null && this.getPos().distanceTo(this.getGoalDestPoint()) < (double)goal.range(this)) {
                    this.facePoint(this.getGoalDestPoint(), 10.0f, 10.0f);
                }
            }
            if (goal.lookAtPlayer() && (player = this.level.getNearestPlayer(this, 10.0)) != null) {
                this.faceEntityMill((Entity)player, 10.0f, 10.0f);
                return;
            }
        }
    }

    public void setGoalBuildingDestPoint(Point newDest) {
        if (this.goalInformation == null) {
            this.goalInformation = new Goal.GoalInformation(null, null, null);
        }
        this.goalInformation.setDestBuildingPos(newDest);
    }

    public void setGoalDestEntity(Entity ent) {
        if (this.goalInformation == null) {
            this.goalInformation = new Goal.GoalInformation(null, null, null);
        }
        this.goalInformation.setTargetEnt(ent);
        if (ent != null) {
            this.setPathDestPoint(new Point(ent), 2);
        }
        if (ent instanceof MillVillager) {
            MillVillager v = (MillVillager)ent;
            this.dialogueTargetFirstName = v.firstName;
            this.dialogueTargetLastName = v.familyName;
        }
    }

    public void setGoalDestPoint(Point newDest) {
        if (this.goalInformation == null) {
            this.goalInformation = new Goal.GoalInformation(null, null, null);
        }
        this.goalInformation.setDest(newDest);
        this.setPathDestPoint(newDest, 0);
    }

    public void setGoalInformation(Goal.GoalInformation info) {
        this.goalInformation = info;
        if (info != null) {
            if (info.getTargetEnt() != null) {
                this.setPathDestPoint(new Point(info.getTargetEnt()), 2);
            } else if (info.getDest() != null) {
                this.setPathDestPoint(info.getDest(), 0);
            } else {
                this.setPathDestPoint(null, 0);
            }
        } else {
            this.setPathDestPoint(null, 0);
        }
    }

    public void setHousePoint(Point p) {
        this.housePoint = p;
        this.house = null;
    }

    public void setInv(Item item, int meta, int nb) {
        this.inventory.put(InvItem.createInvItem(item, meta), nb);
        this.updateVillagerRecord();
    }

    public void setNextGoal() throws Exception {
        ConstructionIP cip;
        Goal nextGoal = null;
        this.clearGoal();
        for (Goal goal : this.getGoals()) {
            if (!goal.isPossible(this)) continue;
            if (MillConfigValues.LogGeneralAI >= 2 && this.extraLog) {
                MillLog.minor(this, "Priority for goal " + goal.gameName(this) + ": " + goal.priority(this));
            }
            if (nextGoal == null || nextGoal.leasure && !goal.leasure) {
                nextGoal = goal;
                continue;
            }
            if (nextGoal != null && nextGoal.priority(this) >= goal.priority(this)) continue;
            nextGoal = goal;
        }
        if (MillConfigValues.LogGeneralAI >= 2 && this.extraLog) {
            MillLog.minor(this, "Selected this: " + nextGoal);
        }
        if (nextGoal != null) {
            this.speakSentence(nextGoal.key + ".chosen");
            this.goalKey = nextGoal.key;
            this.heldItem = ItemStack.EMPTY;
            this.heldItemOffHand = ItemStack.EMPTY;
            this.heldItemCount = Integer.MAX_VALUE;
            nextGoal.onAccept(this);
            this.goalStarted = this.level.getGameTime();
            this.lastGoalTime.put(nextGoal, this.level.getGameTime());
            AttributeInstance iattributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
            iattributeinstance.removeModifier(SPRINT_SPEED_BOOST);
            if (nextGoal.sprint) {
                iattributeinstance.addTransientModifier(SPRINT_SPEED_BOOST);
            }
        } else {
            this.goalKey = null;
        }
        if (MillConfigValues.LogBuildingPlan >= 1 && nextGoal != null && nextGoal.key.equals(Goal.getResourcesForBuild.key) && (cip = this.getCurrentConstruction()) != null) {
            MillLog.major(this, this.func_70005_c_() + " is new builder, for: " + cip.getBuildingLocation().planKey + "_" + cip.getBuildingLocation().level + ". Blocks loaded: " + cip.getBblocks().length);
        }
    }

    public void setPathDestPoint(Point newDest, int tolerance) {
        if (!(newDest != null && newDest.equals(this.pathDestPoint) || this.pathDestPoint != null && newDest != null && !((double)tolerance < newDest.distanceTo(this.pathDestPoint)))) {
            this.pathFailureRecovery.reset();
            this.navigation.stop();
            this.pathEntity = null;
        }
        this.pathDestPoint = newDest;
    }

    public void setTexture(ResourceLocation tx) {
        this.texture = tx;
    }

    public void setTownHallPoint(Point p) {
        this.townHallPoint = p;
        this.townHall = null;
    }

    public void setVillagerId(long villagerId) {
        this.villagerId = villagerId;
    }

    public void speakSentence(String key) {
        this.speakSentence(key, 600, 3, 1);
    }

    public void speakSentence(String key, int delay, int distance, int chanceOn) {
        if ((long)delay > this.level.getDayTime() - this.speech_started) {
            return;
        }
        if (!MillCommonUtilities.chanceOn(chanceOn)) {
            return;
        }
        if (this.getTownHall() == null || this.getTownHall().closestPlayer == null || this.getPos().distanceTo((Entity)this.getTownHall().closestPlayer) > (double)distance) {
            return;
        }
        key = key.toLowerCase();
        this.speech_key = null;
        if (this.getCulture().hasSentences(this.getNameKey() + "." + key)) {
            this.speech_key = this.getNameKey() + "." + key;
        } else if (this.getCulture().hasSentences(this.getGenderString() + "." + key)) {
            this.speech_key = this.getGenderString() + "." + key;
        } else if (this.getCulture().hasSentences("villager." + key)) {
            this.speech_key = "villager." + key;
        }
        if (this.speech_key != null) {
            this.speech_variant = MillCommonUtilities.randomInt(this.getCulture().getSentences(this.speech_key).size());
            this.speech_started = this.level.getDayTime();
            this.sendVillagerPacket();
            ServerSender.sendVillageSentenceInRange(this.level, this.getPos(), 30, this);
        }
    }

    public void specificUpdate() throws Exception {
        if (this.isLocalMerchant()) {
            this.localMerchantUpdate();
        }
        if (this.isForeignMerchant()) {
            this.foreignMerchantUpdate();
        }
    }

    public int takeFromBuilding(Building building, Item item, int meta, int nb) {
        if (nb <= 0) return 0;
        if (item == Blocks.OAK_LOG.asItem() && meta == -1) {
            int total = 0;
            for (org.millenaire.common.block.WoodVariant wood : org.millenaire.common.block.WoodVariant.values()) {
                if (total == nb) break;
                Item log = wood.log().asItem();
                int taken = building.takeGoods(log, 0, nb - total);
                if (taken > 0) this.addToInv(log, 0, taken);
                total += taken;
            }
            return total;
        }
        nb = building.takeGoods(item, meta, nb);
        this.addToInv(item, meta, nb);
        return nb;
    }

    public int takeFromInv(Block block, int meta, int nb) {
        return this.takeFromInv(Item.byBlock((Block)block), meta, nb);
    }

    public int takeFromInv(BlockState blockState, int nb) {
        return this.takeFromInv(InvItem.createInvItem(blockState), nb);
    }

    public int takeFromInv(InvItem item, int nb) {
        return this.takeFromInv(item.getItem(), item.meta, nb);
    }

    public int takeFromInv(Item item, int meta, int nb) {
        if (nb <= 0) return 0;
        if (item == Blocks.OAK_LOG.asItem() && meta == -1) {
            int total = 0;
            for (org.millenaire.common.block.WoodVariant wood : org.millenaire.common.block.WoodVariant.values()) {
                if (total == nb) break;
                total += this.takeFromInv(wood.log().asItem(), 0, nb - total);
            }
            return total;
        }
        InvItem key = InvItem.createInvItem(item, meta);
        if (this.inventory.containsKey(key)) {
            nb = Math.min(nb, this.inventory.get(key));
            this.inventory.put(key, this.inventory.get(key) - nb);
            this.updateVillagerRecord();
            this.updateClothTexturePath();
            return nb;
        }
        return 0;
    }

    private void targetDefender() {
        int bestDist = Integer.MAX_VALUE;
        MillVillager target = null;
        for (MillVillager v : this.getTownHall().getKnownVillagers()) {
            if (!v.helpsInAttacks() || v.isRaider || !(this.getPos().distanceToSquared((Entity)v) < (double)bestDist)) continue;
            target = v;
            bestDist = (int)this.getPos().distanceToSquared((Entity)v);
        }
        if (target != null && this.getPos().distanceToSquared((Entity)target) <= 100.0) {
            this.setTarget((LivingEntity)target);
        }
    }

    private void targetRaider() {
        int bestDist = Integer.MAX_VALUE;
        MillVillager target = null;
        for (MillVillager v : this.getTownHall().getKnownVillagers()) {
            if (!v.isRaider || !(this.getPos().distanceToSquared((Entity)v) < (double)bestDist)) continue;
            target = v;
            bestDist = (int)this.getPos().distanceToSquared((Entity)v);
        }
        if (target != null && this.getPos().distanceToSquared((Entity)target) <= 25.0) {
            this.setTarget((LivingEntity)target);
        }
    }

    private void teenagerNightAction() {
        for (Point p : this.getTownHall().getKnownVillages()) {
            Building distantVillage;
            if (this.getTownHall().getRelationWithVillage(p) <= 90 || (distantVillage = this.mw.getBuilding(p)) == null || distantVillage.culture != this.getCulture() || distantVillage == this.getTownHall()) continue;
            boolean canMoveIn = false;
            if (MillConfigValues.LogChildren >= 1) {
                MillLog.major(this, "Attempting to move to village: " + distantVillage.getVillageQualifiedName());
            }
            Building distantInn = null;
            for (Building distantBuilding : distantVillage.getBuildings()) {
                if (!canMoveIn && distantBuilding != null && distantBuilding.isHouse()) {
                    if (!distantBuilding.canChildMoveIn(this.gender, this.familyName)) continue;
                    canMoveIn = true;
                    continue;
                }
                if (distantInn != null || !distantBuilding.isInn || distantBuilding.getAllVillagerRecords().size() >= 2) continue;
                distantInn = distantBuilding;
            }
            if (!canMoveIn || distantInn == null) continue;
            if (MillConfigValues.LogChildren >= 1) {
                MillLog.major(this, "Moving to village: " + distantVillage.getVillageQualifiedName());
            }
            this.getHouse().transferVillagerPermanently(this.getRecord(), distantInn);
            distantInn.visitorsList.add("panels.childarrived;" + this.func_70005_c_() + ";" + this.getTownHall().getVillageQualifiedName());
        }
    }

    public boolean tryTeleportTo(double d, double d1, double d2) {
        int k;
        int j;
        double d3 = this.getX();
        double d4 = this.getY();
        double d5 = this.getZ();
        this.setPos(d, d1, d2);
        boolean flag = false;
        int i = Mth.floor((double)this.getX());
        if (this.level.hasChunkAt(new BlockPos(i, j = Mth.floor((double)this.getY()), k = Mth.floor((double)this.getZ())))) {
            boolean flag1 = false;
            while (!flag1 && j > this.level.getMinBuildHeight()) {
                BlockState bs = WorldUtilities.getBlockState(this.level, i, j - 1, k);
                if (bs.getBlock() == Blocks.AIR || !bs.getMaterial().blocksMotion()) {
                    this.setPos(this.getX(), this.getY() - 1.0, this.getZ());
                    --j;
                    continue;
                }
                flag1 = true;
            }
            if (flag1) {
                this.setPos(this.getX(), this.getY(), this.getZ());
                if (this.level.noCollision(this, this.getBoundingBox()) && !this.level.containsAnyLiquid(this.getBoundingBox())) {
                    flag = true;
                }
            }
        }
        if (!flag) {
            this.setPos(d3, d4, d5);
            return false;
        }
        return true;
    }

    public boolean teleportToEntity(Entity entity) {
        Vec3 vec3d = new Vec3(this.getX() - entity.getX(), this.getBoundingBox().minY + (double)(this.getBbHeight() / 2.0f) - entity.getY() + (double)entity.getEyeHeight(), this.getZ() - entity.getZ());
        vec3d = vec3d.normalize();
        double d = 16.0;
        double d1 = this.getX() + (this.random.nextDouble() - 0.5) * 8.0 - vec3d.x * 16.0;
        double d2 = this.getY() + (double)(this.random.nextInt(16) - 8) - vec3d.y * 16.0;
        double d3 = this.getZ() + (this.random.nextDouble() - 0.5) * 8.0 - vec3d.z * 16.0;
        return this.tryTeleportTo(d1, d2, d3);
    }

    private void toggleDoor(Point p) {
        BlockState state = p.getBlockActualState(this.level);
        state = (Boolean)state.getValue(DoorBlock.OPEN) != false ? state.setValue(DoorBlock.OPEN, Boolean.valueOf(false)) : state.setValue(DoorBlock.OPEN, Boolean.valueOf(true));
        p.setBlockState(this.level, state);
    }

    public String toString() {
        if (this.vtype != null) {
            return this.func_70005_c_() + "/" + this.vtype.key + "/" + this.getVillagerId() + "/" + this.getPos();
        }
        return this.func_70005_c_() + "/none/" + this.getVillagerId() + "/" + this.getPos();
    }

    private void triggerMobAttacks() {
        List<Entity> entities = WorldUtilities.getEntitiesWithinAABB(this.level, Monster.class, this.getPos(), 16, 5);
        for (Entity ent : entities) {
            Monster mob = (Monster)ent;
            if (mob.getTarget() != null || !mob.hasLineOfSight(this)) continue;
            mob.setTarget((LivingEntity)this);
        }
    }

    private void unlockForNearbyPlayers() {
        UserProfile profile;
        Player player = this.level.getNearestPlayer(this.getX(), this.getY(), this.getZ(), 5.0, false);
        if (player != null && (profile = this.mw.getProfile(player)) != null) {
            profile.unlockVillager(this.getCulture(), this.vtype);
        }
    }

    private void updateClothTexturePath() {
        block8: {
            block7: {
                if (this.vtype == null) {
                    return;
                }
                boolean[] naturalLayers = this.vtype.getClothLayersOfType(NATURAL);
                String bestClothName = null;
                int clothLevel = -1;
                if (this.vtype.hasClothTexture(FREE_CLOTHES)) {
                    bestClothName = FREE_CLOTHES;
                    clothLevel = 0;
                }
                for (InvItem iv : this.inventory.keySet()) {
                    ItemClothes clothes;
                    if (!(iv.item instanceof ItemClothes) || this.inventory.get(iv) <= 0 || (clothes = (ItemClothes)iv.item).getClothPriority(iv.meta) <= clothLevel || !this.vtype.hasClothTexture(clothes.getClothName(iv.meta))) continue;
                    bestClothName = clothes.getClothName(iv.meta);
                    clothLevel = clothes.getClothPriority(iv.meta);
                }
                if (bestClothName == null) break block7;
                if (bestClothName.equals(this.clothName)) break block8;
                this.clothName = bestClothName;
                for (int layer = 0; layer < 2; ++layer) {
                    String texture = naturalLayers[layer] ? this.vtype.getRandomClothTexture(NATURAL, layer) : this.vtype.getRandomClothTexture(bestClothName, layer);
                    if (texture != null && texture.length() > 0) {
                        if (texture.contains(":")) {
                            this.clothTexture[layer] = new ResourceLocation(texture);
                            continue;
                        }
                        this.clothTexture[layer] = new ResourceLocation("millenaire", texture);
                        continue;
                    }
                    this.clothTexture[layer] = null;
                }
                break block8;
            }
            this.clothName = null;
            for (int i = 0; i < 2; ++i) {
                this.clothTexture[i] = null;
            }
        }
    }

    private void updateDialogue() {
        if (this.dialogueKey == null) {
            return;
        }
        CultureLanguage.Dialogue d = this.getCulture().getDialogue(this.dialogueKey);
        if (d == null) {
            this.dialogueKey = null;
            return;
        }
        long timePassed = this.level.getDayTime() - this.dialogueStart;
        if ((long)(d.timeDelays.get(d.timeDelays.size() - 1) + 100) < timePassed) {
            this.dialogueKey = null;
            return;
        }
        String toSpeakKey = null;
        for (int i = 0; i < d.speechBy.size(); ++i) {
            if (this.dialogueRole != d.speechBy.get(i) || timePassed < (long)d.timeDelays.get(i).intValue()) continue;
            toSpeakKey = "chat_" + d.key + "_" + i;
        }
        if (!(toSpeakKey == null || this.speech_key != null && this.speech_key.contains(toSpeakKey))) {
            this.speakSentence(toSpeakKey, 0, 10, 1);
        }
    }

    private void updateHired() {
        try {
            if (this.getHealth() < this.getMaxHealth() & MillCommonUtilities.randomInt(1600) == 0) {
                this.setHealth(this.getHealth() + 1.0f);
            }
            Player entityplayer = this.findPlayerByName(this.hiredBy);
            if (this.level.getDayTime() > this.hiredUntil) {
                if (entityplayer != null) {
                    ServerSender.sendTranslatedSentence(entityplayer, 'f', "hire.hireover", this.func_70005_c_());
                }
                this.hiredBy = null;
                this.hiredUntil = 0L;
                VillagerRecord vr = this.getRecord();
                if (vr != null) {
                    vr.awayhired = false;
                }
                return;
            }
            if (this.getTarget() != null) {
                if (this.getPos().distanceTo((Entity)this.getTarget()) > 80.0 || this.level.getDifficulty() == Difficulty.PEACEFUL || this.getTarget().isRemoved()) {
                    this.setTarget(null);
                }
            } else if (this.isHostile() && this.level.getDifficulty() != Difficulty.PEACEFUL && this.getTownHall().closestPlayer != null && this.getPos().distanceTo((Entity)this.getTownHall().closestPlayer) <= 80.0) {
                this.setTarget((LivingEntity)this.level.getNearestPlayer(this.getX(), this.getY(), this.getZ(), 100.0, true));
            }
            if (this.getTarget() == null) {
                List list = this.level.getEntitiesOfClass(PathfinderMob.class, new AABB(this.getX(), this.getY(), this.getZ(), this.getX() + 1.0, this.getY() + 1.0, this.getZ() + 1.0).expandTowards(16.0, 8.0, 16.0));
                for (Object o : list) {
                    PathfinderMob creature;
                    if (this.getTarget() != null || (creature = (PathfinderMob)o).getTarget() != entityplayer || creature instanceof Creeper) continue;
                    this.setTarget((LivingEntity)creature);
                }
                if (this.getTarget() == null && this.aggressiveStance) {
                    list = this.level.getEntitiesOfClass(Monster.class, new AABB(this.getX(), this.getY(), this.getZ(), this.getX() + 1.0, this.getY() + 1.0, this.getZ() + 1.0).expandTowards(16.0, 8.0, 16.0));
                    if (!list.isEmpty()) {
                        this.setTarget((LivingEntity)list.get(this.level.random.nextInt(list.size())));
                        if (this.getTarget() instanceof Creeper) {
                            this.setTarget(null);
                        }
                    }
                    if (this.getTarget() == null) {
                        list = this.level.getEntitiesOfClass(MillVillager.class, new AABB(this.getX(), this.getY(), this.getZ(), this.getX() + 1.0, this.getY() + 1.0, this.getZ() + 1.0).expandTowards(16.0, 8.0, 16.0));
                        for (Object o : list) {
                            MillVillager villager;
                            if (this.getTarget() != null || !(villager = (MillVillager)o).isHostile()) continue;
                            this.setTarget((LivingEntity)villager);
                        }
                    }
                }
            }
            LivingEntity target = null;
            if (this.getTarget() != null) {
                target = this.getTarget();
                this.heldItem = this.getWeapon();
                this.heldItemOffHand = ItemStack.EMPTY;
                Path newPathEntity = this.getNavigation().createPath(target, 0);
                if (newPathEntity != null) {
                    this.getNavigation().moveTo(newPathEntity, 0.5);
                }
                this.attackEntity((Entity)this.getTarget());
            } else {
                this.heldItem = ItemStack.EMPTY;
                this.heldItemOffHand = ItemStack.EMPTY;
                target = entityplayer;
                int dist = (int)this.getPos().distanceTo((Entity)target);
                if (dist > 16) {
                    this.teleportToEntity((Entity)entityplayer);
                } else if (dist > 4) {
                    Path newPathEntity;
                    boolean rebuildPath = false;
                    if (this.getNavigation().getPath() == null) {
                        rebuildPath = true;
                    } else {
                        Point currentTargetPoint = new Point(this.getNavigation().getPath().getEndNode());
                        if (currentTargetPoint.distanceTo((Entity)entityplayer) > 2.0) {
                            rebuildPath = true;
                        }
                    }
                    if (rebuildPath && (newPathEntity = this.getNavigation().createPath(target, 0)) != null) {
                        this.getNavigation().moveTo(newPathEntity, 0.5);
                    }
                }
            }
            this.prevPoint = this.getPos();
            this.handleDoorsAndFenceGates();
        }
        catch (Exception e) {
            MillLog.printException("Error in hired onUpdate():", e);
        }
    }

    private void updatePathIfNeeded(Point dest) throws Exception {
        if (dest == null) {
            return;
        }
        if (this.getNavigation().isStuck()) {
            this.pathEntity = null;
        }
        if (this.pathEntity != null && this.pathEntity.getNodeCount() > 0 && this.pathEntity.getCurrentTargetPathPoint() != null) {
            this.getNavigation().moveTo((Path)this.pathEntity, 0.5);
        } else if (!this.pathPlannerJPS.isBusy()) {
            this.computeNewPath(dest);
        }
    }

    public float rotlerp(float f, float f1, float f2) {
        float f3;
        for (f3 = f1 - f; f3 < -180.0f; f3 += 360.0f) {
        }
        while (f3 >= 180.0f) {
            f3 -= 360.0f;
        }
        if (f3 > f2) {
            f3 = f2;
        }
        if (f3 < -f2) {
            f3 = -f2;
        }
        return f + f3;
    }

    public void updateVillagerRecord() {
        if (!this.level.isClientSide) {
            this.getRecord().updateRecord(this);
        }
    }

    private boolean visitorNightAction() {
        ++this.visitorNbNights;
        if (this.visitorNbNights > 5) {
            this.leaveVillage();
        } else if (this.isForeignMerchant()) {
            boolean hasItems = false;
            for (InvItem key : this.vtype.foreignMerchantStock.keySet()) {
                if (this.getHouse().countGoods(key) <= 0) continue;
                hasItems = true;
            }
            if (!hasItems) {
                this.leaveVillage();
            }
        }
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbttagcompound) {
        try {
            if (this.vtype == null) {
                MillLog.error(this, "Not saving villager due to null vtype.");
                return;
            }
            super.addAdditionalSaveData(nbttagcompound);
            nbttagcompound.putString("vtype", this.vtype.key);
            nbttagcompound.putString("culture", this.getCulture().key);
            nbttagcompound.putString("texture", this.texture.getPath());
            if (this.housePoint != null) {
                this.housePoint.write(nbttagcompound, "housePos");
            }
            if (this.townHallPoint != null) {
                this.townHallPoint.write(nbttagcompound, "townHallPos");
            }
            if (this.getGoalDestPoint() != null) {
                this.getGoalDestPoint().write(nbttagcompound, "destPoint");
            }
            if (this.getGoalBuildingDestPoint() != null) {
                this.getGoalBuildingDestPoint().write(nbttagcompound, "destBuildingPoint");
            }
            if (this.getPathDestPoint() != null) {
                this.getPathDestPoint().write(nbttagcompound, "pathDestPoint");
            }
            if (this.prevPoint != null) {
                this.prevPoint.write(nbttagcompound, "prevPoint");
            }
            if (this.doorToClose != null) {
                this.doorToClose.write(nbttagcompound, "doorToClose");
            }
            nbttagcompound.putInt("action", this.action);
            if (this.goalKey != null) {
                nbttagcompound.putString("goal", this.goalKey);
            }
            nbttagcompound.putInt("constructionJobId", this.constructionJobId);
            nbttagcompound.putString("firstName", this.firstName);
            nbttagcompound.putString("familyName", this.familyName);
            nbttagcompound.putInt("gender", this.gender);
            nbttagcompound.putLong("lastSpeechLong", this.speech_started);
            nbttagcompound.putLong("villager_lid", this.getVillagerId());
            if (this.dialogueKey != null) {
                nbttagcompound.putString("dialogueKey", this.dialogueKey);
                nbttagcompound.putLong("dialogueStart", this.dialogueStart);
                nbttagcompound.putInt("dialogueRole", this.dialogueRole);
                nbttagcompound.putInt("dialogueColour", (int)this.dialogueColour);
                nbttagcompound.putBoolean("dialogueChat", this.dialogueChat);
            }
            ListTag nbttaglist = MillCommonUtilities.writeInventory(this.inventory);
            nbttagcompound.put("inventoryNew", (Tag)nbttaglist);
            nbttagcompound.put("previousCropState", net.minecraft.nbt.NbtUtils.writeBlockState(this.previousCropState));
            if (this.hiredBy != null) {
                nbttagcompound.putString("hiredBy", this.hiredBy);
                nbttagcompound.putLong("hiredUntil", this.hiredUntil);
                nbttagcompound.putBoolean("aggressiveStance", this.aggressiveStance);
            }
            nbttagcompound.putBoolean("isRaider", this.isRaider);
            nbttagcompound.putInt("visitorNbNights", this.visitorNbNights);
            if (this.clothName != null) {
                nbttagcompound.putString("clothName", this.clothName);
                for (int layer = 0; layer < 2; ++layer) {
                    if (this.clothTexture[layer] == null) continue;
                    nbttagcompound.putString("clothTexture_" + layer, this.clothTexture[layer].toString());
                }
            }
        }
        catch (Exception e) {
            MillLog.printException("Exception when attempting to save villager " + this, e);
        }
    }

    public void writeSpawnData(FriendlyByteBuf ds) {
        try {
            this.writeVillagerStreamData(ds, true);
        }
        catch (IOException e) {
            MillLog.printException("Error in writeSpawnData for villager " + this, e);
        }
    }

    private void writeVillagerStreamData(ByteBuf bb, boolean isSpawn) throws IOException {
        if (this.vtype == null) {
            MillLog.error(this, "Cannot write stream data due to null vtype.");
            return;
        }
        FriendlyByteBuf data = bb instanceof FriendlyByteBuf ? (FriendlyByteBuf)bb : new FriendlyByteBuf(bb);
        data.writeLong(this.getVillagerId());
        StreamReadWrite.writeNullableString(this.vtype.culture.key, data);
        StreamReadWrite.writeNullableString(this.vtype.key, data);
        StreamReadWrite.writeNullableResourceLocation(this.texture, data);
        StreamReadWrite.writeNullableString(this.goalKey, data);
        data.writeInt(this.constructionJobId);
        StreamReadWrite.writeNullablePoint(this.housePoint, data);
        StreamReadWrite.writeNullablePoint(this.townHallPoint, data);
        StreamReadWrite.writeNullableString(this.firstName, data);
        StreamReadWrite.writeNullableString(this.familyName, data);
        data.writeInt(this.gender);
        StreamReadWrite.writeNullableString(this.hiredBy, data);
        data.writeBoolean(this.aggressiveStance);
        data.writeLong(this.hiredUntil);
        data.writeBoolean(this.isUsingBow);
        data.writeBoolean(this.isUsingHandToHand);
        data.writeBoolean(this.isRaider);
        StreamReadWrite.writeNullableString(this.speech_key, data);
        data.writeInt(this.speech_variant);
        data.writeLong(this.speech_started);
        StreamReadWrite.writeNullableItemStack(this.heldItem, data);
        StreamReadWrite.writeNullableItemStack(this.heldItemOffHand, data);
        StreamReadWrite.writeInventory(this.inventory, data);
        StreamReadWrite.writeNullableString(this.clothName, data);
        for (int i = 0; i < 2; ++i) {
            StreamReadWrite.writeNullableResourceLocation(this.clothTexture[i], data);
        }
        StreamReadWrite.writeNullablePoint(this.getGoalDestPoint(), data);
        data.writeBoolean(this.shouldLieDown);
        StreamReadWrite.writeNullableString(this.dialogueTargetFirstName, data);
        StreamReadWrite.writeNullableString(this.dialogueTargetLastName, data);
        data.writeChar((int)this.dialogueColour);
        data.writeBoolean(this.dialogueChat);
        data.writeFloat(this.getHealth());
        data.writeInt(this.visitorNbNights);
        if (this.getTarget() != null) {
            StreamReadWrite.writeNullableUUID(this.getTarget().getUUID(), data);
        } else {
            StreamReadWrite.writeNullableUUID(null, data);
        }
        if (isSpawn) {
            this.calculateMerchantGoods();
            data.writeInt(this.merchantSells.size());
            for (TradeGood g : this.merchantSells.keySet()) {
                StreamReadWrite.writeNullableGoods(g, data);
                data.writeInt(this.merchantSells.get(g).intValue());
            }
        } else {
            data.writeInt(-1);
        }
        if (this.getGoalDestEntity() != null) {
            data.writeInt(this.getGoalDestEntity().getId());
        } else {
            data.writeInt(-1);
        }
        data.writeBoolean(this.isRemoved());
    }

    public static class InvItemAlphabeticalComparator
    implements Comparator<InvItem> {
        @Override
        public int compare(InvItem arg0, InvItem arg1) {
            return arg0.getName().compareTo(arg1.getName());
        }
    }

    public static class EntityGenericSymmFemale
    extends MillVillager {
        public EntityGenericSymmFemale(Level world) {
            this(MillVillagerEntities.FEMALE_SYM, world);
        }
        public EntityGenericSymmFemale(EntityType<? extends MillVillager> type, Level world) {
            super(type, world);
        }
    }

    public static class EntityGenericMale
    extends MillVillager {
        public EntityGenericMale(Level world) {
            this(MillVillagerEntities.MALE, world);
        }
        public EntityGenericMale(EntityType<? extends MillVillager> type, Level world) {
            super(type, world);
        }
    }

    public static class EntityGenericAsymmFemale
    extends MillVillager {
        public EntityGenericAsymmFemale(Level world) {
            this(MillVillagerEntities.FEMALE_ASYM, world);
        }
        public EntityGenericAsymmFemale(EntityType<? extends MillVillager> type, Level world) {
            super(type, world);
        }
    }
}

