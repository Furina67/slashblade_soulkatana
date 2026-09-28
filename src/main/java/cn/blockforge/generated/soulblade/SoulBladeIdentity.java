package cn.blockforge.generated.soulblade;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 魂刀身份识别与主模组桥接（零编译期依赖版）。
 *
 * 本附属不 import 任何 mods.flammpfeil 类：
 *  - 刀的身份 = 主模组共用物品 slashblade:slashblade + blade_state 里的
 *    TranslationKey "item.soulblade.ryomen_sukuna"（命名刀 JSON 写入，随存档保存）；
 *  - 取"活的" blade_state（击杀数/耀魂/手指数等实时值）通过反射调用主模组的
 *    ItemSlashBlade.BLADESTATE capability，编译期完全不需要主模组在 classpath 上。
 * 没装主模组时这些查询一律返回空/否，附属自然"没有游戏内容"。
 */
public final class SoulBladeIdentity {
    /** 命名刀定义 id（data/soulblade/slashblade/named_blades/ryomen_sukuna.json）。 */
    public static final ResourceLocation RYOMEN_SUKUNA =
            new ResourceLocation(SoulBlade.MODID, "ryomen_sukuna");
    /** 主模组的命名刀数据包注册表（用字符串构造，不引用主模组类）。 */
    public static final ResourceKey<Registry<Object>> NAMED_BLADES_KEY = ResourceKey.createRegistryKey(
            new ResourceLocation("slashblade", "named_blades"));

    private static final ResourceLocation SLASHBLADE_ITEM = new ResourceLocation("slashblade", "slashblade");
    private static final String BLADESTATE_TAG = "bladeState";
    /** 主模组 ISlashBladeState.serializeNBT 写的字段是小写 t 的 "translationKey"（旧拼写也兼容）。 */
    private static final String TRANSLATION_KEY = "translationKey";
    private static final String TRANSLATION_KEY_LEGACY = "TranslationKey";

    private static volatile Capability<?> bladeStateCapability;
    private static volatile boolean capabilityProbed;

    private SoulBladeIdentity() { }

    private static Item slashbladeItem() {
        return ForgeRegistries.ITEMS.getValue(SLASHBLADE_ITEM);
    }

    /** 该物品栈是否是「魂刀·两面宿傩」（主模组共用物品 + 命名刀身份）。 */
    public static boolean isSoulBlade(ItemStack stack) {
        Item item = slashbladeItem();
        if (stack.isEmpty() || item == null || stack.getItem() != item) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(BLADESTATE_TAG)) {
            return false;
        }
        CompoundTag state = tag.getCompound(BLADESTATE_TAG);
        String expected = "item." + RYOMEN_SUKUNA.getNamespace() + "." + RYOMEN_SUKUNA.getPath();
        if (expected.equals(state.getString(TRANSLATION_KEY))
                || expected.equals(state.getString(TRANSLATION_KEY_LEGACY))) {
            return true;
        }
        // 标签快照里没有时（极少数情况：capability 尚未回写标签），退回实时状态再认一次
        CompoundTag live = liveState(stack);
        return expected.equals(live.getString(TRANSLATION_KEY))
                || expected.equals(live.getString(TRANSLATION_KEY_LEGACY));
    }

    /** 反射拿主模组的 blade_state 实时序列化（capability 不可用时退回静态标签）。 */
    @SuppressWarnings("unchecked")
    public static CompoundTag liveState(ItemStack stack) {
        try {
            Capability<?> cap = bladeStateCapability();
            if (cap != null) {
                LazyOptional<Object> holder = stack.getCapability((Capability<Object>) cap);
                Object state = holder.orElse(null);
                if (state != null) {
                    return (CompoundTag) state.getClass().getMethod("serializeNBT").invoke(state);
                }
            }
        } catch (Throwable ignored) {
            // 主模组未装/结构不同：退回标签快照
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(BLADESTATE_TAG) ? tag.getCompound(BLADESTATE_TAG) : new CompoundTag();
    }

    private static Capability<?> bladeStateCapability() throws Exception {
        if (!capabilityProbed) {
            capabilityProbed = true;
            Class<?> itemClass = Class.forName("mods.flammpfeil.slashblade.item.ItemSlashBlade");
            bladeStateCapability = (Capability<?>) itemClass.getField("BLADESTATE").get(null);
        }
        return bladeStateCapability;
    }

    /** 读"手指数"（复用 blade_state 的 RepairCounter，即被替换显示的锻造数）。 */
    public static int getFingerCount(ItemStack stack) {
        return liveState(stack).getInt("RepairCounter");
    }

    /** 从命名刀注册表反射构造一把满身份的魂刀（创造栏图标/收录用）。 */
    public static ItemStack namedBladeStack(net.minecraft.core.HolderLookup.RegistryLookup<Object> lookup) {
        try {
            Object definition = lookup.get(ResourceKey.create(NAMED_BLADES_KEY, RYOMEN_SUKUNA))
                    .map(net.minecraft.core.Holder::value).orElse(null);
            if (definition == null) {
                return ItemStack.EMPTY;
            }
            Object blade = definition.getClass().getMethod("getBlade").invoke(definition);
            return blade instanceof ItemStack stack ? stack : ItemStack.EMPTY;
        } catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }
    }

    /** 客户端环境下的便利版（图标等无参场合）。 */
    public static ItemStack namedBladeStackClient() {
        try {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.level == null) {
                return ItemStack.EMPTY;
            }
            return namedBladeStack(minecraft.level.registryAccess().lookupOrThrow(NAMED_BLADES_KEY));
        } catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }
    }

    /** damage_type 注册键（手指即死）。 */
    public static ResourceKey<net.minecraft.world.damagesource.DamageType> fingerDamageKey() {
        return ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(SoulBlade.MODID, "sukuna_finger"));
    }
}
