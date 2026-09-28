package cn.blockforge.generated.soulblade.entity;

import cn.blockforge.generated.soulblade.SoulBladeTextures;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

/**
 * [解] 月牙波动的渲染：垂直于推进方向的白色月牙面片（横三格、竖约1.25格），
 * 随推进距离淡出。使用原版现成 RenderType（entityTranslucent），光影兼容。
 * 贴图路径不写死在这里，统一走 config/soulblade-client.toml（见 SoulBladeTextures）。
 */
public class CrescentSlashRenderer extends EntityRenderer<CrescentSlashEntity> {
    private static final float HALF_WIDTH = 1.5F;
    private static final float HALF_HEIGHT = 0.65F;
    /** 淡出窗口：约 13 tick 推完可见段。 */
    private static final int FADE_TICKS = 13;

    public CrescentSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(@NotNull CrescentSlashEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack pose, @NotNull MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        // faceDirection 已把 yaw/pitch 设成"朝向施放者"（与 DomainSlashRenderer 同一套约定），
        // 因此贴图正面朝回玩家，月牙按贴图原样呈现，不再左右镜像。
        pose.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));
        float fade = 1.0F - (float) entity.tickCount / FADE_TICKS;
        int alpha = (int) (255 * Mth.clamp(fade, 0.0F, 1.0F));
        drawQuad(pose, buffer.getBuffer(RenderType.entityTranslucent(SoulBladeTextures.crescent())),
                HALF_WIDTH, HALF_HEIGHT, alpha);
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }

    /** 在局部 XY 平面（法线 +Z）画一个居中的贴图四边形。 */
    static void drawQuad(PoseStack pose, VertexConsumer consumer, float halfWidth, float halfHeight, int alpha) {
        PoseStack.Pose matrix = pose.last();
        float minX = -halfWidth;
        float maxX = halfWidth;
        float minY = -halfHeight;
        float maxY = halfHeight;
        int light = 0xF000F0;
        consumer.vertex(matrix.pose(), minX, maxY, 0.0F).color(255, 255, 255, alpha)
                .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(matrix.normal(), 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix.pose(), maxX, maxY, 0.0F).color(255, 255, 255, alpha)
                .uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(matrix.normal(), 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix.pose(), maxX, minY, 0.0F).color(255, 255, 255, alpha)
                .uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(matrix.normal(), 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix.pose(), minX, minY, 0.0F).color(255, 255, 255, alpha)
                .uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(matrix.normal(), 0.0F, 0.0F, 1.0F).endVertex();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull CrescentSlashEntity entity) {
        return SoulBladeTextures.crescent();
    }
}
