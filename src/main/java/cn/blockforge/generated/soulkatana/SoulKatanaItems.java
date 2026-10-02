package cn.blockforge.generated.soulkatana;

import cn.blockforge.generated.soulkatana.item.ReformedHumanItem;
import cn.blockforge.generated.soulkatana.item.SukunaFingerItem;
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
 * 因此附属只需要注册普通物品（手指、改造人），刀本身是"数据"。
 */
public final class SoulKatanaItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SoulKatana.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SoulKatana.MODID);

    /** 两面宿傩的手指：紫色稀有度，可食用，食用后立刻死亡。 */
    public static final RegistryObject<Item> SUKUNA_FINGER = ITEMS.register("sukuna_finger",
            SukunaFingerItem::new);

    /** 改造人：「魂刀·真人」的合成素材，雪屋地下室宝箱固定 4 个。 */
    public static final RegistryObject<Item> REFORMED_HUMAN = ITEMS.register("reformed_human",
            ReformedHumanItem::new);

    /** 独立创造标签栏「魂刀」：收录手指、改造人与本模组的命名刀，今后所有模组物品都放这里。 */
    public static final RegistryObject<CreativeModeTab> SOULBLADE_TAB = TABS.register("soulkatana",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.soulkatana"))
                    .icon(() -> {
                        ItemStack blade = SoulKatanaIdentity.namedBladeStackClient();
                        return blade.isEmpty() ? new ItemStack(SUKUNA_FINGER.get()) : blade;
                    })
                    .displayItems((params, output) -> {
                        output.accept(new ItemStack(SUKUNA_FINGER.get()));
                        output.accept(new ItemStack(REFORMED_HUMAN.get()));
                        var blades = params.holders().lookupOrThrow(SoulKatanaIdentity.NAMED_BLADES_KEY);
                        ItemStack sukuna = SoulKatanaIdentity.namedBladeStack(
                                blades, SoulKatanaIdentity.RYOMEN_SUKUNA);
                        if (!sukuna.isEmpty()) {
                            output.accept(sukuna);
                        }
                        ItemStack realPerson = SoulKatanaIdentity.namedBladeStack(
                                blades, SoulKatanaIdentity.REAL_PERSON);
                        if (!realPerson.isEmpty()) {
                            output.accept(realPerson);
                        }
                    })
                    .build());

    private SoulKatanaItems() { }
}
