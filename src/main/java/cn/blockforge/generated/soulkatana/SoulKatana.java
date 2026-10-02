package cn.blockforge.generated.soulkatana;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * 拔刀剑附属·魂刀 —— 「拔刀剑：重锋」(slashblade, 1.20.1 Forge) 的附属模组。
 * 主模组缺失时 mods.toml 的硬前置会阻止游戏启动，即"无主模组则没有游戏内容"。
 *
 * 内容：
 *  - 命名刀「魂刀·两面宿傩」：承载在重锋的共用刀物品上（blade_state 机制），
 *    [解]（左键落空施放月牙波动）、[捌]（右键命中实体施放自适应斩）、[冷却]、
 *    [领域展开·伏魔御厨子]（鼠标中键）、手指数（魔改锻造数，喂手指增长）。
 *  - 命名刀「魂刀·真人」：主手持有时解锁「化身」(Identity) 的全部变身功能（软依赖，
 *    可在 config/soulkatana-server.toml 关闭），右键击中生物发动 [无为转变]。
 *  - 物品「两面宿傩的手指」：紫色稀有度，结构宝箱 20% 概率生成，食用即死。
 *  - 物品「改造人」：真人刀的合成素材，雪屋地下室宝箱固定刷新 4 个。
 *  - 独立创造标签栏「魂刀」，本模组物品全部收录其中。
 */
@Mod(SoulKatana.MODID)
public final class SoulKatana {
    public static final String MODID = "soulkatana";

    public static ResourceLocation prefix(String path) {
        return new ResourceLocation(MODID, path);
    }

    public SoulKatana() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        SoulKatanaItems.ITEMS.register(bus);
        SoulKatanaItems.TABS.register(bus);
        SoulKatanaEntities.ENTITY_TYPES.register(bus);
        SoulKatanaSounds.SOUNDS.register(bus);
        SoulKatanaEffects.register(bus);
        SoulKatanaNetwork.init();
        // 刀的全部贴图路径走外部配置：config/soulkatana-client.toml（改路径即换贴图，无需重编译）
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SoulKatanaTextures.SPEC, "soulkatana-client.toml");
        // 与「化身」联动的开关：config/soulkatana-server.toml（identity_require_soul_blade）
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SoulKatanaConfig.spec(), "soulkatana-server.toml");
    }
}
