package org.millenaire.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import org.millenaire.common.block.BlockFirePit;
import org.millenaire.common.entity.TileEntityFirePit;

public class TESRFirePit implements BlockEntityRenderer<TileEntityFirePit> {
    private static final float[][] COOKING = {{0.5F, 1, 0.4F, 25, 180, -45}, {0.5F, 0.9F, 0.5F, 0, 45, -45}, {0.5F, 1, 0.6F, -25, 180, -45}};
    private static final float[][] COOKED = {{0.5F, 0.9F, 0.4F, 25, 180, -45}, {0.5F, 0.9F, 0.5F, 0, -45, -45}, {0.5F, 0.9F, 0.6F, -25, 180, -45}};
    private final ItemRenderer itemRenderer;
    public TESRFirePit(BlockEntityRendererProvider.Context context) { itemRenderer = net.minecraft.client.Minecraft.getInstance().getItemRenderer(); }
    private void renderItem(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        itemRenderer.renderStatic(stack, ItemTransforms.TransformType.FIXED, light, overlay, pose, buffers, 0);
    }
    private void renderPlaced(ItemStack stack, float[] transform, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (stack.isEmpty()) return;
        pose.pushPose(); pose.translate(transform[0], transform[1], transform[2]);
        pose.mulPose(Quaternion.fromXYZDegrees(new Vector3f(transform[3], transform[4], transform[5])));
        pose.scale(0.35F, 0.35F, 0.35F); renderItem(stack, pose, buffers, light, overlay); pose.popPose();
    }
    @Override public void render(TileEntityFirePit pit, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!(pit.getBlockState().getBlock() instanceof BlockFirePit)) return;
        pose.pushPose(); pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Vector3f.YP.rotationDegrees((float)pit.getBlockState().getValue(BlockFirePit.ALIGNMENT).angle)); pose.translate(-0.5, -0.5, -0.5);
        ItemStack fuel = pit.fuel.getStackInSlot(0);
        if (!fuel.isEmpty()) {
            pose.pushPose(); pose.translate(0.5, 0.2, 0.5); pose.mulPose(Vector3f.XP.rotationDegrees(90));
            int turn = (pit.getBlockPos().getX() + pit.getBlockPos().getY() + pit.getBlockPos().getZ()) & 3;
            pose.mulPose(Vector3f.ZP.rotationDegrees(turn * 90)); pose.scale(0.5F, 0.5F, 0.5F);
            renderItem(fuel, pose, buffers, light, overlay); pose.popPose();
        }
        for (int i = 0; i < 3; i++) {
            renderPlaced(pit.inputs.getStackInSlot(i), COOKING[i], pose, buffers, light, overlay);
            renderPlaced(pit.outputs.getStackInSlot(i), COOKED[i], pose, buffers, light, overlay);
        }
        pose.popPose();
    }
}
