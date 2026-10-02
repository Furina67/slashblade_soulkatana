package cn.blockforge.generated.soulkatana.client;

import cn.blockforge.generated.soulkatana.SoulKatanaIdentity;
import cn.blockforge.generated.soulkatana.SoulKatanaNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 魂刀客户端接入：注册"领域展开"按键（默认鼠标中键，可在控制设置改键）。
 * 刀本身的渲染/第一人称持握/连段动作全部由主模组的 slashblade:slashblade
 * 物品管线接管，附属不需要注册渲染器或物品属性。
 */
@Mod.EventBusSubscriber(modid = "soulkatana", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SoulKatanaClient {

    /** [领域展开]：鼠标中键。 */
    public static final KeyMapping KEY_DOMAIN = new KeyMapping(
            "key.soulkatana.domain", KeyConflictContext.IN_GAME,
            InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_MIDDLE, "key.categories.soulkatana");

    private SoulKatanaClient() { }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KEY_DOMAIN);
    }

    /** 中键按下：手持任意魂刀（宿傩/真人）才上报服务端开/关领域（否则保留原版行为，如创造取方块）。 */
    public static void handleDomainKey() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (SoulKatanaIdentity.isAnySoulBlade(minecraft.player.getMainHandItem())) {
            SoulKatanaNetwork.sendToServer(
                    new SoulKatanaNetwork.SkillPacket(SoulKatanaNetwork.ACTION_DOMAIN_TOGGLE));
        }
    }

    /**
     * 游戏内按键轮询：即使鼠标事件被别的模组吞掉（例如创造中键取方块），
     * consumeClick 也能在本 tick 消费一次领域展开。
     */
    @Mod.EventBusSubscriber(modid = "soulkatana", value = Dist.CLIENT)
    public static final class DomainKeyPoller {
        private DomainKeyPoller() { }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            while (KEY_DOMAIN.consumeClick()) {
                handleDomainKey();
            }
        }
    }
}
