package cn.blockforge.generated.soulkatana;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class SoulKatanaSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SoulKatana.MODID);

    /** 领域前摇（开始读条）时播放一次的"伏魔御厨子前摇"。 */
    public static final RegistryObject<SoundEvent> DOMAIN_CHARGE = register("domain_charge");
    /** 神龛展开（领域开启）时播放一次的"伏魔御厨子"。 */
    public static final RegistryObject<SoundEvent> DOMAIN_EXPAND = register("domain_expand");
    /** [解]/[捌] 出手时的斩击音。 */
    public static final RegistryObject<SoundEvent> SUKUNA_SLASH = register("sukuna_slash_1");
    /** 宿傩「领域展开」的前置音：在 DOMAIN_EXPAND 之前播放（仅主手持宿傩刀）。 */
    public static final RegistryObject<SoundEvent> MALEVOLENT_SHRINE = register("malevolent_shrine");
    /** [无为转变] 化身选择界面打开时播放（客户端 UI 音）。 */
    public static final RegistryObject<SoundEvent> IDLE_TRANSFIGURATION = register("idle_transfiguration");
    /** 真人领域【自闭圆顿裹】展开时播放的"自闭圆顿裹"。 */
    public static final RegistryObject<SoundEvent> SELF_EMBODIMENT_OF_PERFECTION =
            register("self_embodiment_of_perfection");
    /** 真人领域进入前摇（toggle 触发 CHARGING）时在界主位置播放的展开台词。 */
    public static final RegistryObject<SoundEvent> SELF_EMBODIMENT_OF_PERFECTION_VOICELINE =
            register("self_embodiment_of_perfection_voiceline");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(SoulKatana.MODID, name)));
    }

    private SoulKatanaSounds() { }
}
