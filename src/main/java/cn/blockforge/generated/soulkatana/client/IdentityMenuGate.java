package cn.blockforge.generated.soulkatana.client;

import cn.blockforge.generated.soulkatana.SoulKatanaConfig;
import cn.blockforge.generated.soulkatana.SoulKatanaIdentity;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 化身界面闸门（客户端侧）：
 *  - Identity 本体的快捷键（MENU_KEY）会在客户端直接 setScreen(new IdentityScreen())，
 *    整个过程是「客户端按键 → 客户端 tick → 客户端开界面」，不经过服务端，
 *    因此服务端的 swap/unlock/tryCast 拦截对它完全无效。
 *  - 这里用 Forge 原生的 ScreenEvent.Opening 事件，在 IdentityScreen 即将打开时拦一道：
 *    若配置要求「须手持魂刀·真人」且主手不满足，则取消本次开界面 —— 静默无反应。
 *
 *  软依赖：只按类名字符串判断，不 import draylar.identity.*，Identity 缺席/改名都不会崩。
 */
@Mod.EventBusSubscriber(modid = "soulkatana", value = Dist.CLIENT)
public final class IdentityMenuGate {

    /** Identity 化身选择界面的完整类名（软依赖：字符串匹配，避免硬引用）。 */
    private static final String IDENTITY_SCREEN_CLASS = "draylar.identity.screen.IdentityScreen";

    private IdentityMenuGate() { }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() == null) {
            return;
        }
        if (!IDENTITY_SCREEN_CLASS.equals(event.getNewScreen().getClass().getName())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        // 配置未开启「须持刀」→ 放行，保持原版 Identity 行为。
        if (!SoulKatanaConfig.identityRequireSoulBlade()) {
            return;
        }
        // 主手未持「魂刀·真人」→ 取消开界面（静默无反应）。
        if (!SoulKatanaIdentity.isRealPerson(minecraft.player.getMainHandItem())) {
            event.setCanceled(true);
        }
    }
}
