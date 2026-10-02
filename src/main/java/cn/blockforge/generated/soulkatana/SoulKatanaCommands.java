package cn.blockforge.generated.soulkatana;

import cn.blockforge.generated.soulkatana.integration.IdentityInterop;
import cn.blockforge.generated.soulkatana.skill.SoulSkill;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

/**
 * /soulkatana status —— 联动自查指令。
 *
 * 打印：Identity 是否装载、变身审核钩子是否挂接成功（失败给出原因摘要）、
 * "变身需持真人刀"闸门开关、以及当前玩家剩余的[冷却]秒数。
 * 用于回答"弹没弹化身界面/闸门生没生效"这类问题的现场判定。
 */
@Mod.EventBusSubscriber(modid = SoulKatana.MODID)
public final class SoulKatanaCommands {

    private SoulKatanaCommands() { }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("soulkatana")
                .then(Commands.literal("status")
                        .executes(context -> reportStatus(context.getSource()))));
    }

    private static int reportStatus(CommandSourceStack source) {
        boolean loaded = IdentityInterop.isIdentityLoaded();
        // isAvailable()：已挂接则直接为 true；未挂接但 Identity 在场时当场懒重试一次
        boolean wired = loaded && IdentityInterop.isAvailable();
        boolean gateOn = SoulKatanaConfig.identityRequireSoulBlade();
        ServerPlayer player = source.getPlayer();
        double cdSeconds = player != null ? SoulSkill.cooldownRemainingTicks(player) / 20.0D : -1.0D;

        String identityLine = loaded
                ? "「化身」(identity) 已装载；变身审核钩子"
                        + (wired ? "已挂接：[无为转变]将弹出化身选择界面。"
                                 : "挂接失败（" + IdentityInterop.lastWireError()
                                         + "），[无为转变]退回随机转变。")
                : "未检测到「化身」(identity)：[无为转变]使用随机转变，变身不受闸门约束。";
        String gateLine = gateOn
                ? "闸门：开启——获得/切换化身需主手持「魂刀·真人」（变回本人不受限）。"
                : "闸门：关闭——化身获得/切换保持 Identity 原行为。";

        source.sendSuccess(() -> Component.literal("[魂刀] " + identityLine)
                .withStyle(loaded && wired ? ChatFormatting.GREEN : ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("[魂刀] " + gateLine)
                .withStyle(gateOn ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY), false);
        if (player != null) {
            String cdLine = cdSeconds > 0.0D
                    ? String.format(Locale.ROOT, "当前[冷却]剩余 %.1f 秒（只锁特殊攻击，普攻不受影响）。", cdSeconds)
                    : "当前不在[冷却]中。";
            source.sendSuccess(() -> Component.literal("[魂刀] " + cdLine)
                    .withStyle(cdSeconds > 0.0D ? ChatFormatting.RED : ChatFormatting.GRAY), false);
        }
        return 1;
    }
}
