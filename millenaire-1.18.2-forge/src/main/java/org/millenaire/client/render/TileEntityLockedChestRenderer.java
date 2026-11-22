package org.millenaire.client.render;
import java.util.Calendar;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.millenaire.common.block.BlockLockedChest;
import org.millenaire.common.entity.TileEntityLockedChest;

public class TileEntityLockedChestRenderer implements BlockEntityRenderer<TileEntityLockedChest> {
    private final ModelPart single,left,right;
    private final boolean christmas;
    public TileEntityLockedChestRenderer(BlockEntityRendererProvider.Context context) {
        single=context.bakeLayer(ModelLayers.CHEST);left=context.bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT);right=context.bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT);
        Calendar date=Calendar.getInstance();christmas=date.get(Calendar.MONTH)==Calendar.DECEMBER && date.get(Calendar.DAY_OF_MONTH)>=24 && date.get(Calendar.DAY_OF_MONTH)<=26;
    }
    public static ChestType chestType(Direction facing,Direction neighbor) {
        return neighbor==facing.getClockWise()?ChestType.LEFT:neighbor==facing.getCounterClockWise()?ChestType.RIGHT:ChestType.SINGLE;
    }
    @Override public void render(TileEntityLockedChest chest,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        Direction facing=chest.getBlockState().getValue(BlockLockedChest.FACING);
        ChestType type=ChestType.SINGLE;
        float openness=Mth.lerp(partial,chest.prevLidAngle,chest.lidAngle);
        if(chest.hasLevel()) {
            for(Direction direction:Direction.Plane.HORIZONTAL) {
                var pos=chest.getBlockPos().relative(direction);
                if(chest.getLevel().getBlockState(pos).getBlock()!=chest.getBlockState().getBlock())continue;
                if(chest.getLevel().getBlockEntity(pos) instanceof TileEntityLockedChest other) {
                    type=chestType(facing,direction);
                    openness=Math.max(openness,Mth.lerp(partial,other.prevLidAngle,other.lidAngle));
                    int otherLight=LevelRenderer.getLightColor(chest.getLevel(),pos);
                    light=LightTexture.pack(Math.max(LightTexture.block(light),LightTexture.block(otherLight)),Math.max(LightTexture.sky(light),LightTexture.sky(otherLight)));
                    break;
                }
            }
        }
        ModelPart model=type==ChestType.LEFT?left:type==ChestType.RIGHT?right:single;
        float closed=1-Mth.clamp(openness,0,1);
        model.getChild("lid").xRot=-(1-closed*closed*closed)*(float)Math.PI/2;
        model.getChild("lock").xRot=model.getChild("lid").xRot;
        pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(Vector3f.YP.rotationDegrees(-facing.toYRot()));pose.translate(-.5,-.5,-.5);
        model.render(pose,Sheets.chooseMaterial(chest,type,christmas).buffer(buffers,RenderType::entityCutout),light,overlay);
        pose.popPose();
    }
}
