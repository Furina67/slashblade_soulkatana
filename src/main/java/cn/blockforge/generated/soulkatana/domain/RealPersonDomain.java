package cn.blockforge.generated.soulkatana.domain;

import cn.blockforge.generated.soulkatana.SoulKatanaEffects;
import cn.blockforge.generated.soulkatana.SoulKatanaIdentity;
import cn.blockforge.generated.soulkatana.SoulKatanaSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * [领域展开 · 自闭圆顿裹]（服务端）：主手持「魂刀·真人」时按鼠标中键触发，骨架照 SoulDomain。
 *  1. 台词闸门：按下中键先完整播完台词（self_embodiment_of_perfection_voiceline，服务端解析 OGG
 *     得到精确时长，实测约 7.5 秒 = 150 tick）——台词完整播完才进入第 2 步，
 *     台词期间再按中键无效，收刀/死亡/下线则停音中止；
 *  2. 展开：台词播完的当 tick 立即以界主脚下为球心、半径 64 格的【上半球】领域成立（判定含 Y 轴，
 *     对半球内除界主外的一切活物挂 BUFF【触及灵魂】，播展开音（self_embodiment_of_perfection），
 *     画暗红（0xA01010）尘粒子边界环——只有边界粒子，没有渲染层/着色器/后处理；
 *  3. 领域存续期间每 tick 维护半球内目标的 soul_touch 时长（出球/死亡即时摘除）；
 *  4. 全域必中：界主每一次"发起攻击"——哪怕对着空气/方块空挥（客户端 MISS/BLOCK 上报
 *     ACTION_DOMAIN_ATTACK），甚至没打中任何东西——都对领域内所有挂【触及灵魂】的实体
 *     各结算一次 dealDomainHit；命中实体的那一下走原版近战（directHit 不重复结算）。
 *     dealDomainHit 完整复刻原版 Player.attack() 的伤害公式，而护甲/抗性/吸收的清零
 *     由事件层保证（SoulKatanaEvents，触及灵魂真实伤害）；
 *  5. 再按一次中键关闭（即时收尾）；界主死亡/换维度/下线/走出领域自动收尾；关闭后 30 秒冷却。
 * 领域不破坏地形、不生成任何方块或实体。
 */
@Mod.EventBusSubscriber(modid = "soulkatana")
public final class RealPersonDomain {
    public static final int RADIUS_BLOCKS = 64;
    private static final double RADIUS = RADIUS_BLOCKS;
    /** self_embodiment_of_perfection_voiceline.ogg 的完整时长（tick）；0 = 未解析。 */
    private static int voicelineTicks;
    /** 解析失败时的兜底：按当前音频实测 7.499s × 20 = 150 tick。 */
    private static final int VOICELINE_FALLBACK_TICKS = 150;
    public static final int DOMAIN_COOLDOWN_TICKS = 600;
    /** 冷却键与宿傩领域复用同一个（两把刀共享一条领域冷却）。 */
    private static final String DOMAIN_COOLDOWN_KEY = "SoulbladeDomainCd";
    /** [触及灵魂] 的维持时长：界主每 tick 续一次，领域收尾时显式摘除，残留的靠自然过期兜底。 */
    private static final int SOUL_TOUCH_MAINTAIN_TICKS = 40;
    /** 边界环约每 3 格一个粒子（与 SoulDomain 同密度）。 */
    private static final int RING_POINTS = (int) Math.ceil(2.0D * Math.PI * RADIUS / 3.0D);
    /** 暗红 0xA01010 的尘粒子。 */
    private static final DustParticleOptions DOMAIN_RING = new DustParticleOptions(
            new Vector3f(0xA0 / 255.0F, 0x10 / 255.0F, 0x10 / 255.0F), 0.2F);
    private static final Map<UUID, ActiveDomain> ACTIVE_DOMAINS = new HashMap<>();
    private static final Map<UUID, ChargingDomain> CHARGING_DOMAINS = new HashMap<>();
    /** 防事件重入：全域结算进行中，不再触发第二次全域结算（见 onOwnerAttack）。 */
    private static boolean resolving = false;

    private RealPersonDomain() { }

    /** 真人刀中键入口（SoulKatanaNetwork 按手持刀分流到这里）。 */
    public static void toggle(ServerPlayer player) {
        if (player.isSpectator() || !player.isAlive() || !isHoldingRealPerson(player)) {
            return;
        }
        UUID id = player.getUUID();
        if (ACTIVE_DOMAINS.containsKey(id)) {
            close(player);
            return;
        }
        if (CHARGING_DOMAINS.containsKey(id) || SoulDomain.isDomainCoolingDown(player)) {
            return;
        }
        beginCharge(player);
    }

    public static boolean isHoldingRealPerson(Player player) {
        return SoulKatanaIdentity.isRealPerson(player.getMainHandItem());
    }

    /** 台词闸门开始：登记读条（时长 = 台词 OGG 完整时长），并在界主位置播台词。 */
    private static void beginCharge(ServerPlayer player) {
        ServerLevel server = (ServerLevel) player.level();
        ChargingDomain charging = new ChargingDomain(server.getGameTime());
        CHARGING_DOMAINS.put(player.getUUID(), charging);
        charging.musicNotified.addAll(playDomainSound(server, player.position(),
                SoulKatanaSounds.SELF_EMBODIMENT_OF_PERFECTION_VOICELINE.get(), 1.6F));
    }

    /** self_embodiment_of_perfection_voiceline.ogg 播完所需 tick 数（服务端解析 OGG，与真实音频严格对齐）。 */
    private static int voicelineDurationTicks() {
        if (voicelineTicks <= 0) {
            voicelineTicks = SoulDomain.readOggDurationTicks(
                    "/assets/soulkatana/sounds/self_embodiment_of_perfection_voiceline.ogg",
                    VOICELINE_FALLBACK_TICKS);
        }
        return voicelineTicks;
    }

    /** 展开领域（台词已完整播完的当 tick 调用）：半球内首个挂 [触及灵魂]、播展开音、画边界环。台词未播完则停音名单移交界主统一收尾。 */
    public static void cast(ServerPlayer player, Set<UUID> voicelineListeners) {
        ServerLevel server = (ServerLevel) player.level();
        Vec3 center = player.position();
        ActiveDomain domain = new ActiveDomain(server, center, player.getId());
        ACTIVE_DOMAINS.put(player.getUUID(), domain);
        if (voicelineListeners != null) {
            domain.musicNotified.addAll(voicelineListeners);
        }
        server.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 1.0D, center.z,
                32, 1.0D, 1.0D, 1.0D, 0.2D);
        domain.musicNotified.addAll(playDomainSound(server, center,
                SoulKatanaSounds.SELF_EMBODIMENT_OF_PERFECTION.get(), 1.2F));
        maintainSoulTouch(domain, player);
        sendBoundary(server, center, player);
        player.displayClientMessage(Component.translatable("message.soulkatana.real_domain_started"), false);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        tick(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            onOwnerDisconnect(player);
        }
    }

    /** 维护每个玩家的前摇与领域。 */
    private static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        ServerLevel server = (ServerLevel) player.level();

        ChargingDomain charging = CHARGING_DOMAINS.get(id);
        if (charging != null) {
            if (!player.isAlive() || !isHoldingRealPerson(player)) {
                cancelCharging(player);
            } else if (server.getGameTime() - charging.startedAt >= voicelineDurationTicks()) {
                // 台词音效已完整播完：这一刻才展开领域、挂 BUFF、播展开音，不再与台词后半段重叠。
                CHARGING_DOMAINS.remove(id);
                cast(player, charging.musicNotified);
            }
        }

        ActiveDomain domain = ACTIVE_DOMAINS.get(id);
        if (domain == null) {
            return;
        }
        if (domain.level != player.level() || !player.isAlive()
                || domain.ownerEntityId != player.getId()) {
            ACTIVE_DOMAINS.remove(id);
            releaseSoulTouch(domain);
            stopDomainMusic(domain.level, domain.musicNotified,
                    SoulKatanaSounds.SELF_EMBODIMENT_OF_PERFECTION.get(),
                    SoulKatanaSounds.SELF_EMBODIMENT_OF_PERFECTION_VOICELINE.get());
            return;
        }
        // 界主走出领域范围（水平半径外）立刻结束
        double awayX = player.getX() - domain.center.x;
        double awayZ = player.getZ() - domain.center.z;
        if (awayX * awayX + awayZ * awayZ > RADIUS * RADIUS) {
            close(player);
            return;
        }
        maintainSoulTouch(domain, player);
        if (server.getGameTime() % 10L == 0L) {
            sendBoundary(server, domain.center, player);
        }
    }

    private static void cancelCharging(ServerPlayer player) {
        ChargingDomain charging = CHARGING_DOMAINS.remove(player.getUUID());
        if (charging != null && player.level() instanceof ServerLevel server) {
            stopDomainMusic(server, charging.musicNotified,
                    SoulKatanaSounds.SELF_EMBODIMENT_OF_PERFECTION_VOICELINE.get());
        }
    }

    private static void close(ServerPlayer player) {
        cancelCharging(player);
        ActiveDomain existing = ACTIVE_DOMAINS.remove(player.getUUID());
        if (existing == null) {
            return;
        }
        releaseSoulTouch(existing);
        stopDomainMusic(existing.level, existing.musicNotified,
                SoulKatanaSounds.SELF_EMBODIMENT_OF_PERFECTION.get(),
                SoulKatanaSounds.SELF_EMBODIMENT_OF_PERFECTION_VOICELINE.get());
        player.getPersistentData().putLong(DOMAIN_COOLDOWN_KEY,
                existing.level.getGameTime() + DOMAIN_COOLDOWN_TICKS);
    }

    /** 界主下线的收尾：摘 BUFF、停音，与 SoulDomain 同款处理（不留僵尸领域）。 */
    private static void onOwnerDisconnect(ServerPlayer player) {
        cancelCharging(player);
        ActiveDomain domain = ACTIVE_DOMAINS.remove(player.getUUID());
        if (domain == null) {
            return;
        }
        releaseSoulTouch(domain);
        stopDomainMusic(domain.level, domain.musicNotified,
                SoulKatanaSounds.SELF_EMBODIMENT_OF_PERFECTION.get(),
                SoulKatanaSounds.SELF_EMBODIMENT_OF_PERFECTION_VOICELINE.get());
    }

    /** 领域关闭/消散：显式摘掉所有目标身上的 [触及灵魂]。 */
    private static void releaseSoulTouch(ActiveDomain domain) {
        for (LivingEntity touched : domain.touched.values()) {
            if (touched != null && !touched.isRemoved()) {
                touched.removeEffect(SoulKatanaEffects.SOUL_TOUCH.get());
            }
        }
        domain.touched.clear();
    }

    /**
     * 半球扫描：给领域内（除界主）的活物挂/续 [触及灵魂]，出半球或死亡的即时摘除。
     * 每 tick 由 tick() 调用一次，纯标记效果、不产生任何视觉。
     */
    private static void maintainSoulTouch(ActiveDomain domain, ServerPlayer owner) {
        AABB search = new AABB(domain.center.x - RADIUS, domain.center.y,
                domain.center.z - RADIUS, domain.center.x + RADIUS,
                domain.center.y + RADIUS, domain.center.z + RADIUS);
        Map<UUID, LivingEntity> insideNow = new HashMap<>();
        for (LivingEntity target : domain.level.getEntitiesOfClass(LivingEntity.class, search,
                entity -> entity != owner && entity.isAlive())) {
            if (!inDome(domain.center, target)) {
                continue;
            }
            insideNow.put(target.getUUID(), target);
            target.addEffect(new MobEffectInstance(SoulKatanaEffects.SOUL_TOUCH.get(),
                    SOUL_TOUCH_MAINTAIN_TICKS, 0, false, false, false));
        }
        Iterator<Map.Entry<UUID, LivingEntity>> it = domain.touched.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, LivingEntity> entry = it.next();
            if (insideNow.containsKey(entry.getKey())) {
                continue;
            }
            LivingEntity gone = entry.getValue();
            if (gone != null && !gone.isRemoved()) {
                gone.removeEffect(SoulKatanaEffects.SOUL_TOUCH.get());
            }
            it.remove();
        }
        domain.touched.putAll(insideNow);
    }

    /**
     * 半球判定（关键，区别于宿傩领域的纯水平 dx²+dz²）：
     * 球（带 Y 轴）且目标只取界主脚下平面以上——dx²+dy²+dz² ≤ R² 且 target.getY() ≥ center.getY()。
     */
    private static boolean inDome(Vec3 center, Entity target) {
        double dx = target.getX() - center.x;
        double dy = target.getY() - center.y;
        double dz = target.getZ() - center.z;
        return dy >= 0.0D && dx * dx + dy * dy + dz * dz <= RADIUS * RADIUS;
    }

    /**
     * 全域必中的总入口：界主"发起攻击"（含对空气/方块空挥）时对半球内所有
     * [触及灵魂] 实体各结算一次 dealDomainHit。
     * 两个触发源都收敛到这里：
     *  - 客户端左键 MISS/BLOCK 上报的 ACTION_DOMAIN_ATTACK（directHit 为 null）；
     *  - 左键真的命中实体的 AttackEntityEvent（directHit 交给原版近战结算，这里跳过防双算）。
     */
    public static void onOwnerAttack(ServerPlayer owner) {
        onOwnerAttack(owner, null);
    }

    /** @param directHit 本击已由原版近战结算的实体（可为 null），全域结算时跳过防重复。 */
    public static void onOwnerAttack(ServerPlayer owner, Entity directHit) {
        if (resolving) {
            return;
        }
        ActiveDomain domain = ACTIVE_DOMAINS.get(owner.getUUID());
        if (domain == null || domain.level != owner.level() || !isHoldingRealPerson(owner)) {
            return;
        }
        // 同一次挥刀只结算一轮：空挥上报与实体命中事件同 tick 撞车时按 tick 去重
        if (domain.lastBurstTick == domain.level.getGameTime()) {
            return;
        }
        domain.lastBurstTick = domain.level.getGameTime();
        List<LivingEntity> targets = new ArrayList<>(domain.touched.values());
        resolving = true;
        try {
            for (LivingEntity target : targets) {
                if (target == directHit || target.isRemoved() || !target.isAlive()) {
                    continue;
                }
                if (!inDome(domain.center, target)) {
                    continue;
                }
                dealDomainHit(owner, target);
            }
        } finally {
            resolving = false;
        }
        domain.touched.entrySet().removeIf(entry ->
                entry.getValue().isRemoved() || !entry.getValue().isAlive());
    }

    /** 左键命中实体时原版路径也会发起一次全域结算（directHit 不重复吃）。 */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        onOwnerAttack(player, event.getTarget());
    }

    /**
     * 必中结算：伤害公式逐项复刻原版 Player.attack()（与界主普通攻击打同一目标的结果一致）：
     *  1) 攻击力面板 getAttributeValue(ATTACK_DAMAGE)——原版的力量药水(MobEffects.DAMAGE_BOOST)
     *     本身就是挂在攻击力上的属性修饰符（每级 +3.0 × (amplifier+1)），已折算进第 1) 项，
     *     与原版一致地"计算"，不重复叠加；
     *  2) 附魔增伤 EnchantmentHelper.getDamageBonus(主手, 目标 MobType)（原版对玩家目标即 UNDEFINED）；
     *  3) 蓄力倍率 getAttackStrengthScale(0.5F)：基础伤害 ×(0.2+0.8·scale²)，附魔增伤 ×scale；
     *  4) 暴击（满蓄力 + 滞空下落的原版条件）×1.5；
     *  5) 火矢附魔命中后点燃（等级 ×4 秒，照 SoulDomain 的写法）。
     * 强制扣血：先把 invulnerableTime/hurtTime 清零再走 hurt()，死亡动画/不死图腾/掉落全部保留；
     * 护甲/抗性/吸收的作废由事件层保证（SoulKatanaEvents 对 [触及灵魂] 目标还原真实伤害）。
     */
    public static void dealDomainHit(Player attacker, LivingEntity target) {
        ItemStack weapon = attacker.getMainHandItem();
        float base = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float enchantBonus = EnchantmentHelper.getDamageBonus(weapon, target.getMobType());
        float scale = attacker.getAttackStrengthScale(0.5F);
        base *= 0.2F + scale * scale * 0.8F;
        enchantBonus *= scale;
        boolean critical = scale > 0.9F && attacker.fallDistance > 0.0F && !attacker.onGround()
                && !attacker.onClimbable() && !attacker.isInWater()
                && !attacker.hasEffect(MobEffects.BLINDNESS) && !attacker.isPassenger()
                && !attacker.isSprinting();
        if (critical) {
            base *= 1.5F;
        }
        float damage = base + enchantBonus;
        if (damage <= 0.0F) {
            return;
        }
        DamageSource source = attacker.damageSources().playerAttack(attacker);
        if (target.isInvulnerableTo(source)) {
            return;
        }
        target.invulnerableTime = 0;
        target.hurtTime = 0;
        if (target.hurt(source, damage)) {
            target.invulnerableTime = 0;
            int fire = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, weapon);
            if (fire > 0) {
                target.setSecondsOnFire(fire * 4);
            }
        }
    }

    /**
     * 演出音：播放一次并广播，返回真正收到这段音乐的玩家名单——
     * 半径用 SoundEvent#getRange，与服务端内部广播判定一致（照 SoulDomain 同款实现）。
     */
    private static Set<UUID> playDomainSound(ServerLevel server, Vec3 pos, SoundEvent sound, float volume) {
        server.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, 1.0F);
        Set<UUID> heard = new HashSet<>();
        double range = (double) sound.getRange(volume);
        double rangeSquared = range * range;
        for (ServerPlayer target : server.players()) {
            if (target.distanceToSqr(pos.x, pos.y, pos.z) <= rangeSquared) {
                heard.add(target.getUUID());
            }
        }
        return heard;
    }

    /** 停播音乐：按名单补发 ClientboundStopSoundPacket（全发会误伤宿傩领域的演出音）。 */
    private static void stopDomainMusic(ServerLevel server, Set<UUID> listeners, SoundEvent... sounds) {
        if (listeners.isEmpty()) {
            return;
        }
        for (SoundEvent sound : sounds) {
            ClientboundStopSoundPacket stop = new ClientboundStopSoundPacket(sound.getLocation(), SoundSource.PLAYERS);
            for (UUID listener : listeners) {
                ServerPlayer target = server.getServer().getPlayerList().getPlayer(listener);
                if (target != null) {
                    target.connection.send(stop);
                }
            }
        }
        listeners.clear();
    }

    /** 边界环：只画半球底圈（暗红尘粒子）；普通粒子广播只达 32 格，界主另走逐玩家通道。 */
    private static void sendBoundary(ServerLevel server, Vec3 center, ServerPlayer owner) {
        double ringY = center.y + 0.1D;
        for (int i = 0; i < RING_POINTS; i++) {
            double angle = i * Math.PI * 2.0D / RING_POINTS;
            double x = center.x + Math.cos(angle) * RADIUS;
            double z = center.z + Math.sin(angle) * RADIUS;
            server.sendParticles(DOMAIN_RING, x, ringY, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            if (owner != null) {
                server.sendParticles(owner, DOMAIN_RING, true, x, ringY, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    /** 前摇登记（照 SoulDomain 的 ChargingDomain）。 */
    private static final class ChargingDomain {
        private final long startedAt;
        private final Set<UUID> musicNotified = new HashSet<>();

        private ChargingDomain(long startedAt) {
            this.startedAt = startedAt;
        }
    }

    /** 领域登记：球心固定在展开瞬间界主脚下，维护 [触及灵魂] 名单与演出音名单。 */
    private static final class ActiveDomain {
        private final ServerLevel level;
        private final Vec3 center;
        private final int ownerEntityId;
        private final Set<UUID> musicNotified = new HashSet<>();
        /** 半球内挂了 [触及灵魂] 的目标（不含界主），每 tick 维护。 */
        private final Map<UUID, LivingEntity> touched = new HashMap<>();
        /** 上一轮全域必中结算的 tick（空挥上报与命中事件同 tick 去重）。 */
        private long lastBurstTick = Long.MIN_VALUE;

        private ActiveDomain(ServerLevel level, Vec3 center, int ownerEntityId) {
            this.level = level;
            this.center = center;
            this.ownerEntityId = ownerEntityId;
        }
    }
}
