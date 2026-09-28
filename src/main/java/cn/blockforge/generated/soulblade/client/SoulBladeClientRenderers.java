package cn.blockforge.generated.soulblade.client;

import cn.blockforge.generated.soulblade.SoulBladeEntities;
import cn.blockforge.generated.soulblade.entity.CrescentSlashRenderer;
import cn.blockforge.generated.soulblade.entity.DomainSlashRenderer;
import cn.blockforge.generated.soulblade.entity.ShrineRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** [解]、领域斩击、神龛三个实体的渲染器注册。 */
@Mod.EventBusSubscriber(modid = "soulblade", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SoulBladeClientRenderers {

    private SoulBladeClientRenderers() { }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SoulBladeEntities.CRESCENT_SLASH.get(), CrescentSlashRenderer::new);
        event.registerEntityRenderer(SoulBladeEntities.DOMAIN_SLASH.get(), DomainSlashRenderer::new);
        event.registerEntityRenderer(SoulBladeEntities.SHRINE.get(), ShrineRenderer::new);
    }
}
