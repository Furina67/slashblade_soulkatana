package cn.blockforge.generated.soulkatana;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 服务端配置（config/soulkatana-server.toml，随世界存档保存在服务端）。
 *
 * identity_require_soul_blade：
 *   true （默认）——装载 Identity（「化身」）时，玩家必须主手持「魂刀·真人」
 *                  才能获得 / 切换化身（变回玩家本人永远放行，不会把人锁在生物形态）；
 *   false         ——恢复 Identity 原行为，本模组不加任何变身限制。
 * Identity 未安装时该项无实际效果（本模组照常运行）。
 */
public final class SoulKatanaConfig {
    private static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue IDENTITY_REQUIRE_SOUL_BLADE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("魂刀·真人 与「化身」(Identity) 模组的联动设置。Identity 未安装时这些设置无实际效果。")
                .push("identity");
        IDENTITY_REQUIRE_SOUL_BLADE = builder
                .comment(
                        "是否要求主手持「魂刀·真人」才能获得/切换化身。",
                        "true  = 手持魂刀·真人才可变身（变回玩家本人不受限制）；",
                        "false = 不限制，Identity 原行为。")
                .define("identity_require_soul_blade", true);
        builder.pop();
        SPEC = builder.build();
    }

    private SoulKatanaConfig() { }

    /** 读取"变身需持真人刀"开关；配置尚未加载（极早期/异常）时按默认值 true 处理。 */
    public static boolean identityRequireSoulBlade() {
        try {
            return IDENTITY_REQUIRE_SOUL_BLADE.get();
        } catch (Throwable notLoadedYet) {
            return true;
        }
    }

    public static ForgeConfigSpec spec() {
        return SPEC;
    }
}
