package org.millenaire.client.render;

import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.common.block.BlockMillBed;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.item.MillItems;
import org.millenaire.common.pathing.atomicstryker.AS_PathEntity;
import org.millenaire.common.quest.QuestInstance;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.world.UserProfile;

public class RenderMillVillager extends HumanoidMobRenderer<MillVillager, ModelMillVillager> {
    public static final EntityRendererProvider<MillVillager> FACTORY_MALE =
            context -> new RenderMillVillager(context, new ModelMillVillager());
    public static final EntityRendererProvider<MillVillager> FACTORY_FEMALE_SYM =
            context -> new RenderMillVillager(context, new ModelFemaleSymmetrical());
    public static final EntityRendererProvider<MillVillager> FACTORY_FEMALE_ASYM =
            context -> new RenderMillVillager(context, new ModelFemaleAsymmetrical());

    public RenderMillVillager(EntityRendererProvider.Context context, ModelMillVillager model) {
        super(context, model, 0.5F);
        addLayer(new HumanoidArmorLayer<>(this, new ModelMillVillager(0.5F), new ModelMillVillager(1.0F)));
        for (int layer = 0; layer < 2; layer++) addLayer(new LayerVillagerClothes(this, model, layer));
    }

    @Override
    protected boolean shouldShowName(MillVillager villager) { return false; }

    @Override
    public ResourceLocation getTextureLocation(MillVillager villager) { return villager.texture; }

    @Override
    protected void scale(MillVillager villager, PoseStack pose, float partialTicks) {
        float scale = villager.getRecord() == null ? 1.0F : villager.getRecord().scale;
        pose.scale(scale, scale, scale);
    }

    @Override
    public void render(MillVillager villager, float yaw, float partialTicks, PoseStack pose,
            MultiBufferSource buffers, int light) {
        super.render(villager, yaw, partialTicks, pose, buffers, light);
        if (!villager.isInvisible() && !Minecraft.getInstance().options.hideGui)
            doRenderVillagerName(villager, 0, 0, 0, pose, buffers, light);
    }

    private void displayText(PoseStack pose, MultiBufferSource buffers, int light,
            MillVillager villager, String text, int colour, double x, double y, double z) {
        if (text.isEmpty() || entityRenderDispatcher.distanceToSqr(villager) > 64 * 64) return;
        boolean sneaking = villager.isCrouching();
        pose.pushPose();
        try {
            pose.translate(x, y + villager.getBbHeight() + 0.5F - (sneaking ? 0.25F : 0), z);
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            pose.scale(-0.025F, -0.025F, 0.025F);
            float left = -getFont().width(text) / 2.0F;
            int shift = "deadmau5".equals(text) ? -10 : 0;
            getFont().drawInBatch(text, left, shift, colour, false, pose.last().pose(), buffers,
                    !sneaking, 0xB3000000, light);
            if (!sneaking) getFont().drawInBatch(text, left, shift, colour, false,
                    pose.last().pose(), buffers, false, 0, light);
        } finally { pose.popPose(); }
    }

    private void renderIcons(PoseStack pose, MultiBufferSource buffers, int light,
            MillVillager villager, List<ItemStack> icons, double x, double y, double z, int maxDistance) {
        if (entityRenderDispatcher.distanceToSqr(villager) > maxDistance * maxDistance) return;
        for (int i = 0; i < icons.size(); i++) {
            if (icons.get(i).isEmpty()) continue;
            pose.pushPose();
            try {
                pose.translate(x, y + villager.getBbHeight() + 0.5F - (villager.isCrouching() ? 0.25F : 0), z);
                pose.mulPose(entityRenderDispatcher.cameraOrientation());
                pose.translate((i - (icons.size() - 1) / 2.0F) * -0.5F, 0, 0);
                pose.scale(0.4F, 0.4F, 0.05F);
                Minecraft.getInstance().getItemRenderer().renderStatic(icons.get(i), ItemTransforms.TransformType.GUI,
                        light, OverlayTexture.NO_OVERLAY, pose, buffers, villager.getId());
            } finally { pose.popPose(); }
        }
    }

    @Override
    protected void setupRotations(MillVillager v, PoseStack pose, float par2, float rotationYaw, float partialTicks) {
        if (v.isAlive() && v.isVillagerSleeping()) {
            float orientation = -v.getBedOrientationInDegrees() + 90.0f;
            if (orientation == 0.0f) {
                pose.translate((float)0.5f, (float)0.0f, (float)-0.5f);
            } else if ((double)orientation == 90.0) {
                pose.translate((float)-0.5f, (float)0.0f, (float)-0.5f);
            } else if ((double)orientation == -180.0) {
                pose.translate((float)-0.5f, (float)0.0f, (float)0.5f);
            } else if ((double)orientation == -90.0) {
                pose.translate((float)0.5f, (float)0.0f, (float)0.5f);
            }
            pose.mulPose(Vector3f.YP.rotationDegrees(orientation));
            pose.mulPose(Vector3f.ZP.rotationDegrees(this.getFlipDegrees(v)));
            pose.mulPose(Vector3f.YP.rotationDegrees(270.0f));
            Block block = v.getPos().getBlock(v.level);
            if (block instanceof BlockMillBed) {
                float adjustement = 0.0f + (float)((BlockMillBed)block).getBedHeight() / 16.0f;
                pose.translate((float)0.0f, (float)0.0f, (float)(0.6f - adjustement));
            } else if (block instanceof BedBlock) {
                pose.translate((float)0.0f, (float)0.0f, (float)0.1f);
            }
        } else {
            super.setupRotations(v, pose, par2, rotationYaw, partialTicks);
        }
    }

    private List<ItemStack> defineSpecialIcons(MillVillager villager) {
        ArrayList<ItemStack> icons = new ArrayList<ItemStack>();
        if (villager.vtype != null) {
            if (villager.vtype.isChief) {
                icons.add(Items.GOLDEN_HELMET.getDefaultInstance());
            }
            if (villager.getCurrentGoal() != null && villager.getCurrentGoal().getFloatingIcon() != null) {
                icons.add(villager.getCurrentGoal().getFloatingIcon());
            }
            if (villager.isForeignMerchant()) {
                icons.add(MillItems.PURSE.getDefaultInstance());
            }
            if (villager.vtype.hireCost > 0) {
                icons.add(MillItems.DENIER.getDefaultInstance());
            }
            if (villager.isRaider) {
                icons.add(MillItems.NORMAN_AXE.getDefaultInstance());
            }
        }
        return icons;
    }

    public void doRenderVillagerName(MillVillager villager, double x, double y, double z, PoseStack pose, MultiBufferSource buffers, int light) {
        // Labels are rendered after the model pose is restored. Keep their anchor
        // at the entity instead of applying the old model-space sleeping offset.
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || Mill.clientWorld == null || villager.vtype == null) return;
        UserProfile profile = Mill.clientWorld.getProfile((Player)player);
        if (profile == null) return;
        float f4 = villager.distanceTo((Entity)player);
        if (f4 < (float)MillConfigValues.VillagersNamesDistance) {
            List<ItemStack> specialIcons;
            String s;
            String gameSpeech = villager.getGameSpeech(player.getName().getString());
            String nativeSpeech = villager.getNativeSpeech(player.getName().getString());
            float height = 0.0f;
            if (MillConfigValues.DEV && Mill.serverWorlds.size() > 0 && Mill.serverWorlds.get(0).getVillagerById(villager.getVillagerId()) != null && !MillConfigValues.DEV) {
                MillVillager dv = Mill.serverWorlds.get(0).getVillagerById(villager.getVillagerId());
                AS_PathEntity pe = dv.pathEntity;
                if (pe != null && pe.pointsCopy != null) {
                    Node[] pp = pe.pointsCopy;
                    if (pp != null && pp.length > 0) {
                        String s2 = "";
                        for (int i = pe.getNextNodeIndex(); i < pp.length && i < pe.getNextNodeIndex() + 5; ++i) {
                            s2 = s2 + "(" + pp[i] + ") ";
                        }
                        this.displayText(pose, buffers, light, villager, s2, -1593835521, (float)x, (float)y + height, (float)z);
                        height += 0.32f;
                    }
                    if (pe != null) {
                        if (pe.getNodeCount() > 0) {
                            this.displayText(pose, buffers, light, villager, "Path: " + pe.getNodeCount() + " end: " + pe.getCurrentTargetPathPoint() + " dist: " + Math.round(villager.getPos().horizontalDistanceTo(pe.getEndNode()) * 10.0) / 10L + " index: " + pe.getNextNodeIndex() + " " + dv.isPathFinding() + ", stuck: " + dv.longDistanceStuck, -1593835521, (float)x, (float)y + height, (float)z);
                        } else {
                            this.displayText(pose, buffers, light, villager, "Empty path, stuck: " + dv.longDistanceStuck, -1593835521, (float)x, (float)y + height, (float)z);
                        }
                        height += 0.32f;
                    }
                } else {
                    this.displayText(pose, buffers, light, villager, "Null path entity, stuck: " + dv.longDistanceStuck, -1593835521, (float)x, (float)y + height, (float)z);
                    height += 0.32f;
                }
                if (dv.getTarget() == null) {
                    this.displayText(pose, buffers, light, villager, "Pos: " + dv.getPos() + " Path dest: " + dv.getPathDestPoint() + " Goal dest: " + dv.getGoalDestPoint() + " dist: " + Math.round(dv.getPos().horizontalDistanceTo(dv.getPathDestPoint()) * 10.0) / 10L + " sm: " + dv.stopMoving + " jps busy: " + dv.pathPlannerJPS.isBusy(), -1593835521, (float)x, (float)y + height, (float)z);
                } else {
                    this.displayText(pose, buffers, light, villager, "Pos: " + dv.getPos() + " Entity: " + dv.getTarget() + " dest: " + new Point((Entity)dv.getTarget()) + " dist: " + Math.round(dv.getPos().horizontalDistanceTo(new Point((Entity)dv.getTarget())) * 10.0) / 10L + " sm: " + dv.stopMoving + " jps busy: " + dv.pathPlannerJPS.isBusy(), -1593835521, (float)x, (float)y + height, (float)z);
                }
                height += 0.32f;
            }
            if (villager.hiredBy == null) {
                String subLine;
                String line;
                ArrayList<String> lines;
                if (gameSpeech != null) {
                    lines = new ArrayList<String>();
                    line = gameSpeech;
                    while (line.length() > 60) {
                        int cutoff = line.lastIndexOf(32, 60);
                        if (cutoff == -1) {
                            cutoff = 60;
                        }
                        subLine = line.substring(0, cutoff);
                        line = line.substring(subLine.length()).trim();
                        lines.add(subLine);
                    }
                    lines.add(line);
                    for (int i = lines.size() - 1; i >= 0; --i) {
                        this.displayText(pose, buffers, light, villager, (String)lines.get(i), -1596166533, (float)x, (float)y + height, (float)z);
                        height += 0.32f;
                    }
                }
                if (nativeSpeech != null) {
                    lines = new ArrayList();
                    line = nativeSpeech;
                    while (line.length() > 60) {
                        int cutoff = line.lastIndexOf(32, 60);
                        if (cutoff == -1) {
                            cutoff = 60;
                        }
                        subLine = line.substring(0, cutoff);
                        line = line.substring(subLine.length()).trim();
                        lines.add(subLine);
                    }
                    lines.add(line);
                    for (int i = lines.size() - 1; i >= 0; --i) {
                        this.displayText(pose, buffers, light, villager, (String)lines.get(i), -1603244324, (float)x, (float)y + height, (float)z);
                        height += 0.32f;
                    }
                }
                if (MillConfigValues.displayNames && villager.getCurrentGoal() != null) {
                    this.displayText(pose, buffers, light, villager, villager.getCurrentGoal().gameName(villager), -1596142994, (float)x, (float)y + height, (float)z);
                    height += 0.32f;
                }
                if (villager.getTarget() != null) {
                    this.displayText(pose, buffers, light, villager, LanguageUtilities.string("other.villagerattackinglabel", villager.getTarget().getName().getString()), -1593901056, (float)x, (float)y + height, (float)z);
                    height += 0.32f;
                }
                if (profile.villagersInQuests.containsKey(villager.getVillagerId())) {
                    QuestInstance qi = profile.villagersInQuests.get(villager.getVillagerId());
                    if (qi.getCurrentVillager().id == villager.getVillagerId()) {
                        this.displayText(pose, buffers, light, villager, "[" + qi.getLabel(profile) + "]", -1596072483, (float)x, (float)y + height, (float)z);
                        height += 0.32f;
                    }
                }
                if (villager.isRaider) {
                    this.displayText(pose, buffers, light, villager, LanguageUtilities.string("ui.raider"), -1593872773, (float)x, (float)y + height, (float)z);
                    height += 0.32f;
                }
                if (villager.vtype.showHealth) {
                    this.displayText(pose, buffers, light, villager, LanguageUtilities.string("hire.health") + ": " + (double)villager.getHealth() * 0.5 + "/" + (double)villager.getMaxHealth() * 0.5, -1596072483, (float)x, (float)y + height, (float)z);
                    height += 0.32f;
                }
            } else if (villager.hiredBy.equals(profile.playerName)) {
                s = LanguageUtilities.string("hire.health") + ": " + (double)villager.getHealth() * 0.5 + "/" + (double)villager.getMaxHealth() * 0.5;
                s = villager.aggressiveStance ? s + " - " + LanguageUtilities.string("hire.aggressive") : s + " - " + LanguageUtilities.string("hire.passive");
                this.displayText(pose, buffers, light, villager, s, -1596142994, (float)x, (float)y + height, (float)z);
                s = LanguageUtilities.string("hire.timeleft", "" + Math.round((villager.hiredUntil - villager.level.getDayTime()) / 1000L));
                this.displayText(pose, buffers, light, villager, s, -1596072483, (float)x, (float)y + (height += 0.32f), (float)z);
                height += 0.32f;
            } else {
                s = LanguageUtilities.string("hire.hiredby", villager.hiredBy);
                this.displayText(pose, buffers, light, villager, s, -1596072483, (float)x, (float)y + height, (float)z);
                height += 0.32f;
            }
            if (villager.isRemoved()) {
                this.displayText(pose, buffers, light, villager, "Dead on client!", -1593901056, (float)x, (float)y + height, (float)z);
            }
            if (villager.isDeadOnServer) {
                this.displayText(pose, buffers, light, villager, "Dead on server!", -1593901056, (float)x, (float)y + height, (float)z);
            }
            if (MillConfigValues.displayNames && !villager.vtype.hideName) {
                this.displayText(pose, buffers, light, villager, villager.func_70005_c_() + ", " + villager.getNativeOccupationName(), -1593835521, (float)x, (float)y + height, (float)z);
                height += 0.32f;
            }
            if (!(specialIcons = this.defineSpecialIcons(villager)).isEmpty()) {
                this.renderIcons(pose, buffers, light, villager, specialIcons, (float)x, (float)y + (height += 0.2f), (float)z, 64);
                this.displayText(pose, buffers, light, villager, "", -1593835521, (float)x, (float)y + height, (float)z);
            }
        }
    }

}
