package cn.blockforge.generated.soulkatana.client;

import cn.blockforge.generated.soulkatana.SoulKatanaSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * [无为转变] 客户端侧：弹出 Identity 自带的化身选择界面。
 *
 * Identity 的 IdentityScreen 是无参构造（见其 MenuKeyPressHandler 的用法），
 * 这里用反射拿到它——本模组编译期不依赖 Identity；
 * 客户端没装 Identity（理论上单人不会出现）时静默失败，
 * 服务端的挂起选择会在 60 秒后自然过期。
 *
 * 拿到实例后套一层 PausableIdentityScreen：isPauseScreen()=true，
 * 开界面即暂停游戏（单人/局域网）。界面打开的同时播一次
 * idle_transfiguration（UI 通道，SimpleSoundInstance.forUI）。
 * 选完的关闭动作由服务端下发 CloseIdentityMenuPacket 驱动（见 closeIdentityMenuIfOpen）。
 */
public final class IdentityMenuClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("SoulKatana|IdentityMenuClient");
    private static final String SCREEN_CLASS = "draylar.identity.screen.IdentityScreen";

    private IdentityMenuClient() { }

    public static void openIdentityMenu() {
        try {
            Class<?> screenClass = Class.forName(SCREEN_CLASS);
            Screen screen = (Screen) screenClass.getDeclaredConstructor().newInstance();
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(new PausableIdentityScreen(screen));
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                    SoulKatanaSounds.IDLE_TRANSFIGURATION.get(), 1.0F));
        } catch (Throwable identityMissingOnClient) {
            LOGGER.debug("[魂刀] 客户端无法打开化身选择界面（Identity 不在客户端？）。", identityMissingOnClient);
        }
    }

    /**
     * 服务端驱动的"选完自动关闭"：只认化身选择界面本身——我们套的暂停层，
     * 或玩家用 Identity 快捷键换开的原生 IdentityScreen（软依赖：按类名字符串匹配）。
     * 其余界面（背包/箱子等）一律不动，避免误关。
     */
    public static void closeIdentityMenuIfOpen() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (screen == null) {
            return;
        }
        if (screen instanceof PausableIdentityScreen
                || SCREEN_CLASS.equals(screen.getClass().getName())) {
            minecraft.setScreen(null);
        }
    }
}
