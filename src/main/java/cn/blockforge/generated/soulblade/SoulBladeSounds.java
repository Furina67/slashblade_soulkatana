package cn.blockforge.generated.soulblade;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class SoulBladeSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SoulBlade.MODID);

    /** 领域前摇（开始读条）时播放一次的"伏魔御厨子前摇"。 */
    public static final RegistryObject<SoundEvent> DOMAIN_CHARGE = register("domain_charge");
    /** 神龛展开（领域开启）时播放一次的"伏魔御厨子"。 */
    public static final RegistryObject<SoundEvent> DOMAIN_EXPAND = register("domain_expand");
    /** [解]/[捌] 出手时的斩击音。 */
    public static final RegistryObject<SoundEvent> SUKUNA_SLASH = register("sukuna_slash_1");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(SoulBlade.MODID, name)));
    }

    private SoulBladeSounds() { }
}
