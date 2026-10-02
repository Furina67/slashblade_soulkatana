package cn.blockforge.generated.soulkatana.entity;

import cn.blockforge.generated.soulkatana.SoulKatanaTextures;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Axis;

/** Renders the authored bone-and-roof shrine as a stationary domain anchor. */
public final class ShrineRenderer extends EntityRenderer<ShrineEntity> {

    public ShrineRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(ShrineEntity entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        // 嘴部在模型 -Z 面。与 MobRenderer.rotateCorpse 同一套约定：绕 Y 转
        // (180° - yaw) 恰好把模型 -Z 转到实体的世界朝向上。服务端生成神龛时
        // 已把 yaw 同步为界主朝向，此前渲染器从不旋转，才让嘴部永远朝北；
        // 这里按实体 yaw 转正。神龛是静止锚点、出生即固定角度，不做插值。
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
        // The authored mesh is 83 pixels tall; 1.73x makes the landmark about 9 blocks high.
        pose.scale(1.73F, 1.73F, 1.73F);
        pose.translate(0.0D, -0.02D, 0.0D);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(SoulKatanaTextures.shrine()));
        ShrineModel.render(pose, consumer, packedLight);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(ShrineEntity entity) {
        return SoulKatanaTextures.shrine();
    }
}
