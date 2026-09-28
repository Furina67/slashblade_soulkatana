package cn.blockforge.generated.soulblade.entity;

import cn.blockforge.generated.soulblade.SoulBladeTextures;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 领域斩击/捌命中的贴图斩击渲染：整幅贴斩击原画，按对角线换算边长
 * （对角线 = 刀长），绕视线轴旋转 slashAngle；寿命内顺序播完样式帧动画。
 * 全部使用原版现成 RenderType（entityCutoutNoCull），光影兼容。
 * 帧贴图路径不写死在这里，统一走 config/soulblade-client.toml（见 SoulBladeTextures）。
 */
public class DomainSlashRenderer extends EntityRenderer<DomainSlashEntity> {

    public DomainSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(@NotNull DomainSlashEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack pose, @NotNull MultiBufferSource buffer, int packedLight) {
        if (entity.shouldRenderVisual()) {
            ResourceLocation[] styleFrames = SoulBladeTextures.domainSlashFrames(entity.getSlashStyle());
            ResourceLocation texture = styleFrames[entity.frameFor(styleFrames.length)];
            pose.pushPose();
            // 面片法线已在服务端对准观察者；再绕视线轴旋转出这一刀的角度
            pose.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
            pose.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));
            pose.mulPose(Axis.ZP.rotation(entity.getSlashAngle()));
            // 贴图斩痕约占对角线的 70%：边长按对角线换算，避免斜角埋进墙体
            float half = entity.getVisualLength() * 0.7071F * 0.5F;
            CrescentSlashRenderer.drawQuad(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(texture)),
                    half, half, 255);
            pose.popPose();
        }
        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull DomainSlashEntity entity) {
        return SoulBladeTextures.domainSlashFrames(entity.getSlashStyle())[0];
    }
}
