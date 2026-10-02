package cn.blockforge.generated.soulkatana.client;

import cn.blockforge.generated.soulkatana.SoulKatanaIdentity;
import cn.blockforge.generated.soulkatana.SoulKatanaNetwork;
import cn.blockforge.generated.soulkatana.skill.SoulSkill;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 鼠标按键监听：
 *  - 中键（默认绑定，可在控制设置里改；同时硬编码 GLFW 中键兜底）→ 领域展开开/关。
 *  - 左键：准星未命中实体（MISS / BLOCK）时上报服务端施放 [解]。
 *    这比只依赖 LeftClickEmpty 更稳：拔刀剑空挥时准星经常是 BLOCK（脚下/空气块），
 *    原版 Minecraft.startAttack 不会走 Empty 分支，导致 [解] 永远不上报。
 *  - 右键：按下即上报服务端尝试施放 [捌]（目标由服务端宽松锁定：前方锥体内有实体才生效；
 *    准星大致朝前 6.5 格即可，不要求像素级对准）。
 * 手持魂刀时才接管，否则保留原版行为。
 */
@Mod.EventBusSubscriber(modid = "soulkatana", value = Dist.CLIENT)
public final class SoulKatanaMouseInput {

    /** 避免按住左键每 tick 连发 [解]：松开后再按才允许下一发。 */
    private static boolean leftClickArmed = true;
    /** 右键同理：按住右键不连发 [捌]，松开再按才允许下一发。 */
    private static boolean rightClickArmed = true;

    private SoulKatanaMouseInput() { }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }
        if (!SoulKatanaIdentity.isAnySoulBlade(minecraft.player.getMainHandItem())) {
            return;
        }
        int button = event.getButton();
        int action = event.getAction();

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (action == GLFW.GLFW_RELEASE) {
                leftClickArmed = true;
                return;
            }
            if (action != GLFW.GLFW_PRESS || !leftClickArmed) {
                return;
            }
            leftClickArmed = false;
            // 命中实体 → 普通攻击，不施放 [解]；MISS / BLOCK / 空挥 → 施放 [解]
            HitResult hit = minecraft.hitResult;
            boolean entityHit = hit != null && hit.getType() == HitResult.Type.ENTITY;
            if (!entityHit && SoulKatanaIdentity.isRealPerson(minecraft.player.getMainHandItem())) {
                // 真人刀空挥也算"发起攻击"：上报服务端结算真人领域的"全域必中"。
                // 这一发不受 [解]/[捌] 技能冷却限制——冷却只挡特殊攻击，不挡攻击本身。
                SoulKatanaNetwork.sendToServer(
                        new SoulKatanaNetwork.SkillPacket(SoulKatanaNetwork.ACTION_DOMAIN_ATTACK));
            }
            if (SoulSkill.isOnCooldown(minecraft.player)) {
                return;
            }
            if (entityHit || !SoulKatanaIdentity.isSoulKatana(minecraft.player.getMainHandItem())) {
                // 命中实体走原版普通攻击；真人刀没有 [解]（服务端也会拒），不发多余包
                return;
            }
            SoulKatanaNetwork.sendToServer(
                    new SoulKatanaNetwork.SkillPacket(SoulKatanaNetwork.ACTION_TATAYAKU));
            return;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (action == GLFW.GLFW_RELEASE) {
                rightClickArmed = true;
                return;
            }
            if (action != GLFW.GLFW_PRESS || !rightClickArmed) {
                return;
            }
            rightClickArmed = false;
            if (SoulSkill.isOnCooldown(minecraft.player)) {
                return;
            }
            // 按下即上报，目标由服务端宽松锁定（准星大致朝前 6.5 格内有实体即施放）。
            // 上一版要求客户端 hitResult 已是 ENTITY 才上报：拔刀剑触及距离很短，
            // 站远一点准星根本不会进入 ENTITY 态，[捌] 因此永远不触发——这就是"不生效"的根因。
            SoulKatanaNetwork.sendToServer(
                    new SoulKatanaNetwork.SkillPacket(SoulKatanaNetwork.ACTION_KANESADA));
            return;
        }

        if (action != GLFW.GLFW_PRESS) {
            return;
        }
        int boundKey = SoulKatanaClient.KEY_DOMAIN.getKey().getValue();
        boolean domainKey = button == boundKey || button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
        if (domainKey) {
            SoulKatanaClient.handleDomainKey();
            event.setCanceled(true);
        }
    }
}
