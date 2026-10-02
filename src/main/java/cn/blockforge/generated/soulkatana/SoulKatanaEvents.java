package cn.blockforge.generated.soulkatana;

import cn.blockforge.generated.soulkatana.integration.IdleTransformation;
import cn.blockforge.generated.soulkatana.item.SukunaFingerItem;
import cn.blockforge.generated.soulkatana.skill.SoulSkill;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 交互与生成事件层（零编译期依赖版）：
 *  - 左键：手持魂刀时拦截"挖方块"（替换为 [解]），落空同理；命中实体走原版普通攻击。
 *  - 右键：准星命中实体时拦截蓄力（替换为 [捌]），否则原版行为；
 *          持「魂刀·真人」右键击中生物 → [无为转变]（见 IdleTransformation）。
 *  - 雪屋地下室宝箱：固定 4 个改造人；其余常见结构宝箱 20% 概率出手指。
 *  - 铁砧：魂刀 + 两面宿傩的手指 → 手指数 +1（魔改的锻造数）。
 *  - tooltip：刀名染紫（附魔金苹果同款稀有度色）、"锻造数"行替换为"手指数"、附操作提示。
 *  - 攻速：手持魂刀时把攻击速度压到 1.6（面板值）。
 *  - 战利品：结构宝箱 20% 概率出现手指。
 *  - [触及灵魂] 真实伤害：挂在真人领域目标身上的标记 BUFF，令其受到的护甲/抗性/吸收
 *    减伤全部作废（LivingHurtEvent 记原始伤害 → LivingDamageEvent 改写回并退还吸收心）。
 */
@Mod.EventBusSubscriber(modid = "soulkatana")
public final class SoulKatanaEvents {
    /** 攻速 1.6：原版基础 4.0，差值 -2.4 以 transient modifier 挂在玩家身上。 */
    private static final UUID SOUL_KATANA_SPEED_UUID =
            UUID.fromString("7c1f1b6e-52a4-4a3e-9e1d-256070859001");
    private static final AttributeModifier SOUL_KATANA_SPEED = new AttributeModifier(
            SOUL_KATANA_SPEED_UUID, "soulkatana_speed", 1.6D - 4.0D,
            AttributeModifier.Operation.ADDITION);

    private SoulKatanaEvents() { }

    // ======================================================================================
    // [触及灵魂] 真实伤害层（真人领域【自闭圆顿裹】）
    //
    // Forge 47.4.0 的结算顺序（已在 actuallyHurt 字节码里核对）：
    //   LivingHurtEvent(护甲前，原始值) → 护甲 → 抗性(魔咒) → 伤害吸收扣心 →
    //   LivingDamageEvent(即将真正扣血的最终值) → setHealth。
    // 所以"清空一切减伤"落在这一头一尾：
    //  - LivingHurtEvent：挂着 [触及灵魂] 的目标受击瞬间记下原始伤害与当时的吸收心
    //    （完全落在同一次 hurt 调用栈里，无跨调用污染）；
    //  - LivingDamageEvent：把最终扣血改回原始值，并退还结算途中被吸走的吸收心——
    //    护甲/抗性/吸收全部作废，实打实扣血；死亡动画、掉落、统计全保留（没走 setHealth 捷径）。
    // 只跑服务端；效果是纯标记，这里不产生任何视觉/提示。
    // ======================================================================================

    /** UUID → {LivingHurtEvent 时刻的原始伤害, 当时的伤害吸收值}；LivingDamageEvent 消费即移除。 */
    private static final Map<UUID, float[]> SOUL_TOUCH_RAW_DAMAGE = new HashMap<>();

    @SubscribeEvent
    public static void onSoulTouchIncomingDamage(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || event.getAmount() <= 0.0F
                || !entity.hasEffect(SoulKatanaEffects.SOUL_TOUCH.get())) {
            return;
        }
        SOUL_TOUCH_RAW_DAMAGE.put(entity.getUUID(),
                new float[]{event.getAmount(), entity.getAbsorptionAmount()});
    }

    @SubscribeEvent
    public static void onSoulTouchFinalDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()
                || !entity.hasEffect(SoulKatanaEffects.SOUL_TOUCH.get())) {
            return;
        }
        float[] raw = SOUL_TOUCH_RAW_DAMAGE.remove(entity.getUUID());
        if (raw == null) {
            return;
        }
        if (raw[0] > event.getAmount()) {
            event.setAmount(raw[0]);
        }
        // vanilla 在两道事件之间已扣掉吸收心：按记录退还，让吸收盾也形同虚设
        if (entity.getAbsorptionAmount() < raw[1]) {
            entity.setAbsorptionAmount(raw[1]);
        }
    }

    @SubscribeEvent
    public static void onSoulTouchDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            SOUL_TOUCH_RAW_DAMAGE.remove(event.getEntity().getUUID());
        }
    }

    /** 左键点方块：手持魂刀时替换掉原版挖方块，施放 [解]。 */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !SoulKatanaIdentity.isSoulKatana(event.getItemStack())) {
            return;
        }
        if (event.getSide() == net.minecraftforge.fml.LogicalSide.CLIENT) {
            event.setCanceled(true);
            // 客户端上报已由 SoulKatanaMouseInput 统一处理，这里只拦挖方块
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
        if (!SoulKatanaIdentity.isSoulKatana(event.getItemStack())) {
            return;
        }
        // SoulKatanaMouseInput 已经上报过一次；这里作为兜底（未装本附属客户端时）。
        SoulKatanaNetwork.sendToServer(
                new SoulKatanaNetwork.SkillPacket(SoulKatanaNetwork.ACTION_TATAYAKU));
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
                || !SoulKatanaIdentity.isSoulKatana(event.getItemStack())) {
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

    /**
     * [无为转变]：主手持「魂刀·真人」右键击中生物。
     * Identity 可用 → 弹 Identity 化身选择界面，选中的化身施加给被击中实体（单人/服务器一致）；
     * 未装 Identity → 被击中实体随机变成一种生物（boss 除外）。
     * boss、傀儡与"已被转变过"的实体不生效（提示后同样占用这次交互，避免主模组顺手蓄力）。
     */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getSide() != net.minecraftforge.fml.LogicalSide.SERVER
                || event.getHand() != InteractionHand.MAIN_HAND
                || !SoulKatanaIdentity.isRealPerson(event.getItemStack())) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player
                && IdleTransformation.tryCast(player, event.getTarget())) {
            event.setCanceled(true);
        }
    }

    /**
     * 玩家进入世界：重建[无为转变]随机候选池——遍历所有已注册生物（含模组生物），
     * 避免池子停留在旧注册表快照上。
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            IdleTransformation.rebuildPool(player.serverLevel());
        }
    }

    /** 玩家退出：清掉挂起的[无为转变]选择，防止 UUID 泄漏。 */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            IdleTransformation.clearPending(player);
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
        if (SoulKatanaIdentity.isAnySoulBlade(event.player.getMainHandItem())) {
            if (!speed.hasModifier(SOUL_KATANA_SPEED)) {
                speed.addTransientModifier(SOUL_KATANA_SPEED);
            }
        } else if (speed.hasModifier(SOUL_KATANA_SPEED)) {
            speed.removeModifier(SOUL_KATANA_SPEED);
        }
    }

    /**
     * 魂刀 tooltip：
     *  - 刀名染成紫色（附魔金苹果同款）；
     *  - 宿傩刀：主模组的"锻造数"行替换为"手指数"（实时值）+ [解]/[捌]/[领域展开] 提示；
     *  - 真人刀：[无为转变] 与"手持解锁化身"提示。
     */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        boolean sukuna = SoulKatanaIdentity.isSoulKatana(stack);
        boolean realPerson = SoulKatanaIdentity.isRealPerson(stack);
        if (!sukuna && !realPerson) {
            return;
        }
        List<Component> tooltip = event.getToolTip();
        if (!tooltip.isEmpty() && tooltip.get(0) instanceof net.minecraft.network.chat.MutableComponent name) {
            name.withStyle(ChatFormatting.DARK_PURPLE);
        }
        if (sukuna) {
            int fingers = SoulKatanaIdentity.getFingerCount(stack);
            for (int i = 0; i < tooltip.size(); i++) {
                if (tooltip.get(i).getContents() instanceof TranslatableContents contents
                        && "slashblade.tooltip.refine".equals(contents.getKey())) {
                    Component line = Component.translatable("tooltip.soulkatana.finger_count", fingers)
                            .withStyle(fingers >= 20 ? ChatFormatting.LIGHT_PURPLE
                                    : fingers >= 10 ? ChatFormatting.GOLD : ChatFormatting.YELLOW);
                    tooltip.set(i, line);
                    break;
                }
            }
            tooltip.add(Component.translatable("tooltip.soulkatana.skills")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else {
            tooltip.add(Component.translatable("tooltip.soulkatana.real_person")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
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
        if (!SoulKatanaIdentity.isSoulKatana(left)
                || right.getItem() != SoulKatanaItems.SUKUNA_FINGER.get()) {
            return;
        }
        int fingers = SoulKatanaIdentity.getFingerCount(left);
        ItemStack output = left.copy();
        CompoundTag fresh = SoulKatanaIdentity.liveState(left).copy();
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
        // 雪屋地下室的宝箱：固定刷新 4 个改造人（roll 4 次，物品不可堆叠 → 落在 4 个随机格子）
        if (key.getPath().contains("igloo")) {
            event.getTable().addPool(net.minecraft.world.level.storage.loot.LootPool.lootPool()
                    .name("soulkatana_reformed_human")
                    .setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(4.0F))
                    .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(
                                    SoulKatanaItems.REFORMED_HUMAN.get())
                            .setWeight(1))
                    .build());
            return;
        }
        if (!STRUCTURE_CHESTS.contains(key)) {
            return;
        }
        event.getTable().addPool(net.minecraft.world.level.storage.loot.LootPool.lootPool()
                .name("soulkatana_finger")
                .setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1.0F))
                .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(
                                SoulKatanaItems.SUKUNA_FINGER.get())
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
