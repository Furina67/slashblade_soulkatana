package cn.blockforge.generated.soulblade;

import cn.blockforge.generated.soulblade.item.SukunaFingerItem;
import cn.blockforge.generated.soulblade.skill.SoulSkill;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 交互与生成事件层（零编译期依赖版）：
 *  - 左键：手持魂刀时拦截"挖方块"（替换为 [解]），落空同理；命中实体走原版普通攻击。
 *  - 右键：准星命中实体时拦截蓄力（替换为 [捌]），否则原版行为。
 *  - 铁砧：魂刀 + 两面宿傩的手指 → 手指数 +1（魔改的锻造数）。
 *  - tooltip：刀名染紫（附魔金苹果同款稀有度色）、"锻造数"行替换为"手指数"、附操作提示。
 *  - 攻速：手持魂刀时把攻击速度压到 1.6（面板值）。
 *  - 战利品：结构宝箱 20% 概率出现手指。
 */
@Mod.EventBusSubscriber(modid = "soulblade")
public final class SoulBladeEvents {
    /** 攻速 1.6：原版基础 4.0，差值 -2.4 以 transient modifier 挂在玩家身上。 */
    private static final UUID SOUL_BLADE_SPEED_UUID =
            UUID.fromString("7c1f1b6e-52a4-4a3e-9e1d-256070859001");
    private static final AttributeModifier SOUL_BLADE_SPEED = new AttributeModifier(
            SOUL_BLADE_SPEED_UUID, "soulblade_speed", 1.6D - 4.0D,
            AttributeModifier.Operation.ADDITION);

    private SoulBladeEvents() { }

    /** 左键点方块：手持魂刀时替换掉原版挖方块，施放 [解]。 */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !SoulBladeIdentity.isSoulBlade(event.getItemStack())) {
            return;
        }
        if (event.getSide() == net.minecraftforge.fml.LogicalSide.CLIENT) {
            event.setCanceled(true);
            // 客户端上报已由 SoulBladeMouseInput 统一处理，这里只拦挖方块
        } else {
            // 兜底：非本附属客户端或创造直连时，服务端同样拦截挖方块并结算 [解]
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer player) {
                SoulSkill.castTatayaku(player);
            }
        }
    }

    /** 左键点空气：只在客户端有事件，上报由服务端结算。 */
    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (!SoulBladeIdentity.isSoulBlade(event.getItemStack())) {
            return;
        }
        // SoulBladeMouseInput 已经上报过一次；这里作为兜底（未装本附属客户端时）。
        SoulBladeNetwork.sendToServer(
                new SoulBladeNetwork.SkillPacket(SoulBladeNetwork.ACTION_TATAYAKU));
    }

    /**
     * 右键兜底路径（主路径是客户端按键上报）：准星前方有活体目标时右键一律归 [捌]
     * 占用——拦掉主模组蓄力；是否真的施放由 castKanesada 内部按冷却判定（与客户端上报去重）。
     * 目标锁定用宽松的"前方锥体"，不要求像素级对准。
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getSide() != net.minecraftforge.fml.LogicalSide.SERVER
                || event.getHand() != InteractionHand.MAIN_HAND
                || !SoulBladeIdentity.isSoulBlade(event.getItemStack())) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            net.minecraft.world.entity.LivingEntity target = SoulSkill.findKanesadaTarget(serverPlayer);
            if (target != null) {
                event.setCanceled(true);
                SoulSkill.castKanesada(serverPlayer, target);
            }
        }
    }

    /** 手持魂刀期间把攻速压到 1.6（离开主手即恢复）。 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) {
            return;
        }
        AttributeInstance speed = event.player.getAttribute(Attributes.ATTACK_SPEED);
        if (speed == null) {
            return;
        }
        if (SoulBladeIdentity.isSoulBlade(event.player.getMainHandItem())) {
            if (!speed.hasModifier(SOUL_BLADE_SPEED)) {
                speed.addTransientModifier(SOUL_BLADE_SPEED);
            }
        } else if (speed.hasModifier(SOUL_BLADE_SPEED)) {
            speed.removeModifier(SOUL_BLADE_SPEED);
        }
    }

    /**
     * 魂刀 tooltip：
     *  - 刀名染成紫色（附魔金苹果同款）；
     *  - 主模组的"锻造数"行替换为"手指数"（实时值）；
     *  - 追加一行操作提示。
     */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!SoulBladeIdentity.isSoulBlade(stack)) {
            return;
        }
        List<Component> tooltip = event.getToolTip();
        if (!tooltip.isEmpty() && tooltip.get(0) instanceof net.minecraft.network.chat.MutableComponent name) {
            name.withStyle(ChatFormatting.DARK_PURPLE);
        }
        int fingers = SoulBladeIdentity.getFingerCount(stack);
        for (int i = 0; i < tooltip.size(); i++) {
            if (tooltip.get(i).getContents() instanceof TranslatableContents contents
                    && "slashblade.tooltip.refine".equals(contents.getKey())) {
                Component line = Component.translatable("tooltip.soulblade.finger_count", fingers)
                        .withStyle(fingers >= 20 ? ChatFormatting.LIGHT_PURPLE
                                : fingers >= 10 ? ChatFormatting.GOLD : ChatFormatting.YELLOW);
                tooltip.set(i, line);
                break;
            }
        }
        tooltip.add(Component.translatable("tooltip.soulblade.skills")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    /**
     * 铁砧喂手指：左槽魂刀、右槽手指 → 手指数 +1（消耗一根手指，2 级）。
     * 手指数复用 blade_state 的 RepairCounter（tooltip 已替换显示为"手指数"）。
     */
    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (event.getPlayer().level().isClientSide()) {
            return;
        }
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (!SoulBladeIdentity.isSoulBlade(left)
                || right.getItem() != SoulBladeItems.SUKUNA_FINGER.get()) {
            return;
        }
        int fingers = SoulBladeIdentity.getFingerCount(left);
        ItemStack output = left.copy();
        CompoundTag fresh = SoulBladeIdentity.liveState(left).copy();
        fresh.putInt("RepairCounter", fingers + 1);
        output.getOrCreateTag().put("bladeState", fresh);
        event.setOutput(output);
        event.setCost(2);
    }

    /** 结构宝箱 20% 概率掉落手指：向常见结构宝箱追加一个独立掉落池。 */
    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        ResourceLocation key = event.getName();
        if (!"minecraft".equals(key.getNamespace()) || !key.getPath().startsWith("chests/")) {
            return;
        }
        if (!STRUCTURE_CHESTS.contains(key)) {
            return;
        }
        event.getTable().addPool(net.minecraft.world.level.storage.loot.LootPool.lootPool()
                .name("soulblade_finger")
                .setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1.0F))
                .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(
                                SoulBladeItems.SUKUNA_FINGER.get())
                        .setWeight(1))
                .add(net.minecraft.world.level.storage.loot.entries.EmptyLootItem.emptyItem().setWeight(4))
                .build());
    }

    private static final Set<ResourceLocation> STRUCTURE_CHESTS = Set.of(
            new ResourceLocation("minecraft", "chests/ancient_city"),
            new ResourceLocation("minecraft", "chests/ancient_city_ice"),
            new ResourceLocation("minecraft", "chests/woodland_mansion"),
            new ResourceLocation("minecraft", "chests/desert_temple"),
            new ResourceLocation("minecraft", "chests/jungle_temple"),
            new ResourceLocation("minecraft", "chests/end_city_treasure"),
            new ResourceLocation("minecraft", "chests/bastion_treasure"),
            new ResourceLocation("minecraft", "chests/buried_treasure"),
            new ResourceLocation("minecraft", "chests/shipwreck_treasure"),
            new ResourceLocation("minecraft", "chests/stronghold_corridor"));
}
