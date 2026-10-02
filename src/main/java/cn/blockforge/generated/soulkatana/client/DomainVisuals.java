package cn.blockforge.generated.soulkatana.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import cn.blockforge.generated.soulkatana.SoulKatanaNetwork;

/**
 * [领域展开]的客户端环境视觉（移植自宿傩参考包 DomainVisuals，只保留领域链）：
 *  - 前摇（1.5 秒）：雾/天空渐染暗红（天空与地平线由雾色推导，改一处即可）；
 *  - 开启：满额暗红、地形雾收缩到等效视距 4 区块、12 秒轻微屏幕震颤、
 *    挂"暗化 + 色差"后处理链（soulkatana:shaders/post/sukuna_domain.json）；
 *  - 关闭/死亡/换维度：状态清零并卸载后处理，绝不残留。
 */
@Mod.EventBusSubscriber(modid = "soulkatana", value = Dist.CLIENT)
public final class DomainVisuals {
    /** 满额时的暗红雾色。 */
    private static final float DARK_RED_R = 0.30F;
    private static final float DARK_RED_G = 0.018F;
    private static final float DARK_RED_B = 0.055F;
    private static final int PRECAST_RAMP_TICKS = 30;
    private static final int PRECAST_TIMEOUT_TICKS = 40;
    private static final float STRENGTH_STEP = 0.05F;
    /** 领域开启期间的等效地形雾：远平面 64 格（配合 64 格半径），近平面按原版公式推导。 */
    private static final float FOG_FAR = 64.0F;
    private static final float FOG_NEAR = 50.0F;
    private static final int DOMAIN_SHAKE_TICKS = 240;
    private static final ResourceLocation POST_CHAIN =
            new ResourceLocation("soulkatana", "shaders/post/sukuna_domain.json");
    private static final int MAX_LOAD_ATTEMPTS = 20;

    private static boolean precasting;
    private static boolean auraActive;
    private static int precastTicks;
    private static float strength;
    private static boolean postLoaded;
    private static int loadAttempts;
    private static int shakeTicks;
    private static ResourceKey<Level> lastDimension;

    private DomainVisuals() { }

    /** 服务端包入口：mode 取 SoulKatanaNetwork.DOMAIN_VISUAL_PRECAST/ACTIVE/OFF。 */
    public static void applyDomainVisual(int mode) {
        if (mode == SoulKatanaNetwork.DOMAIN_VISUAL_ACTIVE) {
            precasting = false;
            strength = 1.0F;
            if (!auraActive) {
                auraActive = true;
                // 只有上升沿才重放 12 秒震颤；0.5 秒一次的幂等对账包不会反复刷新
                shakeTicks = DOMAIN_SHAKE_TICKS;
            }
        } else if (mode == SoulKatanaNetwork.DOMAIN_VISUAL_PRECAST) {
            if (!auraActive) {
                precasting = true;
                precastTicks = 0;
            }
        } else {
            precasting = false;
            auraActive = false;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            resetAll();
            lastDimension = null;
            return;
        }
        if (minecraft.player.isDeadOrDying()) {
            precasting = false;
            auraActive = false;
        }
        if (!minecraft.level.dimension().equals(lastDimension)) {
            resetAll();
            lastDimension = minecraft.level.dimension();
        }
        if (precasting && ++precastTicks > PRECAST_TIMEOUT_TICKS) {
            precasting = false;
        }
        if (shakeTicks > 0 && !auraActive) {
            shakeTicks = 0;
        }
        float target = auraActive ? 1.0F : precasting
                ? Math.min(1.0F, precastTicks / (float) PRECAST_RAMP_TICKS) : 0.0F;
        strength += Mth.clamp(target - strength, -STRENGTH_STEP, STRENGTH_STEP);
        if (!auraActive && !precasting && strength < 0.004F) {
            strength = 0.0F;
        }
        syncPostEffect(minecraft);
    }

    /** 挂撤后处理链；链被原版/别的模组中途撤走时（每 tick 对账）自动补挂。 */
    private static void syncPostEffect(Minecraft minecraft) {
        boolean wanted = auraActive;
        if (wanted && !postLoaded) {
            if (loadAttempts < MAX_LOAD_ATTEMPTS) {
                try {
                    minecraft.gameRenderer.loadEffect(POST_CHAIN);
                    postLoaded = true;
                    loadAttempts = 0;
                } catch (Throwable ignored) {
                    loadAttempts++;
                }
            }
        } else if (wanted && minecraft.gameRenderer.currentEffect() == null) {
            // 槽位被顶掉：再补挂一次
            postLoaded = false;
            loadAttempts = 0;
        } else if (!wanted && postLoaded) {
            if (minecraft.gameRenderer.currentEffect() != null) {
                minecraft.gameRenderer.shutdownEffect();
            }
            postLoaded = false;
            loadAttempts = 0;
        }
    }

    /** 换世界/断开时清零。 */
    public static void resetAll() {
        precasting = false;
        auraActive = false;
        precastTicks = 0;
        strength = 0.0F;
        shakeTicks = 0;
        if (postLoaded) {
            Minecraft.getInstance().gameRenderer.shutdownEffect();
            postLoaded = false;
        }
        loadAttempts = 0;
    }

    /** 前摇/领域期间把雾色向暗红插值（天空、云、地平线同步）。 */
    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (strength <= 0.004F) {
            return;
        }
        if (event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        event.setRed(Mth.lerp(strength, event.getRed(), DARK_RED_R));
        event.setGreen(Mth.lerp(strength, event.getGreen(), DARK_RED_G));
        event.setBlue(Mth.lerp(strength, event.getBlue(), DARK_RED_B));
    }

    /** 领域开启期间收缩地形雾（只收缩、不放大）。 */
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!auraActive || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) {
            return;
        }
        if (event.getFarPlaneDistance() <= FOG_FAR) {
            return;
        }
        event.setFarPlaneDistance(FOG_FAR);
        event.setNearPlaneDistance(FOG_NEAR);
    }

    /** 12 秒轻微屏幕震颤：给相机 roll 加一条衰减的呼吸抖动。 */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (shakeTicks <= 0) {
            return;
        }
        float decay = Math.min(1.0F, shakeTicks / 60.0F);
        float time = (float) (System.currentTimeMillis() % 100000L) / 1000.0F;
        event.setRoll(event.getRoll()
                + Mth.sin(time * 6.3F) * 0.011F * decay
                + Mth.sin(time * 17.0F) * 0.004F * decay);
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().getFps() > 0) {
            shakeTicks--;
        }
    }
}
