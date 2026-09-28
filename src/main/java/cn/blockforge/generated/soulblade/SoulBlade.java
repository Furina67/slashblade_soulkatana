package cn.blockforge.generated.soulblade;

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
 *  - 物品「两面宿傩的手指」：紫色稀有度，结构宝箱 20% 概率生成，食用即死。
 *  - 独立创造标签栏「魂刀」，本模组物品全部收录其中。
 */
@Mod(SoulBlade.MODID)
public final class SoulBlade {
    public static final String MODID = "soulblade";

    public static ResourceLocation prefix(String path) {
        return new ResourceLocation(MODID, path);
    }

    public SoulBlade() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        SoulBladeItems.ITEMS.register(bus);
        SoulBladeItems.TABS.register(bus);
        SoulBladeEntities.ENTITY_TYPES.register(bus);
        SoulBladeSounds.SOUNDS.register(bus);
        SoulBladeNetwork.init();
        // 刀的全部贴图路径走外部配置：config/soulblade-client.toml（改路径即换贴图，无需重编译）
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SoulBladeTextures.SPEC, "soulblade-client.toml");
    }}
