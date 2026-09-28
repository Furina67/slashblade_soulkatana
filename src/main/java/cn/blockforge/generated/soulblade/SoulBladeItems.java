package cn.blockforge.generated.soulblade;

import cn.blockforge.generated.soulblade.item.SukunaFingerItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 魂刀物品与创造标签栏注册。
 * 拔刀剑重锋机制：所有刀共用主模组的 slashblade:slashblade 物品，
 * 刀的身份由命名刀 JSON（数据包注册表）+ blade_state 决定，
 * 因此附属只需要注册普通物品（手指），刀本身是"数据"。
 */
public final class SoulBladeItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SoulBlade.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SoulBlade.MODID);

    /** 两面宿傩的手指：紫色稀有度，可食用，食用后立刻死亡。 */
    public static final RegistryObject<Item> SUKUNA_FINGER = ITEMS.register("sukuna_finger",
            SukunaFingerItem::new);

    /** 独立创造标签栏「魂刀」：收录手指与本模组的命名刀，今后所有模组物品都放这里。 */
    public static final RegistryObject<CreativeModeTab> SOULBLADE_TAB = TABS.register("soulblade",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.soulblade"))
                    .icon(() -> {
                        ItemStack blade = SoulBladeIdentity.namedBladeStackClient();
                        return blade.isEmpty() ? new ItemStack(SUKUNA_FINGER.get()) : blade;
                    })
                    .displayItems((params, output) -> {
                        output.accept(new ItemStack(SUKUNA_FINGER.get()));
                        ItemStack blade = SoulBladeIdentity.namedBladeStack(
                                params.holders().lookupOrThrow(SoulBladeIdentity.NAMED_BLADES_KEY));
                        if (!blade.isEmpty()) {
                            output.accept(blade);
                        }
                    })
                    .build());

    private SoulBladeItems() { }
}
