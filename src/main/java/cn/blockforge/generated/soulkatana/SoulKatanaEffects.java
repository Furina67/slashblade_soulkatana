package cn.blockforge.generated.soulkatana;

import cn.blockforge.generated.soulkatana.effect.SoulTouchEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * MobEffect 注册器（本模组第一个效果注册）：
 * [触及灵魂] 是真人领域【自闭圆顿裹】"全域必中"的纯标记 BUFF，
 * 它的存在本身就是事件层的判定依据（见 SoulKatanaEvents 的减伤清空处理）。
 */
public final class SoulKatanaEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, SoulKatana.MODID);

    /** [触及灵魂]：无属性修改、无 tick 逻辑、无视觉提示的标记效果。 */
    public static final RegistryObject<MobEffect> SOUL_TOUCH =
            EFFECTS.register("soul_touch", SoulTouchEffect::new);

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }

    private SoulKatanaEffects() { }
}
