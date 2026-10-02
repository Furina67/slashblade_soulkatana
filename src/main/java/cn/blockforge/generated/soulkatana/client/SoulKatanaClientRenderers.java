package cn.blockforge.generated.soulkatana.client;

import cn.blockforge.generated.soulkatana.SoulKatanaEntities;
import cn.blockforge.generated.soulkatana.entity.CrescentSlashRenderer;
import cn.blockforge.generated.soulkatana.entity.DomainSlashRenderer;
import cn.blockforge.generated.soulkatana.entity.ShrineRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** [解]、领域斩击、神龛三个实体的渲染器注册。 */
@Mod.EventBusSubscriber(modid = "soulkatana", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SoulKatanaClientRenderers {

    private SoulKatanaClientRenderers() { }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SoulKatanaEntities.CRESCENT_SLASH.get(), CrescentSlashRenderer::new);
        event.registerEntityRenderer(SoulKatanaEntities.DOMAIN_SLASH.get(), DomainSlashRenderer::new);
        event.registerEntityRenderer(SoulKatanaEntities.SHRINE.get(), ShrineRenderer::new);
    }
}
