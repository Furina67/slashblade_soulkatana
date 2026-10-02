package cn.blockforge.generated.soulkatana.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 「化身」选择界面的暂停包装（软依赖：只持有 Screen 引用，不 import draylar 类）。
 *
 * Identity 自带的 IdentityScreen 把 isPauseScreen()（Yarn 名 shouldPause）写死为
 * false —— 开着界面游戏照跑。我们不改它的源码，而是在外面套一层本类：
 *  - isPauseScreen() 返回 true：单人/局域网下开界面即挂起客户端世界；
 *  - 渲染、tick、鼠标、键盘事件全部透传给被包装的实例，交互与原生界面完全一致
 *    （EntityWidget 左键选中仍然只发 swap 请求 + disableAll，界面不自行关闭）；
 *  - 「选完自动关」由服务端驱动（CloseIdentityMenuPacket），本类不掺和；
 *  - 界面关闭时（选完/Esc/服务端下发）removed() 会透传，保证 Identity 自身清理执行。
 *
 * 1.20.1 官方映射下 Screen#init(Minecraft,int,int) 是 public final：
 * 直接调用它即可让内层界面补齐 minecraft/width/height/font 并跑自己的 init()，
 * 全程不需要反射。
 */
public final class PausableIdentityScreen extends Screen {
    private final Screen inner;

    public PausableIdentityScreen(Screen inner) {
        super(Component.translatable("screen.soulkatana.identity_menu"));
        this.inner = inner;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    protected void init() {
        this.inner.init(this.minecraft, this.width, this.height);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        // 内层界面自己按新尺寸重建布局；包装层没有控件，无需处理
        this.inner.resize(minecraft, width, height);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.inner.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        this.inner.tick();
    }

    @Override
    public void removed() {
        super.removed();
        this.inner.removed();
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.inner.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.inner.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.inner.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.inner.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return this.inner.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.inner.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return this.inner.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.inner.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return this.inner.shouldCloseOnEsc();
    }
}
